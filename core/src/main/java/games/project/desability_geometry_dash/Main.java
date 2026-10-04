package games.project.desability_geometry_dash;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {

    private SpriteBatch spriteBatch;
    private FitViewport viewport;
    private FitViewport uiViewport;

    // =========================
    // UI
    // =========================

    private Stage stage;
    private Skin skin;
    private Label powerUpLabel;
    private float powerUpLabelTimer = 0f;

    private Label gameOverLabel;
    private Label shootInstructionLabel;
    private Label moveInstructionLabel;
    private Label scoreLabel;

    // =========================
    // Status do Jogador
    // =========================
    private int playerLives = 3;
    private int score = 0;
    private Texture lifeTexture;
    private boolean isGameOver = false;

    // =========================
    // Texturas
    // =========================

    private Texture backgroundTexture;
    private Texture starshipTexture;
    private Texture laserTexture;
    private Texture largeAsteroidTexture;
    private Texture smallAsteroidTexture;
    private Texture powerUpTexture;

    // =========================
    // Paralaxe
    // =========================

    private float backgroundOffsetX = 0f;
    private float backgroundOffsetY = 0f;
    private float parallaxSpeed = 0.5f;

    // =========================
    // Animações
    // =========================

    private Animation<TextureRegion> flyingAnimation;
    private Texture[] flyingTextures;

    private Animation<TextureRegion> explosionAnimation;
    private Texture[] explosionTextures;

    // =========================
    // Entidades do Jogo
    // =========================

    private PlayerStarship player;
    private Array<GameObject> gameObjects;
    private Array<Explosion> explosions;
    private Array<Sprite> laserSprites;

    // =========================
    // Timers e Parâmetros
    // =========================

    private float laserSpeed = 6f;
    private float asteroidTimer = 0f;
    private float powerUpTimer = 0f;
    private float powerUpSpawnInterval = 3f;

    // =========================
    // Sons
    // =========================

    private Sound collisionSound;
    private Sound explosionSound;
    private Sound shootSound;
    private Music backgroundMusic;

    // =========================
    // Arduino
    // =========================
    private UltrassonicSensorReader sensorReader;

    @Override
    public void create() {

        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(8, 5);
        uiViewport = new FitViewport(800, 500); // Resolução para UI em pixels

        // =========================
        // INTERFACE
        // =========================

        stage = new Stage(uiViewport);

        skin = new Skin(Gdx.files.internal("uiskin.json"));
        skin.getFont("default-font").getRegion().getTexture().setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        float padding = 15f;
        float heartSize = 32f;

        // --- Label Score ---
        scoreLabel = new Label("Score: 0", skin);
        scoreLabel.setFontScale(0.6f); // Fonte menor para não poluir
        scoreLabel.pack();
        scoreLabel.setPosition(padding, uiViewport.getWorldHeight() - scoreLabel.getHeight() - padding);
        stage.addActor(scoreLabel);

        // --- Label Power Up ---
        powerUpLabel = new Label("+speed!", skin);
        powerUpLabel.pack();

        float posX = (uiViewport.getWorldWidth() - powerUpLabel.getWidth()) / 2f;
        float posY = uiViewport.getWorldHeight() - powerUpLabel.getHeight() - 0.2f;

        powerUpLabel.setPosition(Math.round(posX), Math.round(posY));
        powerUpLabel.setVisible(false);
        stage.addActor(powerUpLabel);

        // --- Label Game Over ---
        Label.LabelStyle gameOverStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.RED);
        gameOverLabel = new Label("GAME OVER\nPress R to restart", gameOverStyle);
        gameOverLabel.setAlignment(Align.center);
        gameOverLabel.pack();

        float goPosX = (uiViewport.getWorldWidth() - gameOverLabel.getWidth()) / 2f;
        float goPosY = (uiViewport.getWorldHeight() - gameOverLabel.getHeight()) / 2f;

        gameOverLabel.setPosition(Math.round(goPosX), Math.round(goPosY));
        gameOverLabel.setVisible(false);
        stage.addActor(gameOverLabel);

        // --- Labels de Instruções ---
        shootInstructionLabel = new Label("SPACE: Shoot", skin);
        moveInstructionLabel = new Label("HAND: Move", skin);

        // Reduz o tamanho da fonte para 0.6f
        shootInstructionLabel.setFontScale(0.6f);
        moveInstructionLabel.setFontScale(0.6f);

        shootInstructionLabel.pack();
        moveInstructionLabel.pack();

        float heartsBottomY = uiViewport.getWorldHeight() - heartSize - padding;

        shootInstructionLabel.setPosition(
            Math.round(uiViewport.getWorldWidth() - shootInstructionLabel.getWidth() - padding),
            Math.round(heartsBottomY - shootInstructionLabel.getHeight() - 24f)
        );
        moveInstructionLabel.setPosition(
            Math.round(uiViewport.getWorldWidth() - moveInstructionLabel.getWidth() - padding),
            Math.round(heartsBottomY - shootInstructionLabel.getHeight() - moveInstructionLabel.getHeight() - 20f)
        );

        stage.addActor(shootInstructionLabel);
        stage.addActor(moveInstructionLabel);

        // =========================
        // CARREGAMENTO DE TEXTURAS
        // =========================

        backgroundTexture = new Texture("background.jpg");
        starshipTexture = new Texture("starship.png");
        laserTexture = new Texture("laser.png");
        largeAsteroidTexture = new Texture("asteroid.png");
        smallAsteroidTexture = new Texture("large_asteroid.png");
        powerUpTexture = new Texture("power_up.png");
        lifeTexture = new Texture("life.png");

        // =========================
        // ANIMAÇÃO DA NAVE
        // =========================

        flyingTextures = new Texture[2];
        TextureRegion[] flyingFrames = new TextureRegion[2];

        for (int i = 0; i < 2; i++) {
            flyingTextures[i] =
                new Texture(String.format("flying_part_%d.png", i + 1));

            flyingFrames[i] =
                new TextureRegion(flyingTextures[i]);
        }

        flyingAnimation = new Animation<>(0.1f, flyingFrames);
        flyingAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // =========================
        // ANIMAÇÃO DA EXPLOSÃO
        // =========================

        explosionTextures = new Texture[3];
        TextureRegion[] explosionFrames = new TextureRegion[3];

        for (int i = 0; i < 3; i++) {
            explosionTextures[i] =
                new Texture(String.format("explosion_part_%d.png", i + 1));

            explosionFrames[i] =
                new TextureRegion(explosionTextures[i]);
        }

        explosionAnimation = new Animation<>(0.1f, explosionFrames);

        // =========================
        // INICIALIZAÇÃO DOS OBJETOS
        // =========================

        Sprite playerSprite = new Sprite(starshipTexture);
        playerSprite.setSize(1, 1);

        // =========================
        // ARDUINO
        // =========================
        sensorReader = new UltrassonicSensorReader("COM3");
        sensorReader.start();

        player = new PlayerStarship(
            playerSprite,
            viewport,
            flyingAnimation,
            sensorReader
        );

        player.setPosition(3.5f, 0.5f);

        gameObjects = new Array<>();
        explosions = new Array<>();
        laserSprites = new Array<>();

        spawnPowerUp();

        // =========================
        // ÁUDIO
        // =========================


        collisionSound =
            Gdx.audio.newSound(
                Gdx.files.internal("starship_collision.mp3")
            );

        shootSound =
            Gdx.audio.newSound(
                Gdx.files.internal("shoot_sound.mp3")
            );

        explosionSound =
            Gdx.audio.newSound(
                Gdx.files.internal("explosion_sound.mp3")
            );

        backgroundMusic =
            Gdx.audio.newMusic(
                Gdx.files.internal("space_sound.mp3")
            );

        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();


    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void render() {
        if (!isGameOver) {
            input();
            logic();
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                restartGame();
            }
        }

        draw();
    }

    private void input() {

        float delta = Gdx.graphics.getDeltaTime();
        float speed = player.getSpeed();

        backgroundOffsetX -=
            speed * delta * parallaxSpeed;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            createLaser();
        }
    }

    private void logic() {

        float delta = Gdx.graphics.getDeltaTime();
        float worldHeight = viewport.getWorldHeight();

        if (powerUpLabelTimer > 0) {

            powerUpLabelTimer -= delta;

            if (powerUpLabelTimer <= 0) {
                powerUpLabelTimer = 0;
                powerUpLabel.setVisible(false);
            }
        }

        player.update(delta);

        for (int i = laserSprites.size - 1; i >= 0; i--) {

            Sprite laser = laserSprites.get(i);

            laser.translateY(laserSpeed * delta);

            if (laser.getY() > worldHeight) {
                laserSprites.removeIndex(i);
            }
        }

        asteroidTimer += delta;

        if (asteroidTimer > 1.5f) {
            asteroidTimer = 0;
            spawnRandomAsteroid();
        }

        powerUpTimer += delta;

        if (powerUpTimer > powerUpSpawnInterval) {
            powerUpTimer = 0;
            spawnPowerUp();
        }

        Array<GameObject> toRemove = new Array<>();
        Array<GameObject> toAdd = new Array<>();

        for (GameObject obj : gameObjects) {

            obj.update(delta);

            if (obj.getY() + obj.getHeight() < 0) {
                toRemove.add(obj);
            }
        }

        processCollisions(toRemove, toAdd);

        gameObjects.removeAll(toRemove, true);
        gameObjects.addAll(toAdd);

        for (int i = explosions.size - 1; i >= 0; i--) {

            Explosion exp = explosions.get(i);

            exp.stateTime += delta;

            if (explosionAnimation.isAnimationFinished(exp.stateTime)) {
                explosions.removeIndex(i);
            }
        }
    }

    private void processCollisions(
        Array<GameObject> toRemove,
        Array<GameObject> toAdd
    ) {

        for (int i = laserSprites.size - 1; i >= 0; i--) {

            Sprite laser = laserSprites.get(i);

            for (GameObject obj : gameObjects) {

                if (
                    obj instanceof Asteroid &&
                        laser.getBoundingRectangle()
                            .overlaps(obj.getCollisionBox())
                ) {

                    createExplosion(obj);
                    toRemove.add(obj);
                    laserSprites.removeIndex(i);

                    if (obj instanceof LargeAsteroid) {
                        splitLargeAsteroid((LargeAsteroid) obj, toAdd);
                    } else if (obj instanceof SmallAsteroid) {
                        score += 5;
                        scoreLabel.setText("Score: " + score);
                    }

                    break;
                }
            }
        }

        for (GameObject obj : gameObjects) {

            if (isGameOver) continue;

            if (
                player.getCollisionBox()
                    .overlaps(obj.getCollisionBox())
            ) {

                if (obj instanceof PowerUp) {

                    player.increaseSpeed(player.getSpeed() * 0.1f);
                    showPowerUpMessage();
                    toRemove.add(obj);

                } else if (obj instanceof Asteroid) {

                    collisionSound.play();
                    createExplosion(obj);
                    toRemove.add(obj);

                    if (obj instanceof LargeAsteroid) {
                        splitLargeAsteroid((LargeAsteroid) obj, toAdd);
                    } else if (obj instanceof SmallAsteroid) {
                        playerLives -= 1;
                    }

                    if (playerLives <= 0) {
                        playerLives = 0;
                        isGameOver = true;
                        gameOverLabel.setVisible(true);
                        backgroundMusic.stop();
                    }
                }
            }
        }
    }

    private void showPowerUpMessage() {
        powerUpLabelTimer = 3f;
        powerUpLabel.setVisible(true);
    }

    private void splitLargeAsteroid(
        LargeAsteroid large,
        Array<GameObject> toAdd
    ) {

        for (int i = 0; i < 2; i++) {

            Sprite smallSprite =
                new Sprite(smallAsteroidTexture);

            smallSprite.setSize(0.5f, 0.5f);

            smallSprite.setPosition(
                large.getX() +
                    MathUtils.random(-0.2f, 0.2f),

                large.getY()
            );

            SmallAsteroid small =
                new SmallAsteroid(smallSprite);

            toAdd.add(small);
        }
    }

    private void spawnRandomAsteroid() {

        Sprite astSprite =
            new Sprite(largeAsteroidTexture);

        float worldWidth =
            viewport.getWorldWidth();

        float worldHeight =
            viewport.getWorldHeight();

        if (MathUtils.randomBoolean()) {

            astSprite.setSize(1f, 1f);

            astSprite.setPosition(
                MathUtils.random(0f, worldWidth - 1f),
                worldHeight
            );

            gameObjects.add(
                new LargeAsteroid(astSprite)
            );

        } else {

            astSprite.setSize(0.5f, 0.5f);

            astSprite.setPosition(
                MathUtils.random(
                    0f,
                    worldWidth - 0.5f
                ),
                worldHeight
            );

            gameObjects.add(
                new SmallAsteroid(astSprite)
            );
        }
    }

    private void spawnPowerUp() {

        Sprite pSprite =
            new Sprite(powerUpTexture);

        float size = 0.6f;

        pSprite.setSize(size, size);

        float spawnX =
            MathUtils.random(
                0f,
                viewport.getWorldWidth() - size
            );

        float spawnY =
            viewport.getWorldHeight() - size;

        pSprite.setPosition(
            spawnX,
            spawnY
        );

        gameObjects.add(
            new PowerUp(pSprite)
        );
    }

    private void createLaser() {

        Sprite laserSprite =
            new Sprite(laserTexture);

        laserSprite.setSize(
            0.2f,
            0.6f
        );

        laserSprite.setX(
            player.getX() +
                (player.getWidth() -
                    laserSprite.getWidth()) / 2f
        );

        laserSprite.setY(
            player.getY() +
                player.getHeight()
        );

        laserSprites.add(laserSprite);

        shootSound.play();
    }

    private void createExplosion(GameObject obj) {

        Sprite expSprite =
            new Sprite(
                explosionAnimation.getKeyFrame(0)
            );

        expSprite.setSize(
            obj.getWidth(),
            obj.getHeight()
        );

        expSprite.setPosition(
            obj.getX(),
            obj.getY()
        );

        explosions.add(
            new Explosion(expSprite)
        );

        explosionSound.play();
    }

    private void draw() {

        ScreenUtils.clear(Color.BLACK);

        viewport.apply();

        spriteBatch.setProjectionMatrix(
            viewport.getCamera().combined
        );

        spriteBatch.begin();

        float worldWidth =
            viewport.getWorldWidth();

        float worldHeight =
            viewport.getWorldHeight();

        float x1 =
            backgroundOffsetX % worldWidth;

        float y1 =
            backgroundOffsetY % worldHeight;

        if (x1 > 0) {
            x1 -= worldWidth;
        }

        if (y1 > 0) {
            y1 -= worldHeight;
        }

        spriteBatch.draw(
            backgroundTexture,
            x1,
            y1,
            worldWidth,
            worldHeight
        );

        spriteBatch.draw(
            backgroundTexture,
            x1 + worldWidth,
            y1,
            worldWidth,
            worldHeight
        );

        spriteBatch.draw(
            backgroundTexture,
            x1,
            y1 + worldHeight,
            worldWidth,
            worldHeight
        );

        spriteBatch.draw(
            backgroundTexture,
            x1 + worldWidth,
            y1 + worldHeight,
            worldWidth,
            worldHeight
        );

        for (Sprite laser : laserSprites) {
            laser.draw(spriteBatch);
        }

        if (!isGameOver) {
            player.draw(spriteBatch);

            TextureRegion flyingFrame =
                player.getFlyingFrame();

            if (flyingFrame != null) {

                spriteBatch.draw(
                    flyingFrame,
                    player.getX(),
                    player.getY(),
                    player.getWidth(),
                    player.getHeight()
                );
            }
        }

        for (GameObject obj : gameObjects) {
            obj.draw(spriteBatch);
        }

        for (Explosion exp : explosions) {

            TextureRegion frame =
                explosionAnimation.getKeyFrame(
                    exp.stateTime
                );

            exp.sprite.setRegion(frame);

            exp.sprite.draw(spriteBatch);
        }

        spriteBatch.end();

        uiViewport.apply();
        spriteBatch.setProjectionMatrix(uiViewport.getCamera().combined);
        spriteBatch.begin();

        float heartSize = 50f;
        float padding = 15f;

        for (int i = 0; i < playerLives; i++) {
            float x = uiViewport.getWorldWidth() - padding - (i + 1) * (heartSize + padding);
            float y = uiViewport.getWorldHeight() - heartSize - padding;

            spriteBatch.draw(lifeTexture, x, y, heartSize, heartSize);
        }

        spriteBatch.end();

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    private void restartGame() {
        isGameOver = false;
        playerLives = 3;
        score = 0;

        gameOverLabel.setVisible(false);
        powerUpLabel.setVisible(false);
        powerUpLabelTimer = 0f;
        scoreLabel.setText("Score: " + score);

        gameObjects.clear();
        laserSprites.clear();
        explosions.clear();

        asteroidTimer = 0f;
        powerUpTimer = 0f;

        Sprite playerSprite = new Sprite(starshipTexture);
        playerSprite.setSize(1, 1);

        player = new PlayerStarship(
            playerSprite,
            viewport,
            flyingAnimation,
            sensorReader
        );
        player.setPosition(3.5f, 0.5f);

        spawnPowerUp();

        backgroundMusic.play();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {

        spriteBatch.dispose();

        stage.dispose();
        skin.dispose();

        backgroundTexture.dispose();
        starshipTexture.dispose();
        laserTexture.dispose();
        largeAsteroidTexture.dispose();
        smallAsteroidTexture.dispose();
        powerUpTexture.dispose();
        lifeTexture.dispose();

        for (Texture t : explosionTextures) {
            t.dispose();
        }

        for (Texture t : flyingTextures) {
            t.dispose();
        }

        collisionSound.dispose();
        explosionSound.dispose();
        shootSound.dispose();
        backgroundMusic.dispose();
    }
}
