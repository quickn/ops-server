package com.youlai.system.nginx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Liuyun on 2023-09-06 14:43
 **/
@Data
public class NginxFile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer fileId;
    private String fileName;
    private String filePath;

}
