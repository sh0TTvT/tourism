package com.tourismqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.ChatConversation;

/**
 * ChatConversationRepository 数据访问仓储接口。
 * 使用场景：提供聚合根的数据查询与持久化能力，供服务层调用。
 * 核心职责：声明领域对象访问契约并复用 Spring Data 查询机制。
 */
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByIdAndUser_Id(Long id, Long userId);

    List<ChatConversation> findByUser_IdOrderByUpdatedAtDesc(Long userId);
}
