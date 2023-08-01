package com.gyc.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gyc.model.process.ProcessTemplate;
import com.gyc.model.process.ProcessType;
import com.gyc.process.mapper.ProcessTemplateMapper;
import com.gyc.process.service.ProcessService;
import com.gyc.process.service.ProcessTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.process.service.ProcessTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 审批模板 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-12
 */
@Service
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> implements ProcessTemplateService {

    @Resource
    private ProcessTemplateMapper processTemplateMapper;

    @Autowired
    private ProcessService processService;

    @Resource
    private ProcessTypeService processTypeService;

    //分页查询审批模块，把审批类型对应名称查询
    @Override
    public IPage<ProcessTemplate> selectPage(Page<ProcessTemplate> pageParam) {

        LambdaQueryWrapper<ProcessTemplate> queryWrapper = new LambdaQueryWrapper<ProcessTemplate>();
        queryWrapper.orderByDesc(ProcessTemplate::getId);
        //调用mapper方法实现分页查询
        IPage<ProcessTemplate> page = processTemplateMapper.selectPage(pageParam, queryWrapper);
        //第一步分页查询返回分页数据，从分页数据获取列表list集合
        List<ProcessTemplate> processTemplateList = page.getRecords();

        List<Long> processTypeIdList = processTemplateList.stream().map(processTemplate -> processTemplate.getProcessTypeId()).collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(processTypeIdList)) {
            Map<Long, ProcessType> processTypeIdToProcessTypeMap = processTypeService.list(new LambdaQueryWrapper<ProcessType>().in(ProcessType::getId, processTypeIdList)).stream().collect(Collectors.toMap(ProcessType::getId, ProcessType -> ProcessType));
            //遍历list集合，得到每个对象的审批类型id
            for(ProcessTemplate processTemplate : processTemplateList) {
                //根据审批类型id，查询获取对应名称
                ProcessType processType = processTypeIdToProcessTypeMap.get(processTemplate.getProcessTypeId());
                if(null == processType) continue;
                //最终封装
                processTemplate.setProcessTypeName(processType.getName());
            }
        }
        return page;
    }


    //修改模板发布状态，1表示已经发布
    //流程定义部署
    @Transactional
    @Override
    public void publish(Long id) {
        ProcessTemplate processTemplate = this.getById(id);
        //修改模板发布状态，1表示已经发布
        processTemplate.setStatus(1);
        processTemplateMapper.updateById(processTemplate);

        //部署流程定义
        //优先发布在线流程设计
        if(!StringUtils.isEmpty(processTemplate.getProcessDefinitionPath())) {
            processService.deployByZip(processTemplate.getProcessDefinitionPath());
        }
    }
}
