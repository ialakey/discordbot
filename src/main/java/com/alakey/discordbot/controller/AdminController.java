package com.alakey.discordbot.controller;

import com.alakey.discordbot.bot.audio.AudioPlayerSendHandler;
import com.alakey.discordbot.service.*;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import com.alakey.discordbot.bot.config.JdaConfig;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Value("${discord.bot.guildById}")
    private String guildById;
    @Value("${bot.audio-folder}")
    private String pathAudio;
    private final JdaConfig jdaConfig;
    private final BlockedEntityService blockedEntityService;
    private final GuildService guildService;
    private final AudioService audioService;
    private final TextToSpeechService textToSpeechService;
    private final String fragment = "admin";

    public AdminController(
            JdaConfig jdaConfig,
            BlockedEntityService blockedEntityService,
            GuildService guildService,
            AudioService audioService,
            TextToSpeechService textToSpeechService
    ) {
        this.jdaConfig = jdaConfig;
        this.blockedEntityService = blockedEntityService;
        this.guildService = guildService;
        this.audioService = audioService;
        this.textToSpeechService = textToSpeechService;
    }

    private Guild getGuild() {
        return jdaConfig.getJda().getGuildById(guildById);
    }

    @GetMapping
    public String adminPanel(Model model) {
        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("audioFiles", getAudioFiles());

        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());

        List<String> discordUsers = getAllDiscordUsers();
        List<String> discordRoles = getAllDiscordRoles();

        model.addAttribute("discordUsers", discordUsers);
        model.addAttribute("discordRoles", discordRoles);

        return fragment;
    }

    @PostMapping("/addBlockedRole")
    public String addBlockedRole(@RequestParam String roleName, Model model) {
        blockedEntityService.addBlockedRole(roleName);

        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());

        return fragment;
    }

    @PostMapping("/addBlockedUser")
    public String addBlockedUser(@RequestParam String username, Model model) {
        blockedEntityService.addBlockedName(username);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return fragment;
    }

    @PostMapping("/removeBlockedUser")
    public String removeBlockedUser(@RequestParam String username, Model model) {
        blockedEntityService.removeBlockedName(username);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return fragment;
    }

    @PostMapping("/removeBlockedRole")
    public String removeBlockedRole(@RequestParam String roleName, Model model) {
        blockedEntityService.removeBlockedRole(roleName);
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
        return fragment;
    }

    @GetMapping("/getAllDiscordUsers")
    public List<String> getAllDiscordUsers() {
        return guildService.getAllUsers();
    }

    @GetMapping("/getAllDiscordRoles")
    public List<String> getAllDiscordRoles() {
        return guildService.getAllRoles();
    }

    @GetMapping("/getAllVoiceChannels")
    public List<String> getAllVoiceChannels() {
        return guildService.getAllVoiceChannels();
    }

    @PostMapping("/playAndKick")
    public String playAudioAndKick(@RequestParam String voiceChannelName, Model model) {
        Guild guild = getGuild();
        if (guild == null) {
            model.addAttribute("error", "Гильдия не найдена.");
            return fragment;
        }

        VoiceChannel channel = guild.getVoiceChannelsByName(voiceChannelName, true)
                .stream().findFirst().orElse(null);

        if (channel == null) {
            model.addAttribute("error", "Голосовой канал не найден: " + voiceChannelName);
            return fragment;
        }

        File audioFile = new File(pathAudio+ "csgo.mp3");
        if (!audioFile.exists()) {
            model.addAttribute("error", "Аудиофайл не найден.");
            return fragment;
        }

        guild.getAudioManager().openAudioConnection(channel);

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItem(audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            for (Member member : channel.getMembers()) {
                                if (!member.getUser().isBot()) {
                                    guild.kickVoiceMember(member).queue();
                                }
                            }

                            guild.kickVoiceMember(guild.getSelfMember()).queue();
                        }
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    player.playTrack(playlist.getTracks().get(0));
                }
            }

            @Override
            public void noMatches() {
                log.warn("Трек не найден.");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log.error("Ошибка загрузки трека", exception);
            }
        });

        model.addAttribute("message", "Воспроизведение запущено.");

        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());

        return fragment;
    }

    @GetMapping("/getAudioFiles")
    public List<String> getAudioFiles() {
        File audioDir = new File(pathAudio);
        if (!audioDir.exists() || !audioDir.isDirectory()) return List.of();

        File[] files = audioDir.listFiles((dir, name) -> name.endsWith(".mp3"));
        if (files == null) return List.of();

        List<String> fileNames = new ArrayList<>();
        for (File file : files) {
            fileNames.add(file.getName());
        }

        return fileNames;
    }

    @PostMapping("/playAudioFile")
    public String playAudioFile(
            @RequestParam String voiceChannelName,
            @RequestParam String audioFileName,
            Model model) {

        Guild guild = guildService.getGuild();
        VoiceChannel channel = guildService.findVoiceChannel(voiceChannelName);
        File file = new File(pathAudio + audioFileName);

        if (guild == null || channel == null || !file.exists()) {
            model.addAttribute("error", "Ошибка воспроизведения");
        } else {
            audioService.playAudio(guild, channel, file, () -> guild.getAudioManager().closeAudioConnection());
            model.addAttribute("message", "Воспроизведение начато");
        }

        populateModel(model);
        return fragment;
    }

    private void populateModel(Model model) {
        model.addAttribute("voiceChannels", getAllVoiceChannels());
        model.addAttribute("audioFiles", getAudioFiles());
        model.addAttribute("blockedRoles", blockedEntityService.getBlockedRoles());
        model.addAttribute("blockedUsers", blockedEntityService.getBlockedNames());
        model.addAttribute("discordRoles", getAllDiscordRoles());
        model.addAttribute("discordUsers", getAllDiscordUsers());
    }

    @PostMapping("/speakText")
    public String speakText(
            @RequestParam String voiceChannelName,
            @RequestParam String textToSpeak,
            Model model
    ) {
        Guild guild = getGuild();
        if (guild == null) {
            model.addAttribute("error", "Гильдия не найдена.");
            populateModel(model);
            return fragment;
        }

        VoiceChannel channel = guild.getVoiceChannelsByName(voiceChannelName, true)
                .stream().findFirst().orElse(null);
        if (channel == null) {
            model.addAttribute("error", "Канал не найден: " + voiceChannelName);
            populateModel(model);
            return fragment;
        }

        File audioFile = textToSpeechService.synthesize(textToSpeak);
        if (audioFile == null || !audioFile.exists()) {
            model.addAttribute("error", "Не удалось создать аудиофайл.");
            populateModel(model);
            return fragment;
        }

        guild.getAudioManager().openAudioConnection(channel);

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        playerManager.loadItem(audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                player.addListener(new AudioEventAdapter() {
                    @Override
                    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                        if (endReason.mayStartNext) {
                            guild.getAudioManager().closeAudioConnection();
                        }
                    }
                });
            }

            @Override public void playlistLoaded(AudioPlaylist playlist) {}
            @Override public void noMatches() { log.warn("Файл не найден"); }
            @Override public void loadFailed(FriendlyException exception) { log.error("Ошибка загрузки", exception); }
        });

        model.addAttribute("message", "Сообщение озвучено.");
        populateModel(model);
        return fragment;
    }
}
