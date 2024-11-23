package com.aleksandr_kobelskiy.week12practice.exception;

public class AuthException extends ApiException {

    public AuthException(String message, String errorCode) {
        super(message, errorCode);
    }
}
