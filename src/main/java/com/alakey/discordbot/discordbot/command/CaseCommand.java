package com.alakey.discordbot.discordbot.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class CaseCommand implements Command {
    private final String apiUrl;
    private final String chatId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaseCommand(String telegramToken, String chatId) {
        this.apiUrl = "https://api.telegram.org/bot" + telegramToken + "/";
        this.chatId = chatId;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getMessage().delete().queue();

        String telegramMessage = "Кто пойдет за кейсом?" + "\n" + buildTelegramMentions();
        sendMessageToTelegram(telegramMessage);

        String discordMessage = "Кто пойдет за кейсом? @everyone";
        event.getChannel().sendMessage(discordMessage).queue();
    }

    private void sendMessageToTelegram(String messageText) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("chat_id", chatId);
            payload.put("text", messageText);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "sendMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildTelegramMentions() {
        return String.join(" ", getTelegramUsernames());
    }

    private List<String> getTelegramUsernames() {
        return List.of(
                "@z3r01n9",
                "@Walker6054",
                "@oxxxsytop",
                "@literallyAlan",
                "@Romulq",
                "@Mikhailq_gg",
                "@van_gubin",
                "@lIIllIIlIlI",
                "@Yuity31",
                "@i_alakey"
        );
    }
}