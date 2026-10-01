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
| Editing | All fields can be edited. **Changing points only affects future check-ins**; points already earned never change. |
| Archive | Archiving hides a habit but keeps its history. Archived habits can be restored. There is no hard delete in v1. |
| Privacy | A user can only see and edit their own habits. |

### 5.2 Check-ins
| Rule | Detail |
|---|---|
| When | **Today only**, in the user's own time zone. A missed day is gone and can't be logged later. |
| How often | At most **once per day** per habit. Weekly/monthly habits can't be checked in beyond their target for the period. |
| Undo | A check-in can be undone **on the same day only**. Its points (and any streak bonus) are reversed. |
| Active window | A habit can only be checked in between its start and end dates, and only if it isn't archived. |
| Periods | Weeks run **Monday–Sunday** (ISO). Months are calendar months. Both use the user's time zone. |

### 5.3 Points
| Rule | Detail |
|---|---|
| Earning | Each check-in earns the habit's points **as they were at check-in time** (stored with the check-in). |
| Ledger | Every change in points is a row in a ledger: earn, streak bonus, redeem, undo. The balance is the sum of the ledger. |
| Balance | Points available to spend. Never negative. |
| Lifetime XP | Total points ever earned. **Never goes down**, even when points are spent. Determines the user's **level**. |

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
| Habits | Habits page in the browser | ⏳ Sprint 1 |
| Check-ins | Check off today, same-day undo, points ledger | Sprint 2 |
| Streaks | Current/best streak, flat bonuses | Sprint 2 |
| Prizes | Create prizes, redeem, redeem count | Sprint 2 |
| Dashboard | Today view: balance, level, streaks | Sprint 2–3 |
| Analytics | Weekly / monthly / yearly: completion rate per habit, points earned vs spent, best streaks, most-redeemed prizes, year heatmap | Sprint 3 |
| Levels | XP → level | Sprint 3 |
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

## 9. Open questions
| # | Question | Proposed answer |
|---|---|---|
| 1 | What XP is needed for each level? | Level *n* needs 50 × n² XP in total (L1 = 50, L2 = 200, L3 = 450 …), so levels get slower to reach. Decide in Sprint 3. |
| 2 | Undo after spending: points earned today were already used to redeem a prize, so undoing would make the balance negative. | Block the undo and show "You've already spent these points." |
| 3 | What happens when a habit's end date passes? | Show it as **completed** (separate from archived), with its final stats. |
| 4 | Can a prize's cost be edited? | Yes; past redemptions keep the cost they had at the time. |
| 5 | Should weekly habits allow more than one check-in per day? | No; one per day keeps it simple. |
