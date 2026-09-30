package games.project.desability_geometry_dash;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UltrassonicSensorReader implements Runnable{

    private final SerialPort serialPort;
    private volatile int distancia;

    private boolean running;

    public UltrassonicSensorReader(String porta) {

        serialPort = SerialPort.getCommPort(porta);
        serialPort.setBaudRate(9600);

        serialPort.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            3000,
            0
        );
    }

    public void start() {

        if (!serialPort.openPort()) {
            throw new RuntimeException(
                "Não foi possível abrir a porta serial."
            );
        }

        System.out.println("Arduino conectado!");

        running = true;

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {

        try {

            BufferedReader reader =
                new BufferedReader(
                    new InputStreamReader(
                        serialPort.getInputStream()
                    )
                );

            while (running) {

                String linha = reader.readLine();

                if (linha == null) {
                    continue;
                }

                try {

                    distancia = Integer.parseInt(linha);

                } catch (NumberFormatException e) {

                    System.out.println(
                        "Valor inválido: " + linha
                    );
                }
            }

        } catch (Exception e) {

            if (running) {
                e.printStackTrace();
            }
        }
    }

    public int getDistancia() {
        return distancia;
    }

    public void stop() {

        running = false;

        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
