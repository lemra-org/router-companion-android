package org.rm3l.router_companion.api.feedback

//cf. https://doorbell.io/docs/api
class DoorbellSubmitRequest @JvmOverloads constructor(
    /**
     * The email address of the user sending feedback
     */
    var email: String,

    /**
     * The feedback message the user has entered
     */
    var message: String,

    /**
     * The name of the user who is sending feedback
     */
    var name: String? = null,

    /**
     * The IP address of the end user, so your server's IP address isn't the one saved
     */
    var ip: String? = null,

    /**
     * The sentiment of the message, if you don't want this analyzed automatically.
     * The possible values are: positive, neutral, or negative
     */
    var sentiment: Sentiment? = null,

    /**
     * JSON encoded array of strings, or a comma separated string of tags you want to add to the feedback
     */
    var tags: List<String>? = emptyList(),

    /**
     * JSON encoded string of properties (of any data type), any extra metadata you want to attach to the message
     */
    var properties: Map<String, Any>? = emptyMap(),

    /**
     * Array of attachment IDs (get these from uploading to the upload endpoint
     */
    var attachments: List<Long>? = null,

    /**
     * The NPS rating the user submitted alongside the message
     */
    var nps: Int? = null,

    /**
     * The language the site/app is using. The response messages will be in the corresponding language, if translated
     */
    var language: String? = null
)

enum class Sentiment {
    positive,
    neutral,
    negative
}