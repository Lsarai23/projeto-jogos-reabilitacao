package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class Explosion {

    public Sprite sprite;
    public float stateTime;

    public Explosion(Sprite sprite) {
        this.sprite = sprite;
        this.stateTime = 0f;
    }
}
