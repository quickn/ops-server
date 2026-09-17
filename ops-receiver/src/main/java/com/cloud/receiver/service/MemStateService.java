package com.cloud.receiver.service;

import com.cloud.base.util.MyIdWorker;
import com.cloud.ops.system.MemState;
import com.cloud.ops.system.MemStateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemStateService {

    @Resource
    private MemStateMapper memStateMapper;

    public void saveRecord(List<MemState> recordList) throws Exception {
        if (recordList.isEmpty()) {
            return;
        }
        for (MemState as : recordList) {
            as.setId(MyIdWorker.getId());
            memStateMapper.insert(as);
        }
    }


}
