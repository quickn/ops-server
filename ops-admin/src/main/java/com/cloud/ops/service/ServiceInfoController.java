package com.cloud.ops.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.system.common.result.Result;
import com.cloud.system.common.util.SecurityUtils;
import com.cloud.system.mapper.SysDeptServiceMapper;
import com.cloud.system.model.entity.SysDeptService;
import com.cloud.system.model.form.DeptServicesForm;
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
    SysDeptServiceMapper sysDeptServiceMapper;

    @ResponseBody
    @GetMapping("/listAll")
    public Result listAll() {
        List<ServiceInfo> list = serviceInfoMapper.query(ServiceInfoQuery.builder()
                .createBy(SecurityUtils.getUserId()).build());
        return Result.success(list);
    }

    @ResponseBody
    @GetMapping("/listPage")
    @McpTool(name = "serviceList", description = "服务列表")
    public Result serviceList(@ParameterObject ServiceInfoQuery systemInfoQuery) {
        systemInfoQuery.setCreateBy(SecurityUtils.getUserId());
        Page<ServiceInfo> list = serviceInfoMapper.queryPage(systemInfoQuery, serviceInfoMapper.getPage());
        return Result.success(list);
    }

    @ResponseBody
    @PostMapping("/save")
    public Result save(@RequestBody ServiceInfo serviceInfo) {
        // createBy 由 MyMetaObjectHandler 自动填充，无需手动设置
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
     * 根据当前登录用户所在部门获取关联的服务列表
     *
     * @return 服务列表
     */
    @ResponseBody
    @GetMapping("/listByDeptId")
    @McpTool(name = "serviceListByDept", description = "根据部门获取关联服务")
    public Result listByDeptId() {
        Long currentDeptId = SecurityUtils.getDeptId();
        if (currentDeptId == null) {
            return Result.success(Collections.emptyList());
        }
        List<ServiceInfo> list = sysDeptServiceMapper.listServiceByDeptId(currentDeptId);
        if (list.isEmpty()) {
            return listAll();
        }
        return Result.success(list);
    }

    /**
     * 获取指定部门已分配的服务列表
     *
     * @param deptId 目标部门ID
     * @return 服务列表
     */
    @ResponseBody
    @GetMapping("/listDeptServices/{deptId}")
    public Result listDeptServices(@PathVariable Long deptId) {
        List<ServiceInfo> list = sysDeptServiceMapper.listServiceByDeptId(deptId);

        return Result.success(list);
    }

    /**
     * 给部门分配服务（只能分配当前登录用户名下的服务）
     *
     * @param form deptId: 目标部门ID, serviceIds: 勾选的服务ID集合
     * @return Result
     */
    @ResponseBody
    @PostMapping("/assignServices")
    @Transactional(rollbackFor = Exception.class)
    public Result assignServices(@RequestBody DeptServicesForm form) {
        Long currentUserId = SecurityUtils.getUserId();
        Assert.notNull(currentUserId, "未获取到当前登录用户");
        Assert.notNull(form.getDeptId(), "目标部门ID不能为空");

        // 删除目标部门原有的服务关联
        sysDeptServiceMapper.delete(new LambdaQueryWrapper<SysDeptService>()
                .eq(SysDeptService::getDeptId, form.getDeptId()));

        if (CollectionUtil.isEmpty(form.getServiceIds())) {
            return Result.success();
        }

        // 只允许分配当前用户名下的服务
        List<ServiceInfo> ownedServices = serviceInfoMapper.query(ServiceInfoQuery.builder()
                .createBy(currentUserId).build());
        Map<Integer, ServiceInfo> ownedServiceMap = ownedServices.stream()
                .collect(Collectors.toMap(ServiceInfo::getId, Function.identity()));

        for (Integer serviceId : form.getServiceIds()) {
            ServiceInfo service = ownedServiceMap.get(serviceId);
            if (service == null) {
                continue;
            }
            SysDeptService deptService = new SysDeptService();
            deptService.setDeptId(form.getDeptId());
            deptService.setServiceId(service.getId());
            deptService.setServiceName(service.getName());
            deptService.setCreateTime(LocalDateTime.now());
            sysDeptServiceMapper.insert(deptService);
        }
        return Result.success();
    }

}
