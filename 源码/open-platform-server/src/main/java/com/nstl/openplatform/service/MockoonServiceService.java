package com.nstl.openplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nstl.openplatform.entity.MockoonService;
import java.util.List;

/**
 * Mockoon 服务管理 Service 接口
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public interface MockoonServiceService extends IService<MockoonService> {

    /**
     * 获取所有启用的服务
     *
     * @return 服务列表
     */
    List<MockoonService> getActiveList();

    /**
     * 校验并标准化服务信息
     */
    MockoonService validateAndNormalize(MockoonService service, boolean update);

    /**
     * 新增服务
     */
    boolean createService(MockoonService service);

    /**
     * 更新服务
     */
    boolean updateService(MockoonService service);
}
