package org.rm3l.router_companion.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.kotlin.setAppTheme
import org.wordpress.passcodelock.AppLockManager
import org.wordpress.passcodelock.PasscodePreferenceFragment

const val SUBTITLE = "PIN Lock"

class PinLockPreferenceActivity : AppCompatActivity() {

    private var mPasscodePreferenceFragment: PasscodePreferenceFragment? = null
    private var mSamplePreferenceFragment: PinLockPreferenceFragment? = null

    companion object {
        private val KEY_PASSCODE_FRAGMENT = "passcode-fragment"
        private val KEY_PREFERENCE_FRAGMENT = "preference-fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setAppTheme(null, false)
        this.setContentView(R.layout.settings)

        val toolbar = findViewById<Toolbar>(R.id.settings_toolbar)
        toolbar?.let {
            it.title = AbstractDDWRTSettingsActivity.SETTINGS
            it.subtitle = SUBTITLE
            it.setTitleTextAppearance(applicationContext, R.style.ToolbarTitle)
            it.setSubtitleTextAppearance(applicationContext, R.style.ToolbarSubtitle)
            it.setTitleTextColor(ContextCompat.getColor(this@PinLockPreferenceActivity, R.color.white))
            it.setSubtitleTextColor(ContextCompat.getColor(this@PinLockPreferenceActivity, R.color.white))
            setSupportActionBar(it)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        mSamplePreferenceFragment = fragmentManager.findFragmentByTag(
                KEY_PREFERENCE_FRAGMENT) as PinLockPreferenceFragment?
        mPasscodePreferenceFragment = fragmentManager.findFragmentByTag(
                KEY_PASSCODE_FRAGMENT) as PasscodePreferenceFragment?

        if (mSamplePreferenceFragment == null || mPasscodePreferenceFragment == null) {
            val passcodeArgs = Bundle()
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false)
            mSamplePreferenceFragment = PinLockPreferenceFragment()
            mPasscodePreferenceFragment = PasscodePreferenceFragment()
            mPasscodePreferenceFragment?.arguments = passcodeArgs

            fragmentManager.beginTransaction()
                    .replace(R.id.settings_content_frame, mPasscodePreferenceFragment, KEY_PASSCODE_FRAGMENT)
                    .add(R.id.settings_content_frame, mSamplePreferenceFragment, KEY_PREFERENCE_FRAGMENT)
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_feedback -> {
                Utils.openFeedbackForm(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        val togglePreference = mSamplePreferenceFragment?.findPreference(
                getString(org.wordpress.passcodelock.R.string.pref_key_passcode_toggle)) as SwitchPreference?
        val changePreference = mSamplePreferenceFragment?.findPreference(
                getString(org.wordpress.passcodelock.R.string.pref_key_change_passcode))

        if (togglePreference != null && changePreference != null) {
            mPasscodePreferenceFragment?.setPreferences(togglePreference, changePreference)
            togglePreference.isChecked = AppLockManager.getInstance().appLock.isPasswordLocked
        }
    }
}

class PinLockPreferenceFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        addPreferencesFromResource(R.xml.pref_pin_lock_v2)
    }
}