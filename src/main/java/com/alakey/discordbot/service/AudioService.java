package com.alakey.discordbot.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.io.File;

public interface AudioService {
    void playAudio(Guild guild, VoiceChannel channel, File audioFile, Runnable onEndCallback);
}
