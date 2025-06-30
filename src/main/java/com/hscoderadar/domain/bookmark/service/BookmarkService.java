package com.hscoderadar.domain.bookmark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.common.exception.ErrorCode;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.bookmark.dto.BookmarkCreateRequest;
import com.hscoderadar.domain.bookmark.dto.BookmarkResponse;
import com.hscoderadar.domain.bookmark.dto.BookmarkUpdateRequest;
import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.bookmark.repository.BookmarkRepository;
import com.hscoderadar.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

  private final BookmarkRepository bookmarkRepository;
  private final ObjectMapper objectMapper;

  @SneakyThrows // objectMapper.writeValueAsString()의 예외 처리를 위해 추가
  @Transactional
  public BookmarkResponse createBookmark(User user, BookmarkCreateRequest request) {
    // 중복 북마크 확인
    bookmarkRepository.findByUserAndTargetValue(user, request.targetValue())
        .ifPresent(b -> {
          throw new IllegalArgumentException("이미 존재하는 북마크입니다.");
        });

    // sseEventData를 JSON 문자열로 변환
    String sseEventDataAsString = objectMapper.writeValueAsString(request.sseEventData());

    // 빌더를 사용하여 엔티티 생성
    Bookmark bookmark = Bookmark.builder()
        .user(user)
        .type(request.type())
        .targetValue(request.targetValue())
        .displayName(request.displayName())
        .sseGenerated(request.sseGenerated())
        .sseEventData(sseEventDataAsString) // 변환된 문자열을 전달
        .smsNotificationEnabled(request.smsNotificationEnabled())
        .emailNotificationEnabled(request.emailNotificationEnabled())
        .build();

    Bookmark savedBookmark = bookmarkRepository.save(bookmark);
    return BookmarkResponse.from(savedBookmark);
  }

  public List<BookmarkResponse> getBookmarksByUser(User user) {
    return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
        .map(BookmarkResponse::from)
        .collect(Collectors.toList());
  }

  @Transactional
  public BookmarkResponse updateBookmark(Long bookmarkId, User user,
      BookmarkUpdateRequest request) {
    Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
        .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다."));

    if (!bookmark.getUser().getId().equals(user.getId())) {
      throw new SecurityException("해당 북마크에 대한 권한이 없습니다.");
    }

    if (request.displayName() != null) {
      bookmark.updateDisplayName(request.displayName());
    }
    if (request.smsNotificationEnabled() != null && request.emailNotificationEnabled() != null) {
      bookmark.updateNotificationSettings(request.smsNotificationEnabled(), request.emailNotificationEnabled());
    }

    return BookmarkResponse.from(bookmark);
  }

  @Transactional
  public void deleteBookmark(Long bookmarkId, User user) {
    Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
        .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다."));

    if (!bookmark.getUser().getId().equals(user.getId())) {
      throw new SecurityException("해당 북마크에 대한 권한이 없습니다.");
    }

    bookmarkRepository.delete(bookmark);
  }

  @Transactional(readOnly = true)
  public Page<Bookmark> getBookmarksByUser(User user, Pageable pageable) {
    return bookmarkRepository.findByUser(user, pageable);
  }

  public List<Bookmark> getBookmarksByType(User user, Bookmark.BookmarkType type) {
    return bookmarkRepository.findByUserAndType(user, type);
  }

}