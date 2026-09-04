# Texas Hold'em Poker API — Status Report

_Generated 2026-09-04. Covers the functional audit of the deployed API and the fixes applied in response._

## 1. What this project is

`vendredi.soir.karata` is a POJA-generated Spring Boot service (Java 21, Gradle, PostgreSQL, deployed to AWS Lambda) whose actual product is a Texas Hold'em poker REST API, specified in `doc/api.yml` (OpenAPI 3.0.3, base path `/poker`).

- `core/` — pure Java, framework-free, event-sourced domain model (`Game`, `Deal`, `Player`, `Deck`, `Card`, `Hand`, and a sealed `Action` hierarchy: `Bet`, `Call`, `Check`, `Fold`, `Raise`, `SmallBlind`, `BigBlind`, `DealHoleCard`, `RevealCards`, `Showdown`, `AwardPot`, `InitializePlayerChips`). All state (chips, pot, board, phase) is derived by replaying the action list — there is no snapshotting.
- `core/rules/TexasHoldemRules` — legality checks, next-player logic, hand evaluation, minimum raise. Intentionally simplified (no button rotation, no side pots) — this report treats those simplifications as accepted design, not bugs.
- `service/GameService` — persistence + replay-to-build-`Game`, pessimistic-lock concurrency (`SELECT ... FOR UPDATE`).
- `service/DealService` — turn validation, dispatches player actions to the `Dealer`.
- `service/JwtService` — hand-rolled HMAC-SHA256 JWT check.
- REST surface (`PokerController`): create/get game, join (buy-in), start a deal, take an action, get own hand.

Deployed at: `https://62zx5a4vo6n3zjykzu7dx3a4zy0imiwo.lambda-url.eu-west-3.on.aws/` (eu-west-3, via `.github/workflows/cd-compute.yml` on push to `preprod`).

## 2. Functional audit — what was actually tested

The question asked was: **"can we play a full game?"** — so the API was exercised end-to-end against the live deployment (create game → join players → start deal → check hand → take actions), not just read from source. Three real, reproducible defects were found this way; none of them are visible from a code read alone plus unit tests, because the unit tests only exercise the core domain with hand-held, in-memory `Player` objects that never go through a JSON/DB round trip.

### 2.1 Chip ledger silently zeroed out (fixed)
**Symptom:** `POST /games/{id}/players` with `buyInAmount: 1000` returns `204`, but `GET /games/{id}` then shows `chips: 0` for that player.

**Root cause:** `core/entity/Player.java` had no `equals()`/`hashCode()` override, so it used default reference (identity) equality. `Game.getChips()` matches `InitializePlayerChips` actions to a player via `Player.equals()`. Every time `GameService.getGame()` replays history, `ActionMapper` deserializes a **new** `Player` instance from the stored JSON, which is never `==`-equal (and, without an override, never `.equals()`-equal) to the `Player` instances built elsewhere (e.g. from `PlayerEntity` rows). The lookup silently failed and always summed to `0`. This wasn't just a display bug — `TexasHoldemRules.isActionLegal` also calls `getChips()`, so it corrupted betting legality too. It only "worked" in unit tests because those tests hand-construct one `Player` instance and reuse the same reference throughout.

**Fix:** `Player` now has `@EqualsAndHashCode(of = "name")` — a player is uniquely identified by username within a game, matching how usernames are already used everywhere else (JWT claim, turn checks, hand lookup).

### 2.2 Starting a deal crashed with a SQL error (fixed)
**Symptom:** `POST /games/{id}/deals` returned `500`:
```
value too long for type character varying(255)
```

**Root cause:** `ActionEntity.payload` was a plain `String` field with no explicit column definition, so Hibernate defaulted it to `varchar(255)`. The very first action of any deal, `ShuffleDeck`, serializes the full 52-card deck to JSON, which is well over 255 characters — so the insert failed and **no deal could ever be started** via the API, blocking everything downstream (hole cards, betting, showdown all depend on a deal existing).

**Fix:** `ActionEntity.payload` is now `@Column(columnDefinition = "TEXT")`. A new Flyway migration (`V42_3__Widen_poker_action_payload_column.sql`) widens the column on databases where `poker_action` already exists (e.g. the live `preprod` DB); it's a guarded no-op on a fresh database, where Hibernate's `ddl-auto=update` will create the column as `TEXT` directly from the updated entity annotation.

