package com.nstl.sandbox.common;

import lombok.Data;

/**
 * 统一响应结果
 *
 * @author NSTL开发团队
 * @version 1.0.0
 */
@Data
public class Result<T> {
    
    private Integer code;
    private String message;
    private T data;
    
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
