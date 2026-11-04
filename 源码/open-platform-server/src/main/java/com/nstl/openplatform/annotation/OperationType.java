package com.nstl.openplatform.annotation;

/**
 * 操作类型枚举
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
public enum OperationType {

    /**
     * 查询
     */
    QUERY(1, "查询"),

    /**
     * 新增
     */
    INSERT(2, "新增"),

    /**
     * 修改
     */
    UPDATE(3, "修改"),

    /**
     * 删除
     */
    DELETE(4, "删除"),

    /**
     * 导出
     */
    EXPORT(5, "导出"),

    /**
     * 登录
     */
    LOGIN(6, "登录"),

    /**
     * 登出
     */
    LOGOUT(7, "登出"),

    /**
     * 其他
     */
    OTHER(8, "其他");

    private final Integer code;
    private final String desc;

    OperationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
