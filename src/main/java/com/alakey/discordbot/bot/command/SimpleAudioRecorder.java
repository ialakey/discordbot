package com.alakey.discordbot.bot.command;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SimpleAudioRecorder implements AudioReceiveHandler {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final String fileName;
    private final MessageChannel feedbackChannel;

    public SimpleAudioRecorder(String fileName, MessageChannel feedbackChannel) {
        this.fileName = fileName;
        this.feedbackChannel = feedbackChannel;
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    public void close() {
        try {
            saveToWavFile(outputStream.toByteArray());
        } catch (IOException e) {
            feedbackChannel.sendMessage("Не удалось сохранить аудио: " + e.getMessage()).queue();
        }
    }

    @Override
    public void handleCombinedAudio(net.dv8tion.jda.api.audio.CombinedAudio combinedAudio) {
        byte[] audioData = combinedAudio.getAudioData(1.0);

        try {
            outputStream.write(audioData);
        } catch (IOException e) {
            feedbackChannel.sendMessage("Ошибка записи аудио: " + e.getMessage()).queue();
        }
    }

    private void saveToWavFile(byte[] rawData) throws IOException {
        AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, true);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
             AudioInputStream ais = new AudioInputStream(bais, format, rawData.length / format.getFrameSize());
             FileOutputStream fos = new FileOutputStream(fileName)) {

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fos);
        }
    }
}
