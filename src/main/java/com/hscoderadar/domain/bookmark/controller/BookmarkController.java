package com.hscoderadar.domain.bookmark.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.bookmark.dto.request.BookmarkCreateRequest;
import com.hscoderadar.domain.bookmark.dto.response.BookmarkResponse;
import com.hscoderadar.domain.bookmark.dto.request.BookmarkUpdateRequest;
import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.bookmark.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "북마크 API", description = "사용자의 북마크(관심 항목) 관리")
public class BookmarkController {

  private final BookmarkService bookmarkService;

  @Operation(summary = "북마크 목록 조회", description = "사용자의 전체 북마크 목록을 페이징하여 조회합니다.")
  @ApiResponseMessage("북마크 목록 조회 성공")
  @GetMapping
  public Page<BookmarkResponse> getUserBookmarks(
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
      Pageable pageable) {
    Page<Bookmark> bookmarkPage = bookmarkService.getBookmarksByUser(principalDetails.getUser(), pageable);
    List<BookmarkResponse> bookmarkResponses = bookmarkPage.getContent().stream().map(BookmarkResponse::from)
        .collect(Collectors.toList());
    return new PageImpl<>(bookmarkResponses, pageable, bookmarkPage.getTotalElements());
  }

  @Operation(summary = "북마크 추가", description = "새로운 북마크를 추가합니다.")
  @ApiResponseMessage("북마크 추가 성공")
  @PostMapping
  public BookmarkResponse addBookmark(
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
      @RequestBody BookmarkCreateRequest request) {
    return bookmarkService.createBookmark(principalDetails.getUser(), request);
  }

  @Operation(summary = "북마크 수정", description = "기존 북마크의 일부 정보를 수정합니다.")
  @ApiResponseMessage("북마크 수정 성공")
  @PutMapping("/{id}")
  public BookmarkResponse updateBookmark(
      @Parameter(description = "수정할 북마크 ID", required = true, in = ParameterIn.PATH) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
      @RequestBody BookmarkUpdateRequest request) {
    return bookmarkService.updateBookmark(id, principalDetails.getUser(), request);
  }

  @Operation(summary = "북마크 삭제", description = "선택한 북마크를 삭제합니다.")
  @ApiResponseMessage("북마크 삭제 성공")
  @DeleteMapping("/{id}")
  public void deleteBookmark(
      @Parameter(description = "삭제할 북마크 ID", required = true, in = ParameterIn.PATH) @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails) {
    bookmarkService.deleteBookmark(id, principalDetails.getUser());
  }

  @Operation(summary = "북마크 유형별 조회", description = "특정 유형(HSCODE, KEYWORD 등)의 북마크 목록만 조회합니다.")
  @ApiResponseMessage("북마크 유형별 조회 성공")
  @GetMapping("/type")
  public List<BookmarkResponse> getBookmarksByType(
      @RequestParam("type") Bookmark.BookmarkType type,
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails) {
    List<Bookmark> bookmarks = bookmarkService.getBookmarksByType(principalDetails.getUser(), type);
    return bookmarks.stream().map(BookmarkResponse::from)
        .collect(Collectors.toList());
  }
}