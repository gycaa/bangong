package com.gyc.process.service.impl;

import com.gyc.auth.service.SysUserService;
import com.gyc.model.process.ProcessRecord;
import com.gyc.model.system.SysUser;
import com.gyc.process.mapper.ProcessRecordMapper;
import com.gyc.process.service.ProcessRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gyc.security.custom.LoginUserInfoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 审批记录 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-07-13
 */
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProcessRecordServiceImpl extends ServiceImpl<ProcessRecordMapper, ProcessRecord> implements ProcessRecordService {

    @Autowired
    private ProcessRecordMapper processRecordMapper;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public void record(Long processId, Integer status, String description) {

            SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());

            ProcessRecord processRecord = new ProcessRecord();

            processRecord.setProcessId(processId);
            processRecord.setStatus(status);
            processRecord.setDescription(description);
            processRecord.setOperateUserId(sysUser.getId());
            processRecord.setOperateUser(sysUser.getName());

            processRecordMapper.insert(processRecord);
    }
}

