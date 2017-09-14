/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.router_companion.about;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Calendar;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * About Dialog: fills in the dialog with text retrieved from a given raw file
 * (some of them are parameterized)
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
@Deprecated
public class AboutDialog extends Dialog {

  public static final String CURRENT_YEAR_INFO_TXT = "%CURRENT_YEAR%";
  public static final String VERSION_CODE_INFO_TXT = "%VERSION_CODE%";
  public static final String VERSION_NAME_INFO_TXT = "%VERSION_NAME%";
  public static final String APP_NAME_INFO_TXT = "%APP_NAME%";
  public static final String DONATIONS_LIB_INFO_TXT = "%DONATIONS_LIB%";
  public static final String SUPPORT_WEBSITE_HREF = "%SUPPORT_WEBSITE_HREF%";

  private final Context mContext;

  private boolean isThemeLight;
  private AlertDialog mOssLicensesAlertDialog;

  /**
   * Constructor
   *
   * @param context the activity context
   */
  public AboutDialog(@NonNull final Context context) {
    super(context);
    mContext = context;
    super.setTitle(mContext.getString(R.string.menuitem_about));
    isThemeLight = ColorUtils.Companion.isThemeLight(mContext);
  }

  /**
   * Format text view links
   *
   * @param textView the text view
   * @param text the text to set as content
   */
  private void setTextContentAndLinkify(@NonNull final TextView textView,
      @NonNull final String text) {
    textView.setLinkTextColor(
        ContextCompat.getColor(mContext, isThemeLight ? R.color.ddwrt_green : R.color.win8_lime));
    textView.setLinksClickable(true);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
    textView.setText(Utils.linkifyHtml(text, Linkify.ALL));
  }

  /**
   * Get raw text file content
   *
   * @param id the raw file id in the context
   * @return the raw file contents
   */
  @Nullable private String readRawTextFile(int id) {
    try {
      return new String(ByteStreams.toByteArray(mContext.getResources().openRawResource(id)));
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Called when the activity is first created.
   * <p/>
   * Fills the dialog content
   */
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.about);

    boolean fileFound;
    TextView tv = findViewById(R.id.legal_text);
    if (BuildConfig.DONATIONS) {
      //Show GPL License terms
      final String legalTxtRaw = readRawTextFile(R.raw.legal);
      fileFound = (legalTxtRaw != null);
      if (fileFound) {
        setTextContentAndLinkify(tv, legalTxtRaw);
      }
      tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);
    } else {
      tv.setVisibility(View.GONE);
    }

    tv = findViewById(R.id.info_text);
    final String infoText = readRawTextFile(R.raw.info);
    fileFound = (infoText != null);
    if (fileFound) {
      setTextContentAndLinkify(tv, infoText.replaceAll(CURRENT_YEAR_INFO_TXT,
          String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
          .replaceAll(APP_NAME_INFO_TXT, mContext.getString(mContext.getApplicationInfo().labelRes))
          .replaceAll(VERSION_CODE_INFO_TXT, String.valueOf(BuildConfig.VERSION_CODE))
          .replaceAll(VERSION_NAME_INFO_TXT, BuildConfig.VERSION_NAME)
          .replaceAll(SUPPORT_WEBSITE_HREF, RouterCompanionAppConstants.SUPPORT_WEBSITE));
    }
    tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);

    tv = findViewById(R.id.contributors);
    final String contributorsTxt = readRawTextFile(R.raw.contributors);
    fileFound = (contributorsTxt != null);
    if (fileFound) {
      //noinspection ConstantConditions
      setTextContentAndLinkify(tv, contributorsTxt.replaceAll(DONATIONS_LIB_INFO_TXT,
          BuildConfig.DONATIONS ? "&#8226; <a href=\"https://github.com/dschuermann/"
              + "android-donations-lib\" target=\"_blank\">android-donations-lib</a>" : ""));
    }
    tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);

    findViewById(R.id.oss_licenses).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final Context context = AboutDialog.this.getContext();
        final WebView view =
            (WebView) LayoutInflater.from(context).inflate(R.layout.dialog_licenses, null);
        view.setBackgroundColor(
            ContextCompat.getColor(mContext, isThemeLight ? R.color.white : R.color.black));
        view.loadUrl(String.format("file:///android_asset/open_source_licenses_%s.html",
            isThemeLight ? "light" : "dark"));
        mOssLicensesAlertDialog = new AlertDialog.Builder(context).setCancelable(true)
            .setTitle("Open Source Licenses")
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create();
        mOssLicensesAlertDialog.setCanceledOnTouchOutside(true);
        mOssLicensesAlertDialog.show();
      }
    });
  }
}
