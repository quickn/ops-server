package com.cloud.receiver.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * 创建和更新时间的统一
 * Created by Liuyun on 2021-11-22 10:55
 **/
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        if (metaObject.hasGetter("createTime")) {
            Object created = getFieldValByName("createTime", metaObject);
            if (null == created) {
                // 字段为空，可以进行填充
                this.setNewFieldValByName("createTime", new Date(), metaObject);
            }
        }
        if (metaObject.hasGetter("updateTime")) {
            Object created = getFieldValByName("updateTime", metaObject);
            if (null == created) {
                // 字段为空，可以进行填充
                this.setNewFieldValByName("updateTime", new Date(), metaObject);
            }
        }
        //   IMyMetaObjectHandler iMyMetaObjectHandler = SpringUtil.getBean(IMyMetaObjectHandler.class);
        //  if (iMyMetaObjectHandler != null) iMyMetaObjectHandler.insertFill(this, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新数据时，直接更新字段
        this.setNewFieldValByName("updateTime", new Date(), metaObject);
        //    IMyMetaObjectHandler iMyMetaObjectHandler = SpringUtil.getBean(com.cloud.collector.park.common.config.mybatis.IMyMetaObjectHandler.class);
        //  if (iMyMetaObjectHandler != null) iMyMetaObjectHandler.updateFill(this, metaObject);
    }

    public MetaObjectHandler setNewFieldValByName(String fieldName, Object fieldVal, MetaObject metaObject) {
        if (metaObject.hasGetter(fieldName)) {
            if ("updateTime".equals(fieldName) && fieldVal != null) {
                return this.setFieldValByName(fieldName, fieldVal, metaObject);
            }
            Object object = metaObject.getValue(fieldName);//以传入的为准
            if (object != null) {
                return this;
            }
            return this.setFieldValByName(fieldName, fieldVal, metaObject);
        }
        return this;
    }
}
