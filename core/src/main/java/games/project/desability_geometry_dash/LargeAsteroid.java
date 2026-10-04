    package games.project.desability_geometry_dash;

    import com.badlogic.gdx.graphics.g2d.Sprite;

    public class LargeAsteroid extends Asteroid {

        public LargeAsteroid(Sprite sprite) {

            super(
                sprite,
                1.5f,
                45f
            );
        }

        @Override
        public void update(float delta) {

            // Asteroide grande é mais lento
            speed = 1.5f;

            // Rotação mais lenta
            rotationSpeed = 45f;

            super.update(delta);
        }
    }
