package org.rm3l.router_companion.mgmt

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.aboutlibraries.ui.LibsActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.rm3l.ddwrt.R
import org.rm3l.maoni.ui.MaoniActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.Companion.NEW_ROUTER_ADDED
import org.rm3l.router_companion.mgmt.RouterManagementActivity.Companion.ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE
import org.rm3l.router_companion.mgmt.register.ManageRouterFragmentActivity
import org.rm3l.router_companion.settings.RouterManagementSettingsActivity
import org.robolectric.Robolectric.setupActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RouterManagementActivityTest {

    private var activity: RouterManagementActivity? = null

    @Before
    fun beforeEachTest() {
        activity = setupActivity(RouterManagementActivity::class.java)
    }

    @Test
    fun clickingAddRouterBtn_shouldOpenUpWizard() {
        activity!!.findViewById<FloatingActionButton>(R.id.router_list_add).performClick()
        try {
            testAddRouterWizard()
            Assert.fail("Test commented out due to Robolectric issue")
        } catch (e: NotImplementedError) {
            // Expected
        }
    }

    @Test
    fun clickingAddRouterMenuItem_shouldOpenUpWizard() {
        shadowOf(activity).clickMenuItem(R.id.router_list_actionbar_add)
        try {
            testAddRouterWizard()
            Assert.fail("Test commented out due to Robolectric issue")
        } catch (e: NotImplementedError) {
            // Expected
        }
    }

    private fun testAddRouterWizard() {
        val startedIntent = shadowOf(activity).nextStartedActivityForResult
        Assert.assertEquals(NEW_ROUTER_ADDED, startedIntent.requestCode)
        val shadowIntent = shadowOf(startedIntent.intent)
        Assert.assertEquals(ManageRouterFragmentActivity::class.java, shadowIntent.intentClass)

        TODO(
            "ManageRouterFragmentActivity cannot be tested due to Robolectric issues: " +
                "FragmentManager is already executing transactions"
        )

        // TODO Issue with RoboElectric: FragmentManager is already executing transactions
//        //Register a demo router.
//        val manageRouterActivity = setupActivity(ManageRouterFragmentActivity::class.java)
//        manageRouterActivity.findViewById<Button>(R.id.router_add_ip_demo).performClick()
//
//        //Check that view has been updated with demo details
//        assertEquals(BuildConfig.APPLICATION_ID,
//            manageRouterActivity.findViewById<TextView>(R.id.router_add_ip).text)

//        TODO Proceed with the other steps in the wizard
    }

    @Test
    fun clickingRefreshRouterListMenuItem_shouldRefreshData() {
        shadowOf(activity).clickMenuItem(R.id.router_list_refresh)
        // TODO
    }

    @Test
    fun clickingAboutMenuItem_shouldOpenAboutActivity() {
        shadowOf(activity).clickMenuItem(R.id.router_list_about)
        val startedIntent = shadowOf(activity).nextStartedActivity
        val shadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(LibsActivity::class.java, shadowIntent.intentClass)
    }

    @Test
    fun clickingRouterManagementSettingsMenuItem_shouldOpenUpActivity() {
        shadowOf(activity).clickMenuItem(R.id.router_list_settings)
        val startedIntent = shadowOf(activity).nextStartedActivityForResult
        Assert.assertEquals(ROUTER_MANAGEMENT_SETTINGS_ACTIVITY_CODE, startedIntent.requestCode)
        val shadowIntent = shadowOf(startedIntent.intent)
        Assert.assertEquals(RouterManagementSettingsActivity::class.java, shadowIntent.intentClass)
    }

    @Test
    fun clickingSendFeedbackMenuItem_shouldOpenUpActivity() {
        shadowOf(activity).clickMenuItem(R.id.router_list_feedback)
        val startedIntent = shadowOf(activity).nextStartedActivity
        val shadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(MaoniActivity::class.java, shadowIntent.intentClass)
    }
}
