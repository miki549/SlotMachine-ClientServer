package com.example.slotmachine.client;

import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class ResourceLoader {
    private static final String IMAGE_PATH = "src/main/resources/";
    private static final String SOUND_PATH = "src/main/resources/sounds/";
    private static final String BACKGROUND_PATH = "src/main/resources/backgrounds/";

    public static Image loadImage(String fileName) {
        return new Image("file:" + IMAGE_PATH + fileName);
    }

    public static MediaPlayer loadSound(String fileName, double volume) {
        Media media = new Media(new File(SOUND_PATH + fileName).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volume);
        return mediaPlayer;
    }
    public static MediaPlayer loadBackground(String fileName) {
        return loadBackgroundWithRetry(fileName);
    }
    
    private static MediaPlayer loadBackgroundWithRetry(String fileName) {
        MediaPlayer lastPlayer = null;
        
        for (int attempt = 1; true; attempt++) {
            try {
                // Dispose previous player if exists
                if (lastPlayer != null) {
                    try {
                        lastPlayer.stop();
                        lastPlayer.dispose();
                    } catch (Exception e) {
                        // Ignore disposal errors
                    }
                    lastPlayer = null;
                }
                
                File videoFile = new File(BACKGROUND_PATH + fileName);
                if (!videoFile.exists()) {
                    System.err.println("Hiba: A video fajl nem talalhato: " + videoFile.getAbsolutePath());
                    return null;
                }
                
                String mediaUrl = videoFile.toURI().toString();
                System.out.println("Video betoltese (probalas " + attempt + "/" + 3 + "): " + mediaUrl);
                
                // Create new Media and MediaPlayer objects for each attempt
                Media videoMedia = new Media(mediaUrl);
                MediaPlayer player = new MediaPlayer(videoMedia);
                
                // Add error handling for media loading
                int finalAttempt = attempt;
                player.setOnError(() -> System.err.println("Hiba a video betoltese kor (probalas " + finalAttempt + "): " + player.getError().toString()));
                
                // Add ready event handler
                player.setOnReady(() -> System.out.println("Video sikeresen betoltve: " + fileName));
                
                // Add additional error handling for media creation
                int finalAttempt1 = attempt;
                videoMedia.setOnError(() -> System.err.println("Hiba a Media objektum letrehozasakor (probalas " + finalAttempt1 + "): " + videoMedia.getError().toString()));
                
                // Store reference for potential disposal
                lastPlayer = player;
                
                // Wait a bit for the player to initialize
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
                // Check if player is in a valid state
                if (player.getStatus() != MediaPlayer.Status.UNKNOWN && player.getStatus() != MediaPlayer.Status.HALTED) {
                    return player;
                }
                
                // If this is the last attempt, return the player anyway
                if (attempt == 3) {
                    return player;
                }
                
                // Wait before next attempt
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
            } catch (Exception e) {
                System.err.println("Hiba a hattervideo betoltese kor (probalas " + attempt + "): " + e.getMessage());
                if (attempt == 3) {
                    e.printStackTrace();
                    return null;
                }
                
                // Várunk egy kicsit a következő próbálkozás előtt
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }
}
