package games.project.desability_geometry_dash;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.EnumMap;
import java.util.Locale;

public class Main implements ApplicationListener {
    private enum State { TITLE, TUTORIAL, PREPARE, PERFECT, CALIBRATE, COUNTDOWN, PLAY, PAUSED, RESULT, GAME_OVER, WIN, SHOW_RESULTS }
    private static final float UW = 1280f, UH = 720f, MIN_DISTANCE = 15f;
    private static final float SPACE_HOLD_SECONDS = .5f;
    private static final String[] RESULTS = {"Hmm, o bolo não cresceu...", "Hmm, falta uma cobertura...", "Perfeito!"};
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private FitViewport world, ui;
    private BitmapFont font;
    private Texture background, life;
    private Texture tutorialLivesTexture, tutorialObjectivesTexture;
    private Texture[] baker, cakes;
    private Animation<TextureRegion> bakerAnimation;
    private EnumMap<FallingIngredient.Type, Texture> ingredientTextures;
    private final Array<FallingIngredient> ingredients = new Array<>();
    private Chef chef;
    private UltrassonicSensorReader sensor;
    private boolean sensorConnected;
    private String sensorMessage = "Conectando ao sensor na COM3...";
    private Sound catchSound, missSound;
    private Music music;
    private State state = State.TITLE;
    private State pausedState = State.PLAY;
    private boolean spaceDown, spaceHoldTriggered;
    private float spaceHoldTime;
    private float timer, spawnTimer, maxDistance, amplitude;
    private final float[] phaseMinDistance = new float[3];
    private final float[] phaseMaxDistance = new float[3];
    private static final float MIN_MOVEMENT_DELTA_CM = 1f;
    private boolean hasPreviousSensorSample;
    private float previousSensorDistance, segmentDistanceCm, segmentDurationSeconds;
    private long previousSensorTimestampNanos, phaseMeasurementStartNanos;
    private int movementDirection;
    private float totalMotionDistanceCm, totalMotionDurationSeconds, peakVelocityCmPerSecond;
    private int phase = 1, lives = 3, score, hiScore, phaseStartScore;
    private EnumMap<FallingIngredient.Type, Integer> required, collected;

    @Override public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        world = new FitViewport(16, 9);
        ui = new FitViewport(UW, UH);
        background = new Texture("background.jpg");
        background.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font = new BitmapFont(Gdx.files.internal("default.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        life = keyed("life.png");
        baker = new Texture[] {keyed("baker.png"), keyed("baker_pos2.png")};
        bakerAnimation = new Animation<>(0.24f, new TextureRegion(baker[0]), new TextureRegion(baker[1]));
        bakerAnimation.setPlayMode(Animation.PlayMode.LOOP);
        chef = new Chef(new Sprite(baker[0]), world, bakerAnimation);
        chef.setSize(2.35f, 1.65f);
        chef.setPosition((world.getWorldWidth() - chef.getWidth()) / 2f, 0.2f);
        ingredientTextures = new EnumMap<>(FallingIngredient.Type.class);
        for (FallingIngredient.Type t : FallingIngredient.Type.values()) ingredientTextures.put(t, keyed(t.textureFile));
        cakes = new Texture[] {keyed("cake-1.png"), keyed("cake-2.png"), keyed("cake-3.png")};

        tutorialLivesTexture = new Texture("tutorial-lifes.png");
        tutorialObjectivesTexture = new Texture("tutorial-obj-score.png");

        tutorialLivesTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tutorialObjectivesTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        required = requirements(1);
        collected = new EnumMap<>(FallingIngredient.Type.class);
        resetMovementMetrics();
        hiScore = Gdx.app.getPreferences("carrot-cake-game").getInteger("hiScore", 0);
        catchSound = Gdx.audio.newSound(Gdx.files.internal("catch.mp3"));
        missSound = Gdx.audio.newSound(Gdx.files.internal("miss.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        music.setLooping(true);
        music.setVolume(0.35f);
        music.play();
        connectSensor();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int key) {
                if (key != Input.Keys.SPACE) return false;
                if (spaceDown) return true;
                spaceDown = true;

                if (state == State.TITLE || state == State.WIN) {
                    spaceHoldTime = 0f;
                    spaceHoldTriggered = false;
                    return true;
                }
                if (state == State.GAME_OVER) { restartPhase(); return true; }
                if (state == State.PLAY || state == State.COUNTDOWN) {
                    pausedState = state;
                    state = State.PAUSED;
                    music.pause();
                    return true;
                }
                if (state == State.PAUSED) {
                    state = pausedState;
                    music.play();
                    return true;
                }
                return true;
            }

            @Override public boolean keyUp(int key) {
                if (key != Input.Keys.SPACE) return false;
                if (!spaceDown) return true;
                spaceDown = false;

                if (state == State.TUTORIAL) {
                    state = State.TITLE;
                } else if (state == State.TITLE && !spaceHoldTriggered) {
                    if (sensorConnected) beginPreparation();
                    else connectSensor();
                } else if (state == State.WIN && !spaceHoldTriggered) {
                    state = State.SHOW_RESULTS;
                } else if (state == State.SHOW_RESULTS) {
                    state = State.WIN;
                }

                spaceHoldTime = 0f;
                spaceHoldTriggered = false;
                return true;
            }
        });
    }
    private void connectSensor() {
        if (sensor != null) sensor.stop();
        sensor = new UltrassonicSensorReader("COM3");
        try {
            sensor.start(); sensorConnected = true; sensorMessage = "Sensor conectado.";
        } catch (RuntimeException ex) {
            sensorConnected = false;
            sensorMessage = "Sensor indisponível. Verifique a COM3 e tente novamente.";
            System.err.println(ex.getMessage());
        }
        chef.setSensorReader(sensor);
    }

