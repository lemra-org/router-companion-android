package org.rm3l.router_companion.resources

/**
 * Created by rm3l on 15/06/15.
 */
data class ProcNetDevReceive(
        /**
         * Received
         */
    /**
     * @return the rx_Bytes
     */
    val rxBytes: Long,
    /**
     * @return the rx_Packets
     */
    val rxPackets: Int,
    /**
     * @return the rx_Erros
     */
    val rxErrors: Int,
    /**
     * @return the rx_Dropped
     */
    val rxDropped: Int,
    /**
     * @return the rx_Fifo
     */
    val rxFifo: Int,
    /**
     * @return the rx_Frame
     */
    val rxFrame: Int,
    /**
     * @return the rx_Compressed
     */
    val rxCompressed: Int,
    /**
     * @return the rx_Multicast
     */
    val rxMulticast: Int
)
