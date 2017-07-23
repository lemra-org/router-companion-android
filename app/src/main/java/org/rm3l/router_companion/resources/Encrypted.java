/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
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
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.router_companion.resources;

import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//import org.apache.commons.codec.binary.Base64;

import static com.google.common.base.Charsets.UTF_8;

/**
 * An encrypted object
 */
public class Encrypted {

  private static final String LOG_TAG = Encrypted.class.getSimpleName();

  //TODO Look for a better way to encrypt data
  private static final String PRIVATE_SET_INT = "novells sky#yes ";
  private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
  private static final String AES = "AES";

  /**
   * Encrypt some data
   *
   * @param data the data to encrypt
   * @return the data encrypted, or <code>null</code> if no data was specified,
   * or if data could not encoded properly
   */
  @Nullable public static String e(@Nullable final String data) {

    if (data == null) {
      return null;
    }

    return new String(Base64.encode(e(data.getBytes(UTF_8)), Base64.DEFAULT), UTF_8);
  }

  /**
   * Decrypt some encrypted data
   *
   * @param encryptedData the encrypted data to decrypt
   * @return the data decrypted, or <code>null</code> if no data was specified,
   * or if data could not encoded properly
   */
  @Nullable public static String d(@Nullable final String encryptedData) {

    if (encryptedData == null) {
      return null;
    }

    final byte[] b64 = d(Base64.decode(encryptedData.getBytes(UTF_8), Base64.DEFAULT));
    return b64 != null ? new String(b64, UTF_8) : null;
  }

  @Nullable public static byte[] e(@Nullable final byte[] byteArrayToEncrypt) {

    if (byteArrayToEncrypt == null) {
      return null;
    }

    Exception e;
    try {
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);

      byte[] iv = new byte[16];
      byte[] aesKey = \"fake-key\";
      byte[] ciphertext;

      SecureRandom secureRandom = new SecureRandom();
      secureRandom.nextBytes(iv);

      SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
      ciphertext = cipher.doFinal(byteArrayToEncrypt);

      final byte[] result = Arrays.copyOf(ciphertext, ciphertext.length + iv.length);
      System.arraycopy(iv, 0, result, ciphertext.length, iv.length);
      //return ArrayUtils.addAll(ciphertext, iv);
      return result;
    } catch (final IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | BadPaddingException ibse) {
      e = ibse;
    }

    Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to encrypt: " + e);
    throw new IllegalStateException(e);
  }

  @Nullable public static byte[] d(@Nullable final byte[] byteArrayToDecryptIV) {

    if (byteArrayToDecryptIV == null) {
      return null;
    }

    Exception e;
    try {
      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);

      byte[] iv = Arrays.copyOfRange(byteArrayToDecryptIV, byteArrayToDecryptIV.length - 16,
          byteArrayToDecryptIV.length);
      byte[] byteArrayToDecrypt =
          Arrays.copyOfRange(byteArrayToDecryptIV, 0, byteArrayToDecryptIV.length - 16);
      byte[] aesKey = \"fake-key\";
      byte[] uncipheredtext;

      SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
      uncipheredtext = cipher.doFinal(byteArrayToDecrypt);

      return uncipheredtext;
    } catch (final IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ibse) {
      e = ibse;
    }

    Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to decrypt: " + e);
    throw new IllegalStateException(e);
  }
}