    private Texture keyed(String file) {
        Pixmap pix = new Pixmap(Gdx.files.internal(file));
        Color key = new Color(), sample = new Color();
        Color.rgba8888ToColor(key, pix.getPixel(0, 0));
        int kr = Math.round(key.r * 255), kg = Math.round(key.g * 255), kb = Math.round(key.b * 255);
        int left = pix.getWidth(), top = pix.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < pix.getHeight(); y++) for (int x = 0; x < pix.getWidth(); x++) {
            Color.rgba8888ToColor(sample, pix.getPixel(x, y));
            boolean isKey = sample.a > .5f
                && Math.abs(Math.round(sample.r * 255) - kr) < 14
                && Math.abs(Math.round(sample.g * 255) - kg) < 14
                && Math.abs(Math.round(sample.b * 255) - kb) < 14;
            if (isKey) {
                pix.setColor(0, 0, 0, 0);
                pix.drawPixel(x, y);
            } else if (sample.a > .05f) {
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        if (right >= left && bottom >= top) {
            Pixmap cropped = new Pixmap(right - left + 1, bottom - top + 1, pix.getFormat());
            cropped.setBlending(Pixmap.Blending.None);
            cropped.drawPixmap(pix, 0, 0, left, top, cropped.getWidth(), cropped.getHeight());
            pix.dispose();
            pix = cropped;
        }
        Texture result = new Texture(pix);
        result.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pix.dispose();
        return result;
    }

    private void beginPreparation() { state = State.PREPARE; timer = 0; }
    @Override public void resize(int w, int h) { world.update(w, h, true); ui.update(w, h, true); }
    @Override public void render() { update(Math.min(Gdx.graphics.getDeltaTime(), .05f)); draw(); }

    private void update(float dt) {
        if ((state == State.TITLE || state == State.WIN) && spaceDown && !spaceHoldTriggered) {
            spaceHoldTime += dt;
            if (spaceHoldTime >= SPACE_HOLD_SECONDS) {
                spaceHoldTriggered = true;
                if (state == State.TITLE) state = State.TUTORIAL;
                else restartWholeGame();
            }
        }
        switch (state) {
            case PREPARE:
                if (sensorConnected && sensor.getDistance() > 0 && sensor.getDistance() <= MIN_DISTANCE) {
                    state = State.PERFECT; timer = 2f;
                }
                break;
            case PERFECT:
                if ((timer -= dt) <= 0) { state = State.CALIBRATE; timer = 5f; }
                break;
            case CALIBRATE:
                if ((timer -= dt) <= 0) {
                    maxDistance = sensor.getDistance();
                    if (maxDistance <= MIN_DISTANCE) timer = 5f;
                    else { amplitude = maxDistance - MIN_DISTANCE; chef.configureCalibration(MIN_DISTANCE, maxDistance, amplitude); startPhase(1); }
                }
                break;
            case COUNTDOWN:
                recordSensorSample();
                chef.update(dt);
                if ((timer -= dt) <= 0) state = State.PLAY;
                break;
            case PLAY: updateGame(dt); break;
            case RESULT:
                if ((timer -= dt) <= 0) { if (phase < 3) startPhase(phase + 1); else state = State.WIN; }
                break;
            default: break;
        }
    }

    private void updateGame(float dt) {
        recordSensorSample();
        chef.update(dt);
        if ((spawnTimer += dt) >= .82f) { spawnTimer = 0; spawnIngredient(); }
        Rectangle basket = chef.getBasketBounds();
        for (int i = ingredients.size - 1; i >= 0; i--) {
            FallingIngredient item = ingredients.get(i);
            item.update(dt);
            if (item.getY() + item.getHeight() < 0) ingredients.removeIndex(i);
            else if (basket.overlaps(item.getCollisionBox())) {
                ingredients.removeIndex(i); handleCatch(item.getType());
                if (state != State.PLAY) break;
            }
        }
    }

    private void spawnIngredient() {
        Array<FallingIngredient.Type> needed = new Array<>();
        for (FallingIngredient.Type t : required.keySet())
            if (collected.getOrDefault(t, 0) < required.get(t)) needed.add(t);
        FallingIngredient.Type type = needed.size > 0 && MathUtils.randomBoolean(.72f)
            ? needed.random() : FallingIngredient.Type.values()[MathUtils.random(FallingIngredient.Type.values().length - 1)];
        Sprite sprite = new Sprite(ingredientTextures.get(type));
        float itemHeight = .95f; float itemWidth = itemHeight * sprite.getTexture().getWidth() / sprite.getTexture().getHeight(); sprite.setSize(itemWidth, itemHeight);
        sprite.setPosition(MathUtils.random(0, world.getWorldWidth() - sprite.getWidth()), world.getWorldHeight() - sprite.getHeight());
        ingredients.add(new FallingIngredient(sprite, type, MathUtils.random(1.7f, 2.4f)));
    }

    private void handleCatch(FallingIngredient.Type type) {
        int count = collected.getOrDefault(type, 0), target = required.getOrDefault(type, 0);
        if (target > count) {
            collected.put(type, count + 1);
            score += lives == 3 ? 5 : lives == 2 ? 3 : 1;
            if (score > hiScore) {
                hiScore = score;
                Gdx.app.getPreferences("carrot-cake-game").putInteger("hiScore", hiScore).flush();
            }
            catchSound.play();
            if (phaseComplete()) finishPhase();
        } else {
            lives--; missSound.play();
            if (lives <= 0) { lives = 0; score = phaseStartScore; finishMotionSegment(); resetSensorMotionBaseline(); state = State.GAME_OVER; music.stop(); }
        }
    }

    private boolean phaseComplete() {
        for (FallingIngredient.Type t : required.keySet())
            if (collected.getOrDefault(t, 0) < required.get(t)) return false;
        return true;
    }

    private EnumMap<FallingIngredient.Type, Integer> requirements(int n) {
        EnumMap<FallingIngredient.Type, Integer> r = new EnumMap<>(FallingIngredient.Type.class);
        r.put(FallingIngredient.Type.EGG, 3); r.put(FallingIngredient.Type.FLOUR, 2);
        r.put(FallingIngredient.Type.CARROT, 3); r.put(FallingIngredient.Type.SUGAR, 1);
        if (n >= 2) r.put(FallingIngredient.Type.YEAST, 1);
        if (n >= 3) r.put(FallingIngredient.Type.CHOCOLATE, 1);
        return r;
    }
    private void recordSensorSample() {
        if (sensor == null || !sensorConnected) return;
        UltrassonicSensorReader.Reading reading = sensor.getLatestReading();
        long timestamp = reading.getTimestampNanos();
        float distance = reading.getDistanceCm();
        if (timestamp <= 0L || timestamp < phaseMeasurementStartNanos || distance <= 0f || timestamp == previousSensorTimestampNanos) return;

        int phaseIndex = phase - 1;
        if (phaseIndex < 0 || phaseIndex >= phaseMinDistance.length) return;
        phaseMinDistance[phaseIndex] = Math.min(phaseMinDistance[phaseIndex], distance);
        phaseMaxDistance[phaseIndex] = Math.max(phaseMaxDistance[phaseIndex], distance);

        if (hasPreviousSensorSample) {
            float elapsed = (timestamp - previousSensorTimestampNanos) / 1_000_000_000f;
            float change = distance - previousSensorDistance;
            float traveled = Math.abs(change);
            if (elapsed > 0f) {
                if (traveled >= MIN_MOVEMENT_DELTA_CM) {
                    int direction = change > 0f ? 1 : -1;
                    if (movementDirection != 0 && direction != movementDirection) finishMotionSegment();
                    movementDirection = direction;
                    segmentDistanceCm += traveled;
                    segmentDurationSeconds += elapsed;
                    float velocity = traveled / elapsed;
                    if (Float.isFinite(velocity)) peakVelocityCmPerSecond = Math.max(peakVelocityCmPerSecond, velocity);
                } else if (movementDirection != 0) {
                    segmentDurationSeconds += elapsed;
                }
            }
        }

        previousSensorDistance = distance;
        previousSensorTimestampNanos = timestamp;
        hasPreviousSensorSample = true;
    }

    private void finishMotionSegment() {
        if (segmentDistanceCm > 0f && segmentDurationSeconds > 0f) {
            totalMotionDistanceCm += segmentDistanceCm;
            totalMotionDurationSeconds += segmentDurationSeconds;
            peakVelocityCmPerSecond = Math.max(peakVelocityCmPerSecond, segmentDistanceCm / segmentDurationSeconds);
        }
        segmentDistanceCm = 0f;
        segmentDurationSeconds = 0f;
        movementDirection = 0;
    }

    private void resetSensorMotionBaseline() {
        hasPreviousSensorSample = false;
        previousSensorDistance = 0f;
        previousSensorTimestampNanos = 0L;
        segmentDistanceCm = 0f;
        segmentDurationSeconds = 0f;
        movementDirection = 0;
    }

    private void resetMovementMetrics() {
        for (int i = 0; i < phaseMinDistance.length; i++) {
            phaseMinDistance[i] = Float.POSITIVE_INFINITY;
            phaseMaxDistance[i] = Float.NEGATIVE_INFINITY;
        }
        totalMotionDistanceCm = 0f;
        totalMotionDurationSeconds = 0f;
        phaseMeasurementStartNanos = 0L;
        peakVelocityCmPerSecond = 0f;
        resetSensorMotionBaseline();
    }

    private float averageVelocityCmPerSecond() {
        return totalMotionDurationSeconds > 0f ? totalMotionDistanceCm / totalMotionDurationSeconds : 0f;
    }

    private String phaseRangeText(int phaseIndex) {
        float minimum = phaseMinDistance[phaseIndex];
        float maximum = phaseMaxDistance[phaseIndex];
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || maximum < minimum)
            return String.format(Locale.ROOT, "Fase %d: sem leituras", phaseIndex + 1);
        return String.format(Locale.ROOT, "Fase %d: min %.1f cm | max %.1f cm | ROM %.1f cm",
            phaseIndex + 1, minimum, maximum, maximum - minimum);
    }

