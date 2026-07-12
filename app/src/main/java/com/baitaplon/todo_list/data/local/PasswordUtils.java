package com.baitaplon.todo_list.data.local;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordUtils {

    private static final String HASH_ALGORITHM = "SHA-256";

    private PasswordUtils() {
    }

    public static String createSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashed = digest.digest((salt + password).getBytes());
            return bytesToHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tạo được hash mật khẩu", e);
        }
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        if (salt == null || expectedHash == null) {
            return false;
        }
        return hashPassword(password, salt).equals(expectedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}