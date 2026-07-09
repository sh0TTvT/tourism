package com.tourismqa.service;

/**
 * 大模型消息单元。
 * 使用场景：
 * 在模型请求中表达单条上下文消息，兼容 system/user/assistant 角色协议。
 *
 * @param role 消息角色
 * @param content 消息内容
 */
public record LlmMessage(String role, String content) {
}
