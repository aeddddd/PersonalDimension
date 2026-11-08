# Personal Plane

A standalone Minecraft **1.20.1 / 1.21.1 NeoForge** mod that gives players a personal flat dimension with
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

- `1.20.1-neoforge`: Minecraft 1.20.1, NeoForge 47.1.106+, Java 17
- `1.21.1-neoforge`: Minecraft 1.21.1, NeoForge 21.1.265+, Java 21
- Optional: FTB Teams + FTB Library (team sharing), FTB Ranks (optional permission nodes)

## Build

```bash
./gradlew build
```

The output jar is in `build/libs/`.

The first build needs network access to resolve ModDevGradle and NeoFormRuntime
(NeoForge's maven may be unreachable in some networks; a pre-populated Gradle cache
allows `--offline`).

## Branches

- `1.20.1-neoforge` — Minecraft 1.20.1 / NeoForge 47.x
- `1.21.1-neoforge` — Minecraft 1.21.1 / NeoForge 21.1.x (current)

## License

GNU General Public License v3.0.
