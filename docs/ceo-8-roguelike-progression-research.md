# CEO-8 Research: Roguelike progression expansion for Chess Roguelike

Date: 2026-04-17

## 0) Snapshot of current baseline (why expansion is needed)

From current project content/runtime:
- The run is currently short and linear (`maxRounds: 5`) with one repeated loop: battle -> pick one upgrade -> next round.
- Upgrade effect types are currently limited to three buckets: `add_piece`, `add_ability`, `heal`.
- Ability catalog is currently narrow (`DOUBLE_MOVE`, `SHIELD`, `EXPLOSION`, `EXTRA_RANGE`, `NONE`).
- Build identity is mostly additive power accumulation (more pieces + occasional ability assignment), with limited long-term tradeoff systems.

This confirms the issue statement: the game has a solid tactical baseline, but low run-level expressive variety.

---

## 1) Reference categories (with practical lessons)

## A. Node-map deckbuilder progression (Slay the Spire family)
Key references:
- Slay the Spire wiki.gg: Map Locations
  - https://slaythespire.wiki.gg/wiki/Map_Locations
- Slay the Spire wiki.gg: Map Generation
  - https://slaythespire.wiki.gg/wiki/Map_Generation
- Slay the Spire wiki.gg: Relics
  - https://slaythespire.wiki.gg/wiki/Relics

What matters for this project:
- **Pathing is a build decision**, not just travel.
- **Room-type cadence** (combat, elite, rest, shop, unknown) creates medium-term planning tension.
- **Relic-like passive layer** creates compounding identity with minimal UI overhead.
- **Unknown/event rooms** inject risk/reward spikes and reduce run determinism.

## B. Telegraphed tactics clarity (Into the Breach family)
Key references:
- Game Developer interview (Road to the IGF):
  - https://www.gamedeveloper.com/game-platforms/road-to-the-igf-subset-games-i-into-the-breach-i-

What matters:
- Showing enemy intent can preserve tactical depth while increasing fairness and speed of comprehension.
- “Perfect play under pressure” is compelling when consequences are explicit.
- This is highly compatible with mobile constraints (quick parse, low ambiguity).

## C. Reward-pool quality and “play the hand you’re dealt” (Against the Storm family)
Key references:
- Official wiki: Updates & Devlog
  - https://wiki.hoodedhorse.com/Against_the_Storm/Updates_and_Devlog
- Eremite 1.6 update note (cornerstone/reward-pool rebalance philosophy)
  - https://eremitegames.com/elder-update-1-6/

What matters:
- Roguelike depth is not only “more content”; it is also **better offer quality and offer context**.
- Maintaining run adaptation requires reducing dead/irrelevant rolls.
- Reward systems should preserve uncertainty while minimizing meaningless picks.

## D. Multi-axis growth in compact runs (Monster Train family)
Key references:
- Monster Train Wiki (artifacts overview)
  - https://monster-train.fandom.com/wiki/Artifacts

What matters:
- Strong runs combine multiple progression layers simultaneously (core pieces, tactical actions, passives, economy constraints).
- Choosing among these layers each node creates strong “build steering” every few minutes.

---

## 2) Key lessons translated to a mobile-friendly chess roguelike

1. **Every room should ask one strategic question**
   - “Do I improve board control now, or scale my combo engine later?”
2. **Compounding passives should do most identity work**
   - Easy to parse, low input burden, high replay value.
3. **Pathing should be short but meaningful**
   - 2-3 branches per step is enough on mobile.
4. **Offer relevance is crucial**
   - Reward choices should key off current build tags (piece-heavy, ability-heavy, economy-heavy).
5. **Telegraphing + rapid feedback enables tactical tension without real-time complexity**
   - Preserve turn-based chess logic, add immediacy through presentation/response.

---

## 3) Expanded design space: stats / builds / choices

Below is a broad menu of candidate systems.

## A. Player-facing stat families (run stats)
Core-safe:
- **Command Points (CP)**: spend on tactical abilities each turn.
- **Draw / Offer Quality**: increases chance of seeing synergy-tagged rewards.
- **Threat Buffer (HP/Integrity)**: run fail-state margin.
- **Tempo**: bonus for ending turns efficiently (e.g., unused CP conversion).

Promising:
- **Initiative**: chance to act first in certain encounters.
- **Zone Control**: buffs tied to controlling marked tiles.
- **Combo Meter**: rewards sequence play (capture -> reposition -> ability).

