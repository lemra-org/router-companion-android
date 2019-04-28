package org.rm3l.router_companion.api.iana

/**
 * Created by rm3l on 28/11/17.
 */

//Problem with deserialization of Kotlin Data classes with Retrofit and Gson:
//https://xrubio.com/2019/04/the-case-of-deserialization-of-a-kotlin-data-class/
//
//Gson does not make use of accessors, and Proguard (R8) tends to drop non-public class members
//cf. https://github.com/google/gson/issues/232

class RecordListResponse(@JvmField val data: Data) {
    fun getData() = data

    override fun toString(): String {
        return "RecordListResponse(data=$data)"
    }
}

class Data(@JvmField val records: List<Record>) {
    fun getRecords() = records

    override fun toString(): String {
        return "Data(records=$records)"
    }
}

class Record @JvmOverloads constructor(
        @JvmField val serviceName: String? = null /*Service Name*/,
        @JvmField val portNumber: Long? = null /*Port Number*/,
        @JvmField val transportProtocol: Protocol? = null /*Transport Protocol*/,
        @JvmField val description: String? = null /*Description*/,
        @JvmField val assignee: Person? = null /*Assignee*/,
        @JvmField val contact: Person? = null /*Contact*/,
        @JvmField val registrationDate: String? = null /*Registration Date*/,
        @JvmField val modificationDate: String? = null /*Modification Date*/,
        @JvmField val reference: String? = null /*Reference*/,
        @JvmField val serviceCode: String? = null /*Service Code*/,
        @JvmField val knownUnauthorizedUses: String? = null /*Unauthorized Uses*/,
        @JvmField val assignmentNotes: String? = null /*Assignment Notes*/) {

    fun getServiceName() = serviceName
    fun getPortNumber() = portNumber
    fun getTransportProtocol() = transportProtocol
    fun getDescription() = description
    fun getAssignee() = assignee
    fun getContact() = contact
    fun getRegistrationDate() = registrationDate
    fun getModificationDate() = modificationDate
    fun getReference() = reference
    fun getServiceCode() = serviceCode
    fun getKnownUnauthorizedUses() = knownUnauthorizedUses
    fun getAssignmentNotes() = assignmentNotes

    override fun toString(): String {
        return "Record(serviceName=$serviceName, portNumber=$portNumber, transportProtocol=$transportProtocol, " +
                "description=$description, assignee=$assignee, contact=$contact, registrationDate=$registrationDate, " +
                "modificationDate=$modificationDate, reference=$reference, serviceCode=$serviceCode, " +
                "knownUnauthorizedUses=$knownUnauthorizedUses, assignmentNotes=$assignmentNotes)"
    }
}

class Person @JvmOverloads constructor (
        @JvmField val id: String,
        @JvmField val name: String,
        @JvmField val org: String? = null,
        @JvmField val uri: String? = null,
        @JvmField val updated: String? = null) {

    fun getId() = id
    fun getName() = name
    fun getOrg() = org
    fun getUri() = uri
    fun getUpdated() = updated

    override fun toString(): String {
        return "Person(id='$id', name='$name', org=$org, uri=$uri, updated=$updated)"
    }
}

enum class Protocol {
    TCP,
    UDP,
    DCCP,
    SCTP,
    DDP
}