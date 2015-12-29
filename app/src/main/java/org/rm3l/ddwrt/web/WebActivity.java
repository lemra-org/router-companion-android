package org.rm3l.ddwrt.web;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

/**
 * Created by rm3l on 04/07/15.
 */
public abstract class WebActivity extends AppCompatActivity {

    protected Toolbar mToolbar;

    protected WebView mWebview;

    @Nullable
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Let's display the progress in the activity title bar, like the
        // browser app does.
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);

        final boolean themeLight = ColorUtils.isThemeLight(this);
        if (themeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.activity_web);

        mInterstitialAd = AdUtils.requestNewInterstitial(this,
                R.string.interstitial_ad_unit_id_web_exit);

        AdUtils.buildAndDisplayAdViewIfNeeded(this,
                (AdView) findViewById(R.id.web_adView));

        if (themeLight) {
            final Resources resources = getResources();
            getWindow().getDecorView()
                    .setBackgroundColor(
                            ContextCompat.getColor(this,
                                    android.R.color.white));
        }

        mToolbar = (Toolbar) findViewById(R.id.web_toolbar);
        if (mToolbar != null) {
            final int titleResId = this.getTitleResId();
            if (titleResId > 0) {
                mToolbar.setTitle(titleResId);
            } else {
                mToolbar.setTitle(this.getTitleStr());
            }
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mWebview = (WebView) findViewById(R.id.web_webview);

        mWebview.getSettings().setJavaScriptEnabled(true);

        final Activity activity = this;
        mWebview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                activity.setProgress(progress * 1000);
            }
        });
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Utils.reportException(null, new WebException(("Error: " + failingUrl + " - errorCode=" + errorCode +
                        ": " + description)));
                Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //Fixes issue https://fabric.io/lemra-inc2/android/apps/org.rm3l.ddwrt/issues/568283d6f5d3a7f76bb8f2dc
                if (url == null) {
                    return false;
                }
                if (Uri.parse(url).getHost().endsWith("rm3l.org")) {
                    // This is my web site, so do not override; let my WebView load the page
                    return false;
                } else if (url.startsWith("mailto:")){
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                }
                // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        });

        mWebview.loadUrl(this.getUrl());
    }

    protected abstract CharSequence getTitleStr();

    protected abstract int getTitleResId();

    @NonNull
    protected abstract String getUrl();

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) &&
                mWebview != null &&
                mWebview.canGoBack()) {
            mWebview.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {

        if (BuildConfig.WITH_ADS &&
                mInterstitialAd != null && AdUtils.canDisplayInterstialAd(this)) {

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    WebActivity.super.finish();
                }

                @Override
                public void onAdOpened() {
                    //Save preference
                    getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE)
                            .edit()
                            .putLong(
                                    DDWRTCompanionConstants.AD_LAST_INTERSTITIAL_PREF,
                                    System.currentTimeMillis())
                            .apply();
                }
            });

            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                WebActivity.super.finish();
            }

        } else {
            super.finish();
        }

    }

    class WebException extends DDWRTCompanionException {

        public WebException(@Nullable String detailMessage) {
            super(detailMessage);
        }

    }
}