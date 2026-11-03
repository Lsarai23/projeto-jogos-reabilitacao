package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Chef extends GameObject {
    private final FitViewport viewport;
    private final Animation<TextureRegion> animation;
    private final Rectangle basket = new Rectangle();
    private final TextureRegion drawFrame = new TextureRegion();
    private boolean facingLeft;
    private UltrassonicSensorReader sensor;
    private float minDistance = 15f, amplitude = 35f, response = 8f, animationTime;
    public Chef(Sprite sprite, FitViewport viewport, Animation<TextureRegion> animation) {
        super(sprite); this.viewport = viewport; this.animation = animation;
    }
    public void setSensorReader(UltrassonicSensorReader sensor) { this.sensor = sensor; }
    public void setSize(float width, float height) { sprite.setSize(width, height); updateCollisionBox(); }
    public void configureCalibration(float min, float max, float amplitude) {
        minDistance = min; this.amplitude = Math.max(1f, max - min);
        response = MathUtils.clamp(5f + this.amplitude * .12f, 5f, 12f);
    }
    @Override public void update(float dt) {
        if (sensor != null && sensor.getDistance() > 0) {
            float normalized = MathUtils.clamp((sensor.getDistance() - minDistance) / amplitude, 0f, 1f);
            float targetX = normalized * (viewport.getWorldWidth() - sprite.getWidth());
            float oldX = sprite.getX();
            sprite.setX(MathUtils.lerp(oldX, targetX, 1f - (float)Math.exp(-response * dt)));
            if (sprite.getX() < oldX - .001f) facingLeft = true;
            else if (sprite.getX() > oldX + .001f) facingLeft = false;
            animationTime = Math.abs(targetX - sprite.getX()) > .01f ? animationTime + dt : 0f;
        }
        sprite.setX(MathUtils.clamp(sprite.getX(), 0f, viewport.getWorldWidth() - sprite.getWidth()));
        updateCollisionBox();
    }
    public Rectangle getBasketBounds() {
        float basketLeft = sprite.getX() + sprite.getWidth() * (facingLeft ? .03f : .54f);
        return basket.set(basketLeft, sprite.getY() + sprite.getHeight() * .24f,
            sprite.getWidth() * .43f, sprite.getHeight() * .31f);
    }
    @Override public void draw(SpriteBatch batch) {
        drawFrame.setRegion(animation.getKeyFrame(animationTime));
        if (drawFrame.isFlipX() != facingLeft) drawFrame.flip(true, false);
        batch.draw(drawFrame, sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight());
    }
}