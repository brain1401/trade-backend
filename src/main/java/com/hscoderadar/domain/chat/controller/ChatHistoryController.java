package com.hscoderadar.domain.chat.controller;

import com.hscoderadar.common.response.ApiResponseMessage;
import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.chat.dto.response.ChatHistoryDetailResponse;
import com.hscoderadar.domain.chat.dto.response.SessionResponse;
import com.hscoderadar.domain.chat.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 채팅 기록 API", description = "회원 전용 채팅 기록 조회 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

  private final ChatHistoryService chatHistoryService;

  @Operation(summary = "채팅 세션 목록 조회", description = "로그인한 회원의 과거 채팅 세션 목록을 최신순으로 페이징하여 조회합니다.")
  @ApiResponseMessage("채팅 기록이 성공적으로 조회되었습니다.")
  @GetMapping("/histories")
  public Page<SessionResponse> getChatSessions(
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
      @Parameter(hidden = true) Pageable pageable) {
    return chatHistoryService.getChatSessions(principalDetails.getUser(), pageable);
  }

  @Operation(summary = "개별 채팅 세션 상세 조회", description = "특정 채팅 세션의 전체 대화 내용을 조회합니다.")
  @ApiResponseMessage("채팅 세션 상세 내역이 성공적으로 조회되었습니다.")
  @GetMapping("/{sessionId}")
  public ChatHistoryDetailResponse getChatHistoryDetail(
      @Parameter(hidden = true) @AuthenticationPrincipal PrincipalDetails principalDetails,
      @PathVariable UUID sessionId) {
    return chatHistoryService.getChatHistoryDetail(principalDetails.getUser(), sessionId);
  }
}