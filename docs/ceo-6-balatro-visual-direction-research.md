# CEO-6 Research: Balatro-like visual direction for Chess Roguelike

## 0) Executive summary

Target direction: **"Occult tabletop tactics"** — a dark, premium, highly readable board UI that mixes:

- Balatro’s punchy card-show framing and contrast discipline,
- Into the Breach-level tactical clarity,
- Darkest Dungeon / Inscryption mood cues (texture, ritual framing, dramatic lighting),
- mobile-first information hierarchy (one-thumb, low text burden, fast tactical parsing).

This should feel **stylized and memorable**, but never sacrifice core tactical readability.

---

## 1) Reference clusters

### A. Core mood and presentation language

#### A1. Balatro (primary mood anchor)
- https://store.steampowered.com/app/2379780/Balatro/
- Why relevant:
  - Strong color contrast and clear card silhouettes.
  - Premium-feeling retro treatment (CRT fuzz + pixel texture + neon accents) without losing legibility.
  - Excellent “reward anticipation” pacing (shop, blind reveal, score ramp).

#### A2. Inscryption (dark card-table narrative mood)
- https://store.steampowered.com/app/1092790/Inscryption/
- Why relevant:
  - Tabletop intimacy + ritualized interactions.
  - Controlled use of darkness/vignette to push attention to action center.

#### A3. Darkest Dungeon II (dark fantasy intensity and framing)
- https://store.steampowered.com/app/1940340/Darkest_Dungeon_II/
- Why relevant:
  - High-drama gothic framing, strong iconography language.
  - Good inspiration for danger/pain/debuff visual semantics.

#### A4. Wildfrost (stylized board+card readability)
- https://store.steampowered.com/app/1811990/Wildfrost/
- Why relevant:
  - Clear board lanes, compact tooltips, and status readability.
  - Friendly but disciplined UI hierarchy.

### B. Tactical board readability and intent communication

#### B1. Into the Breach (gold standard for tactical parse speed)
- https://store.steampowered.com/app/590380/Into_the_Breach/
- Why relevant:
  - Encodes intent, danger, and outcomes with very little noise.
  - Board remains readable under dense states.

#### B2. Fights in Tight Spaces (threat + action telegraphing)
- https://store.steampowered.com/app/1265820/Fights_in_Tight_Spaces/
- Why relevant:
  - Excellent target lines and planned-action readability.

#### B3. Tactical Breach Wizards (ability readability in dense tactical scenes)
- https://store.steampowered.com/app/1043810/Tactical_Breach_Wizards/
- Why relevant:
  - Great use of previews and constrained color language.

### C. Reward/shop/map cadence references

#### C1. Slay the Spire (run cadence + map/reward/shop rhythm)
- https://store.steampowered.com/app/646570/Slay_the_Spire/
- Why relevant:
  - Minimal UI, strong pacing between fights and rewards.

### C2. Monster Train (high-density upgrade/reward UI)
- https://store.steampowered.com/app/1102190/Monster_Train/
- Why relevant:
  - Handles many upgrades/statuses without total visual collapse.

### C3. Loop Hero (meta progression + dark retro atmosphere)
- https://store.steampowered.com/app/1282730/Loop_Hero/
- Why relevant:
  - Pixel/dither/retro cues still feel modern and premium.

## D. Mobile and touch-first readability references

### D1. Marvel Snap (mobile card readability and snap decisions)
- https://www.marvelsnap.com/
- Why relevant:
  - Aggressive clarity for tiny screens, punchy state changes, short turn loops.

### D2. Legends of Runeterra (keyword + tooltip ecosystem)
- https://playruneterra.com/
- Why relevant:
  - Good expandable info layers for novices and experts.

### D3. Hearthstone (feedback-driven turn transitions)
- https://hearthstone.blizzard.com/
- Why relevant:
  - Strong turn ownership signaling and satisfying feedback loops.

## E. Motion/polish and “premium juiciness” references

### E1. Balatro trailer / gameplay clips (high-impact low-duration feedback)
- https://www.youtube.com/@Playstack/videos