### 2.3 No automatic deal orchestration (fixed)
**Symptom:** Even with the two bugs above fixed, there was no way to actually play a hand. `DealService.startDeal` only shuffled the deck — nothing posted blinds, dealt hole cards, or ever advanced from PRE_FLOP to FLOP/TURN/RIVER/SHOWDOWN. The core engine already supported all of this (proven by the existing `FullTexasHoldemDealTest`, which drives blinds/hole cards/streets/showdown by hand), but the service layer never called any of it.

**Fix — `DealService.startDeal`:**
- Requires at least 2 players with chips > 0 (`BadRequestException` otherwise).
- Automatically posts small blind (first eligible player) and big blind (second eligible player), using the game's configured blind amounts.
- Deals 2 hole cards to each eligible player, one card per player per round (round-robin), drawn in the order the deck was shuffled.

**Fix — automatic street progression, called after every player action (`DealService.progressDealIfNeeded`):**
- If every other player has folded, immediately triggers `Showdown` and awards the pot to the sole remaining player — without requiring a 5-card hand (see 2.3.1 below).
- Otherwise, once the current betting round is complete (every active player has acted this street and all active contributions are equal), automatically reveals the next street: 3 cards for the flop, 1 for the turn, 1 for the river, then triggers `Showdown` after river betting closes.

New supporting core logic (in `core/`, pure Java, no framework dependency — consistent with `AGENTS.md`):
- `Rules.isBettingRoundComplete(deal, players)` / `TexasHoldemRules` implementation — every active player must have a `PlayerAction` in the current street and an equal round contribution.
- `Deal.nextCards(int count)` — returns the next N undealt cards from the shuffled order, computed from how many hole/community cards have already been dealt in this deal's history (safe to call across independently-replayed request handling, since it's derived from persisted history rather than mutable deck state).
- `TexasHoldemRules.evaluateWinners` — now special-cases "only one non-folded player remains" and awards them the pot directly, instead of requiring `>= 5` cards to evaluate a hand (which would silently produce an empty winners map on an early fold, dropping the pot).
- `TexasHoldemRules.determineNextPlayer` — now returns `null` once a `Showdown` has occurred in the deal, so the existing "No active turn or deal is over" check in `DealService.takeAction` correctly rejects actions on a finished hand (previously it could look like a player still had a turn).
- `Deal.getCurrentPhase()` — now reports `SHOWDOWN` once a `Showdown` action exists (previously the "SHOWDOWN" branch was dead code: reveal count never exceeds 3 in real play, so a completed hand permanently showed as `RIVER`).

### 2.3.1 Known simplification carried forward
The betting-round-complete check treats blinds as "acted", which means the classic **big-blind option** (BB should still get a chance to act if everyone just calls around to them preflop) is not implemented — the round is considered closed as soon as contributions match, even if the BB never voluntarily acted. This is consistent with the project's existing "simplified" rules philosophy (no button rotation, no side pots) and was left as-is per instructions not to over-engineer beyond fixing what's broken. Flagged here for visibility, not fixed.

## 3. Explicitly left as-is (per instruction)

**Authentication is not real.** `JwtService` has a dev fallback: when `jwt.secret` is unset (as it currently is on the deployed environment) and the bearer token isn't a 3-part JWT, it trusts the raw token string as the username with zero verification. Anyone can act as any player by sending `Authorization: Bearer <username>`. This was confirmed live and is a real security gap, but per instruction it is **not being fixed now** ("too much to add auth while still debugging") — noted here so it isn't forgotten.

## 4. Other gaps noticed but not in scope for this pass

- `GET /games/{gameId}/events` (SSE timeline) is documented in `doc/api.yml` but was never implemented — building real event streaming is a new feature, not a fix, so it was left alone. `POST /games/{gameId}/deals` was implemented but undocumented; the spec has been updated to include it.
- No side-pot handling for all-in scenarios with unequal stacks (existing simplification, unchanged).
- No real button rotation between hands — the same first/second eligible player always post SB/BB (existing simplification, unchanged).
- Stale, conflicting **open PR #2** ("Performant Poker OpenAPI Specification", from the `jules` AI agent, 2026-07-05) is superseded by the merged spec and marked `CONFLICTING` by GitHub — likely safe to close, but left untouched pending explicit confirmation.

## 5. Verification

Per project convention, changes were **not** built or tested locally — CI (`.github/workflows/ci.yml`, `./gradlew test` + format check) runs on every push/PR and is the source of truth for build/test status. Check `gh pr checks` or `gh run list` after pushing rather than running Gradle locally.
