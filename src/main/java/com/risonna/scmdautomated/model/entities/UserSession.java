package com.risonna.scmdautomated.model.entities;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class UserSession {
    private static UserSession instance;
    private String username;
    private String encryptedPassword;
    private boolean isLoggedIn;
    private final SecretKey secretKey;

    private UserSession() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            this.secretKey = keyGen.generateKey();
            this.setLoggedIn(false);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing secret key", e);
        }
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.encryptedPassword = encryptPassword(password);
        this.isLoggedIn = true;
    }

    public void logout() {
        this.username = null;
        this.encryptedPassword = null;
        this.isLoggedIn = false;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    public void setLoggedIn(boolean isLoggedIn){
        this.isLoggedIn = isLoggedIn;
    }

    public String getUsername() {
        return username;
    }

    private String encryptPassword(String password) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes());
            System.out.println(Base64.getEncoder().encodeToString(encryptedBytes));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }

    public String decryptPassword() {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            System.out.println("Decrypted password: " + new String(decryptedBytes));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password", e);
        }
    }
}