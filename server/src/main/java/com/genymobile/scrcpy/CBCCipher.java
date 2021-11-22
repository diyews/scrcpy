package com.genymobile.scrcpy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CBCCipher {
    static Cipher cipher;
    static String cipherKey = "0123456789abcdef";
    static SecretKeySpec secretKeySpec = new SecretKeySpec(cipherKey.getBytes(StandardCharsets.UTF_8), "AES");

    static {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    /* encrypt take 1-5ms */
    static byte[] encrypt(byte[] byteArray) {
        final byte[] iv = new byte[cipherKey.length()];
        new SecureRandom().nextBytes(iv);
        final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        try {
            assert cipher != null;
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            final byte[] encrypted = cipher.doFinal(byteArray);
            ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
            encryptedStream.write(iv);
            encryptedStream.write(encrypted);
            return encryptedStream.toByteArray();
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
