package com.example.slotmachine;

import com.example.slotmachine.client.ServerConfigDialog;
import com.example.slotmachine.client.LoginDialog;
import com.example.slotmachine.client.ApiClient;
import com.example.slotmachine.server.dto.LoginResponse;
import java.util.prefs.Preferences;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;

import static com.example.slotmachine.ConfigManager.get;

public class MainMenu extends Application {

    private Stage settingsStage;
    private static MediaPlayer mainMenuMusic;
    
    // Static variable to track if settings dialog is open
    private static boolean isSettingsDialogOpen = false;
    private MediaPlayer buttonClickSound;
    private MediaPlayer backgroundVideo, introVideo, loopVideo;
    private static boolean introPlayed = false;
    private static boolean videoInitialized = false;
    private static MediaPlayer staticBackgroundVideo, staticIntroVideo, staticLoopVideo;
    private double volume = 0.2;
    public String currentWindowSize = "small"; // Default window size

    @Override
    public void start(Stage primaryStage) {
        // Main menu music
        if (mainMenuMusic == null) {
            mainMenuMusic = ResourceLoader.loadSound("menumusic.mp3", volume);
            mainMenuMusic.setCycleCount(MediaPlayer.INDEFINITE);
            mainMenuMusic.play();
        }
        buttonClickSound = ResourceLoader.loadSound("buttonclick1.mp3", volume);

        // Videók betöltése - csak egyszer inicializáljuk
        if (!videoInitialized) {
            System.out.println("Videók betöltése...");
            introVideo = ResourceLoader.loadBackground("intro.mp4");
            loopVideo = ResourceLoader.loadBackground("loop.mp4");

            // Check if videos loaded successfully
            if (introVideo == null || loopVideo == null) {
                System.err.println("Nem sikerült betölteni a háttérvideókat, fallback háttér használata");
                backgroundVideo = null;
                introVideo = null;
                loopVideo = null;
            } else {
                // Statikus referenciák mentése
                staticIntroVideo = introVideo;
                staticLoopVideo = loopVideo;
                staticBackgroundVideo = introVideo; // Kezdetben az intro videó a háttér
                videoInitialized = true;
            }
        } else {
            // Ha már inicializálva volt, használjuk a statikus referenciákat
            introVideo = staticIntroVideo;
            loopVideo = staticLoopVideo;
        }

        if (introVideo != null && loopVideo != null) {
            // Add error handling for both videos
            introVideo.setOnError(() -> {
                System.err.println("Hiba az intro videó betöltésekor: " + introVideo.getError().toString());
                // Ha az intro videó hibás, próbáljuk meg újra betölteni
                System.out.println("Újrapróbálkozás az intro videó betöltésével...");
                MediaPlayer retryIntro = ResourceLoader.loadBackground("intro.mp4");
                if (retryIntro != null) {
                    introVideo = retryIntro;
                    introVideo.setCycleCount(1);
                    if (!introPlayed) {
                        backgroundVideo = introVideo;
                        introVideo.play();
                    }
                }
            });
            
            loopVideo.setOnError(() -> {
                System.err.println("Hiba a loop videó betöltésekor: " + loopVideo.getError().toString());
                // Ha a loop videó hibás, próbáljuk meg újra betölteni
                System.out.println("Újrapróbálkozás a loop videó betöltésével...");
                MediaPlayer retryLoop = ResourceLoader.loadBackground("loop.mp4");
                if (retryLoop != null) {
                    loopVideo = retryLoop;
                    loopVideo.setCycleCount(MediaPlayer.INDEFINITE);
                }
            });
            System.out.println("Videók sikeresen betöltve, inicializálás...");
            
            // Inicializálás előtt várjuk meg, amíg a videók készen állnak
            introVideo.setOnReady(() -> {
                System.out.println("Intro videó készen áll");
                introVideo.setCycleCount(1);
            });
            
            loopVideo.setOnReady(() -> {
                System.out.println("Loop videó készen áll");
                loopVideo.setCycleCount(MediaPlayer.INDEFINITE);
            });

            // Ellenőrizzük, hogy az intro már lejátszódott-e korábban
            if (!introPlayed) {
                System.out.println("Intro videó indítása...");
                backgroundVideo = introVideo;

                // Ha az első videó véget ér, induljon a második és állítsuk be háttérnek is
                introVideo.setOnEndOfMedia(() -> {
                    System.out.println("Intro videó lejátszva, loop videó indítása...");
                    introPlayed = true;
                    backgroundVideo = loopVideo;
                    staticBackgroundVideo = loopVideo; // Statikus referenciát is frissítjük
                    loopVideo.play();

                    // A MediaView frissítése az új videóra
                    Platform.runLater(() -> {
                        try {
                            MediaView view = (MediaView) ((StackPane) ((BorderPane) primaryStage.getScene().getRoot()).getCenter()).getChildren().get(0);
                            view.setMediaPlayer(backgroundVideo);
                            System.out.println("MediaView sikeresen frissítve");
                        } catch (Exception e) {
                            System.err.println("Hiba a MediaView frissítésekor: " + e.getMessage());
                        }
                    });
                });

                // Csak akkor indítsuk el, ha még nem fut
                if (introVideo.getStatus() != MediaPlayer.Status.PLAYING) {
                    introVideo.play();
                }
            } else {
                System.out.println("Loop videó indítása...");
                backgroundVideo = loopVideo;
                staticBackgroundVideo = loopVideo; // Statikus referenciát is frissítjük
                // Csak akkor indítsuk el, ha még nem fut
                if (loopVideo.getStatus() != MediaPlayer.Status.PLAYING) {
                    loopVideo.play();
                }
            }
        }

        // Main menu UI
        BorderPane mainMenu = createMainMenu(primaryStage);

        // Create scene for the main menu
        Scene mainMenuScene = new Scene(mainMenu, ConfigManager.get("MMWidth"), ConfigManager.get("MMHeight"));
        mainMenuScene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");

        primaryStage.setTitle("Lucky Sphinx");
        primaryStage.setScene(mainMenuScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // Segédfüggvény a ButtonClickSound beállításához
    private void addSoundToWidget(Control obj) {
        obj.setOnMousePressed(_ -> {
            buttonClickSound.stop();
            buttonClickSound.seek(Duration.ZERO);
            buttonClickSound.play();
        });
    }
    private BorderPane createMainMenu(Stage primaryStage) {
        // Background
        StackPane mainPane = new StackPane();

        // Add background (video or fallback)
        if (backgroundVideo != null) {
            MediaView videoView = new MediaView(backgroundVideo);
            videoView.setFitWidth(ConfigManager.get("MMWidth"));
            videoView.setFitHeight(ConfigManager.get("MMHeight"));
            videoView.setPreserveRatio(false);
            mainPane.getChildren().add(videoView);
        } else if (staticBackgroundVideo != null) {
            // Ha nincs lokális backgroundVideo, használjuk a statikus referenciát
            MediaView videoView = new MediaView(staticBackgroundVideo);
            videoView.setFitWidth(ConfigManager.get("MMWidth"));
            videoView.setFitHeight(ConfigManager.get("MMHeight"));
            videoView.setPreserveRatio(false);
            mainPane.getChildren().add(videoView);
        } else {
            // Add fallback background
            Rectangle fallbackBackground = new Rectangle(
                ConfigManager.get("MMWidth"),
                ConfigManager.get("MMHeight"),
                Color.rgb(0, 0, 0, 0.8)
            );
            mainPane.getChildren().add(fallbackBackground);
        }

        // Gombok létrehozása
        Button playButton = new Button("Play");
        playButton.getStyleClass().add("settings-button");
        addSoundToWidget(playButton);

        playButton.setStyle(String.format("-fx-font-size: %dpx;-fx-padding: %dpx %dpx;", get("MMPlayButtonFontSize"),get("MMPlayButtonPaddingY"),get("MMPlayButtonPaddingX")));
        playButton.setPrefSize(ConfigManager.get("MMPlayButtonWidth"), ConfigManager.get("MMPlayButtonHeight"));
        playButton.setOnAction(_ -> showLoginDialog(primaryStage));

        Button settingsButton = new Button("Settings");
        settingsButton.getStyleClass().add("settings-button");
        addSoundToWidget(settingsButton);
        settingsButton.setStyle(String.format("-fx-font-size: %dpx;-fx-padding: %dpx %dpx;", get("MMSettingsButtonFontSize"),get("MMSettingsButtonPaddingY"),get("MMSettingsButtonPaddingX")));
        settingsButton.setPrefSize(ConfigManager.get("MMSettingsButtonWidth"), ConfigManager.get("MMSettingsButtonHeight"));
        settingsButton.setOnAction(_ -> showSettings(primaryStage));

        // Felső gomb elhelyezése
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(
                ConfigManager.get("MMSettingsButtonPaddingV"),
                ConfigManager.get("MMSettingsButtonPaddingV1"),
                ConfigManager.get("MMSettingsButtonPaddingV2"),
                ConfigManager.get("MMSettingsButtonPaddingV3")
        ));
        mainPane.getChildren().add(settingsButton);

        // Alsó gomb elhelyezése
        StackPane.setAlignment(playButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(playButton, new Insets(
                ConfigManager.get("MMPlayButtonPaddingV"),
                ConfigManager.get("MMPlayButtonPaddingV1"),
                ConfigManager.get("MMPlayButtonPaddingV2"),
                ConfigManager.get("MMPlayButtonPaddingV3")
        ));
        mainPane.getChildren().add(playButton);

        // BorderPane létrehozása a végső elrendezéshez
        BorderPane menuLayout = new BorderPane();
        menuLayout.setCenter(mainPane);

        return menuLayout;
    }

    private void showSettings(Stage primaryStage) {  // Add primaryStage parameter
        // Check if login dialog is open - if so, don't allow settings to open
        if (LoginDialog.isLoginDialogOpen()) {
            return;
        }
        
        if(settingsStage != null) {
            settingsStage.show();
            settingsStage.toFront();
            return;
        }
        
        // Set flag to indicate settings dialog is open
        isSettingsDialogOpen = true;
        settingsStage = new Stage();
        settingsStage.initStyle(StageStyle.TRANSPARENT);
        settingsStage.initModality(Modality.NONE);
        settingsStage.initOwner(primaryStage);  // Set the owner stage

        Label percentageLabel = new Label(String.format("Volume: %d%%", (int) (mainMenuMusic.getVolume() * 100)));
        percentageLabel.setStyle(String.format("-fx-font-size: %dpx;", get("MMVolumeLabelFontSize")));
        percentageLabel.setPrefWidth(get("MMVolumeLabelWidth"));
        percentageLabel.getStyleClass().add("dialog-label");

        Slider volumeSlider = new Slider(0, 1, volume);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setStyle(String.format("-fx-font-size: %dpx", get("MMVolumeSliderFontSize")));
        volumeSlider.setPrefSize(get("MMVolumeSliderWidth"), get("MMVolumeSliderHeight"));
        volumeSlider.getStyleClass().add("dialog-slider");
        volumeSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == 0) return "0%";
                if (value == 1) return "100%";
                return "";
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        //hang hozzáadása
        addSoundToWidget(volumeSlider);

        // Window size controls
        Label windowSizeLabel = new Label("Window Size:");
        windowSizeLabel.setStyle(String.format("-fx-font-size: %dpx", get("MMWindowSizeLabelFontSize")));
        windowSizeLabel.getStyleClass().add("dialog-label");

        ComboBox<String> windowSizeCombo = new ComboBox<>();
        windowSizeCombo.getItems().addAll("Normal", "Small");
        windowSizeCombo.getStyleClass().add("combo-box");

        //hang hozzáadása
        addSoundToWidget(windowSizeCombo);
        windowSizeCombo.setCellFactory(_ -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            };
            cell.setOnMousePressed(_ -> {
                buttonClickSound.setVolume(volume);
                buttonClickSound.stop();
                buttonClickSound.seek(Duration.ZERO);
                buttonClickSound.play();
            });

            return cell;
        });


