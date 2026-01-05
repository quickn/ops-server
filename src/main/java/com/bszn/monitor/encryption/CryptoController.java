package com.bszn.monitor.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * @author wzh
 * @date 2026/1/5 8:34
 * @description: 加密控制器
 */
@Slf4j
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    /**
     * 生成密钥对
     */
    @PostMapping("/generate")
    public String generateKeyPair(@Validated @RequestBody KeyPairRequest request) {
        try {
            return cryptoService.generateKeyPair(request);
        } catch (Exception e) {
            log.error("生成密钥对失败", e);
            return "生成密钥对失败: " + e.getMessage();
        }
    }

    /**
     * 加密
     */
    @PostMapping("/encrypt")
    public String encrypt(@Validated @RequestBody EncryptRequest request) {
        try {
            return cryptoService.encrypt(request);
        } catch (Exception e) {
            log.error("加密失败", e);
            return "加密失败: " + e.getMessage();
        }
    }

    /**
     * 解密
     */
    @PostMapping("/decrypt")
    public String decrypt(@Validated @RequestBody DecryptRequest request) {
        try {
            return cryptoService.decrypt(request);
        } catch (Exception e) {
            log.error("解密失败", e);
            return "解密失败: " + e.getMessage();
        }
    }

    /**
     * 获取公钥
     */
    @GetMapping("/publicKey/{userId}")
    public String getPublicKey(@PathVariable Long userId) {
        try {
            String publicKey = cryptoService.getPublicKey(userId);
            return publicKey != null ? publicKey : "用户未生成密钥对";
        } catch (Exception e) {
            log.error("获取公钥失败", e);
            return "获取公钥失败: " + e.getMessage();
        }
    }

    /**
     * 快速使用示例
     */
    @GetMapping("/quick/{userId}")
    public String quickDemo(@PathVariable Long userId) {
        try {
            // 1. 生成密钥对
            KeyPairRequest request = new KeyPairRequest();
            request.setUserId(userId);
            String publicKey = cryptoService.generateKeyPair(request);

            // 2. 加密示例数据
            EncryptRequest encryptRequest = new EncryptRequest();
            encryptRequest.setUserId(userId);
            encryptRequest.setPlainText("Hello, RSA Encryption!");
            String encrypted = cryptoService.encrypt(encryptRequest);

            // 3. 解密
            DecryptRequest decryptRequest = new DecryptRequest();
            decryptRequest.setUserId(userId);
            decryptRequest.setCipherText(encrypted);
            String decrypted = cryptoService.decrypt(decryptRequest);

            return String.format("公钥: %s\n加密后: %s\n解密后: %s",
                    publicKey.substring(0, 50) + "...",
                    encrypted,
                    decrypted);

        } catch (Exception e) {
            return "操作失败: " + e.getMessage();
        }
    }
}
