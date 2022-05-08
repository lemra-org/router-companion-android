/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */

package org.rm3l.router_companion.resources;

import static com.google.common.base.Charsets.UTF_8;

import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.common.base.Strings;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.common.utils.ExceptionUtils;
import org.rm3l.router_companion.utils.kotlin.ContextUtils;

/** An encrypted object */
public class Encrypted {

  private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";

  private static final String AES = "AES";

  public static final int PRIV_KEY_MIN_LENGTH = 16;

  /**
   * Decrypt some encrypted data
   *
   * @param encryptedData the encrypted data to decrypt
   * @return the data decrypted, or <code>null</code> if no data was specified, or if data could not
   *     encoded properly
   */
  @Nullable
  public static String d(@Nullable final String encryptedData) {

    if (encryptedData == null) {
      return null;
    }

    final byte[] b64 = d(Base64.decode(encryptedData.getBytes(UTF_8), Base64.DEFAULT));
    return b64 != null ? new String(b64, UTF_8) : null;
  }

  @Nullable
  public static byte[] d(@Nullable final byte[] byteArrayToDecryptIV) {

    if (byteArrayToDecryptIV == null) {
      return null;
    }

    Exception e;
    try {
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);

      byte[] iv =
          Arrays.copyOfRange(
              byteArrayToDecryptIV,
              byteArrayToDecryptIV.length - PRIV_KEY_MIN_LENGTH,
              byteArrayToDecryptIV.length);
      byte[] byteArrayToDecrypt =
          Arrays.copyOfRange(
              byteArrayToDecryptIV, 0, byteArrayToDecryptIV.length - PRIV_KEY_MIN_LENGTH);
      byte[] aesKey = getEncryptionKey().getBytes(UTF_8);
      byte[] uncipheredtext;

      SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
      uncipheredtext = cipher.doFinal(byteArrayToDecrypt);

      return uncipheredtext;
    } catch (final IllegalBlockSizeException
        | BadPaddingException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException ibse) {
      e = ibse;
    }

    FirebaseCrashlytics.getInstance().log("Failed to decrypt: " + e);
    Toast.makeText(
            RouterCompanionApplication.getCurrentActivity(),
            "Could not decrypt data. Error message is: "
                + ExceptionUtils.getRootCause(e)
                + ". The issue will be reported (depending on your preferences).",
            Toast.LENGTH_LONG)
        .show();
    return null;
  }

  @Nullable
  public static byte[] e(@Nullable final byte[] byteArrayToEncrypt) {

    if (byteArrayToEncrypt == null) {
      return null;
    }

    Exception e;
    try {
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);

      byte[] iv = new byte[PRIV_KEY_MIN_LENGTH];
      byte[] aesKey = getEncryptionKey().getBytes(UTF_8);
      byte[] ciphertext;

      SecureRandom secureRandom = new SecureRandom();
      secureRandom.nextBytes(iv);

      SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
      ciphertext = cipher.doFinal(byteArrayToEncrypt);

      final byte[] result = Arrays.copyOf(ciphertext, ciphertext.length + iv.length);
      System.arraycopy(iv, 0, result, ciphertext.length, iv.length);
      // return ArrayUtils.addAll(ciphertext, iv);
      return result;
    } catch (final IllegalBlockSizeException
        | NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | BadPaddingException ibse) {
      e = ibse;
    }

    FirebaseCrashlytics.getInstance().log("Failed to encrypt: " + e);
    throw new IllegalStateException(e);
  }

  private static String getEncryptionKey() {
    String key =
        Objects.requireNonNull(
            ContextUtils.getConfigProperty(
                RouterCompanionApplication.getCurrentActivity(), "ENCRYPTION_KEY", ""));
    final int length = key.length();
    // Add padding if necessary
    if (length < PRIV_KEY_MIN_LENGTH) {
      key += Strings.repeat(" ", PRIV_KEY_MIN_LENGTH - length);
    } else {
      if (length % PRIV_KEY_MIN_LENGTH != 0) {
        // Pad up to the nearest multiplier
        key += Strings.repeat(" ", PRIV_KEY_MIN_LENGTH - length % PRIV_KEY_MIN_LENGTH);
      }
    }
    Log.d("XXX", "[" + key + "], length=" + key.length());
    return key;
  }

  /**
   * Encrypt some data
   *
   * @param data the data to encrypt
   * @return the data encrypted, or <code>null</code> if no data was specified, or if data could not
   *     encoded properly
   */
  @Nullable
  public static String e(@Nullable final String data) {

    if (data == null) {
      return null;
    }

    return new String(Base64.encode(e(data.getBytes(UTF_8)), Base64.DEFAULT), UTF_8);
  }
}
