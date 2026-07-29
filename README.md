# VerityAI

An AI assistant plugin for Minecraft (Paper) servers, powered by [OpenRouter](https://openrouter.ai), letting players chat directly with an AI in-game.

## Installation

1. Download the latest `VerityAI.jar` from the [Releases](../../releases) page.
2. Drop the jar into your Paper server's `plugins/` folder (server version 1.21+).
3. Start/restart the server once so the plugin generates its default config (`plugins/VerityAI/config.yml`).
4. Get an API key from OpenRouter (see below) and add it to `config.yml`.
5. Restart the server or run `/verity reload`.

## Getting an OpenRouter API Key

1. Go to [openrouter.ai](https://openrouter.ai) and create an account (or sign in with Google/GitHub).
2. Once logged in, go to the **Keys** section (direct link: [openrouter.ai/keys](https://openrouter.ai/keys)).
3. Click **Create Key**, give it a name, and copy the generated key.
4. Paste it into `plugins/VerityAI/config.yml` under `ai.api-keys`:

   ```yaml
   ai:
     api-keys:
       - "sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
   ```

   You can add multiple keys — VerityAI automatically falls back to the next one if a key fails or gets rate-limited.

> Some models on OpenRouter are free (e.g. the default `nvidia/nemotron-3-ultra-550b-a55b:free`), but stronger models may require credit on your OpenRouter account. See current pricing at [openrouter.ai/models](https://openrouter.ai/models).

## Commands

The main command is `/verity`, with the following subcommands:

```
/verity reload                          - Reload the configuration
/verity clear [player]                  - Clear conversation memory
/verity info                            - Show plugin info
/verity toggle                          - Enable/disable Verity globally
/verity personality [name]              - Change Verity's active personality preset
/verity owner [player]                  - View/set the configured server owner
/verity map                             - Show the in-chat mini-map
/verity stats                           - Show usage stats
/verity model [name]                    - View/switch the primary AI model
/verity task <add|remove|list>          - Manage your personal reminders
/verity quest                           - Generate a random quest
/verity tutorial <topic>                - In-game help topics
/verity feedback <good|bad> [correction]- Record feedback/corrections
```

## Requirements

- Paper (or compatible forks) 1.21+
- Java 21+
- An OpenRouter API key

## Soft Dependencies

VerityAI integrates with these plugins if present (all optional — VerityAI works fine without them):

- PlaceholderAPI
- Vault
- LuckPerms
- Essentials
