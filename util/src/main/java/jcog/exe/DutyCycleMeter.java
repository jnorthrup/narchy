package jcog.exe;

/**
 * Measure CPU load.
 * from: jsyn (LoadAnalyzer.java) */
public class DutyCycleMeter {
    private long stopTime;
    private long startTime;
    private double averageTotalTime;
    private double averageOnTime;

    protected DutyCycleMeter() {
        stopTime = System.nanoTime();
    }

    /**
     * Call this when you stop doing something. Ideally all of the time since start() was spent on
     * doing something without interruption.
     */
    public void stop() {
        long previousStopTime = stopTime;
        stopTime = System.nanoTime();
        long onTime = stopTime - startTime;
        long totalTime = stopTime - previousStopTime;
        if (totalTime > 0L) {

            double rate = 0.01;
            averageOnTime = (averageOnTime * (1.0 - rate)) + ((double) onTime * rate);
            averageTotalTime = (averageTotalTime * (1.0 - rate)) + ((double) totalTime * rate);
        }
    }

    /** Call this when you start doing something. */
    public void start() {
        startTime = System.nanoTime();
    }

    /** Calculate, on average, how much of the time was spent doing something. */
    public double getAverageLoad() {
        if (averageTotalTime > 0.0) {
            return averageOnTime / averageTotalTime;
        } else {
            return 0.0;
        }
    }
}
