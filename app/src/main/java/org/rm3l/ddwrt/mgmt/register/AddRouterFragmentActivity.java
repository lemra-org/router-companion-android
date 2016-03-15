package org.rm3l.ddwrt.mgmt.register;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 15/03/16.
 */
public class AddRouterFragmentActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_add_router);
    }

}
