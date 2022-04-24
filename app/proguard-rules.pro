#
# DD-WRT Companion is a mobile app that lets you connect to,
# monitor and manage your DD-WRT routers on the go.
#
# Copyright (C) 2014-2022  Armel Soro
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# Contact Info: Armel Soro &lt;armel+router_companion AT rm3l DOT org&gt;
#

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
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

-printconfiguration config.txt

# please KEEP ALL THE NAMES
-keepnames class ** { *; }

#Repackage all class files that are renamed, by moving them into the single given package.
-repackageclasses ''

#Specifies that the access modifiers of classes and class members may be broadened during processing. This can improve the results of the optimization step.
-allowaccessmodification

#Specifies that interfaces may be merged, even if their implementing classes don't implement all interface methods.
#This can reduce the size of the output by reducing the total number of classes.
-mergeinterfacesaggressively

#Allow different obfuscated names
-useuniqueclassmembernames

# Keep enumerations
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

###########################
# DD-WRT Companion
###########################
-keep class org.rm3l.router_companion.firmwares.impl.** {
    *;
}
###########################


#########
# MobFox
#########
-keep class com.mobfox. {*;}
-keep class com.mobfox.adapter. {*;}
-keep class com.mobfox.sdk. {*;}
#########

########
# Guava
########
-dontwarn javax.lang.model.element.Modifier

# Note: We intentionally don't add the flags we'd need to make Enums work.
# That's because the Proguard configuration required to make it work on
# optimized code would preclude lots of optimization, like converting enums
# into ints.

# Throwables uses internal APIs for lazy stack trace resolution
-dontnote sun.misc.SharedSecrets
-keep class sun.misc.SharedSecrets {
  *** getJavaLangAccess(...);
}
-dontnote sun.misc.JavaLangAccess
-keep class sun.misc.JavaLangAccess {
  *** getStackTraceElement(...);
  *** getStackTraceDepth(...);
}

# FinalizableReferenceQueue calls this reflectively
# Proguard is intelligent enough to spot the use of reflection onto this, so we
# only need to keep the names, and allow it to be stripped out if
# FinalizableReferenceQueue is unused.
-keepnames class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
# However, it cannot "spot" that this method needs to be kept IF the class is.
-keepclassmembers class com.google.common.base.internal.Finalizer {
  *** startFinalizer(...);
}
-keepnames class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}
-keepclassmembers class com.google.common.base.FinalizableReference {
  void finalizeReferent();
}

# Striped64, LittleEndianByteArray, UnsignedBytes, AbstractFuture
-dontwarn sun.misc.Unsafe

# Striped64 appears to make some assumptions about object layout that
# really might not be safe. This should be investigated.
-keepclassmembers class com.google.common.cache.Striped64 {
  *** base;
  *** busy;
}
-keepclassmembers class com.google.common.cache.Striped64$Cell {
  <fields>;
}

-dontwarn java.lang.SafeVarargs

-keep class java.lang.Throwable {
  *** addSuppressed(...);
}

# Futures.getChecked, in both of its variants, is incompatible with proguard.

# Used by AtomicReferenceFieldUpdater and sun.misc.Unsafe
-keepclassmembers class com.google.common.util.concurrent.AbstractFuture** {
  *** waiters;
  *** value;
  *** listeners;
  *** thread;
  *** next;
}
-keepclassmembers class com.google.common.util.concurrent.AtomicDouble {
  *** value;
}
-keepclassmembers class com.google.common.util.concurrent.AggregateFutureState {
  *** remaining;
  *** seenExceptions;
}

# Since Unsafe is using the field offsets of these inner classes, we don't want
# to have class merging or similar tricks applied to these classes and their
# fields. It's safe to allow obfuscation, since the by-name references are
# already preserved in the -keep statement above.
-keep,allowshrinking,allowobfuscation class com.google.common.util.concurrent.AbstractFuture** {
  <fields>;
}

# Futures.getChecked (which often won't work with Proguard anyway) uses this. It
# has a fallback, but again, don't use Futures.getChecked on Android regardless.
-dontwarn java.lang.ClassValue

# MoreExecutors references AppEngine
-dontnote com.google.appengine.api.ThreadManager
-keep class com.google.appengine.api.ThreadManager {
  static *** currentRequestThreadFactory(...);
}
-dontnote com.google.apphosting.api.ApiProxy
-keep class com.google.apphosting.api.ApiProxy {
  static *** getCurrentEnvironment (...);
}
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
################################

