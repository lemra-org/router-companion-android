package org.rm3l.ddwrt.actions.activity;

/**
 * Created by rm3l on 10/01/16.
 */
public class PingRTT {

    private float min;
    private float max;
    private float avg;
    private float stddev;

    private float packetLoss;

    public float getMin() {
        return min;
    }

    public PingRTT setMin(float min) {
        this.min = min;
        return this;
    }

    public float getMax() {
        return max;
    }

    public PingRTT setMax(float max) {
        this.max = max;
        return this;
    }

    public float getAvg() {
        return avg;
    }

    public PingRTT setAvg(float avg) {
        this.avg = avg;
        return this;
    }

    public float getStddev() {
        return stddev;
    }

    public PingRTT setStddev(float stddev) {
        this.stddev = stddev;
        return this;
    }

    public float getPacketLoss() {
        return packetLoss;
    }

    public PingRTT setPacketLoss(float packetLoss) {
        this.packetLoss = packetLoss;
        return this;
    }
}
