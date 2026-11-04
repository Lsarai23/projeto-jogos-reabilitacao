package games.project.desability_geometry_dash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/** Métodos estáticos para carregar e preparar texturas do jogo. */
public final class TextureFactory {
    private TextureFactory() {
        // Classe utilitária; os métodos são chamados diretamente pela classe.
    }

    /**
     * Remove o fundo da cor do pixel superior esquerdo, recorta a imagem ao conteúdo
     * visível e cria uma textura com filtragem pixelada.
     */
    public static Texture keyed(String file) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(file));
        Color key = new Color(), sample = new Color();
        Color.rgba8888ToColor(key, pixmap.getPixel(0, 0));
        int keyRed = Math.round(key.r * 255);
        int keyGreen = Math.round(key.g * 255);
        int keyBlue = Math.round(key.b * 255);
        int left = pixmap.getWidth(), top = pixmap.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                Color.rgba8888ToColor(sample, pixmap.getPixel(x, y));
                boolean isBackground = sample.a > 0.5f
                    && Math.abs(Math.round(sample.r * 255) - keyRed) < 14
                    && Math.abs(Math.round(sample.g * 255) - keyGreen) < 14
                    && Math.abs(Math.round(sample.b * 255) - keyBlue) < 14;
                if (isBackground) {
                    pixmap.setColor(0, 0, 0, 0);
                    pixmap.drawPixel(x, y);
                } else if (sample.a > 0.05f) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right >= left && bottom >= top) {
            Pixmap cropped = new Pixmap(right - left + 1, bottom - top + 1, pixmap.getFormat());
            cropped.setBlending(Pixmap.Blending.None);
            cropped.drawPixmap(pixmap, 0, 0, left, top, cropped.getWidth(), cropped.getHeight());
            pixmap.dispose();
            pixmap = cropped;
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }
}
