package com.hscoderadar.domain.dashboard.repository;

import com.hscoderadar.domain.dashboard.entity.UserDashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardRepository extends JpaRepository<UserDashboardSummary, Long> {

    /**
     * 사용자 ID로 대시보드 요약 정보를 조회
     * v_user_dashboard_summary 뷰를 사용하여 단일 쿼리로 모든 정보를 가져옴
     */
    Optional<UserDashboardSummary> findByUserId(Long userId);
}