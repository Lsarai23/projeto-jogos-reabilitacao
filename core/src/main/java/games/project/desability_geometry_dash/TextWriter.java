package games.project.desability_geometry_dash;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Métodos estáticos para desenhar texto e complementar acentos ausentes na fonte bitmap. */
public final class TextWriter {
    private TextWriter() {
        // Classe utilitária; os métodos são chamados diretamente pela classe.
    }

    /** Desenha uma linha de texto centralizada em uma largura lógica conhecida. */
    public static void centered(SpriteBatch batch, BitmapFont font, String text,
                                float canvasWidth, float y, float scale, Color color) {
        draw(batch, font, text, (canvasWidth - textWidth(font, text, scale)) / 2f,
            y, scale, color);
    }

    /** Mede o texto usando os glifos base disponíveis na fonte do jogo. */
    public static float textWidth(BitmapFont font, String text, float scale) {
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            width += font.getData().getGlyph(base(text.charAt(i))).xadvance * scale;
        }
        return width;
    }

    /** Desenha os glifos e acrescenta o sinal diacrítico quando necessário. */
    public static void draw(SpriteBatch batch, BitmapFont font, String text,
                            float x, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        float lineHeight = font.getLineHeight();
        for (int i = 0; i < text.length(); i++) {
            char original = text.charAt(i);
            char glyph = base(original);
            font.draw(batch, String.valueOf(glyph), x, y);
            String accent = mark(original);
            if (!accent.isEmpty()) {
                float accentY = original == 'ç' || original == 'Ç'
                    ? y - lineHeight * 0.12f : y + lineHeight * 0.25f;
                float accentX = x + font.getData().getGlyph(glyph).xadvance * scale * 0.08f;
                font.draw(batch, accent, accentX, accentY);
            }
            x += font.getData().getGlyph(glyph).xadvance * scale;
        }
        // Restaura o estado compartilhado da fonte antes do próximo elemento da interface.
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /** Substitui cada letra acentuada pelo glifo sem acento da fonte. */
    private static char base(char c) {
        switch (c) {
            case 'ã': case 'á': case 'à': case 'â': return 'a';
            case 'Ã': case 'Á': case 'À': case 'Â': return 'A';
            case 'é': case 'ê': case 'è': return 'e';
            case 'É': case 'Ê': case 'È': return 'E';
            case 'í': case 'ì': return 'i';
            case 'Í': case 'Ì': return 'I';
            case 'õ': case 'ó': case 'ò': case 'ô': return 'o';
            case 'Õ': case 'Ó': case 'Ò': case 'Ô': return 'O';
            case 'ú': case 'ù': return 'u';
            case 'Ú': case 'Ù': return 'U';
            case 'ç': return 'c';
            case 'Ç': return 'C';
            default: return c;
        }
    }

    /** Retorna o sinal gráfico correspondente a uma letra acentuada. */
    private static String mark(char c) {
        switch (c) {
            case 'ã': case 'õ': case 'Ã': case 'Õ': return "~";
            case 'á': case 'é': case 'í': case 'ó': case 'ú':
            case 'Á': case 'É': case 'Í': case 'Ó': case 'Ú':
            case 'à': case 'è': case 'ì': case 'ò': case 'ù':
            case 'À': case 'È': case 'Ì': case 'Ò': case 'Ù': return "'";
            case 'â': case 'ê': case 'ô': case 'Â': case 'Ê': case 'Ô': return "^";
            case 'ç': case 'Ç': return ",";
            default: return "";
        }
    }
}
