package com.cloud.ops.email;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created by Liuyun on 2023-09-11 14:59
 **/
@Mapper
public interface MailConfigMapper extends BaseQueryMapper<MailConfig, MailConfig> {
}
