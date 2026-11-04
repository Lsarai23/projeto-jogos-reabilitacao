package games.project.desability_geometry_dash;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Lê distâncias da porta serial em segundo plano e mantém a leitura mais recente. */
public class UltrassonicSensorReader implements Runnable {
    /** Distâncias acima desse limite são descartadas antes de atualizar a leitura válida. */
    public static final int MAX_DISTANCE_CM = 50;

    private final SerialPort serialPort;
    // volatile permite que o jogo leia os valores atualizados pela thread serial.
    private volatile Reading latestReading = new Reading(0, 0);
    private volatile long latestOutOfRangeTimestampNanos;
    private volatile boolean running;

    /** Configura a porta serial e a velocidade de comunicação com o sensor. */
    public UltrassonicSensorReader(String port) {
        serialPort = SerialPort.getCommPort(port);
        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 3000, 0);
    }

    /** Abre a porta e inicia a thread que recebe as leituras do sensor. */
    public void start() {
        if (!serialPort.openPort()) throw new RuntimeException("Não foi possível abrir a porta serial.");
        running = true;
        Thread thread = new Thread(this, "ultrasonic-sensor-reader");
        thread.setDaemon(true); thread.start();
    }

    /** Lê linhas da porta serial e publica cada distância válida com seu horário. */
    @Override public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()))) {
            while (running) {
                String line = reader.readLine();
                if (line == null) continue;
                try {
                    int distanceCm = Integer.parseInt(line.trim());
                    long timestampNanos = System.nanoTime();
                    if (distanceCm > MAX_DISTANCE_CM) {
                        // Registra o evento, mas não expõe a distância inválida como amostra.
                        latestOutOfRangeTimestampNanos = timestampNanos;
                    } else {
                        latestReading = new Reading(distanceCm, timestampNanos);
                    }
                }
                catch (NumberFormatException e) { System.err.println("Leitura inválida do sensor: " + line); }
            }
        } catch (Exception e) { if (running) e.printStackTrace(); }
    }

    /** Retorna a distância, em centímetros, da leitura mais recente. */
    public int getDistance() { return latestReading.distanceCm; }

    /** Retorna a distância e o horário da última amostra recebida. */
    public Reading getLatestReading() { return latestReading; }

    /** Retorna o horário da última leitura acima do limite aceito. */
    public long getLatestOutOfRangeTimestampNanos() { return latestOutOfRangeTimestampNanos; }

    /**
     * Informa se chegou uma distância positiva de até 50 cm depois do evento indicado
     * e depois da leitura mais recente fora do limite.
     */
    public boolean hasInRangeReadingAfter(long timestampNanos) {
        Reading reading = latestReading;
        long latestInvalidTimestamp = latestOutOfRangeTimestampNanos;
        return reading.distanceCm > 0
            && reading.distanceCm <= MAX_DISTANCE_CM
            && reading.timestampNanos > timestampNanos
            && reading.timestampNanos > latestInvalidTimestamp;
    }

    /** Amostra imutável de distância e instante de recebimento em nanossegundos. */
    public static final class Reading {
        private final int distanceCm;
        private final long timestampNanos;

        private Reading(int distanceCm, long timestampNanos) {
            this.distanceCm = distanceCm;
            this.timestampNanos = timestampNanos;
        }

        /** Retorna a distância medida em centímetros. */
        public int getDistanceCm() { return distanceCm; }

        /** Retorna o instante da amostra no relógio monotônico do sistema. */
        public long getTimestampNanos() { return timestampNanos; }
    }

    /** Interrompe a leitura e fecha a porta serial se ela estiver aberta. */
    public void stop() { running = false; if (serialPort.isOpen()) serialPort.closePort(); }
}
