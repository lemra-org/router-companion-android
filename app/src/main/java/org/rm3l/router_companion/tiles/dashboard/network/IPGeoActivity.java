package org.rm3l.router_companion.tiles.dashboard.network;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import android.Manifest.permission;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import java.util.ArrayList;
import java.util.Collections;
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
import org.rm3l.router_companion.utils.PermissionsUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.map.MyOwnItemizedOverlay;

/** Created by rm3l on 02/01/16. */
public class IPGeoActivity extends AppCompatActivity {

  public static final String PUBLIC_IP_TO_DISPLAY = "PUBLIC_IP_TO_DISPLAY";

  private String mRouterUuid;

  private MapView map;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();

    final String publicIpToGeolocate = intent.getStringExtra(PUBLIC_IP_TO_DISPLAY);

    if (isNullOrEmpty(publicIpToGeolocate)) {
      Toast.makeText(
              this, "Don't know what to geo-locate - please try again later.", Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    mRouterUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

    final Router router = RouterManagementActivity.Companion.getDao(this).getRouter(mRouterUuid);
    ColorUtils.Companion.setAppTheme(
        this, router != null ? router.getRouterFirmware() : null, false);

    setContentView(R.layout.activity_ip_geo);

    final InterstitialAd interstitialAd =
        AdUtils.requestNewInterstitial(
            this, R.string.interstitial_ad_unit_id_wireless_network_generate_qr_code);

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

    map = findViewById(R.id.activity_ip_geo_map);

    // Permission requests
    PermissionsUtils.requestPermissions(
        this,
        Collections.singletonList(permission.WRITE_EXTERNAL_STORAGE),
        () -> {
          map.postInvalidate();
          return null;
        },
        () -> null,
        "Storage access is required to display and cache map tiles");

    final TextView errorView = (TextView) findViewById(R.id.activity_ip_geo_map_error);

    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.activity_ip_geo_map_loading);

    new AsyncTask<String, Void, Void>() {

      @Override
      protected Void doInBackground(String... params) {
        try {
          final IPWhoisInfo ipWhoisInfo =
              ActiveIPConnectionsDetailActivity.mIPWhoisInfoCache.get(publicIpToGeolocate);
          if (ipWhoisInfo == null) {
            throw new DDWRTCompanionException(
                "Unable to geo-locate IP Address - please try again later.");
          }
          final double latitude = Double.parseDouble(ipWhoisInfo.getLatitude());
          final double longitude = Double.parseDouble(ipWhoisInfo.getLongitude());

          runOnUiThread(
              () -> {
                map.setTileSource(RouterCompanionAppConstants.TILE_SOURCE);
                map.setBuiltInZoomControls(true);
                map.setMultiTouchControls(true);

                // Night mode
                //                            map.getController().setInvertedTiles(!ColorUtils
                //                                    .isThemeLight(IPGeoActivity.this));
                map.getOverlayManager()
                    .getTilesOverlay()
                    .setColorFilter(
                        ColorUtils.Companion.isThemeLight(IPGeoActivity.this)
                            ? null
                            : TilesOverlay.INVERT_COLORS);

                // Act according to user-defined data usage control setting
                // map.setUseDataConnection(Utils.canUseDataConnection(this));
                map.setUseDataConnection(true);

                final IMapController mapController = map.getController();
                mapController.setZoom(11);

                final GeoPoint publicIpPoint = new GeoPoint(latitude, longitude);
                final OverlayItem overlayItem =
                    new OverlayItem(
                        ipWhoisInfo.getIp(),
                        String.format(
                            "- Prefix: %s\n"
                                + "- Country: %s (%s)\n"
                                + "- Region: %s\n"
                                + "- City: %s\n"
                                + "- Organization: %s\n"
                                + "- ASN: %s\n"
                                + "- Latitude: %s\n"
                                + "- Longitude: %s",
                            nullToEmpty(ipWhoisInfo.getPrefix()),
                            nullToEmpty(ipWhoisInfo.getCountry()),
                            nullToEmpty(ipWhoisInfo.getCountry_code()),
                            nullToEmpty(ipWhoisInfo.getRegion()),
                            nullToEmpty(ipWhoisInfo.getCity()),
                            nullToEmpty(ipWhoisInfo.getOrganization()),
                            nullToEmpty(ipWhoisInfo.getAsn()),
                            nullToEmpty(ipWhoisInfo.getLatitude()),
                            nullToEmpty(ipWhoisInfo.getLongitude())),
                        publicIpPoint);
                final ArrayList<OverlayItem> overlayItems = new ArrayList<>();
                overlayItems.add(overlayItem);
                final MyOwnItemizedOverlay overlay =
                    new MyOwnItemizedOverlay(IPGeoActivity.this, overlayItems);

                mapController.setCenter(publicIpPoint);

                map.getOverlays().add(overlay);

                map.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
              });
        } catch (final Exception e) {
          ReportingUtils.reportException(IPGeoActivity.this, e);
          // No worries
          runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
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
          runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  progressBar.setVisibility(View.GONE);
                }
              });
        }
        return null;
      }
    }.execute(publicIpToGeolocate);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // this will refresh the osmdroid configuration on resuming.
    // if you make changes to the configuration, use
    // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    // Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    if (map != null) {
      map.onResume(); // needed for compass, my location overlays, v6.0.0 and up
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    // this will refresh the osmdroid configuration on resuming.
    // if you make changes to the configuration, use
    // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    // Configuration.getInstance().save(this, prefs);
    if (map != null) {
      map.onPause(); // needed for compass, my location overlays, v6.0.0 and up
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_activity_ip_geo, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int i = item.getItemId();
    if (i == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (i == R.id.action_feedback) {
      Utils.openFeedbackForm(this, mRouterUuid);
      //                final Intent intent = new Intent(IPGeoActivity.this,
      // FeedbackActivity.class);
      //                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouterUuid);
      //                final File screenshotFile = new File(getCacheDir(),
      // "feedback_screenshot.png");
      //                ViewGroupUtils.exportViewToFile(IPGeoActivity.this,
      // getWindow().getDecorView(), screenshotFile);
      //                intent.putExtra(FeedbackActivity.SCREENSHOT_FILE,
      // screenshotFile.getAbsolutePath());
      //                intent.putExtra(FeedbackActivity.CALLER_ACTIVITY,
      // this.getClass().getCanonicalName());
      //                startActivity(intent);
      ////                Utils.buildFeedbackDialog(this, true);
      return true;
    } else {
    }
    return super.onOptionsItemSelected(item);
  }
}
