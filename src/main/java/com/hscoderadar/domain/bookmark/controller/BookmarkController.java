package com.hscoderadar.domain.bookmark.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.bookmark.dto.BookmarkDto;
import com.hscoderadar.domain.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponseMessage("북마크가 성공적으로 생성되었습니다.")
    public BookmarkDto.BookmarkResponse createBookmark(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody BookmarkDto.BookmarkCreateRequest request) {
        return bookmarkService.createBookmark(principalDetails.getUser(), request);
    }

    @GetMapping
    @ApiResponseMessage("북마크 목록을 성공적으로 조회했습니다.")
    public List<BookmarkDto.BookmarkResponse> getMyBookmarks(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return bookmarkService.getBookmarksByUser(principalDetails.getUser());
    }

    @PutMapping("/{id}")
    @ApiResponseMessage("북마크가 성공적으로 수정되었습니다.")
    public BookmarkDto.BookmarkResponse updateBookmark(
            @PathVariable("id") Long bookmarkId,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody BookmarkDto.BookmarkUpdateRequest request) {
        return bookmarkService.updateBookmark(bookmarkId, principalDetails.getUser(), request);
    }

    @DeleteMapping("/{id}")
    @ApiResponseMessage("북마크가 성공적으로 삭제되었습니다.")
    public ResponseEntity<Void> deleteBookmark(
            @PathVariable("id") Long bookmarkId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        bookmarkService.deleteBookmark(bookmarkId, principalDetails.getUser());
        return ResponseEntity.noContent().build();
    }

}