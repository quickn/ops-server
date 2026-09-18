package com.cloud.ops.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.result.Result;
import com.cloud.system.common.util.SecurityUtils;
import com.cloud.system.mapper.SysUserServiceMapper;
import com.cloud.system.model.entity.SysUserService;
import com.cloud.system.model.form.UserServicesForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Tag(name = "服务")
@RestController
@RequestMapping(value = "/serviceInfo")
@Slf4j
public class ServiceInfoController {
    @Resource
    ServiceInfoMapper serviceInfoMapper;

    @Resource
    ServiceInfoService serviceInfoService;

    @Resource
    SysUserServiceMapper sysUserServiceMapper;

    @ResponseBody
    @GetMapping("/listAll")
    public Result listAll() {
        List<ServiceInfo> list = serviceInfoMapper.query(ServiceInfoQuery.builder()
                .userId(SecurityUtils.getUserId()).build());
        return Result.success(list);
    }

    @ResponseBody
    @GetMapping("/listPage")
    @McpTool(name = "serviceList", description = "服务列表")
    public Result serviceList(@ParameterObject ServiceInfoQuery systemInfoQuery) {
        systemInfoQuery.setUserId(SecurityUtils.getUserId());
        Page<ServiceInfo> list = serviceInfoMapper.queryPage(systemInfoQuery, serviceInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @PostMapping("/save")
    public Result save(@RequestBody ServiceInfo serviceInfo) {
        serviceInfo.setUserId(SecurityUtils.getUserId());
        serviceInfoService.saveOrUpdate(serviceInfo);
        return Result.success();
    }


    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result delete(@PathVariable Integer ids) {
        serviceInfoService.removeById(ids);
        return Result.success();
    }

    /**
     * 根据用户ID获取关联的服务列表
     *
     * @return 服务列表
     */
    @ResponseBody
    @GetMapping("/listByUserId")
    @McpTool(name = "serviceListByUser", description = "根据用户获取关联服务")
    public Result listByUserId() {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null) {
            return Result.success(Collections.emptyList());
        }
        List<ServiceInfo> list = sysUserServiceMapper.listServiceByUserId(currentUserId);
        return Result.success(list);
    }

    /**
     * 获取指定用户已分配的服务列表
     *
     * @param userId 目标用户ID
     * @return 服务列表
     */
    @ResponseBody
    @GetMapping("/listUserServices/{userId}")
    public Result listUserServices(@PathVariable Long userId) {
        List<ServiceInfo> list = sysUserServiceMapper.listServiceByUserId(userId);
        return Result.success(list);
    }

    /**
     * 给用户分配服务（只能分配当前登录用户名下的服务）
     *
     * @param form userId: 目标用户ID, serviceIds: 勾选的服务ID集合
     * @return Result
     */
    @ResponseBody
    @PostMapping("/assignServices")
    @Transactional(rollbackFor = Exception.class)
    public Result assignServices(@RequestBody UserServicesForm form) {
        Long currentUserId = SecurityUtils.getUserId();
        Assert.notNull(currentUserId, "未获取到当前登录用户");
        Assert.notNull(form.getUserId(), "目标用户ID不能为空");

        // 删除目标用户原有的服务关联
        sysUserServiceMapper.delete(new LambdaQueryWrapper<SysUserService>()
                .eq(SysUserService::getUserId, form.getUserId()));

        if (CollectionUtil.isEmpty(form.getServiceIds())) {
            return Result.success();
        }

        // 只允许分配当前用户名下的服务
        List<ServiceInfo> ownedServices = serviceInfoMapper.query(ServiceInfoQuery.builder()
                .userId(currentUserId).build());
        Map<Integer, ServiceInfo> ownedServiceMap = ownedServices.stream()
                .collect(Collectors.toMap(ServiceInfo::getId, Function.identity()));

        for (Integer serviceId : form.getServiceIds()) {
            ServiceInfo service = ownedServiceMap.get(serviceId);
            if (service == null) {
                continue;
            }
            SysUserService userServices = new SysUserService();
            userServices.setUserId(form.getUserId());
            userServices.setServiceId(service.getId());
            userServices.setServiceName(service.getName());
            userServices.setCreateTime(LocalDateTime.now());
            sysUserServiceMapper.insert(userServices);
        }
        return Result.success();
    }

}