Experimental:
- **Forecast tokens**: limited peeks/rerolls of enemy intentions or upcoming reward pools.

## B. Piece progression concepts
Core-safe:
- Piece rank tiers (I/II/III) with one branch per promotion.
- Piece role augments (Vanguard, Sniper, Defender, Trickster).
- Piece-specific passive slots (1–2 per run).

Promising:
- Bond links between two pieces (shared buffs, assist triggers).
- Capture evolutions (piece mutates after N captures).

Experimental:
- Positional auras on pieces that reshape nearby move legality.

## C. Tactical ability (card-like) layer
Core-safe:
- Small tactical deck (8–12 cards), draw 3 per player turn.
- 0–2 CP costs, simple effects: push, shield, pin, warp, mark.
- Exhaust/retire mechanics for powerful one-shots.

Promising:
- Card tags: `mobility`, `control`, `burst`, `economy`, `setup`.
- “If piece type X acted this turn” conditional upgrades.

Experimental:
- Reaction cards (play during enemy turn at strict windows).

## D. Passive relic/artifact categories
Core-safe:
- Opening relics (first two turns).
- Capture relics (on kill triggers).
- Position relics (bonus on center files, back rank, diagonals).

Promising:
- Route relics (extra reward if path chooses elite/event).
- Economy relics (shop discount, reroll refunds).

Experimental:
- Relics that alter board topology rules (temporary blockers/portals).

## E. Economy / risk-reward systems
Core-safe:
- Gold + one premium reroll currency.
- Shop + shrine + event nodes.
- “Take a curse for stronger reward” offers.

Promising:
- Debt relics (gain power now, penalty if objective missed later).
- Insurance mechanics (pay now to reduce loss on defeat).

Experimental:
- Auction rooms where you bid future penalties for current power.

## F. Event / shrine / curse menu examples
Core-safe:
- Trade max HP for rare relic.
- Remove one weak card, gain one random uncommon.
- Sacrifice a pawn to empower chosen piece.

Promising:
- Locked chest requiring specific tactical challenge in next room.
- Temporal curse: “enemy acts twice on turn 3” for elite-tier reward.

Experimental:
- Symmetry inversion event (board mirrored next fight).

---

## 4) Candidate progression models (3-5)

## Model 1 — Piece-as-Deck (recommended baseline)
**What player collects:** piece upgrade chips, promotion branches, piece passives.

**What persists during a fight:** piece stats/passives + limited active abilities attached to pieces.

**What changes between rooms:** promotions, role swaps, passive slots, occasional recruit piece.

**Build identity driver:** roster composition + promoted role distribution.

**Fit:** Very high. Feels chess-native. Low cognitive overhead.

**Risk:** Can become stat-checky unless tactical ability layer is present.

---

## Model 2 — Tactical Ability Card Model
**What player collects:** cards/spells, card upgrades, draw/energy modifiers.

**Persists during fight:** hand/deck/discard + board state.

**Between rooms:** add/remove/upgrade cards, gain relics modifying card rules.

**Identity driver:** card engine + piece synergies.

**Fit:** High, if deck size remains compact for mobile.

**Risk:** Can overshadow chess if cards dominate movement decisions.

---

## Model 3 — Relic-Centric Build Model
**What player collects:** mostly passive artifacts, fewer explicit cards/piece changes.

**Persists during fight:** many automatic modifiers.

**Between rooms:** choose/forge/relic-combine.

**Identity driver:** passive trigger web.

**Fit:** Medium-high for mobile readability.

**Risk:** Hidden math and low intentionality unless triggers are clearly surfaced.

---

## Model 4 — Squad Growth + Objective Rooms
**What player collects:** squad members with role kits, objective modifiers, supply resources.

**Persists:** squad health/state + mission-scoped perks.

**Between rooms:** recruit/bench, recover, objective-route choices.

**Identity driver:** team comp + objective handling style.

**Fit:** Medium. Strong tactical fantasy, but heavier production scope.

**Risk:** complexity and UI burden for near-term demo.

---

## Model 5 — Hybrid Chess + Cards + Artifacts (best long-term target)
**What player collects:** promoted pieces, small tactical deck, relics, economy tech.

**Persists:** all three layers in constrained form.

**Between rooms:** node pathing with 2-3 branches + targeted reward pools.

