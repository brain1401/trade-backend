package com.hscoderadar.domain.bookmark.repository;

import com.hscoderadar.domain.bookmark.entity.Bookmark;
import com.hscoderadar.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  /**
   * 특정 사용자의 모든 북마크를 조회
   * 생성 시간의 내림차순으로 정렬
   */
  List<Bookmark> findByUserOrderByCreatedAtDesc(User user);

  /**
   * 사용자와 북마크 타입을 기준으로 북마크 목록을 조회
   */
  List<Bookmark> findByUserAndType(User user, Bookmark.BookmarkType type);

  /**
   * 사용자와 대상 값(HS Code 또는 화물관리번호)으로 특정 북마크가 존재하는지 확인
   */
  Optional<Bookmark> findByUserAndTargetValue(User user, String targetValue);

  List<Bookmark> findByUser(User user);

  Page<Bookmark> findByUser(User user, Pageable pageable);

  Optional<Bookmark> findByIdAndUser(Long id, User user);
}