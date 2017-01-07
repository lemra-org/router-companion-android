package org.rm3l.router_companion.resources;

/**
 * Created by rm3l on 15/06/15.
 */
public class ProcNetDevReceive {

    /**
     * Received
     */
    private long rxBytes;
    private int rxCompressed;
    private int rxDropped;
    private int rxErrors;
    private int rxFifo;
    private int rxFrame;
    private int rxMulticast;
    private int rxPackets;

    /**
     * @param rxBytes
     * @param rxCompressed
     * @param rxDropped
     * @param rxErrors
     * @param rxFifo
     * @param rxFrame
     * @param rxMulticast
     * @param rxPackets
     */
    public ProcNetDevReceive(
            long rxBytes,
            int rxPackets,
            int rxErrors,
            int rxDropped,
            int rxFifo,
            int rxFrame,
            int rxCompressed,
            int rxMulticast) {
        this.rxBytes = rxBytes;
        this.rxCompressed = rxCompressed;
        this.rxDropped = rxDropped;
        this.rxErrors = rxErrors;
        this.rxFifo = rxFifo;
        this.rxFrame = rxFrame;
        this.rxMulticast = rxMulticast;
        this.rxPackets = rxPackets;
    }

    /**
     * @return the rx_Bytes
     */
    public long getRxBytes() {
        return rxBytes;
    }

    /**
     * @return the rx_Compressed
     */
    public int getRxCompressed() {
        return rxCompressed;
    }

    /**
     * @return the rx_Dropped
     */
    public int getRxDropped() {
        return rxDropped;
    }

    /**
     * @return the rx_Erros
     */
    public int getRxErrors() {
        return rxErrors;
    }

    /**
     * @return the rx_Fifo
     */
    public int getRxFifo() {
        return rxFifo;
    }

    /**
     * @return the rx_Frame
     */
    public int getRxFrame() {
        return rxFrame;
    }

    /**
     * @return the rx_Multicast
     */
    public int getRxMulticast() {
        return rxMulticast;
    }

    /**
     * @return the rx_Packets
     */
    public int getRxPackets() {
        return rxPackets;
    }
}
