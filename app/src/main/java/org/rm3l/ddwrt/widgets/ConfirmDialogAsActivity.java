package org.rm3l.ddwrt.widgets;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.rm3l.ddwrt.R;

public abstract class ConfirmDialogAsActivity extends ActionBarActivity {

    public static final String TITLE = ConfirmDialogAsActivity.class.getSimpleName() + ".TITLE";
    public static final String MESSAGE = ConfirmDialogAsActivity.class.getSimpleName() + ".MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(false);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.confirm_dialog_as_activity);

        final Intent intent = getIntent();

        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(intent.getStringExtra(TITLE));
        }

        ((TextView) findViewById(R.id.confirm_dialog_as_activity_message))
                .setText(intent.getStringExtra(MESSAGE));

        findViewById(R.id.confirm_dialog_as_activity_no_button)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View.OnClickListener noButtonOnClickListener = getNoButtonOnClickListener();
                    if (noButtonOnClickListener != null) {
                        noButtonOnClickListener.onClick(view);
                    }
                    finish();
                }
            });

        findViewById(R.id.confirm_dialog_as_activity_yes_button)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
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
