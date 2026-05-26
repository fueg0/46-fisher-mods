# 46 Fisher Mods — Paper Plugin Pack

A Minecraft Java Edition plugin pack for game jam development. Multi-module Gradle project, each plugin is a standalone Paper/Bukkit module.

**Stack:** Paper API 26.1.2-R0.1-SNAPSHOT, Java 21, shadowJar, Groovy DSL build

## Build

```bash
chmod +x gradlew
./gradlew shadowJar
```

Output: each module's `build/libs/` contains a fat jar with dependencies relocated.

## Build a Single Plugin

```bash
./gradlew :vog:shadowJar
./gradlew :poke:shadowJar
./gradlew :46-fisher-random-death:shadowJar
```

## Project Structure

```
46-fisher-mods/
├── vog/                    # Voice of God — TTS + echo audio effects
├── poke/                   # (in development)
├── 46-fisher-random-death/ # (in development)
├── minecraft_skills/       # Shared skill docs & references
├── gradlew
└── settings.gradle         # include 'vog', 'poke', '46-fisher-random-death'
```

## Adding a New Plugin

1. Create the module directory (e.g., `myplugin/`)
2. Add `include 'myplugin'` to `settings.gradle`
3. Copy a `build.gradle` from an existing plugin (matching Groovy DSL + Paper API)
4. Create `src/main/resources/plugin.yml`
5. Run `./gradlew :myplugin:shadowJar`

## Shared Resources

Place shared skill references at `minecraft_skills/` at the repo root. These are
copied into agent contexts so all plugins share the same knowledge base.