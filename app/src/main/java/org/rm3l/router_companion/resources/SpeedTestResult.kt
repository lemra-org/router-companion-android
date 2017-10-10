package org.rm3l.router_companion.resources

import org.rm3l.router_companion.actions.activity.PingRTT

/**
 * Created by rm3l on 12/01/16.
 */
class SpeedTestResult {

    /**
     * the internal id (in DB)
     */
    private var id = -1L

    private var router: String? = null

    private //YYYY-MM-dd
    var date: String? = null

    private var wanPing: Number? = null

    private var wanDl: Number? = null

    private var wanUl: Number? = null

    /**
     * Useless ?
     */
    private var connectionType: String? = null

    private var connectionDl: Number? = null

    private var connectionUl: Number? = null

    private var server: String? = null

    private var serverCountryCode: String? = null

    private var wanPingRTT: PingRTT? = null

    private var wanDLFileSize: Number? = null

    private var wanDLDuration: Long? = null

    private var wanULFileSize: Number? = null

    private var wanULDuration: Long? = null

    private var connectionDLFileSize: Number? = null

    private var connectionDLDuration: Long? = null

    private var connectionULFileSize: Number? = null

    private var connectionULDuration: Long? = null

    constructor(router: String, date: String, server: String,
                wanPing: Number, wanDl: Number, wanUl: Number,
                connectionType: String?, connectionDl: Number?, connectionUl: Number?,
                serverCountryCode: String?) {

        this.router = router
        this.date = date
        this.wanDl = wanDl
        this.wanUl = wanUl
        this.connectionType = connectionType
        this.connectionDl = connectionDl
        this.connectionUl = connectionUl
        this.server = server
        this.serverCountryCode = serverCountryCode
        this.wanPing = wanPing
        this.wanPingRTT = PingRTT().setAvg(wanPing.toFloat())
    }

    constructor()

    fun getId(): Long {
        return id
    }

    fun setId(id: Long): SpeedTestResult {
        this.id = id
        return this
    }

    fun getServerCountryCode(): String? {
        return serverCountryCode
    }

    fun setServerCountryCode(serverCountryCode: String?): SpeedTestResult {
        this.serverCountryCode = serverCountryCode
        return this
    }

    fun getRouter(): String? {
        return router
    }

    fun setRouter(router: String?): SpeedTestResult {
        this.router = router
        return this
    }

    fun getDate(): String? {
        return date
    }

    fun setDate(date: String?): SpeedTestResult {
        this.date = date
        return this
    }

    fun getWanPing(): Number? {
        return wanPing
    }

    fun setWanPing(wanPing: Number?): SpeedTestResult {
        this.wanPing = wanPing
        return this
    }

    fun getWanDl(): Number? {
        return wanDl
    }

    fun setWanDl(wanDl: Number?): SpeedTestResult {
        this.wanDl = wanDl
        return this
    }

    fun getWanUl(): Number? {
        return wanUl
    }

    fun setWanUl(wanUl: Number?): SpeedTestResult {
        this.wanUl = wanUl
        return this
    }

    fun getConnectionType(): String? {
        return connectionType
    }

    fun setConnectionType(connectionType: String?): SpeedTestResult {
        this.connectionType = connectionType
        return this
    }

    fun getConnectionDl(): Number? {
        return connectionDl
    }

    fun setConnectionDl(connectionDl: Number?): SpeedTestResult {
        this.connectionDl = connectionDl
        return this
    }

    fun getConnectionUl(): Number? {
        return connectionUl
    }

    fun setConnectionUl(connectionUl: Number?): SpeedTestResult {
        this.connectionUl = connectionUl
        return this
    }

    fun getServer(): String? {
        return server
    }

    fun setServer(server: String?): SpeedTestResult {
        this.server = server
        return this
    }

    fun getWanPingRTT(): PingRTT? {
        return wanPingRTT
    }

    fun setWanPingRTT(wanPingRTT: PingRTT?): SpeedTestResult {
        this.wanPingRTT = wanPingRTT
        if (wanPingRTT != null) {
            this.setWanPing(wanPingRTT.avg)
        }
        return this
    }

    fun getWanDLFileSize(): Number? {
        return wanDLFileSize
    }

    fun setWanDLFileSize(wanDLFileSize: Number?): SpeedTestResult {
        this.wanDLFileSize = wanDLFileSize
        return this
    }

    fun getWanDLDuration(): Long? {
        return wanDLDuration
    }

    fun setWanDLDuration(wanDLDuration: Long?): SpeedTestResult {
        this.wanDLDuration = wanDLDuration
        return this
    }

    fun getWanULFileSize(): Number? {
        return wanULFileSize
    }

    fun setWanULFileSize(wanULFileSize: Number?): SpeedTestResult {
        this.wanULFileSize = wanULFileSize
        return this
    }

    fun getWanULDuration(): Long? {
        return wanULDuration
    }

    fun setWanULDuration(wanULDuration: Long?): SpeedTestResult {
        this.wanULDuration = wanULDuration
        return this
    }

    fun getConnectionDLFileSize(): Number? {
        return connectionDLFileSize
    }

    fun setConnectionDLFileSize(connectionDLFileSize: Number?): SpeedTestResult {
        this.connectionDLFileSize = connectionDLFileSize
        return this
    }

    fun getConnectionDLDuration(): Long? {
        return connectionDLDuration
    }

    fun setConnectionDLDuration(connectionDLDuration: Long?): SpeedTestResult {
        this.connectionDLDuration = connectionDLDuration
        return this
    }

    fun getConnectionULFileSize(): Number? {
        return connectionULFileSize
    }

    fun setConnectionULFileSize(connectionULFileSize: Number?): SpeedTestResult {
        this.connectionULFileSize = connectionULFileSize
        return this
    }

    fun getConnectionULDuration(): Long? {
        return connectionULDuration
    }

    fun setConnectionULDuration(connectionULDuration: Long?): SpeedTestResult {
        this.connectionULDuration = connectionULDuration
        return this
    }
}

