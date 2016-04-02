package org.rm3l.ddwrt.widgets.wizard;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.ColorUtils;

/**
 * Created by rm3l on 01/04/16.
 */
public abstract class MaterialWizardFragmentActivity extends FragmentActivity {

    @Override
    public final void onCreate(Bundle savedInstanceState) {

        if (isFullScreen()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

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

        setContentView(getContentView());
    }

    @LayoutRes
    protected abstract int getContentView();

    /**
     * Override to make activity full screen.
     * @return whether to make activity full screen
     */
    protected boolean isFullScreen() {
        return false;
    }

}
