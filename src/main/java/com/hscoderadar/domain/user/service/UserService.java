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
     *
     * @param userId  대상 사용자 ID
     * @param request 수정할 정보
     */
    @Transactional
    public void updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. id=" + userId));

        // 이름 변경 요청이 있는 경우
        if (StringUtils.hasText(request.name())) {
            user.updateName(request.name());
        }

        // 비밀번호 변경 요청이 있는 경우
        if (StringUtils.hasText(request.newPassword())) {
            // 현재 비밀번호가 맞는지 확인
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            // 새로운 비밀번호와 비밀번호 확인이 일치하는지 확인
            if (!request.newPassword().equals(request.newPasswordConfirm())) {
                throw new IllegalArgumentException("새로운 비밀번호가 일치하지 않습니다.");
            }

            // 비밀번호를 암호화하여 업데이트
            user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        }
    }

    @Transactional
    public void deleteMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. id=" + userId));
        userRepository.delete(user);
    }
}
