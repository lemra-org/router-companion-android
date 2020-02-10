package org.rm3l.router_companion.tasker.api.urlshortener.firebase.dynamiclinks.resources;

public class ShortLinksDataRequest {

  private DynamicLinkInfo dynamicLinkInfo;
  private Suffix suffix;

  public DynamicLinkInfo getDynamicLinkInfo() {
    return dynamicLinkInfo;
  }

  public ShortLinksDataRequest setDynamicLinkInfo(final DynamicLinkInfo dynamicLinkInfo) {
    this.dynamicLinkInfo = dynamicLinkInfo;
    return this;
  }

  public Suffix getSuffix() {
    return suffix;
  }

  public ShortLinksDataRequest setSuffix(final Suffix suffix) {
    this.suffix = suffix;
    return this;
  }

  public static class DynamicLinkInfo {
    private String domainUriPrefix = "https://ddwrtcompanion.page.link";
    private String link;
    private AndroidInfo androidInfo;
    private IosInfo iosInfo;
    private NavigationInfo navigationInfo;
    private AnalyticsInfo analyticsInfo;
    private SocialMetaTagInfo socialMetaTagInfo;

    public String getDomainUriPrefix() {
      return domainUriPrefix;
    }

    public DynamicLinkInfo setDomainUriPrefix(final String domainUriPrefix) {
      this.domainUriPrefix = domainUriPrefix;
      return this;
    }

    public String getLink() {
      return link;
    }

    public DynamicLinkInfo setLink(final String link) {
      this.link = link;
      return this;
    }

    public AndroidInfo getAndroidInfo() {
      return androidInfo;
    }

    public DynamicLinkInfo setAndroidInfo(final AndroidInfo androidInfo) {
      this.androidInfo = androidInfo;
      return this;
    }

    public IosInfo getIosInfo() {
      return iosInfo;
    }

    public DynamicLinkInfo setIosInfo(final IosInfo iosInfo) {
      this.iosInfo = iosInfo;
      return this;
    }

    public NavigationInfo getNavigationInfo() {
      return navigationInfo;
    }

    public DynamicLinkInfo setNavigationInfo(final NavigationInfo navigationInfo) {
      this.navigationInfo = navigationInfo;
      return this;
    }

    public AnalyticsInfo getAnalyticsInfo() {
      return analyticsInfo;
    }

    public DynamicLinkInfo setAnalyticsInfo(final AnalyticsInfo analyticsInfo) {
      this.analyticsInfo = analyticsInfo;
      return this;
    }

    public SocialMetaTagInfo getSocialMetaTagInfo() {
      return socialMetaTagInfo;
    }

    public DynamicLinkInfo setSocialMetaTagInfo(final SocialMetaTagInfo socialMetaTagInfo) {
      this.socialMetaTagInfo = socialMetaTagInfo;
      return this;
    }

    public static class AndroidInfo {
      private String androidPackageName;
      private String androidFallbackLink;
      private String androidMinPackageVersionCode;

      public String getAndroidPackageName() {
        return androidPackageName;
      }

      public AndroidInfo setAndroidPackageName(final String androidPackageName) {
        this.androidPackageName = androidPackageName;
        return this;
      }

      public String getAndroidFallbackLink() {
        return androidFallbackLink;
      }

      public AndroidInfo setAndroidFallbackLink(final String androidFallbackLink) {
        this.androidFallbackLink = androidFallbackLink;
        return this;
      }

      public String getAndroidMinPackageVersionCode() {
        return androidMinPackageVersionCode;
      }

      public AndroidInfo setAndroidMinPackageVersionCode(
          final String androidMinPackageVersionCode) {
        this.androidMinPackageVersionCode = androidMinPackageVersionCode;
        return this;
      }
    }

    public static class IosInfo {
      private String iosBundleId;
      private String iosFallbackLink;
      private String iosCustomScheme;
      private String iosIpadFallbackLink;
      private String iosIpadBundleId;
      private String iosAppStoreId;

      public String getIosBundleId() {
        return iosBundleId;
      }

      public IosInfo setIosBundleId(final String iosBundleId) {
        this.iosBundleId = iosBundleId;
        return this;
      }

      public String getIosFallbackLink() {
        return iosFallbackLink;
      }

      public IosInfo setIosFallbackLink(final String iosFallbackLink) {
        this.iosFallbackLink = iosFallbackLink;
        return this;
      }

      public String getIosCustomScheme() {
        return iosCustomScheme;
      }

