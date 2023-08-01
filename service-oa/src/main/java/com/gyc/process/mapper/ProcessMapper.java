package com.gyc.process.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gyc.model.process.Process;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyc.vo.process.ProcessQueryVo;
import com.gyc.vo.process.ProcessVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 审批类型 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2023-07-13
 */
@Mapper
public interface ProcessMapper extends BaseMapper<Process> {
    IPage<ProcessVo> selectPage(Page<ProcessVo> page, @Param("vo") ProcessQueryVo processQueryVo);
}