##############################
# Amazon AWS Android
##############################
# Class names are needed in reflection
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**
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
################################

##############################
# jcraft jsch
##############################
-keep class com.jcraft.jsch.jce.*
-keep class * extends com.jcraft.jsch.KeyExchange
-keep class com.jcraft.jsch.**
-dontwarn com.jcraft.jzlib.**
-dontwarn org.ietf.jgss.**
#-keep class com.jcraft.jsch.** { *; }
################################

##############################
# Umano SlidingUpPannel
##############################
-dontwarn android.graphics.Canvas
##############################

##############################
# FabButton
##############################
# In https://github.com/ckurtm/FabButton/blob/master/fabbutton/src/main/java/mbanje/kurt/fabbutton/FabButton.java ,
# R.anim.design_fab_in is used only for SDK >= 14, and we are using a minSDK of 15
-dontwarn mbanje.kurt.fabbutton.FabButton$Behavior
##############################

##############################
# drawableview
##############################
-dontwarn me.panavtec.drawableview.**
#-keep class me.panavtec.drawableview.** { *; }
#-keep class me.panavtec.drawableview.gestures.** { *; }
#-dontwarn me.panavtec.drawableview.internal.**
##############################

##############################
# Google GMS PlayServices
##############################
-keep class com.google.android.gms.measurement.AppMeasurement  { *; }
##############################

##############################
# AboutLibraries
##############################
-keep class .R
-keep class **.R$* {
    <fields>;
}
##############################












###########################


