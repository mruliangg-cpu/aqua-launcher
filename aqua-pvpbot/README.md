# Aqua PvP Bot

PvP practice bots for Minecraft **1.21.11 (Fabric)**. Made by Ulian Voskoboynikov.

Drop `AquaPvPBot-1.0.0-1.21.11-fabric.jar` into `.minecraft/mods` (Fabric Loader). No other mods needed, not even Fabric API.
Works in singleplayer and on your own server (install it on the server; players don't need it).

## Commands

| Command | What it does |
|---|---|
| `/spawnbot <easy\|normal\|hard\|endless\|impossible> [kit]` | Spawns a bot in front of you. 3-2-1 countdown, then it fights you. |
| `/kit <sword\|diamond\|op\|nethpot\|crystal\|mace>` | Gives you a kit. Bots you spawn afterwards use the same kit. |
| `/kit restore` | Gives back the items you had before your first `/kit`. |
| `/killbots` | Removes your bots. |
| `/pvpbot` | Menu with clickable buttons. |

## Difficulties

- **Easy**: slow reactions, misses a lot, no crits.
- **Normal**: strafes a bit, sometimes crits, heals when low, uses a bow at range.
- **Hard**: crits, w-taps, spacing, shield, axes your shield, throws pearls.
- **Impossible**: near-perfect reactions, aim and timing.
- **Endless**: waves. Every bot you beat brings a stronger one (Normal → Impossible by wave 12). If you picked the same kit with `/kit`, it is refilled between waves.

Bots never cheat. They have 20 HP, 3-block reach and the same kit as you. They just play better or worse.

## Kits

Sword · Diamond (shield, axe, gapples, bow) · OP (god netherite, enchanted gapples, totems, potions) ·
NethPot (splash healing, strength, speed, totems) · Crystal (obsidian, end crystals, totems, anchors) ·
Mace (Density V + Wind Burst mace, wind charges, totems).

## Building

GitHub Actions builds the jar on every push to the `aqua-pvpbot` branch (`.github/workflows/pvpbot.yml`).
Add `[test]` to the commit message to run a real dedicated server where bots fight each other with every kit,
and `[release]` to publish the jar as a GitHub pre-release.
