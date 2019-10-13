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
package org.rm3l.router_companion.tiles.status.router;

import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.PermissionChecker;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.common.base.Joiner;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

public class RouterCpuInfoActivity extends AppCompatActivity {

    public static final String CPU_INFO_OUTPUT = "CPU_INFO_OUTPUT";

    private static final String LOG_TAG = RouterCpuInfoActivity.class.getSimpleName();

    private String mCpuInfoMultiLine;

    private File mFileToShare;

    private Router mRouter;

    private String mRouterUuid;

    private ShareActionProvider mShareActionProvider;

    private Toolbar mToolbar;

    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        mRouter = RouterManagementActivity.Companion.getDao(this).getRouter(mRouterUuid);
        if (mRouter == null) {
            Toast.makeText(this, "Whoops - Router not found. Has it been deleted?", Toast.LENGTH_SHORT)
                    .show();
            Utils.reportException(null, new IllegalStateException("Router not found"));
            finish();
            return;
        }

        ColorUtils.Companion.setAppTheme(this, mRouter.getRouterFirmware(), false);

        final boolean themeLight = ColorUtils.Companion.isThemeLight(this);
        //        if (themeLight) {
        //            //Light
        //            setTheme(R.style.AppThemeLight);
        ////            getWindow().getDecorView()
        ////                    .setBackgroundColor(ContextCompat.getColor(this,
        ////                            android.R.color.white));
        //        } else {
        //            //Default is Dark
        //            setTheme(R.style.AppThemeDark);
        //        }

        setContentView(R.layout.tile_status_router_cpuinfo);

        mToolbar = (Toolbar) findViewById(R.id.tile_status_router_cpuinfo_view_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("CPU Information");
            mToolbar.setSubtitle(
                    String.format("%s (%s:%d)", mRouter.getDisplayName(), mRouter.getRemoteIpAddress(),
                            mRouter.getRemotePort()));
            mToolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            mToolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            mToolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            setSupportActionBar(mToolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final String[] mCpuInfo = intent.getStringArrayExtra(CPU_INFO_OUTPUT);

        if (mCpuInfo == null || mCpuInfo.length == 0) {
            Toast.makeText(this, "Internal Error - No CPU Info available!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mCpuInfoMultiLine = Joiner.on("\n").join(mCpuInfo);

        ((TextView) findViewById(R.id.tile_status_router_cpuinfo)).setText(mCpuInfoMultiLine);
    }

    @Override
    protected void onDestroy() {
        if (mFileToShare != null) {
            //noinspection ResultOfMethodCallIgnored
            mFileToShare.delete();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {

        switch (requestCode) {
            case RouterCompanionAppConstants.Permissions.STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_router_cpuinfo_share);
                        menuItem.setEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
                    Utils.displayMessage(this, "Sharing of Router CPU Data will be unavailable", Style.INFO);
                    if (optionsMenu != null) {
                        final MenuItem menuItem = optionsMenu.findItem(R.id.tile_status_router_cpuinfo_share);
                        menuItem.setEnabled(false);
                    }
                }
                return;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_status_router_cpuinfo_options, menu);

        this.optionsMenu = menu;

        //Permission requests
        final int rwExternalStoragePermissionCheck =
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rwExternalStoragePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(this, "Storage access is required to share Router CPU Data.",
                        "OK", Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                                    throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(RouterCpuInfoActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        RouterCompanionAppConstants.Permissions.STORAGE);
                            }
                        }, null, true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RouterCompanionAppConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        final MenuItem shareMenuItem = menu.findItem(R.id.tile_status_router_cpuinfo_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
        if (mShareActionProvider == null) {
            mShareActionProvider = new ShareActionProvider(this);
            MenuItemCompat.setActionProvider(shareMenuItem, mShareActionProvider);
        }

        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            mFileToShare = new File(getCacheDir(),
                    Utils.getEscapedFileName(String.format("CPU_Info__%s", nullToEmpty(mRouterUuid)))
                            + ".txt");

            Exception exception = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
                //noinspection ConstantConditions
                outputStream.write(mCpuInfoMultiLine.getBytes());
            } catch (IOException e) {
                exception = e;
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (exception != null) {
                Utils.displayMessage(this, "Error while trying to share CPU Info - please try again later",
                        Style.ALERT);
                return true;
            }

            setShareFile(mFileToShare);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == R.id.action_feedback) {
            Utils.openFeedbackForm(this, mRouter);
            //                final Intent intent = new Intent(RouterCpuInfoActivity.this, FeedbackActivity.class);
            //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
            //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
            //                ViewGroupUtils.exportViewToFile(RouterCpuInfoActivity.this, getWindow().getDecorView(), screenshotFile);
            //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
            //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
            //                startActivity(intent);
            ////                Utils.buildFeedbackDialog(this, true);
            return true;
        } else {
        }
        return super.onOptionsItemSelected(item);
    }

    private void setShareFile(File file) {
        if (mShareActionProvider == null) {
            return;
        }

        final Uri uriForFile =
                FileProvider.getUriForFile(this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, file);

        mShareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                        grantUriPermission(intent.getComponent().getPackageName(), uriForFile,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        return true;
                    }
                });

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);

        //sendIntent.setType("text/plain");
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                String.format("CPU Info for Router '%s'", mRouter.getCanonicalHumanReadableName()));

        sendIntent.putExtra(Intent.EXTRA_TEXT, fromHtml(
                String.format("%s%s", mCpuInfoMultiLine, Utils.getShareIntentFooter())
                        .replaceAll("\n", "<br/>")));

        sendIntent.setData(uriForFile);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setShareIntent(sendIntent);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}
