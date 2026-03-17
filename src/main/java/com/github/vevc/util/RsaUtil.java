package com.github.vevc.util;

import com.github.vevc.constant.AppConst;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA utility for config encryption/decryption
 * @author vevc
 */
public final class RsaUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final int MAX_DECRYPT_BLOCK = 128;

    public static String encryptByPublicKey(String data, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] dataBytes = data.getBytes();
        int inputLen = dataBytes.length;
        int offLen = 0;
        byte[] resultBytes = new byte[0];

        for (int i = 0; inputLen - offLen > 0; offLen = i) {
            byte[] cache;
            if (inputLen - offLen > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(dataBytes, offLen, MAX_ENCRYPT_BLOCK);
                i = offLen + MAX_ENCRYPT_BLOCK;
            } else {
                cache = cipher.doFinal(dataBytes, offLen, inputLen - offLen);
                i = inputLen;
            }
            byte[] temp = new byte[resultBytes.length + cache.length];
            System.arraycopy(resultBytes, 0, temp, 0, resultBytes.length);
            System.arraycopy(cache, 0, temp, resultBytes.length, cache.length);
            resultBytes = temp;
        }

        return Base64.getEncoder().encodeToString(resultBytes);
    }

    public static String decryptByPrivateKey(String encryptedData, String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] dataBytes = Base64.getDecoder().decode(encryptedData);
        int inputLen = dataBytes.length;
        int offLen = 0;
        byte[] resultBytes = new byte[0];

        for (int i = 0; inputLen - offLen > 0; offLen = i) {
            byte[] cache;
            if (inputLen - offLen > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(dataBytes, offLen, MAX_DECRYPT_BLOCK);
                i = offLen + MAX_DECRYPT_BLOCK;
            } else {
                cache = cipher.doFinal(dataBytes, offLen, inputLen - offLen);
                i = inputLen;
            }
            byte[] temp = new byte[resultBytes.length + cache.length];
            System.arraycopy(resultBytes, 0, temp, 0, resultBytes.length);
            System.arraycopy(cache, 0, temp, resultBytes.length, cache.length);
            resultBytes = temp;
        }

        return new String(resultBytes);
    }

    private RsaUtil() {
        throw new IllegalStateException("Utility class");
    }
}
