package com.alakey.discordbot.bot.command;

import com.alakey.discordbot.bot.audio.DeleteCommandTest;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();
    private final String pathAudio;

    public CommandManager(String pathAudio) {
        this.pathAudio = pathAudio;

        commands.put("!delete", new DeleteCommandTest());
        commands.put("!speak", new SpeakCommand());
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
                File audioFile = new File(pathAudio, fileName);

                if (audioFile.exists()) {
                    new PlayFileCommand(audioFile).execute(event);
                } else {
                    event.getChannel().sendMessage("Файл не найден: " + fileName).queue();
                }
            }
        }
    }
}
