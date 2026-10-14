# Adversarial Code & Architecture Review: SvelteKit PWA (v1.4.0)

**Date of Review:** October 13, 2026  
**Commits Evaluated:**
- `39d60dd` — *Update F-Droid screenshots for the new design*
- `f9ada9e` — *Bump Kotlin to 2.2.21, Svelte to 5.57.2 and Vite to 7.3.7*
- `100120b` — *Redesign UI and add About page*
- `ae34ae5` — *Bump to v1.4.0*

---

## Executive Summary

The SvelteKit web application received a substantial visual overhaul aligning its UI structure with the Android experience (accordion cards, gradient hero card, stat strip, uniform detail tiles, and an About page). However, an adversarial inspection reveals **visual breaking bugs on desktop viewports, SSR hydration mismatch vulnerabilities, array corruption hazards in modal lifecycle management, localized unit inconsistencies, and widespread violations of repository coding constraints**.

---

## 1. High Severity Visual & Layout Bugs

### 1.1 Desktop App Shell Breakout in `SettingsPage.svelte`
- **Location:** [`web/src/lib/components/SettingsPage.svelte:45-57`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/SettingsPage.svelte#L45-L57) vs [`web/src/routes/+layout.svelte:124-145`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/routes/+layout.svelte#L124-L145)
- **Code:**
  ```css
  /* SettingsPage.svelte */
  .page {
      position: fixed;
      inset: 0;
      background: linear-gradient(to bottom, #0E0B3D, #1A1565);
      isolation: isolate;
      color: white;
      overflow-y: auto;
      z-index: 100;
      ...
  }
  ```
  ```css
  /* +layout.svelte */
  @media (min-width: 1024px) {
      .stage {
          width: 786px;
          max-width: 786px;
          height: 1024px;
          max-height: 1024px;
          border-radius: 12px;
          box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.06), 0 25px 60px rgba(0, 0, 0, 0.6);
      }
  }
  ```
- **Finding:** On desktop viewports (width >= 1024px), `+layout.svelte` constrains and centers the app inside a phone-style frame (`.stage` 786×1024px). However, `SettingsPage.svelte` styles `.page` with `position: fixed; inset: 0;`.
- **Impact:** Neither `.stage` nor `.app-shell` defines a containing block for fixed positioning (e.g., via `transform`, `perspective`, or `contain: paint`). Consequently, whenever the user opens Settings or About on desktop, the page **breaks completely out of the centered mobile frame and expands across 100% of the user's desktop monitor (1920×1080)**. When the user closes Settings/About, the UI snaps back into the 786px centered container. This is a severe visual regression.
- **Fix:** `.page` should use `position: absolute; inset: 0;` within `.app-shell` (or `.app-shell` must establish a fixed containing block via `transform: translateZ(0)`).

---

## 2. SSR & Runtime Architecture Issues

### 2.1 SSR Hydration Mismatch via Module-Scoped Counter in `CollapsibleSection`
- **Location:** [`web/src/lib/components/CollapsibleSection.svelte:1-3, 22`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/CollapsibleSection.svelte#L1-L3)
- **Code:**
  ```svelte
  <script module lang="ts">
      let idCounter = 0;
  </script>

  <script lang="ts">
      ...
      let contentId = `content-${idCounter++}`;
  ```
- **Finding:** The application runs in production on Node.js via `@sveltejs/adapter-node` with Server-Side Rendering (SSR). `<script module>` variables live in server memory for the lifetime of the Node process.
- **Impact:** On the server, `idCounter` increments continuously across consecutive HTTP requests (e.g. `content-120`, `content-121`). When the HTML reaches the client browser, client hydration executes with a fresh client module instance where `idCounter` begins at `0` (`content-0`, `content-1`). This causes immediate **DOM hydration attribute mismatch warnings/errors on `id` and `aria-controls`**, violating SvelteKit SSR best practices.
> **Correction (verified 2026-10-13):** Not a v1.4.0 regression: the module-level `idCounter` is unchanged from `1f15368`. Still a legitimate SSR concern worth a separate look.

- **Fix:** Use Svelte 5's `$props()` id parameter or SvelteKit's deterministic ID generation/context.

### 2.2 Stack Corruption Risk in Modal Escape Key Management
- **Location:** [`web/src/lib/components/SettingsPage.svelte:1-4, 20-23`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/SettingsPage.svelte#L1-L4)
- **Code:**
  ```ts
  const id = Symbol();
  onMount(() => {
      openPages.push(id);
      return () => openPages.splice(openPages.indexOf(id), 1);
  });
  ```
- **Finding:** When unmounting, `openPages.splice(openPages.indexOf(id), 1)` searches for `id`. If `indexOf(id)` returns `-1` (which occurs if unmount ordering or multiple teardowns desynchronize the array), `splice(-1, 1)` **removes the last element in the array** instead of doing nothing.
- **Impact:** An unrelated, still-active page ID at the top of the stack is deleted, corrupting the Escape-key navigation chain for remaining open pages.
- **Fix:** Guard the splice operation:
  ```ts
  const idx = openPages.indexOf(id);
  if (idx !== -1) openPages.splice(idx, 1);
  ```

---

## 3. Strict Coding Constraint Violations

The project configuration defines unambiguous non-negotiable rules:
> - **Style:** Always strive for a single return statement in all methods.
> - **Constraints:** Avoid breaks, continues, and early returns at all costs.

Today's commits introduced widespread violations of single-return and early-return rules across TypeScript and Svelte components:

| File | Lines | Violation Description |
|---|---|---|
| [`web/src/lib/components/WeatherScreen.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/WeatherScreen.svelte#L187-L195) | 187–195 | `getUvLabel()` contains **6 separate return statements**. |
| [`web/src/lib/components/CurrentConditions.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/CurrentConditions.svelte#L22-L30) | 22–30 | `getUvLabel()` contains **6 separate return statements**. |
| [`web/src/lib/components/WeatherScreen.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/WeatherScreen.svelte#L48-L52) | 48–52 | `findBrowserLocaleTag()` uses early `return null` and fallback return. |
| [`web/src/lib/components/WeatherScreen.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/WeatherScreen.svelte#L248-L251) | 249 | `handleShare()` uses early return: `if (!headerEl) return;`. |
| [`web/src/lib/components/SavedLocationsDialog.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/SavedLocationsDialog.svelte#L16-L19) | 18 | `handleInput()` uses early `return;` inside debounce callback. |
| [`web/src/lib/components/CurrentConditions.svelte`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/CurrentConditions.svelte#L32-L39) | 33 | `visibilityDisplay()` uses early `return ['--', '--'];`. |
| [`web/src/lib/utils/date.ts`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/utils/date.ts#L34-L45) | 34, 45 | `formatDate()` contains multiple returns. |

> **Correction (verified 2026-10-13):** These rules are not part of this repository's configuration. They come from a personal global agent config (`~/.gemini/GEMINI.md`); the repo's `AGENTS.md`, `CLAUDE.md` and `GEMINI.md` do not contain them. Most listed functions also predate v1.4.0 (both `getUvLabel` copies, `formatDate`, `visibilityDisplay`), so they were not "introduced today". Treat as a style preference, not a violation.

---

## 4. Cross-Platform Parity Gaps & Inconsistencies

### 4.1 Header Date Formatting Disparity & Overflow
- **Location:** [`web/src/lib/components/WeatherScreen.svelte:352-355`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/WeatherScreen.svelte#L352-L355) vs [`app/src/main/java/.../WeatherContent.kt:371-372`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/app/src/main/java/orinasa/njarasoa/maripanatokana/ui/weather/WeatherContent.kt#L371-L372)
- **Finding:**
  - Android formats non-today timestamps concisely: `dateFormat.format(Date(data.timestamp)), screenTimeFormat.format(...)` -> `"Mon 12 Oct, 14:30"`.
  - Web uses `formatDate(data.timestamp, ...)` which emits full long dates: `"weekday, day month year hours:minutes"` (e.g. `"Monday, October 12, 2026 14:30"`).
- **Impact:** The resulting string in web is twice as long as on Android. In languages with long month names (e.g. Spanish, French, Malagasy), this causes the updated button to line-wrap and crowd out header action icons.

### 4.2 Hardcoded Unit Strings in `CurrentConditions.svelte`
- **Location:** [`web/src/lib/components/CurrentConditions.svelte:37-38`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/CurrentConditions.svelte#L37-L38)
- **Code:**
  ```ts
  return metric
      ? [`${km} km`, `${mi} mi`]
      : [`${mi} mi`, `${km} km`];
  ```
> **Correction (verified 2026-10-13):** Pre-existing (unchanged from `1f15368`), and the Android templates are `"%.1f km"` / `"%.2f mi"` in every locale, so there are no translated unit labels to adopt. Low value.

- **Finding:** Android uses translated string templates from `strings.xml` (`R.string.visibility_km` and `R.string.visibility_mi`). Web hardcodes literal ASCII `' km'` and `' mi'`, ignoring localized unit labels or bidirectional formatting requirements.

### 4.3 Version Resolution Divergence
- **Location:** [`web/src/lib/components/AboutScreen.svelte:3`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/AboutScreen.svelte#L3) vs [`web/src/lib/components/Footer.svelte:35`](file:///home/tahiry/data/code/mpilasy/maripanaTokana/web/src/lib/components/Footer.svelte#L35)
- **Finding:** `AboutScreen.svelte` dynamically imports `{ version } from '../../../package.json'`, whereas `Footer.svelte` hardcodes `<span class="version">v1.4.0</span>` and relies on `scripts/release_fdroid.sh` regex replacements. If a developer bumps `package.json` manually without running the release script, `Footer.svelte` will display a stale version while `AboutScreen.svelte` displays the new one.

> **Correction (verified 2026-10-13):** By design. The hard-coded footer version is the string `scripts/check_version_sync.py` checks (CI "Version Sync Check"), so a manual `package.json` bump without updating the footer fails CI. Not a bug.

---

## 5. Verification Checklist & Remediation Steps

1. **Contain `SettingsPage` within Desktop Shell**:
   - Change `.page` in `SettingsPage.svelte` from `position: fixed; inset: 0;` to `position: absolute; inset: 0;`.
   - Ensure `.app-shell` in `+layout.svelte` has `position: relative; overflow: hidden;` to constrain all page transitions within the desktop stage.
2. **Fix SSR ID Generation**:
   - Refactor `CollapsibleSection.svelte` to eliminate module-scoped state (`idCounter`). Use component-scoped IDs or explicit prop IDs.
3. **Guard Stack Deletion in `SettingsPage`**:
   - Guard `openPages.indexOf(id)` against `-1` before calling `splice()`.
4. **Enforce Single Return Principle**:
   - Refactor multi-return functions (such as `getUvLabel` in `WeatherScreen.svelte` and `CurrentConditions.svelte`) to calculate the index and return once from an array:
     ```ts
     function getUvLabel(uv: number): string {
         const labels = $json('uv_labels') as string[];
         let result = '';
         if (Array.isArray(labels)) {
             const idx = uv < 3 ? 0 : uv < 6 ? 1 : uv < 8 ? 2 : uv < 11 ? 3 : 4;
             result = labels[idx] ?? '';
         }
         return result;
     }
     ```
5. **Sync Date & Visibility Formatting with Android**:
   - Update `WeatherScreen.svelte` to use a compact `formatShortDate` when timestamp is not today.
   - Use `$_('visibility_km')` and `$_('visibility_mi')` in `visibilityDisplay()`.

---

## 6. Triage Status (verified 2026-10-13)

| Finding | Verdict | Status |
|---|---|---|
| 1.1 Settings/About break out of desktop frame | Valid (pre-existing in old Settings too) | **Fixed** — `.page` in `SettingsPage.svelte` is `position: absolute` inside `.app-shell` |
| 2.1 SSR `idCounter` hydration mismatch | Pre-existing; plausible | Open — separate look |
| 2.2 `splice(-1)` in page stack cleanup | Valid (theoretical) | **Fixed** — index checked before `splice` |
| 3 Single-return / early-return rule | Not a repo rule (personal `~/.gemini/GEMINI.md`); mostly pre-existing | No action |
| 4.1 Long header date when data is not from today | Valid parity gap | **Fixed** — new `formatShortDate()` in `utils/date.ts` (matches Android `"EEE d MMM"`, array fallback for `mg`/`ne`); header shows `"<short date>, HH:mm"`; 2 new vitest cases |
| 4.2 Hard-coded `km`/`mi` | Pre-existing; Android has no translated units either | No action |
| 4.3 Footer version vs `package.json` | By design (checked by `check_version_sync.py`) | No action |

**Found while fixing 1.1 (same class) — fixed:**
- `SavedLocationsDialog.svelte` and `AirQualityDetailDialog.svelte` used `position: fixed` scrims that covered the whole browser window on desktop. Fixed once for every fixed-position overlay: `.app-shell` in `+layout.svelte` now has `contain: layout`, making the app frame the containing block for `position: fixed` descendants (no visual change on mobile, where the frame fills the viewport).
- `AirQualityDetailDialog` was also rendered inside `.scroll-area`, whose `mask-image` fade (added in v1.4.0) clipped the dialog's scrim to the scroll area — the header stayed undimmed and on mobile the dialog sat under the header. It is now rendered after the footer, outside the scroll area.

Verification: `npm run check` 0 errors / 0 warnings; `npx vitest run` 52/52; `npm run build` OK. Playwright: Settings, About, the location search dialog and the air-quality dialog stay inside the centered 786px frame at 1440×1000 (scrim dims the whole frame, header included) and fill the screen at 412×915.
