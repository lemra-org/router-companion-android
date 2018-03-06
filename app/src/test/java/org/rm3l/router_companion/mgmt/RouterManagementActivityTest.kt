package org.rm3l.router_companion.mgmt

import android.support.design.widget.FloatingActionButton
import org.junit.*
import org.junit.runner.*
import org.rm3l.ddwrt.R
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouterManagementActivityTest {

    @Test
    fun clickingAddRouter_shouldOpenUpWizard() {
        val activity = Robolectric.setupActivity(RouterManagementActivity::class.java)
        activity.findViewById<FloatingActionButton>(R.id.router_list_add).performClick()

        //TODO Add assertions here
    }
}