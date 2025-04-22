package com.alakey.discordbot.bot.command;

import com.alakey.discordbot.bot.audio.DeleteCommandTest;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private final String pathAudio;

    public CommandManager(String pathAudio, String telegramToken, String chatId) {
        this.pathAudio = pathAudio;

        commands.put("!delete", new DeleteCommandTest());
        commands.put("!speak", new SpeakCommand());
        commands.put("!record", new RecordCommand(telegramToken, chatId));
    }

    public void handleCommand(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!")) {
            String[] parts = message.split(" ", 2);

            String commandKey = parts[0];
            Command command = commands.get(commandKey);

            if (command != null) {
                command.execute(event);
            } else {
                String fileName = commandKey.substring(1) + ".mp3";
                String voiceChannelName = (parts.length > 1) ? parts[1].trim() : null;

                File audioFile = new File(pathAudio, fileName);

                if (audioFile.exists()) {
                    new PlayFileCommand(audioFile, voiceChannelName).execute(event);
                } else {
                    event.getChannel().sendMessage("Файл не найден: " + fileName).queue();
                }
            }
        }
    }
}
