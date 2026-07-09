package com.tourismqa.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器。
 * 使用场景：
 * 统一拦截控制器抛出的异常并输出结构化错误响应，保证前端错误处理协议一致。
 * 核心职责：
 * 1. 处理业务异常并透传业务状态码。
 * 2. 处理参数校验异常并返回字段级错误信息。
 * 3. 兜底处理未知异常，防止异常栈直接暴露给调用方。
 *
 * <p>框架作用：`@RestControllerAdvice` 将异常处理逻辑织入所有 REST 控制器。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(errorBody(ex.getStatus(), ex.getMessage()));
    }

    /**
     * 处理请求参数校验异常。
     *
     * @param ex 参数校验异常
     * @return 包含字段错误明细的响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "参数校验失败");
        Map<String, String> fields = new HashMap<>();
        // 将同一请求中的字段错误扁平化返回，便于前端逐项展示。
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            fields.put(err.getField(), err.getDefaultMessage());
        }
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理请求参数约束校验异常。
     *
     * @param ex 约束校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "参数校验失败");
        Map<String, String> fields = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理请求参数类型不匹配异常。
     *
     * @param ex 参数类型异常
     * @return 统一错误响应
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST.value(), "请求参数格式错误: " + ex.getMessage()));
    }

    /**
     * 处理未显式捕获的其他异常。
     *
     * @param ex 未知异常
     * @return 服务端错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误: " + ex.getMessage()));
    }

    /**
     * 构造统一错误响应体。
     *
     * @param status HTTP 状态码
     * @param message 错误消息
     * @return 错误响应体
     */
    private Map<String, Object> errorBody(int status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("message", message);
        return body;
    }
}
