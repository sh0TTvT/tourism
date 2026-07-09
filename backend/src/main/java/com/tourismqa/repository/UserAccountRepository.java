package com.tourismqa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;

/**
 * UserAccountRepository 数据访问仓储接口。
 * 使用场景：提供聚合根的数据查询与持久化能力，供服务层调用。
 * 核心职责：声明领域对象访问契约并复用 Spring Data 查询机制。
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByRole(UserRole role);

    boolean existsByRoleAndBannedFalse(UserRole role);

    long countByRole(UserRole role);

    long countByRoleAndBannedFalse(UserRole role);
}
