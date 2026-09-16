package com.cloud.ops.encryption;

/**
 * @author wzh
 * @date 2026/1/5 8:30
 * @description: 加密接口
 */
public interface CryptoService {

    /**
     * 为用户生成密钥对
     */
    String generateKeyPair(KeyPairRequest request) throws Exception;

    /**
     * 加密
     */
    String encrypt(EncryptRequest request) throws Exception;

    /**
     * 解密
     */
    String decrypt(DecryptRequest request) throws Exception;

    /**
     * 获取公钥
     */
    String getPublicKey(Long userId);
}