        windowSizeCombo.setValue(currentWindowSize.substring(0, 1).toUpperCase() + currentWindowSize.substring(1));
        windowSizeCombo.setStyle(String.format("-fx-font-size: %dpx", get("MMComboBoxFontSize")));


        // Server configuration button
        Button serverConfigButton = new Button("Server Settings");
        serverConfigButton.getStyleClass().add("server-config-button");
        addSoundToWidget(serverConfigButton);
        serverConfigButton.setStyle(String.format("-fx-font-size: %dpx;", get("MMComboBoxFontSize")));
        serverConfigButton.setOnAction(_ -> {
            ServerConfigDialog serverDialog = new ServerConfigDialog();
            serverDialog.showAndWait(settingsStage);
        });

        // Create HBox for volume control (label + slider)
        HBox volumeControl = new HBox(get("SettingsVolumeSpacing"));
        volumeControl.setAlignment(Pos.CENTER_LEFT);
        volumeControl.getChildren().addAll(percentageLabel, volumeSlider);
        
        // Create HBox for window size control (label + combo box)
        HBox windowSizeControl = new HBox(get("SettingsWindowSizeSpacing"));
        windowSizeControl.setAlignment(Pos.CENTER_LEFT);
        windowSizeControl.getChildren().addAll(windowSizeLabel, windowSizeCombo);
        
