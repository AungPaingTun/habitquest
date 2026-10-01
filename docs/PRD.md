# HabitQuest: Product Requirements

| | |
|---|---|
| **Owner** | Aung Paing Tun |
| **Status** | v1 (private prototype), in development |
| **Last updated** | 2026-10-01 |

## 1. Problem

Building good habits is hard because the reward comes much later. You eat healthy today, but you don't feel the benefit for weeks. Most habit trackers only show checkmarks, which stop being motivating after a few days.

## 2. Solution

HabitQuest turns habits into a game you play against yourself:

> **Do a habit → earn points → spend points on a reward you chose yourself.**

The user sets the value of each habit and the price of each reward. "Eat healthy" might be worth 2 points a day, and "Hotpot dinner" might cost 40 points. Each good day brings you visibly closer to something you actually want, and the dashboard shows how your habits are trending over weeks, months and years.

## 3. Target user

- **v1:** one person tracking their own habits. Self-motivated, but loses interest when progress isn't visible.
- **v2:** the same person plus friends who can see each other's progress, like characters in a game.

## 4. Core loop

```
Create habits (with point values) ──► Check off habits each day ──► Earn points (+ streak bonus)
         ▲                                                                 │
         │                                                                 ▼
  See progress on the dashboard ◄──── Redeem a prize (points spent) ◄── Create prizes (with costs)
```

## 5. Rules

These rules are the source of truth. Code and tests must follow them.

### 5.1 Habits
| Rule | Detail |
|---|---|
| Fields | Name (required, ≤100 chars), icon (optional emoji), points per check-in (1–100) |
| Frequency | **Daily** (default), **N times per week** (1–7), or **N times per month** (1–31) |
| Date range | Start date (defaults to today) and optional end date (≥ start date). Example: "Run 2× per week during October" |
| Editing | Name, icon, points and dates can always be edited. **Changing points only affects future check-ins**; points already earned never change. **Frequency and target are locked after the first check-in**, so a streak is always measured against the rules it started with; to change them, archive the habit and create a new one. |
| Archive | Archiving hides a habit but keeps its history. Archived habits can be restored. There is no hard delete in v1. |
| Completed | Once the end date has passed, the habit shows as **completed** (separate from archived) with its final stats. No more check-ins. |
| Privacy | A user can only see and edit their own habits. |

### 5.2 Check-ins
| Rule | Detail |
|---|---|
| When | **Today only**, in the user's own time zone. A missed day is gone and can't be logged later. |
| How often | At most **once per day** per habit, for every habit type. **Progress counts days, not amount**: reading 40 pages instead of 20 still counts once. Weekly/monthly habits can't be checked in beyond their target for the period. |
| Undo | A check-in can be undone **on the same day only**. Its points (and any streak bonus) are reversed, as if never earned. **Blocked if those points were already spent**, i.e. the balance would go negative ("You've already spent these points"). |
| Active window | A habit can only be checked in between its start and end dates, and only if it isn't archived. |
| Periods | Weeks run **Monday–Sunday** (ISO). Months are calendar months. Both use the user's time zone. |

### 5.3 Points
| Rule | Detail |
|---|---|
| Earning | Each check-in earns the habit's points **as they were at check-in time** (stored with the check-in). |
| Ledger | Every change in points is a row in a ledger: earn, streak bonus, redeem, undo. The balance is the sum of the ledger. |
| Balance | Points available to spend. Never negative. |
| Lifetime XP | Total points ever earned. **Never goes down** when points are spent (only a same-day undo removes them). Determines the user's **level**. |
| Levels | Level *n* needs **50 × n²** lifetime XP, so each level takes longer: L1 = 50 · L2 = 200 · L3 = 450 · L5 = 1,250 · L10 = 5,000. Below 50 XP the user is Level 0. |

### 5.4 Streaks
Streak bonuses are **flat extra points, not multipliers**. They are easy to understand and can't grow out of control.

| Habit type | What counts as a streak | Bonus |
|---|---|---|
| Daily | Consecutive days checked in | 7+ days: **+2** per check-in · 30+ days: **+4** per check-in (cap) |
| Weekly / monthly | Consecutive periods where the target was met | When the target is reached during a streak of 3+ periods: **+2** · 8+ periods: **+4** (cap) |