    private String velocityText(float velocity) {
        return velocity > 0f && Float.isFinite(velocity)
            ? String.format(Locale.ROOT, "%.1f cm/s", velocity) : "-- cm/s";
    }

    private void startPhase(int n) {
        finishMotionSegment();
        phase = n;
        phaseMinDistance[n - 1] = Float.POSITIVE_INFINITY;
        phaseMaxDistance[n - 1] = Float.NEGATIVE_INFINITY;
        phaseMeasurementStartNanos = System.nanoTime();
        resetSensorMotionBaseline();
        phaseStartScore = score;
        required = requirements(n);
        collected.clear();
        ingredients.clear();
        spawnTimer = 0f;
        state = State.COUNTDOWN;
        timer = 3f;
    }

    private void finishPhase() {
        finishMotionSegment();
        resetSensorMotionBaseline();
        ingredients.clear();
        if (phase == 3) state = State.WIN;
        else { state = State.RESULT; timer = 3f; }
    }
    private void restartPhase() { score = phaseStartScore; lives = 3; startPhase(phase); music.play(); }

    private void restartWholeGame() {
        score = 0;
        lives = 3;
        phase = 1;
        phaseStartScore = 0;
        maxDistance = 0f;
        amplitude = 0f;
        resetMovementMetrics();
        required = requirements(1);
        collected.clear();
        ingredients.clear();
        chef.setPosition((world.getWorldWidth() - chef.getWidth()) / 2f, 0.2f);
        beginPreparation();
    }

