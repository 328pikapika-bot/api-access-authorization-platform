package com.nstl.openplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理控制器
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
// @Tag(name = "后台管理", description = "管理员功能接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
}
