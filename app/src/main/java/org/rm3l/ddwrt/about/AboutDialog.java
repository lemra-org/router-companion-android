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

package org.rm3l.ddwrt.about;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;

import java.io.IOException;

import static android.text.util.Linkify.EMAIL_ADDRESSES;
import static android.text.util.Linkify.MAP_ADDRESSES;
import static android.text.util.Linkify.WEB_URLS;

/**
 * About Dialog: fills in the dialog with text retrieved from a given raw file
 * (some of them are parameterized)
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class AboutDialog extends Dialog {

    public static final String VERSION_CODE_INFO_TXT = "%VERSION_CODE%";
    public static final String VERSION_NAME_INFO_TXT = "%VERSION_NAME%";
    public static final String APP_NAME_INFO_TXT = "%APP_NAME%";
    private static final int[] BIT_FIELDS_TO_LINKIFY = new int[]{
            EMAIL_ADDRESSES, MAP_ADDRESSES, WEB_URLS
    };
    private final Context mContext;

    /**
     * Constructor
     *
     * @param context the activity context
     */
    public AboutDialog(@NotNull final Context context) {
        super(context);
        mContext = context;
        super.setTitle(mContext.getString(R.string.menuitem_about));
    }

    /**
     * Format text view links
     *
     * @param textView the text view
     * @param text     the text to set as content
     */
    private static void setTextContentAndLinkify(@NotNull final TextView textView, @NotNull final String text) {
        textView.setText(Html.fromHtml(text));
        textView.setLinkTextColor(Color.WHITE);
        for (final int bitFieldToLinkify : BIT_FIELDS_TO_LINKIFY) {
            Linkify.addLinks(textView, bitFieldToLinkify);
        }
    }

    /**
     * Get raw text file content
     *
     * @param id the raw file id in the context
     * @return the raw file contents
     */
    @Nullable
    private String readRawTextFile(int id) {
        try {
            return IOUtils.toString(mContext.getResources().openRawResource(id));
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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        boolean fileFound;
        TextView tv = (TextView) findViewById(R.id.legal_text);
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

        tv = (TextView) findViewById(R.id.info_text);
        final String infoText = readRawTextFile(R.raw.info);
        fileFound = (infoText != null);
        if (fileFound) {
            setTextContentAndLinkify(tv,
                    infoText
                            .replaceAll(APP_NAME_INFO_TXT, mContext.getString(mContext.getApplicationInfo().labelRes))
                            .replaceAll(VERSION_CODE_INFO_TXT, String.valueOf(BuildConfig.VERSION_CODE))
                            .replaceAll(VERSION_NAME_INFO_TXT, BuildConfig.VERSION_NAME));
        }
        tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);

        tv = (TextView) findViewById(R.id.contributors);
        final String contributorsTxt = readRawTextFile(R.raw.contributors);
        fileFound = (contributorsTxt != null);
        if (fileFound) {
            setTextContentAndLinkify(tv, contributorsTxt);
        }
        tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);
    }

}
