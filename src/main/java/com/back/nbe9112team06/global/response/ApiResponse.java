package com.back.nbe9112team06.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ApiResponse<T>(
        String resultCode,
        String msg,
        T data
) {
    public ApiResponse(String msg, String resultCode){
        this(resultCode, msg,null);
    }

    @JsonIgnore
    public int getStatusCode() {
        return Integer.parseInt(resultCode.split("-")[0]);
    }
}
