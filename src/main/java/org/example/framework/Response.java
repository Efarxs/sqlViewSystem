package org.example.framework;

public class Response {
    private boolean isSuccess;
    private String message;

    public Response(){}

    public Response(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public Response(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSucccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