### E2. Inscryption trailer (camera rhythm + reveal pacing)
- https://www.youtube.com/@DevolverDigital/videos

### E3. Darkest Dungeon II trailers (FX restraint + dramatic punctuation)
- https://www.youtube.com/@RedHookStudios/videos

## F. UI pattern libraries for benchmarking (practical production help)

### F1. Game UI Database
- https://www.gameuidatabase.com/
- Why relevant:
  - Quickly compare menu/HUD/inventory/tooltip patterns by genre.

### F2. Interface in Game
- https://interfaceingame.com/
- Why relevant:
  - Searchable screenshot references for concrete layout decisions.

---

## 2) What makes these references work (concrete principles)

1. **Strong focal hierarchy**
   - One dominant zone (board/combat) + one secondary zone (status/actions) + quiet tertiary UI.
2. **Tight semantic color mapping**
   - Colors are reserved for meaning (danger, legal move, capture, buff/debuff), not decoration.
3. **Silhouette-first readability**
   - Units/cards readable by shape at small scale before details are visible.
4. **Information layering**
   - Immediate state visible without taps; advanced detail revealed on demand.
5. **Moment-based polish**
   - Most states are calm; animation/FX spike only on key moments (capture, reward reveal, elite clear).
6. **Text economy**
   - Single-line intent/objective and concise keywords outperform long prose.
7. **Consistent frame grammar**
   - Borders, corners, separators, and icon weights feel from one visual family.

---

## 3) What to borrow vs what to avoid

## Borrow
- Balatro’s **contrast discipline** and reward reveal cadence.
- Into the Breach’s **intent preview clarity**.
- Inscryption/DD2’s **dark tabletop drama** (lighting, material cues).
- Mobile CCGs’ **state readability at tiny sizes**.
- Wildfrost/STS’s **clean run flow screens** (map, reward, shop, choose-1).

## Avoid
- Over-texturing the board so overlays become muddy.
- Excessive chromatic/glow effects that make legal/capture/danger colors ambiguous.
- Animations that delay tactical input.
- Tiny serif-heavy text in active combat views.
- Copying Balatro’s exact motifs (joker iconography, specific card ornament style, exact palette recipe).

---

## 4) Proposed visual direction for this project

## Direction name
**Velvet Hex: Occult Tactics**

## Palette (initial)
- **Board base darks:** #131018, #1A1522
- **Cell separation lines:** #2A2336
- **Primary action accent (legal move):** #5DB0FF
- **Capture accent:** #FF6B6B
- **Danger/threat accent:** #F08A2B
- **Buff/protect accent:** #54D18A
- **Rare/reward accent (gold):** #D8B15B
- **Mythic/neon accent (very limited):** #B06CFF

Rule: keep simultaneous accent colors in-board to **max 3 at once**.

## Texture approach
- Subtle grain + velvet/paper microtexture on panels only.
- Board cells mostly clean matte with mild noise at low opacity.
- Optional CRT scanline pass only in menus/reward moments (not on high-precision board overlays).

## Typography mood
- Display font: sharp, occult-leaning serif for titles only.
- UI font: highly legible sans for all gameplay labels/tooltips.
- Numeric font: monospaced or tabular for HP, damage, counters.

## Icon shape language
- Geometric base (circle/diamond/shield) + ritual notch details.
- Fill-first icons at small sizes; avoid thin line-only symbols.
- Status icons use fixed silhouettes and color-coded rings.

## Frame/border treatment
- 2-layer frames: dark base plate + subtle metallic trim.
- Corner motifs only on high-value panels (objective, reward cards, elite warnings).
- Keep combat HUD border weight low to preserve board dominance.

## VFX restraint level
- Idle ambience: low (dust motes, faint vignette breathing).
- Tactical actions: medium (quick flashes, directional streaks).
- Milestone events (room clear, relic acquisition): high but short (<800ms highlight burst).

---

## 5) UI recommendations (mobile board tactics first)

