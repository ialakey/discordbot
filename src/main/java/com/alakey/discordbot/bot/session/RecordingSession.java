package com.alakey.discordbot.bot.session;

import com.alakey.discordbot.bot.command.SimpleAudioRecorder;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

public class RecordingSession {
    private static RecordingSession instance;

    private SimpleAudioRecorder recorder;
    private ScheduledExecutorService scheduler;
    private AudioManager audioManager;
    private File wavFile;

    private RecordingSession() {}

    public static synchronized RecordingSession getInstance() {
        if (instance == null) {
            instance = new RecordingSession();
        }
        return instance;
    }

    public void start(SimpleAudioRecorder recorder, ScheduledExecutorService scheduler, AudioManager audioManager, File wavFile) {
        this.recorder = recorder;
        this.scheduler = scheduler;
        this.audioManager = audioManager;
        this.wavFile = wavFile;
    }

    public void stop() {
        if (audioManager != null) {
            audioManager.closeAudioConnection();
        }
        if (recorder != null) {
            recorder.close();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public File getWavFile() {
        return wavFile;
    }

    public boolean isActive() {
        return recorder != null;
    }

    public void clear() {
        recorder = null;
        scheduler = null;
        audioManager = null;
        wavFile = null;
    }
}
