package org.rm3l.router_companion.actions

import android.content.SharedPreferences
import org.rm3l.router_companion.actions.RouterAction.SERVICE_NAMES_PORT_NUMBERS_LOOKUP
import org.rm3l.router_companion.api.iana.Protocol
import org.rm3l.router_companion.api.iana.query
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.NetworkUtils

class ServiceNamesPortNumbersMappingLookupAction(
    router: Router,
    listener: RouterActionListener?,
    globalSharedPreferences: SharedPreferences,
    val ports: Collection<Long>? = null,
    val protocols: Collection<Protocol>? = null,
    val services: Collection<String>? = null
) :
        AbstractRouterAction<Void?>(router, listener, SERVICE_NAMES_PORT_NUMBERS_LOOKUP, globalSharedPreferences) {

    override fun doActionInBackground(): RouterActionResult<Void?> {
        var exception: Exception? = null
        try {
            val routerStreamActionListener = listener as? RouterStreamActionListener
            routerStreamActionListener?.notifyRouterActionProgress(SERVICE_NAMES_PORT_NUMBERS_LOOKUP, router,
                    0, null)
            val response = NetworkUtils.getServiceNamePortNumbersService().query(
                    ports = this.ports,
                    protocols = this.protocols,
                    services = this.services?.map { it.toLowerCase() }).execute()
            NetworkUtils.checkResponseSuccessful(response)
            val body = response.body() ?: throw IllegalStateException("Empty response")
            val records = body.data.records
            if (records.isEmpty()) {
                throw IllegalArgumentException("No mapping found")
            }
            val record = records[0]
            routerStreamActionListener?.notifyRouterActionProgress(SERVICE_NAMES_PORT_NUMBERS_LOOKUP, router,
                    100,
                    "- Service Name: ${record.serviceName}\n" +
                            "- Port Number: ${record.portNumber}\n" +
                            "- Transport Protocol: ${record.transportProtocol}\n" +
                            "- Description: ${record.description}\n" +
                            (if (record.assignmentNotes.isNullOrBlank()) "" else "- Assignment Notes: ${record.assignmentNotes}\n") +
                            (if (record.registrationDate.isNullOrBlank()) "" else "- Registration Date: ${record.registrationDate}\n") +
                            (if (record.modificationDate.isNullOrBlank()) "" else "- Modification Date: ${record.modificationDate}\n"))
        } catch (e: Exception) {
            e.printStackTrace()
            exception = e
        }
        return RouterActionResult(null, exception)
    }
}