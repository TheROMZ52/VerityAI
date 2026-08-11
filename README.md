<div align="center">

  ![verity banner](https://cdn.modrinth.com/data/cached_images/a20fc1f9c30851b62c2264a9c179e817654f2652.jpeg)


# VerityAI

**An AI assistant plugin for Minecraft (Paper) servers, powered by [OpenRouter](https://openrouter.ai).**

[![Modrinth](https://img.shields.io/badge/Modrinth-VerityAI-1bd96a?logo=modrinth&logoColor=white)](https://modrinth.com/plugin/verityai)
[![bStats Servers](https://img.shields.io/bstats/servers/33005?label=servers)](https://bstats.org/plugin/bukkit/VerityAI/33005)
[![License](https://img.shields.io/badge/license-GPL-blue)](LICENSE)

</div>



VerityAI allows players to chat directly with an AI assistant inside Minecraft with support for multiple AI models, conversation memory, commands, and server integrations.

## ✨ Features

- 💬 Chat with AI directly in-game
- 🧠 Conversation memory system
- 🔄 Automatic API key fallback
- 🎭 Multiple personality presets
- 📊 Usage statistics
- 🗺️ In-game mini-map support
- 📝 Personal reminders and tasks
- 🎯 AI-generated quests
- 📚 In-game tutorials and help
- 💡 Player feedback and correction system
- 🔌 Integration with popular server plugins

## 📥 Installation

1. Download the latest **VerityAI.jar**
2. Place the jar file into your server's:

```text
plugins/
```

folder.

3. Start or restart your Paper server.
4. VerityAI will generate its default configuration:

```text
plugins/VerityAI/config.yml
```

5. Add your OpenRouter API key to the configuration file.
6. Restart your server or use:

```text
/verity reload
```

## 🔑 Getting an OpenRouter API Key

1. Go to OpenRouter and create an account or sign in.
2. Open the **Keys** section.
3. Click **Create Key**.
4. Copy your generated API key.
5. Add it to `config.yml`:

```yml
ai:
  api-keys:
    - "sk-or-v1-your-key-here"
```

You can add multiple API keys.

VerityAI automatically switches to another key if the current key fails or reaches its rate limit.

> Some OpenRouter models are free, while stronger models may require credits.

## 📜 Commands

| Command | Description |
|---|---|
| `/verity reload` | Reload configuration |
| `/verity clear [player]` | Clear conversation memory |
| `/verity info` | Show plugin information |
| `/verity toggle` | Enable or disable Verity |
| `/verity personality [name]` | Change AI personality preset |
| `/verity owner [player]` | View or set server owner (target must be online) |
| `/verity op <add\|remove\|list> [player]` | Manage trusted "ops" who get the same elevated command access as the owner |
| `/verity map` | Show in-chat mini-map |
| `/verity stats` | Show usage statistics |
| `/verity model [name]` | View or switch AI model |
| `/verity task <add\|remove\|list>` | Manage personal reminders |
| `/verity quest [interval <minutes>]` | Generate a quest, or configure automatic quests for everyone online |
| `/verity tutorial <topic>` | Get in-game help |
| `/verity feedback <good\|bad> [correction]` | Submit AI feedback |

## ⚙️ Requirements

- Paper (or compatible forks) **1.21+**
- Java **21+**
- OpenRouter API key

## 🔌 Soft Dependencies

VerityAI can integrate with these plugins if installed:

- PlaceholderAPI
- Vault
- LuckPerms
- Essentials

All dependencies are optional. VerityAI works without them.

## ⚙️ Configuration

Main configuration file:

```text
plugins/VerityAI/config.yml
```

You can customize:

- AI models
- API keys
- Personality presets
- Plugin behavior
- Server settings

## 📊 Metrics

VerityAI uses [bStats](https://bstats.org/plugin/bukkit/VerityAI/33005) for anonymous usage statistics (server count, versions, feature usage — never anything personal). Opt out anytime via `plugins/bStats/config.yml`.

## 🛠️ Building from source

```bash
mvn clean package
```
The finished jar is written to `target/VerityAI.jar`.

## 📜 License

VerityAI is free and open-source software licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

You are free to:

* ✅ Use VerityAI for personal or commercial purposes
* ✅ Study and modify the source code
* ✅ Fork and redistribute modified versions
* ✅ Distribute copies of the plugin

When redistributing VerityAI or a modified version, you must comply with the terms of the **GPL-3.0** license.

The full license text is available in the [`LICENSE`](LICENSE) file.

**Copyright © 2026 TheROMZ52**

## 🚀 Support

If you find a bug or have a suggestion, please create an issue or contact the developer.

Enjoy using **VerityAI**! 🤖
