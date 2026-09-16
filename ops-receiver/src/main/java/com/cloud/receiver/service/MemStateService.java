package com.cloud.receiver.service;

import com.cloud.receiver.entity.MemState;
import com.cloud.receiver.mapper.MemStateMapper;
import com.cloud.receiver.util.MyIdWorker;
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
