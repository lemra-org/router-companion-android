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
package org.rm3l.router_companion.settings;

import static com.google.common.base.Strings.isNullOrEmpty;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;
import java.util.Locale;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.ReportingUtils;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public abstract class AbstractRouterSettingsActivity extends AbstractDDWRTSettingsActivity {

    @Nullable
    protected Router mRouter;

    @NonNull
    protected String mRouterUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mRouterUuid = getIntent().getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        boolean doFinish = false;
        //noinspection ConstantConditions
        if ((mRouter = RouterManagementActivity.getDao(this).getRouter(this.mRouterUuid)) == null) {
            Toast.makeText(this, "No router set or router no longer exists", Toast.LENGTH_LONG).show();
            doFinish = true;
        }

        //Need to call super.onCreate prior to calling finish()
        super.onCreate(savedInstanceState);

        if (doFinish) {
            finish();
        }
    }

    @Override
    public void finish() {
        final Intent data = new Intent();
        setResult(RESULT_OK, data);

        super.finish();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (isNullOrEmpty(this.mRouterUuid)) {
            Toast.makeText(this, "Whoops - internal error. Issue will be reported!", Toast.LENGTH_LONG)
                    .show();
            ReportingUtils.reportException(null, new IllegalStateException(
                    "RouterSettingsActivity: Router UUID is null: " + this.mRouterUuid));
            finish();
        }
        return super.getSharedPreferences(this.mRouterUuid, mode);
    }

    @NonNull
    @Override
    protected abstract PreferenceFragment getPreferenceFragment();

    @Nullable
    @Override
    protected String getRouterUuid() {
        return mRouterUuid;
    }

    @Nullable
    @Override
    protected String getToolbarSubtitle() {
        if (mRouter == null) {
            return null;
        }
        return String.format(Locale.US, "%s (%s:%d)", mRouter.getDisplayName(),
                mRouter.getRemoteIpAddress(), mRouter.getRemotePort());
    }

    @Override
    protected void setAppTheme() {
        ColorUtils.Companion.setAppTheme(this, mRouter.getRouterFirmware(), false);
    }
}