    private boolean isSetupScreen() {
        return state == State.TITLE || state == State.TUTORIAL || state == State.PREPARE || state == State.PERFECT || state == State.CALIBRATE;
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        world.apply(); batch.setProjectionMatrix(world.getCamera().combined); batch.begin();
        if (!isSetupScreen()) batch.draw(background, 0, 0, world.getWorldWidth(), world.getWorldHeight());
        if (!isSetupScreen() && state != State.RESULT && state != State.WIN && state != State.SHOW_RESULTS) {
            chef.draw(batch);
            if (state == State.PLAY || state == State.PAUSED || state == State.GAME_OVER)
                for (FallingIngredient item : ingredients) item.draw(batch);
        }
        batch.end();
        if (state == State.RESULT || state == State.WIN || state == State.GAME_OVER || state == State.PAUSED || state == State.SHOW_RESULTS) darken();
        ui.apply(); batch.setProjectionMatrix(ui.getCamera().combined);

        if (state == State.TUTORIAL) drawTutorialFrames();
        if (state == State.SHOW_RESULTS) drawResultsFrames();
        batch.begin();
        if (state == State.RESULT || state == State.WIN) drawCake();
        if (state == State.COUNTDOWN || state == State.PLAY || state == State.PAUSED || state == State.GAME_OVER) drawHud();
        if (state == State.TUTORIAL) drawTutorialImages();
        drawMessage(); batch.end();
    }

