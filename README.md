<div align="center">

<img src="https://cdn.modrinth.com/data/cached_images/a20fc1f9c30851b62c2264a9c179e817654f2652.jpeg" width="140" alt="VerityAI logo"/>

# VerityAI

**An AI assistant plugin for Minecraft (Paper) servers, powered by [OpenRouter](https://openrouter.ai).**

[![Modrinth](https://img.shields.io/badge/Modrinth-VerityAI-1bd96a?logo=modrinth&logoColor=white)](https://modrinth.com/plugin/verityai)
[![bStats Servers](https://img.shields.io/bstats/servers/33005?label=servers)](https://bstats.org/plugin/bukkit/VerityAI/33005)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

</div>

Talk to Verity right in chat — ask questions, get real-time help with builds, quests, reminders, and more, all backed by a real AI model you choose yourself.

## ✨ Features

- **Chat naturally** — trigger with `@verity <message>` or start hands-free conversation mode with `/verity chat`
- **Live world awareness** — Verity answers using real data (coordinates, nearby biomes, weather, TPS, inventory) instead of guessing
- **Long-term memory** — remembers facts about each player across sessions, with optional semantic (embedding-based) search
- **Personalities** — switch between built-in presets (funny, formal, tutor, admin...) or write your own, even per-world
- **Function calling** — lets supported AI models run real actions: check player status, run whitelisted commands, manage economy, remember facts
- **Automatic quests** — Verity sends players fresh quest suggestions on a timer
- **Personal reminders** — players can set their own daily in-game reminders
- **Soft integrations** — Vault (economy), LuckPerms, EssentialsX, PlaceholderAPI — all optional, all auto-detected
- **Multi-key & multi-model fallback** — configure several API keys and models; VerityAI automatically retries and falls back if one fails

## 📦 Installation

1. Download the latest `VerityAI.jar` from [Releases](../../releases) or [Modrinth](https://modrinth.com/plugin/verityai)
2. Drop it into your Paper server's `plugins/` folder (Paper 1.21+)
3. Start the server once to generate `plugins/VerityAI/config.yml`
4. Get an API key from [OpenRouter](https://openrouter.ai/keys) and add it under `ai.api-keys` in `config.yml`
5. Restart, or run `/verity reload`

Full setup and configuration reference: see [config.yml](src/main/resources/config.yml) — every setting is documented inline.

## 🔑 Getting an OpenRouter API key

1. Sign up at [openrouter.ai](https://openrouter.ai)
2. Go to [openrouter.ai/keys](https://openrouter.ai/keys) → **Create Key**
3. Paste it into `config.yml`:
   ```yaml
   ai:
     api-keys:
       - "sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
   ```

VerityAI ships with a free-tier model + fallback chain by default. OpenRouter's free catalog changes frequently — check [openrouter.ai/models?supported_parameters=tools](https://openrouter.ai/models?supported_parameters=tools) if you want to pick your own.

## 🎮 Commands

```
/verity reload                          - Reload the configuration
/verity clear [player]                  - Clear conversation memory
/verity info                            - Show plugin info
/verity toggle                          - Enable/disable Verity globally
/verity personality [name]              - Change Verity's active personality
/verity owner [player]                  - Set the server owner (must be online)
/verity map                             - Show the in-chat mini-map
/verity stats                           - Show usage stats
/verity model [name]                    - View/switch the primary AI model
/verity task <add|remove|list>          - Manage your personal reminders
/verity quest [interval <minutes>]      - Request a quest / configure auto-quests
/verity tutorial <topic>                - In-game help topics
/verity feedback <good|bad> [correction]- Record feedback/corrections
```

## 🧩 Requirements

- Paper (or compatible forks) 1.21+
- Java 21+
- An OpenRouter API key

## 🤝 Soft Dependencies (all optional)

PlaceholderAPI · Vault · LuckPerms · Essentials

## 📊 Metrics

VerityAI uses [bStats](https://bstats.org/plugin/bukkit/VerityAI/33005) for anonymous usage statistics (server count, versions, feature usage — never anything personal). Opt out anytime via `plugins/bStats/config.yml`.

## 🛠️ Building from source

```bash
mvn clean package
```
The finished jar is written to `target/VerityAI.jar`.

## 📄 License

MIT
