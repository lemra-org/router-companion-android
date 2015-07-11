package org.rm3l.ddwrt.resources;

/**
 * Created by rm3l on 15/06/15.
 */
public class ProcNetDevTransmit {

    /**
     * Transmited
     */
    private long txBytes;
    private int txCarrierErrors;
    private int txCollisions;
    private int txCompressed;
    private int txDropped;
    private int txErrors;
    private int txFifo;
    private int txPackets;

    /**
     * @param tXBytes
     * @param tXCarrierErrors
     * @param tXCollisions
     * @param tXCompressed
     * @param tXDropped
     * @param tXErros
     * @param tXFifo
     * @param tXPackets
     */
    public ProcNetDevTransmit(long tXBytes,
                              int tXPackets,
                              int tXErros,
                              int tXDropped,
                              int tXFifo,
                              int tXCollisions,
                              int tXCarrierErrors,
                              int tXCompressed) {
        txBytes = tXBytes;
        txCarrierErrors = tXCarrierErrors;
        txCollisions = tXCollisions;
        txCompressed = tXCompressed;
        txDropped = tXDropped;
        txErrors = tXErros;
        txFifo = tXFifo;
        txPackets = tXPackets;
    }



    /**
     * @return the tX_Bytes
     */
    public long getTxBytes() {
        return txBytes;
    }



    /**
     * @return the tX_CarrierErrors
     */
    public int getTxCarrierErrors() {
        return txCarrierErrors;
    }



    /**
     * @return the tX_Collisions
     */
    public int getTxCollisions() {
        return txCollisions;
    }



    /**
     * @return the tX_Compressed
     */
    public int getTxCompressed() {
        return txCompressed;
    }



    /**
     * @return the tX_Dropped
     */
    public int getTxDropped() {
        return txDropped;
    }



    /**
     * @return the tX_Erros
     */
    public int getTxErrors() {
        return txErrors;
    }



    /**
     * @return the tX_Fifo
     */
    public int getTxFifo() {
        return txFifo;
    }



    /**
     * @return the tX_Packets
     */
    public int getTxPackets() {
        return txPackets;
    }
}
