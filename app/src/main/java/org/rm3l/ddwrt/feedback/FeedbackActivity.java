package org.rm3l.ddwrt.feedback;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.ColorUtils;

import java.io.File;

/**
 * Created by rm3l on 24/04/16.
 */
public class FeedbackActivity extends AppCompatActivity {

    private static final String LOG_TAG = FeedbackActivity.class.getSimpleName();

    public static final String SCREENSHOT_FILE = "SCREENSHOT_FILE";

    private boolean mIsThemeLight;

    private Bitmap mBitmap;

    private DDWRTCompanionDAO mDao;
    private Menu optionsMenu;
    private Router mRouter;

    private TextInputLayout emailInputLayout;
    private EditText email;

    private TextInputLayout contentInputLayout;
    private EditText content;

    private CheckBox includeScreenshotAndLogs;

    private ImageView screenshot;

    private EditText routerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsThemeLight = ColorUtils.isThemeLight(this);
        if (mIsThemeLight) {
            //Light
            setTheme(R.style.AppThemeLight);
            getWindow().getDecorView()
                    .setBackgroundColor(ContextCompat.getColor(this,
                            android.R.color.white));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        mDao = RouterManagementActivity.getDao(this);

        setContentView(R.layout.activity_feedback);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_feedback_toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Send Feedback");
            toolbar.setTitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(),
                    R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        emailInputLayout = (TextInputLayout) findViewById(R.id.activity_feedback_email_input_layout);
        email = (EditText) findViewById(R.id.activity_feedback_email);

        contentInputLayout = (TextInputLayout) findViewById(R.id.activity_feedback_content_input_layout);
        content = (EditText) findViewById(R.id.activity_feedback_content);

        routerInfo = (EditText) findViewById(R.id.activity_feedback_router_information_content);

        includeScreenshotAndLogs = (CheckBox) findViewById(R.id.activity_feedback_include_screenshot_and_logs);

        screenshot = (ImageView) findViewById(R.id.activity_feedback_include_screenshot_and_logs_content_screenshot);

        final Intent intent = getIntent();

        final String routerSelected =
                intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
        if (routerSelected == null || (mRouter = mDao.getRouter(routerSelected)) == null) {
            routerInfo.setEnabled(true);
        } else {
            //Fill with router information
            //TODO
            routerInfo.setEnabled(false);
            routerInfo.setText(
                    String.format("- Model: %s\n" +
                            "- Firmware: %s\n" +
                            "- Kernel: %s\n" +
                            "- CPU Model: %s\n" +
                            "- CPU Cores: %s\n" +
                            "- Memory (Total): %s\n" +
                            "- Memory (Free): %s\n" +
                            "- NVRAM: %s",
                            mRouter.getRouterModel(),
                            "TODO FW",
                            "TODO KERNEL",
                            "TODO CPU MODEL",
                            "TODO CPU CORES",
                            "TODO MEM TOTAL",
                            "TODO MEM FREE",
                            "TODO NVRAM"));
        }

        final String screenshotFilePath = intent.getStringExtra(SCREENSHOT_FILE);
        if (!TextUtils.isEmpty(screenshotFilePath)) {
            final File file = new File(screenshotFilePath);
            if (file.exists()) {
                mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                screenshot.setImageBitmap(mBitmap);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_feedback, menu);
        this.optionsMenu = menu;
        //TODO
        return true;
    }

}
