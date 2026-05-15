package com.back.nbe9112team06.domain.timeblock.repository

import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface AvailableDateTimeRepository : JpaRepository<AvailableDateTime, Int>