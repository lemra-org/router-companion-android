package org.rm3l.router_companion.widgets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.utils.ColorUtils;

public abstract class ConfirmDialogAsActivity extends Activity {

  public static final String TITLE = ConfirmDialogAsActivity.class.getSimpleName() + ".TITLE";
  public static final String MESSAGE = ConfirmDialogAsActivity.class.getSimpleName() + ".MESSAGE";

  @Override protected void onCreate(Bundle savedInstanceState) {

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setFinishOnTouchOutside(true);

    super.onCreate(savedInstanceState);

    ColorUtils.Companion.setAppTheme(this, null, false);

    //        final boolean themeLight = ColorUtils.isThemeLight(this);

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

    setContentView(R.layout.confirm_dialog_as_activity);

    final Intent intent = getIntent();

    ((TextView) findViewById(R.id.confirm_dialog_as_activity_title)).setText(
        intent.getStringExtra(TITLE));

    ((TextView) findViewById(R.id.confirm_dialog_as_activity_message)).setText(
        intent.getStringExtra(MESSAGE));

    findViewById(R.id.confirm_dialog_as_activity_no_button).setOnClickListener(
        new View.OnClickListener() {
          @Override public void onClick(View view) {
            final View.OnClickListener noButtonOnClickListener = getNoButtonOnClickListener();
            if (noButtonOnClickListener != null) {
              noButtonOnClickListener.onClick(view);
            }
            finish();
          }
        });

    findViewById(R.id.confirm_dialog_as_activity_yes_button).setOnClickListener(
        new View.OnClickListener() {
          @Override public void onClick(View view) {
            final View.OnClickListener yesButtonOnClickListener = getYesButtonOnClickListener();
            if (yesButtonOnClickListener != null) {
              yesButtonOnClickListener.onClick(view);
            }
            finish();
          }
        });
  }

  protected abstract View.OnClickListener getYesButtonOnClickListener();

  protected abstract View.OnClickListener getNoButtonOnClickListener();
}
