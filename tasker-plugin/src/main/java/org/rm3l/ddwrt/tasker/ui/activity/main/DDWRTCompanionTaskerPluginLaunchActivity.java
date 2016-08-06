/*
 * Copyright (c) 2016 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.rm3l.ddwrt.tasker.ui.activity.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.rm3l.ddwrt.tasker.Constants;
import org.rm3l.ddwrt.tasker.R;
import org.rm3l.ddwrt.tasker.feedback.maoni.FeedbackHandler;
import org.rm3l.maoni.Maoni;

/**
 * EntryPoint Activity
 */
public class DDWRTCompanionTaskerPluginLaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.ddwrt_companion_tasker_main_activity);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setTitle("DD-WRT Companion");
            toolbar.setSubtitle("Tasker Plugin");
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public final boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.ddwrt_companion_tasker_feedback:
                new Maoni.Builder(Constants.FILEPROVIDER_AUTHORITY)
                        .withTheme(R.style.AppThemeLight_StatusBarTransparent)
                        .withWindowTitle("Send Feedback")
                        .withExtraLayout(R.layout.activity_feedback_maoni)
                        .withHandler(new FeedbackHandler(this))
                        .build()
                        .start(this);
                break;
            case R.id.ddwrt_companion_tasker_about:
                //TODO About
                Toast.makeText(DDWRTCompanionTaskerPluginLaunchActivity.this,
                        "[TODO] About", Toast.LENGTH_SHORT).show();
                break;
            case R.id.exit:
                finish();
                break;
            default:
                break;
        }
        return true;
    }

}
