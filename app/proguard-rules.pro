#
# DD-WRT Companion is a mobile app that lets you connect to,
# monitor and manage your DD-WRT routers on the go.
#
# Copyright (C) 2014  Armel Soro
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
# Contact Info: Armel Soro &lt;apps+ddwrt AT rm3l DOT org&gt;
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
-dontwarn javax.annotation.**
-dontwarn org.ietf.jgss.**
-dontwarn com.jcraft.jzlib.**
-dontwarn sun.misc.**
#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

# ACRA needs "annotations" so add this...
# Note: This may already be defined in the default "proguard-android-optimize.txt"
# file in the SDK. If it is, then you don't need to duplicate it. See your
# "project.properties" file to get the path to the default "proguard-android-optimize.txt".
-keepattributes *Annotation*
# Keep all the ACRA classes
-keep class org.acra.** { *; }

-keep public class com.google.android.gms.ads.** {
   public *;
}

-keep public class com.google.ads.** {
   public *;
}

# ensure that the appcompat libraries are in the Proguard exclusion list
-keep class android.support.v4.app.** { *; }
-keep class android.support.v4.view.** { *; }
-keep class android.support.v4.widget.** { *; }

-keep interface android.support.v4.app.** { *; }
-keep interface android.support.v4.view.** { *; }

-keep class android.support.v7.app.** { *; }
-keep interface android.support.v7.app.** { *; }
-keep class android.support.v7.widget.** { *; }

-keep class com.jcraft.jsch.**

