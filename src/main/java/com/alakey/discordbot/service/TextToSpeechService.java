package com.alakey.discordbot.service;

import java.io.File;

public interface TextToSpeechService {
    File synthesize(String text);
}
