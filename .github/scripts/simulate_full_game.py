#!/usr/bin/env python3
"""Drives a real, complete heads-up game through the live poker API over HTTP - not mocked.

Two existing users (default user1/user2, password 1234567890 - falls back to registering them
if login fails, so this also works against a freshly-seeded environment) create a table, buy in,
and play a configurable number of hands. Every action is decided from the server's own
personalized "you" block (callAmount/minRaise/maxRaise) each turn, varying between
check/bet/call/raise/fold to exercise as many real code paths as possible per run - not just the
lightest possible check-down. Stops early (not a failure) if a player's stack gets too short to
continue, since this is a heads-up game with no rebuy mechanism.

Only uses the Python standard library (urllib) - no pip install step needed in CI.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request

API_URL = os.environ["API_URL"].rstrip("/")
# /ping and /health/db are top-level paths, not under /poker - derive the site root so warm_up()
# can reach them regardless of whether API_URL was given with or without the /poker suffix.
ROOT_URL = API_URL[: -len("/poker")] if API_URL.endswith("/poker") else API_URL
HANDS = int(os.environ.get("HANDS", "15"))
SMALL_BLIND = int(os.environ.get("SMALL_BLIND", "10"))
BIG_BLIND = int(os.environ.get("BIG_BLIND", "20"))
BUY_IN = int(os.environ.get("BUY_IN", "2000"))
USERNAME_1 = os.environ.get("USERNAME_1", "user1")
USERNAME_2 = os.environ.get("USERNAME_2", "user2")
PASSWORD = os.environ.get("PASSWORD", "1234567890")
MAX_ACTIONS_PER_HAND = 60


def log(msg):
    print(msg, flush=True)


def http(method, path, token=None, body=None, timeout=60, base=None):
    url = f"{base or API_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            if not raw:
                return resp.status, None
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                # /ping (and similar plain-text endpoints) don't return JSON.
                return resp.status, raw.decode(errors="replace")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw.decode(errors="replace")
        return e.code, parsed


def login_or_register(username, password):
    status, body = http("POST", "/auth/login", body={"username": username, "password": password})
    if status == 200:
        log(f"Logged in as {username}")
        return body["token"]
    log(f"Login failed for {username} ({status}: {body}) - trying to register instead")
    status, body = http(
        "POST", "/auth/register", body={"username": username, "password": password}
    )
    if status == 201:
        log(f"Registered {username}")
        return body["token"]
    log(f"FATAL: could not login or register {username}: {status} {body}")
    sys.exit(1)


def decide_action(action_counter, call_amount, current_round_bet, min_raise, max_raise):
    """Varies deliberately (rather than always checking/calling) to exercise as many action
    types and code paths as possible across a run - the point of this being "the heaviest
    possible" simulation, not the lightest one that would just check every street down."""
    raise_type = "BET" if current_round_bet == 0 else "RAISE"
    can_raise = max_raise > 0 and max_raise >= min_raise and max_raise > call_amount

    if call_amount == 0:
        if can_raise and action_counter % 4 == 0:
            return raise_type, min_raise
        return "CHECK", None

    # Facing a bet with less than enough chips to call in full: there's no all-in-for-less/
    # side-pot support in this engine, so a full call would be illegal - fold instead, same as
    # the web/mobile clients now gray out Call in this exact situation.
    if max_raise < call_amount:
        return "FOLD", None
    if action_counter % 7 == 0:
        return "FOLD", None
    if can_raise and action_counter % 3 == 0:
        return raise_type, min_raise
    return "CALL", call_amount


def warm_up(budget_seconds=180):
    """Free-tier hosts (and Neon's own serverless compute) can take 60-100+s to respond to the
    very first request after a period of inactivity - absorb that here, visibly and with a
    generous timeout, so a cold start doesn't look like a mysterious hang once real gameplay
    starts, and doesn't need every single call site to carry its own oversized timeout."""
    log("Warming up (first request to a cold host/DB can take a while)...")
    deadline = time.time() + budget_seconds
    attempt = 0
    while time.time() < deadline:
        attempt += 1
        try:
            status, _ = http("GET", "/ping", timeout=budget_seconds, base=ROOT_URL)
            if status == 200:
                log(f"Warm (attempt {attempt}).")
                return
            log(f"Attempt {attempt}: /ping returned {status}, retrying...")
        except Exception as e:
            log(f"Attempt {attempt}: {e}, retrying...")
        time.sleep(3)
    log(f"FATAL: API did not respond within {budget_seconds}s of warm-up attempts.")
    sys.exit(1)


