package com.hscoderadar.domain.feed.service;

import com.hscoderadar.domain.feed.dto.response.FeedResponse;
import com.hscoderadar.domain.feed.repository.UpdateFeedRepository;
import com.hscoderadar.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

  private final UpdateFeedRepository updateFeedRepository;

  public Page<FeedResponse> getFeeds(User user, Pageable pageable) {
    return updateFeedRepository.findByUserOrderByCreatedAtDesc(user, pageable)
        .map(FeedResponse::from);
  }

  @Transactional
  public void markFeedAsRead(User user, Long feedId) {
    // 1. 피드의 존재 여부와 소유권 확인
    updateFeedRepository.findByIdAndUser(feedId, user)
        .orElseThrow(() -> new SecurityException("해당 피드를 찾을 수 없거나 접근 권한이 없습니다. ID: " + feedId));

    // 2. is_read 필드만 업데이트
    updateFeedRepository.markAsReadByFeedId(feedId);
  }

  @Transactional
  public int markAllFeedsAsRead(User user) {
    return updateFeedRepository.markAllAsReadForUser(user);
  }
}