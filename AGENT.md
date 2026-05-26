# AGENT — 46 Fisher Mods

## What This Repo Is

A Minecraft Java Edition plugin pack for game jam development. 46 plugins as a long-term goal,
starting with simple gameplay additions. Each plugin is a standalone Paper/Bukkit module in
a multi-module Gradle repo. The focus is on creative, fun, self-contained gameplay plugins —
not server infrastructure or datapack/command-chain work.

**Current plugins:** `vog` (Voice of God — TTS + echo audio), `poke`, `46-fisher-random-death`

## Stack

- **Platform:** Paper API 26.1.2-R0.1-SNAPSHOT (Minecraft 1.21.x)
- **Java:** 21
- **Build:** Gradle (Groovy DSL), shadowJar with relocated dependencies
- **Modules:** Multi-module Gradle repo — each plugin is its own module under the root

## Build Commands

```bash
chmod +x gradlew          # first time only
./gradlew shadowJar       # build all plugins
./gradlew :vog:shadowJar  # build one plugin
```

## Repo Layout

```
46-fisher-mods/
├── vog/                    # Voice of God — TTS, echo reverb, Plasmo Voice
├── poke/                   # minimal stub plugin
├── 46-fisher-random-death/ # random death message on punch
├── minecraft_skills/       # shared skill docs & references
│   ├── unhelpful/          # skills not yet validated — do not use directly
│   ├── minecraft-codex-skills/  # Codex/Claude Code plugin wrapper
│   └── README.md           # skill index
├── gradlew
└── settings.gradle        # root build, include all plugin modules
```

## Adding a New Plugin

1. Create the module directory (e.g., `myplugin/`)
2. Add `include 'myplugin'` to `settings.gradle`
3. Add `build.gradle` using the Paper/Groovy pattern (copy from an existing plugin)
4. Create `src/main/resources/plugin.yml`
5. Run `./gradlew :myplugin:shadowJar`

## Skill Ecosystem

Skills live at `minecraft_skills/` in the repo root. The index is `minecraft_skills/README.md`.

**Available skills:**
| Skill | What it covers |
|-------|----------------|
| `minecraft-plugin-dev` | Paper/Bukkit plugin development (Java 21) — events, commands, schedulers, PDC, Adventure, resource pack audio, FFmpeg |
| `minecraft-modding` | NeoForge/Fabric mod development |
| `minecraft-ci-release` | GitHub Actions, Modrinth/CurseForge publishing |
| `minecraft-commands-scripting` | Command chains, scoreboards, NBT, execute, RCON |
| `minecraft-server-admin` | Server ops, tuning, Docker, backups |

**Skill routing:**
| Task | Use skill |
|------|-----------|
| Write a Paper plugin | `minecraft-plugin-dev` |
| Write a Forge/Fabric mod | `minecraft-modding` |
| Command chains, scoreboards, NBT | `minecraft-commands-scripting` |
| Server ops, tuning, backups | `minecraft-server-admin` |
| CI/CD, publishing | `minecraft-ci-release` |

## Audio / TTS Notes (vog plugin)

- Minecraft resource packs require **OGG Vorbis** audio. Most TTS APIs return MP3 — convert with FFmpeg.
- `player.playSound()` can fail silently on headless servers (Docker, no audio driver).
  If packets send but clients hear nothing, use the Plasmo Voice addon Opus streaming approach.
- "Voice from sky" effect = echo layering at the player's position, NOT spatial distance.
  Minecraft's distance attenuation silences distant sounds entirely.
- Always stream the audio file, not raw TTS bytes — file-based audio carries correct encoding headers.

## Code Conventions

- Plugin main class extends `JavaPlugin`, follows standard Paper plugin pattern
- `plugin.yml` with `api-version: '1.21'`
- Version in `build.gradle` (`version = '1.0.0-SNAPSHOT'`), expanded into `plugin.yml` via `processResources`
- Shadow fat jar (no assembly, no relocate — use `com.gradleup.shadow` with `archiveClassifier = ''`)
- JUnit 6 for tests where applicable
- Groovy DSL (`.gradle`, not `.gradle.kts`)

## Style

- Pragmatic, self-contained plugins
- Fun gameplay — game jam spirit, not production-grade infrastructure
- Each plugin owns its code and config; minimal cross-plugin dependencies
- Comments where non-obvious; delete dead code