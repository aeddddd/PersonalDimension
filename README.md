# Personal Plane

A standalone Minecraft **1.20.1 / NeoForge** mod that gives players a personal flat dimension with
custom floor presets, colors, multi-floor layouts, rules and an asynchronous recarpet pipeline.

> 中文说明见 [README_zh.md](README_zh.md)。


## Features

- Personal dimension core item: create, enter and leave a personal dimension.
- Personal Plane Manager block: rules, floor presets/colors and layout settings.
- FTB Teams integration: by default a team shares one dimension; each player can switch to a
  private personal dimension. Without FTB Teams the mod falls back to owner/OP access.
- No bedrock, no natural mob spawning, configurable floor height, multi-floor layouts,
  asynchronous floor recarpeting with per-chunk apply budget.

## Requirements

- Minecraft 1.20.1
- NeoForge 47.1.106+
- Java 17
- Optional: FTB Teams + FTB Library (for team sharing), FTB Ranks (optional permission nodes)

## Build

```bash
./gradlew build
```

The output jar is in `build/libs/`.

## Branches

- `1.20.1-neoforge` — Minecraft 1.20.1 / NeoForge 47.x (default)
- `1.21.1-neoforge` — Minecraft 1.21.1 / NeoForge 21.1.x

## License

GNU General Public License v3.0.
