package com.hscoderadar.domain.bookmark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hscoderadar.domain.bookmark.dto.BookmarkDto;
import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.bookmark.repository.BookmarkRepository;
import com.hscoderadar.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

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
    public BookmarkDto.BookmarkResponse createBookmark(User user, BookmarkDto.BookmarkCreateRequest request) {
        // 중복 북마크 확인
        bookmarkRepository.findByUserAndTargetValue(user, request.getTargetValue())
            .ifPresent(b -> {
                throw new IllegalArgumentException("이미 존재하는 북마크입니다.");
            });

        // sseEventData를 JSON 문자열로 변환
        String sseEventDataAsString = objectMapper.writeValueAsString(request.getSseEventData());

        // 빌더를 사용하여 엔티티 생성
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .type(request.getType())
                .targetValue(request.getTargetValue())
                .displayName(request.getDisplayName())
                .sseGenerated(request.isSseGenerated())
                .sseEventData(sseEventDataAsString) // 변환된 문자열을 전달
                .smsNotificationEnabled(request.isSmsNotificationEnabled())
                .emailNotificationEnabled(request.isEmailNotificationEnabled())
                .build();

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkDto.BookmarkResponse.from(savedBookmark);
    }

    public List<BookmarkDto.BookmarkResponse> getBookmarksByUser(User user) {
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(BookmarkDto.BookmarkResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public BookmarkDto.BookmarkResponse updateBookmark(Long bookmarkId, User user, BookmarkDto.BookmarkUpdateRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
            .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다."));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 북마크에 대한 권한이 없습니다.");
        }

        if (request.getDisplayName() != null) {
            bookmark.updateDisplayName(request.getDisplayName());
        }
        if (request.getSmsNotificationEnabled() != null && request.getEmailNotificationEnabled() != null) {
            bookmark.updateNotificationSettings(request.getSmsNotificationEnabled(), request.getEmailNotificationEnabled());
        }
        
        return BookmarkDto.BookmarkResponse.from(bookmark);
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

}