package com.tourismqa.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tourismqa.exception.ApiException;

/**
 * 数据库可用性探测服务。
 * 使用场景：
 * 在关键业务入口（如登录、注册）执行数据库连通性检查，快速失败并返回可诊断错误。
 * 核心职责：
 * 1. 从数据源获取连接并进行短时有效性校验。
 * 2. 将底层 SQL 异常统一映射为业务异常。
 *
 * <p>框架作用：`@Service` 声明业务组件，默认单例 Bean。</p>
 */
@Service
public class DatabaseConnectionService {

    private final DataSource dataSource;

    public DatabaseConnectionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 校验数据库当前是否可用。
     *
     * @throws ApiException 当数据库连接失败或连接不可用时抛出
     */
    public void ensureAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "数据库连接不可用，请稍后重试");
            }
        } catch (SQLException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE.value(), "数据库连接失败，请检查 MySQL 服务");
        }
    }
}
