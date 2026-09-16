package com.cloud.ops.docker;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DockerContainerServiceImpl extends ServiceImpl<DockerContainerMapper, DockerContainer> implements IDockerContainerService {

    @Override
    public List<DockerContainer> getByServiceIdAndDockerName(Integer serviceId, String dockerName) {
        return this.baseMapper.selectList(Wrappers.<DockerContainer>lambdaQuery().eq(DockerContainer::getServiceId, serviceId).eq(DockerContainer::getNames,
                dockerName));
    }
}
