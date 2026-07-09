package com.tourismqa.exception;

/**
 * 业务异常模型。
 * 使用场景：
 * 在服务层按业务语义抛出可控异常，并由全局异常处理器转换为标准 HTTP 响应。
 * 核心职责：
 * 1. 封装业务失败的 HTTP 状态码。
 * 2. 继承运行时异常以减少显式捕获样板代码。
 */
public class ApiException extends RuntimeException {

    private final int status;

    /**
     * 构造业务异常。
     *
     * @param status HTTP 状态码
     * @param message 错误信息
     */
    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return 状态码
     */
    public int getStatus() {
        return status;
    }
}
