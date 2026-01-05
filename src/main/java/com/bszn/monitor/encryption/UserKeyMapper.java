package com.bszn.monitor.encryption;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author wzh
 * @date 2026/1/5 8:27
 * @description: 用户秘钥mapper
 */
@Mapper
public interface UserKeyMapper extends BaseMapper<UserKey> {

    @Select("SELECT * FROM user_key WHERE user_id = #{userId}")
    UserKey selectByUserId(@Param("userId") Long userId);
}