        // Create HBox for server settings (empty space + button to align with other controls)
        HBox serverControl = new HBox(get("SettingsServerSpacing"));
        serverControl.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        spacer.setPrefWidth(windowSizeLabel.getPrefWidth());
        serverControl.getChildren().addAll(spacer, serverConfigButton);
        
        // Create VBox for all controls with smaller spacing
        VBox allControls = new VBox(12);
        allControls.setAlignment(Pos.CENTER_LEFT);
        allControls.getChildren().addAll(volumeControl, windowSizeControl, serverControl);

        volumeSlider.valueProperty().addListener((_, _, newValue) -> {
            volume = newValue.doubleValue();
            mainMenuMusic.setVolume(volume);
            percentageLabel.setText(String.format("Volume: %d%%", (int) (volume * 100))); // Százalék frissítése
        });

        windowSizeCombo.setOnAction(_ -> {
            String newSize = windowSizeCombo.getValue().toLowerCase();
            if (!newSize.equals(currentWindowSize)) {
                currentWindowSize = newSize;
                ConfigManager.setConfigFile(currentWindowSize + "config.properties");

                // Close settings window
                settingsStage.close();
                settingsStage = null;

                // Stop current videos before restart
                if (backgroundVideo != null) {
                    backgroundVideo.stop();
                }
                if (introVideo != null) {
                    introVideo.stop();
                }
                if (loopVideo != null) {
                    loopVideo.stop();
                }

                // Reset intro played flag so intro video plays again after window size change
                introPlayed = false;

                // Restart the application with the new window size
                Platform.runLater(() -> {
                    primaryStage.close();
                    Stage newStage = new Stage();
                    newStage.setResizable(false);
                    start(newStage);
                });
            }
        });

