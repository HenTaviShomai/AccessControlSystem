package com.AccessControlSystem.exception;


import com.AccessControlSystem.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}