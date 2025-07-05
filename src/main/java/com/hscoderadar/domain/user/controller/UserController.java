package com.hscoderadar.domain.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hscoderadar.config.oauth.PrincipalDetails;
import com.hscoderadar.domain.user.dto.UserUpdateRequest;
import com.hscoderadar.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 프로필 수정 (이름, 비밀번호)
     * @param userId 사용자 ID
     * @param request 수정할 정보
     * @return 성공 메시지
     */
    @PatchMapping("/profile")
    public String updateUserProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody UserUpdateRequest request) {

        userService.updateProfile(principalDetails.getUser().getId(), request);

        return "프로필이 성공적으로 업데이트되었습니다.";
    }

    @DeleteMapping("/leave")
    public String deleteUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        
        userService.deleteMe(principalDetails.getUser().getId());
        
        return "회원 탈퇴에 성공하였습니다.";
    }
    
}
