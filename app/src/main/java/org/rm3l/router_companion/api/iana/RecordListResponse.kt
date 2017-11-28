package org.rm3l.router_companion.api.iana

/**
 * Created by rm3l on 28/11/17.
 */
data class RecordListResponse(val data: Data)

data class Data(val records: List<Record>)

data class Record @JvmOverloads constructor(
        val serviceName: String? = null /*Service Name*/,
        val portNumber: Long? = null /*Port Number*/,
        val transportProtocol: Protocol? = null /*Transport Protocol*/,
        val description: String? = null /*Description*/,
        val assignee: Person? = null /*Assignee*/,
        val contact: Person? = null /*Contact*/,
        val registrationDate: String? = null /*Registration Date*/,
        val modificationDate: String? = null /*Modification Date*/,
        val reference: String? = null /*Reference*/,
        val serviceCode: String? = null /*Service Code*/,
        val knownUnauthorizedUses: String? = null /*Unauthorized Uses*/,
        val assignmentNotes: String? = null /*Assignment Notes*/)

data class Person @JvmOverloads constructor (
        val id: String,
        val name: String,
        val org: String? = null,
        val uri: String? = null,
        val updated: String? = null)

enum class Protocol {
    TCP,
    UDP,
    DCCP,
    SCTP
}