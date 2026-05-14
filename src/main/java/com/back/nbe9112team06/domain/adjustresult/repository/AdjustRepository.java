package com.back.nbe9112team06.domain.adjustresult.repository;

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustRepository extends JpaRepository<AdjustResult, Integer> {
}
