package org.rm3l.ddwrt.actions.activity;

import android.content.Context;
import android.content.Intent;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTCompanionException;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.web.WebActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static org.rm3l.ddwrt.main.DDWRTMainActivity.SAVE_ROUTER_SELECTED;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.HTTPS_ENABLE;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.HTTP_LANPORT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.HTTP_PASSWD;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.HTTP_USERNAME;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.HTTP_WANPORT;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.LAN_IPADDR;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.REMOTE_MGT_HTTPS;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.WAN_IPADDR;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

/**
 * Created by rm3l on 08/03/16.
 */
public class OpenWebManagementPageActivity extends WebActivity {

    private static final String LOG_TAG = OpenWebManagementPageActivity.class.getSimpleName();

    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View mLoadingView;

    private TextView mErrorTextView;

    private AtomicReference<String> mUrl = new AtomicReference<>(null);

    private DDWRTCompanionDAO mDao;

    private Router mRouter;

    @Override
    protected CharSequence getTitleStr() {
        return "Web Management Interface";
    }

    @Override
    protected int getTitleResId() {
        return 0;
    }

    @NonNull
    @Override
    protected String getUrl() {
        return mUrl.get();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (mRouter != null) {
            savedInstanceState.putString(SAVE_ROUTER_SELECTED, mRouter.getUuid());
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDao = RouterManagementActivity.getDao(this);

        final Intent intent = getIntent();
        String uuid = intent.getStringExtra(ROUTER_SELECTED);
        if (uuid == null) {
            if (savedInstanceState != null) {
                uuid = savedInstanceState.getString(SAVE_ROUTER_SELECTED);
            }
        }
        this.mRouter = this.mDao.getRouter(uuid);
        if (this.mRouter == null) {
            Toast.makeText(this,
                    "No router set or router no longer exists", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mWebview.setVisibility(View.GONE);

        mErrorTextView = (TextView)
                findViewById(R.id.error_placeholder);

        mLoadingView = findViewById(R.id.loading_view);
        mLoadingView.setVisibility(View.VISIBLE);

        new WebManagementLoaderAsyncTask().execute();
    }

    class WebManagementLoaderAsyncTask extends AsyncTask<Void, Void,
            WebManagementLoaderAsyncTask.Result> {

        private boolean canConnect(@NonNull final String urlStr)  {
            Crashlytics.log(Log.DEBUG, LOG_TAG, "--> Trying GET '" + urlStr + "'");
            HttpURLConnection urlConnection = null;
            try {
                final URL url = new URL(urlStr);
                if (url.getProtocol().toLowerCase().equals("https")) {
                    trustAllHosts();
                    final HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                    https.setHostnameVerifier(DO_NOT_VERIFY);
                    urlConnection = https;
                } else {
                    urlConnection = (HttpURLConnection) url.openConnection();
                }
                final int statusCode = urlConnection.getResponseCode();
                Crashlytics.log(Log.DEBUG, LOG_TAG, "GET " + urlStr + " : " + statusCode);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.log(Log.DEBUG, LOG_TAG, "Didn't succedd in GET'ing " + urlStr);
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected Result doInBackground(Void... params) {
            try {
                final NVRAMInfo nvRamInfoFromRouter = SSHUtils.getNVRamInfoFromRouter(
                        OpenWebManagementPageActivity.this,
                        mRouter,
                        getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                                Context.MODE_PRIVATE),
                        LAN_IPADDR,
                        WAN_IPADDR,
                        HTTP_LANPORT,
                        HTTP_WANPORT,
                        HTTP_USERNAME,
                        HTTP_PASSWD,
                        HTTPS_ENABLE,
                        REMOTE_MGT_HTTPS);

                if (nvRamInfoFromRouter == null || nvRamInfoFromRouter.isEmpty()) {
                    throw new DDWRTCompanionException(
                            "Unable to retrieve info about HTTPd service");
                }

                String lanUrl = "http";
                String wanUrl = "http";
                final String lanIpAddr = nvRamInfoFromRouter
                        .getProperty(LAN_IPADDR, EMPTY_STRING);
                final String lanPort = nvRamInfoFromRouter
                        .getProperty(HTTP_LANPORT, EMPTY_STRING);

                final String wanIpAddr = nvRamInfoFromRouter
                        .getProperty(WAN_IPADDR, EMPTY_STRING);
                final String wanPort = nvRamInfoFromRouter
                        .getProperty(HTTP_WANPORT, EMPTY_STRING);

                if ("1".equals(nvRamInfoFromRouter.getProperty(HTTPS_ENABLE))) {
                    lanUrl += "s";
                }
                if ("1".equals(nvRamInfoFromRouter.getProperty(REMOTE_MGT_HTTPS))) {
                    wanUrl += "s";
                }
                lanUrl += ("://" + lanIpAddr + (TextUtils.isEmpty(lanPort) ? "" : (":" + lanPort)));
                wanUrl += ("://" + wanIpAddr + (TextUtils.isEmpty(wanPort) ? "" : (":" + wanPort)));

                if (canConnect(lanUrl)) {
                    mUrl.set(lanUrl);
                } else if (canConnect(wanUrl)) {
                    mUrl.set(wanUrl);
                } else {
                    //Try with router IP / DNS
                    String urlFromRouterRemoteIpOrDns = "http";
                    final String remoteIpAddress = mRouter.getRemoteIpAddress();
                    if ("1".equals(nvRamInfoFromRouter.getProperty(HTTPS_ENABLE))) {
                        urlFromRouterRemoteIpOrDns += "s";
                    }
                    urlFromRouterRemoteIpOrDns +=
                            ("://" + remoteIpAddress + (TextUtils.isEmpty(lanPort) ? "" : (":" + lanPort)));
                    if (canConnect(urlFromRouterRemoteIpOrDns)) {
                        mUrl.set(urlFromRouterRemoteIpOrDns);
                    } else {
                        //WAN
                        urlFromRouterRemoteIpOrDns = "http";
                        if ("1".equals(nvRamInfoFromRouter.getProperty(REMOTE_MGT_HTTPS))) {
                            urlFromRouterRemoteIpOrDns += "s";
                        }
                        urlFromRouterRemoteIpOrDns +=
                                ("://" + remoteIpAddress + (TextUtils.isEmpty(wanPort) ? "" : (":" + wanPort)));
                        if (canConnect(urlFromRouterRemoteIpOrDns)) {
                            mUrl.set(urlFromRouterRemoteIpOrDns);
                        } else {
                            //TODO Maybe display dialog where user can explicitly provide the information
                            throw new DDWRTCompanionException("Could not connect to router");
                        }
                    }
                }
                return new Result(null);

            } catch (final Exception e) {
                e.printStackTrace();
                return new Result(e);
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            final Exception exception = result.getException();
            if (exception != null) {
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                final Throwable rootCause = Throwables.getRootCause(exception);
                mErrorTextView.setVisibility(View.VISIBLE);
                mErrorTextView.setText("Error: " +
                        ExceptionUtils.getRootCauseMessage(exception));
                mErrorTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(OpenWebManagementPageActivity.this,
                                    rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                mWebview.setVisibility(View.GONE);
            } else {
                mWebview.setVisibility(View.VISIBLE);
                mErrorTextView.setVisibility(View.GONE);
                final WebSettings webviewSettings = mWebview.getSettings();
                webviewSettings.setJavaScriptEnabled(true);
                webviewSettings.setDomStorageEnabled(true);

                mWebview.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Utils.reportException(null, new WebException(("Error: " + failingUrl + " - errorCode=" + errorCode +
                                ": " + description)));
                        Toast.makeText(OpenWebManagementPageActivity.this,
                                "Oh no! " + description, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        //Case of self-signed certificates
                        handler.proceed();
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        //Fixes issue https://fabric.io/lemra-inc2/android/apps/org.rm3l.ddwrt/issues/568283d6f5d3a7f76bb8f2dc
                        if (url == null) {
                            return false;
                        }
                        return false;
//                        if (Uri.parse(url).getHost().endsWith("rm3l.org")) {
//                            // This is my web site, so do not override; let my WebView load the page
//                            return false;
//                        } else if (url.startsWith("mailto:")){
//                            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
//                            return true;
//                        }
//                        // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
//                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                        startActivity(intent);
//                        return true;
                    }
                });

                mWebview.loadUrl(mUrl.get());
            }
            mLoadingView.setVisibility(View.GONE);
        }

        class Result {
            private final Exception exception;

            public Result(Exception exception) {
                this.exception = exception;
            }

            public Exception getException() {
                return exception;
            }
        }
    }
}
