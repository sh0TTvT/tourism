package com.tourismqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourismqa.entity.KgChangeLog;

public interface KgChangeLogRepository extends JpaRepository<KgChangeLog, Long> {

    List<KgChangeLog> findTop100ByOrderByCreatedAtDesc();
}
