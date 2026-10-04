package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Asteroid extends GameObject {

    protected float speed;
    protected float rotationSpeed;
    protected Vector2 direction;

    public Asteroid(
        Sprite sprite,
        float speed,
        float rotationSpeed
    ) {
        super(sprite);

        this.speed = speed;
        this.rotationSpeed = rotationSpeed;
        this.sprite.setOriginCenter();

        direction = new Vector2(0, -1);
    }

    @Override
    public void update(float delta) {

        sprite.translate(
            direction.x * speed * delta,
            direction.y * speed * delta
        );

        sprite.rotate(rotationSpeed * delta);

        updateCollisionBox();
    }

    public float getSpeed() {
        return speed;
    }
}
