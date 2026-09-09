package vendredi.soir.karata.core.action;

import lombok.NoArgsConstructor;

/**
 * A contentless phase-boundary marker for variants with no shared board (so no RevealCards action
 * to bound a betting round against) - e.g. Five-Card Draw's transition from the pre-draw betting
 * round into the draw phase, and again from the draw phase into the post-draw betting round. {@link
 * vendredi.soir.karata.core.entity.Deal#getActionsInCurrentPhase} treats this exactly like a
 * RevealCards boundary.
 */
@NoArgsConstructor
public final class AdvancePhase implements DealerAction {}
