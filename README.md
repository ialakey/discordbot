# Discord Bot

**A Spring Boot bot that bridges a Discord server and a Telegram chat.** It plays audio into voice
channels, speaks text out loud, records what happens in a channel and forwards the recording to
Telegram — and it can be driven from either side: Discord commands or Telegram commands.

<p>
  <img src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/JDA-5-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="JDA"/>
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
</p>

![Admin panel](https://github.com/user-attachments/assets/ca173ae0-2c0e-4405-b78f-24fd5c32bcd1)

---

## What it does

- **Two-way Discord ↔ Telegram bridge.** A Telegram command can make the bot speak or play audio in
  a Discord voice channel; a Discord command can push a recording or a message into Telegram.
- **Soundboard.** Any `.mp3` in the audio folder becomes a command automatically — drop `csgo.mp3`
  in and `!csgo` plays it. No code change, no restart of the command registry.
- **Text-to-speech.** `!speak <channel> <text>` synthesises speech with gTTS and streams it into the
  voice channel through LavaPlayer.
- **Voice recording.** `!record` captures a voice channel to a file, `!stop` ends the session and
  delivers the audio to the configured Telegram chat.
- **Voice channel moderation.** Removes users from a channel after playing a message, with a
  persisted block list of users and roles.
- **Web admin panel.** A Thymeleaf UI at `/admin` to trigger playback, send TTS, and manage blocked users
  and roles without touching Discord.

---

## Commands

### Discord

| Command | Description | Example |
|---|---|---|
| `!<filename>` | Plays `<filename>.mp3` from the audio folder in your current or a named voice channel | `!csgo` · `!alo General` |
| `!speak <channel> <text>` | Converts text to speech and plays it in the channel | `!speak General Hello, how are you?` |
| `!record <channel>` | Starts recording the voice channel | `!record General` |
| `!stop` | Stops recording and sends the file to Telegram | `!stop` |
| `!delete <channel>` | Plays a message, then removes users from the channel | `!delete General` |
| `!case` | Posts "Who will go for the case?" to both Discord and Telegram | `!case` |

Unknown `!command` names fall through to the soundboard: the bot looks for `command.mp3` and reports
back if the file is missing.

### Telegram

| Command | Description |
|---|---|
| `/vcinfo` | Lists who is currently sitting in which voice channel |
| `/sendvoice <channel>` | Sends a voice message into the given Discord channel |
| `/case` | Posts "Who will go for the case?" to both sides |

---

## Tech stack

| Area | Choice |
|---|---|
| Runtime | Java 17, Spring Boot 3.4, Gradle |
| Discord | [JDA 5](https://github.com/discord-jda/JDA) + [LavaPlayer](https://github.com/sedmelluq/lavaplayer) for audio |
| Telegram | Long-polling client (`TelegramBotPoller`) |
| Speech | `gtts4j` (Google Text-to-Speech) |
| Web | Spring MVC + Thymeleaf admin UI, Spring Security, springdoc OpenAPI |
| Storage | Spring Data JPA over H2 — blocked users and roles |
| Media | ffmpeg (installed in the runtime image) |

---

## Architecture

```
src/main/java/com/alakey/discordbot/
├── discordbot/
│   ├── config/JdaConfig.java           # JDA client bootstrap
│   ├── command/                        # one class per command, dispatched by CommandManager
│   │   ├── CommandManager.java         #   routes "!x" → command, else → soundboard lookup
│   │   ├── SpeakCommand.java
│   │   ├── RecordCommand.java / SimpleAudioRecorder.java
│   │   ├── StopCommand.java
│   │   ├── PlayFileCommand.java
│   │   └── CaseCommand.java
│   ├── audio/AudioPlayerSendHandler.java  # LavaPlayer → JDA audio bridge
│   ├── listener/VoiceChannelListener.java
│   └── session/RecordingSession.java
├── telegrambot/TelegramBotPoller.java  # Telegram long polling, mirrors commands
├── service/                            # AudioService, TextToSpeechService, GuildService,
│                                       # VoiceChannelInfoService, BlockedEntityService, cache
├── controller/AdminController.java     # web admin panel
├── model/ + repository/                # BlockedUser, BlockedRole (JPA)
└── config/SecurityConfig.java
```

`CommandManager` is the single dispatch point: explicit commands are registered in a map, and
anything unmatched is resolved as a soundboard file. That is why adding a sound requires no code.

---

## Configuration

The application reads everything from environment variables — it will not start without them:

| Variable | Purpose |
|---|---|
| `DISCORD_BOT_TOKEN` | Discord bot token |
| `DISCORD_GUILD_BY_ID` | ID of the guild (server) the bot serves |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token |
| `TELEGRAM_CHAT_ID` | Chat that receives recordings and messages |
| `AUDIO_FOLDER` | Path to the `.mp3` soundboard folder |
| `USERNAME` / `PASSWORD` | Credentials for the web admin panel |

---

## Running

### Docker (recommended)

```bash
docker pull ialakey/discordbot:latest

docker run -p 8080:8080 \
  -e DISCORD_BOT_TOKEN=... \
  -e DISCORD_GUILD_BY_ID=... \
  -e TELEGRAM_BOT_TOKEN=... \
  -e TELEGRAM_CHAT_ID=... \
  -e AUDIO_FOLDER=/app/audio \
  -e USERNAME=admin \
  -e PASSWORD=secret \
  ialakey/discordbot:latest
```

The image is a two-stage build: Gradle produces the boot jar, then a slim JDK 17 image adds ffmpeg
and the bundled audio folder.

### Building it yourself

```bash
git clone https://github.com/ialakey/discordbot.git
cd discordbot
docker build -t discordbot .
```

### From source

```bash
./gradlew bootRun
```

Requires a local ffmpeg installation.

---

## Web interface

| URL | What |
|---|---|
| http://localhost:8080/admin | Admin panel — play audio, send TTS, kick from a channel, manage blocked users and roles |
| http://localhost:8080/swagger-ui/index.html | OpenAPI documentation |

The panel is protected by Spring Security using `USERNAME` / `PASSWORD`.

---

## Author

Ilia Alakov — [Telegram](https://t.me/i_alakey) · [LinkedIn](https://www.linkedin.com/in/ilia-alakov)
