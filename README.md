
# [Discord Bot App](https://discordbot-production-0d7a.up.railway.app/)

Этот проект представляет собой приложение для Discord-бота, которое контейнеризовано с использованием Docker.

## Структура проекта

- **Dockerfile**: Файл для сборки Docker-образа.
- **swagger-ui**: Интерфейс для работы с API через Swagger.

## Установка и запуск

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/your-username/discordbot-app.git
   cd discordbot-app
   ```

2. Построение Docker-образа:
   ```bash
   docker build -t discordbot-app .
   ```

3. Запуск контейнера:
   ```bash
   docker run -p 8080:8080 discordbot-app
   ```

4. Перейдите по следующему URL в вашем браузере, чтобы проверить работу приложения:
    - Основной интерфейс: [http://localhost:8080](http://localhost:8080)
    - Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Примечания

- Для работы с API через Swagger UI вы можете использовать [Swagger Docs](http://localhost:8080/swagger-ui/index.html), чтобы быстро ознакомиться с возможностями бота.
- Убедитесь, что Docker установлен и запущен на вашем устройстве.

## Лицензия

Этот проект лицензирован под MIT лицензией. Подробнее см. в файле LICENSE.

## Скриншот

![image](https://github.com/user-attachments/assets/ca173ae0-2c0e-4405-b78f-24fd5c32bcd1)
