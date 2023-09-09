package com.youlai.heath;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.system.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;


@Tag(name = "接口健康监控")
@RestController
@RequestMapping("/heath")
public class HeathMonitorController {

    @Resource
    private HeathMonitorService heathMonitorService;

    /**
     * 根据条件查询心跳监控列表
     *
     * @return
     */
    @GetMapping(value = "/listHeaths")
    public Result<Page<HeathMonitor>> listHeaths(@ParameterObject HeathQueryPage heathQueryPage) {
        Page<HeathMonitor> pageInfo = heathMonitorService.queryPage(heathQueryPage);
        return Result.success(pageInfo);
    }

    /**
     * 保存心跳监控信息
     *
     * @return
     */
    @PostMapping(value = "save")
    public Result saveHeathMonitor(@RequestBody HeathMonitor heathMonitor) {
        heathMonitorService.saveOrUpdate(heathMonitor);
        return Result.success();
    }


    /**
     * 删除心跳监控
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/delete/{ids}")
    @ResponseBody
    public Result delete(@PathVariable Integer ids) {
        heathMonitorService.removeById(ids);
        return Result.success();
    }
}
