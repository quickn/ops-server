package com.bszn.monitor.agentConfig;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理服务配置 Service 实现类
 */
@Service
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig> implements AgentConfigService {

    @Override
    public List<AgentServerConfigVO> listConfigs(AgentConfigQuery query) {
        LambdaQueryWrapper<AgentConfig> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键字搜索（服务名）
        queryWrapper.like(StrUtil.isNotBlank(query.getKeywords()), 
                AgentConfig::getServiceName, query.getKeywords());
        
        // 是否存在Nginx
        queryWrapper.eq(query.getIsNginx() != null, 
                AgentConfig::getIsNginx, query.getIsNginx());
        
        // 服务id
        queryWrapper.eq(query.getServiceId() != null, 
                AgentConfig::getServiceId, query.getServiceId());
        
        // 按创建时间降序
        queryWrapper.orderByDesc(AgentConfig::getCreateTime);
        
        List<AgentConfig> entityList = baseMapper.selectList(queryWrapper);
        
        // 转换为 VO
        List<AgentServerConfigVO> voList = new ArrayList<>();
        if (CollUtil.isNotEmpty(entityList)) {
            for (AgentConfig entity : entityList) {
                AgentServerConfigVO vo = new AgentServerConfigVO();
                BeanUtils.copyProperties(entity, vo);
                voList.add(vo);
            }
        }
        
        return voList;
    }

    @Override
    public boolean deleteByIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return false;
        }
        
        List<Integer> idList = new ArrayList<>();
        String[] idArray = ids.split(",");
        for (String idStr : idArray) {
            try {
                idList.add(Integer.parseInt(idStr.trim()));
            } catch (NumberFormatException e) {
                // 忽略无效ID
            }
        }
        
        if (CollUtil.isEmpty(idList)) {
            return false;
        }
        
        return baseMapper.deleteBatchIds(idList) > 0;
    }

}
