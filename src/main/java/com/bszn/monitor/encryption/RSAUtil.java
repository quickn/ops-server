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

    // 2048位RSA的最大加密长度
    private static final int MAX_ENCRYPT_BLOCK = 245;
    // 2048位RSA的最大解密长度
    private static final int MAX_DECRYPT_BLOCK = 256;

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
     * 公钥加密（支持长文本分段加密）
     */
    public static String encrypt(String plainText, String publicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        byte[] data = plainText.getBytes("UTF-8");
        int inputLen = data.length;

        // 如果数据长度小于等于最大加密块，直接加密
        if (inputLen <= MAX_ENCRYPT_BLOCK) {
            byte[] encrypted = cipher.doFinal(data);
            return Base64.getEncoder().encodeToString(encrypted);
        }

        // 长文本需要分段加密
        StringBuilder result = new StringBuilder();
        int offset = 0;
        int segmentNum = 0;

        while (inputLen - offset > 0) {
            byte[] segment;
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                segment = new byte[MAX_ENCRYPT_BLOCK];
            } else {
                segment = new byte[inputLen - offset];
            }

            System.arraycopy(data, offset, segment, 0, segment.length);
            byte[] encryptedSegment = cipher.doFinal(segment);

            // 对每个加密片段进行Base64编码，并用分隔符连接
            if (segmentNum > 0) {
                result.append("|");
            }
            result.append(Base64.getEncoder().encodeToString(encryptedSegment));

            offset += segment.length;
            segmentNum++;
        }

        log.info("长文本分段加密，总长度：{}，分段数：{}", inputLen, segmentNum);
        return result.toString();
    }

    /**
     * 私钥解密（支持长文本分段解密）
     */
    public static String decrypt(String cipherText, String privateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PrivateKey priKey = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, priKey);

        // 检查是否是分段加密的结果（包含分隔符"|"）
        if (cipherText.contains("|")) {
            // 分段解密
            String[] segments = cipherText.split("\\|");
            StringBuilder result = new StringBuilder();

            for (String segment : segments) {
                byte[] encryptedSegment = Base64.getDecoder().decode(segment);
                byte[] decryptedSegment = cipher.doFinal(encryptedSegment);
                result.append(new String(decryptedSegment, "UTF-8"));
            }

            return result.toString();
        } else {
            // 单段解密
            byte[] encrypted = Base64.getDecoder().decode(cipherText);
            byte[] original = cipher.doFinal(encrypted);
            return new String(original, "UTF-8");
        }
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