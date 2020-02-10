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

import android.Manifest.permission;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.common.base.Joiner;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

public class RouterCpuInfoActivity extends AppCompatActivity {

  public static final String CPU_INFO_OUTPUT = "CPU_INFO_OUTPUT";

  private String mCpuInfoMultiLine;

  private File mFileToShare;

  private Router mRouter;

  private String mRouterUuid;

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

    setContentView(R.layout.tile_status_router_cpuinfo);

    final Toolbar toolbar = findViewById(R.id.tile_status_router_cpuinfo_view_toolbar);
    if (toolbar != null) {
      toolbar.setTitle("CPU Information");
      toolbar.setSubtitle(
          String.format(
              "%s (%s:%d)",
              mRouter.getDisplayName(), mRouter.getRemoteIpAddress(), mRouter.getRemotePort()));
      toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
      toolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
      toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
      toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
      setSupportActionBar(toolbar);
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
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.tile_status_router_cpuinfo_options, menu);
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
      return true;
    } else if (i == R.id.tile_status_router_cpuinfo_share) {
      PermissionsUtils.requestPermissions(
          this,
          Collections.singletonList(permission.WRITE_EXTERNAL_STORAGE),
          () -> {
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            mFileToShare =
                new File(
                    getCacheDir(),
                    Utils.getEscapedFileName(
                            String.format("CPU_Info__%s", nullToEmpty(mRouterUuid)))
                        + ".txt");

            Exception exception = null;
            OutputStream outputStream = null;
            try {
              outputStream = new BufferedOutputStream(new FileOutputStream(mFileToShare, false));
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
              Utils.displayMessage(
                  this,
                  "Error while trying to share CPU Info - please try again later",
                  Style.ALERT);
              return null;
            }

            final Uri uriForFile =
                FileProvider.getUriForFile(
                    this, RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, mFileToShare);

            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);

            sendIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                String.format("CPU Info for Router '%s'", mRouter.getCanonicalHumanReadableName()));

            sendIntent.putExtra(
                Intent.EXTRA_TEXT,
                fromHtml(
                    String.format("%s%s", mCpuInfoMultiLine, Utils.getShareIntentFooter())
                        .replaceAll("\n", "<br/>")));

            sendIntent.setDataAndType(uriForFile, "text/html");
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(
                Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));

            return null;
          },
          () -> null,
          "Storage access is required to share Router CPU Data");
    }
    return super.onOptionsItemSelected(item);
  }
}
