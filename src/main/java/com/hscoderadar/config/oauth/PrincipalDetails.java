package com.hscoderadar.config.oauth;

import com.hscoderadar.domain.user.entity.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Spring Security 인증 컨텍스트에서 사용되는 통합 사용자 Principal 객체
 *
 * <p>일반 로그인과 OAuth2 로그인을 모두 지원하는 어댑터 클래스로, Spring Security의 {@link UserDetails}와 {@link OAuth2User}
 * 인터페이스를 동시에 구현하여 두 가지 인증 방식을 통일적으로 처리
 *
 * <h3>지원하는 인증 방식:</h3>
 *
 * <ul>
 *   <li><strong>일반 로그인:</strong> 이메일/비밀번호 기반 인증
 *   <li><strong>OAuth2 로그인:</strong> Google, Naver, Kakao 소셜 로그인
 * </ul>
 *
 * <h3>Spring Security 통합:</h3>
 *
 * <ul>
 *   <li>{@link UserDetails} 구현으로 일반 인증 지원
 *   <li>{@link OAuth2User} 구현으로 OAuth2 인증 지원
 *   <li>SecurityContext에서 일관된 사용자 정보 접근
 * </ul>
 *
 * <h3>사용 예시:</h3>
 *
 * <pre>{@code
 * // 컨트롤러에서 인증된 사용자 정보 접근
 * @GetMapping("/profile")
 * public ResponseEntity<?> getProfile(Authentication authentication) {
 *     PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
 *     User user = principal.getUser();
 *     return ResponseEntity.ok(user);
 * }
 * }</pre>
 *
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see UserDetails
 * @see OAuth2User
 * @see CustomOAuth2UserService
 * @see com.hscoderadar.domain.auth.service.CustomUserDetailsService
 */
@Data
public class PrincipalDetails implements UserDetails, OAuth2User {

  /** 애플리케이션 내부 사용자 엔티티 */
  private final User user;

  /** OAuth2 제공업체에서 받은 원본 사용자 속성 (OAuth2 로그인 시에만 사용) */
  private final Map<String, Object> attributes;

  /**
   * 일반 로그인용 생성자
   *
   * <p>이메일/비밀번호 기반 인증 시 사용되며, OAuth2 속성은 null로 설정됨
   *
   * @param user 인증된 사용자 엔티티
   */
  public PrincipalDetails(User user) {
    this.user = user;
    this.attributes = null;
  }

  /**
   * OAuth2 로그인용 생성자
   *
   * <p>소셜 로그인 성공 시 {@link CustomOAuth2UserService}에서 호출되며, OAuth2 제공업체의 원본 사용자 속성 정보를 함께 저장
   *
   * @param user 인증된 사용자 엔티티
   * @param attributes OAuth2 제공업체에서 받은 사용자 속성
   */
  public PrincipalDetails(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  /**
   * 사용자 권한 목록 반환 (Spring Security UserDetails 구현)
   *
   * <p>현재는 모든 사용자에게 ROLE_USER 권한을 부여하며, 향후 역할 기반 권한 관리로 확장 가능
   *
   * @return 사용자 권한 컬렉션
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    // 기본적으로 모든 사용자에게 USER 권한 부여
    authorities.add(() -> "ROLE_USER");
    return authorities;
  }

  /**
   * 사용자 비밀번호 반환 (일반 로그인용)
   *
   * <p>OAuth 사용자의 경우 비밀번호 해시가 null이므로, 일반 로그인 시도 시 인증이 실패하도록 빈 문자열 반환
   *
   * @return 암호화된 비밀번호 해시 또는 빈 문자열 (OAuth 사용자)
   */
  @Override
  public String getPassword() {
    // OAuth 사용자의 경우 비밀번호 해시가 null이므로 빈 문자열 반환
    // 이로 인해 일반 로그인 시도 시 인증이 실패함
    return user.getPasswordHash() != null ? user.getPasswordHash() : "";
  }

  /**
   * 사용자명 반환 (Spring Security 식별자)
   *
   * <p>이메일을 사용자명으로 사용하여 일관된 식별자 제공
   *
   * @return 사용자 이메일 주소
   */
  @Override
  public String getUsername() {
    return user.getEmail();
  }

  /**
   * 계정 만료 여부 확인
   *
   * @return true (계정 만료 없음)
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 계정 잠금 여부 확인
   *
   * @return true (계정 잠금 없음)
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * 자격 증명 만료 여부 확인
   *
   * @return true (자격 증명 만료 없음)
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 계정 활성화 상태 확인
   *
   * @return true (모든 계정 활성화)
   */
  @Override
  public boolean isEnabled() {
    return true;
  }

  /**
   * 사용자 이름 반환 (OAuth2User 구현)
   *
   * @return 사용자 이름
   */
  @Override
  public String getName() {
    return user.getName();
  }

  /**
   * OAuth2 속성 반환 (OAuth2User 구현)
   *
   * @return OAuth2 제공업체 원본 속성 또는 null (일반 로그인 시)
   */
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
