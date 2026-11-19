# More Steam
More Steam is a Create addon that makes train bogeys blow off steam: from the cylinders of piston bogeys when a train stops, stands, or pulls away, with sound.

> **Disclaimer:** all of the code in this mod is vibe-coded: written by AI, steered, tested and approved by me. The assets (textures, models and sounds) are not AI-made.

## Download
Get the jar from the [Releases](https://github.com/SunshineStar74/moreSteam/releases) page. The latest release, **v.1.4.6**, is for Minecraft 1.21.1 with NeoForge and Create 6.0.10.

## What it does
- **Firebox.** The cylinder steam only comes from carriages with a **Firebox** block glued into them. The Firebox is in the Functional Blocks creative tab; it has no crafting recipe yet.
- **Stop steam.** When a train stops, Create's own small downward puffs last longer on every bogey (three times as long by default). On top of that, the cylinders of piston bogeys blow steam backwards for up to 5 seconds, with a hiss. How much steam, how far it shoots, how long it lasts, and how long and loud the hiss is all depend on how hard the train stopped: how far it travelled since the last stop, and how fast it was going when it started braking. A long run braked from full speed gives the full burst; a short shunt gives little or none.
- **Idle steam.** While a train with a firebox stands still, each cylinder lets off a 5-second burst every 10 to 15 seconds, each on its own clock. After a stop, idle steam waits until the stop hiss has died away, plus 20 seconds.
- **Pull-away steam.** When a train leaves a standstill, every cylinder bursts at once with a pressure-release sound, then puffs every 4 to 7 seconds until the train reaches 20% of its top speed. Slowing down never starts these bursts.
- **Bursts go backwards.** Every burst shoots towards the back of the train, from just below the cylinders.
- **Sound.** Each cylinder has its own positional sound that rides along with the train and can be heard up to 32 blocks away.

### Which bogeys have pistons
Every bogey style with Walschaerts, Gearless or Scotch Yoke in its name. Scotch Yokes have a front and a back cylinder that blow independently.

| Mod | Piston bogeys |
|---|---|
| Create | Large bogey (a Scotch Yoke) |
| Steam 'n' Rails | All "Create Styled" (Scotch Yoke) bogeys, including narrow and wide gauge, and Comically Large |
| Blocks and Bogeys | Scotch Yoke, Walschaerts and Gearless bogeys of every length |

Bigger bogeys let off more steam (see `steamAxleBase` below). Small pistons get smaller bursts without the big clouds. Bogeys without pistons only keep Create's own puffs.

## Requirements
| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 or newer |
| Create | 6.0.10 (any 6.0.x from 6.0.10 on) |

Steam 'n' Rails and Blocks and Bogeys are optional; their bogeys are supported when they are installed.

The effects are client-side, but the mod also registers particles, sounds and the Firebox block. Install it on the server as well as on the clients.

## Configuration
Settings are in `config/moresteam-client.toml`, or in-game under Mods > More Steam > Config, which opens Create's config screen. Changes apply right away.

| Section | Setting | Default | Meaning |
|---|---|---|---|
| | `soundRange` | `32` | How far away, in blocks, the bursts can be heard. 16 to 256. |
| | `steamAxleBase` | `2.0` | Steam per axle: amount = base ^ (axles - 2). Grows very fast; at 3.0 a six-axle bogey makes 81 times the steam. |
| `stopSteam` | `enableStopSteam` | `true` | Backward bursts and hiss when stopping. When off, Create's puffs keep Create's own length too. |
| | `createPuffMultiplier` | `3.0` | How much longer Create's own downward puffs last after a stop. 1.0 is Create's duration. |
| | `burstLength` | `5.0` | Longest stop burst, in seconds, after a long and fast trip. |
| | `pressureCurve` | `4.0` | How much less steam slow or short trips give. 1.0 is a straight line. |
| `idleSteam` | `enableIdleSteam` | `true` | Bursts while standing. |
| | `delayAfterStop` | `20.0` | Seconds of quiet after the stop hiss before idle steam starts. |
| | `intervalMin` / `intervalMax` | `10.0` / `15.0` | Wait between a cylinder's bursts, in seconds. |
| | `burstLength` | `5.0` | Length of each burst, in seconds. |
| `pullAwaySteam` | `enablePullAwaySteam` | `true` | Bursts while leaving a standstill. |
| | `intervalMin` / `intervalMax` | `4.0` / `7.0` | Wait between a cylinder's bursts, in seconds. |
| | `burstLength` | `1.0` | Length of each burst, in seconds. |
| | `cutoff` | `0.2` | Fraction of the train's top speed at which the bursts stop. |

## Branches
| Branch | Contents |
|---|---|
| `main` | NeoForge 1.21.1 / Create 6.0.10. The latest released code; releases are built from here. |
| `Steam-1.20.1` | Forge 1.20.1 / Create 6.0.8. Not maintained. |

New work happens on short-lived branches off `main` and is merged back through a pull request once it has been tested.

## Credits
- **Sounds** are from [Pixabay](https://pixabay.com), under the [Pixabay Content License](https://pixabay.com/service/license-summary/). `1_sec_no_loop_burst` is by DRAGON STUDIO; all the others are by freesound_community.

## Building
On the `main` branch. Needs a JDK 21; Gradle finds an installed one, or downloads it.

```
./gradlew build
```

The jar ends up in `build/libs`. `./gradlew runClient` starts a development client with Create loaded. To test Steam 'n' Rails or Blocks and Bogeys bogeys, put their jars in `run/mods`.

## How it works
Everything hooks into `CarriageParticles#tick` in Create, which runs on the client for every carriage near the camera. A mixin stretches Create's stop-steam window, and at the end of each tick adds the cylinder bursts and their sounds. Cylinder positions come from each bogey style's model. The stop pressure is read from Create's own steam counter (`Train.accumulatedSteamRelease`, which grows with the distance travelled) on arrival. Because this targets Create internals, the mod only accepts Create 6.0.x and refuses to load with a newer minor version until it has been checked against it.
