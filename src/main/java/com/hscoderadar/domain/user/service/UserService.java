package com.hscoderadar.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.hscoderadar.domain.user.dto.UserUpdateRequest;
import com.hscoderadar.domain.user.entity.User;
import com.hscoderadar.domain.user.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 프로필 정보(이름, 비밀번호)를 수정
     * @param userId 대상 사용자 ID
     * @param request 수정할 정보
     */
    @Transactional
    public void updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. id=" + userId));

        // 이름 변경 요청이 있는 경우
        if (StringUtils.hasText(request.getName())) {
            user.updateName(request.getName());
        }
        // 비밀번호 변경 요청이 있는 경우
        if (StringUtils.hasText(request.getPassword())) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.updatePasswordHash(encodedPassword);
        }
    }

    @Transactional
    public void deleteMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. id=" + userId));
        userRepository.delete(user);
    }
}
