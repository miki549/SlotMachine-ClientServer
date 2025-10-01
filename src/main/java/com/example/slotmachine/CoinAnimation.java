package com.example.slotmachine;

import javafx.animation.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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

    private Pane container;
    private List<ImageView> activeCoins;
    private List<Timeline> activeSpinTimelines; // Pörgés animációk tárolása
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
        this.random = new Random();
        this.coinSize = get("CoinSize"); // Konfigurációból olvassuk az érme méretét
        this.coinGenerationInterval = get("CoinGenerationInterval"); // Generálási intervallum
        this.coinsPerGeneration = get("CoinsPerGeneration"); // Érmék száma generálásonként
        
        // Biztonsági ellenőrzés - tisztítsuk a container-t
        container.getChildren().clear();
    }
    
    /**
     * Elindítja a pénzérme animációt - két oldalról ferdén felfelé dobódnak be
     */
    public void startCoinAnimation(double gameWidth, double gameHeight) {
        startCoinAnimation(gameWidth, gameHeight, Double.MAX_VALUE); // Default: no stop time
    }
    
    /**
     * Elindítja a pénzérme animációt - folyamatos érme generálással
     * @param stopCreatingCoinsAt idő másodpercben, amikor le kell állítani az új érmék létrehozását
     */
    public void startCoinAnimation(double gameWidth, double gameHeight, double stopCreatingCoinsAt) {
        // Tisztítás
        stopCoinAnimation();
        
        // Animáció kezdési idejének beállítása
        animationStartTime = System.currentTimeMillis();
        
        // Folyamatos érme generálás Timeline-nal
        // Mindkét oldalról egyszerre generálunk érméket konfigurálható intervallumonként
        coinGeneratorTimeline = new Timeline(new KeyFrame(Duration.millis(coinGenerationInterval), e -> {
            double elapsedTime = (System.currentTimeMillis() - animationStartTime) / 1000.0;
            
            // Ha elértük a stop időt, állítsuk le a generálást
            if (elapsedTime >= stopCreatingCoinsAt - 0.3) { // 0.3s biztonsági margó a befejezés előtt
                System.out.println("Stopping coin generation - elapsed: " + elapsedTime + ", stop at: " + stopCreatingCoinsAt);
                coinGeneratorTimeline.stop();
                return;
            }
            
            // Több érme generálása mindkét oldalról
            for (int i = 0; i < coinsPerGeneration; i++) {
                // Bal oldalról érkező érme
                double leftStartY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);
                double leftTargetX = gameWidth + coinSize;
                double leftTargetY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);
                createFlyingCoinImmediate(
                    -coinSize,
                    leftStartY,
                    leftTargetX,
                    leftTargetY,
                    gameWidth,
                    gameHeight
                );
                
                // Jobb oldalról érkező érme
                double rightStartY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);
                double rightTargetX = -coinSize;
                double rightTargetY = gameHeight * 0.1 + random.nextDouble() * (gameHeight * 0.8);
                createFlyingCoinImmediate(
                    gameWidth + coinSize,
                    rightStartY,
                    rightTargetX,
                    rightTargetY,
                    gameWidth,
                    gameHeight
                );
            }
        }));
        
        coinGeneratorTimeline.setCycleCount(Timeline.INDEFINITE);
        coinGeneratorTimeline.play();
        
        System.out.println("Starting continuous coin generation until " + stopCreatingCoinsAt + " seconds");
    }
    
    /**
     * Létrehoz egy repülő pénzérmét azonnal (késleltetés nélkül)
     */
    private void createFlyingCoinImmediate(double startX, double startY, double targetX, double targetY, 
                                          double gameWidth, double gameHeight) {
        ImageView coin = createCoinImageView();
        
        // Kezdeti pozíció beállítása - középre helyezzük, majd translate-tel mozgatjuk
        coin.setLayoutX(container.getWidth() / 2 - coinSize / 2);
        coin.setLayoutY(container.getHeight() / 2 - coinSize / 2);
        
        // Kezdeti translate értékek beállítása
        coin.setTranslateX(startX - (container.getWidth() / 2 - coinSize / 2));
        coin.setTranslateY(startY - (container.getHeight() / 2 - coinSize / 2));
        
        container.getChildren().add(coin);
        activeCoins.add(coin);
        
        // Fizikai animáció - parabola pálya
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
     * Fizikai animáció - parabola pálya, gravitáció, pattogás
     */
    private void animateCoinPhysics(ImageView coin, double startX, double startY, double targetX, double targetY) {
        // Pörgés animáció
        startCoinSpinning(coin);
        
        // Folyamatos parabola animáció Timeline-nal
        double duration = 1000 + random.nextDouble() * 500; 
        
        // Számítsuk ki a translate értékeket
        double centerX = container.getWidth() / 2 - coinSize / 2;
        double centerY = container.getHeight() / 2 - coinSize / 2;
        
        double currentTranslateX = coin.getTranslateX();
        double currentTranslateY = coin.getTranslateY();
        
        // Parabola paraméterek
        double maxHeight = 150 + random.nextDouble() * 50; // 150-250 pixel magasság
        double peakTime = duration * 0.4; // 40%-ban éri el a csúcsot
        
        // Timeline a folyamatos parabola animációhoz
        Timeline parabolaTimeline = new Timeline();
        
        // X mozgás (lineáris)
        parabolaTimeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO, new KeyValue(coin.translateXProperty(), currentTranslateX)),
            new KeyFrame(Duration.millis(duration), new KeyValue(coin.translateXProperty(), targetX - centerX))
        );
        
        // Y mozgás (parabola)
        parabolaTimeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO, new KeyValue(coin.translateYProperty(), currentTranslateY)),
            new KeyFrame(Duration.millis(peakTime), new KeyValue(coin.translateYProperty(), currentTranslateY - maxHeight, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(duration), new KeyValue(coin.translateYProperty(), targetY - centerY, Interpolator.EASE_IN))
        );
        
        // Egyszerű parabola animáció - fade out nélkül
        parabolaTimeline.setOnFinished(e -> removeCoin(coin));
        parabolaTimeline.play();
    }
    
    /**
     * Pénzérme pörgés animáció - képek váltogatása
     */
    private void startCoinSpinning(ImageView coin) {
        Timeline spinTimeline = new Timeline();
        spinTimeline.setCycleCount(Animation.INDEFINITE);
        
        final int[] currentFrame = {1};
        
        spinTimeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(100), e -> { // 10 FPS pörgés
                currentFrame[0] = (currentFrame[0] % COIN_COUNT) + 1;
                String imagePath = "src/main/resources/pictures/coins/coin" + currentFrame[0] + ".png";
                coin.setImage(new Image("file:" + imagePath));
            })
        );
        
        // Pörgés animáció tárolása a leállításhoz
        activeSpinTimelines.add(spinTimeline);
        spinTimeline.play();
    }
    
    /**
     * Eltávolít egy pénzérmét
     */
    private void removeCoin(ImageView coin) {
        // Leállítjuk az érme pörgés animációját
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
        // Generátor Timeline leállítása
        if (coinGeneratorTimeline != null) {
            coinGeneratorTimeline.stop();
            coinGeneratorTimeline = null;
        }
        
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