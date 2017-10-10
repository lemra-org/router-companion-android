package org.rm3l.router_companion.resources

/**
 * Created by rm3l on 15/06/15.
 */
data class ProcNetDevTransmit
/**
 * @param tXBytes
 * *
 * @param tXCarrierErrors
 * *
 * @param tXCollisions
 * *
 * @param tXCompressed
 * *
 * @param tXDropped
 * *
 * @param tXErros
 * *
 * @param tXFifo
 * *
 * @param tXPackets
 */
(
        /**
         * Transmited
         */
        /**
         * @return the tX_Bytes
         */
        val txBytes: Long,
        /**
         * @return the tX_Packets
         */
        val txPackets: Int,
        /**
         * @return the tX_Erros
         */
        val txErrors: Int,
        /**
         * @return the tX_Dropped
         */
        val txDropped: Int,
        /**
         * @return the tX_Fifo
         */
        val txFifo: Int,
        /**
         * @return the tX_Collisions
         */
        val txCollisions: Int,
        /**
         * @return the tX_CarrierErrors
         */
        val txCarrierErrors: Int,
        /**
         * @return the tX_Compressed
         */
        val txCompressed: Int)
