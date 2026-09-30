package games.project.desability_geometry_dash;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    private UltrassonicSensorReader arduino;

    @Override
    public void create() {
        arduino = new UltrassonicSensorReader("COM3");
        arduino.start();
        setScreen(new FirstScreen(arduino));
    }
}