def main():
    log(f"Target API: {API_URL}")
    log(f"Simulating up to {HANDS} hands, blinds {SMALL_BLIND}/{BIG_BLIND}, buy-in {BUY_IN}")

    warm_up()

    tokens = {
        USERNAME_1: login_or_register(USERNAME_1, PASSWORD),
        USERNAME_2: login_or_register(USERNAME_2, PASSWORD),
    }

    table_name = f"CI Simulation {int(time.time())}"
    status, body = http(
        "POST",
        "/games",
        body={
            "name": table_name,
            "blinds": {"small": SMALL_BLIND, "big": BIG_BLIND},
            "defaultBuyIn": BUY_IN,
        },
    )
    if status != 201:
        log(f"FATAL: could not create game: {status} {body}")
        sys.exit(1)
    game_id = body["gameId"]
    log(f'Created game {game_id} ("{table_name}")')

    for username in (USERNAME_1, USERNAME_2):
        status, body = http(
            "POST",
            f"/games/{game_id}/players",
            token=tokens[username],
            body={"buyInAmount": BUY_IN},
        )
        if status != 204:
            log(f"FATAL: {username} could not buy in: {status} {body}")
            sys.exit(1)
        log(f"{username} bought in for {BUY_IN}")

    action_counter = 0
    total_actions = 0
    hands_completed = 0

    for hand_index in range(1, HANDS + 1):
        status, state = http("GET", f"/games/{game_id}")
        chips_by_username = {p["username"]: p["chips"] for p in state["players"]}
        short_stacked = [
            u for u in (USERNAME_1, USERNAME_2) if chips_by_username.get(u, 0) <= BIG_BLIND
        ]
        if short_stacked:
            log(f"Stopping early before hand {hand_index}: {short_stacked} too short-stacked to continue")
            break

        status, body = http("POST", f"/games/{game_id}/deals", token=tokens[USERNAME_1])
        if status != 201:
            log(f"FATAL: could not start hand {hand_index}: {status} {body}")
            sys.exit(1)
        deal_id = body["currentDealId"]
        log(f"--- Hand {hand_index}/{HANDS} (deal {deal_id}) ---")

        actions_this_hand = 0
        while True:
            actions_this_hand += 1
            if actions_this_hand > MAX_ACTIONS_PER_HAND:
                log(f"FATAL: hand {hand_index} exceeded {MAX_ACTIONS_PER_HAND} actions - aborting")
                sys.exit(1)

            status, state = http("GET", f"/games/{game_id}")
            deal = state["currentDeal"]
            if deal is None or deal["phase"] == "SHOWDOWN":
                outcome = deal["outcome"] if deal else None
                if outcome and outcome.get("winners"):
                    winners = ", ".join(
                        f"{w['username']} (+{w['amount']})" for w in outcome["winners"]
                    )
                    log(f"Hand {hand_index} finished: {winners}")
                else:
                    log(f"Hand {hand_index} finished (no outcome recorded)")
                hands_completed += 1
                break

            active_player_id = deal["activePlayerId"]
            if active_player_id is None:
                log(f"FATAL: hand {hand_index} has no active player but isn't at showdown either")
                sys.exit(1)

            acting_username = next(
                (p["username"] for p in state["players"] if p["playerId"] == active_player_id),
                None,
            )
            if acting_username is None:
                log(f"FATAL: could not resolve active player {active_player_id} to a username")
                sys.exit(1)

            status, personal = http("GET", f"/games/{game_id}", token=tokens[acting_username])
            you = personal["you"]
            if you is None:
                log(f"FATAL: no personalized 'you' block for {acting_username} on their own turn")
                sys.exit(1)

            action_type, amount = decide_action(
                action_counter,
                you["callAmount"],
                deal["currentRoundBet"],
                you["minRaise"],
                you["maxRaise"],
            )
            action_counter += 1
            total_actions += 1

            action_body = {"actionType": action_type}
            if amount is not None:
                action_body["amount"] = amount
            status, resp = http(
                "POST",
                f"/deals/{deal_id}/actions",
                token=tokens[acting_username],
                body=action_body,
            )
            if status != 204:
                log(
                    f"FATAL: {acting_username}'s {action_type}"
                    f"{f' {amount}' if amount is not None else ''} was rejected: {status} {resp}"
                )
                sys.exit(1)
            log(f"  {acting_username}: {action_type}{f' {amount}' if amount is not None else ''}")

    status, final_state = http("GET", f"/games/{game_id}")
    log("--- Final chip counts ---")
    for p in final_state["players"]:
        log(f"  {p['username']}: {p['chips']}")
    log(f"Completed {hands_completed}/{HANDS} hands, {total_actions} total actions.")

    if hands_completed == 0:
        log("FATAL: no hands completed at all.")
        sys.exit(1)

    log("Simulation finished successfully.")


if __name__ == "__main__":
    main()
