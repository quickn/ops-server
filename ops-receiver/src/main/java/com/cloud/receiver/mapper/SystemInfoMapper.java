package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.SystemInfo;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemInfoMapper extends BaseMapper<SystemInfo> {


    @Select(" select sys.* from system_info sys left join service_info s on sys.service_id=s.id where s.is_monitor=1 ")
    List<SystemInfo> listAll();
}
