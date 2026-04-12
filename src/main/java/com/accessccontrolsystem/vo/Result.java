package com.accessccontrolsystem.vo;



import com.accessccontrolsystem.enums.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Data
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;
    private String traceId;

    private Result() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // 成功
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMsg(ErrorCode.SUCCESS.getMsg());
        result.setData(data);
        log.debug("返回成功结果, traceId: {}", result.getTraceId());
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败（使用错误码枚举）
    public static <T> Result<T> error(ErrorCode errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMsg(errorCode.getMsg());
        log.debug("返回错误结果, traceId: {}, code: {}, msg: {}",
                result.getTraceId(), errorCode.getCode(), errorCode.getMsg());
        return result;
    }

    // 失败（自定义消息）
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        log.debug("返回错误结果, traceId: {}, code: {}, msg: {}", result.getTraceId(), code, msg);
        return result;
    }

    // 失败（使用错误码枚举 + 自定义消息）
    public static <T> Result<T> error(ErrorCode errorCode, String msg) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMsg(msg);
        log.debug("返回错误结果, traceId: {}, code: {}, msg: {}",
                result.getTraceId(), errorCode.getCode(), msg);
        return result;
    }
}