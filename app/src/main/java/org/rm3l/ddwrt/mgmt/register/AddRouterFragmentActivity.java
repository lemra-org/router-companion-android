package org.rm3l.ddwrt.mgmt.register;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.ColorUtils;

/**
 * Created by rm3l on 15/03/16.
 */
public class AddRouterFragmentActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ColorUtils.isThemeLight(this)) {
            //Light
            setTheme(R.style.AppThemeLight);
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this, R.color.GhostWhite));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(R.layout.wizard_add_router);
    }

}