**Identity driver:** cross-layer synergies (piece tag x card tag x relic trigger).

**Fit:** Very high for “Slay the Spire-like progression mindset” while keeping chess core.

**Risk:** requires careful scope gates and good content tagging pipeline.

---

## 5) Light action layer research: keep tactics, add energy

Goal: more immediacy without converting to real-time action.

## Safe options
- **Snappier resolution cadence**: fast animation defaults with optional speed toggle.
- **Input buffering**: preselect move while animation resolves.
- **Intent pulse windows**: brief highlight pulses to emphasize danger/action order.
- **Micro-confirm reactions**: one optional trigger window for specific cards (e.g., parry).

## Promising options
- **Perfect timing bonus (tiny)**
  - Example: tap-to-confirm during a small timing window grants +1 shield or +1 CP.
  - Must be optional and low impact to preserve tactical fairness/accessibility.

- **Between-turn interstitial actions**
  - Spend a scarce token between enemy telegraphs to rotate one piece direction or guard tile.

## Avoid
- Twitch-heavy dodge systems.
- Real-time enemy movement while player plans.
- Mechanics that hide board consequences behind animation speed.

---

## 6) Design space map by risk band

## Safe / core (for next demo)
- 2-3 branch node map.
- 4 room types: battle / elite / shop / event.
- Small relic catalog with explicit trigger text.
- Piece promotion branch (one choice per piece tier).
- Compact tactical deck (starter 6-8 cards).

## Experimental but promising
- Bonded-piece synergies.
- Debt/curse economy contracts.
- Reaction windows.
- Objective-modifier rooms (survive, escort, hold zone).

## Likely too complex for near-term demo
- Full squad bench management.
- Heavy procedural board topology mutations.
- Deep reaction chains with stack-resolution rules.
- Multi-currency macro systems with crafting trees.

---

## 7) Recommended near-term prototype direction

Recommend **Model 5 in constrained form** (hybrid), with strict MVP limits:

### MVP Layer A: Chess roster identity
- Start each run with Hero King + 2 core pieces.
- Promotions at fixed checkpoints (after rooms 2/5/8).

### MVP Layer B: Tactical card mini-deck
- Deck size: 8 starter, cap 14.
- Reward cadence: after each non-event combat choose 1 of 3 card offers.

### MVP Layer C: Relics/passives
- 1 starting relic + up to 5 run relics.
- Each relic tied to one clear trigger family.

### MVP Layer D: Node map cadence
- 10-room act prototype.
- Node choice each step: 2 options only (mobile-first clarity).
- Include elite tension and one guaranteed shop before boss.

### MVP Layer E: Offer quality logic
- Tag every reward with synergy tags.
- Simple weighted reroll: +weight for tags matching existing run profile.

This delivers meaningful build arcs in a short session and should make room-to-room choices feel distinctly roguelike by room 3.

---

## 8) Mechanics to avoid for now

- Large deck sizes (>20) on mobile first prototype.
- Overlapping hidden modifiers without UI callouts.
- Too many parallel resources (gold + shards + essence + favors + keys etc.).
- High variance events that permanently brick runs early.
- Action-timing mechanics that meaningfully gate success.

---

## 9) Priority shortlist: first prototypes to implement

1. **Node map v1** (battle/elite/shop/event; 2-way branching).
2. **Relic system v1** (15-20 passives; explicit trigger text).
3. **Card mini-deck v1** (10-14 cards, low-complexity effects).
4. **Piece promotion v1** (3 pieces, 2 branches each).
5. **Synergy-tagged reward algorithm** (reduce dead offers).
6. **Event room pack v1** (8-12 events with clear risk/reward).
7. **Intent + threat UI polish** (telegraph clarity + fast readability).

---

## 10) Suggested implementation sequencing (aligned with CEO-7 dependency)

1. Finalize rule integrity baseline (CEO-7).
2. Add progression scaffolding data model (node map, relics, cards).
3. Ship minimal hybrid run loop (10 rooms).
4. Add reward-quality weighting and analytics hooks.
5. Expand content breadth only after retention/readability checks.

---

## 11) Success metrics for this direction

- By room 3, players can describe their build in one sentence.
- >70% of players choose different path types across first 3 runs.
- Event/shop/elite pick rates remain meaningfully distributed (no dominant node).
- Early quit rate decreases after adding map + identity layers.
- Average run length stays mobile-friendly while decision density increases.
