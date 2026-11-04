package games.project.desability_geometry_dash;

import com.badlogic.gdx.ApplicationListener;

/**
 * Ponto de entrada do libGDX. A lógica e os recursos da partida pertencem ao controlador.
 */
public class Main implements ApplicationListener {
    private final GameController game = new GameController();

    @Override
    public void create() {
        game.create();
    }

    @Override
    public void resize(int width, int height) {
        game.resize(width, height);
    }

    @Override
    public void render() {
        game.render();
    }

    @Override
    public void pause() {
        game.pause();
    }

    @Override
    public void resume() {
        game.resume();
    }

    @Override
    public void dispose() {
        game.dispose();
    }
}
