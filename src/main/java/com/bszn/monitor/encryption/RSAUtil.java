package com.bszn.monitor.encryption;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RSAUtil {
    
    private static final String RSA = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    
    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA);
        keyPairGen.initialize(KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }
    
    /**
     * 获取Base64编码的公钥
     */
    public static String getPublicKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
    
    /**
     * 获取Base64编码的私钥
     */
    public static String getPrivateKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }
    
    /**
     * 公钥加密
     */
    public static String encrypt(String plainText, String publicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * 私钥解密
     */
    public static String decrypt(String cipherText, String privateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PrivateKey priKey = keyFactory.generatePrivate(keySpec);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        
        byte[] encrypted = Base64.getDecoder().decode(cipherText);
        byte[] original = cipher.doFinal(encrypted);
        return new String(original);
    }
    
    /**
     * 简单加密私钥（实际项目需要用更安全的方式）
     */
    public static String encryptPrivateKey(String privateKey, String password) {
        // 这里简单示例，实际应该使用AES等加密算法
        return Base64.getEncoder().encodeToString((privateKey + ":" + password).getBytes());
    }
    
    /**
     * 解密私钥
     */
    public static String decryptPrivateKey(String encryptedPrivateKey, String password) {
        String decoded = new String(Base64.getDecoder().decode(encryptedPrivateKey));
        String[] parts = decoded.split(":");
        if (parts.length == 2 && parts[1].equals(password)) {
            return parts[0];
        }
        throw new RuntimeException("私钥密码错误");
    }
}