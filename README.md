# Possession

A Fabric 1.20.1 mod centered around possessing creatures, sharing their perspective, and using species-specific abilities through a Soul Charm trinket.

## Status

- Current release stage: `0.1.0-beta`
- Primary target: singleplayer / integrated server gameplay
- Main dependencies: `Fabric API`, `Trinkets`

This project is already playable, but it is still an early public version. Some forms and edge cases are still being polished.

## Features

- Possess nearby living entities while wearing the Soul Charm
- See the world through the target's body
- Leave the target body manually or when possession is interrupted
- Use special abilities for selected creatures
- Gain the Soul Charm automatically after the player's first death
- Tune stats and behavior parameters through `config/possession.json`

## Requirements

- Minecraft `1.20.1`
- Fabric Loader
- Fabric API
- Trinkets

## Installation

1. Install Fabric for Minecraft `1.20.1`
2. Put `Fabric API`, `Trinkets`, and `Possession` into the `mods` folder
3. Launch the game once to generate the config file
4. If needed, edit `config/possession.json`

## Soul Charm

The Soul Charm is required for possession.

Ways to obtain it:

- Automatically granted after the player's first death and respawn
- Crafting recipe:

```text
  String
Amethyst Shard + Ender Pearl + Amethyst Shard
  String
```

The Soul Charm is designed to be worn through Trinkets.

## Default Controls

- `J`: attach / possess target
- `K`: detach
- `R`: use special ability
- `V`: toggle possession camera perspective
- Movement / attack / use item: follow normal Minecraft controls while possessing

## Current Supported Highlight Forms

These forms currently have dedicated logic or gameplay flavor beyond the default possession behavior:

- Creeper
- Skeleton
- Zombie
- Frog
- Dolphin
- Squid / Glow Squid
- Slime / Magma Cube
- Spider / Cave Spider
- Camel
- Goat
- Enderman
- Wolf

Examples of unique behavior:

- Creeper: self-detonation
- Skeleton: bow shot
- Frog: swallow and spit
- Goat: charged jump and ram
- Enderman: teleport and block carry/place
- Zombie: armor inheritance and weapon-damage carryover

## Gameplay Notes

- Monsters must be below half health before possession can start
- The possessed body has its own health handling and boosted body stats
- If the target body dies during possession, the player receives backlash damage
- Manual detachment avoids target-death backlash

## Configuration

After the first launch, the mod writes:

- `config/possession.json`

Current config structure includes:

- `general`: core possession rules
- `client`: HUD and trinket model settings
- `goat`: dedicated goat charge and ram tuning
- `forms`: stat and cooldown tuning per form
- `behaviors`: skill behavior parameters such as explosion power, teleport range, swallow range, and projectile tuning

## Known Issues

- The mod is primarily tested in singleplayer so far
- Not every vanilla creature has a fully customized form yet
- Rare camera or movement edge cases may still appear for unusual mobs
- This beta focuses on the main possession loop first, then broader compatibility

## AI-Assisted Development

This project was developed with AI assistance. Codex and GPT-4.5 were used during design, implementation, debugging, and refactoring. Final feature decisions, gameplay direction, testing, and release preparation are handled by the project author.

## Building From Source

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The built jar will be generated under:

- `build/libs/`

## License

Currently distributed as `All-Rights-Reserved` unless stated otherwise in future releases.
