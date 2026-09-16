package com.cloud.ops.encryption;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wzh
 * @date 2026/1/5 8:28
 * @description: dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptRequest {

    @NotBlank(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "待加密文本不能为空")
    private String plainText;

}
