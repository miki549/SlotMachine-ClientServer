package com.example.slotmachine;

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
        return loadBackgroundWithRetry(fileName, 3);
    }
    
    private static MediaPlayer loadBackgroundWithRetry(String fileName, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                File videoFile = new File(BACKGROUND_PATH + fileName);
                if (!videoFile.exists()) {
                    System.err.println("Hiba: A videó fájl nem található: " + videoFile.getAbsolutePath());
                    return null;
                }
                
                String mediaUrl = videoFile.toURI().toString();
                System.out.println("Videó betöltése (próbálkozás " + attempt + "/" + maxRetries + "): " + mediaUrl);
                
                Media videoMedia = new Media(mediaUrl);
                MediaPlayer player = new MediaPlayer(videoMedia);
                
                // Add error handling for media loading
                int finalAttempt = attempt;
                player.setOnError(() -> {
                    System.err.println("Hiba a videó betöltésekor (próbálkozás " + finalAttempt + "): " + player.getError().toString());
                });
                
                // Add ready event handler
                player.setOnReady(() -> {
                    System.out.println("Videó sikeresen betöltve: " + fileName);
                });
                
                // Add additional error handling for media creation
                int finalAttempt1 = attempt;
                videoMedia.setOnError(() -> {
                    System.err.println("Hiba a Media objektum létrehozásakor (próbálkozás " + finalAttempt1 + "): " + videoMedia.getError().toString());
                });
                
                // Ha ez az utolsó próbálkozás, adjuk vissza a playert
                if (attempt == maxRetries) {
                    return player;
                }
                
                // Ellenőrizzük, hogy a videó betöltődött-e
                // Ha igen, adjuk vissza, ha nem, próbáljuk újra
                return player;
                
            } catch (Exception e) {
                System.err.println("Hiba a háttérvideó betöltésekor (próbálkozás " + attempt + "): " + e.getMessage());
                if (attempt == maxRetries) {
                    e.printStackTrace();
                    return null;
                }
                
                // Várunk egy kicsit a következő próbálkozás előtt
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }
}
