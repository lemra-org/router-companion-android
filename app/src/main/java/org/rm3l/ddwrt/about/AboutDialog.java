/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    private static final String ABOUT_TITLE = "About";

    private final Context mContext;

    /**
     * Constructor
     *
     * @param context the activity context
     */
    public AboutDialog(@NotNull final Context context) {
        super(context);
        mContext = context;
        super.setTitle(ABOUT_TITLE);
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
        Linkify.addLinks(textView, Linkify.ALL);
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

        TextView tv = (TextView) findViewById(R.id.legal_text);
        final String legalTxtRaw = readRawTextFile(R.raw.legal);
        boolean fileFound = (legalTxtRaw != null);
        if (fileFound) {
            setTextContentAndLinkify(tv, legalTxtRaw);
        }
        tv.setVisibility(fileFound ? View.VISIBLE : View.GONE);

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