1. **Top center: Objective chip + turn owner banner**
   - Example: `PLAYER TURN · Survive 3 turns`.
2. **Board center dominates 60–70% vertical space.**
3. **Bottom dock (thumb zone):** selected piece, 2–4 actions, End Turn.
4. **Threat toggle:** one-tap mode that overlays enemy intent lines and danger tiles.
5. **Overlay priority stack:**
   - Selected piece ring > legal move tiles > capture tiles > danger heat > objective tiles.
6. **Color + pattern redundancy:**
   - Danger = orange + hatch pattern;
   - Capture = red + pulse edge;
   - Legal move = blue + soft fill.
7. **Tooltip behavior:**
   - Tap-hold for details; single-tap remains action-first.
8. **State compression:**
   - Replace verbose logs with compact event toasts (`Bishop stunned`, `Relic charged`).
9. **Reward screen:**
   - Show “before → after” preview with immediate tactical implication.
10. **Performance budget for clarity:**
   - Prioritize 60fps board interactions over decorative particles.

---

## 6) Asset production implications

If this direction is chosen, production should plan these asset categories:

1. **Board system assets**
   - Cell themes (normal/objective/hazard), overlay atlases, intent arrows, reticles.
2. **Piece readability set**
   - Silhouette-first sprite sets with distinct base poses and team tint masks.
3. **UI frame kit**
   - Modular panels, headers, chips, tabs, rarity borders.
4. **Icon library**
   - Status, damage types, intents, rewards, currencies, room types.
5. **FX package**
   - Capture bursts, turn transition stingers, reward reveals, relic acquisition pulses.
6. **Typography package**
   - Display + UI + numeric fonts and localization-safe fallback stack.
7. **Shader/material set**
   - Grain/vignette/foil/scanline controls with runtime intensity toggles.
8. **Audio UI set (critical for feel)**
   - Select/move/capture/reward/turn-change signatures aligned with visual events.

Suggested indie-friendly workflow:
- Build a **small UI token system** first (spacing, corner radius, frame thickness, glow intensity).
- Produce one **vertical slice room** fully polished before scaling globally.

---

## 7) Priority shortlist (top 10 references for immediate redesign)

1. **Balatro (Steam page + trailers)** – contrast, premium retro texture, reward cadence.  
   https://store.steampowered.com/app/2379780/Balatro/
2. **Into the Breach** – tactical intent readability model.  
   https://store.steampowered.com/app/590380/Into_the_Breach/
3. **Inscryption** – occult tabletop mood and center-stage framing.  
   https://store.steampowered.com/app/1092790/Inscryption/
4. **Wildfrost** – compact yet readable board/card presentation.  
   https://store.steampowered.com/app/1811990/Wildfrost/
5. **Slay the Spire** – map/reward/shop pacing grammar.  
   https://store.steampowered.com/app/646570/Slay_the_Spire/
6. **Darkest Dungeon II** – dark fantasy iconography and dramatic punctuation.  
   https://store.steampowered.com/app/1940340/Darkest_Dungeon_II/
7. **Marvel Snap** – mobile-first interaction economy and state clarity.  
   https://www.marvelsnap.com/
8. **Legends of Runeterra** – layered tooltip/keyword readability.  
   https://playruneterra.com/
9. **Fights in Tight Spaces** – threat lines and action forecast legibility.  
   https://store.steampowered.com/app/1265820/Fights_in_Tight_Spaces/
10. **Game UI Database** – practical HUD/menu pattern comparison for implementation.  
   https://www.gameuidatabase.com/

---

## 8) Immediate prototype plan (recommended next step)

1. **Prototype A: Board readability only**
   - Implement tile language + threat overlay + turn banner with zero decorative FX.
2. **Prototype B: Reward reveal moment**
   - Implement one “choose 1 of 3” reward panel with before/after preview and short reveal animation.
3. **Prototype C: Menu and run HUD polish pass**
   - Apply frame/texture/typography system without changing mechanics.

Success check for all three: new players should parse legal moves, danger, and turn ownership within **10 seconds** on a phone-sized screen.
