package com.back.nbe9112team06.domain.timeblock.repository

import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime
import org.springframework.data.jpa.repository.JpaRepository

interface AvailableTimeRepository : JpaRepository<AvailableTime, Int>