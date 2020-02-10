package org.rm3l.router_companion.utils;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** Created by rm3l on 19/01/2017. */
public final class FileUtils {

  enum FileSize {
    EXABYTE("EB", ONE_EB_BI),
    PETABYTE("PB", ONE_PB_BI),
    TERABYTE("TB", ONE_TB_BI),
    GIGABYTE("GB", ONE_GB_BI),
    MEGABYTE("MB", ONE_MB_BI),
    KILOBYTE("KB", ONE_KB_BI),
    BYTE("bytes", BigInteger.ONE);

    private final BigInteger byteCount;

    private final String unit;

    FileSize(String unit, BigInteger byteCount) {
      this.unit = unit;
      this.byteCount = byteCount;
    }

    private BigInteger byteCount() {
      return byteCount;
    }

    private String unit() {
      return unit;
    }
  }

  /** The number of bytes in a kilobyte. */
  public static final long ONE_KB = 1024;

  /**
   * The number of bytes in a kilobyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_KB_BI = BigInteger.valueOf(ONE_KB);

  /** The number of bytes in a megabyte. */
  public static final long ONE_MB = ONE_KB * ONE_KB;

  /**
   * The number of bytes in a megabyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);

  /** The file copy buffer size (30 MB) */
  private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;

  /** The number of bytes in a gigabyte. */
  public static final long ONE_GB = ONE_KB * ONE_MB;

  /**
   * The number of bytes in a gigabyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);

  /** The number of bytes in a terabyte. */
  public static final long ONE_TB = ONE_KB * ONE_GB;

  /**
   * The number of bytes in a terabyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);

  /** The number of bytes in a petabyte. */
  public static final long ONE_PB = ONE_KB * ONE_TB;

  /**
   * The number of bytes in a petabyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);

  /** The number of bytes in an exabyte. */
  public static final long ONE_EB = ONE_KB * ONE_PB;

  /**
   * The number of bytes in an exabyte.
   *
   * @since 2.4
   */
  public static final BigInteger ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);

  /** The number of bytes in a zettabyte. */
  public static final BigInteger ONE_ZB =
      BigInteger.valueOf(ONE_KB).multiply(BigInteger.valueOf(ONE_EB));

  /** The number of bytes in a yottabyte. */
  public static final BigInteger ONE_YB = ONE_KB_BI.multiply(ONE_ZB);

  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  /**
   * Formats a file's size into a human readable format
   *
   * @param fileSize the file's size as BigInteger
   * @return the size as human readable string
   *     <p>Inspired from <a href="https://issues.apache.org/jira/browse/IO-373">this</a>, to fix
   *     some rounding issues
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
    if (fileSizeInUnit
            .divide(BigDecimal.valueOf(100.0), BigDecimal.ROUND_DOWN)
            .compareTo(BigDecimal.ONE)
        >= 0) {
      val = fileSizeInUnit.setScale(0, ROUNDING_MODE).toString();
    } else if (fileSizeInUnit
            .divide(BigDecimal.valueOf(10.0), BigDecimal.ROUND_DOWN)
            .compareTo(BigDecimal.ONE)
        >= 0) {
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

  public static File getTempFile(Context context, Uri url) throws IOException {
    return getTempFile(context, url.getLastPathSegment());
  }

  public static File getTempFile(Context context, String fileName) throws IOException {
    return File.createTempFile(fileName, null, context.getCacheDir());
  }

  private FileUtils() {}
}
