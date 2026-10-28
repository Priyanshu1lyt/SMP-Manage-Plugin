# SmpRecording

A Paper plugin (Minecraft 1.21.11, Java 21) with simple moderation and fun tools for SMP recording.

## Commands

All commands need the `smprec.admin` permission (OP by default).

| Command | What it does |
|---|---|
| `/ban <player> [reason]` | Ban a player (works for offline players too) |
| `/unban <player>` | Unban a player |
| `/mute <player>` | Mute a player |
| `/unmute <player>` | Unmute a player |
| `/chat <on\|off>` | Turn chat on/off for everyone (admins can still chat) |
| `/immortal <player>` | Toggle immortal mode. Health never goes below half a heart, the player cannot die |
| `/joinleave <on\|off>` | Turn join/leave messages on/off (aliases: `/join-and-leave`, `/jl`) |

Bans, mutes and the chat / join-leave settings are saved in `plugins/SmpRecording/data.yml`. Immortal mode resets when the player leaves or the server restarts.

## Build with GitHub

1. Upload this project to a GitHub repo.
2. Open the **Actions** tab. The build runs on every push, or press **Run workflow**.
3. Download the jar from **Artifacts -> SmpRecording**.

## Install

Put `SmpRecording.jar` in your server's `plugins` folder and restart the server.

## Build locally

```
gradle build
```

The jar will be at `build/libs/SmpRecording.jar`.
