package org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources

import org.rm3l.router_companion.api.urlshortener.firebase.dynamiclinks.resources.SuffixOption.UNGUESSABLE

// https://firebase.google.com/docs/reference/dynamic-links/link-shortener

data class ShortLinksDataRequest @JvmOverloads constructor(
    var dynamicLinkInfo: DynamicLinkInfo,
    var suffix: Suffix? = null
)

data class DynamicLinkInfo @JvmOverloads constructor(
    var domainUriPrefix: String = "https://ddwrtcompanion.page.link",
    var link: String,
    var androidInfo: AndroidInfo? = null,
    var iosInfo: IosInfo? = null,
    var navigationInfo: NavigationInfo? = null,
    var analyticsInfo: AnalyticsInfo? = null,
    var socialMetaTagInfo: SocialMetaTagInfo? = null
)

data class AndroidInfo(
    var androidPackageName: String? = null,
    var androidFallbackLink: String? = null,
    var androidMinPackageVersionCode: String? = null
)

data class IosInfo(
    var iosBundleId: String? = null,
    var iosFallbackLink: String? = null,
    var iosCustomScheme: String? = null,
    var iosIpadFallbackLink: String? = null,
    var iosIpadBundleId: String? = null,
    var iosAppStoreId: String? = null
)

data class NavigationInfo(var enableForcedRedirect: Boolean? = false)

data class AnalyticsInfo(
    var googlePlayAnalytics: GooglePlayAnalytics? = null,
    var itunesConnectAnalytics: ItunesConnectAnalytics? = null
)

data class GooglePlayAnalytics(
    var utmSource: String? = null,
    var utmMedium: String? = null,
    var utmCampaign: String? = null,
    var utmTerm: String? = null,
    var utmContent: String? = null,
    var gclid: String? = null
)

data class ItunesConnectAnalytics(
    var at: String? = null,
    var ct: String? = null,
    var mt: String? = null,
    var pt: String? = null
)

data class SocialMetaTagInfo(
    var socialTitle: String? = null,
    var socialDescription: String? = null,
    var socialImageLink: String? = null
)

data class Suffix(
    var option: SuffixOption? = UNGUESSABLE
)

enum class SuffixOption {
    SHORT,
    UNGUESSABLE
}