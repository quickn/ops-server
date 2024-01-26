package com.youlai.monitor.docker;

import com.youlai.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DockerContainerMapper  extends BaseQueryMapper<DockerContainer, DockerContainer> {

}
