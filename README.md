# Discord Bot App

Приложение Discord-бота с контейнеризацией через Docker. Поддерживает взаимодействие с Discord и Telegram, включая голосовые команды, озвучку текста и управление голосовыми каналами.

---

## 📁 Структура проекта

- `Dockerfile` — инструкция для сборки Docker-образа.
- `swagger-ui/` — веб-интерфейс документации API через Swagger.

---

## 🚀 Установка и запуск

### 1. Получить Docker-образ с Docker Hub
```bash
docker pull ialakey/discordbot:latest
```

### 2. Клонировать репозиторий
```bash
git clone https://github.com/your-username/discordbot-app.git
cd discordbot-app
```

### 3. Построить образ вручную (если нужно)
```bash
docker build -t discordbot-app .
```

### 4. Запустить контейнер
```bash
docker run -p 8080:8080 discordbot-app
```

---

## 🌐 Веб-интерфейс

- Главная страница: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

> ⚠️ Убедитесь, что Docker установлен и работает на вашем устройстве.

---

## 🧠 Команды

### 📡 Discord

| Команда | Описание | Пример |
|--------|----------|--------|
| `!delete <название_канала>` | Исключает участников из указанного голосового канала после озвучивания | `!delete голосовой канал для тестов` |
| `!speak <канал> <текст>` | Озвучивает текст в указанном голосовом канале | `!speak голосовой канал Привет, как дела?` |
| `!record <канал>` | Начинает запись в голосовом канале | `!record голосовой канал для тестов` |
| `!case` | Отправляет сообщение "Кто пойдет за кейсом?" в Discord и Telegram | `!case` |
| `!stop` | Останавливает запись и отправляет файл в Telegram | `!stop` |

### ✈️ Telegram

| Команда | Описание | Пример |
|--------|----------|--------|
| `/vcinfo` | Список пользователей в голосовых каналах | `/vcinfo` |
| `/sendvoice <канал>` | Отправляет голосовое сообщение в Discord | `/sendvoice голосовой канал` |
| `/case` | Отправляет сообщение "Кто пойдет за кейсом?" в Discord и Telegram | `/case` |

---

## 🖼️ Скриншот

![image](https://github.com/user-attachments/assets/ca173ae0-2c0e-4405-b78f-24fd5c32bcd1)

---

## 📄 Лицензия

Проект лицензирован под MIT License. Подробности в файле `LICENSE`.