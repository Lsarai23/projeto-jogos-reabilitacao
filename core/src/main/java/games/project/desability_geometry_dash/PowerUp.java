package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class PowerUp extends GameObject {

    // Em uma viewport de altura 5, 0.4f faz o item flutuar devagar
    private float speed = 0.4f;

    public PowerUp(Sprite sprite) {
        super(sprite);
        // Centraliza a origem para girar no próprio eixo sem "saltar"
        this.sprite.setOriginCenter();
    }

    @Override
    public void update(float delta) {
        // Descida lenta
        sprite.translateY(-speed * delta);

        updateCollisionBox();
    }
}
