package org.ohdsi.webapi.util;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;

public class EncryptionUtils {

    public static String encryptionKey;

    public static String encrypt(String data) throws Exception {
        if (data == null) {
            return null;
        }
        var salt = KeyGenerators.string().generateKey(); // generates a random 8-byte salt that is then hex-encoded
        var textEncryptor = Encryptors.text(encryptionKey, salt);
        var encryptedData = textEncryptor.encrypt(data);
        return salt + ":" + encryptedData; // store salt along with encrypted data
    }

    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null) {
            return null;
        }
        String[] parts = encryptedData.split(":", 2);
        String salt = parts[0];
        String encryptedText = parts[1];
        var textEncryptor = Encryptors.text(encryptionKey, salt);
        return textEncryptor.decrypt(encryptedText);
    }
}
