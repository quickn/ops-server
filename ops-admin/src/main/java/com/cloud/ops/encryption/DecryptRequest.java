package com.cloud.ops.encryption;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/5 8:29
 * @description: dto
 */
@Data
public class DecryptRequest {
    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "待解密文本不能为空")
    private String cipherText;
}
