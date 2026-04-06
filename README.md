# Discord Bot App

A Discord bot application containerized with Docker. Supports interaction with Discord and Telegram, including voice commands, text-to-speech, and voice channel management.

---

## 📁 Project Structure

- `Dockerfile` — instructions for building the Docker image.
- `swagger-ui/` — web interface for API documentation via Swagger.

---

## 🚀 Installation and Launch

### 1. Pull the Docker image from Docker Hub
```bash
docker pull ialakey/discordbot:latest
```

### 2. Clone the repository
```bash
git clone https://github.com/ialakey/discordbot-app.git
cd discordbot-app
```

### 3. Build the image manually (if needed)
```bash
docker build -t discordbot-app .
```

### 4. Run the container
```bash
docker run -p 8080:8080 discordbot-app
```

---

## 🌐 Web Interface

- Main page: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

> ⚠️ Make sure Docker is installed and running on your device.

---

## 🧠 Commands

### 📡 Discord

| Command | Description | Example |
|--------|-------------|--------|
| `!delete <channel_name>` | Removes users from the specified voice channel after playing a message | `!delete test voice channel` |
| `!speak <channel> <text>` | Converts text to speech in the specified voice channel | `!speak voice channel Hello, how are you?` |
| `!record <channel>` | Starts recording in the voice channel | `!record test voice channel` |
| `!case` | Sends "Who will go for the case?" message to Discord and Telegram | `!case` |
| `!stop` | Stops recording and sends the file to Telegram | `!stop` |

### ✈️ Telegram

| Command | Description | Example |
|--------|-------------|--------|
| `/vcinfo` | List of users in voice channels | `/vcinfo` |
| `/sendvoice <channel>` | Sends a voice message to Discord | `/sendvoice voice channel` |
| `/case` | Sends "Who will go for the case?" message to Discord and Telegram | `/case` |

---

## 🖼️ Screenshot

![image](https://github.com/user-attachments/assets/ca173ae0-2c0e-4405-b78f-24fd5c32bcd1)

---

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
