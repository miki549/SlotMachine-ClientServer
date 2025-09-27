package com.example.slotmachine.client;

import com.example.slotmachine.ConfigManager;
import com.example.slotmachine.ResourceLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.prefs.Preferences;

import static com.example.slotmachine.ConfigManager.get;

public class ServerConfigDialog {
    private static final String SERVER_URL_KEY = "server_url";
    private static final String PC_SERVER_URL = "http://46.139.211.149:8081";
    private static final String LAPTOP_SERVER_URL = "http://46.139.211.149:8082";
    
    private final Preferences prefs;
    private String serverUrl;
    private MediaPlayer buttonClickSound;
    private double volume;
    
    // Static variable to track if server config dialog is open
    private static boolean isServerConfigDialogOpen = false;
    
    public ServerConfigDialog() {
        this(0.2); // Default volume for backward compatibility
    }
    
    public ServerConfigDialog(double volume) {
        prefs = Preferences.userNodeForPackage(ServerConfigDialog.class);
        // Use saved URL or default to PC server without checking availability
        serverUrl = prefs.get(SERVER_URL_KEY, PC_SERVER_URL);
        this.volume = volume;
        
        // Initialize button click sound with the provided volume
        buttonClickSound = ResourceLoader.loadSound("buttonclick1.mp3", volume);
    }
    
