package org.rm3l.router_companion;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;

/**
 * Created by rm3l on 17/09/16.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = new Intent(this, RouterManagementActivity.class);
        startActivity(intent);
        finish();
    }
}
