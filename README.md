# Tracker

A tiny health tracker for the Light Phone III — just a simple way to keep tabs on your water intake, sleep, steps, and (if it applies to you) your cycle, without any distractions.

This repo is built on Light's SDK scaffolding for Light Phone III tools, but at this point it's really just the Tracker app. The actual app code lives in [`tool/`](./tool) — see [`tool/README.md`](./tool/README.md) for the same rundown below, plus anything that's app-specific. SDK-level documentation (for anyone building their own separate tool on this scaffolding) is still in [`docs/`](./docs).

## What it does

- **Water** — log how much you're drinking today, in whatever unit you prefer
- **Sleep** — jot down how many hours of sleep you got each night
- **Steps** — track your daily step count
- **Cycle** *(optional)* — log period start/end dates, flow, mood, and energy, and see your next expected date on the home screen. Off by default — flip it on in Settings if it's useful to you
- **Forgot to log something?** Every entry screen lets you pick a date from a calendar, so you can backdate an entry instead of losing that day
- **History** — Water, Steps, and Sleep each have a history view showing last month's total and a yearly average; Cycle has its own history of past logged cycles
- **Settings** — flip to a light theme if you'd rather not stare at black-on-white all day (or vice versa), set your default water unit, turn Cycle tracking on/off, or reset your data

## Using it

Install the APK on your Light Phone III and you're done — no accounts, no setup, no companion app required. It just runs.