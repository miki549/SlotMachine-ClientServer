package com.example.slotmachine.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.prefs.Preferences;

public class ServerConfigDialog {
    private static final String SERVER_URL_KEY = "server_url";
    private static final String DEFAULT_SERVER_URL = "http://46.139.211.149:8081";
    private static final String LOCALHOST_URL = "http://localhost:8081";
    
    private final Preferences prefs;
    private String serverUrl;
    
    public ServerConfigDialog() {
        prefs = Preferences.userNodeForPackage(ServerConfigDialog.class);
        serverUrl = prefs.get(SERVER_URL_KEY, getPreferredDefaultUrl());
    }
    
    private String getPreferredDefaultUrl() {
        // Check if localhost server is available
        if (isLocalServerAvailable()) {
            return LOCALHOST_URL;
        }
        return DEFAULT_SERVER_URL;
    }
    
    private boolean isLocalServerAvailable() {
        try {
            ApiClient testClient = new ApiClient(LOCALHOST_URL);
            return testClient.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String showAndWait(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Szerver Beállítások");
        dialog.setResizable(false);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        Label serverLabel = new Label("Szerver cím:");
        TextField serverField = new TextField(serverUrl);
        serverField.setPrefWidth(300);
        
        Label exampleLabel = new Label("Példa: http://localhost:8081 (helyi szerver), http://46.139.211.149:8081 (külső szerver), http://192.168.1.100:8081 (helyi hálózat)");
        exampleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        Button testButton = new Button("Kapcsolat Tesztelése");
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Mégse");
        
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px;");
        
        grid.add(serverLabel, 0, 0);
        grid.add(serverField, 1, 0);
        grid.add(exampleLabel, 1, 1);
        grid.add(testButton, 1, 2);
        grid.add(statusLabel, 1, 3);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(okButton, cancelButton);
        grid.add(buttonBox, 1, 4);
        
        testButton.setOnAction(_ -> {
            String testUrl = serverField.getText().trim();
            if (testUrl.isEmpty()) {
                statusLabel.setText("❌ Kérem adja meg a szerver címét!");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
                return;
            }
            
            statusLabel.setText("🔄 Kapcsolat tesztelése...");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: blue;");
            
            // Aszinkron tesztelés
            Thread testThread = new Thread(() -> {
                try {
                    ApiClient testClient = new ApiClient(testUrl);
                    boolean connected = testClient.isConnected();
                    
                    Platform.runLater(() -> {
                        if (connected) {
                            statusLabel.setText("✅ Kapcsolat sikeres!");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
                        } else {
                            statusLabel.setText("❌ Nem sikerült csatlakozni a szerverhez!");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Hiba: " + ex.getMessage());
                        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
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
                statusLabel.setText("❌ Kérem adja meg a szerver címét!");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
            }
        });
        
        cancelButton.setOnAction(_ -> {
            serverUrl = null;
            dialog.close();
        });
        
        // Enter key support
        serverField.setOnAction(_ -> okButton.fire());
        
        Scene scene = new Scene(grid);
        dialog.setScene(scene);
        dialog.showAndWait();
        
        return serverUrl;
    }
    
    public String getServerUrl() {
        return prefs.get(SERVER_URL_KEY, getPreferredDefaultUrl());
    }
    
    public void saveServerUrl(String url) {
        prefs.put(SERVER_URL_KEY, url);
    }
}
