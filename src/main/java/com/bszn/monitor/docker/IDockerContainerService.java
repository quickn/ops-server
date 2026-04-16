package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IDockerContainerService extends IService<DockerContainer> {
    List<DockerContainer> getByServiceIdAndDockerName(Integer serviceId, String dockerName);
}
