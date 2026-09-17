/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic AES encryption utility for TAPI SBI controller passwords.
 * Uses AES-GCM encryption for secure password storage.
 */
public final class BasicEncryptionUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BasicEncryptionUtil.class);

    // AES-GCM parameters
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits

    // Fixed key for basic encryption. TODO: Make configurable
    private static final String FIXED_KEY = "TapiSbiCtrl2026!"; // 16 bytes for AES-128

    private BasicEncryptionUtil() {
        // Utility class
    }

    /**
     * Encrypts a plain text password using AES-GCM encryption.
     *
     * @param plainPassword The plain text password to encrypt
     * @return Base64 encoded encrypted password with IV prepended
     */
    public static String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            LOG.warn("Attempted to encrypt null or empty password");
            return "";
        }

        try {
            // Create secret key from fixed key
            SecretKeySpec secretKey = new SecretKeySpec(FIXED_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the password
            byte[] encryptedData = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            // Return Base64 encoded result
            String encrypted = Base64.getEncoder().encodeToString(encryptedWithIv);
            LOG.debug("Password encrypted successfully using AES-GCM, length: {}", encrypted.length());
            return encrypted;

        } catch (GeneralSecurityException e) {
            LOG.error("Failed to encrypt password with AES-GCM", e);
            // Fallback to Base64 encoding if AES fails
            LOG.warn("Falling back to Base64 encoding for password");
            return Base64.getEncoder().encodeToString(plainPassword.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Decrypts an encrypted password using AES-GCM decryption.
     *
     * @param encryptedPassword The Base64 encoded encrypted password
     * @return Decrypted plain text password
     */
    public static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            LOG.warn("Attempted to decrypt null or empty password");
            return "";
        }

        try {
            // Decode from Base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedPassword);

            // Check minimum length (IV + some encrypted data + auth tag)
            if (encryptedWithIv.length < GCM_IV_LENGTH + GCM_TAG_LENGTH + 1) {
                LOG.warn("Encrypted password too short, attempting Base64 decode fallback");
                return new String(Base64.getDecoder().decode(encryptedPassword), StandardCharsets.UTF_8);
            }

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            // Create secret key from fixed key
            SecretKeySpec secretKey = new SecretKeySpec(FIXED_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            String decrypted = new String(decryptedData, StandardCharsets.UTF_8);
            LOG.debug("Password decrypted successfully using AES-GCM");
            return decrypted;

        } catch (GeneralSecurityException e) {
            LOG.error("Failed to decrypt password with AES-GCM: {}, attempting Base64 decode fallback",
                    e.getMessage());
            // Fallback to Base64 decoding (for backward compatibility)
            try {
                return new String(Base64.getDecoder().decode(encryptedPassword), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException fallbackException) {
                LOG.error("Fallback Base64 decode also failed: {}", fallbackException.getMessage());
                return encryptedPassword; // Return as-is if all fails
            }
        }
    }
}
