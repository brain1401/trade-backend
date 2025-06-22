package com.hscoderadar.domain.users.repository;

import com.hscoderadar.domain.users.entity.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 엔티티에 대한 데이터 액세스를 담당하는 Repository 인터페이스입니다.
 * 
 * <p>
 * 이 인터페이스는 Spring Data JPA를 기반으로 하며, 다음과 같은 사용자 관련 데이터 작업을 제공합니다:
 * <ul>
 * <li>기본 CRUD 작업 (상속된 JpaRepository 메서드)</li>
 * <li>이메일 기반 사용자 조회 및 중복 검사</li>
 * <li>JWT Refresh Token 기반 사용자 조회</li>
 * <li>사용자 정보 업데이트 (비밀번호, 이름)</li>
 * <li>사용자 검색 및 통계 조회</li>
 * </ul>
 * 
 * <p>
 * <strong>성능 최적화:</strong>
 * <ul>
 * <li>{@code @EntityGraph}를 사용한 N+1 문제 해결</li>
 * <li>{@code @Modifying} 쿼리를 통한 배치 업데이트</li>
 * <li>인덱스 기반 효율적인 조회 쿼리</li>
 * </ul>
 * 
 * <p>
 * <strong>트랜잭션 고려사항:</strong>
 * <ul>
 * <li>{@code @Modifying} 어노테이션이 적용된 메서드는 트랜잭션 내에서 실행 필요</li>
 * <li>벌크 업데이트 후에는 영속성 컨텍스트 클리어 권장</li>
 * </ul>
 * 
 * @author HsCodeRadar Team
 * @since 1.0.0
 * @see User
 * @see JpaRepository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자를 조회하며 연관된 엔티티들을 함께 로드합니다.
     * 
     * <p>
     * 이 메서드는 {@code @EntityGraph}를 사용하여 사용자 설정({@code userSettings})과
     * SNS 계정 정보({@code snsAccounts})를 함께 로드하므로 N+1 문제를 방지합니다.
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>로그인 시 사용자 인증</li>
     * <li>사용자 프로필 조회</li>
     * <li>OAuth2 로그인 후 사용자 정보 확인</li>
     * </ul>
     * 
     * <h3>성능 특징:</h3>
     * <ul>
     * <li>단일 쿼리로 모든 연관 데이터 로드</li>
     * <li>이메일 필드에 인덱스가 있어 빠른 조회</li>
     * </ul>
     * 
     * @param email 조회할 사용자의 이메일 주소 (null이면 안됨)
     * @return 해당 이메일의 사용자 정보와 연관 엔티티들을 포함한 Optional 객체
     * 
     * @see EntityGraph
     * @see User#getUserSettings()
     * @see User#getSnsAccounts()
     */
    @EntityGraph(attributePaths = { "userSettings", "snsAccounts" })
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 지정된 이메일이 이미 사용 중인지 확인합니다.
     * 
     * <p>
     * 이 메서드는 회원가입 시 이메일 중복 검사에 사용됩니다.
     * 실제 사용자 객체를 조회하지 않고 존재 여부만 확인하므로 성능상 유리합니다.
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>회원가입 시 이메일 중복 검사</li>
     * <li>사용자 이메일 변경 시 중복 확인</li>
     * </ul>
     * 
     * @param email 중복 검사할 이메일 주소
     * @return 이메일이 이미 사용 중이면 true, 그렇지 않으면 false
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 사용자의 비밀번호를 업데이트합니다.
     * 
     * <p>
     * 이 메서드는 {@code @Modifying} 어노테이션을 사용한 벌크 업데이트 쿼리입니다.
     * 비밀번호 변경 시 {@code updatedAt} 필드도 자동으로 현재 시간으로 업데이트됩니다.
     * 
     * <h3>보안 고려사항:</h3>
     * <ul>
     * <li>호출 전에 비밀번호가 이미 BCrypt로 암호화되어 있어야 함</li>
     * <li>트랜잭션 내에서 실행되어야 함</li>
     * <li>실행 후 영속성 컨텍스트 클리어 권장</li>
     * </ul>
     * 
     * <h3>사용 예시:</h3>
     * 
     * <pre>{@code
     * String encodedPassword = passwordEncoder.encode(newPassword);
     * userRepository.updatePassword(userId, encodedPassword);
     * }</pre>
     * 
     * @param userId       비밀번호를 변경할 사용자의 ID
     * @param passwordHash BCrypt로 암호화된 새 비밀번호
     * @throws org.springframework.dao.DataAccessException 데이터베이스 접근 오류 시
     * 
     * @see Modifying
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * 사용자의 이름을 업데이트합니다.
     * 
     * <p>
     * 이 메서드는 벌크 업데이트 쿼리를 사용하여 사용자 이름을 변경하고
     * {@code updatedAt} 필드를 현재 시간으로 자동 업데이트합니다.
     * 
     * <h3>주의사항:</h3>
     * <ul>
     * <li>트랜잭션 내에서 실행되어야 함</li>
     * <li>실행 후 영속성 컨텍스트 클리어 권장</li>
     * <li>이름 유효성 검사는 서비스 계층에서 수행</li>
     * </ul>
     * 
     * @param userId 이름을 변경할 사용자의 ID
     * @param name   새로운 사용자 이름 (null이면 안됨)
     * @throws org.springframework.dao.DataAccessException 데이터베이스 접근 오류 시
     * 
     * @see Modifying
     */
    @Modifying
    @Query("UPDATE User u SET u.name = :name, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updateName(@Param("userId") Long userId, @Param("name") String name);

    /**
     * 이름으로 사용자를 검색하고 이름 순으로 정렬하여 반환합니다.
     * 
     * <p>
     * 이 메서드는 부분 문자열 매칭을 사용하여 사용자를 검색합니다.
     * 검색 결과는 이름의 오름차순으로 정렬됩니다.
     * 
     * <h3>검색 특징:</h3>
     * <ul>
     * <li>대소문자 구분 없는 부분 매칭</li>
     * <li>결과는 이름 순으로 정렬</li>
     * <li>빈 문자열 검색 시 모든 사용자 반환</li>
     * </ul>
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>관리자 페이지에서 사용자 검색</li>
     * <li>사용자 자동완성 기능</li>
     * <li>팀원 찾기 기능</li>
     * </ul>
     * 
     * @param name 검색할 이름 (부분 문자열)
     * @return 이름에 해당 문자열을 포함하는 사용자 목록 (이름 순 정렬)
     */
    List<User> findByNameContainingOrderByNameAsc(@Param("name") String name);

    /**
     * 지정된 기간 내에 가입한 사용자들을 조회합니다.
     * 
     * <p>
     * 이 메서드는 통계 및 분석 목적으로 특정 기간의 신규 가입자를 조회할 때 사용됩니다.
     * 결과는 가입일 역순(최신순)으로 정렬됩니다.
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>월별/일별 신규 가입자 통계</li>
     * <li>마케팅 캠페인 효과 분석</li>
     * <li>사용자 증가 추이 분석</li>
     * </ul>
     * 
     * <h3>사용 예시:</h3>
     * 
     * <pre>{@code
     * LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
     * LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
     * List<User> monthlyUsers = userRepository.findUsersCreatedBetween(startOfMonth, endOfMonth);
     * }</pre>
     * 
     * @param startDate 조회 시작 날짜/시간 (포함)
     * @param endDate   조회 종료 날짜/시간 (포함)
     * @return 해당 기간에 가입한 사용자 목록 (가입일 역순)
     * 
     * @see LocalDateTime
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 사용자의 활성화된 북마크 개수를 조회합니다.
     * 
     * <p>
     * 이 메서드는 사용자별 북마크 통계를 제공하며, 모니터링이 활성화된 북마크만 카운트합니다.
     * 대시보드나 사용자 프로필에서 활용도를 표시할 때 사용됩니다.
     * 
     * <h3>카운트 기준:</h3>
     * <ul>
     * <li>해당 사용자 소유의 북마크</li>
     * <li>{@code monitoringEnabled = true}인 북마크만 포함</li>
     * <li>삭제된 북마크는 제외</li>
     * </ul>
     * 
     * <h3>사용 사례:</h3>
     * <ul>
     * <li>대시보드 요약 정보 표시</li>
     * <li>사용자 활동 수준 분석</li>
     * <li>북마크 할당량 관리</li>
     * </ul>
     * 
     * @param userId 북마크 개수를 조회할 사용자의 ID
     * @return 해당 사용자의 활성화된 북마크 개수
     * 
     * @see com.hscoderadar.domain.bookmarks.entity.Bookmark
     */
    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.monitoringEnabled = true")
    long countActiveBookmarksByUserId(@Param("userId") Long userId);

    /**
     * Refresh Token으로 사용자를 조회합니다.
     * 
     * <p>
     * 이 메서드는 JWT 토큰 갱신 과정에서 Refresh Token의 유효성을 확인하고
     * 해당 토큰을 소유한 사용자를 찾는 데 사용됩니다.
     * 
     * <h3>보안 특징:</h3>
     * <ul>
     * <li>Token Rotation 방식에서 토큰 소유자 확인</li>
     * <li>데이터베이스 기반 토큰 검증</li>
     * <li>토큰 탈취 공격 방지</li>
     * </ul>
     * 
     * <h3>사용 과정:</h3>
     * <ol>
     * <li>클라이언트가 Refresh Token 제출</li>
     * <li>JWT 형식 및 만료 시간 검증</li>
     * <li>이 메서드로 데이터베이스에서 토큰 소유자 확인</li>
     * <li>새로운 토큰 쌍 발급 및 기존 토큰 교체</li>
     * </ol>
     * 
     * <h3>주의사항:</h3>
     * <ul>
     * <li>토큰이 null이거나 빈 문자열인 경우 빈 Optional 반환</li>
     * <li>데이터베이스에 저장된 토큰과 정확히 일치해야 함</li>
     * <li>토큰 갱신 후에는 기존 토큰이 무효화됨</li>
     * </ul>
     * 
     * @param refreshToken 검증할 Refresh Token 문자열
     * @return 해당 토큰을 소유한 사용자 정보를 포함한 Optional 객체
     * 
     * @see com.hscoderadar.config.jwt.JwtTokenProvider
     * @see com.hscoderadar.domain.auth.service.AuthService#refreshTokens(String)
     */
    Optional<User> findByRefreshToken(String refreshToken);
}