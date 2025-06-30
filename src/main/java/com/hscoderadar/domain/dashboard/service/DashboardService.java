package com.hscoderadar.domain.dashboard.service;

import com.hscoderadar.domain.dashboard.dto.response.DashboardSummaryResponse;
import com.hscoderadar.domain.dashboard.repository.DashboardRepository;
import com.hscoderadar.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

  private final DashboardRepository dashboardRepository;

  public DashboardSummaryResponse getDashboardSummary(User user) {
    return dashboardRepository.findByUserId(user.getId())
        .map(DashboardSummaryResponse::from)
        .orElseThrow(() -> new IllegalArgumentException("사용자 대시보드 정보를 찾을 수 없습니다. user_id: " + user.getId()));
  }
}