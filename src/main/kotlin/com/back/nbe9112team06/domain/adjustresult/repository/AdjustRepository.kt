package com.back.nbe9112team06.domain.adjustresult.repository

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import org.springframework.data.jpa.repository.JpaRepository

interface AdjustRepository : JpaRepository<AdjustResult, Int>