      public IosInfo setIosCustomScheme(final String iosCustomScheme) {
        this.iosCustomScheme = iosCustomScheme;
        return this;
      }

      public String getIosIpadFallbackLink() {
        return iosIpadFallbackLink;
      }

      public IosInfo setIosIpadFallbackLink(final String iosIpadFallbackLink) {
        this.iosIpadFallbackLink = iosIpadFallbackLink;
        return this;
      }

      public String getIosIpadBundleId() {
        return iosIpadBundleId;
      }

      public IosInfo setIosIpadBundleId(final String iosIpadBundleId) {
        this.iosIpadBundleId = iosIpadBundleId;
        return this;
      }

      public String getIosAppStoreId() {
        return iosAppStoreId;
      }

      public IosInfo setIosAppStoreId(final String iosAppStoreId) {
        this.iosAppStoreId = iosAppStoreId;
        return this;
      }
    }

    public static class NavigationInfo {
      private boolean enableForcedRedirect = false;

      public boolean isEnableForcedRedirect() {
        return enableForcedRedirect;
      }

      public NavigationInfo setEnableForcedRedirect(final boolean enableForcedRedirect) {
        this.enableForcedRedirect = enableForcedRedirect;
        return this;
      }
    }

    public static class AnalyticsInfo {

      public static class GooglePlayAnalytics {
        private String utmSource;
        private String utmMedium;
        private String utmCampaign;
        private String utmTerm;
        private String utmContent;
        private String gclid;

        public String getUtmSource() {
          return utmSource;
        }

        public GooglePlayAnalytics setUtmSource(final String utmSource) {
          this.utmSource = utmSource;
          return this;
        }

        public String getUtmMedium() {
          return utmMedium;
        }

        public GooglePlayAnalytics setUtmMedium(final String utmMedium) {
          this.utmMedium = utmMedium;
          return this;
        }

        public String getUtmCampaign() {
          return utmCampaign;
        }

        public GooglePlayAnalytics setUtmCampaign(final String utmCampaign) {
          this.utmCampaign = utmCampaign;
          return this;
        }

        public String getUtmTerm() {
          return utmTerm;
        }

        public GooglePlayAnalytics setUtmTerm(final String utmTerm) {
          this.utmTerm = utmTerm;
          return this;
        }

        public String getUtmContent() {
          return utmContent;
        }

        public GooglePlayAnalytics setUtmContent(final String utmContent) {
          this.utmContent = utmContent;
          return this;
        }

        public String getGclid() {
          return gclid;
        }

        public GooglePlayAnalytics setGclid(final String gclid) {
          this.gclid = gclid;
          return this;
        }
      }

      public static class ItunesConnectAnalytics {
        private String at;
        private String ct;
        private String mt;
        private String pt;

        public String getAt() {
          return at;
        }

        public ItunesConnectAnalytics setAt(final String at) {
          this.at = at;
          return this;
        }

        public String getCt() {
          return ct;
        }

        public ItunesConnectAnalytics setCt(final String ct) {
          this.ct = ct;
          return this;
        }

        public String getMt() {
          return mt;
        }

        public ItunesConnectAnalytics setMt(final String mt) {
          this.mt = mt;
          return this;
        }

        public String getPt() {
          return pt;
        }

        public ItunesConnectAnalytics setPt(final String pt) {
          this.pt = pt;
          return this;
        }
      }
    }

    public static class SocialMetaTagInfo {
      private String socialTitle;
      private String socialDescription;
      private String socialImageLink;

      public String getSocialTitle() {
        return socialTitle;
      }

      public SocialMetaTagInfo setSocialTitle(final String socialTitle) {
        this.socialTitle = socialTitle;
        return this;
      }

      public String getSocialDescription() {
        return socialDescription;
      }

      public SocialMetaTagInfo setSocialDescription(final String socialDescription) {
        this.socialDescription = socialDescription;
        return this;
      }

      public String getSocialImageLink() {
        return socialImageLink;
      }

      public SocialMetaTagInfo setSocialImageLink(final String socialImageLink) {
        this.socialImageLink = socialImageLink;
        return this;
      }
    }
  }

  public static class Suffix {
    private SuffixOption option = SuffixOption.UNGUESSABLE;

    public SuffixOption getOption() {
      return option;
    }

    public Suffix setOption(final SuffixOption option) {
      this.option = option;
      return this;
    }

    public enum SuffixOption {
      SHORT,
      UNGUESSABLE
    }
  }
}
