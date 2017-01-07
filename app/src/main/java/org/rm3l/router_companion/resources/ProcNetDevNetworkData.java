package org.rm3l.router_companion.resources;

/**
 * Created by rm3l on 15/06/15.
 */
public class ProcNetDevNetworkData {

    public ProcNetDevNetworkData(String interfaceName,
                       long rxBytes,
                       int rxPackets,
                       int rxErrors,
                       int rxDropped,
                       int rxFifo,
                       int rxFrame,
                       int rxCompressed,
                       int rxMulticast,
                       long txBytes,
                       int txPackets,
                       int txErrors,
                       int txDropped,
                       int txFifo,
                       int txCollisions,
                       int txCarrierErrors,
                       int txCompressed) {

        this.interfaceName = interfaceName;
        this.mReceive = new ProcNetDevReceive(rxBytes, rxPackets, 
                rxErrors, rxDropped, rxFifo, rxFrame, rxCompressed, rxMulticast);
        this.mTransmit = new ProcNetDevTransmit(txBytes, txPackets, 
                txErrors, txDropped, txFifo, txCollisions, txCarrierErrors, txCompressed);

    }
    private String interfaceName;
    private ProcNetDevReceive mReceive;
    private ProcNetDevTransmit mTransmit;

    /**
     * INTERFACE, R_BYTES, R_PACKETS, R_ERRORS, R_DROP, R_FIFO, R_FRAME,
     * R_COMPRESSED, R_MULTICAST, T_BYTES, T_PACKETS, T_ERRORS, T_DROP, T_FIFO,
     * T_COLLS, T_CARRIER, T_COMPRESSED
     */
    @Override
    public String toString() {
        return interfaceName + ", " + String.valueOf(this.mReceive.getRxBytes() + ", ")
                + String.valueOf(this.mReceive.getRxPackets() + ", ")
                + String.valueOf(this.mReceive.getRxErrors() + ", ")
                + String.valueOf(this.mReceive.getRxDropped() + ", ")
                + String.valueOf(this.mReceive.getRxFifo() + ", ")
                + String.valueOf(this.mReceive.getRxFrame() + ", ")
                + String.valueOf(this.mReceive.getRxCompressed() + ", ")
                + String.valueOf(this.mReceive.getRxMulticast() + ", ")
                + String.valueOf(this.mTransmit.getTxBytes() + ", ")
                + String.valueOf(this.mTransmit.getTxPackets() + ", ")
                + String.valueOf(this.mTransmit.getTxErrors() + ", ")
                + String.valueOf(this.mTransmit.getTxDropped() + ", ")
                + String.valueOf(this.mTransmit.getTxFifo() + ", ")
                + String.valueOf(this.mTransmit.getTxCollisions() + ", ")
                + String.valueOf(this.mTransmit.getTxCarrierErrors() + ", ")
                + String.valueOf(this.mTransmit.getTxCompressed());
    }

    /**
     * @return the interfaceName
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * @return the mReceive
     */
    public ProcNetDevReceive getReceive() {
        return mReceive;
    }

    /**
     * @return the mTransmit
     */
    public ProcNetDevTransmit getTransmit() {
        return mTransmit;
    }
}
