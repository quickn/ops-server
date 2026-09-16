package com.cloud.ops.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author wzh
 * @date 2026/1/5 8:31
 * @description: 加密业务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoServiceImpl implements CryptoService {

    private final UserKeyMapper userKeyMapper;

    @Value("${decrypt.password}")
    private String defaultPassword;

    @Value("${decrypt.public-key}")
    private String publicKey;

    @Override
    @Transactional
    public String generateKeyPair(KeyPairRequest request) throws Exception {
        Long userId = request.getUserId();

        // 检查是否已存在
        UserKey exist = userKeyMapper.selectByUserId(userId);

        // 生成密钥对
        KeyPair keyPair = RSAUtil.generateKeyPair();
        String publicKey = RSAUtil.getPublicKey(keyPair);
        String privateKey = RSAUtil.getPrivateKey(keyPair);

        // 加密私钥存储
        String encryptedPrivateKey = RSAUtil.encryptPrivateKey(privateKey, defaultPassword);

        UserKey userKey = new UserKey();
        userKey.setUserId(userId);
        userKey.setPublicKey(publicKey);
        userKey.setPrivateKey(encryptedPrivateKey);
        userKey.setCreateTime(LocalDateTime.now());
        userKey.setUpdateTime(LocalDateTime.now());

        if (exist != null) {
            userKey.setId(exist.getId());
            userKeyMapper.updateById(userKey);
            log.info("更新用户 {} 的密钥对", userId);
        } else {
            userKeyMapper.insert(userKey);
            log.info("为用户 {} 生成密钥对", userId);
        }

        return publicKey;
    }

    @Override
    public String encrypt(EncryptRequest request) throws Exception {
        Long userId = request.getUserId();
        String plainText = request.getPlainText();

        // 获取用户公钥
        String publicKey = getPublicKey(userId);
        if (!StringUtils.hasText(publicKey)) {
            throw new RuntimeException("用户 " + userId + " 未生成密钥对");
        }

        // 加密
        return RSAUtil.encrypt(plainText, publicKey);
    }

    @Override
    public String decrypt(DecryptRequest request) throws Exception {
        Long userId = request.getUserId();
        String cipherText = request.getCipherText();

        // 获取用户密钥记录
        UserKey userKey = userKeyMapper.selectByUserId(userId);

        if (userKey == null) {
            throw new RuntimeException("用户 " + userId + " 未生成密钥对");
        }

        // 解密私钥
        String privateKey = RSAUtil.decryptPrivateKey(userKey.getPrivateKey(), defaultPassword);

        // 解密数据
        return RSAUtil.decrypt(cipherText, privateKey);
    }

    @Override
    public String getPublicKey(Long userId) {
        if (Objects.isNull(userId)) {
            return publicKey;
        }
        UserKey userKey = userKeyMapper.selectByUserId(userId);
        return userKey != null ? userKey.getPublicKey() : publicKey;
    }

}