package com.example.slotmachine.client;

import com.example.slotmachine.server.dto.LoginResponse;
import com.example.slotmachine.ConfigManager;
import com.example.slotmachine.MainMenu;
import com.example.slotmachine.ResourceLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import static com.example.slotmachine.ConfigManager.get;

public class LoginDialog {
    private final ApiClient apiClient;
    private LoginResponse loginResponse;
    private boolean cancelled = false;
    private MediaPlayer buttonClickSound;
    
    // Static variable to track if login dialog is open
    private static boolean isLoginDialogOpen = false;

    public LoginDialog(ApiClient apiClient) {
        this.apiClient = apiClient;
        
        // Initialize button click sound with fixed volume 0.2
        buttonClickSound = ResourceLoader.loadSound("buttonclick1.mp3", 0.2);
    }
    
    // Static method to check if login dialog is open
    public static boolean isLoginDialogOpen() {
        return isLoginDialogOpen;
    }

    public LoginResponse showAndWait(Stage parentStage) {
        // Check if login dialog is already open
        if (isLoginDialogOpen) {
            return null;
        }
        
        // Check if server config dialog is open - if so, don't allow login to open
        if (ServerConfigDialog.isServerConfigDialogOpen()) {
            return null;
        }
        
        // Check if settings dialog is open - if so, don't allow login to open
        if (MainMenu.isSettingsDialogOpen()) {
            return null;
        }
        
        // Set flag to indicate login dialog is open
        isLoginDialogOpen = true;
        
        Stage loginStage = new Stage();
        loginStage.initStyle(StageStyle.TRANSPARENT);
        loginStage.initModality(Modality.NONE);
        loginStage.initOwner(parentStage);
        
        // Make dialog follow parent window movement
        parentStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (loginStage.isShowing()) {
                loginStage.setX(parentStage.getX() + (parentStage.getWidth() - loginStage.getWidth()) / 2);
            }
        });
        
        parentStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (loginStage.isShowing()) {
                loginStage.setY(parentStage.getY() + (parentStage.getHeight() - loginStage.getHeight()) / 2);
            }
        });

        // UI elements
        Label titleLabel = new Label("Login");
        titleLabel.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: white; -fx-font-weight: bold;", get("LoginTitleFontSize")));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(get("LoginFieldWidth"));
        usernameField.setStyle(String.format("-fx-font-size: %dpx;", get("LoginFieldFontSize")));

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(get("LoginFieldWidth"));
        passwordField.setStyle(String.format("-fx-font-size: %dpx;", get("LoginFieldFontSize")));

        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(get("LoginButtonWidth"));
        loginButton.setPrefHeight(get("LoginButtonHeight"));
        loginButton.setStyle(String.format("-fx-font-size: %dpx;", get("LoginButtonFontSize")));
        loginButton.getStyleClass().add("login-button");
        addSoundToWidget(loginButton);

        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(get("LoginButtonWidth"));
        registerButton.setPrefHeight(get("LoginButtonHeight"));
        registerButton.setStyle(String.format("-fx-font-size: %dpx;", get("LoginButtonFontSize")));
        registerButton.getStyleClass().add("register-button");
        addSoundToWidget(registerButton);

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(get("LoginButtonWidth"));
        cancelButton.setPrefHeight(get("LoginButtonHeight"));
        cancelButton.setStyle(String.format("-fx-font-size: %dpx;", get("LoginButtonFontSize")));
        cancelButton.getStyleClass().add("cancel-button");
        addSoundToWidget(cancelButton);

        Label statusLabel = new Label();
        statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(get("LoginStatusMaxWidth")); // Allow text wrapping for long messages
        statusLabel.setAlignment(Pos.CENTER); // Center align the text
        statusLabel.setTextAlignment(TextAlignment.CENTER); // Center align multi-line text
        // Set fixed height to prevent layout shifting when text wraps
        statusLabel.setPrefHeight(get("LoginStatusFontSize") * 5); // Fixed height for 2-3 lines
        statusLabel.setMinHeight(get("LoginStatusFontSize") * 5); // Minimum height
        statusLabel.setMaxHeight(get("LoginStatusFontSize") * 5); // Maximum height

        // Event handlers
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill in all fields!");
                statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
                return;
            }

            loginButton.setDisable(true);
            statusLabel.setText("Logging in...");
            statusLabel.setStyle(String.format("-fx-text-fill: rgb(135, 206, 235); -fx-font-size: %dpx;", get("LoginStatusFontSize")));

            // Háttérben futtatjuk a hálózati hívást
            new Thread(() -> {
                try {
                    LoginResponse response = apiClient.login(username, password);
                    
                    Platform.runLater(() -> {
                        loginResponse = response;
                        loginStage.close();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText(ex.getMessage());
                        statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
                        loginButton.setDisable(false);
                    });
                }
            }).start();
        });

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill in all fields!");
                statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
                return;
            }

            registerButton.setDisable(true);
            statusLabel.setText("Registering...");
            statusLabel.setStyle(String.format("-fx-text-fill: rgb(135, 206, 235); -fx-font-size: %dpx;", get("LoginStatusFontSize")));

            new Thread(() -> {
                try {
                    apiClient.register(username, password);
                    
                    Platform.runLater(() -> {
                        statusLabel.setText("Registration successful! Please log in now.");
                        statusLabel.setStyle(String.format("-fx-text-fill: rgb(152,255,152); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
                        registerButton.setDisable(false);
                        usernameField.clear();
                        passwordField.clear();
                        usernameField.requestFocus();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText(ex.getMessage());
                        statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("LoginStatusFontSize")));
                        registerButton.setDisable(false);
                    });
                }
            }).start();
        });

        cancelButton.setOnAction(e -> {
            cancelled = true;
            loginStage.close();
        });

        // Enter lenyomásra bejelentkezés
        passwordField.setOnAction(e -> loginButton.fire());

        VBox layout = new VBox(get("LoginLayoutSpacing"));
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(get("LoginLayoutPadding")));
        
        // Create a container for buttons to keep them together
        VBox buttonContainer = new VBox(get("LoginButtonContainerSpacing"));
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(loginButton, registerButton, cancelButton);
        
        layout.getChildren().addAll(
            titleLabel, usernameField, passwordField, 
            buttonContainer, statusLabel
        );

        layout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10;");
        layout.setEffect(new DropShadow(get("LoginDropShadowRadius"), Color.BLACK));

        // Lekerekített sarkok
        Rectangle clip = new Rectangle(get("LoginDialogWidth"), get("LoginDialogHeight"));
        clip.setArcWidth(get("LoginClipArcWidth"));
        clip.setArcHeight(get("LoginClipArcHeight"));
        layout.setClip(clip);

        Scene scene = new Scene(layout, get("LoginDialogWidth"), get("LoginDialogHeight"));
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");

        loginStage.setScene(scene);
        loginStage.setResizable(false);
        
        // Set all window sizes consistently
        setWindowSize(loginStage, scene, layout, clip);

        // Középre igazítás
        loginStage.setOnShown(e -> {
            loginStage.setX(parentStage.getX() + (parentStage.getWidth() - loginStage.getWidth()) / 2);
            loginStage.setY(parentStage.getY() + (parentStage.getHeight() - loginStage.getHeight()) / 2);
        });

        // Fókusz beállítása
        Platform.runLater(usernameField::requestFocus);
        
        // Add listener to reset flag when dialog is closed
        loginStage.setOnHidden(e -> {
            isLoginDialogOpen = false;
        });

        loginStage.showAndWait();

        return cancelled ? null : loginResponse;
    }
    
    /**
     * Helper method to add sound to widgets
     */
    private void addSoundToWidget(Control obj) {
        obj.setOnMousePressed(_ -> {
            buttonClickSound.stop();
            buttonClickSound.seek(Duration.ZERO);
            buttonClickSound.play();
        });
    }
    
    /**
     * Helper method to set window size consistently across all components
     */
    private void setWindowSize(Stage stage, Scene scene, VBox layout, Rectangle clip) {
        // Load window size from config
        double windowWidth = get("LoginDialogWidth");
        double windowHeight = get("LoginDialogHeight");
        
        // Set layout size
        layout.setPrefSize(windowWidth, windowHeight);
        layout.setMinSize(windowWidth, windowHeight);
        layout.setMaxSize(windowWidth, windowHeight);
        
        // Set clip size
        clip.setWidth(windowWidth);
        clip.setHeight(windowHeight);
        
        // Set stage size
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
    }
}
