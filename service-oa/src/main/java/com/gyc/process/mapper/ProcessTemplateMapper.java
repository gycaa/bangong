package com.gyc.process.mapper;

import com.gyc.model.process.ProcessTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swagger.annotations.Api;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * <p>
 * 审批模板 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-12
 */
@Mapper
public interface ProcessTemplateMapper extends BaseMapper<ProcessTemplate> {


}
