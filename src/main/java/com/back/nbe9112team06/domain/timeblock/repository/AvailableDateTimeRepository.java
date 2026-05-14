package com.back.nbe9112team06.domain.timeblock.repository;

import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailableDateTimeRepository extends JpaRepository<AvailableDateTime, Integer> {
}