package com.hscoderadar.domain.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 갱신 요청 데이터를 담는 DTO 클래스입니다.
 * 
 * <p>
 * 이 클래스는 Access Token이 만료되었을 때 클라이언트가 새로운 토큰을 요청할 때 사용됩니다.
 * Refresh Token을 이용하여 새로운 Access Token과 Refresh Token을 발급받을 수 있습니다.
 * 
 * <h3>요청 형식:</h3>
 * 
 * <pre>{@code
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 * }</pre>
 * 
 * <h3>Token Rotation 방식:</h3>
 * <ul>
 * <li>기존 Refresh Token은 새 토큰 발급과 동시에 무효화</li>
 * <li>새로운 Access Token과 Refresh Token 쌍 발급</li>
 * <li>토큰 탈취 공격 시 피해 범위 최소화</li>
 * </ul>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 * <li>Refresh Token은 데이터베이스에서 검증</li>
 * <li>JWT 형식 및 만료 시간 검증</li>
 * <li>토큰 재사용 공격 방지</li>
 * </ul>
 * 
 * <h3>사용 시나리오:</h3>
 * <ol>
 * <li>클라이언트가 API 호출 시 Access Token 만료 감지</li>
 * <li>저장된 Refresh Token으로 갱신 요청</li>
 * <li>새로운 토큰 쌍 수신 후 원본 API 재호출</li>
 * </ol>
 * 
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see com.hscoderadar.domain.auth.controller.AuthController#refresh(RefreshTokenRequest)
 * @see com.hscoderadar.domain.auth.service.AuthService#refreshTokens(String)
 * @see com.hscoderadar.config.jwt.JwtTokenProvider
 */
@Data
@NoArgsConstructor
public class RefreshTokenRequest {

    /**
     * 토큰 갱신에 사용할 Refresh Token입니다.
     * 
     * <p>
     * 이 토큰은 로그인 시 또는 이전 토큰 갱신 시 발급받은 유효한 Refresh Token이어야 합니다.
     * 서버에서는 이 토큰의 유효성과 데이터베이스 존재 여부를 확인합니다.
     * 
     * <h3>토큰 검증 과정:</h3>
     * <ol>
     * <li>JWT 형식 및 서명 검증</li>
     * <li>토큰 만료 시간 검증</li>
     * <li>데이터베이스에서 토큰 존재 여부 확인</li>
     * <li>토큰 소유자 확인</li>
     * </ol>
     * 
     * <h3>제약사항:</h3>
     * <ul>
     * <li>null이거나 빈 문자열이면 안됨</li>
     * <li>유효한 JWT 형식이어야 함</li>
     * <li>만료되지 않은 토큰이어야 함</li>
     * <li>데이터베이스에 저장된 토큰과 일치해야 함</li>
     * </ul>
     * 
     * <h3>오류 상황:</h3>
     * <ul>
     * <li>토큰이 만료된 경우 → 재로그인 필요</li>
     * <li>토큰이 데이터베이스에 없는 경우 → 이미 사용된 토큰</li>
     * <li>토큰 형식이 잘못된 경우 → 변조되거나 손상된 토큰</li>
     * </ul>
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>HTTPS 연결에서만 전송</li>
     * <li>로컬 저장소에 안전하게 보관</li>
     * <li>XSS 공격 방지를 위해 HttpOnly 쿠키 사용 권장</li>
     * <li>토큰 갱신 후 즉시 기존 토큰 삭제</li>
     * </ul>
     * 
     * @example eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjE2MjM5MDIyfQ.signature
     */
    private String refreshToken;
}