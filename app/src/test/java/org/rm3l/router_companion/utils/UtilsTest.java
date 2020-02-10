package org.rm3l.router_companion.utils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.RouterCompanionAppConstants.LINE_SEPARATOR;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/** Created by rm3l on 30/10/2016. */
@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

  @Mock private Activity mMockActivity;

  @Mock private Context mMockContext;

  @Test
  public void testDecimalToIp4() {
    assertEquals("0.0.0.0", Utils.decimalToIp4(0));
    assertEquals("1.2.3.4", Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {1, 2, 3, 4})));
    assertEquals("10.11.12.13", Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {10, 11, 12, 13})));
    assertEquals(
        "192.168.33.77", Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {192, 168, 33, 77})));
    assertEquals(
        "172.17.17.17", Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {172, 17, 17, 17})));
    assertEquals(
        "73.150.2.210", Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {73, 150, 2, 210})));
  }

  @Test
  public void testDisplayMessage() {
    Utils.displayMessage(mMockActivity, "Lorem Ipsum...", Style.ALERT);
    verify(mMockActivity, times(1)).runOnUiThread(any(Runnable.class));
  }

  @Test
  public void testGetApplicationName_FromNonNullContext() {
    final ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
    final String label = BuildConfig.APPLICATION_ID + "_" + BuildConfig.VERSION_NAME;
    final PackageManager packageManager = mock(PackageManager.class);
    when(mMockContext.getPackageManager()).thenReturn(packageManager);
    when(applicationInfo.loadLabel(any(PackageManager.class))).thenReturn(label);
    when(mMockContext.getApplicationInfo()).thenReturn(applicationInfo);
    assertEquals(label, Utils.getApplicationName(mMockContext));
  }

  @Test
  public void testGetApplicationName_FromNullContext() {
    Assert.assertNull(Utils.getApplicationName(null));
  }

  @Test
  public void testGetLines() throws IOException {
    final String str = String.format("line1%1$sline2%1$s%1$smy line 3 %1$s", LINE_SEPARATOR);
    final String[] lines = Utils.getLines(new BufferedReader(new StringReader(str)));
    assertNotEquals(0, lines.length);
    assertEquals(4, lines.length);
  }

  @Test
  public void testGetNextLoaderId_Twice() {
    final long loaderId1 = Utils.getNextLoaderId();
    assertTrue(loaderId1 >= 1);
    assertTrue(Utils.getNextLoaderId() > loaderId1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIp4ToDecimal_InvalidLength() {
    Utils.ip4ToDecimal(new int[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIp4ToDecimal_PreconditionCheck_Part_Higher_Than_255() {
    Utils.ip4ToDecimal(new int[] {192, 567, 77});
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIp4ToDecimal_PreconditionCheck_Part_Negative() {
    Utils.ip4ToDecimal(new int[] {192, 168, -10});
  }

  @Test
  public void testOpenDonateActivity() {
    Utils.openDonateActivity(mMockContext);
    // TODO Check that DonateActivity intent has actually been passed
    verify(mMockContext, times(1)).startActivity(any(Intent.class));
  }

  @Test
  public void testReadAll_EmptyBufferedReader() throws IOException {
    final StringBuffer result = new StringBuffer();
    Utils.readAll(new BufferedReader(new StringReader(EMPTY_STRING)), result);
    assertTrue(result.toString().isEmpty());
  }

  @Test
  public void testReadAll_Twice() throws IOException {
    final StringBuffer result = new StringBuffer();
    final String str = "Lorem Ipsum Dolor Sit amet";
    Utils.readAll(new BufferedReader(new StringReader(str)), result);
    assertEquals(str, result.toString());

    // Test appending to an existing str
    final String str2 = "a new line";
    Utils.readAll(new BufferedReader(new StringReader(str2)), result);
    assertEquals(str + str2, result.toString());
  }

  @Test(expected = StringIndexOutOfBoundsException.class)
  public void testTruncateText_NegativeMaxLength() {
    Assert.assertEquals("Hey", Utils.truncateText("Hey", -1));
  }

  @Test
  public void testTruncateText_NullStr() {
    Assert.assertNull(Utils.truncateText(null, 0));
  }

  @Test
  public void testTruncateText_PositiveMaxLen() {
    Assert.assertEquals(
        "Hey bro...", Utils.truncateText("Hey brothers and sisters!", "Hey brothe".length()));
  }
}
