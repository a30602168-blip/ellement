# Elements SMP (Fabric mod, server-side only)

Minecraft 1.21.11 | Fabric | Java 21 | **Clients do not need the mod** (vanilla clients can join).

## Installation
1. `./gradlew build`
2. Put `build/libs/elementsmp-1.0.0.jar` into the server's `mods` folder (together with Fabric API).

## Gameplay
On first join every player randomly receives one of eight elements (Wind, Sand, Lava, Water, Earth, Void and the
rare **Nuke** and **Orbital**) and a **focus item**. Each element has 3 abilities with cooldowns.

### Orbital (rare, ~1.5% chance)
No kills needed.
1. **Orbital Strike Cannon** - 90 s cooldown. Aim at the ground (up to 150 blocks): after a short charge, 30 massive blasts rain down and carve a crater.
2. **Stab Shot** - 60 s cooldown. A line of 40 blasts drills a narrow, deep tunnel straight ahead.
Explosions destroy blocks; set `orbitalBlockDamage` to `false` in the config to disable that.

### Nuke (legendary, ~1% chance)
Pulling Nuke shows a **LEGENDARY PULL** title on screen and announces it to the server. Only kills of other players count.
1. **Acid Rain** - radius 25, 10 s, light damage every second to all enemies inside.
2. **Shockwave Aura** - needs 2 kills; pushes all enemies away for 3 s.
3. **Nuclear Detonation** - needs 20 kills; a 7 s explosion expands to a radius of 100 and kills everything inside
   (except the caster).

- Right-click with the focus item: cast the selected ability
- Shift + right-click: switch ability
- The action bar shows the ability and its cooldown

## Commands
| Command | Permission | Description |
|---|---|---|
| `/element` | everyone | Shows your element and abilities, opens the GUI |
| `/element gui` | everyone | Open the GUI |
| `/element cast <1-3>` | everyone | Cast an ability directly |
| `/element focus` | everyone | Get a new focus item |
| `/element info <player>` | admin | Show a player's element |
| `/element set <player> <element>` | admin | Change a player's element |
| `/element reset <player>` | admin | Reset the assignment (rolls again immediately) |
| `/element kills give <player> <amount>` | admin | Give Nuke kills |
| `/element kills take <player> <amount>` | admin | Remove Nuke kills |
| `/element kills set <player> <amount>` | admin | Set Nuke kills |
| `/element reload` | admin | Reload the configuration |

## Configuration
`config/elementsmp/config.json` is created on first start:
- `probabilities`: base probability per element
- `balanceStrength`: the higher, the less often already frequently assigned elements are rolled (0 = pure chance)
- `giveFocusItem`: automatically give the focus item
- `orbitalBlockDamage`: Orbital explosions destroy blocks (default true)
- `consumeKills`: if true, kill requirements are spent when the ability is used
- `abilities`: per ability `cooldownSeconds`, `power`, `radius`, `durationSeconds`, `damage`, `selfDamage`, `killsRequired`, `range`, `count`

Player data (UUID -> element): `config/elementsmp/players.json`, Nuke kills: `config/elementsmp/kills.json`.

## Extending
To add an ability, add an `Ability` entry in the matching `*Abilities` class (package `com.example.ability`).
Its config values are added automatically.
