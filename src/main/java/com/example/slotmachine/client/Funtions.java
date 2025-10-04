package com.example.slotmachine.client;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Funtions {

    /**
     * Középre igazít egy Stage-et egy másik Stage-hez képest, és mozgásnál is középen tartja
     * @param stageToCenter A középre igazítandó Stage
     * @param parentStage A referencia Stage
     */
    public static void centerStage(Stage stageToCenter, Stage parentStage) {
        // Először középre igazítjuk
        Platform.runLater(() -> {
            if (stageToCenter != null && stageToCenter.isShowing()) {
                double centerX = parentStage.getX() + (parentStage.getWidth() - stageToCenter.getWidth()) / 2;
                double centerY = parentStage.getY() + (parentStage.getHeight() - stageToCenter.getHeight()) / 2;

                stageToCenter.setX(centerX);
                stageToCenter.setY(centerY);
            }
        });

        // Hozzáadjuk a mozgatás követést
        parentStage.xProperty().addListener((_, _, _) -> {
            if (stageToCenter != null && stageToCenter.isShowing()) {
                Platform.runLater(() -> {
                    if (stageToCenter.isShowing()) {
                        double centerX = parentStage.getX() + (parentStage.getWidth() - stageToCenter.getWidth()) / 2;
                        stageToCenter.setX(centerX);
                    }
                });
            }
        });

        parentStage.yProperty().addListener((_, _, _) -> {
            if (stageToCenter != null && stageToCenter.isShowing()) {
                Platform.runLater(() -> {
                    if (stageToCenter.isShowing()) {
                        double centerY = parentStage.getY() + (parentStage.getHeight() - stageToCenter.getHeight()) / 2;
                        stageToCenter.setY(centerY);
                    }
                });
            }
        });
    }

    /**
     * Középre igazít egy Stage-et egy Scene-hez képest, és mozgásnál is középen tartja
     * @param stageToCenter A középre igazítandó Stage
     * @param parentScene A referencia Scene
     */
    public static void centerStage(Stage stageToCenter, Scene parentScene) {
        Window parentWindow = parentScene.getWindow();
        if (parentWindow instanceof Stage) {
            centerStage(stageToCenter, (Stage) parentWindow);
        }
    }
}