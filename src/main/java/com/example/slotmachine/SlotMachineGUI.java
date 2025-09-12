package com.example.slotmachine;

import com.example.slotmachine.client.ApiClient;
import com.example.slotmachine.client.ServerConfigDialog;
import com.example.slotmachine.client.LoginDialog;
import com.example.slotmachine.server.dto.LoginResponse;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.Media;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.slotmachine.ConfigManager.get;
import static com.example.slotmachine.Funtions.centerStage;


import static com.example.slotmachine.GameSettings.*;

public class SlotMachineGUI extends Application {

    enum SpinMode {
        NORMAL,
        QUICK,
        TURBO
    }

    private SpinMode spinMode = SpinMode.NORMAL;
    private int autoSpinCount = 20;
    private final Image[] symbols = new Image[SYMBOL_COUNT];
    private final ImageView[][] reels = new ImageView[GRID_SIZE][GRID_SIZE];
    private final BorderPane root = new BorderPane();
    private SlotMachine game;
    private ApiClient apiClient;
    private boolean disableButton = false;
    private boolean isSpinning = false;
    private Button spinButton ;
    private final Button increaseBetButton = createBetButton("file:src/main/resources/buttons/bet/plus.png");
    private final Button decreaseBetButton = createBetButton("file:src/main/resources/buttons/bet/minus.png");
    private final Button autoplaySettingsButton = new Button();
    private final Button buyBonusButton = createBonusButton();
    private final Button volumeButton = new Button();
    private Text balanceText = new Text("Credit: $0");
    private final Text winText = new Text("");
    private final Random random = new Random();

    private MediaPlayer gameMusic;
    private MediaPlayer buttonClickSound, hitSound;
    private MediaPlayer[] spinSounds, fallSounds;
    private double initialVolume = 1.0;
    private int spinsRemaining;
    private SpinParameters spinParams;
    // CreditLoader már nem szükséges online módban
    // private final CreditLoader creditLoader = new CreditLoader(game);
    private Stage autoSpinSettingsStage, volumeSettingsStage, lowBalanceStage;
    private MediaPlayer bonusBackgroundMusic;
    private MediaPlayer bonusTriggerSound;
    private MediaPlayer retriggerSound;
    private Stage bonusResultsStage;
    private boolean isBonusMode = false;
    private Timeline balancePollingTimer;

    private static class SpinParameters {
        int pauseDelay,totalCycles,cycleDuration,transitionDuration;
        double maxBlurRadius;

