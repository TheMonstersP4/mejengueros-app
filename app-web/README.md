# Mejengueros Web

React public site for Mejengueros. The app recreates the approved poster/cancha design handoff with Tailwind CSS, reusable landing sections, and a small test suite.

## Routes

- `/`: product landing page for Mejengueros.
- `/team`: public landing page for The Monsters team.

## Scripts

```bash
npm install
npm run dev
npm test
npm run build
```

## Structure

- `src/pages/landing`: product landing page composition and tests.
- `src/pages/team`: team landing page composition and tests.
- `src/features/landing`: product-specific landing sections and content.
- `src/features/team`: team-specific landing content.
- `src/shared`: reusable UI primitives and global styles.

## Design

The landing uses the dark Mejengueros palette, lime accent, Anton display typography, Archivo Narrow body typography, and a CSS-only cancha background inspired by the handoff.
