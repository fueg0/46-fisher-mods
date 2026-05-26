# minecraft-agent-skills

Skills for AI coding agents working on Minecraft development projects.

## Skills

| Skill | What it covers |
|-------|----------------|
| `minecraft-plugin-dev` | Paper/Bukkit/Spigot plugin development — events, commands, schedulers, PDC, Adventure, resource pack audio |
| `minecraft-modding` | NeoForge + Fabric mod development — blocks, items, entities, events, data gen |
| `minecraft-ci-release` | GitHub Actions pipelines, Modrinth/CurseForge publishing, semantic versioning |
| `minecraft-commands-scripting` | Vanilla commands, scoreboards, NBT paths, execute chains, JSON text, RCON |
| `minecraft-server-admin` | Server setup, JVM tuning, Docker, Velocity proxy, backups, security |

## Format

Each skill is a directory containing a `SKILL.md` with YAML frontmatter (`name`, `description`)
and markdown body. Agents load skill content based on the `description` field matching the task.

## This Repo

This repo (`minecraft_skills/`) is a curated subset of the full
[minecraft-agent-skills](https://github.com/Jahrome907/minecraft-agent-skills) bundle,
trimmed to skills that are actually useful for the `46-fisher-mods` Paper plugin project.
The full bundle includes additional skills for datapacks, world generation, resource packs,
testing, WorldEdit ops, and EssentialsX ops — add them from the upstream if needed.