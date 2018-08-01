# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/rm3l/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# please KEEP ALL THE NAMES
-keepnames class ** { *; }

#-libraryjars libs

-dontwarn javax.annotation.**



#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*


-dump class_files.txt
-printseeds seeds.txt
-printusage unused.txt
# Must be commented out, as specified by Crashlytics / Fabric guide
#see https://docs.fabric.io/android/crashlytics/dex-and-proguard.html
#-printmapping mapping.txt

-keepattributes SourceFile,LineNumberTable,Exceptions,Signature,Enclosing

-dontwarn io.fabric.sdk.android.services.common.AdvertisingInfoReflectionStrategy

# ensure that the appcompat libraries are in the Proguard exclusion list
-keep class android.support.v4.app.** { *; }
-keep class android.support.v4.view.** { *; }
-keep class android.support.v4.widget.** { *; }
-keep interface android.support.v4.widget.** { *; }

-keep interface android.support.v4.app.** { *; }
-keep interface android.support.v4.view.** { *; }

-keep class android.support.v7.app.** { *; }
-keep interface android.support.v7.app.** { *; }
-keep class android.support.v7.widget.** { *; }
-keep interface android.support.v7.widget.** { *; }


-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.billing.IInAppBillingService
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

-keep class com.android.common.view.** { *; }


#Retrofit (from Square)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

## Configuration for Guava 18.0
##
## disagrees with instructions provided by Guava project: https://code.google.com/p/guava-libraries/wiki/UsingProGuardWithGuava
#
#-dontwarn com.google.errorprone.annotations.**
#-dontwarn om.google.j2objc.annotations.**
#
#-dontwarn com.google.common.collect.**
#
#
#-keep class com.google.common.io.Resources {
#    public static <methods>;
#}
#-keep class com.google.common.collect.Lists {
#    public static ** reverse(**);
#}
#-keep class com.google.common.base.Charsets {
#    public static <fields>;
#}
#
#-keep class com.google.common.base.Joiner {
#    public static com.google.common.base.Joiner on(java.lang.String);
#    public ** join(...);
#}
#
#-keep class com.google.common.collect.MapMakerInternalMap$ReferenceEntry
#-keep class com.google.common.cache.LocalCache$ReferenceEntry
#
## http://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
#-dontwarn javax.annotation.**
#-dontwarn javax.inject.**
#-dontwarn sun.misc.Unsafe
#
## Guava 19.0
#-dontwarn java.lang.ClassValue
#-dontwarn com.google.j2objc.annotations.Weak
#-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Guava 24.0-android
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-dontwarn com.google.common.util.concurrent.FuturesGetChecked**
-dontwarn javax.lang.model.element.Modifier
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

# Retrolambda
-dontwarn java.lang.invoke.*

# AWS SDK
# Class names are needed in reflection
-keepnames class com.amazonaws.**
# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler
# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**
# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**


# LeakCanary
-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }
-dontwarn com.squareup.leakcanary.**


-dontwarn okio.**

#Wizardroid
-keepnames class * { @org.codepond.android.wizardroid.ContextVariable *;}


-dontwarn com.squareup.okhttp.**

-keep class me.panavtec.drawableview.** { *; }
-keep class me.panavtec.drawableview.gestures.** { *; }
-dontwarn me.panavtec.drawableview.internal.**


#-keep class .R
#-keep class **.R$* {
#    <fields>;
#}
-keepclasseswithmembers class org.rm3l.router_companion.tasker.R$* {
    public static final int define_*;
}
-keepattributes InnerClasses
-keep class org.rm3l.router_companion.tasker.R
-keep class org.rm3l.router_companion.tasker.R$* {
    <fields>;
}

-keep class org.rm3l.router_companion.tasker.BuildConfig

-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.jms.**
-dontwarn javax.naming.**
-dontwarn javax.mail.**
-dontwarn java.beans.**
-dontwarn java.management.**


-dontwarn org.apache.log4j.**


### Kotlin ###
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
#-keep class kotlin.reflect.jvm.internal.**
#-keep class kotlin.internal.**

-keep class org.rm3l.maoni.common.model.** { *; }

#-keepattributes Signature
#-keep class sun.misc.Unsafe { *; }
#-keep class * implements java.io.Serializable { *; }

-keep class khttp.**
-dontwarn java.nio.**

-dontwarn org.json.**
-dontwarn java.util.Base64
-dontwarn java.util.Base64$Encoder

-dontwarn com.android.support.**
-dontwarn org.jetbrains.anko.appcompat.v7.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.jms.**
-dontwarn javax.naming.**
-dontwarn javax.mail.**
-dontwarn java.beans.**
-dontwarn java.management.**

-dontwarn org.apache.log4j.**


### Kotlin ###
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
#-keep class kotlin.reflect.jvm.internal.**
#-keep class kotlin.internal.**


-keep class khttp.**
-dontwarn java.nio.**

-dontwarn org.json.**
-dontwarn java.util.Base64
-dontwarn java.util.Base64$Encoder


-dontwarn com.avocarrot.**
-keep class com.avocarrot.** { *; }
-keepclassmembers class com.avocarrot.** { *; }
-keep class com.avocarrot.androidsdk.** { *; }
-keepclassmembers class com.avocarrot.androidsdk.** { *; }
-keep public class * extends android.view.View {
    public void *(android.content.Context);
    public void *(android.content.Context, android.util.AttributeSet);
    public void *(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-dontwarn com.squareup.okhttp.**

-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**

-keep class com.airbnb.deeplinkdispatch.** { *; }
-keepclasseswithmembers class * {
     @com.airbnb.deeplinkdispatch.DeepLink <methods>;
}

-dontwarn okio.**

# LeakCanary
-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }
-dontwarn com.squareup.leakcanary.**

#Retrofit (from Square)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Configuration for Guava 18.0
#
# disagrees with instructions provided by Guava project: https://code.google.com/p/guava-libraries/wiki/UsingProGuardWithGuava

-dontwarn com.google.errorprone.annotations.**
-dontwarn om.google.j2objc.annotations.**

-dontwarn com.google.common.collect.**

-keep class com.google.common.io.Resources {
    public static <methods>;
}
-keep class com.google.common.collect.Lists {
    public static ** reverse(**);
}
-keep class com.google.common.base.Charsets {
    public static <fields>;
}

-keep class com.google.common.base.Joiner {
    public static com.google.common.base.Joiner on(java.lang.String);
    public ** join(...);
}

-keep class com.google.common.collect.MapMakerInternalMap$ReferenceEntry
-keep class com.google.common.cache.LocalCache$ReferenceEntry

-dontwarn com.google.common.collect.MinMaxPriorityQueue

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# http://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-dontwarn com.google.common.util.concurrent.FuturesGetChecked**
-dontwarn javax.lang.model.element.Modifier
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**


# AWS SDK
# Class names are needed in reflection
-keepnames class com.amazonaws.**
# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler
# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**
# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**



-keep class me.panavtec.drawableview.** { *; }
-keep class me.panavtec.drawableview.gestures.** { *; }
-dontwarn me.panavtec.drawableview.internal.**


# Evernote Android Job
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper
-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver
-keep public class com.evernote.android.job.JobRescheduleService

-dontwarn com.android.support.**
-dontwarn org.jetbrains.anko.appcompat.v7.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class com.twofortyfouram.annotation.**
-dontwarn com.twofortyfouram.annotation.**

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class android.graphics.** { *; }
-dontwarn android.**