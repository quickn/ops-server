package com.bszn.system.converter;

import com.bszn.system.model.entity.SysDept;
import com.bszn.system.model.form.DeptForm;
import com.bszn.system.model.vo.DeptVO;
import org.mapstruct.Mapper;

/**
 * 部门对象转换器
 *
 * @author haoxr
 * @since 2022/7/29
 */
@Mapper(componentModel = "spring")
public interface DeptConverter {

    DeptForm entity2Form(SysDept entity);
    DeptVO entity2Vo(SysDept entity);

    SysDept form2Entity(DeptForm deptForm);

}