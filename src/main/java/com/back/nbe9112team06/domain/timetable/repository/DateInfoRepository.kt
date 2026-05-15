package com.back.nbe9112team06.domain.timetable.repository

import com.back.nbe9112team06.domain.timetable.entity.DateInfo
import org.springframework.data.jpa.repository.JpaRepository

interface DateInfoRepository : JpaRepository<DateInfo, Int>
