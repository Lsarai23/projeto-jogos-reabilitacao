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

/**
 * Coordena os estados, as regras e o ciclo principal do jogo Baker's Rush.
 * A apresentação, as regras e as métricas ficam em classes auxiliares.
 */
public class GameController implements ApplicationListener {
    private enum State { TITLE, TUTORIAL, PREPARE, PERFECT, CALIBRATE, COUNTDOWN, PLAY, PAUSED, SENSOR_WARNING, SENSOR_COUNTDOWN, RESULT, GAME_OVER, WIN, SHOW_RESULTS }
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
    private long lastHandledOutOfRangeTimestampNanos;
    private Sound catchSound, missSound;
    private Music music;
    private State state = State.TITLE;
    private State pausedState = State.PLAY;
    private boolean spaceDown, spaceHoldTriggered;
    private float spaceHoldTime;
    private float timer, spawnTimer, maxDistance, amplitude;
    private boolean calibrationRangeWarning;
    private final SensorMotionMetrics motionMetrics = new SensorMotionMetrics();
    private int phase = 1, lives = 3, score, hiScore, phaseStartScore;
    private EnumMap<FallingIngredient.Type, Integer> required, collected;

    /** Inicializa recursos, estado inicial, sensor e controles de teclado. */
    @Override public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        world = new FitViewport(16, 9);
        ui = new FitViewport(UW, UH);
        background = new Texture("background.jpg");
        background.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font = new BitmapFont(Gdx.files.internal("default.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        life = TextureFactory.keyed("life.png");
        baker = new Texture[] {TextureFactory.keyed("baker.png"), TextureFactory.keyed("baker_pos2.png")};
        bakerAnimation = new Animation<>(0.24f, new TextureRegion(baker[0]), new TextureRegion(baker[1]));
        bakerAnimation.setPlayMode(Animation.PlayMode.LOOP);
        chef = new Chef(new Sprite(baker[0]), world, bakerAnimation);
        chef.setSize(2.35f, 1.65f);
        chef.setPosition((world.getWorldWidth() - chef.getWidth()) / 2f, 0.2f);
        ingredientTextures = new EnumMap<>(FallingIngredient.Type.class);
        for (FallingIngredient.Type t : FallingIngredient.Type.values())
            ingredientTextures.put(t, TextureFactory.keyed(t.textureFile));
        cakes = new Texture[] {TextureFactory.keyed("cake-1.png"), TextureFactory.keyed("cake-2.png"), TextureFactory.keyed("cake-3.png")};

        tutorialLivesTexture = new Texture("tutorial-lifes.png");
        tutorialObjectivesTexture = new Texture("tutorial-obj-score.png");

        tutorialLivesTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tutorialObjectivesTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        required = GameRules.requirements(1);
        collected = new EnumMap<>(FallingIngredient.Type.class);
        motionMetrics.reset();
        hiScore = Gdx.app.getPreferences("carrot-cake-game").getInteger("hiScore", 0);
        catchSound = Gdx.audio.newSound(Gdx.files.internal("catch.mp3"));
        missSound = Gdx.audio.newSound(Gdx.files.internal("miss.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
        music.setLooping(true);
        music.setVolume(0.35f);
        music.play();
        connectSensor();

        // Distingue o toque curto, a pausa e o tutorial acionado por pressionamento longo.
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
    /** Tenta abrir o sensor e atualiza a mensagem exibida na tela inicial. */
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

    /** Prepara a fase de aproximação da mão antes da calibração. */
    private void beginPreparation() { state = State.PREPARE; timer = 0; }
    @Override public void resize(int w, int h) { world.update(w, h, true); ui.update(w, h, true); }
    @Override public void render() { update(Math.min(Gdx.graphics.getDeltaTime(), .05f)); draw(); }

    /** Avança temporizadores e transições sem depender da taxa de quadros. */
    private void update(float dt) {
        long outOfRangeTimestamp = sensor == null
            ? 0L : sensor.getLatestOutOfRangeTimestampNanos();
        if (outOfRangeTimestamp > lastHandledOutOfRangeTimestampNanos
            && state != State.PAUSED) {
            long previousOutOfRangeTimestamp = lastHandledOutOfRangeTimestampNanos;
            lastHandledOutOfRangeTimestampNanos = outOfRangeTimestamp;
            if (state == State.CALIBRATE) {
                UltrassonicSensorReader.Reading latestReading = sensor.getLatestReading();
                boolean recoveredSincePreviousWarning =
                    latestReading.getDistanceCm() > 0
                        && latestReading.getDistanceCm() <= UltrassonicSensorReader.MAX_DISTANCE_CM
                        && latestReading.getTimestampNanos() > previousOutOfRangeTimestamp;
                if (!calibrationRangeWarning || recoveredSincePreviousWarning) timer = 5f;
                calibrationRangeWarning = true;
            } else if (state == State.PLAY || state == State.COUNTDOWN) {
                pausedState = state;
                state = State.SENSOR_WARNING;
                music.pause();
                // Fecha o segmento antes da pausa para não contar o intervalo sem movimento.
                motionMetrics.finishCurrentSegment();
                motionMetrics.resetSensorBaseline();
            } else if (state == State.SENSOR_COUNTDOWN) {
                state = State.SENSOR_WARNING;
            }
        }

        if ((state == State.TITLE || state == State.WIN) && spaceDown && !spaceHoldTriggered) {
            spaceHoldTime += dt;
            if (spaceHoldTime >= SPACE_HOLD_SECONDS) {
                spaceHoldTriggered = true;
                if (state == State.TITLE) state = State.TUTORIAL;
                else restartWholeGame();
            }
        }
        // Cada estado controla sua própria espera ou transição de fase.
        switch (state) {
            case PREPARE:
                if (sensorConnected && sensor.getDistance() > 0 && sensor.getDistance() <= MIN_DISTANCE) {
                    state = State.PERFECT; timer = 2f;
                }
                break;
            case PERFECT:
                if ((timer -= dt) <= 0) {
                    state = State.CALIBRATE;
                    timer = 5f;
                    long latestOutOfRange = sensor.getLatestOutOfRangeTimestampNanos();
                    if (latestOutOfRange > 0L
                        && !sensor.hasInRangeReadingAfter(latestOutOfRange)) {
                        calibrationRangeWarning = true;
                    }
                }
                break;
            case CALIBRATE:
                if ((timer -= dt) <= 0) {
                    if (calibrationRangeWarning
                        && !sensor.hasInRangeReadingAfter(lastHandledOutOfRangeTimestampNanos)) {
                        timer = 5f;
                    } else {
                        calibrationRangeWarning = false;
                        maxDistance = sensor.getDistance();
                        if (maxDistance <= MIN_DISTANCE) timer = 5f;
                        else {
                            amplitude = maxDistance - MIN_DISTANCE;
                            chef.configureCalibration(MIN_DISTANCE, maxDistance, amplitude);
                            startPhase(1);
                        }
                    }
                }
                break;
            case COUNTDOWN:
                motionMetrics.recordLatest(sensor, sensorConnected, phase);
                chef.update(dt);
                if ((timer -= dt) <= 0) state = State.PLAY;
                break;
            case PLAY: updateGame(dt); break;
            case SENSOR_WARNING:
                if (sensor != null
                    && sensor.hasInRangeReadingAfter(lastHandledOutOfRangeTimestampNanos)) {
                    state = State.SENSOR_COUNTDOWN;
                    timer = 3f;
                }
                break;
            case SENSOR_COUNTDOWN:
                if ((timer -= dt) <= 0f) {
                    state = pausedState;
                    music.play();
                }
                break;
            case RESULT:
                if ((timer -= dt) <= 0) { if (phase < 3) startPhase(phase + 1); else state = State.WIN; }
                break;
            default: break;
        }
    }

    /** Atualiza sensor, chef e ingredientes enquanto a partida está ativa. */
    private void updateGame(float dt) {
        motionMetrics.recordLatest(sensor, sensorConnected, phase);
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

    /** Escolhe um ingrediente e o posiciona no topo para iniciar sua queda. */
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

    /** Aplica pontos ou perda de vida conforme o objetivo atual da fase. */
    private void handleCatch(FallingIngredient.Type type) {
        int count = collected.getOrDefault(type, 0), target = required.getOrDefault(type, 0);
        if (target > count) {
            collected.put(type, count + 1);
            score += GameRules.pointsForLives(lives);
            if (score > hiScore) {
                hiScore = score;
                Gdx.app.getPreferences("carrot-cake-game").putInteger("hiScore", hiScore).flush();
            }
            catchSound.play();
            if (GameRules.isPhaseComplete(required, collected)) finishPhase();
        } else {
            lives--; missSound.play();
            if (lives <= 0) {
                lives = 0;
                score = phaseStartScore;
                motionMetrics.finishCurrentSegment();
                motionMetrics.resetSensorBaseline();
                state = State.GAME_OVER;
                music.stop();
            }
        }
    }

    /** Zera os objetivos da fase e inicia sua contagem regressiva. */
    private void startPhase(int n) {
        phase = n;
        calibrationRangeWarning = false;
        motionMetrics.beginPhase(n);
        phaseStartScore = score;
        required = GameRules.requirements(n);
        collected.clear();
        ingredients.clear();
        spawnTimer = 0f;
        state = State.COUNTDOWN;
        timer = 3f;
    }

    /** Fecha a medição e encaminha o jogo para o resultado ou para a vitória. */
    private void finishPhase() {
        motionMetrics.finishPhaseMeasurement();
        ingredients.clear();
        if (phase == 3) state = State.WIN;
        else { state = State.RESULT; timer = 3f; }
    }
    /** Reinicia a fase atual sem manter os pontos conquistados nela. */
    private void restartPhase() { score = phaseStartScore; lives = 3; startPhase(phase); music.play(); }

    /** Limpa as métricas e volta ao início de uma partida completa. */
    private void restartWholeGame() {
        score = 0;
        lives = 3;
        phase = 1;
        phaseStartScore = 0;
        maxDistance = 0f;
        amplitude = 0f;
        calibrationRangeWarning = false;
        motionMetrics.reset();
        required = GameRules.requirements(1);
        collected.clear();
        ingredients.clear();
        chef.setPosition((world.getWorldWidth() - chef.getWidth()) / 2f, 0.2f);
        beginPreparation();
    }

    /** Indica estados em que o cenário da partida não deve ser desenhado. */
    private boolean isSetupScreen() {
        return state == State.TITLE || state == State.TUTORIAL || state == State.PREPARE || state == State.PERFECT || state == State.CALIBRATE;
    }

    /** Desenha o cenário e a interface adequada ao estado atual. */
    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        world.apply(); batch.setProjectionMatrix(world.getCamera().combined); batch.begin();
        if (!isSetupScreen()) batch.draw(background, 0, 0, world.getWorldWidth(), world.getWorldHeight());
        if (!isSetupScreen() && state != State.RESULT && state != State.WIN && state != State.SHOW_RESULTS) {
            chef.draw(batch);
            if (state == State.PLAY || state == State.PAUSED || state == State.GAME_OVER
                || state == State.SENSOR_WARNING || state == State.SENSOR_COUNTDOWN)
                for (FallingIngredient item : ingredients) item.draw(batch);
        }
        batch.end();
        if (state == State.RESULT || state == State.WIN || state == State.GAME_OVER
            || state == State.PAUSED || state == State.SENSOR_WARNING
            || state == State.SENSOR_COUNTDOWN || state == State.SHOW_RESULTS) darken();
        ui.apply(); batch.setProjectionMatrix(ui.getCamera().combined);

        if (state == State.TUTORIAL) drawTutorialFrames();
        if (state == State.SHOW_RESULTS) drawResultsFrames();
        batch.begin();
        if (state == State.RESULT || state == State.WIN) drawCake();
        if (state == State.COUNTDOWN || state == State.PLAY || state == State.PAUSED
            || state == State.GAME_OVER || state == State.SENSOR_WARNING
            || state == State.SENSOR_COUNTDOWN) drawHud();
        if (state == State.TUTORIAL) drawTutorialImages();
        drawMessage(); batch.end();
    }

    /** Escurece a cena atrás de telas sobrepostas, como pausa e resultados. */
    private void darken() {
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(ui.getCamera().combined); shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, state == State.GAME_OVER ? .68f : .76f);
        shapes.rect(0, 0, UW, UH); shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    /** Desenha os contornos que organizam as áreas do tutorial. */
    private void drawTutorialFrames() {
        shapes.setProjectionMatrix(ui.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GOLD);
        shapes.rect(36f, 377f, 1208f, 216f);
        shapes.rect(36f, 207f, 1208f, 160f);
        shapes.rect(36f, 70f, 1208f, 127f);
        shapes.end();
    }

    /** Desenha os contornos que organizam as áreas da tela de resultados. */
    private void drawResultsFrames() {
        shapes.setProjectionMatrix(ui.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.GOLD);
        shapes.rect(36f, 490f, 1208f, 103f);
        shapes.rect(36f, 260f, 1208f, 220f);
        shapes.rect(36f, 100f, 1208f, 150f);
        shapes.end();
    }
    /** Exibe ilustrações do jogo e dos objetivos no tutorial. */
    private void drawTutorialImages() {
        batch.draw(tutorialObjectivesTexture, 62f, 384f, 110f, 200.66f);

        batch.draw(tutorialLivesTexture, 62f, 120f, 230f, 56.83f);
        batch.draw(baker[0], 950f, 212f, 270f, 151f);
        batch.draw(cakes[2], 1010f, 429f, 200f, 111.64f);
    }
    /** Centraliza a imagem do bolo correspondente à fase concluída. */
    private void drawCake() {
        Texture cake = cakes[phase - 1]; float w = 450, h = w * cake.getHeight() / cake.getWidth();
        batch.draw(cake, (UW - w) / 2, (UH - h) / 2 - 20, w, h);
    }
    /** Desenha pontuação, vidas e ingredientes que ainda faltam coletar. */
    private void drawHud() {
        TextWriter.draw(batch, font, "SCORE " + String.format(Locale.ROOT, "%06d", score), 36, 688, .82f, Color.WHITE);
        TextWriter.draw(batch, font, "HI-SCORE " + String.format(Locale.ROOT, "%06d", hiScore), 480, 688, .72f, Color.WHITE);
        TextWriter.draw(batch, font, "FASE " + phase, 36, 620, .72f, Color.GOLD);
        for (int i = 0; i < lives; i++) batch.draw(life, 1125 + i * 48, 650, 42, 42);
        TextWriter.draw(batch, font, "Objetivos:", 42, 558, .74f, Color.GOLD);
        float y = 500f;
        for (FallingIngredient.Type t : FallingIngredient.Type.values()) {
            int target = required.getOrDefault(t, 0);
            int remaining = target - collected.getOrDefault(t, 0);
            if (remaining <= 0) continue;

            Texture icon = ingredientTextures.get(t);
            float iconHeight = .95f * UW / world.getWorldWidth();
            float iconWidth = iconHeight * icon.getWidth() / (float) icon.getHeight();
            batch.draw(icon, 42f, y - iconHeight / 2f - 8f, iconWidth, iconHeight);
            TextWriter.draw(batch, font, remaining + "x", 52f + iconWidth, y, .55f, Color.WHITE);
            y -= 78f;
        }
    }

    /** Desenha os textos e instruções próprios do estado atual. */
    private void drawMessage() {
        switch (state) {
            case TITLE:
                TextWriter.centered(batch, font, "Baker's Rush: Recipe Catch", UW, 580, 1.0f, Color.GOLD);
                TextWriter.centered(batch, font, "Pressione e solte ESPA\u00c7O para iniciar.", UW, 390f, .58f, Color.WHITE);
                TextWriter.centered(batch, font, "Segure ESPA\u00c7O para abrir o tutorial.", UW, 350f, .58f, Color.WHITE);
                TextWriter.draw(batch, font, sensorMessage, 32f, 28f, .58f, sensorConnected ? Color.GREEN : Color.RED);
                break;
            case TUTORIAL:
                TextWriter.centered(batch, font, "COMO JOGAR", UW, 675f, 1.0f, Color.GOLD);

                TextWriter.draw(batch, font, "OBJETIVOS E PONTOS", 205f, 560f, .78f, Color.GOLD);
                TextWriter.draw(batch, font, "Colete os itens pedidos.", 205f, 525f, .70f, Color.WHITE);
                TextWriter.draw(batch, font, "3 vidas: +5 pontos.", 205f, 490f, .70f, Color.WHITE);
                TextWriter.draw(batch, font, "2 vidas: +3 pontos.", 205f, 455f, .70f, Color.WHITE);
                TextWriter.draw(batch, font, "1 vida: +1 ponto.", 205f, 420f, .70f, Color.WHITE);


                TextWriter.draw(batch, font, "MOVIMENTO", 62f, 342f, .90f, Color.GOLD);
                TextWriter.draw(batch, font, "Aproxime: esquerda.", 62f, 309f, .82f, Color.WHITE);
                TextWriter.draw(batch, font, "Afaste: direita.", 62f, 278f, .82f, Color.WHITE);
                TextWriter.draw(batch, font, "VIDAS E CONTROLES", 300f, 184f, .76f, Color.GOLD);
                TextWriter.draw(batch, font, "Item errado ou extra: perde 1 vida.", 300f, 158f, .66f, Color.WHITE);
                TextWriter.draw(batch, font, "3 perdas: Game Over. ESPA\u00c7O reinicia a fase.", 300f, 132f, .66f, Color.WHITE);
                TextWriter.draw(batch, font, "ESPA\u00c7O pausa e retoma durante a fase.", 300f, 106f, .66f, Color.WHITE);
                TextWriter.centered(batch, font, "Solte ESPA\u00c7O para sair do tutorial.", UW, 38f, .58f, Color.WHITE);
                break;
            case PREPARE:
                TextWriter.centered(batch, font, "Posicione uma mão na frente do sensor.", UW, 395, .72f, Color.WHITE);
                if (sensorConnected && (sensor.getDistance() <= 0 || sensor.getDistance() > MIN_DISTANCE))
                    TextWriter.centered(batch, font, "Aproxime mais", UW, 335, .66f, Color.RED);
                break;
            case PERFECT:
                TextWriter.centered(batch, font, "Perfeito!", UW, 385, 1.1f, Color.GREEN);
                TextWriter.centered(batch, font, "Mantenha a mão nessa posição.", UW, 325, .58f, Color.WHITE);
                break;
            case CALIBRATE:
                TextWriter.centered(batch, font, "Afaste a mão horizontalmente o máximo que puder.", UW, 390, .65f, Color.WHITE);
                TextWriter.centered(batch, font, "Calibração: " + Math.max(0, (int)Math.ceil(timer)), UW, 330, .75f, Color.GOLD);
                if (calibrationRangeWarning)
                    TextWriter.centered(batch, font, "Não remova o braço da linha de visão do sensor.", UW, 275, .52f, Color.RED);
                else if (maxDistance > 0 && maxDistance <= MIN_DISTANCE)
                    TextWriter.centered(batch, font, "Afaste mais a mão para calibrar.", UW, 275, .5f, Color.RED);
                break;
            case COUNTDOWN:
                TextWriter.centered(batch, font, "FASE " + phase, UW, 410, 1.15f, Color.GOLD);
                TextWriter.centered(batch, font, "Começa em " + Math.max(1, (int)Math.ceil(timer)), UW, 340, .75f, Color.WHITE);
                break;
            case RESULT:
                TextWriter.centered(batch, font, RESULTS[phase - 1], UW, 620, .62f, Color.WHITE);
                break;
            case GAME_OVER:
                TextWriter.centered(batch, font, "GAME OVER", UW, 405, 1.35f, Color.RED);
                TextWriter.centered(batch, font, "Pressione ESPA\u00c7O para repetir a fase " + phase, UW, 330, .66f, Color.WHITE);
                break;
            case PAUSED:
                TextWriter.centered(batch, font, "PAUSADO", UW, 405, 1.2f, Color.GOLD);
                TextWriter.centered(batch, font, "Pressione ESPAÇO para continuar.", UW, 330, .65f, Color.WHITE);
                break;
            case SENSOR_WARNING:
                TextWriter.centered(batch, font, "Não remova o braço da linha de visão do sensor.", UW, 405, .66f, Color.RED);
                TextWriter.centered(batch, font, "Aguardando uma leitura válida.", UW, 350, .58f, Color.WHITE);
                break;
            case SENSOR_COUNTDOWN:
                TextWriter.centered(batch, font, "Mantenha o braço na linha de visão do sensor.", UW, 405, .66f, Color.GOLD);
                TextWriter.centered(batch, font, "A fase continua em " + Math.max(1, (int)Math.ceil(timer)), UW, 350, .72f, Color.WHITE);
                break;
            case WIN:
                TextWriter.centered(batch, font, "Perfeito!", UW, 620f, .92f, Color.GREEN);
                TextWriter.centered(batch, font, "Segure ESPA\u00c7O para reiniciar.", UW, 190f, .62f, Color.WHITE);
                TextWriter.centered(batch, font, "Toque e solte ESPA\u00c7O para ver resultados.", UW, 150f, .62f, Color.WHITE);
                break;
            case SHOW_RESULTS:
                TextWriter.centered(batch, font, "RESULTADOS", UW, 675f, 1.0f, Color.GOLD);
                TextWriter.draw(batch, font, "SCORE", 120f, 573f, .78f, Color.GOLD);
                TextWriter.draw(batch, font, String.format(Locale.ROOT, "%06d", score), 120f, 536f, .96f, Color.WHITE);
                TextWriter.draw(batch, font, "HI-SCORE", 700f, 573f, .78f, Color.GOLD);
                TextWriter.draw(batch, font, String.format(Locale.ROOT, "%06d", hiScore), 700f, 536f, .96f, Color.WHITE);

                TextWriter.draw(batch, font, "AMPLITUDE DE MOVIMENTO REAL (ROM)", 62f, 450f, .72f, Color.GOLD);
                TextWriter.draw(batch, font, motionMetrics.phaseRangeText(0), 62f, 412f, .72f, Color.WHITE);
                TextWriter.draw(batch, font, motionMetrics.phaseRangeText(1), 62f, 376f, .72f, Color.WHITE);
                TextWriter.draw(batch, font, motionMetrics.phaseRangeText(2), 62f, 340f, .72f, Color.WHITE);

                TextWriter.draw(batch, font, "PICO E M\u00c9DIA DE VELOCIDADE (SPEED)", 62f, 220f, .72f, Color.GOLD);
                TextWriter.draw(batch, font, "Pico: " + motionMetrics.velocityText(motionMetrics.getPeakVelocityCmPerSecond()), 62f, 182f, .72f, Color.WHITE);
                TextWriter.draw(batch, font, "M\u00e9dia global: " + motionMetrics.velocityText(motionMetrics.averageVelocityCmPerSecond()), 62f, 145f, .72f, Color.WHITE);
                TextWriter.centered(batch, font, "ESPA\u00c7O: voltar \u00e0 tela final.", UW, 40f, .55f, Color.WHITE);
                break;
            default:
                break;
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