        Button closeButton = new Button();
        closeButton.getStyleClass().add("dialog-close-button");
        addSoundToWidget(closeButton);
        closeButton.setPrefSize(ConfigManager.get("MMSettingsCloseButtonWidth"), ConfigManager.get("MMSettingsCloseButtonHeight"));
        closeButton.setOnAction(_ -> {
            settingsStage.close();
            settingsStage = null;
        });
        //Escape-re kilépés
        settingsStage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            settingsStage.close();
            settingsStage = null;
        });

        StackPane root = new StackPane();

        VBox settingsLayout = new VBox(20);
        settingsLayout.getChildren().add(allControls);
        settingsLayout.setStyle("-fx-background-color: #333; " +
                "-fx-border-color: #ffd700; " +
                "-fx-border-width: 2px; " +
                "-fx-padding: 20px; " +
                "-fx-spacing: 10px;");
        settingsLayout.setAlignment(Pos.CENTER);
        settingsLayout.setOpacity(0);
        settingsLayout.setEffect(new DropShadow(100, Color.BLACK));

        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(ConfigManager.get("MMSettingsCloseButtonPaddingV"), ConfigManager.get("MMSettingsCloseButtonPaddingV1"), ConfigManager.get("MMSettingsCloseButtonPaddingV2"), ConfigManager.get("MMSettingsCloseButtonPaddingV3")));

        root.getChildren().addAll(settingsLayout, closeButton);

        Scene settingsScene = new Scene(root, ConfigManager.get("MMSettingSceneWidth"), ConfigManager.get("MMSettingSceneHeight"));
        settingsScene.setFill(Color.TRANSPARENT);
        settingsScene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");
        settingsLayout.getStyleClass().add("dialog-vbox");

        Rectangle clip = new Rectangle(ConfigManager.get("MMSettingSceneWidth"), ConfigManager.get("MMSettingSceneHeight"));
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        root.setClip(clip);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), settingsLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        settingsStage.setScene(settingsScene);
        
        // Add listener to reset flag when dialog is closed
        settingsStage.setOnHidden(e -> {
            isSettingsDialogOpen = false;
        });
        
        settingsStage.show();

        // Center the settings window after it's visible
        Platform.runLater(() -> Funtions.centerStage(settingsStage, primaryStage));
    }

    private void showLoginDialog(Stage primaryStage) {
        // Check if login dialog is already open - if so, don't allow another one
        if (LoginDialog.isLoginDialogOpen()) {
            return;
        }
        
        // Check if settings dialog is open - if so, don't allow login to open
        if (isSettingsDialogOpen) {
            return;
        }
        
        // Check if server config dialog is open - if so, don't allow login to open
        if (ServerConfigDialog.isServerConfigDialogOpen()) {
            return;
        }
        
        // Get server configuration without creating dialog
        String serverUrl = getServerUrlFromPreferences();
        ApiClient apiClient = new ApiClient(serverUrl);
        
        // Show login dialog
        LoginDialog loginDialog = new LoginDialog(apiClient);
        LoginResponse loginResponse = loginDialog.showAndWait(primaryStage);
        
        if (loginResponse != null) {
            // Successful login - transition to game
            transitionToGame(primaryStage, apiClient, loginResponse);
        }
        // If loginResponse is null, user cancelled - stay in main menu
    }

    private void transitionToGame(Stage primaryStage, ApiClient apiClient, LoginResponse loginResponse) {
        // Don't stop the music here - let SlotMachineGUI handle it after successful login
        try {
            SlotMachineGUI slotMachineGUI = new SlotMachineGUI();
            slotMachineGUI.setInitialVolume(volume);
            slotMachineGUI.startWithLogin(primaryStage, apiClient, loginResponse);
        } catch (Exception e) {
            System.err.println("Error starting SlotMachineGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Static method to stop main menu music from other classes
    public static void stopMainMenuMusic() {
        if (mainMenuMusic != null) {
            Timeline fadeOut = new Timeline(new KeyFrame(
                    Duration.seconds(1),
                    new KeyValue(mainMenuMusic.volumeProperty(), 0)
            ));
            fadeOut.setOnFinished(_ -> {
                mainMenuMusic.stop();
                mainMenuMusic = null;
            });
            fadeOut.play();
        }
    }
    
    // Static method to check if settings dialog is open
    public static boolean isSettingsDialogOpen() {
        return isSettingsDialogOpen;
    }
    
    private String getServerUrlFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(ServerConfigDialog.class);
        String serverUrl = prefs.get("server_url", "http://46.139.211.149:8081");
        return serverUrl;
    }

    @Override
    public void stop() {
        // Ne állítsuk le a statikus videókat, csak a lokális referenciákat
        if (backgroundVideo != null && backgroundVideo != staticBackgroundVideo) {
            backgroundVideo.stop();
        }
        if (mainMenuMusic != null) {
            mainMenuMusic.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}