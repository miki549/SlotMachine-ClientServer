package com.example.slotmachine;

import javafx.animation.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve; // Új import
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.slotmachine.ConfigManager.get;

/**
 * Pénzérme animáció osztály - repülő és pörgő pénzérmék kezelése
 */
public class CoinAnimation {

    private static final int COIN_COUNT = 12; // coin1.png - coin12.png
    private static final double TARGET_OFFSET_MAX = 50.0; // Max. pixel eltérés a célponttól (természetesebb szórás)

    private Pane container;
    private List<ImageView> activeCoins;
    private List<Timeline> activeSpinTimelines; // Pörgés animációk tárolása
    private List<PathTransition> activePathTransitions; // Pálya animációk tárolása
    private Random random;
    private Timeline coinGeneratorTimeline;
    private long animationStartTime;
    private int coinSize;
    private int coinGenerationInterval;
    private int coinsPerGeneration;

    public CoinAnimation(Pane container) {
        this.container = container;
        this.activeCoins = new ArrayList<>();
        this.activeSpinTimelines = new ArrayList<>();
        this.activePathTransitions = new ArrayList<>(); // Inicializálás
        this.random = new Random();
        this.coinSize = get("CoinSize"); // Konfigurációból olvassuk az érme méretét
        this.coinGenerationInterval = get("CoinGenerationInterval"); // Generálási intervallum
        this.coinsPerGeneration = get("CoinsPerGeneration"); // Érmék száma generálásonként

        // Biztonsági ellenőrzés - tisztítsuk a container-t
        container.getChildren().clear();
    }

    public void startCoinAnimation(double gameWidth, double gameHeight) {
        startCoinAnimation(gameWidth, gameHeight, Double.MAX_VALUE);
    }

