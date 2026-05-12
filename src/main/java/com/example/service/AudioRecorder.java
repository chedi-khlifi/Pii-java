package com.example.service;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioRecorder {
    private AudioFormat format;
    private TargetDataLine line;
    private File wavFile;
    private boolean isRecording = false;

    public AudioRecorder(String fileName) {
        this.wavFile = new File(fileName);
        this.format = new AudioFormat(16000, 16, 1, true, false);
    }

    public void start() {
        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Line not supported");
                    return;
                }
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                isRecording = true;
                AudioInputStream ais = new AudioInputStream(line);
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
            } catch (LineUnavailableException | IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        isRecording = false;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    public File getWavFile() {
        return wavFile;
    }
}
