package org.rm3l.router_companion.resources

/**
 * Created by rm3l on 15/06/15.
 */
class ProcNetDevNetworkData(
        /**
         * @return the interfaceName
         */
        val interfaceName: String, rxBytes: Long, rxPackets: Int, rxErrors: Int,
        rxDropped: Int, rxFifo: Int, rxFrame: Int, rxCompressed: Int, rxMulticast: Int, txBytes: Long,
        txPackets: Int, txErrors: Int, txDropped: Int, txFifo: Int, txCollisions: Int,
        txCarrierErrors: Int,
        txCompressed: Int) {
    /**
     * @return the mReceive
     */
    val receive: ProcNetDevReceive
    /**
     * @return the mTransmit
     */
    val transmit: ProcNetDevTransmit

    init {
        this.receive = ProcNetDevReceive(rxBytes, rxPackets, rxErrors, rxDropped, rxFifo, rxFrame,
                rxCompressed, rxMulticast)
        this.transmit = ProcNetDevTransmit(txBytes, txPackets, txErrors, txDropped, txFifo,
                txCollisions,
                txCarrierErrors, txCompressed)
    }

    /**
     * INTERFACE, R_BYTES, R_PACKETS, R_ERRORS, R_DROP, R_FIFO, R_FRAME,
     * R_COMPRESSED, R_MULTICAST, T_BYTES, T_PACKETS, T_ERRORS, T_DROP, T_FIFO,
     * T_COLLS, T_CARRIER, T_COMPRESSED
     */
    override fun toString(): String {
        return interfaceName + ", " + (this.receive.rxBytes.toString() + ", ") + (this.receive.rxPackets.toString() + ", ") + (this.receive.rxErrors.toString() + ", ") + (this.receive.rxDropped.toString() + ", ") + (this.receive.rxFifo.toString() + ", ") + (this.receive.rxFrame.toString() + ", ") + (this.receive.rxCompressed.toString() + ", ") + (this.receive.rxMulticast.toString() + ", ") + (this.transmit.txBytes.toString() + ", ") + (this.transmit.txPackets.toString() + ", ") + (this.transmit.txErrors.toString() + ", ") + (this.transmit.txDropped.toString() + ", ") + (this.transmit.txFifo.toString() + ", ") + (this.transmit.txCollisions.toString() + ", ") + (this.transmit.txCarrierErrors.toString() + ", ") + this.transmit.txCompressed.toString()
    }
}
