package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class FallingIngredient extends GameObject {
    public enum Type {
        EGG("egg.png", "Ovo"), FLOUR("flour.png", "Farinha"),
        CARROT("carrot.png", "Cenoura"), SUGAR("sugar.png", "Açúcar"),
        YEAST("yeast.png", "Fermento"), CHOCOLATE("chocolate.png", "Chocolate");
        public final String textureFile, label;
        Type(String file, String label) { textureFile = file; this.label = label; }
    }
    private final Type type;
    private final float speed;
    public FallingIngredient(Sprite sprite, Type type, float speed) { super(sprite); this.type = type; this.speed = speed; }
    @Override public void update(float dt) { sprite.translateY(-speed * dt); updateCollisionBox(); }
    public Type getType() { return type; }
}