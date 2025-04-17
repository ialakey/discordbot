package com.alakey.discordbot.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.List;

public interface GuildService {
    Guild getGuild();
    VoiceChannel findVoiceChannel(String name);
    List<String> getAllVoiceChannels();
    List<String> getAllRoles();
    List<String> getAllUsers();
}
