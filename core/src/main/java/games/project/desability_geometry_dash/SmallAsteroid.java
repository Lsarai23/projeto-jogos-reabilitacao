package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class SmallAsteroid extends Asteroid {

    public SmallAsteroid(Sprite sprite) {

        super(
            sprite,
            4.0f,   // velocidade
            180f    // rotação
        );
    }

    @Override
    public void update(float delta) {

        // Asteroide pequeno é mais rápido
        speed = 4.0f;

        // Gira rapidamente
        rotationSpeed = 180f;

        super.update(delta);
    }
}
