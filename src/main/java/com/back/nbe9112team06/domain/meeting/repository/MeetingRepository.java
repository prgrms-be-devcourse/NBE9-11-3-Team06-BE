package com.back.nbe9112team06.domain.meeting.repository;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Integer> {
    boolean existsByRandomUrl(String randomUrl);
    Optional<Meeting> findByRandomUrl(String randomUrl);
    List<Meeting> findByMember_IdOrderByCreatedAtDesc(Integer memberId);
}
