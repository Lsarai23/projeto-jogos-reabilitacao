package games.project.desability_geometry_dash;

import java.util.Locale;

/** Acumula amplitude, distância percorrida e velocidades lidas pelo sensor. */
public final class SensorMotionMetrics {
    private static final float MIN_MOVEMENT_DELTA_CM = 1f;
    private final float[] phaseMinDistance = new float[3];
    private final float[] phaseMaxDistance = new float[3];
    private boolean hasPreviousSample;
    private float previousDistance, segmentDistanceCm, segmentDurationSeconds;
    private long previousTimestampNanos, phaseMeasurementStartNanos;
    private int movementDirection;
    private float totalMotionDistanceCm, totalMotionDurationSeconds, peakVelocityCmPerSecond;

    public SensorMotionMetrics() {
        reset();
    }

    /** Incorpora uma leitura válida do sensor para a fase atual. */
    public void recordLatest(UltrassonicSensorReader sensor, boolean connected, int phase) {
        if (sensor == null || !connected) return;
        UltrassonicSensorReader.Reading reading = sensor.getLatestReading();
        long timestamp = reading.getTimestampNanos();
        float distance = reading.getDistanceCm();
        if (timestamp <= 0L || timestamp < phaseMeasurementStartNanos
            || distance <= 0f || distance > UltrassonicSensorReader.MAX_DISTANCE_CM
            || timestamp == previousTimestampNanos) return;
        int phaseIndex = phase - 1;
        if (phaseIndex < 0 || phaseIndex >= phaseMinDistance.length) return;
        phaseMinDistance[phaseIndex] = Math.min(phaseMinDistance[phaseIndex], distance);
        phaseMaxDistance[phaseIndex] = Math.max(phaseMaxDistance[phaseIndex], distance);

        if (hasPreviousSample) {
            float elapsed = (timestamp - previousTimestampNanos) / 1_000_000_000f;
            float change = distance - previousDistance;
            float traveled = Math.abs(change);
            if (elapsed > 0f) {
                if (traveled >= MIN_MOVEMENT_DELTA_CM) {
                    int direction = change > 0f ? 1 : -1;
                    // Fecha o trecho quando a mão inverte a direção do movimento.
                    if (movementDirection != 0 && direction != movementDirection) finishCurrentSegment();
                    movementDirection = direction;
                    segmentDistanceCm += traveled;
                    segmentDurationSeconds += elapsed;
                    float velocity = traveled / elapsed;
                    if (Float.isFinite(velocity)) peakVelocityCmPerSecond = Math.max(peakVelocityCmPerSecond, velocity);
                } else if (movementDirection != 0) {
                    segmentDurationSeconds += elapsed;
                }
            }
        }
        previousDistance = distance;
        previousTimestampNanos = timestamp;
        hasPreviousSample = true;
    }

    /** Inicia a medição da fase e zera apenas a amplitude correspondente. */
    public void beginPhase(int phase) {
        finishCurrentSegment();
        int phaseIndex = phase - 1;
        if (phaseIndex >= 0 && phaseIndex < phaseMinDistance.length) {
            phaseMinDistance[phaseIndex] = Float.POSITIVE_INFINITY;
            phaseMaxDistance[phaseIndex] = Float.NEGATIVE_INFINITY;
        }
        phaseMeasurementStartNanos = System.nanoTime();
        resetSensorBaseline();
    }

    /** Fecha o trecho em andamento quando uma fase termina. */
    public void finishPhaseMeasurement() {
        finishCurrentSegment();
        resetSensorBaseline();
    }

    /** Incorpora distância e duração do trecho atual às métricas globais. */
    public void finishCurrentSegment() {
        if (segmentDistanceCm > 0f && segmentDurationSeconds > 0f) {
            totalMotionDistanceCm += segmentDistanceCm;
            totalMotionDurationSeconds += segmentDurationSeconds;
            peakVelocityCmPerSecond = Math.max(peakVelocityCmPerSecond,
                segmentDistanceCm / segmentDurationSeconds);
        }
        segmentDistanceCm = 0f;
        segmentDurationSeconds = 0f;
        movementDirection = 0;
    }

    /** Descarta a leitura anterior para não medir movimento entre fases ou pausas. */
    public void resetSensorBaseline() {
        hasPreviousSample = false;
        previousDistance = 0f;
        previousTimestampNanos = 0L;
        segmentDistanceCm = 0f;
        segmentDurationSeconds = 0f;
        movementDirection = 0;
    }

    /** Limpa todas as métricas ao iniciar uma nova partida completa. */
    public void reset() {
        for (int i = 0; i < phaseMinDistance.length; i++) {
            phaseMinDistance[i] = Float.POSITIVE_INFINITY;
            phaseMaxDistance[i] = Float.NEGATIVE_INFINITY;
        }
        totalMotionDistanceCm = 0f;
        totalMotionDurationSeconds = 0f;
        phaseMeasurementStartNanos = 0L;
        peakVelocityCmPerSecond = 0f;
        resetSensorBaseline();
    }

    /** Retorna a velocidade média acumulada durante os trechos medidos. */
    public float averageVelocityCmPerSecond() {
        return totalMotionDurationSeconds > 0f
            ? totalMotionDistanceCm / totalMotionDurationSeconds : 0f;
    }

    /** Retorna a maior velocidade observada desde o início da partida. */
    public float getPeakVelocityCmPerSecond() {
        return peakVelocityCmPerSecond;
    }

    /** Formata a amplitude registrada para uma fase. */
    public String phaseRangeText(int phaseIndex) {
        float minimum = phaseMinDistance[phaseIndex];
        float maximum = phaseMaxDistance[phaseIndex];
        if (!Float.isFinite(minimum) || !Float.isFinite(maximum) || maximum < minimum)
            return String.format(Locale.ROOT, "Fase %d: sem leituras", phaseIndex + 1);
        return String.format(Locale.ROOT, "Fase %d: min %.1f cm | max %.1f cm | ROM %.1f cm",
            phaseIndex + 1, minimum, maximum, maximum - minimum);
    }

    /** Formata uma velocidade ou indica que ainda não há movimento medido. */
    public String velocityText(float velocity) {
        return velocity > 0f && Float.isFinite(velocity)
            ? String.format(Locale.ROOT, "%.1f cm/s", velocity) : "-- cm/s";
    }
}
