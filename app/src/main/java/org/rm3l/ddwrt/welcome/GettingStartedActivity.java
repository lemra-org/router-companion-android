package org.rm3l.ddwrt.welcome;

import com.stephentuso.welcome.WelcomeScreenBuilder;
import com.stephentuso.welcome.ui.WelcomeActivity;
import com.stephentuso.welcome.util.WelcomeScreenConfiguration;

import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 21/08/16.
 */
public class GettingStartedActivity extends WelcomeActivity {

    @Override
    protected WelcomeScreenConfiguration configuration() {
        return new WelcomeScreenBuilder(this)
                .theme(R.style.CustomWelcomeScreenTheme)
                .defaultTitleTypefacePath("Montserrat-Bold.ttf")
                .defaultHeaderTypefacePath("Montserrat-Bold.ttf")
                .titlePage(R.drawable.welcome_photo, "Welcome", R.color.orange_background)
                .basicPage(R.drawable.welcome_photo,
                        "Simple to use",
                        "Add a welcome screen to your app with only a few lines of code.",
                        R.color.red_background)
                .parallaxPage(
                        R.layout.welcome_parallax_example,
                        "Easy parallax",
                        "Supply a layout and parallax effects will automatically be applied",
                        R.color.purple_background,
                        0.2f,
                        2f)
                .basicPage(R.drawable.welcome_photo,
                        "Customizable",
                        "All elements of the welcome screen can be customized easily.",
                        R.color.blue_background)
                .swipeToDismiss(true)
                .exitAnimation(android.R.anim.fade_out)
                .build();
    }


    public static String welcomeKey() {
        return "Main_" + Integer.toString(BuildConfig.VERSION_CODE);
    }
}
