package org.rm3l.ddwrt.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.rm3l.ddwrt.BuildConfig;

/**
 * Created by rm3l on 30/10/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Mock
    Context mMockContext;

    @Test
    public void testGetApplicationName_FromNullContext() {
        Assert.assertNull(Utils.getApplicationName(null));
    }

    @Test
    public void testGetApplicationName_FromNonNullContext() {
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        Mockito.when(applicationInfo.loadLabel(Mockito.any(PackageManager.class)))
                .thenReturn(BuildConfig.APPLICATION_ID + "_" + BuildConfig.VERSION_NAME);
        Mockito.when(mMockContext.getApplicationInfo()).thenReturn(applicationInfo);
        Assert.assertEquals(BuildConfig.APPLICATION_ID + "_" + BuildConfig.VERSION_NAME,
                Utils.getApplicationName(mMockContext));
    }

}