    // Static method to check if server config dialog is open
    public static boolean isServerConfigDialogOpen() {
        return isServerConfigDialogOpen;
    }
    
    
    public String showAndWait(Stage owner) {
        // Check if server config dialog is already open
        if (isServerConfigDialogOpen) {
            return null;
        }
        
        // Set flag to indicate server config dialog is open
        isServerConfigDialogOpen = true;
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.NONE);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.TRANSPARENT);
        
        // Make dialog follow parent window movement
        owner.xProperty().addListener((obs, oldVal, newVal) -> {
            if (dialog.isShowing()) {
                dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
            }
        });
        
        owner.yProperty().addListener((obs, oldVal, newVal) -> {
            if (dialog.isShowing()) {
                dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2 + 20);
            }
        });
        dialog.setResizable(false);
        
        // Title label
        Label titleLabel = new Label("Server Settings");
        titleLabel.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: white; -fx-font-weight: bold;", get("ServerConfigTitleFontSize")));
        
        // Server URL label
        Label serverLabel = new Label("Server URL:");
        serverLabel.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: white;", get("ServerConfigLabelFontSize")));
        
        // Server URL input field
        TextField serverField = new TextField(serverUrl);
        serverField.setPrefWidth(get("ServerConfigFieldWidth"));
        serverField.setStyle(String.format("-fx-font-size: %dpx;", get("ServerConfigFieldFontSize")));
        
        // Example label
        Label exampleLabel = new Label("Example: http://46.139.211.149:8081 (PC server), http://46.139.211.149:8082 (laptop server)");
        exampleLabel.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: gray;", get("ServerConfigExampleFontSize")));
        exampleLabel.setWrapText(true);
        exampleLabel.setMaxWidth(get("ServerConfigFieldWidth"));
        
        // Test button
        Button testButton = new Button("Test Connection");
        testButton.setPrefWidth(get("ServerConfigButtonWidth"));
        testButton.setPrefHeight(get("ServerConfigButtonHeight"));
        testButton.setStyle(String.format("-fx-font-size: %dpx;", get("ServerConfigButtonFontSize")));
        testButton.getStyleClass().add("dialog-save-button");
        addSoundToWidget(testButton);
        
        // OK and Cancel buttons
        Button okButton = new Button("OK");
        okButton.setPrefWidth(get("ServerConfigButtonWidth"));
        okButton.setPrefHeight(get("ServerConfigButtonHeight"));
        okButton.setStyle(String.format("-fx-font-size: %dpx;", get("ServerConfigButtonFontSize")));
        okButton.getStyleClass().add("dialog-save-button");
        addSoundToWidget(okButton);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(get("ServerConfigButtonWidth"));
        cancelButton.setPrefHeight(get("ServerConfigButtonHeight"));
        cancelButton.setStyle(String.format("-fx-font-size: %dpx;", get("ServerConfigButtonFontSize")));
        cancelButton.getStyleClass().add("dialog-save-button");
        addSoundToWidget(cancelButton);
        
        // Status label
        Label statusLabel = new Label();
        statusLabel.setStyle(String.format("-fx-text-fill: rgb(135, 206, 235); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(get("ServerConfigStatusMaxWidth"));
        statusLabel.setAlignment(Pos.CENTER);
        
        // Close button
        Button closeButton = new Button();
        closeButton.getStyleClass().add("dialog-close-button");
        closeButton.setPrefSize(25, 25);
        addSoundToWidget(closeButton);
        
        // Main layout
        VBox mainLayout = new VBox(get("ServerConfigLayoutSpacing"));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(get("ServerConfigLayoutPadding")));
        
        // Button container
        VBox buttonContainer = new VBox(get("ServerConfigButtonContainerSpacing"));
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.getChildren().addAll(testButton, okButton, cancelButton);
        
        // Add all elements to main layout
        mainLayout.getChildren().addAll(
            titleLabel, serverLabel, serverField, exampleLabel, 
            buttonContainer, statusLabel
        );
        
        // Style the main layout
        mainLayout.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10;");
        mainLayout.setEffect(new DropShadow(get("ServerConfigDropShadowRadius"), Color.BLACK));
        
        // Create rounded corners for main layout
        Rectangle mainClip = new Rectangle(get("ServerConfigDialogWidth"), get("ServerConfigDialogHeight"));
        mainClip.setArcWidth(get("ServerConfigClipArcWidth"));
        mainClip.setArcHeight(get("ServerConfigClipArcHeight"));
        mainLayout.setClip(mainClip);
        
        // Stack pane for close button positioning
        StackPane root = new StackPane(mainLayout, closeButton);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(10, 10, 0, 0));
        
        // Create a separate clip for the root StackPane to ensure rounded corners
        Rectangle rootClip = new Rectangle(get("ServerConfigDialogWidth"), get("ServerConfigDialogHeight"));
        rootClip.setArcWidth(get("ServerConfigClipArcWidth"));
        rootClip.setArcHeight(get("ServerConfigClipArcHeight"));
        root.setClip(rootClip);
        
        testButton.setOnAction(_ -> {
            String testUrl = serverField.getText().trim();
            if (testUrl.isEmpty()) {
                statusLabel.setText("❌ Please enter server URL!");
                statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
                return;
            }
            
            // Disable button and show testing status
            testButton.setDisable(true);
            testButton.setText("Testing...");
            statusLabel.setText("🔄 Testing connection...");
            statusLabel.setStyle(String.format("-fx-text-fill: rgb(135, 206, 235); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
            
            // Aszinkron tesztelés
            Thread testThread = new Thread(() -> { 
                try {
                    ApiClient testClient = new ApiClient(testUrl);
                    boolean connected = testClient.isConnected();
                    
                    Platform.runLater(() -> {
                        // Re-enable button and reset text
                        testButton.setDisable(false);
                        testButton.setText("Test Connection");
                        
                        if (connected) {
                            statusLabel.setText("✅ Connection successful!");
                            statusLabel.setStyle(String.format("-fx-text-fill: rgb(152,255,152); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
                        } else {
                            statusLabel.setText("❌ Failed to connect to server!");
                            statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        // Re-enable button and reset text
                        testButton.setDisable(false);
                        testButton.setText("Test Connection");
                        
                        statusLabel.setText("❌ Error: " + ex.getMessage());
                        statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
                    });
                }
            });
            testThread.setDaemon(true);
            testThread.start();
        });
        
        okButton.setOnAction(_ -> {
            String newUrl = serverField.getText().trim();
            if (!newUrl.isEmpty()) {
                // Ensure URL has protocol
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "http://" + newUrl;
                }
                serverUrl = newUrl;
                prefs.put(SERVER_URL_KEY, serverUrl);
                dialog.close();
            } else {
                statusLabel.setText("❌ Please enter server URL!");
                statusLabel.setStyle(String.format("-fx-text-fill: rgb(255,71,74); -fx-font-size: %dpx;", get("ServerConfigStatusFontSize")));
            }
        });
        
        cancelButton.setOnAction(_ -> {
            serverUrl = null;
            dialog.close();
        });
        
        closeButton.setOnAction(_ -> {
            serverUrl = null;
            dialog.close();
        });
        
        // Enter key support
        serverField.setOnAction(_ -> okButton.fire());
        
        // Create scene
        Scene scene = new Scene(root, get("ServerConfigDialogWidth"), get("ServerConfigDialogHeight"));
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add("file:src/main/resources/configs/normalstyle.css");
        
        dialog.setScene(scene);
        
        // Position dialog slightly lower than center
        dialog.setOnShown(e -> {
            dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
            dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2 + 20); // 50px lower than center
        });
        
        // Add listener to reset flag when dialog is closed
        dialog.setOnHidden(e -> {
            isServerConfigDialogOpen = false;
        });
        
        dialog.showAndWait();
        
        return serverUrl;
    }
    
    public String getServerUrl() {
        return prefs.get(SERVER_URL_KEY, PC_SERVER_URL);
    }
    
    public void saveServerUrl(String url) {
        prefs.put(SERVER_URL_KEY, url);
    }
    
    /**
     * Update the volume for button click sounds
     */
    public void setVolume(double volume) {
        this.volume = volume;
        if (buttonClickSound != null) {
            buttonClickSound.setVolume(volume);
        }
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
}
