package com.cloud.ops.docker;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DockerContainerMapper  extends BaseQueryMapper<DockerContainer, DockerContainer> {

    @Select(" select names from docker_container where is_monitor=1 and service_id = #{serviceId} group by names order by sort desc ")
    List<String> getListByServiceId(Integer serviceId);
}
