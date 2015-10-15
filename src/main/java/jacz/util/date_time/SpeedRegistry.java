package jacz.util.date_time;

import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Stores periodic speeds from a speed monitor
 */
public class SpeedRegistry implements SimpleTimerAction {

    private final SpeedMonitor speedMonitor;

    private final long monitorFrequency;

    private int capacity;

    private final Queue<Double> speedRegistry;

    private final Timer timer;

    public SpeedRegistry(long millisToStore, long timeToStore, long monitorFrequency) {
        this(new SpeedMonitor(millisToStore), timeToStore, monitorFrequency);
    }

    public SpeedRegistry(SpeedMonitor speedMonitor, long timeToStore, long monitorFrequency) {
        this.speedMonitor = speedMonitor;
        this.monitorFrequency = monitorFrequency;
        setTimeToStore(timeToStore);
        speedRegistry = new ArrayDeque<>();
        timer = new Timer(monitorFrequency, this);
    }

    public long getMonitorFrequency() {
        return monitorFrequency;
    }

    public void setTimeToStore(long timeToStore) {
        capacity = (int) (timeToStore / monitorFrequency);
    }

    public synchronized Double[] getRegistry() {
        return speedRegistry.toArray(new Double[speedRegistry.size()]);
    }

    public void addProgress(long quantity) {
        speedMonitor.addProgress(quantity);
    }

    @Override
    public synchronized Long wakeUp(Timer timer) {
        speedRegistry.add(speedMonitor.getAverageSpeed());
        while (speedRegistry.size() > capacity) {
            speedRegistry.poll();
        }
        return null;
    }

    public synchronized void stop() {
        timer.stop();
    }

    @Override
    public String toString() {
        return "SpeedRegistry{" +
                "speedRegistry=" + speedRegistry +
                '}';
    }
}