    private void darken() {
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(ui.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, state == State.GAME_OVER ? .68f : .76f);
        shapes.rect(0, 0, UW, UH); shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void drawTutorialFrames() {
        shapes.setProjectionMatrix(ui.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GOLD);
        shapes.rect(36f, 377f, 1208f, 216f);
        shapes.rect(36f, 207f, 1208f, 160f);
        shapes.rect(36f, 70f, 1208f, 127f);
        shapes.end();
    }

    private void drawResultsFrames() {
        shapes.setProjectionMatrix(ui.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GOLD);
        shapes.rect(36f, 490f, 1208f, 103f);
        shapes.rect(36f, 260f, 1208f, 220f);
        shapes.rect(36f, 100f, 1208f, 150f);
        shapes.end();
    }
    private void drawTutorialImages() {
        batch.draw(tutorialObjectivesTexture, 62f, 384f, 110f, 200.66f);

        batch.draw(tutorialLivesTexture, 62f, 120f, 230f, 56.83f);
        batch.draw(baker[0], 950f, 212f, 270f, 151f);
        batch.draw(cakes[2], 1010f, 429f, 200f, 111.64f);
    }
    private void drawCake() {
        Texture cake = cakes[phase - 1]; float w = 450, h = w * cake.getHeight() / cake.getWidth();
        batch.draw(cake, (UW - w) / 2, (UH - h) / 2 - 20, w, h);
    }
    private void drawHud() {
        drawText("SCORE " + String.format(Locale.ROOT, "%06d", score), 36, 688, .82f, Color.WHITE);
        drawText("HI-SCORE " + String.format(Locale.ROOT, "%06d", hiScore), 480, 688, .72f, Color.WHITE);
        drawText("FASE " + phase, 36, 620, .72f, Color.GOLD);
        for (int i = 0; i < lives; i++) batch.draw(life, 1125 + i * 48, 650, 42, 42);
        drawText("Objetivos:", 42, 558, .74f, Color.GOLD);
        float y = 500f;
        for (FallingIngredient.Type t : FallingIngredient.Type.values()) {
            int target = required.getOrDefault(t, 0);
            int remaining = target - collected.getOrDefault(t, 0);
            if (remaining <= 0) continue;

            Texture icon = ingredientTextures.get(t);
            float iconHeight = .95f * UW / world.getWorldWidth();
            float iconWidth = iconHeight * icon.getWidth() / (float) icon.getHeight();
            batch.draw(icon, 42f, y - iconHeight / 2f - 8f, iconWidth, iconHeight);
            drawText(remaining + "x", 52f + iconWidth, y, .55f, Color.WHITE);
            y -= 78f;
        }
    }

    private void drawMessage() {
        switch (state) {
            case TITLE:
                centered("Baker's Rush: Recipe Catch", 580, 1.0f, Color.GOLD);
                centered("Pressione e solte ESPA\u00c7O para iniciar.", 390f, .58f, Color.WHITE);
                centered("Segure ESPA\u00c7O para abrir o tutorial.", 350f, .58f, Color.WHITE);
                drawText(sensorMessage, 32f, 28f, .58f, sensorConnected ? Color.GREEN : Color.RED);
                break;
            case TUTORIAL:
                centered("COMO JOGAR", 675f, 1.0f, Color.GOLD);

                drawText("OBJETIVOS E PONTOS", 205f, 560f, .78f, Color.GOLD);
                drawText("Colete os itens pedidos.", 205f, 525f, .70f, Color.WHITE);
                drawText("3 vidas: +5 pontos.", 205f, 490f, .70f, Color.WHITE);
                drawText("2 vidas: +3 pontos.", 205f, 455f, .70f, Color.WHITE);
                drawText("1 vida: +1 ponto.", 205f, 420f, .70f, Color.WHITE);


                drawText("MOVIMENTO", 62f, 342f, .90f, Color.GOLD);
                drawText("Aproxime: esquerda.", 62f, 309f, .82f, Color.WHITE);
                drawText("Afaste: direita.", 62f, 278f, .82f, Color.WHITE);
                drawText("VIDAS E CONTROLES", 300f, 184f, .76f, Color.GOLD);
                drawText("Item errado ou extra: perde 1 vida.", 300f, 158f, .66f, Color.WHITE);
                drawText("3 perdas: Game Over. ESPA\u00c7O reinicia a fase.", 300f, 132f, .66f, Color.WHITE);
                drawText("ESPA\u00c7O pausa e retoma durante a fase.", 300f, 106f, .66f, Color.WHITE);
                centered("Solte ESPA\u00c7O para sair do tutorial.", 38f, .58f, Color.WHITE);
                break;
            case PREPARE:
                centered("Posicione uma mão na frente do sensor.", 395, .72f, Color.WHITE);
                if (sensorConnected && (sensor.getDistance() <= 0 || sensor.getDistance() > MIN_DISTANCE))
                    centered("Aproxime mais", 335, .66f, Color.RED);
                break;
            case PERFECT:
                centered("Perfeito!", 385, 1.1f, Color.GREEN);
                centered("Mantenha a mão nessa posição.", 325, .58f, Color.WHITE);
                break;
            case CALIBRATE:
                centered("Afaste a mão horizontalmente o máximo que puder.", 390, .65f, Color.WHITE);
                centered("Calibração: " + Math.max(0, (int)Math.ceil(timer)), 330, .75f, Color.GOLD);
                if (maxDistance > 0 && maxDistance <= MIN_DISTANCE)
                    centered("Afaste mais a mão para calibrar.", 275, .5f, Color.RED);
                break;
            case COUNTDOWN:
                centered("FASE " + phase, 410, 1.15f, Color.GOLD);
                centered("Começa em " + Math.max(1, (int)Math.ceil(timer)), 340, .75f, Color.WHITE);
                break;
            case RESULT:
                centered(RESULTS[phase - 1], 620, .62f, Color.WHITE);
                break;
            case GAME_OVER:
                centered("GAME OVER", 405, 1.35f, Color.RED);
                centered("Pressione ESPA\u00c7O para repetir a fase " + phase, 330, .66f, Color.WHITE);
                break;
            case PAUSED:
                centered("PAUSADO", 405, 1.2f, Color.GOLD);
                centered("Pressione ESPAÇO para continuar.", 330, .65f, Color.WHITE);
                break;
            case WIN:
                centered("Perfeito!", 620f, .92f, Color.GREEN);
                centered("Segure ESPA\u00c7O para reiniciar.", 190f, .62f, Color.WHITE);
                centered("Toque e solte ESPA\u00c7O para ver resultados.", 150f, .62f, Color.WHITE);
                break;
            case SHOW_RESULTS:
                centered("RESULTADOS", 675f, 1.0f, Color.GOLD);
                drawText("SCORE", 120f, 553f, .78f, Color.GOLD);
                drawText(String.format(Locale.ROOT, "%06d", score), 120f, 516f, .96f, Color.WHITE);
                drawText("HI-SCORE", 700f, 553f, .78f, Color.GOLD);
                drawText(String.format(Locale.ROOT, "%06d", hiScore), 700f, 516f, .96f, Color.WHITE);

                drawText("AMPLITUDE DE MOVIMENTO REAL (ROM)", 62f, 450f, .72f, Color.GOLD);
                drawText(phaseRangeText(0), 62f, 412f, .72f, Color.WHITE);
                drawText(phaseRangeText(1), 62f, 376f, .72f, Color.WHITE);
                drawText(phaseRangeText(2), 62f, 340f, .72f, Color.WHITE);

                drawText("PICO E M\u00c9DIA DE VELOCIDADE (SPEED)", 62f, 220f, .72f, Color.GOLD);
                drawText("Pico: " + velocityText(peakVelocityCmPerSecond), 62f, 182f, .72f, Color.WHITE);
                drawText("M\u00e9dia global: " + velocityText(averageVelocityCmPerSecond()), 62f, 145f, .72f, Color.WHITE);
                centered("ESPA\u00c7O: voltar \u00e0 tela final.", 40f, .55f, Color.WHITE);
                break;
            default:
                break;
        }
    }

    private void centered(String s, float y, float scale, Color color) { drawText(s, (UW - textWidth(s, scale)) / 2, y, scale, color); }

    private float textWidth(String s, float scale) {
        float width = 0;
        for (int i = 0; i < s.length(); i++) width += font.getData().getGlyph(base(s.charAt(i))).xadvance * scale;
        return width;
    }
    private void drawText(String s, float x, float y, float scale, Color color) {
        font.getData().setScale(scale); font.setColor(color);
        float line = font.getLineHeight();
        for (int i = 0; i < s.length(); i++) {
            char original = s.charAt(i), b = base(original);
            font.draw(batch, String.valueOf(b), x, y);
            String mark = mark(original);
            if (!mark.isEmpty()) {
                float markY = original == 'ç' || original == 'Ç' ? y - line * .12f : y + line * .25f;
                font.draw(batch, mark, x + font.getData().getGlyph(b).xadvance * scale * .08f, markY);
            }
            x += font.getData().getGlyph(b).xadvance * scale;
        }
        font.getData().setScale(1); font.setColor(Color.WHITE);
    }
    private char base(char c) {
        switch (c) {
            case 'ã': case 'á': case 'à': case 'â': return 'a'; case 'Ã': case 'Á': case 'À': case 'Â': return 'A';
            case 'é': case 'ê': case 'è': return 'e'; case 'É': case 'Ê': case 'È': return 'E';
            case 'í': case 'ì': return 'i'; case 'Í': case 'Ì': return 'I';
            case 'õ': case 'ó': case 'ò': case 'ô': return 'o'; case 'Õ': case 'Ó': case 'Ò': case 'Ô': return 'O';
            case 'ú': case 'ù': return 'u'; case 'Ú': case 'Ù': return 'U'; case 'ç': return 'c'; case 'Ç': return 'C';
            default: return c;
        }
    }
    private String mark(char c) {
        switch (c) {
            case 'ã': case 'õ': case 'Ã': case 'Õ': return "~";
            case 'á': case 'é': case 'í': case 'ó': case 'ú': case 'Á': case 'É': case 'Í': case 'Ó': case 'Ú': return "'";
            case 'à': case 'è': case 'ì': case 'ò': case 'ù': case 'À': case 'È': case 'Ì': case 'Ò': case 'Ù': return "'";
            case 'â': case 'ê': case 'ô': case 'Â': case 'Ê': case 'Ô': return "^";
            case 'ç': case 'Ç': return ",";
            default: return "";
        }
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void dispose() {
        if (sensor != null) sensor.stop();
        batch.dispose(); shapes.dispose(); font.dispose(); background.dispose(); life.dispose();
        for (Texture t : baker) t.dispose();
        for (Texture t : ingredientTextures.values()) t.dispose();
        for (Texture t : cakes) t.dispose();
        tutorialLivesTexture.dispose(); tutorialObjectivesTexture.dispose();
        catchSound.dispose(); missSound.dispose(); music.dispose();
    }
}