#
#-dontwarn javax.annotation.**
#-dontwarn org.ietf.jgss.**
#-dontwarn com.jcraft.jzlib.**
#-dontwarn sun.misc.**
#
## Because the following classes make use of reflection (clazz.newInstance(...))
#-keep class org.rm3l.router_companion.fragments.**
#-keep class org.rm3l.router_companion.prefs.sort.**
#-keep class org.rm3l.router_companion.firmwares.**
#
##ACRA specifics
## Restore some Source file names and restore approximate line numbers in the stack traces,
## otherwise the stack traces are pretty useless
#-keepattributes SourceFile,LineNumberTable
#
#-optimizationpasses 5
#-dontusemixedcaseclassnames
#-dontskipnonpubliclibraryclasses
#-dontpreverify
#-verbose
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#
#
#-dump class_files.txt
#-printseeds seeds.txt
#-printusage unused.txt
## Must be commented out, as specified by Crashlytics / Fabric guide
##see https://docs.fabric.io/android/crashlytics/dex-and-proguard.html
##-printmapping mapping.txt
#
#-keepattributes SourceFile,LineNumberTable,Exceptions,Signature,Enclosing
#
#
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class com.android.vending.licensing.ILicensingService
#-keep public class com.android.vending.billing.IInAppBillingService
#
#-keepclasseswithmembernames class * {
#    native <methods>;
#}
#
#-keepclasseswithmembernames class * {
#    public <init>(android.content.Context,android.util.AttributeSet);
#}
#
#-keepclasseswithmembernames class * {
#    public <init>(android.content.Context, android.util.AttributeSet,int);
#}
#
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-keepattributes InnerClasses,EnclosingMethod
#
## ACRA needs "annotations" so add this...
## Note: This may already be defined in the default "proguard-android-optimize.txt"
## file in the SDK. If it is, then you don't need to duplicate it. See your
## "project.properties" file to get the path to the default "proguard-android-optimize.txt".
#-keepattributes *Annotation*
## Keep all the ACRA classes
##-keep class org.acra.** { *; }
#
#-keep public class com.google.android.gms.ads.** {
#   public *;
#}
#
#-keep public class com.google.ads.** {
#   public *;
#}
#
## ensure that the appcompat libraries are in the Proguard exclusion list
#-keep class android.support.v4.app.** { *; }
#-keep class android.support.v4.view.** { *; }
#-keep class android.support.v4.widget.** { *; }
#-keep interface android.support.v4.widget.** { *; }
#
#-keep interface android.support.v4.app.** { *; }
#-keep interface android.support.v4.view.** { *; }
#
#-keep class android.support.v7.app.** { *; }
#-keep interface android.support.v7.app.** { *; }
#-keep class android.support.v7.widget.** { *; }
#-keep interface android.support.v7.widget.** { *; }
#
#-keep class com.jcraft.jsch.** { *; }
#
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.preference.Preference
#-keep public class com.android.vending.billing.IInAppBillingService
#-keep public class * extends android.view.View {
#    public <init>(android.content.Context);
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet);
#}
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}
#-keepclassmembers class * extends android.content.Context {
#    public void *(android.view.View);
#    public void *(android.view.MenuItem);
#}
#
#-keep class com.android.common.view.** { *; }
#-keep class com.github.curioustechizen.ago.** { *; }
#
## Add the gson class
#-keep class com.google.gson.** { *; }
#
#-keepattributes Signature
#-keep class sun.misc.Unsafe { *; }
#
#-keep class org.rm3l.router_companion.firmwares.AbstractRouterFirmwareConnector {
#    *;
#}
#
#-keep class org.rm3l.router_companion.firmwares.impl.** {
#    *;
#}
#
#-keep class org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener {
#    *;
#}
#
#-keep class org.rm3l.router_companion.resources.** {
#    *;
#}
#-keep class org.rm3l.router_companion.mgmt.adapters.RouterListRecycleViewAdapter {
#    *;
#}
#
## Keep inner private classes
#-keep public class android.widget.Filter$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.DDWRTApplication$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.main.DDWRTMainActivity$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.mgmt.RouterManagementActivity$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.mgmt.adapters.RouterListRecycleViewAdapter$* {
#    *;
#}
#-keep public class com.android.common.view.SlidingTabLayout$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.fragments.PageSlidingTabStripFragment$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.service.BackgroundService$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.tiles.admin.nvram.NVRAMDataRecyclerViewAdapter$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.tiles.status.wan.WANMonthlyTrafficTile$* {
#    *;
#}
#-keep public class org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile$* {
#    *;
#}
#
#-keep public class org.rm3l.router_companion.tiles.status.wireless.ActiveIPConnectionsDetailActivity$* {
#    *;
#}
#
#-keep public class org.jsoup.** {
#    public *;
#}
#
#-dontwarn **CanvasView
#-keep public class * extends com.samsung.** { *; }
#-keep public class com.samsung.** {
#    public *;
#}
#
#-keep public class * extends android.view.View {
#    public void *(android.content.Context);
#    public void *(android.content.Context, android.util.AttributeSet);
#    public void *(android.content.Context, android.util.AttributeSet, int);
#    public void set*(...);
#}
#
#-dontwarn com.squareup.okhttp.**
#
#-keep class com.crashlytics.** { *; }
#-keep class com.crashlytics.android.**
#
#-keep class com.airbnb.deeplinkdispatch.** { *; }
#-keepclasseswithmembers class * {
#     @com.airbnb.deeplinkdispatch.DeepLink <methods>;
#}
#
#-dontwarn okio.**
#
##Wizardroid
#-keepnames class * { @org.codepond.android.wizardroid.ContextVariable *;}
#
##Otto EventBus
#-keepattributes *Annotation*
#-keepclassmembers class ** {
#    @com.squareup.otto.Subscribe public *;
#    @com.squareup.otto.Produce public *;
#}
#-dontwarn com.squareup.otto.*
#
## LeakCanary
#-keep class org.eclipse.mat.** { *; }
#-keep class com.squareup.leakcanary.** { *; }
#-dontwarn com.squareup.leakcanary.**
#
##Retrofit (from Square)
#-dontwarn retrofit2.**
#-keep class retrofit2.** { *; }
#-keepattributes Signature
#-keepattributes Exceptions
#
## Configuration for Guava 18.0
##
## disagrees with instructions provided by Guava project: https://code.google.com/p/guava-libraries/wiki/UsingProGuardWithGuava
#
#-dontwarn com.google.errorprone.annotations.**
#-dontwarn om.google.j2objc.annotations.**
#
#-dontwarn com.google.common.collect.**
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
#-dontwarn com.google.common.collect.MinMaxPriorityQueue
#
#-keepclasseswithmembers public class * {
#    public static void main(java.lang.String[]);
#}
#
## http://stackoverflow.com/questions/9120338/proguard-configuration-for-guava-with-obfuscation-and-optimization
#-dontwarn javax.annotation.**
#-dontwarn javax.inject.**
#-dontwarn sun.misc.Unsafe
#-dontwarn com.google.common.collect.MinMaxPriorityQueue
#-dontwarn com.google.common.util.concurrent.FuturesGetChecked**
#-dontwarn javax.lang.model.element.Modifier
#-dontwarn afu.org.checkerframework.**
#-dontwarn org.checkerframework.**
#
### Retrolambda
##-dontwarn java.lang.invoke.*
#
## AWS SDK
## Class names are needed in reflection
#-keepnames class com.amazonaws.**
## Request handlers defined in request.handlers
#-keep class com.amazonaws.services.**.*Handler
## The following are referenced but aren't required to run
#-dontwarn com.fasterxml.jackson.**
#-dontwarn org.apache.commons.logging.**
## Android 6.0 release removes support for the Apache HTTP client
#-dontwarn org.apache.http.**
## The SDK has several references of Apache HTTP client
#-dontwarn com.amazonaws.http.**
#-dontwarn com.amazonaws.metrics.**
#
## Welcome-Android
#-keepclassmembers class * extends com.stephentuso.welcome.ui.WelcomeActivity {
#    public static java.lang.String welcomeKey();
#}
#
#
#-keep class me.panavtec.drawableview.** { *; }
#-keep class me.panavtec.drawableview.gestures.** { *; }
#-dontwarn me.panavtec.drawableview.internal.**
#
##kryo
##-dontwarn sun.reflect.**
##-dontwarn java.beans.**
##-keep,allowshrinking class com.esotericsoftware.** {
##   <fields>;
##   <methods>;
##}
##-keep,allowshrinking class java.beans.** { *; }
##-keep,allowshrinking class sun.reflect.** { *; }
##-keep,allowshrinking class com.esotericsoftware.kryo.** { *; }
##-keep,allowshrinking class com.esotericsoftware.kryo.io.** { *; }
##-keep,allowshrinking class sun.nio.ch.** { *; }
##-dontwarn sun.nio.ch.**
##-dontwarn sun.misc.**
#
#-dontwarn java.awt.**
#-dontwarn javax.swing.**
#-dontwarn javax.jms.**
#-dontwarn javax.naming.**
#-dontwarn javax.mail.**
#-dontwarn java.beans.**
#-dontwarn java.management.**
#
#-dontwarn org.apache.log4j.**
#
#### Kotlin ###
#-dontwarn kotlin.**
#-keepclassmembers class **$WhenMappings {
#    <fields>;
#}
#-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
#}
##-keep class kotlin.reflect.jvm.internal.**
##-keep class kotlin.internal.**
#
#-keep class org.rm3l.maoni.common.model.** { *; }
#
##-keepattributes Signature
##-keep class sun.misc.Unsafe { *; }
##-keep class * implements java.io.Serializable { *; }
#
#-keep class khttp.**
#-dontwarn java.nio.**
#
#-dontwarn org.json.**
#-dontwarn java.util.Base64
#-dontwarn java.util.Base64$Encoder
#
## In https://github.com/ckurtm/FabButton/blob/master/fabbutton/src/main/java/mbanje/kurt/fabbutton/FabButton.java ,
## R.anim.design_fab_in is used only for SDK >= 14, and we are using a minSDK of 15
#-dontwarn mbanje.kurt.fabbutton.FabButton$Behavior
#
## osmdroid
#-dontwarn org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
#
## MaterialDrawer
#-dontwarn com.mikepenz.iconics.**
#
##aboutlibraries (with auto-detection)
##-keep class .R
##-keep class **.R$* {
##    <fields>;
##}
#-keepclasseswithmembers class org.rm3l.ddwrt.R$* {
#    public static final int define_*;
#}
#-keepattributes InnerClasses
#-keep class org.rm3l.ddwrt.R
#-keep class org.rm3l.ddwrt.R$* {
#    <fields>;
#}
#
## Evernote Android Job
#-dontwarn com.evernote.android.job.gcm.**
#-dontwarn com.evernote.android.job.util.GcmAvailableHelper
#-keep public class com.evernote.android.job.v21.PlatformJobService
#-keep public class com.evernote.android.job.v14.PlatformAlarmService
#-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
#-keep public class com.evernote.android.job.JobBootReceiver
#-keep public class com.evernote.android.job.JobRescheduleService
#
#-dontwarn com.android.support.**
#-dontwarn org.jetbrains.anko.appcompat.v7.**
#
## OkHttp
#-keepattributes Signature
#-keepattributes *Annotation*
#-keep class okhttp3.** { *; }
#-keep interface okhttp3.** { *; }
#-dontwarn okhttp3.**
#
#-keep class org.rm3l.ddwrt.BuildConfig
#
## For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-keep class android.graphics.** { *; }
#-dontwarn android.graphics.**
#
##androidx
#-dontwarn com.google.android.material.**
#-keep class com.google.android.material.** { *; }
#
#-dontwarn androidx.**
#-keep class androidx.** { *; }
#-keep interface androidx.** { *; }