- A missed day (or a missed period target) resets the streak to 0.
- The bonus amounts and thresholds live in one config class so they are easy to tune.
- The current streak and best streak are shown for every habit.

### 5.5 Prizes
| Rule | Detail |
|---|---|
| Fields | Name (required), icon (optional), cost in points (≥1) |
| Cost is locked | The cost **can never be changed** once the prize is created, so the goal doesn't move mid-challenge. To change it, archive the prize and create a new one. Name and icon can still be edited. |
| Redeem | Allowed only if balance ≥ cost. The cost is deducted from the balance (XP is not affected). |
| Repeatable | Prizes are never used up and can be redeemed again. Each prize shows its **redeem count** (e.g. "Hotpot ×2"). |
| Safety | Redeeming twice at the same moment (a double-click) can't spend the points twice. |
| Archive | Prizes can be archived like habits; redemption history is kept. |

## 6. Features by version

### v1: private prototype
| Area | Feature | Status |
|---|---|---|
| Accounts | Sign up, log in, log out (email + password, JWT) | ✅ Done |
| Habits | Create, list, edit, archive/restore (API) | ✅ Done |
| Habits | Habits page in the browser | ✅ Done |
| Check-ins | Check off today, same-day undo, points ledger | ✅ Done |
| Streaks | Current/best streak, flat bonuses | ✅ Done |
| Prizes | Create prizes, redeem, redeem count | ✅ Done |
| Dashboard | Today view: balance, level, streaks | ✅ Done |
| Analytics | Weekly / monthly / yearly: completion rate per habit, points earned vs spent, best streaks, most-redeemed prizes, year heatmap | Sprint 3 |
| Levels | XP → level | ✅ Done |
| Quality | Tests, CI, deployment to a custom domain | Sprint 4 |

### v2: public (after the prototype)
- Profile visibility: **private** (default) or **public**
- Public profile: display name, avatar, level, XP, streaks, chosen habits
- Follow friends; weekly leaderboards (by XP earned that week, not balance)
- Avatar/character cosmetics unlocked by level
- Login rate limiting and other hardening required before public launch

## 7. Out of scope (for now)
- Native mobile apps (the web app should work on phones)
- Reminders / push notifications
- Logging past days (backfill); this is deliberately not allowed
- Habits that take away points (e.g. "smoked a cigarette: −5")
- Social features in v1
- Payments or real-money rewards

## 8. Success measures (v1, personal use)
- I use it on **5+ days a week for 4 weeks** without losing interest
- At least **one prize redeemed** in the first month
- The analytics show a trend I didn't notice on my own

## 9. Decisions log
Questions that came up while planning, and what was decided (2026-10-01; #6 on 2026-10-02).

| # | Question | Decision | Why |
|---|---|---|---|
| 1 | What XP is needed for each level? | Level *n* needs 50 × n² XP (L1 = 50, L2 = 200, L3 = 450 …) | Early levels come fast to build motivation; later levels take months, so they mean something. |
| 2 | Points earned today were already spent on a prize. Can the check-in still be undone? | No. Undo is blocked. | The balance must never go negative. |
| 3 | What happens when a habit's end date passes? | It shows as **completed**, with its final stats. | A finished challenge is an achievement, not something to hide like an archived habit. |
| 4 | Can a prize's cost be edited? | No. It's locked once created; archive the prize and create a new one instead. | A goal shouldn't move during the challenge. |
| 5 | Can a habit be checked in more than once a day (e.g. 40 pages instead of 20)? | No. One check-in per day for every habit type. | Progress is measured in days of consistency, not in the amount done in one day. |
| 6 | Can a habit's frequency or target be changed after check-ins exist? | No. They're locked after the first check-in (name, icon, points and dates stay editable). | Otherwise old check-ins get recounted under new rules (e.g. daily → weekly turns 30 days into "met weeks" and pays a bonus at once). Same idea as the locked prize cost: no moving goalposts. |
