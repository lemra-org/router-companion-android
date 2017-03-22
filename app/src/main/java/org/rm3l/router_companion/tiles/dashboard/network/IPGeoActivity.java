package org.rm3l.router_companion.tiles.dashboard.network;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import de.keyboardsurfer.android.widget.crouton.Style;
import java.io.File;
import java.util.ArrayList;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.exceptions.DDWRTCompanionException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.IPWhoisInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.wireless.ActiveIPConnectionsDetailActivity;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;
import org.rm3l.router_companion.widgets.map.MyOwnItemizedOverlay;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Created by rm3l on 02/01/16.
 */
public class IPGeoActivity extends AppCompatActivity {

  public static final String PUBLIC_IP_TO_DISPLAY = "PUBLIC_IP_TO_DISPLAY";
  private static final String LOG_TAG = IPGeoActivity.class.getSimpleName();
  private File mFileToShare;
  private ShareActionProvider mShareActionProvider;
  private Menu optionsMenu;

  private String mRouterUuid;

  @Nullable private InterstitialAd mInterstitialAd;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();

    final String publicIpToGeolocate = intent.getStringExtra(PUBLIC_IP_TO_DISPLAY);

    if (isNullOrEmpty(publicIpToGeolocate)) {
      Toast.makeText(this, "Don't know what to geo-locate - please try again later.",
          Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

    final Router router = RouterManagementActivity.getDao(this).getRouter(mRouterUuid);
    ColorUtils.setAppTheme(this, router != null ? router.getRouterFirmware() : null, false);

    final boolean themeLight = ColorUtils.isThemeLight(this);
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

    setContentView(R.layout.activity_ip_geo);

    mInterstitialAd = AdUtils.requestNewInterstitial(this,
        R.string.interstitial_ad_unit_id_wireless_network_generate_qr_code);

    AdUtils.buildAndDisplayAdViewIfNeeded(this, (AdView) findViewById(R.id.activity_ip_geo_adView));

    final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_ip_geo_toolbar);
    if (toolbar != null) {
      toolbar.setTitle("IP Geolocation");
      toolbar.setSubtitle(publicIpToGeolocate);
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
        SnackbarUtils.buildSnackbar(this,
            "Storage access is required to display and cache map tiles.", "OK",
            Snackbar.LENGTH_INDEFINITE, new SnackbarCallback() {
              @Override public void onShowEvent(@Nullable Bundle bundle) throws Exception {

              }

              @Override public void onDismissEventSwipe(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventActionClick(int event, @Nullable Bundle bundle)
                  throws Exception {
                //Request permission
                ActivityCompat.requestPermissions(IPGeoActivity.this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    RouterCompanionAppConstants.Permissions.STORAGE);
              }

              @Override public void onDismissEventTimeout(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventManual(int event, @Nullable Bundle bundle)
                  throws Exception {

              }

              @Override public void onDismissEventConsecutive(int event, @Nullable Bundle bundle)
                  throws Exception {

              }
            }, null, true);
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this,
            new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
            RouterCompanionAppConstants.Permissions.STORAGE);
        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
        // app-defined int constant. The callback method gets the
        // result of the request.
      }
    }

    final TextView errorView = (TextView) findViewById(R.id.activity_ip_geo_map_error);
    final MapView map = (MapView) findViewById(R.id.activity_ip_geo_map);

    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.activity_ip_geo_map_loading);

