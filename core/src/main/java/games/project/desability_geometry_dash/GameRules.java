package games.project.desability_geometry_dash;

import java.util.EnumMap;
import java.util.Map;

/** Regras estáticas de pontuação e objetivos das fases. */
public final class GameRules {
    private GameRules() {
        // Classe utilitária; os métodos são chamados diretamente pela classe.
    }

    /** Retorna os ingredientes necessários para a fase solicitada. */
    public static EnumMap<FallingIngredient.Type, Integer> requirements(int phase) {
        EnumMap<FallingIngredient.Type, Integer> required =
            new EnumMap<>(FallingIngredient.Type.class);
        required.put(FallingIngredient.Type.EGG, 3);
        required.put(FallingIngredient.Type.FLOUR, 2);
        required.put(FallingIngredient.Type.CARROT, 3);
        required.put(FallingIngredient.Type.SUGAR, 1);
        if (phase >= 2) required.put(FallingIngredient.Type.YEAST, 1);
        if (phase >= 3) required.put(FallingIngredient.Type.CHOCOLATE, 1);
        return required;
    }

    /** Calcula os pontos recebidos por um item conforme as vidas restantes. */
    public static int pointsForLives(int lives) {
        return lives == 3 ? 5 : lives == 2 ? 3 : 1;
    }

    /** Informa se todos os ingredientes exigidos já foram coletados. */
    public static boolean isPhaseComplete(
        Map<FallingIngredient.Type, Integer> required,
        Map<FallingIngredient.Type, Integer> collected
    ) {
        for (Map.Entry<FallingIngredient.Type, Integer> objective : required.entrySet()) {
            if (collected.getOrDefault(objective.getKey(), 0) < objective.getValue()) return false;
        }
        return true;
    }
}
