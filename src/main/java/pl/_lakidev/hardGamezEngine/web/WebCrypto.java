package pl._lakidev.hardGamezEngine.web;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

class WebCrypto {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS  = 128;
    private static final String ALGORITHM  = "AES/GCM/NoPadding";

    private final SecretKey key;

    WebCrypto(String rawKey) {
        this.key = deriveKey(rawKey);
    }

    String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(iv.length + encrypted.length);
            buf.put(iv);
            buf.put(encrypted);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new RuntimeException("[WebEngine] Encryption failed", e);
        }
    }

    String decrypt(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            ByteBuffer buf = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("[WebEngine] Decryption failed — wrong key or tampered data", e);
        }
    }

    private static SecretKey deriveKey(String rawKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new RuntimeException("[WebEngine] Key derivation failed", e);
        }
    }
}
