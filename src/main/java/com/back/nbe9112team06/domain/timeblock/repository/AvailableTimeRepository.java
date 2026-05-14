package com.back.nbe9112team06.domain.timeblock.repository;

import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailableTimeRepository extends JpaRepository<AvailableTime, Integer> {
}