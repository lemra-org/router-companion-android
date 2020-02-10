package org.rm3l.router_companion.welcome;

import com.stephentuso.welcome.BasicPage;
import com.stephentuso.welcome.ParallaxPage;
import com.stephentuso.welcome.TitlePage;
import com.stephentuso.welcome.WelcomeActivity;
import com.stephentuso.welcome.WelcomeConfiguration;
import org.rm3l.ddwrt.R;

/** Created by rm3l on 21/08/16. */
public class GettingStartedActivity extends WelcomeActivity {

  /**
   * Note: Only change this to a new value if you want everyone who has already used your app to see
   * the welcome screen again! This key is used to determine whether or not to show the welcome
   * screen.
   *
   * @return the welcome key
   */
  public static String welcomeKey() {
    return "DD-WRT_Companion__7.1.0";
  }

  @Override
  protected WelcomeConfiguration configuration() {
    return new WelcomeConfiguration.Builder(this)

        // .defaultTitleTypefacePath("Montserrat-Bold.ttf")
        // .defaultHeaderTypefacePath("Montserrat-Bold.ttf")

        .page(
            new TitlePage(R.drawable.logo_ddwrt_companion__large, "Welcome. Swipe to get started")
                .background(R.color.purple_background))
        .page(
            new BasicPage(
                    R.drawable.welcome_screen_easy_fun_management,
                    "Easy and fun router management",
                    "Manage and monitor your routers on the go. "
                        + "Your routers must have DD-WRT firmware installed and SSH configured properly.")
                .background(R.color.purple_background))
        .page(
            new BasicPage(
                    R.drawable.welcome_screen_protect_app,
                    "Secure",
                    "All of your sensitive info is encrypted locally.\n"
                        + "And you can now PIN-protect the app. Visit the global settings to manage PIN lock.")
                .background(R.color.purple_background))

        // TODO Add custom fragment, which includes a button to open the Play Store to download the
        // Tasker Plugin
        .page(
            new ParallaxPage(
                    R.layout.welcome_parallax_automation,
                    "Automation",
                    "Get the 'DD-WRT Companion Tasker Plugin' app on Google Play Store, "
                        + "to make the most of your DD-WRT-powered routers.\n"
                        + "This plugin for Tasker allows you to automate various actions via DD-WRT Companion.")
                .background(R.color.purple_background)
                .firstParallaxFactor(0.2f)
                .lastParallaxFactor(2f))
        .page(
            new ParallaxPage(
                    R.layout.welcome_parallax_feedback,
                    "Have your say",
                    "Sending feedback from within the app is now easier. "
                        + "Feel free to submit new ideas, file bugs or simply say hello.\n\n"
                        + "Help and Support: https://ddwrt-companion.app")
                .background(R.color.purple_background))

        //                .basicPage(R.drawable.welcome_screen_notifs_widgets,
        //                        "One more thing...",
        //                        "Bear in mind you can use Home Screen Widgets and Shortcuts for
        // quicker access to DD-WRT Companion.",
        //                        R.color.win8_teal)

        .swipeToDismiss(true)
        .exitAnimation(android.R.anim.fade_out)
        .build();
  }
}