    /**
     * Elindítja a pénzérme animációt - folyamatos érme generálással
     * @param stopCreatingCoinsAt idő másodpercben, amikor le kell állítani az új érmék létrehozását
     */
    public void startCoinAnimation(double gameWidth, double gameHeight, double stopCreatingCoinsAt) {
        stopCoinAnimation();
        animationStartTime = System.currentTimeMillis();

        coinGeneratorTimeline = new Timeline(new KeyFrame(Duration.millis(coinGenerationInterval), e -> {
            double elapsedTime = (System.currentTimeMillis() - animationStartTime) / 1000.0;

            if (elapsedTime >= stopCreatingCoinsAt - 0.3) {
                System.out.println("Stopping coin generation - elapsed: " + elapsedTime + ", stop at: " + stopCreatingCoinsAt);
                coinGeneratorTimeline.stop();
                return;
            }

            for (int i = 0; i < coinsPerGeneration; i++) {

                // --- Bal oldalról érkező érme (kezdeti pozíció) ---
                double leftStartX = -coinSize;
                double leftStartY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);

                // Véletlenszerű célpozíció a természetesebb mozgáshoz
                double leftTargetX = gameWidth + coinSize + random.nextDouble() * TARGET_OFFSET_MAX;
                double leftTargetY = (gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8))
                        + (random.nextBoolean() ? TARGET_OFFSET_MAX : -TARGET_OFFSET_MAX) * random.nextDouble();

                createFlyingCoinImmediate(leftStartX, leftStartY, leftTargetX, leftTargetY);

                // --- Jobb oldalról érkező érme (kezdeti pozíció) ---
                double rightStartX = gameWidth + coinSize;
                double rightStartY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);

                // Véletlenszerű célpozíció a természetesebb mozgáshoz
                double rightTargetX = -coinSize - random.nextDouble() * TARGET_OFFSET_MAX;
                double rightTargetY = (gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8))
                        + (random.nextBoolean() ? TARGET_OFFSET_MAX : -TARGET_OFFSET_MAX) * random.nextDouble();

                createFlyingCoinImmediate(rightStartX, rightStartY, rightTargetX, rightTargetY);
            }
        }));

        coinGeneratorTimeline.setCycleCount(Timeline.INDEFINITE);
        coinGeneratorTimeline.play();

        System.out.println("Starting continuous coin generation until " + stopCreatingCoinsAt + " seconds");
    }

    /**
     * Létrehoz egy repülő pénzérmét azonnal (késleltetés nélkül)
     */
    private void createFlyingCoinImmediate(double startX, double startY, double targetX, double targetY) {
        ImageView coin = createCoinImageView();

        // Kezdeti pozíció beállítása
        coin.setX(startX);
        coin.setY(startY);

        container.getChildren().add(coin);
        activeCoins.add(coin);

        // Fizikai animáció - PathTransition
        animateCoinPhysics(coin, startX, startY, targetX, targetY);
    }

    /**
     * Létrehoz egy pénzérme ImageView-t véletlenszerű képpel
     */
    private ImageView createCoinImageView() {
        int coinFrame = random.nextInt(COIN_COUNT) + 1; // 1-12
        String imagePath = "src/main/resources/pictures/coins/coin" + coinFrame + ".png";

        ImageView coin = new ImageView(new Image("file:" + imagePath));
        coin.setFitWidth(coinSize);
        coin.setFitHeight(coinSize);
        coin.setPreserveRatio(true);

        return coin;
    }

    /**
     * Fizikai animáció - VALÓSÁGHŰBB Parabola pálya CubicCurve és PathTransition segítségével
     */
    private void animateCoinPhysics(ImageView coin, double startX, double startY, double targetX, double targetY) {
        // Pörgés animáció
        startCoinSpinning(coin);

        // --- TERMÉSZETESEBB REPÜLÉS PARAMÉTEREI ---
        double duration = 1200 + random.nextDouble() * 800; // 1.2s - 2.0s
        double maxHeight = 120 + random.nextDouble() * 150; // 120px - 270px

        // A CubicCurve control pontjával adjuk meg a parabola ívét
        CubicCurve parabola = new CubicCurve();
        parabola.setStartX(startX);
        parabola.setStartY(startY);
        parabola.setEndX(targetX);
        parabola.setEndY(targetY);

        // A control pontok magassága (Y) dönti el az ív magasságát
        // A valósághoz híven a control pontok X-pozíciója is véletlenszerű lehet, hogy aszimmetrikus legyen az ív

        // Középpont X-koordinátája (ideális esetben itt van az ív csúcsa)
        double midX = (startX + targetX) / 2;
        double controlPoint1X = midX + (random.nextDouble() - 0.5) * 150; // Enyhe eltolás X-ben
        double controlPoint2X = midX + (random.nextDouble() - 0.5) * 150; // Enyhe eltolás X-ben

        // Control pontok Y-koordinátája (adja az ív magasságát)
        // A control pontoknak a startY és targetY értéknél SOKKAL kisebbnek kell lenniük (feljebb),
        // hogy az ív a maxHeight-nek megfelelő magasságot elérje.
        double controlY = Math.min(startY, targetY) - maxHeight;
        parabola.setControlX1(controlPoint1X);
        parabola.setControlY1(controlY);
        parabola.setControlX2(controlPoint2X);
        parabola.setControlY2(controlY);

        // --- PATH TRANSITION (VALÓS FIZIKA ÉRZÉS) ---
        PathTransition pathTransition = new PathTransition(Duration.millis(duration), parabola, coin);

        // LINEAR interpolátor: a sebesség a görbén (a path hossza mentén) lineárisan változik.
        // Mivel a CubicCurve Y-görbéje exponenciális, ez a kombináció hozza a legközelebb a lassuló/gyorsuló mozgást.
        pathTransition.setInterpolator(Interpolator.LINEAR);

        // Transition tárolása a leállításhoz
        activePathTransitions.add(pathTransition);

        pathTransition.setOnFinished(e -> removeCoin(coin, pathTransition));
        pathTransition.play();
    }

    /**
     * Pénzérme pörgés animáció - képek váltogatása (véletlenszerű sebesség)
     */
    private void startCoinSpinning(ImageView coin) {
        Timeline spinTimeline = new Timeline();
        spinTimeline.setCycleCount(Animation.INDEFINITE);

        // Véletlenszerű intervallum (70ms-tól 130ms-ig) - Változatos pörgési sebesség
        double spinInterval = 70 + random.nextDouble() * 60;

        final int[] currentFrame = {1};

        spinTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(spinInterval), e -> {
                    currentFrame[0] = (currentFrame[0] % COIN_COUNT) + 1;
                    String imagePath = "src/main/resources/pictures/coins/coin" + currentFrame[0] + ".png";
                    coin.setImage(new Image("file:" + imagePath));
                })
        );

        activeSpinTimelines.add(spinTimeline);
        spinTimeline.play();
    }

    /**
     * Eltávolít egy pénzérmét
     */
    private void removeCoin(ImageView coin, PathTransition pathTransition) {
        // Leállítjuk és eltávolítjuk a Pálya animációt
        pathTransition.stop();
        activePathTransitions.remove(pathTransition);

        // Megkeressük a coin indexét és eltávolítjuk a hozzá tartozó spin timeline-t
        int coinIndex = activeCoins.indexOf(coin);
        if (coinIndex >= 0 && coinIndex < activeSpinTimelines.size()) {
            Timeline spinTimeline = activeSpinTimelines.get(coinIndex);
            spinTimeline.stop();
            activeSpinTimelines.remove(coinIndex);
        }

        container.getChildren().remove(coin);
        activeCoins.remove(coin);
    }

    /**
     * Leállítja az összes pénzérme animációt
     */
    public void stopCoinAnimation() {
        if (coinGeneratorTimeline != null) {
            coinGeneratorTimeline.stop();
            coinGeneratorTimeline = null;
        }

        // Összes Pálya animáció leállítása
        for (PathTransition pt : new ArrayList<>(activePathTransitions)) {
            pt.stop();
        }
        activePathTransitions.clear();

        // Összes pörgés animáció leállítása
        for (Timeline spinTimeline : new ArrayList<>(activeSpinTimelines)) {
            spinTimeline.stop();
        }
        activeSpinTimelines.clear();

        // Összes aktív érme eltávolítása
        for (ImageView coin : new ArrayList<>(activeCoins)) {
            container.getChildren().remove(coin);
        }
        activeCoins.clear();

        System.out.println("Coin animation stopped - all timelines and coins cleared");
    }

    /**
     * Visszaadja az aktív érmék számát
     */
    public int getActiveCoinCount() {
        return activeCoins.size();
    }
}