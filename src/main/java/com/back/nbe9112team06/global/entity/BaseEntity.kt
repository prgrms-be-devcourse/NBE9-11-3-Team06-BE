package com.back.nbe9112team06.global.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    //TODO 통합할시 주 생성자로 id을 넣고 다른 entity에서 base(0)으로 생성자를 넣는다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    @Column(name = "modified_at")
    var modifiedAt: LocalDateTime? = null

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BaseEntity
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}
