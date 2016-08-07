package org.rm3l.ddwrt.tasker.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import net.jcip.annotations.NotThreadSafe;

import org.rm3l.ddwrt.common.IRouterService;
import org.rm3l.ddwrt.tasker.Constants;
import org.rm3l.ddwrt.tasker.R;
import org.rm3l.ddwrt.tasker.bundle.PluginBundleValues;
import org.rm3l.ddwrt.tasker.feedback.maoni.FeedbackHandler;
import org.rm3l.ddwrt.tasker.utils.Utils;
import org.rm3l.maoni.Maoni;

@NotThreadSafe
public class EditActivity extends AbstractAppCompatPluginActivity {

    /** Service to which this client will bind */
    private IRouterService routerService;

    /** Connection to the service (inner class) */
    private RouterServiceConnection conn;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final PackageManager packageManager = getPackageManager();

        // connect to the service
        conn = new RouterServiceConnection();
        // name must match the service's Intent filter in the Service Manifest file
        final Intent intent = new Intent("org.rm3l.ddwrt.IRouterService");
        String ddwrtCompanionAppPackage;
        if (Utils.isPackageInstalled("org.rm3l.ddwrt", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.amzn.underground", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.amzn.underground";
        } else if (Utils.isPackageInstalled("org.rm3l.ddwrt.lite", packageManager)) {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt.lite";
        } else {
            ddwrtCompanionAppPackage = "org.rm3l.ddwrt";
        }
        Crashlytics.log(Log.DEBUG, Constants.TAG,
                "ddwrtCompanionAppPackage=" + ddwrtCompanionAppPackage);

        intent.setPackage(ddwrtCompanionAppPackage);
        // bind to the Service, create it if it's not already there
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setTitle(callingApplicationLabel != null ? callingApplicationLabel : null);
            toolbar.setSubtitle(R.string.plugin_name);
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {
        final String message = PluginBundleValues.getMessage(previousBundle);
        ((EditText) findViewById(android.R.id.text1)).setText(message);
    }

    @Override
    public boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        Bundle result = null;

        final String message = ((EditText) findViewById(android.R.id.text1)).getText().toString();
        if (!TextUtils.isEmpty(message)) {
            result = PluginBundleValues.generateBundle(getApplicationContext(), message);
        }

        return result;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull final Bundle bundle) {
        final String message = PluginBundleValues.getMessage(bundle);

        final int maxBlurbLength = getResources().getInteger(
                R.integer.com_twofortyfouram_locale_sdk_client_maximum_blurb_length);

        if (message.length() > maxBlurbLength) {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.ddwrt_companion_tasker_feedback:
                new Maoni.Builder(Constants.FILEPROVIDER_AUTHORITY)
                        .withTheme(R.style.AppThemeLight_StatusBarTransparent)
                        .withWindowTitle("Send Feedback")
                        .withExtraLayout(R.layout.activity_feedback_maoni)
                        .withHandler(new FeedbackHandler(this))
                        .build()
                        .start(this);
                break;
            case R.id.ddwrt_companion_tasker_about:
                //TODO About
                Toast.makeText(this,
                        "[TODO] About", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_discard_changes:
                // Signal to AbstractAppCompatPluginActivity that the user canceled.
                mIsCancelled = true;
                finish();
                break;
            default:
                break;
        }

        return true;
    }

    /** Clean up before Activity is destroyed */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        routerService = null;
    }

    /** Inner class used to connect to UserDataService */
    class RouterServiceConnection implements ServiceConnection {

        /** is called once the bind succeeds */
        public void onServiceConnected(ComponentName name, IBinder service) {
            Crashlytics.log(Log.DEBUG, Constants.TAG, "Service connected");
            routerService = IRouterService.Stub.asInterface(service);

            //Test
            try {
                Crashlytics.log(Log.DEBUG, Constants.TAG,
                        "remoteService#getAllRouters(): " + routerService.getAllRouters());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        /*** is called once the remote service is no longer available */
        public void onServiceDisconnected(ComponentName name) { //
            Crashlytics.log(Log.WARN, Constants.TAG, "Service has unexpectedly disconnected");
            routerService = null;
        }

    }
}