# Tracker

A health tracker for the Light Phone III. Just a simple way to keep tabs on your water intake, sleep, movement, and more, without any distractions.

This repo is built on Light's SDK scaffolding for Light Phone III tools, but at this point it's really just the Tracker tool. The actual tool code lives in [`tool/`](./tool) — see [`tool/README.md`](./tool/README.md) for the same rundown below, plus anything that's tool-specific. SDK-level documentation (for anyone building their own separate tool on this scaffolding) is still in [`docs/`](./docs).

## What it does

- **Water**: log how much you're drinking today, in whatever unit you prefer

- **Sleep**: log the exact date and time you fell asleep, and the date and time you woke up (e.g. sleep 07/07/26 11:30 PM, wake 07/08/26 7:00 AM). It works out the hours for you. You can log bedtime and wake time separately too. Save just the time you went to bed, then the next morning, add when you woke up

- **Movement**: Track however you actually move from Steps, Laps, Distance, or Time, independently of each other, each with its own weekly and monthly totals. Pick which one shows on the Home screen, and turn off any categories you don't use

- **Cycle**: log period start/end dates, and log Flow, Energy, and Mood for each individual day of the cycle, plus see your next expected date on the home screen.  

- **Weight**: log a starting weight and keep logging your current weight over time. It shows your average change per week as one neutral number (no separate "loss" or "gain" framing).

- **Mood**: log how you're feeling (pick up to 5 from a categorized list) plus a short note; backdateable like everything else. The form clears after each save, so logging a second, different mood later the same day is just as easy as the first. If you also track Cycle, Cycle's own mood field automatically links here instead of asking you to log twice

- **Forgot to log something?** Every entry screen is backdateable
- **History**: Water, Movement, and Sleep each have a history view showing the previous month's total and a yearly average; Cycle, Weight, and Mood each have their own history of past entries, with the option to delete any entry you didn't mean to log
- **Settings**:  flip to a light theme if you'd rather not stare at black-on-white all day (or vice versa), enable/disable any of the metrics or Movement Type in the submenu that you do not use, set your default Units of measurement and date/time formats under **Units & Formats** or reset your data tool-wide. Currently the app loads in with Water, Movement and Sleep by default.

## Using it

Install the APK on your Light Phone III.

## Screenshots

<table>
<tr>
<td><img src="tool/screenshots/home_screen.png" width="200" alt="Home screen"><br><sub>Home</sub></td>
<td><img src="tool/screenshots/home_screen_2.png" width="200" alt="Home screen with Cycle tile"><br><sub>Home (Cycle enabled)</sub></td>
<td><img src="tool/screenshots/water_screen.png" width="200" alt="Water screen"><br><sub>Water</sub></td>
</tr>
<tr>
<td><img src="tool/screenshots/water_history.png" width="200" alt="Water history screen"><br><sub>Water history</sub></td>
<td><img src="tool/screenshots/mood_screen.png" width="200" alt="Mood screen"><br><sub>Mood</sub></td>
<td><img src="tool/screenshots/sleep_screen.png" width="200" alt="Sleep screen"><br><sub>Sleep</sub></td>
</tr>
</table>
