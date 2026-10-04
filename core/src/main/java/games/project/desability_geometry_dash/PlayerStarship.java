package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class PlayerStarship extends GameObject {

    // Distância mínima e máxima consideradas pelo sensor (em cm)
    // TODO: Substituir por leitura inicial feita com usuário
    private final float minDistance = 10f;
    private final float maxDistance = 50f;

    // Velocidade de resposta da nave à posição do sensor
    private final float movementResponse = 8f;

    private float speed;
    private boolean moving;

    private UltrassonicSensorReader sensorReader;

    private Animation<TextureRegion> flyingAnimation;
    private float flyingStateTime;

    private FitViewport viewport;

    public PlayerStarship(
        Sprite sprite,
        FitViewport viewport,
        Animation<TextureRegion> flyingAnimation,
        UltrassonicSensorReader sensorReader
    ) {
        super(sprite);

        this.viewport = viewport;
        this.flyingAnimation = flyingAnimation;
        this.sensorReader = sensorReader;

        flyingStateTime = 0f;
    }

    @Override
    public void update(float delta) {

        moving = false;

        // =========================
        // LEITURA DO SENSOR
        // =========================

        float distance = sensorReader.getDistance();

        // Ignora leituras inválidas
        if (distance > 0) {

            // =========================
            // NORMALIZAR DISTÂNCIA
            // =========================

            float normalizedDistance =
                (distance - minDistance)
                    / (maxDistance - minDistance);

            normalizedDistance = MathUtils.clamp(
                normalizedDistance,
                0f,
                1f
            );

            // =========================
            // CONVERTER PARA POSIÇÃO X
            // =========================

            float minX = 0f;

            float maxX =
                viewport.getWorldWidth()
                    - sprite.getWidth();

            float targetX = MathUtils.lerp(
                minX,
                maxX,
                normalizedDistance
            );

            // =========================
            // MOVIMENTO SUAVE
            // =========================

            float currentX = sprite.getX();

            float difference = targetX - currentX;

            if (Math.abs(difference) > 0.01f) {

                float previousX = currentX;

                // Movimento independente do FPS
                float interpolation =
                    1f - (float) Math.exp(
                        -movementResponse * delta
                    );

                float newX = MathUtils.lerp(
                    currentX,
                    targetX,
                    interpolation
                );

                sprite.setX(newX);

                // Velocidade aproximada da nave
                speed = (newX - previousX) / delta;

                moving = true;

            } else {

                sprite.setX(targetX);
                speed = 0f;
                moving = false;
            }
        }

        // =========================
        // LIMITAR À TELA
        // =========================

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        sprite.setX(
            MathUtils.clamp(
                sprite.getX(),
                0f,
                worldWidth - sprite.getWidth()
            )
        );

        sprite.setY(
            MathUtils.clamp(
                sprite.getY(),
                0f,
                worldHeight - sprite.getHeight()
            )
        );

        // =========================
        // ANIMAÇÃO
        // =========================

        if (moving) {
            flyingStateTime += delta;
        } else {
            flyingStateTime = 0f;
        }

        updateCollisionBox();
    }

    public TextureRegion getFlyingFrame() {

        if (flyingStateTime <= 0f) {
            return null;
        }

        return flyingAnimation.getKeyFrame(flyingStateTime);
    }

    public float getSpeed() {
        return speed;
    }

    public void increaseSpeed(float amount) {
        speed += amount;
    }
}
