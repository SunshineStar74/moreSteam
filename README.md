# More Steam
More Steam is a Create mod addon that adds more steam to train bogeys.

## What it does
- **Bigger stop steam.** When a train comes to a stop, Create's small white poofs at the bogeys are replaced by large, slow-fading steam clouds.
- **Longer stop steam.** Create releases steam for at most half a second. More Steam stretches that (three times as long by default).
- **Idle steam.** While a train stands still, each bogey keeps letting off a small wisp of steam every now and then.

Bogey styles from other addons that set their own smoke particle are left alone, and the handcar from Steam 'n' Rails stays steam-free.

## Requirements
| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 or newer |
| Create | 6.0.10 (any 6.0.x from 6.0.10 on) |

The effects are client-side, but the mod registers two particle types. For multiplayer, install it on the server as well as on the clients; joining a server that lacks it has not been tested.

The Forge 1.20.1 / Create 6.0.8 code lives on the `Steam-1.20.1` branch and is not maintained.

## Configuration
Settings are in `config/moresteam-client.toml`, or in-game under Mods > More Steam > Config.

| Setting | Default | Meaning |
|---|---|---|
| `steamDurationMultiplier` | `3.0` | How much longer bogeys release steam after a stop. `1.0` is Create's own duration. Range 1 to 20. |
| `idleSteam` | `true` | Turns the idle wisps on or off. |
| `idleSteamInterval` | `8` | Average ticks between idle wisps, per bogey. Lower means more steam. Range 1 to 200. |

## Building
Needs a JDK 21. Gradle finds an installed one, or downloads it.

```
./gradlew build
```

The jar ends up in `build/libs`. `./gradlew runClient` starts a development client with Create loaded.

## How it works
Everything hooks into `CarriageParticles#tick` in Create, which runs on the client for every carriage near the camera. The mixin swaps the particle Create spawns while a stopped bogey is depressurising, widens the tick window Create uses for that phase, and adds its own low-rate idle puffs at the end of the tick. Because this targets Create internals, the mod only accepts Create 6.0.x and refuses to load with a newer minor version until it has been checked against it.
