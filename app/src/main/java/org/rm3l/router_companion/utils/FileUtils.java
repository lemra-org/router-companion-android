package org.rm3l.router_companion.utils;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static org.apache.commons.io.FileUtils.ONE_EB_BI;
import static org.apache.commons.io.FileUtils.ONE_GB_BI;
import static org.apache.commons.io.FileUtils.ONE_KB_BI;
import static org.apache.commons.io.FileUtils.ONE_MB_BI;
import static org.apache.commons.io.FileUtils.ONE_PB_BI;
import static org.apache.commons.io.FileUtils.ONE_TB_BI;

/**
 * Created by rm3l on 19/01/2017.
 */
public final class FileUtils {

  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  private FileUtils() {
  }

  public static File getTempFile(Context context, Uri url) throws IOException {
    return getTempFile(context, url.getLastPathSegment());
  }

  public static File getTempFile(Context context, String fileName) throws IOException {
    return File.createTempFile(fileName, null, context.getCacheDir());
  }

  /**
   * Formats a file's size into a human readable format
   *
   * @param fileSize the file's size as BigInteger
   * @return the size as human readable string
   *
   * Inspired from <a href="https://issues.apache.org/jira/browse/IO-373">this</a>,
   * to fix some rounding issues
   */
  public static String byteCountToDisplaySize(final BigInteger fileSize) {

    String unit = FileSize.BYTE.unit;
    BigDecimal fileSizeInUnit = BigDecimal.ZERO;
    String val;

    for (FileSize fs : FileSize.values()) {
      BigDecimal size_bd = new BigDecimal(fileSize);
      fileSizeInUnit = size_bd.divide(new BigDecimal(fs.byteCount), 5, ROUNDING_MODE);
      if (fileSizeInUnit.compareTo(BigDecimal.ONE) >= 0) {
        unit = fs.unit;
        break;
      }
    }

    // always round so that at least 3 numerics are displayed (###, ##.#, #.##)
    if (fileSizeInUnit.divide(BigDecimal.valueOf(100.0), BigDecimal.ROUND_DOWN)
        .compareTo(BigDecimal.ONE) >= 0) {
      val = fileSizeInUnit.setScale(0, ROUNDING_MODE).toString();
    } else if (fileSizeInUnit.divide(BigDecimal.valueOf(10.0), BigDecimal.ROUND_DOWN)
        .compareTo(BigDecimal.ONE) >= 0) {
      val = fileSizeInUnit.setScale(1, ROUNDING_MODE).toString();
    } else {
      val = fileSizeInUnit.setScale(2, ROUNDING_MODE).toString();
    }

    // trim zeros at the end
    if (val.endsWith(".00")) {
      val = val.substring(0, val.length() - 3);
    } else if (val.endsWith(".0")) {
      val = val.substring(0, val.length() - 2);
    }

    return String.format("%s %s", val, unit);
  }

  /**
   * Formats a file's size into a human readable format
   *
   * @param fileSize the file's size as long
   * @return the size as human readable string
   */
  public static String byteCountToDisplaySize(final long fileSize) {
    return byteCountToDisplaySize(BigInteger.valueOf(fileSize));
  }

  enum FileSize {
    EXABYTE("EB", ONE_EB_BI), PETABYTE("PB", ONE_PB_BI), TERABYTE("TB", ONE_TB_BI), GIGABYTE("GB",
        ONE_GB_BI), MEGABYTE("MB", ONE_MB_BI), KILOBYTE("KB", ONE_KB_BI), BYTE("bytes",
        BigInteger.ONE);

    private final String unit;
    private final BigInteger byteCount;

    FileSize(String unit, BigInteger byteCount) {
      this.unit = unit;
      this.byteCount = byteCount;
    }

    private String unit() {
      return unit;
    }

    private BigInteger byteCount() {
      return byteCount;
    }

  }
}