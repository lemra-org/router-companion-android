/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.resources;

import android.util.Log;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

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

import static org.apache.commons.lang3.CharEncoding.UTF_8;

/**
 * An encrypted object
 */
public class Encrypted {

    private static final String LOG_TAG = Encrypted.class.getSimpleName();

    private static final String PRIVATE_SET_INT = "novells sky#yes ";
    private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";

    @Nullable
    private final Object originalData;

    @Nullable
    private final Object encryptedData;

    protected Encrypted() {
        this.originalData = null;
        this.encryptedData = null;
    }

    public Encrypted(@Nullable final byte[] dataToEncrypt) {
        this.originalData = dataToEncrypt;
        this.encryptedData = e(dataToEncrypt);
    }

    public Encrypted(@Nullable final String dataToEncrypt) {
        this.originalData = dataToEncrypt;
        this.encryptedData = e(dataToEncrypt);
    }

    @Nullable
    public Object getOriginalData() {
        return originalData;
    }

    @Nullable
    public Object getEncryptedData() {
        return encryptedData;
    }

    /**
     * Encrypt some data
     *
     * @param data the data to encrypt
     * @return the data encrypted, or <code>null</code> if no data was specified,
     * or if data could not encoded properly
     */
    @Nullable
    protected String e(@Nullable final String data) {

        if (data == null) {
            return null;
        }

        try {
            return new String(Base64.encodeBase64(e(data.getBytes(UTF_8))), UTF_8);

        } catch (UnsupportedEncodingException e1) {
            return null;
        }

    }

    /**
     * Decrypt some encrypted data
     *
     * @param encryptedData the encrypted data to decrypt
     * @return the data decrypted, or <code>null</code> if no data was specified,
     * or if data could not encoded properly
     */
    @Nullable
    protected String d(@Nullable final String encryptedData) {

        if (encryptedData == null) {
            return null;
        }

        try {
            final byte[] b64 = d(Base64.decodeBase64(encryptedData.getBytes(UTF_8)));

            return b64 != null ? new String(b64, UTF_8) : null;

        } catch (UnsupportedEncodingException e) {
            return null;
        }

    }

    @Nullable
    protected byte[] e(@Nullable final byte[] byteArrayToEncrypt) {

        if (byteArrayToEncrypt == null) {
            return null;
        }

        final Exception e;
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

            return ArrayUtils.addAll(ciphertext, iv);

        } catch (final IllegalBlockSizeException ibse) {
            e = ibse;
        } catch (final NoSuchPaddingException nspe) {
            e = nspe;
        } catch (final NoSuchAlgorithmException nsae) {
            e = nsae;
        } catch (final UnsupportedEncodingException uee) {
            e = uee;
        } catch (final InvalidKeyException ike) {
            e = ike;
        } catch (final InvalidAlgorithmParameterException iape) {
            e = iape;
        } catch (final BadPaddingException bpe) {
            e = bpe;
        }

        Log.e(LOG_TAG, "Failed to encrypt: " + e);
        throw new IllegalStateException(e);
    }

    @Nullable
    protected byte[] d(@Nullable final byte[] byteArrayToDecryptIV) {

        if (byteArrayToDecryptIV == null) {
            return null;
        }

        final Exception e;
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);

            byte[] iv = Arrays.copyOfRange(byteArrayToDecryptIV,
                    byteArrayToDecryptIV.length - 16,
                    byteArrayToDecryptIV.length);
            byte[] byteArrayToDecrypt = Arrays.copyOfRange(
                    byteArrayToDecryptIV, 0, byteArrayToDecryptIV.length - 16);
            byte[] aesKey = \"fake-key\";
            byte[] uncipheredtext;

            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            uncipheredtext = cipher.doFinal(byteArrayToDecrypt);

            return uncipheredtext;

        } catch (final IllegalBlockSizeException ibse) {
            e = ibse;
        } catch (final NoSuchPaddingException nspe) {
            e = nspe;
        } catch (final NoSuchAlgorithmException nsae) {
            e = nsae;
        } catch (final UnsupportedEncodingException uee) {
            e = uee;
        } catch (final InvalidKeyException ike) {
            e = ike;
        } catch (final InvalidAlgorithmParameterException iape) {
            e = iape;
        } catch (final BadPaddingException bpe) {
            e = bpe;
        }

        Log.e(LOG_TAG, "Failed to decrypt: " + e);
        throw new IllegalStateException(e);

    }
}
