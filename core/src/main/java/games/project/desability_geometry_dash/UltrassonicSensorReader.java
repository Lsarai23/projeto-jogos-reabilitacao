package games.project.desability_geometry_dash;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UltrassonicSensorReader implements Runnable {
    private final SerialPort serialPort;
    private volatile Reading latestReading = new Reading(0, 0);
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
                try { latestReading = new Reading(Integer.parseInt(line.trim()), System.nanoTime()); }
                catch (NumberFormatException e) { System.err.println("Leitura inválida do sensor: " + line); }
            }
        } catch (Exception e) { if (running) e.printStackTrace(); }
    }
    public int getDistance() { return latestReading.distanceCm; }
    public Reading getLatestReading() { return latestReading; }

    public static final class Reading {
        private final int distanceCm;
        private final long timestampNanos;

        private Reading(int distanceCm, long timestampNanos) {
            this.distanceCm = distanceCm;
            this.timestampNanos = timestampNanos;
        }

        public int getDistanceCm() { return distanceCm; }
        public long getTimestampNanos() { return timestampNanos; }
    }
    public void stop() { running = false; if (serialPort.isOpen()) serialPort.closePort(); }
}