    new AsyncTask<String, Void, Void>() {

      @Override protected Void doInBackground(String... params) {
        try {
          final IPWhoisInfo ipWhoisInfo =
              ActiveIPConnectionsDetailActivity.mIPWhoisInfoCache.get(publicIpToGeolocate);
          if (ipWhoisInfo == null) {
            throw new DDWRTCompanionException(
                "Unable to geo-locate IP Address - please try again later.");
          }
          final double latitude = Double.parseDouble(ipWhoisInfo.getLatitude());
          final double longitude = Double.parseDouble(ipWhoisInfo.getLongitude());

          runOnUiThread(new Runnable() {
            @Override public void run() {
              map.setTileSource(RouterCompanionAppConstants.TILE_SOURCE);
              map.setBuiltInZoomControls(true);
              map.setMultiTouchControls(true);

              //Night mode
              //                            map.getController().setInvertedTiles(!ColorUtils
              //                                    .isThemeLight(IPGeoActivity.this));
              map.getOverlayManager()
                  .getTilesOverlay()
                  .setColorFilter(ColorUtils.isThemeLight(IPGeoActivity.this) ? null
                      : TilesOverlay.INVERT_COLORS);

              //Act according to user-defined data usage control setting
              //map.setUseDataConnection(Utils.canUseDataConnection(this));
              map.setUseDataConnection(true);

              final IMapController mapController = map.getController();
              mapController.setZoom(11);

              final GeoPoint publicIpPoint = new GeoPoint(latitude, longitude);
              final OverlayItem overlayItem = new OverlayItem(ipWhoisInfo.getIp(), String.format(
                  "- Prefix: %s\n"
                      + "- Country: %s (%s)\n"
                      + "- Region: %s\n"
                      + "- City: %s\n"
                      + "- Organization: %s\n"
                      + "- ASN: %s\n"
                      + "- Latitude: %s\n"
                      + "- Longitude: %s", nullToEmpty(ipWhoisInfo.getPrefix()),
                  nullToEmpty(ipWhoisInfo.getCountry()), nullToEmpty(ipWhoisInfo.getCountry_code()),
                  nullToEmpty(ipWhoisInfo.getRegion()), nullToEmpty(ipWhoisInfo.getCity()),
                  nullToEmpty(ipWhoisInfo.getOrganization()), nullToEmpty(ipWhoisInfo.getAsn()),
                  nullToEmpty(ipWhoisInfo.getLatitude()), nullToEmpty(ipWhoisInfo.getLongitude())),
                  publicIpPoint);
              final ArrayList<OverlayItem> overlayItems = new ArrayList<>();
              overlayItems.add(overlayItem);
              final MyOwnItemizedOverlay overlay =
                  new MyOwnItemizedOverlay(IPGeoActivity.this, overlayItems);

              mapController.setCenter(publicIpPoint);

              map.getOverlays().add(overlay);

              map.setVisibility(View.VISIBLE);
              errorView.setVisibility(View.GONE);
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          Crashlytics.logException(e);
          //No worries
          runOnUiThread(new Runnable() {
            @Override public void run() {
              map.setVisibility(View.GONE);
              if (e instanceof NumberFormatException) {
                errorView.setText("Unable to geo-locate IP Address - please try again later.");
              } else {
                errorView.setText("Error: " + Utils.handleException(e).first);
              }
              errorView.setVisibility(View.VISIBLE);
            }
          });
        } finally {
          runOnUiThread(new Runnable() {
            @Override public void run() {
              progressBar.setVisibility(View.GONE);
            }
          });
        }
        return null;
      }
    }.execute(publicIpToGeolocate);
  }

  @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.action_feedback:
        Utils.openFeedbackForm(this, mRouterUuid);
        //                final Intent intent = new Intent(IPGeoActivity.this, FeedbackActivity.class);
        //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
        //                final File screenshotFile = new File(getCacheDir(), "feedback_screenshot.png");
        //                ViewGroupUtils.exportViewToFile(IPGeoActivity.this, getWindow().getDecorView(), screenshotFile);
        //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE, screenshotFile.getAbsolutePath());
        //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY, this.getClass().getCanonicalName());
        //                startActivity(intent);
        ////                Utils.buildFeedbackDialog(this, true);
        return true;

      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_activity_ip_geo, menu);
    this.optionsMenu = menu;
    return super.onCreateOptionsMenu(menu);
  }

  @Override public void onRequestPermissionsResult(int requestCode, String permissions[],
      int[] grantResults) {

    switch (requestCode) {
      case RouterCompanionAppConstants.Permissions.STORAGE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted, yay!
          Crashlytics.log(Log.DEBUG, LOG_TAG, "Yay! Permission granted for #" + requestCode);
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
          Crashlytics.log(Log.WARN, LOG_TAG, "Boo! Permission denied for #" + requestCode);
          Utils.displayMessage(this, "Will be unable to display (and cache) map!", Style.INFO);
        }
        return;
      }
      default:
        break;
    }
  }
}
