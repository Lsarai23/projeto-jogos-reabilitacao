package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public abstract class GameObject {

    protected Sprite sprite;
    protected Rectangle collisionBox;

    public GameObject(Sprite sprite) {
        this.sprite = sprite;
        this.collisionBox = new Rectangle();
        updateCollisionBox();
    }

    public abstract void update(float delta);

    public void draw(SpriteBatch spriteBatch) {
        sprite.draw(spriteBatch);
    }

    protected void updateCollisionBox() {
        collisionBox.set(
            sprite.getX(),
            sprite.getY(),
            sprite.getWidth(),
            sprite.getHeight()
        );
    }

    public Sprite getSprite() {
        return sprite;
    }

    public Rectangle getCollisionBox() {
        return sprite.getBoundingRectangle();
    }

    public float getX() {
        return sprite.getX();
    }

    public float getY() {
        return sprite.getY();
    }

    public float getWidth() {
        return sprite.getWidth();
    }

    public float getHeight() {
        return sprite.getHeight();
    }

    public void setPosition(float x, float y) {
        sprite.setPosition(x, y);
        updateCollisionBox();
    }
}
