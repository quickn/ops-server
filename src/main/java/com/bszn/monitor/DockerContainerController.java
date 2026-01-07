package com.bszn.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.monitor.agent.AgentConfig;
import com.bszn.monitor.agent.AgentConfigService;
import com.bszn.monitor.docker.DockerContainer;
import com.bszn.monitor.docker.DockerContainerMapper;
import com.bszn.monitor.docker.DockerQueryPage;
import com.bszn.monitor.msg.IMsgService;
import com.bszn.system.common.result.Result;
import com.bszn.system.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "docker容器")
@RestController
@RequestMapping(value = "/docker/container")
@Slf4j
public class DockerContainerController {

    @Resource
    DockerContainerMapper dockerContainerMapper;

    @Resource
    AgentConfigService agentConfigService;

    @Resource
    IMsgService iMsgService;

    @GetMapping(value = "/list")
    public Result<Page<DockerContainer>> list(@ParameterObject DockerQueryPage dockerQueryPage) {
        Page<DockerContainer> pageInfo = dockerContainerMapper.queryPage(dockerQueryPage, dockerContainerMapper.getPage());
        return Result.success(pageInfo);
    }

    @PostMapping(value = "updateById")
    public Result updateById(@RequestBody DockerContainer dockerContainer) {
        dockerContainerMapper.updateById(dockerContainer);
        return Result.success();
    }

    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result deleteById(@PathVariable Integer ids) {
        dockerContainerMapper.deleteById(ids);
        return Result.success();
    }


    @GetMapping(value = "/restart/{id}")
    public Result restart(@PathVariable Long id) {
        DockerContainer dockerContainer = dockerContainerMapper.selectById(id);
        AgentConfig agentConfig = agentConfigService.getByServiceIdAndHost(dockerContainer.getServiceId(), dockerContainer.getHostname());
        String msg = iMsgService.sendCMDMsgAndResponse(SecurityUtils.getUserId(), agentConfig.getId(), "docker restart " + dockerContainer.getNames(), 60);
        return Result.success(msg);
    }

    @GetMapping(value = "/stop/{id}")
    public Result stop(@PathVariable Long id) {
        DockerContainer dockerContainer = dockerContainerMapper.selectById(id);
        AgentConfig agentConfig = agentConfigService.getByServiceIdAndHost(dockerContainer.getServiceId(), dockerContainer.getHostname());
        String msg = iMsgService.sendCMDMsgAndResponse(SecurityUtils.getUserId(), agentConfig.getId(), "docker stop " + dockerContainer.getNames(), 60);
        return Result.success(msg);
    }

    @GetMapping(value = "/getListByServiceId/{serviceId}")
    public Result<List<String>> getListByServiceId(@PathVariable Integer serviceId) {
        List<String> list = dockerContainerMapper.getListByServiceId(serviceId);
        return Result.success(list);
    }

}