        SpinParameters(int pauseDelay, int totalCycles, int cycleDuration, int transitionDuration, double maxBlurRadius) {
            this.pauseDelay = pauseDelay;
            this.totalCycles = totalCycles;
            this.cycleDuration = cycleDuration;
            this.transitionDuration = transitionDuration;
            this.maxBlurRadius = maxBlurRadius;
        }
    }
    @Override
    public void start(Stage primaryStage) {
        // Inicializálás - használjuk a mentett szerver konfigurációt
        ServerConfigDialog serverConfig = new ServerConfigDialog();
        String serverUrl = serverConfig.getServerUrl();
        apiClient = new ApiClient(serverUrl);
        
        // Bejelentkezési dialógus megjelenítése
        LoginDialog loginDialog = new LoginDialog(apiClient);
        LoginResponse loginResponse = loginDialog.showAndWait(primaryStage);
        
        if (loginResponse == null) {
            // Felhasználó lemondta a bejelentkezést - vissza a főmenübe
            try {
                MainMenu mainMenu = new MainMenu();
                mainMenu.start(primaryStage);
            } catch (Exception e) {
                Platform.exit();
            }
            return;
        }
        
        // SlotMachine inicializálása online módban
        game = new SlotMachine(apiClient);
        
        // Balance beállítása közvetlenül a login response-ból
        game.setBalance(loginResponse.getBalance());
        
        // Balance szöveg frissítése
        balanceText.setText("Credit: $" + loginResponse.getBalance().intValue());
        
        // Balance update listener beállítása
        game.setBalanceUpdateListener(newBalance -> {
            Platform.runLater(() -> {
                balanceText.setText("Credit: $" + (int)newBalance);
                System.out.println("Balance real-time frissítés: $" + newBalance);
            });
        });
        
        // Polling timer indítása (5 másodpercenként)
        startBalancePolling();
        
        System.out.println("Bejelentkezve: " + loginResponse.getUsername() + 
                          ", Balance: $" + loginResponse.getBalance());

        gameMusic = ResourceLoader.loadSound("gamemusic.mp3",0);
        gameMusic.setCycleCount(MediaPlayer.INDEFINITE);
        gameMusic.play();

        Timeline fadeIn = new Timeline(new KeyFrame(
                Duration.seconds(2),
                new KeyValue(gameMusic.volumeProperty(), initialVolume)
        ));
        fadeIn.play();

        hitSound = ResourceLoader.loadSound("hit.mp3",initialVolume);
        buttonClickSound = ResourceLoader.loadSound("buttonclick1.mp3",initialVolume);

        spinSounds = new MediaPlayer[GRID_SIZE];
        fallSounds = new MediaPlayer[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            spinSounds[i] = ResourceLoader.loadSound("spin.mp3",initialVolume);
            fallSounds[i] = ResourceLoader.loadSound("fall.mp3",initialVolume);

        }

        // Load bonus sounds
        bonusBackgroundMusic = ResourceLoader.loadSound("doghouse.mp3", initialVolume);
        bonusTriggerSound = ResourceLoader.loadSound("s1.mp3", initialVolume);
        retriggerSound = ResourceLoader.loadSound("s2.mp3", initialVolume);

        loadSymbols();

        root.setId("root-pane");
        root.setStyle(String.format("-fx-background-size: %d %d;", get("BGImageWidth"), get("BGImageHeight")));

        GridPane slotGrid = new GridPane();
        slotGrid.setAlignment(Pos.CENTER);
        slotGrid.setPrefWidth(get("SymbolSize") * GRID_SIZE);
        slotGrid.setMaxWidth(get("SymbolSize") * GRID_SIZE);
        slotGrid.setPadding(new Insets(40, 0, 0, 0));
        slotGrid.setTranslateX(get("SlotGridTranslateX"));
        slotGrid.setTranslateY(get("SlotGridTranslateY"));

        // Add reels to the grid
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                reels[row][col] = new ImageView(symbols[random.nextInt(SYMBOL_COUNT)]);
                reels[row][col].setFitWidth(get("SymbolSize"));
                reels[row][col].setFitHeight(get("SymbolSize"));
                slotGrid.add(reels[row][col], col, row);
            }
        }

        // Create a clipping region to hide the top row of the slot grid
        Rectangle clip = new Rectangle(get("SymbolSize") * GRID_SIZE, get("SymbolSize") * GRID_SIZE+get("ClipCorrection"));
        clip.setTranslateY(get("ClipTranslateY")); // Move down by one symbol size to hide the top row
        slotGrid.setClip(clip);

        // Balance and Bet texts
        balanceText = new Text("Credit: $" + game.getBalance());
        balanceText.setStyle(String.format("-fx-font-size: %dpx;", get("BalanceTextFontSize")));
        balanceText.getStyleClass().add("balance-text");

        Text betText = new Text("Bet: $" + game.getBet());
        betText.setStyle(String.format("-fx-font-size: %dpx;", get("BetTextFontSize")));
        betText.getStyleClass().add("bet-text");

        HBox betTextContainer = new HBox(betText);
        betTextContainer.setPrefWidth(get("BetTextWidth"));
        betTextContainer.setAlignment(Pos.CENTER);

        // Buttons and styles
        spinButton = createSpinButton();
        spinButton.setOnAction(_ -> {
            if (!disableButton) {
                if (game.getBalance() >= game.getBet()) {
                    disableButtons(true);
                    autoplaySettingsButton.setDisable(true);
                    isSpinning = true;
                    startSpin(1);
                }
                else{
                    showLowBalanceMessage(primaryStage);
                }
            }
        });
        increaseBetButton.setOnAction(_ -> {
            game.increaseBet();
            betText.setText("Bet: $" + game.getBet());
        });
        decreaseBetButton.setOnAction(_ -> {
            game.decreaseBet();
            betText.setText("Bet: $" + game.getBet());
        });
        designAutoSpinButton();
        autoplaySettingsButton.getStyleClass().add("settings-button");
        autoplaySettingsButton.setStyle(String.format("-fx-font-size: %dpx ;-fx-padding: %dpx %dpx;", get("AutoplaySettingsButtonFontSize"),get("AutoPlaySettingsButtonPaddingY"), get("AutoPlaySettingsButtonPaddingX")));
        autoplaySettingsButton.setTranslateY(get("AutoplaySettingsButtonTranslateY"));
        autoplaySettingsButton.setOnAction(_ -> {
            if (isSpinning) {
                isSpinning = false;
                autoSpinCount = 0;
                designAutoSpinButton();
            } else {
                showAutoplaySettingsDialog(primaryStage);
            }
        });

        HBox betControls = new HBox(get("BetControlSpacing"), decreaseBetButton, betTextContainer, increaseBetButton);
        betControls.setAlignment(Pos.CENTER_LEFT);

        volumeButton.getStyleClass().add("volume-button");
        volumeButton.setPrefSize(get("VolumeButtonWidth"), get("VolumeButtonHeight"));
        volumeButton.setOnAction(_ -> showInGameSettings(primaryStage));
        BorderPane.setAlignment(volumeButton,Pos.TOP_RIGHT);
        BorderPane.setMargin(volumeButton,new Insets(get("VolumeButtonPaddingV"), get("VolumeButtonPaddingV1"), get("VolumeButtonPaddingV2"), get("VolumeButtonPaddingV3")));
        root.setTop(volumeButton);

        VBox leftPanel = new VBox(get("LeftPanelSpacing"), buyBonusButton);
        leftPanel.setAlignment(Pos.CENTER_LEFT);
        leftPanel.setPadding(new Insets(get("LeftPanelPaddingV"), get("LeftPanelPaddingV1"), get("LeftPanelPaddingV2"), get("LeftPanelPaddingV3")));

        VBox bottomLeftPanel = new VBox(get("BottomLeftPanelSpacing"), balanceText, betControls);
        bottomLeftPanel.setAlignment(Pos.BOTTOM_LEFT);
        bottomLeftPanel.setPadding(new Insets(get("BottomLeftPanelPaddingV"), get("BottomLeftPanelPaddingV1"), get("BottomLeftPanelPaddingV2"), get("BottomLeftPanelPaddingV3")));

        VBox bottomRightPanel = new VBox(get("BottomRightPanelSpacing"), autoplaySettingsButton, spinButton);
        bottomRightPanel.setAlignment(Pos.BOTTOM_RIGHT);
        bottomRightPanel.setPadding(new Insets(get("BottomRightPanelPaddingV"), get("BottomRightPanelPaddingV1"), get("BottomRightPanelPaddingV2"), get("BottomRightPanelPaddingV3")));

        // Stílus beállítás
        winText.getStyleClass().add("win-text");
        winText.setStyle(String.format("-fx-font-size: %dpx;", get("WinTextFontSize")));
        winText.setVisible(false);

        // Térköz a grid és a nyeremény szöveg között
        Region spacer = new Region();
        //spacer.setMinHeight(20);

        // Konténer a nyeremény szövegnek, középen a grid alatt
        HBox winContainer = new HBox(winText);
        winContainer.setAlignment(Pos.CENTER);
        winContainer.setPadding(new Insets(get("WinTextPaddingV"), get("WinTextPaddingV1"), get("WinTextPaddingV2"), get("WinTextPaddingV3")));

        // Grid alá helyezés
        VBox mainContainer = new VBox(slotGrid, spacer, winContainer);
        mainContainer.setAlignment(Pos.CENTER);

        root.setCenter(mainContainer);
        root.setLeft(leftPanel);
        root.setBottom(new BorderPane(bottomLeftPanel, null, bottomRightPanel, null, null));

        Scene scene = new Scene(root, get("GameWidth"), get("GameHeight"));
        scene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");


        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (!disableButton && !isSpinning) {
                    if(game.getBalance() >= game.getBet()) {
                        disableButtons(true);
                        isSpinning = true;
                        startSpin(1);
                    }
                    else {
                        showLowBalanceMessage(primaryStage);
                    }
                }
            }
        });
        // Gombok fókuszálhatóságának letiltása
        spinButton.setFocusTraversable(false);
        increaseBetButton.setFocusTraversable(false);
        decreaseBetButton.setFocusTraversable(false);
        autoplaySettingsButton.setFocusTraversable(false);
        buyBonusButton.setFocusTraversable(false);

        // Hangok hozzáadása a gombokhoz
        addSoundToWidget(spinButton);
        addSoundToWidget(increaseBetButton);
        addSoundToWidget(decreaseBetButton);
        addSoundToWidget(autoplaySettingsButton);
        addSoundToWidget(buyBonusButton);
        addSoundToWidget(volumeButton);

        primaryStage.setTitle("Lucky Sphinx");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        Platform.runLater(root::requestFocus);
    }

    // Credit fájl betöltés már nem szükséges online módban
    // private void showCreditFileChooser(Stage primaryStage) {
    //     FileChooser fileChooser = new FileChooser();
    //     fileChooser.setTitle("Select Credit File");
    //     fileChooser.getExtensionFilters().add(
    //             new FileChooser.ExtensionFilter("Credit Files", "*.cred")
    //     );
    // 
    //     // Alapértelmezett könyvtár beállítása
    //     File defaultDirectory = new File(System.getProperty("user.home") + "/Downloads");
    //     if (defaultDirectory.exists()) {
    //         fileChooser.setInitialDirectory(defaultDirectory);
    //     }
    // 
    //     File selectedFile = fileChooser.showOpenDialog(primaryStage);
    // 
    //     if (selectedFile != null) {
    //         creditLoader.loadCreditFile(selectedFile, this::showDialog);
    //         balanceText.setText("Credit: $" + game.getBalance());
    //     }
    // }

    private void showDialog(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showAutoplaySettingsDialog(Stage primaryStage) {
        if (autoSpinSettingsStage != null) {
            autoSpinSettingsStage.show();
            autoSpinSettingsStage.toFront();
            return;
        }
        autoSpinSettingsStage = new Stage();
        autoSpinSettingsStage.initStyle(StageStyle.TRANSPARENT);
        autoSpinSettingsStage.initModality(Modality.APPLICATION_MODAL);
        autoSpinSettingsStage.initOwner(primaryStage);

        CheckBox quickSpinCheckBox = new CheckBox("Quick Spin");
        CheckBox turboSpinCheckBox = new CheckBox("Turbo Spin");

        quickSpinCheckBox.getStyleClass().add("dialog-checkbox");
        quickSpinCheckBox.setStyle(String.format("-fx-font-size: %dpx;", get("QuickSpinCheckBoxFontSize")));
        turboSpinCheckBox.getStyleClass().add("dialog-checkbox");
        turboSpinCheckBox.setStyle(String.format("-fx-font-size: %dpx;", get("TurboSpinCheckBoxFontSize")));

        quickSpinCheckBox.setOnAction(_ -> {
            if (quickSpinCheckBox.isSelected()) {
                turboSpinCheckBox.setSelected(false);
                spinMode = SpinMode.QUICK;
            } else {
                spinMode = SpinMode.NORMAL;
            }
        });

        turboSpinCheckBox.setOnAction(_ -> {
            if (turboSpinCheckBox.isSelected()) {
                quickSpinCheckBox.setSelected(false);
                spinMode = SpinMode.TURBO;
            } else {
                spinMode = SpinMode.NORMAL;
            }
        });

        Label spinCountLabel = new Label("Number of Autospins: 100");
        spinCountLabel.getStyleClass().add("dialog-label");
        spinCountLabel.setStyle(String.format("-fx-font-size: %dpx;", get("SpinCountLabelFontSize")));

        Slider spinCountSlider = new Slider();
        spinCountSlider.setMin(0);
        spinCountSlider.setMax(7);
        spinCountSlider.getStyleClass().add("dialog-slider");
        spinCountSlider.setStyle(String.format("-fx-font-size: %dpx;", get("SpinCountSliderFontSize")));
        spinCountSlider.setValue(3); // Alapértelmezésként 3 pozícióban (100 autospin)
        //hang hozzáadása
        addSoundToWidget(spinCountSlider);
        int[] spinValues = {20, 30, 50, 100, 150, 200, 500, 1000};
        autoSpinCount = spinValues[3];

        spinCountSlider.valueProperty().addListener((_, _, newVal) -> {
            int index = newVal.intValue();
            autoSpinCount = spinValues[index];
            spinCountLabel.setText("Number of Autospins: " + autoSpinCount);
        });

        Button saveButton = new Button("Spin");
        addSoundToWidget(saveButton);
        saveButton.getStyleClass().add("dialog-save-button");
        saveButton.setStyle(String.format(
                "-fx-padding: %d %d; -fx-font-size: %dpx;",
                get("SaveButtonPaddingV"),
                get("SaveButtonPaddingH"),
                get("AutoplaySaveButtonFontSize")
        ));

        saveButton.setOnAction(_ -> {
            autoSpinSettingsStage.close();
            autoSpinSettingsStage = null;
            Platform.runLater(root::requestFocus); //fókusz visszaállítása a főablakra
            startAutoSpins();
        });

        VBox dialogVbox = new VBox(get("DialogVboxSpacing"), quickSpinCheckBox, turboSpinCheckBox, spinCountLabel, spinCountSlider, saveButton);
        dialogVbox.setAlignment(Pos.CENTER);
        dialogVbox.setPadding(new Insets(get("DialogVboxPaddingV")));
        dialogVbox.setOpacity(0);
        dialogVbox.setEffect(new DropShadow(100, Color.BLACK));

        dialogVbox.getStyleClass().add("dialog-vbox");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogVbox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Button closeButton = new Button();
        closeButton.getStyleClass().add("dialog-close-button");
        addSoundToWidget(closeButton);
        closeButton.setOnAction(_ ->{
            autoSpinSettingsStage.close();
            autoSpinSettingsStage = null;
            Platform.runLater(root::requestFocus);
        });
        //Escape-re kilépés:
        autoSpinSettingsStage.addEventHandler(KeyEvent.KEY_PRESSED, _ -> {
            autoSpinSettingsStage.close();
            autoSpinSettingsStage = null;
            Platform.runLater(root::requestFocus);
        });
        closeButton.setPrefSize(get("AutoPlayCloseButtonWidth"), get("AutoPlayCloseButtonHeight"));
        autoSpinSettingsStage.setOnHidden(_ -> volumeSettingsStage = null);
        // Position the close button at the top-left corner
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(get("AutoPlayCloseButtonPaddingV"), get("AutoPlayCloseButtonPaddingV1"), get("AutoPlayCloseButtonPaddingV2"), get("AutoPlayCloseButtonPaddingV3")));

        StackPane root = new StackPane(dialogVbox,closeButton);
        // Vágó alakzat létrehozása lekerekített sarkokkal
        Rectangle clip = new Rectangle(get("DialogSceneWidth"), get("DialogSceneHeight")); // A párbeszédablak méretével egyező
        clip.setArcWidth(40); // Lekerekített sarkok szélessége
        clip.setArcHeight(40); // Lekerekített sarkok magassága
        root.setClip(clip); // A vágó alakzat alkalmazása a StackPane-re
        Scene dialogScene = new Scene(root, get("DialogSceneWidth"), get("DialogSceneHeight"));
        dialogScene.setFill(Color.TRANSPARENT);
        dialogScene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");
        autoSpinSettingsStage.setScene(dialogScene);
        autoSpinSettingsStage.setResizable(false);
        autoSpinSettingsStage.show();
        Platform.runLater(()->Funtions.centerStage(autoSpinSettingsStage,primaryStage));
    }
    private void showInGameSettings(Stage primaryStage) {
        if (volumeSettingsStage != null) {
            volumeSettingsStage.show();
            volumeSettingsStage.toFront();
            return;
        }
        volumeSettingsStage = new Stage();
        volumeSettingsStage.initStyle(StageStyle.TRANSPARENT);
        volumeSettingsStage.initModality(Modality.APPLICATION_MODAL);
        volumeSettingsStage.initOwner(primaryStage);

        // Százalékérték címke
        Label percentageLabel = new Label(String.format("Volume: %d%%", (int) (gameMusic.getVolume() * 100)));
        percentageLabel.setStyle(String.format("-fx-font-size: %dpx;", get("AudioSettingsLabelFontSize")));
        percentageLabel.getStyleClass().add("dialog-label");

        Slider volumeSlider = new Slider(0, 1, gameMusic.getVolume());
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.getStyleClass().add("dialog-slider");
        volumeSlider.setStyle(String.format("-fx-font-size: %dpx;", get("VolumeSliderFontSize")));
        volumeSlider.setPadding(new Insets(
                get("VolumeSliderPaddingV"),
                get("VolumeSliderPaddingV1"),
                get("VolumeSliderPaddingV2"),
                get("VolumeSliderPaddingV3")));
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
        volumeSlider.valueProperty().addListener((_, _, newValue) -> {
            double volume = newValue.doubleValue();
            gameMusic.setVolume(volume);
            initialVolume = volume; // Update the initial volume for future game sessions
            percentageLabel.setText(String.format("Volume: %d%%", (int) (volume * 100))); // Százalék frissítése
        });

        // Online módban nem szükséges a credit fájl betöltés
        // Button addCreditsButton = new Button("Add Credits");
        // addCreditsButton.getStyleClass().add("dialog-save-button");
        // addCreditsButton.setStyle(String.format("-fx-font-size: %dpx;", get("AudioSettingsLabelFontSize")));
        // addCreditsButton.setOnAction(_ -> {
        //     volumeSettingsStage.close();
        //     volumeSettingsStage = null;
        //     showCreditFileChooser(primaryStage);
        // });

        Button closeButton = new Button();
        closeButton.getStyleClass().add("dialog-close-button");
        addSoundToWidget(closeButton);
        closeButton.setOnAction(_ -> {
            volumeSettingsStage.close();
            volumeSettingsStage = null;
            Platform.runLater(root::requestFocus); // Fókusz visszaállítása a főablakra
        });
        closeButton.setPrefSize(get("AudioSettingsCloseButtonWidth"), get("AudioSettingsCloseButtonHeight"));
        volumeSettingsStage.setOnHidden(_ -> volumeSettingsStage = null);

        VBox settingsLayout = new VBox(get("AudioSettingsLayoutSpacing"), percentageLabel,volumeSlider);
        settingsLayout.getStyleClass().add("dialog-vbox");
        settingsLayout.setAlignment(Pos.CENTER);
        settingsLayout.setOpacity(0);
        settingsLayout.setEffect(new DropShadow(100, Color.BLACK));

        // Lekerekített vágó alakzat hozzáadása
        StackPane root = new StackPane(settingsLayout, closeButton);
        Rectangle clip = new Rectangle(get("AudioSettingsLayoutClipWidth"), get("AudioSettingsLayoutClipHeight")); // Ablak méretével megegyező
        clip.setArcWidth(get("AudioSettingsLayoutClipArchWidth")); // Lekerekített sarkok szélessége
        clip.setArcHeight(get("AudioSettingsLayoutClipArchHeight")); // Lekerekített sarkok magassága
        root.setClip(clip); // Clip alkalmazása a StackPane-re

        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(get("AudioSettingsCloseButtonPaddingV"), get("AudioSettingsCloseButtonPaddingV1"), get("AudioSettingsCloseButtonPaddingV2"), get("AudioSettingsCloseButtonPaddingV3")));

        Scene settingsScene = new Scene(root, get("AudioSettingsSceneWidth"), get("AudioSettingsSceneHeight"));
        settingsScene.setFill(Color.TRANSPARENT); // Az ablak háttere átlátszó
        settingsScene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");

        //Escape-re kilépés
        settingsScene.addEventHandler(KeyEvent.KEY_PRESSED, _ -> {
            volumeSettingsStage.close();
            volumeSettingsStage = null;
            Platform.runLater(root::requestFocus);
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), settingsLayout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        volumeSettingsStage.setScene(settingsScene);
        volumeSettingsStage.show();
        Platform.runLater(()->Funtions.centerStage(volumeSettingsStage,primaryStage));
    }

    private void showWinningPopup(double payout, int multiplier, Runnable onClose) {
        if (multiplier <= 10) {
            onClose.run(); // Ha nincs jelentős nyeremény, folytatja az automatikus pörgetést
            return;
        }
        String message,soundfile;
        if (multiplier > 100) {
            message = "src/main/resources/pictures/gorgeous.png";
            soundfile = "src/main/resources/sounds/gorgeous.mp3";
        } else if (multiplier > 50) {
            message = "src/main/resources/pictures/amazing.png";
            soundfile = "src/main/resources/sounds/amazing.mp3";
        } else {
            message = "src/main/resources/pictures/fantastic.png";
            soundfile = "src/main/resources/sounds/fantastic.mp3";
        }

        Media sound = new Media(new File(soundfile).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(0);

        double originalVolume = gameMusic.getVolume();

        mediaPlayer.setOnReady(() -> {

            // Háttérzene fokozatos lehalkítása
            Timeline backgroundFadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(gameMusic.volumeProperty(), originalVolume)),
                    new KeyFrame(Duration.millis(500), new KeyValue(gameMusic.volumeProperty(), originalVolume * 0.3))
            );

            // Popup zene fokozatos felhangosítása
            Timeline popupFadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(mediaPlayer.volumeProperty(), 0)),
                    new KeyFrame(Duration.millis(500), new KeyValue(mediaPlayer.volumeProperty(), originalVolume))
            );
            backgroundFadeOut.play();
            popupFadeIn.play();

            double duration = sound.getDuration().toSeconds();
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.TRANSPARENT);

            VBox layout = new VBox(10);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 20; -fx-padding: 20;");
            layout.setEffect(new DropShadow(20, Color.BLACK));

            // Create ImageView instead of Label for the message
            ImageView messageImage = new ImageView(new Image("file:" + message));
            messageImage.setFitWidth(300); // Set appropriate size
            messageImage.setPreserveRatio(true);
            messageImage.setScaleX(0);
            messageImage.setScaleY(0);

            // Create scaling animation
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(1000), messageImage);
            scaleUp.setFromX(0);
            scaleUp.setFromY(0);
            scaleUp.setToX(3); // Scale up
            scaleUp.setToY(3);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scaleBack = new ScaleTransition(Duration.millis(500), messageImage);
            scaleBack.setFromX(3);
            scaleBack.setFromY(3);
            scaleBack.setToX(2.8); // Scale back a bit
            scaleBack.setToY(2.8);
            scaleBack.setInterpolator(Interpolator.EASE_IN);

            SequentialTransition scaleSequence = new SequentialTransition(scaleUp, scaleBack);
            scaleSequence.play();

            Label payoutLabel = new Label("$0");
            payoutLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: white;");

            layout.getChildren().addAll(messageImage, payoutLabel);

            Scene scene = new Scene(layout);
            scene.setFill(Color.TRANSPARENT);

            AtomicBoolean skipped = new AtomicBoolean(false); // nyilvántartja, hogy a felhasználó skippelte-e az animációt
            AtomicBoolean isPlaying = new AtomicBoolean(true);
            // Azonnali növekedési animáció
            Timeline numberAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(new SimpleDoubleProperty(0), 0)),
                    new KeyFrame(Duration.seconds(duration), _ -> {
                        payoutLabel.setText(String.format("$%d", (int)payout));
                        skipped.set(true);
                        isPlaying.set(false);
                    }, new KeyValue(new SimpleDoubleProperty(payout), payout))
            );

            numberAnimation.currentTimeProperty().addListener((_, _, newTime) -> {
                if (!skipped.get()) {
                    double currentValue = (newTime.toSeconds() / duration) * payout;
                    payoutLabel.setText(String.format("$%d", (int)currentValue));
                }
            });

            numberAnimation.play();
            mediaPlayer.play();

            // Popup bezárása után visszaállítjuk az eredeti hangerőt
            mediaPlayer.setOnEndOfMedia(() -> {
                Timeline backroundFadeIn = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(gameMusic.volumeProperty(), gameMusic.getVolume())),
                        new KeyFrame(Duration.millis(1000), new KeyValue(gameMusic.volumeProperty(), originalVolume))
                );
                backroundFadeIn.play();
                isPlaying.set(false);
            });

            scene.setOnKeyPressed(event -> {
                if (!skipped.get() && event.getCode() == KeyCode.SPACE) {
                    payoutLabel.setText(String.format("$%d", (int)payout));
                    skipped.set(true);
                } else if (!isPlaying.get()) {
                    popupStage.close();
                    onClose.run();
                }
            });

            scene.setOnMouseClicked(_ -> {
                if (!skipped.get()) {
                    payoutLabel.setText(String.format("$%d", (int)payout));
                    skipped.set(true);
                } else if (!isPlaying.get()) {
                    popupStage.close();
                    onClose.run();
                }
            });

            popupStage.setScene(scene);
            popupStage.setWidth(((double) get("GameWidth") /3)*2);
            popupStage.setHeight(((double) get("GameHeight") /3)*2);

            centerStage(popupStage, root.getScene());

            popupStage.setOnCloseRequest(Event::consume);

            popupStage.show();
        });
    }
    private void showLowBalanceMessage(Stage primaryStage) {
        if (lowBalanceStage != null) {
            lowBalanceStage.show();
            lowBalanceStage.toFront();
            return;
        }

        lowBalanceStage = new Stage();
        lowBalanceStage.initStyle(StageStyle.TRANSPARENT);
        lowBalanceStage.initModality(Modality.APPLICATION_MODAL);
        lowBalanceStage.initOwner(primaryStage);

        Label messageLabel = new Label("Insufficient funds!");
        messageLabel.getStyleClass().add("dialog-label");
        messageLabel.setStyle(String.format("-fx-font-size: %dpx;", get("SpinCountLabelFontSize")));

        Label detailLabel = new Label("Your balance is too low for the current bet.");
        detailLabel.getStyleClass().add("dialog-label");
        detailLabel.setStyle(String.format("-fx-font-size: %dpx;", get("QuickSpinCheckBoxFontSize")));

        Button closeButton = new Button("OK");
        addSoundToWidget(closeButton);
        closeButton.getStyleClass().add("dialog-save-button");
        closeButton.setStyle(String.format(
                "-fx-padding: %d %d; -fx-font-size: %dpx;",
                get("OKButtonPaddingV"),
                get("OKButtonPaddingH"),
                get("LowBalanceOKButtonFontSize")
        ));

        closeButton.setOnAction(_ -> {
            lowBalanceStage.close();
            lowBalanceStage = null;
            Platform.runLater(root::requestFocus); //reset focus to main window
        });

        // Online módban az admin adja hozzá a krediteket
        // Button addCreditsButton = new Button("Add Credits");
        // addCreditsButton.getStyleClass().add("dialog-save-button");
        // addCreditsButton.setStyle(String.format("-fx-font-size: %dpx;", get("AudioSettingsLabelFontSize")));
        // addCreditsButton.setOnAction(_ -> {
        //     lowBalanceStage.close();
        //     lowBalanceStage = null;
        //     showCreditFileChooser(primaryStage);
        // });

        VBox dialogVbox = new VBox(get("DialogVboxSpacing"), messageLabel, detailLabel, closeButton);
        dialogVbox.setAlignment(Pos.CENTER);
        dialogVbox.setPadding(new Insets(get("DialogVboxPaddingV")));
        dialogVbox.setOpacity(0);
        dialogVbox.setEffect(new DropShadow(100, Color.BLACK));
        dialogVbox.getStyleClass().add("dialog-vbox");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogVbox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Button windowCloseButton = new Button();
        windowCloseButton.getStyleClass().add("dialog-close-button");
        addSoundToWidget(windowCloseButton);
        windowCloseButton.setOnAction(_ -> {
            lowBalanceStage.close();
            lowBalanceStage = null;
            Platform.runLater(root::requestFocus); //reset focus to main window
        });
        windowCloseButton.setPrefSize(get("LowBalanceCloseButtonWidth"), get("LowBalanceCloseButtonHeight"));
        lowBalanceStage.setOnHidden(_ -> lowBalanceStage = null);

        //Escape-re kilépés
        lowBalanceStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                lowBalanceStage.close();
                lowBalanceStage = null;
                Platform.runLater(root::requestFocus); //reset focus to main window
            }
        });

        // Position the close button at the top-right corner
        StackPane.setAlignment(windowCloseButton, Pos.TOP_RIGHT);
        StackPane.setMargin(windowCloseButton, new Insets(get("LowBalanceCloseButtonPaddingV"), get("LowBalanceCloseButtonPaddingV1"), get("LowBalanceCloseButtonPaddingV2"), get("LowBalanceCloseButtonPaddingV3")));

        StackPane root = new StackPane(dialogVbox, windowCloseButton);
        // Create rounded corners for the dialog
        Rectangle clip = new Rectangle(get("LowBalanceSceneWidth"), get("LowBalanceSceneHeight"));
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        root.setClip(clip);

        Scene dialogScene = new Scene(root, get("LowBalanceSceneWidth"), get("LowBalanceSceneHeight"));
        dialogScene.setFill(Color.TRANSPARENT);
        dialogScene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");

        lowBalanceStage.setScene(dialogScene);
        lowBalanceStage.setResizable(false);
        lowBalanceStage.show();
        Platform.runLater(() -> Funtions.centerStage(lowBalanceStage, primaryStage));
    }

    private void disableButtons(boolean bool) {
        Platform.runLater(() -> {
            disableButton = bool;
            spinButton.setDisable(disableButton);
            increaseBetButton.setDisable(disableButton);
            decreaseBetButton.setDisable(disableButton);
        });
    }

    private void startAutoSpins() {
        isSpinning = true;
        autoplaySettingsButton.setGraphic(null);
        autoplaySettingsButton.setText("Stop");
        autoplaySettingsButton.setPrefSize(get("AutoPlaySettingsButtonWidth"), get("AutoPlaySettingsButtonHeight"));
        autoplaySettingsButton.setStyle(String.format("-fx-font-size: %dpx ;-fx-padding: %dpx %dpx;", get("AutoplaySettingsButtonFontSize"),get("AutoPlaySettingsButtonPaddingY"), get("AutoPlaySettingsButtonPaddingX")));
        startSpin(autoSpinCount);
    }

    private void startSpin(int numberOfSpins) {
        this.spinsRemaining = numberOfSpins;
        switch (spinMode) {
            case NORMAL -> this.spinParams = new SpinParameters(150, 65, 20, 100, 5.0);
            case QUICK -> this.spinParams = new SpinParameters(50, 40, 20, 100, 5.0);
            case TURBO -> this.spinParams = new SpinParameters(0, 20, 10, 80, 5.0);
        }
        processNextStep();
    }

    private void processNextStep() {
        if (spinsRemaining <= 0 || !isSpinning) {
            isSpinning = false;
            designAutoSpinButton();
            disableButtons(false);
            autoplaySettingsButton.setDisable(false);
            spinMode = SpinMode.NORMAL;
            return;
        }
        if (game.getBalance() < game.getBet()) {
            Platform.runLater(() -> showLowBalanceMessage((Stage) root.getScene().getWindow()));
            isSpinning = false;
            designAutoSpinButton();
            disableButtons(false);
            autoplaySettingsButton.setDisable(false);
            spinMode = SpinMode.NORMAL;
            return;
        }

        if (!isBonusMode) {
            disableButtons(true);
            
            // Online módban előbb végezzük el a spint, majd küldjük a szerverre
            performSpin(() -> checkAndProcessClusters(() -> {
                double spinPayout = game.getSpinPayout();
                
                // Szerver kommunikáció
                if (game.isOnline()) {
                    boolean success = game.processSpinOnServer(game.getBet(), spinPayout);
                    if (!success) {
                        // Ha nem sikerült a szerveren, visszavonjuk a lokális változásokat
                        game.resetSpinPayout();
                        showErrorMessage("Hiba történt a pörgetés feldolgozása során!");
                        spinsRemaining--;
                        PauseTransition pause = new PauseTransition(Duration.millis(500));
                        pause.setOnFinished(_ -> processNextStep());
                        pause.play();
                        return;
                    }
                } else {
                    // Offline mód - eredeti logika
                    game.decreaseBalance(game.getBet());
                }
                
                game.resetSpinPayout();
                balanceText.setText("Credit: $" + game.getBalance());

                if (spinPayout > 0) {
                    int bet = game.getBet();
                    int multiplier = (int) Math.ceil(spinPayout / bet);
                    showWinningPopup(spinPayout, multiplier, () -> {
                        spinsRemaining--;
                        PauseTransition pause = new PauseTransition(Duration.millis(500));
                        pause.setOnFinished(_ -> processNextStep());
                        pause.play();
                    });
                } else {
                    spinsRemaining--;
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(_ -> processNextStep());
                    pause.play();
                }

                // Check for bonus trigger
                if (game.checkForBonusTrigger()) {
                    startBonusMode();
                }
            }));
        } else {
            // Bonus mode spin
            if (game.hasFreeSpins()) {
                disableButtons(true);
                game.decreaseFreeSpins();
                performSpin(() -> checkAndProcessClusters(() -> {
                    double spinPayout = game.getSpinPayout();
                    game.resetSpinPayout();
                    game.addBonusPayout(spinPayout);

                    if (spinPayout > 0) {
                        int bet = game.getBet();
                        int multiplier = (int) Math.ceil(spinPayout / bet);
                        showWinningPopup(spinPayout, multiplier, () -> {
                            spinsRemaining--;
                            PauseTransition pause = new PauseTransition(Duration.millis(500));
                            pause.setOnFinished(_ -> processNextStep());
                            pause.play();
                        });
                    } else {
                        spinsRemaining--;
                        PauseTransition pause = new PauseTransition(Duration.millis(500));
                        pause.setOnFinished(_ -> processNextStep());
                        pause.play();
                    }

                    // Check for retrigger
                    if (game.checkForRetrigger()) {
                        game.addRetriggerSpins();
                        retriggerSound.play();
                        showRetriggerPopup();
                    }

                    // Check if bonus mode should end
                    if (!game.hasFreeSpins()) {
                        endBonusMode();
                    }
                }));
            } else {
                endBonusMode();
            }
        }
    }

    private void performSpin(Runnable onComplete) {
        final int generatedCycleStart = spinParams.totalCycles - GRID_SIZE;
        int[][] generatedSymbols = game.generateSymbols();
        List<Animation> columnAnimations = new ArrayList<>(GRID_SIZE);

        for (int col = 0; col < GRID_SIZE; col++) {
            final int column = col;

            SequentialTransition columnSequence = new SequentialTransition();

            PauseTransition pause = new PauseTransition(Duration.millis(col * spinParams.pauseDelay));
            columnSequence.getChildren().add(pause);

            Timeline spinTimeline = new Timeline();
            GaussianBlur blur = new GaussianBlur(0);
            for (int row = 0; row < GRID_SIZE; row++) {
                reels[row][column].setEffect(blur);
            }

            Timeline blurTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), 0)),
                    new KeyFrame(Duration.millis((double) (spinParams.totalCycles * spinParams.cycleDuration) / 4), new KeyValue(blur.radiusProperty(), spinParams.maxBlurRadius)),
                    new KeyFrame(Duration.millis((double) (3 * spinParams.totalCycles * spinParams.cycleDuration) / 4), new KeyValue(blur.radiusProperty(), spinParams.maxBlurRadius)),
                    new KeyFrame(Duration.millis(spinParams.totalCycles * spinParams.cycleDuration), new KeyValue(blur.radiusProperty(), 0))
            );
            blurTimeline.setCycleCount(1);

            for (int cycle = 0; cycle < spinParams.totalCycles; cycle++) {
                int currentCycle = cycle;

                spinTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(spinParams.cycleDuration * currentCycle), _ -> {
                    for (int row = GRID_SIZE - 1; row > 0; row--) {
                        ImageView aboveSymbol = reels[row - 1][column];
                        reels[row][column].setImage(aboveSymbol.getImage());
                    }

                    if (currentCycle < generatedCycleStart) {
                        reels[0][column].setImage(symbols[random.nextInt(SYMBOL_COUNT)]);
                    } else {
                        int generatedIndex = currentCycle - generatedCycleStart;
                        int invertedIndex = GRID_SIZE - generatedIndex - 1;
                        reels[0][column].setImage(symbols[generatedSymbols[invertedIndex][column]]);
                    }

                    for (int row = 0; row < GRID_SIZE; row++) {
                        TranslateTransition transition = new TranslateTransition(Duration.millis(spinParams.transitionDuration), reels[row][column]);
                        if (currentCycle == spinParams.totalCycles - 1) {
                            int toY = get("SpinMoveToY");
                            int fromY = get("SpinMoveFromY");
                            transition.setFromY(fromY);
                            transition.setToY(toY);
                            transition.setInterpolator(Interpolator.EASE_OUT);
                            int finalRow = row;
                            transition.setOnFinished(_ -> {
                                TranslateTransition settleTransition = new TranslateTransition(Duration.millis(spinParams.transitionDuration), reels[finalRow][column]);
                                settleTransition.setFromY(toY);
                                settleTransition.setToY(fromY);
                                settleTransition.setOnFinished(_ -> {
                                    // Do nothing
                                });
                                settleTransition.play();
                            });
                        }
                        transition.play();
                    }
                }));
            }
            spinTimeline.setCycleCount(1);

            ParallelTransition spinAndBlur = new ParallelTransition(spinTimeline, blurTimeline);
            spinAndBlur.setOnFinished(_ -> {
                if (spinMode == SpinMode.TURBO) {
                    if (column == GRID_SIZE - 1) { // Csak az utolsó oszlopnál játsszuk le
                        spinSounds[0].stop();
                        spinSounds[0].setVolume(initialVolume * 0.5);
                        spinSounds[0].seek(Duration.ZERO);
                        spinSounds[0].play();
                    }
                } else {
                    // Normál mód: minden oszlop külön-külön lejátsza a hangot
                    spinSounds[column].stop();
                    spinSounds[column].setVolume(initialVolume * 0.5);
                    spinSounds[column].seek(Duration.ZERO);
                    spinSounds[column].play();
                }
            });
            columnSequence.getChildren().add(spinAndBlur);
            columnAnimations.add(columnSequence);
        }

        ParallelTransition allColumns = new ParallelTransition();
        allColumns.getChildren().addAll(columnAnimations);
        allColumns.setOnFinished(_ -> onComplete.run());
        allColumns.play();
    }

    private void checkAndProcessClusters(Runnable onComplete) {
        Map<Integer, List<int[]>> matchedClusters = game.checkForMatches();
        if (!matchedClusters.isEmpty()) {
            game.clearMatchedSymbols(matchedClusters);
            //System.out.println("hit: "+hitPayout + "total: "+game.getSpinPayout());
            // Kiírjuk a találatokat és nyereményeket
            /*System.out.println("Eredmény a pörgetés után:");
            for (Map.Entry<Integer, List<int[]>> entry : matchedClusters.entrySet()) {
                int symbol = entry.getKey();
                int clusterSize = entry.getValue().size();
                Pair<Double, Integer> multiplierInfo = game.getPayoutMultiplier(symbol, clusterSize);
                double payoutMultiplier = multiplierInfo.first;
                double payout = game.getBet() * payoutMultiplier;

                System.out.println("Szimbólum: " + symbol + ", Találatok száma: " + clusterSize +
                        ", Szorzó: " + payoutMultiplier + ", Nyeremény: $" + payout);
            }
            System.out.println("Összes nyeremény: $" + game.getSpinPayout());
            System.out.printf("Aktuális RTP: %.2f%%%n", game.getRTP());*/

            PauseTransition pause = new PauseTransition(Duration.millis(300)); // 500 ms várakozás
            pause.setOnFinished(_ -> {
                animateClusterClearing(matchedClusters, () -> {
                    game.dropAndRefillSymbols();
                    winText.setText("WIN: $" + (int) game.getSpinPayout());
                    winText.setVisible(true);
                    updateGridWithNewSymbols(() -> checkAndProcessClusters(onComplete));
                });
            });
            pause.play();
            balanceText.setText("Credit: $" + game.getBalance());
        } else {
            winText.setVisible(false);
            onComplete.run();
        }
    }

    private void playNextHitSound() {
        // Előző hang leállítása, ha még szólna
        hitSound.stop();
        // Hang lejátszása az aktuális indexről
        hitSound.setVolume(initialVolume * 0.5);
        hitSound.play();
    }

    private void updateGridWithNewSymbols(Runnable onComplete) {
        final int DELAY_BETWEEN_COLUMNS = 100; // milliszekundumban a késleltetés mértéke
        ParallelTransition columnsSequence = new ParallelTransition(); // Módosítottuk SequentialTransition-ről ParallelTransition-re
        int[][] updatedGrid = game.getSymbols(); // Frissített rács állapot a játéklogikából

        for (int col = 0; col < GRID_SIZE; col++) {
            SequentialTransition columnTransition = new SequentialTransition();
            boolean columnHasAnimations = false;

            // Késleltetés hozzáadása az oszlopok közötti különbséghez
            PauseTransition columnDelay = new PauseTransition(Duration.millis(DELAY_BETWEEN_COLUMNS * col));
            columnTransition.getChildren().add(columnDelay);

            for (int row = GRID_SIZE - 1; row >= 0; row--) {
                if (reels[row][col].getImage() == null) {
                    // Üres hely esetén szimbólumot keresünk felette
                    int sourceRow = row - 1;
                    while (sourceRow >= 0 && reels[sourceRow][col].getImage() == null) {
                        sourceRow--;
                    }

                    if (sourceRow >= 0) {
                        // Szimbólum mozgatása a `sourceRow`-ból a `row`-ba
                        Image imageToMove = reels[sourceRow][col].getImage();
                        reels[sourceRow][col].setImage(null); // Töröljük a forrássorból

                        ImageView targetCell = reels[row][col];
                        targetCell.setImage(imageToMove);
                        targetCell.setTranslateY(-(row - sourceRow) * get("SymbolSize")); // Kiindulási hely beállítása

                        TranslateTransition fallDown = new TranslateTransition(Duration.millis(30), targetCell);
                        fallDown.setFromY(-(row - sourceRow) * get("SymbolSize")); // Mozgás kezdeti pontja
                        fallDown.setToY(get("UpdateMoveDownToY")); // Túllendül a végső helyen
                        fallDown.setInterpolator(Interpolator.EASE_OUT);

                        for (int i = 0; i < GRID_SIZE; i++) {
                            for (int j = GRID_SIZE - 1; j >= 0; j--) {
                                reels[j][i].setOpacity(1.0);
                            }
                        }
                        // Túllendülés után visszarántás a végleges helyre
                        TranslateTransition moveBack = new TranslateTransition(Duration.millis(30), targetCell);
                        moveBack.setFromY(get("UpdateMoveBackFromY"));
                        moveBack.setToY(get("UpdateMoveBackToY"));
                        moveBack.setInterpolator(Interpolator.EASE_IN);

                        // Az animációk sorba rendezése
                        SequentialTransition bounceTransition = new SequentialTransition(fallDown, moveBack);
                        columnTransition.getChildren().add(bounceTransition);
                        columnHasAnimations = true;
                    } else {
                        // Nincs felette szimbólum, új szimbólum beszúrása
                        int symbolIndex = updatedGrid[row][col];
                        Image newSymbol = symbols[symbolIndex];
                        for (int i = 0; i < GRID_SIZE; i++) {
                            for (int j = GRID_SIZE - 1; j >= 0; j--) {
                                reels[j][i].setOpacity(1.0);
                            }
                        }

                        ImageView targetCell = reels[row][col];
                        targetCell.setTranslateY(-(row + 1) * get("SymbolSize")); // Kiindulási pont felülről
                        targetCell.setImage(newSymbol);

                        TranslateTransition fallIn = new TranslateTransition(Duration.millis(30), targetCell);
                        fallIn.setFromY(-(row + 1) * get("SymbolSize")); // Kezdőpozíció
                        fallIn.setToY(5); // Túllendül a végleges helyen
                        fallIn.setInterpolator(Interpolator.EASE_OUT);

                        // Túllendülés után visszarántás a végleges helyre
                        TranslateTransition settleTransition = new TranslateTransition(Duration.millis(30), targetCell);
                        settleTransition.setFromY(10);
                        settleTransition.setToY(0);
                        settleTransition.setInterpolator(Interpolator.EASE_IN);

                        SequentialTransition bounceTransition = new SequentialTransition(fallIn, settleTransition);
                        columnTransition.getChildren().add(bounceTransition);
                        columnHasAnimations = true;
                    }
                }
            }

            if (columnHasAnimations) {
                int finalCol = col;
                columnTransition.setOnFinished(_ -> {
                    fallSounds[finalCol].stop(); // Megállítjuk az esetlegesen még futó hangot
                    fallSounds[finalCol].setVolume(initialVolume); // Hangerő csökkentése, hogy ne legyen túl hangos
                    fallSounds[finalCol].seek(Duration.ZERO);
                    fallSounds[finalCol].play();
                });
                columnsSequence.getChildren().add(columnTransition);
            }
        }

        if (!columnsSequence.getChildren().isEmpty()) {
            columnsSequence.setOnFinished(_ -> onComplete.run());
            columnsSequence.play();
        } else {
            // Nincs animáció, futtasd közvetlenül az onComplete-t
            onComplete.run();
        }
    }
    private void createSparkleEffect(Group container, ImageView symbol) {
        // Get symbol position in scene coordinates
        Bounds bounds = symbol.localToScene(symbol.getBoundsInLocal());
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();

        // Create 15-20 particles per symbol
        int particleCount = 15 + random.nextInt(5);

        for (int i = 0; i < particleCount; i++) {
            // Random sparkle size
            double size = 3 + random.nextDouble() * 8;

            // Create circle particle
            Circle particle = new Circle(size);

            // Gold/yellow color with random brightness
            Color particleColor = Color.hsb(45, 0.8, 0.8 + random.nextDouble() * 0.2, 0.9);
            particle.setFill(particleColor);

            // Add glow to particle
            DropShadow particleGlow = new DropShadow(size * 2, particleColor);
            particle.setEffect(particleGlow);

            // Position particle at symbol center
            particle.setCenterX(centerX);
            particle.setCenterY(centerY);

            // Add to container
            container.getChildren().add(particle);

            // Random direction for particle movement
            double angle = random.nextDouble() * 360;
            double distance = 30 + random.nextDouble() * 70;

            // Create movement animation
            TranslateTransition move = new TranslateTransition(Duration.millis(600 + random.nextInt(400)), particle);
            move.setByX(Math.cos(Math.toRadians(angle)) * distance);
            move.setByY(Math.sin(Math.toRadians(angle)) * distance);
            move.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.8, 1.0));

            // Fade out animation
            FadeTransition fade = new FadeTransition(Duration.millis(400 + random.nextInt(300)), particle);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setDelay(Duration.millis(200 + random.nextInt(300)));

            // Scale animation
            ScaleTransition scale = new ScaleTransition(Duration.millis(800), particle);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(0.2);
            scale.setToY(0.2);

            // Play animations together
            ParallelTransition particleAnimation = new ParallelTransition(particle, move, fade, scale);
            particleAnimation.setOnFinished(_ -> container.getChildren().remove(particle));
            particleAnimation.play();
        }
    }

    private void animateClusterClearing(Map<Integer, List<int[]>> matchedClusters, Runnable onComplete) {
        List<Animation> animations = new ArrayList<>();

        // Play hit sound when clusters are cleared
        playNextHitSound();

        // Group all positions for easier animation application
        List<int[]> allPositions = new ArrayList<>();
        matchedClusters.values().forEach(allPositions::addAll);

        // Create a particle effect container that will be added to the scene
        Group particleEffects = new Group();
        root.getChildren().add(particleEffects);

        for (List<int[]> cluster : matchedClusters.values()) {
            for (int[] position : cluster) {
                int row = position[0];
                int col = position[1];
                ImageView symbol = reels[row][col];

                // Create a bright glow effect
                DropShadow glow = new DropShadow();
                glow.setColor(Color.GOLD);
                glow.setRadius(20);
                glow.setSpread(0.8);

                // Scale animation
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(300), symbol);
                scaleUp.setToX(1.2);
                scaleUp.setToY(1.2);

                // Pulsating glow effect
                Timeline glowPulse = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
                        new KeyFrame(Duration.millis(200), new KeyValue(glow.radiusProperty(), 30)),
                        new KeyFrame(Duration.millis(400), new KeyValue(glow.radiusProperty(), 15))
                );
                glowPulse.setCycleCount(2);

                // Apply the glow effect
                symbol.setEffect(glow);

                // Create sparkle particles around the symbol
                createSparkleEffect(particleEffects, symbol);

                // Combine scale and rotate with parallel transition
                ParallelTransition grow = new ParallelTransition(scaleUp, glowPulse);

                // Final fade out
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), symbol);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                // Combine all animations in sequence
                SequentialTransition fullSequence = new SequentialTransition(grow, fadeOut);
                fullSequence.setOnFinished(_ -> {
                    symbol.setImage(null);
                    symbol.setEffect(null);
                    symbol.setScaleX(1.0);
                    symbol.setScaleY(1.0);
                    symbol.setRotate(0);
                });

                animations.add(fullSequence);
            }
        }

        // Run all symbol animations in parallel
        ParallelTransition allAnimations = new ParallelTransition();
        allAnimations.getChildren().addAll(animations);

        // Clean up after animations complete
        allAnimations.setOnFinished(_ -> {
                root.getChildren().remove(particleEffects);
                onComplete.run();
        });

        allAnimations.play();
    }

    private void designAutoSpinButton(){
        ImageView autoSpinIcon = new ImageView(new Image("file:src/main/resources/pictures/autospin.png"));
        autoSpinIcon.setFitWidth(get("AutoplayIconWidth")); // Kép szélessége
        autoSpinIcon.setFitHeight(get("AutoplayIconHeight")); // Kép magassága
        autoSpinIcon.setSmooth(true); // Sima skálázás
        autoSpinIcon.setPreserveRatio(true); // Arányok megtartása
        autoplaySettingsButton.setPrefSize(get("AutoPlaySettingsButtonWidth"), get("AutoPlaySettingsButtonHeight"));
        autoplaySettingsButton.setStyle(String.format("-fx-font-size: %dpx ;-fx-padding: %dpx %dpx;", get("AutoplaySettingsButtonFontSize"),get("AutoPlaySettingsButtonPaddingY"), get("AutoPlaySettingsButtonPaddingX")));
        autoplaySettingsButton.setGraphic(autoSpinIcon); // A kép hozzáadása a gombhoz
        autoplaySettingsButton.setContentDisplay(ContentDisplay.RIGHT); // A szöveg és ikon elrendezése
        autoplaySettingsButton.setText("AUTO "); // Gomb szövege

    }
    private Button createSpinButton() {
        Button button=new Button();
        button.setTranslateY(get("SpinButtonTranslateY"));
        button.setStyle(String.format("-fx-font-size: %dpx; -fx-padding: %dpx %dpx;", get("SpinButtonFontSize"), get("SpinButtonPaddingY"), get("SpinButtonPaddingX")));
        button.getStyleClass().add("spin-button");
        button.setPrefWidth(get("SpinButtonWidth"));
        button.setPrefHeight(get("SpinButtonHeight"));
        button.setText("Spin");
        return button;
    }

    private Button createBetButton(String file) {
        Button button = new Button();
        ImageView icon = new ImageView(new Image(file));
        icon.setFitWidth(get("BetButtonIconWidth"));
        icon.setFitHeight(get("BetButtonIconHeight"));

        button.setGraphic(icon);
        button.setPrefSize(get("BetButtonWidth"), get("BetButtonHeight"));
        button.getStyleClass().add("bet-button");
        return button;
    }

    private Button createBonusButton() {
        Button button = new Button("  Buy\nBonus");
        button.setMinWidth(120);
        button.setStyle(String.format("-fx-font-size: %dpx;", get("BonusButtonFontSize")));
        button.setPrefSize(get("BonusButtonWidth"), get("BonusButtonHeight"));
        button.getStyleClass().add("bonus-button");
        return button;
    }

    private void addSoundToWidget(Control obj) {
        obj.setOnMousePressed(_ -> {
            buttonClickSound.stop();
            buttonClickSound.seek(Duration.ZERO);
            buttonClickSound.play();
        });
    }

    private void loadSymbols() {
        String[] fileNames = {
                "10.png", "j.png", "q.png", "k.png", "a.png", "cross.png", "cat.png", "pharaoh.png", "scatter.png"
        };
        for (int i = 0; i < fileNames.length; i++) {
            symbols[i] = ResourceLoader.loadImage("symbols/"+fileNames[i]);
        }
    }

    public void setInitialVolume(double volume) {
        this.initialVolume = volume;
    }

    private void startBonusMode() {
        isBonusMode = true;
        game.startBonusMode();
        bonusTriggerSound.play();
        
        // Fade out game music and start bonus music
        Timeline fadeOut = new Timeline(new KeyFrame(
                Duration.seconds(1),
                new KeyValue(gameMusic.volumeProperty(), 0)
        ));
        fadeOut.setOnFinished(_ -> {
            gameMusic.stop();
            bonusBackgroundMusic.setVolume(initialVolume);
            bonusBackgroundMusic.setCycleCount(MediaPlayer.INDEFINITE);
            bonusBackgroundMusic.play();
        });
        fadeOut.play();

        // Change background style
        root.setStyle(String.format("-fx-background-size: %d %d; -fx-background-image: url('file:src/main/resources/backgrounds/dog.png');",
            get("BGImageWidth"), get("BGImageHeight")));

        // Update UI elements for bonus mode
        updateUIForBonusMode();
    }

    private void endBonusMode() {
        isBonusMode = false;
        game.endBonusMode();
        
        // Fade out bonus music and start game music
        Timeline fadeOut = new Timeline(new KeyFrame(
                Duration.seconds(1),
                new KeyValue(bonusBackgroundMusic.volumeProperty(), 0)
        ));
        fadeOut.setOnFinished(_ -> {
            bonusBackgroundMusic.stop();
            gameMusic.setVolume(initialVolume);
            gameMusic.play();
        });
        fadeOut.play();

        // Restore original background
        root.setStyle(String.format("-fx-background-size: %d %d;", 
            get("BGImageWidth"), get("BGImageHeight")));

        // Show bonus results
        showBonusResults();

        // Update UI elements back to normal
        updateUIForNormalMode();
    }

    private void updateUIForBonusMode() {
        // Update UI elements for bonus mode
        balanceText.setText("Free Spins: " + game.getRemainingFreeSpins());
        spinButton.setText("Bonus Spin");
        // Add any other UI updates for bonus mode
    }

    private void updateUIForNormalMode() {
        // Restore UI elements to normal mode
        balanceText.setText("Credit: $" + game.getBalance());
        spinButton.setText("Spin");
        // Add any other UI updates for normal mode
    }

    private void showRetriggerPopup() {
        Stage popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(root.getScene().getWindow());

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 20; -fx-padding: 20;");
        layout.setEffect(new DropShadow(20, Color.BLACK));

        Label retriggerLabel = new Label("RETRIGGER!");
        retriggerLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: gold;");
        
        Label spinsLabel = new Label("+" + GameSettings.RETRIGGER_SPINS + " Free Spins!");
        spinsLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: white;");

        layout.getChildren().addAll(retriggerLabel, spinsLabel);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);

        popupStage.setScene(scene);
        popupStage.setWidth(400);
        popupStage.setHeight(200);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), layout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), layout);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2));
        fadeOut.setOnFinished(_ -> popupStage.close());
        fadeOut.play();

        popupStage.show();
        Funtions.centerStage(popupStage, root.getScene());
    }

    private void showBonusResults() {
        if (bonusResultsStage != null) {
            bonusResultsStage.close();
            bonusResultsStage = null;
        }

        bonusResultsStage = new Stage();
        bonusResultsStage.initStyle(StageStyle.TRANSPARENT);
        bonusResultsStage.initModality(Modality.APPLICATION_MODAL);
        bonusResultsStage.initOwner(root.getScene().getWindow());

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 20; -fx-padding: 30;");
        layout.setEffect(new DropShadow(20, Color.BLACK));

        Label titleLabel = new Label("BONUS RESULTS");
        titleLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: gold;");

        Label payoutLabel = new Label("$" + (int)game.getBonusPayout());
        payoutLabel.setStyle("-fx-font-size: 72px; -fx-text-fill: white;");

        Button closeButton = new Button("Continue");
        closeButton.getStyleClass().add("dialog-save-button");
        closeButton.setStyle(String.format("-fx-font-size: %dpx;", get("BonusResultsButtonFontSize")));
        closeButton.setOnAction(_ -> {
            bonusResultsStage.close();
            bonusResultsStage = null;
        });

        layout.getChildren().addAll(titleLabel, payoutLabel, closeButton);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);

        bonusResultsStage.setScene(scene);
        bonusResultsStage.setWidth(500);
        bonusResultsStage.setHeight(400);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), layout);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        bonusResultsStage.show();
        Funtions.centerStage(bonusResultsStage, root.getScene());
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hiba");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startBalancePolling() {
        if (game != null && game.isOnline()) {
            // Timer létrehozása 5 másodperces intervallummal
            balancePollingTimer = new Timeline(new KeyFrame(Duration.seconds(5), _ -> {
                // Háttérben futtatjuk a balance lekérést, hogy ne blokkoljuk a UI-t
                new Thread(() -> {
                    try {
                        game.updateBalanceFromServerWithNotification();
                    } catch (Exception e) {
                        System.err.println("Balance polling error: " + e.getMessage());
                    }
                }).start();
            }));
            
            balancePollingTimer.setCycleCount(Timeline.INDEFINITE);
            balancePollingTimer.play();
            
            System.out.println("Balance polling indítva (5 másodperces intervallum)");
        }
    }

    private void stopBalancePolling() {
        if (balancePollingTimer != null) {
            balancePollingTimer.stop();
            balancePollingTimer = null;
            System.out.println("Balance polling leállítva");
        }
    }

    @Override
    public void stop() {
        stopBalancePolling();
        // Itt lehetne más cleanup is
    }

    public static void main(String[] args) {
        launch(args);
    }
}
