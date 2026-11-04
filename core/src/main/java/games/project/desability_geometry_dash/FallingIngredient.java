package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.g2d.Sprite;

/** Item que cai pela tela e pode ser coletado pelo chef. */
public class FallingIngredient extends GameObject {
    /** Tipos de ingrediente, com o arquivo de imagem e o nome exibido no jogo. */
    public enum Type {
        EGG("egg.png", "Ovo"), FLOUR("flour.png", "Farinha"),
        CARROT("carrot.png", "Cenoura"), SUGAR("sugar.png", "Açúcar"),
        YEAST("yeast.png", "Fermento"), CHOCOLATE("chocolate.png", "Chocolate");
        public final String textureFile, label;

        /** Associa o ingrediente à sua textura e ao rótulo da interface. */
        Type(String file, String label) { textureFile = file; this.label = label; }
    }
    private final Type type;
    private final float speed;

    /** Cria um ingrediente com tipo e velocidade de queda definidos. */
    public FallingIngredient(Sprite sprite, Type type, float speed) { super(sprite); this.type = type; this.speed = speed; }

    /** Move o item para baixo e atualiza sua área de colisão. */
    @Override public void update(float dt) { sprite.translateY(-speed * dt); updateCollisionBox(); }

    /** Retorna o tipo usado para conferir os objetivos da fase. */
    public Type getType() { return type; }
}
