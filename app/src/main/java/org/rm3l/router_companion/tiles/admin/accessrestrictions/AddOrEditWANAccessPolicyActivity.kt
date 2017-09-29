package org.rm3l.router_companion.tiles.admin.accessrestrictions

import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import android.widget.Toast
import com.google.common.base.Strings.isNullOrEmpty
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.kotlin.setAppTheme

class AddOrEditWANAccessPolicyActivity : AppCompatActivity() {

  /**
   * The [android.support.v4.view.PagerAdapter] that will provide
   * fragments for each of the sections. We use a
   * [FragmentPagerAdapter] derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * [android.support.v4.app.FragmentStatePagerAdapter].
   */
  private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

  /**
   * The [ViewPager] that will host the section contents.
   */
  private var mViewPager: ViewPager? = null

  private var mRouter: Router? = null

  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(Utils.getBaseContextToAttach(this, newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val routerUuid = intent.getStringExtra(ROUTER_SELECTED)
    if (isNullOrEmpty(routerUuid)) {
      toast("Internal Error: Router could not be determined")
      finish()
      return
    }

    val dao = RouterManagementActivity.getDao(this)
    mRouter = dao.getRouter(routerUuid)
    if (mRouter == null) {
      toast("Internal Error: Router could not be determined")
      finish()
      return
    }

    setAppTheme(mRouter?.routerFirmware)

    setContentView(R.layout.activity_add_or_edit_wan_access_policy)

    val toolbar = find<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    // Set up the ViewPager with the sections adapter.
    mViewPager = find<ViewPager>(R.id.container)
    mViewPager!!.adapter = mSectionsPagerAdapter

    val fab = find<FloatingActionButton>(R.id.fab)
    fab.onClick {
      Snackbar.make(it!!, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null)
          .show()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_add_or_edit_wanaccess_policy, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    when (item.itemId) {
      R.id.action_settings -> return true
      else -> return super.onOptionsItemSelected(item)
    }
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  class PlaceholderFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
      val rootView = inflater!!.inflate(R.layout.fragment_add_or_edit_wan_access_policy, container,
          false)
      val textView = rootView.find<TextView>(R.id.section_label)
      textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
      return rootView
    }

    companion object {
      /**
       * The fragment argument representing the section number for this
       * fragment.
       */
      private val ARG_SECTION_NUMBER = "section_number"

      /**
       * Returns a new instance of this fragment for the given section
       * number.
       */
      fun newInstance(sectionNumber: Int): PlaceholderFragment {
        val fragment = PlaceholderFragment()
        val args = Bundle()
        args.putInt(ARG_SECTION_NUMBER, sectionNumber)
        fragment.arguments = args
        return fragment
      }
    }
  }

  /**
   * A [FragmentPagerAdapter] that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      return PlaceholderFragment.newInstance(position + 1)
    }

    override fun getCount(): Int {
      // Show 3 total pages.
      return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
      when (position) {
        0 -> return "SECTION 1"
        1 -> return "SECTION 2"
        2 -> return "SECTION 3"
      }
      return null
    }
  }
}
