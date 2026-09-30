package games.project.desability_geometry_dash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {

    public Texture geometryMainCharacter;
    public SpriteBatch spriteBatch;
    private UltrassonicSensorReader arduino;

    public FirstScreen(UltrassonicSensorReader arduino) {
        this.arduino = arduino;
    }

    @Override
    public void show() {
        // Prepare your screen here.
        spriteBatch = new SpriteBatch();
        geometryMainCharacter = new Texture(Utils.getInternalPath("player.jfif"));
    }

    @Override
    public void render(float delta) {
        int distancia = arduino.getDistancia();

        int y = (int)(Gdx.graphics.getHeight() * ((float)distancia / 100));
        if(y > Gdx.graphics.getHeight() - 100) {
            y = Gdx.graphics.getHeight() - 100;
        }
        // Draw your screen here. "delta" is the time since last render in seconds.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        spriteBatch.begin();
        spriteBatch.draw(geometryMainCharacter, 0, y, 100, 100);
        spriteBatch.end();




    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        spriteBatch.getProjectionMatrix().setToOrtho2D(0,0,width, height);

        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        geometryMainCharacter.dispose();
        spriteBatch.dispose();
        arduino.stop();

    }
}
