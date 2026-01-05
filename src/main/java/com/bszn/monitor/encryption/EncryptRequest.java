package com.bszn.monitor.encryption;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author wzh
 * @date 2026/1/5 8:28
 * @description: dto
 */
@Data
public class EncryptRequest {

    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "待加密文本不能为空")
    private String plainText;

}
