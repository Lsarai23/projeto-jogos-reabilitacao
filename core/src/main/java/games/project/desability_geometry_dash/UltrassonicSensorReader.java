package games.project.desability_geometry_dash;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UltrassonicSensorReader implements Runnable {
    private final SerialPort serialPort;
    private volatile int distance;
    private volatile boolean running;
    public UltrassonicSensorReader(String port) {
        serialPort = SerialPort.getCommPort(port);
        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 3000, 0);
    }
    public void start() {
        if (!serialPort.openPort()) throw new RuntimeException("Não foi possível abrir a porta serial.");
        running = true;
        Thread thread = new Thread(this, "ultrasonic-sensor-reader");
        thread.setDaemon(true); thread.start();
    }
    @Override public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()))) {
            while (running) {
                String line = reader.readLine();
                if (line == null) continue;
                try { distance = Integer.parseInt(line.trim()); }
                catch (NumberFormatException e) { System.err.println("Leitura inválida do sensor: " + line); }
            }
        } catch (Exception e) { if (running) e.printStackTrace(); }
    }
    public int getDistance() { return distance; }
    public void stop() { running = false; if (serialPort.isOpen()) serialPort.closePort(); }
}