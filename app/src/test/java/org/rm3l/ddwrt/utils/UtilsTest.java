package org.rm3l.ddwrt.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.rm3l.ddwrt.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import de.keyboardsurfer.android.widget.crouton.Style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.LINE_SEPARATOR;

/**
 * Created by rm3l on 30/10/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Mock
    Context mMockContext;

    @Mock
    Activity mMockActivity;

    @Test
    public void testGetApplicationName_FromNullContext() {
        Assert.assertNull(Utils.getApplicationName(null));
    }

    @Test
    public void testGetApplicationName_FromNonNullContext() {
        final ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        final String label = BuildConfig.APPLICATION_ID + "_" + BuildConfig.VERSION_NAME;
        when(applicationInfo.loadLabel(any(PackageManager.class)))
                .thenReturn(label);
        when(mMockContext.getApplicationInfo()).thenReturn(applicationInfo);
        assertEquals(label,
                Utils.getApplicationName(mMockContext));
    }

    @Test
    public void testGetNextLoaderId_Twice() {
        final long loaderId1 = Utils.getNextLoaderId();
        assertTrue(loaderId1 >= 1);
        assertTrue(Utils.getNextLoaderId() > loaderId1);
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

        //Test appending to an existing str
        final String str2 = "a new line";
        Utils.readAll(new BufferedReader(new StringReader(str2)), result);
        assertEquals(str + str2,
                result.toString());
    }

    @Test
    public void testGetLines() throws IOException {
        final String str = String
                .format("line1%1$sline2%1$s%1$smy line 3 %1$s", LINE_SEPARATOR);
        final String[] lines = Utils.getLines(new BufferedReader(new StringReader(str)));
        assertFalse(lines.length == 0);
        assertEquals(4, lines.length);
    }

    @Test
    public void testOpenDonateActivity() {
        Utils.openDonateActivity(mMockContext);
        //TODO Check that DonateActivity intent has actually been passed
        verify(mMockContext, times(1)).
                startActivity(any(Intent.class));
    }

    @Test
    public void testDisplayMessage() {
        Utils.displayMessage(mMockActivity, "Lorem Ipsum...", Style.ALERT);
        verify(mMockActivity, times(1))
                .runOnUiThread(any(Runnable.class));
    }

    @Test
    public void testDecimalToIp4() {
        assertEquals("0.0.0.0", Utils.decimalToIp4(0));
        assertEquals("1.2.3.4",
                Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {1, 2, 3, 4})));
        assertEquals("10.11.12.13",
                Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {10, 11, 12, 13})));
        assertEquals("192.168.33.77",
                Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {192, 168, 33, 77})));
        assertEquals("172.17.17.17",
                Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {172, 17, 17, 17})));
        assertEquals("73.150.2.210",
                Utils.decimalToIp4(Utils.ip4ToDecimal(new int[] {73, 150, 2, 210})));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIp4ToDecimal_InvalidLength() {
        Utils.ip4ToDecimal(new int[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIp4ToDecimal_PreconditionCheck_Part_Negative() {
        Utils.ip4ToDecimal(new int[] {192, 168, -10});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIp4ToDecimal_PreconditionCheck_Part_Higher_Than_255() {
        Utils.ip4ToDecimal(new int[] {192, 567, 77});
    }

    @Test
    public void testTruncateText_NullStr() {
        Assert.assertNull(Utils.truncateText(null, 0));
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testTruncateText_NegativeMaxLength() {
        Assert.assertEquals("Hey",
                Utils.truncateText("Hey", -1));
    }

    @Test
    public void testTruncateText_PositiveMaxLen() {
        Assert.assertEquals("Hey bro...",
                Utils.truncateText("Hey brothers and sisters!", "Hey brothe".length()));
    }

}
