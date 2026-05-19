package com.back.nbe9112team06.standard.ut

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

class Ut private constructor() {
    init {
        throw UnsupportedOperationException("유틸 클래스는 인스턴스화할 수 없습니다.")
    }

    companion object {
        fun toString(
            secret: String,
            expireSeconds: Long,
            body: Map<String, Any?>
        ): String {
            val secretKey = Keys.hmacShaKeyFor(
                secret.toByteArray(StandardCharsets.UTF_8)
            )

            val now = Date()

            return Jwts.builder()
                .claims(body)
                .issuedAt(now)
                .expiration(Date(now.time + 1000L * expireSeconds))
                .signWith(secretKey)
                .compact()
        }

        private fun getSigningKey(secret: String): SecretKey {
            return Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
        }

        // parseSignedClaims로 서명 검증 + 만료 검증 자동 처리
        fun payloadOrNull(token: String, secret: String): Claims? {
            try {
                return Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token) // 서명 검증 + 만료 자동 검증
                    .payload
            } catch (e: ExpiredJwtException) {
                throw e
            } catch (e: Exception) {
                return null
            }
        }

        // validate는 payloadOrNull 재사용으로 단순화
        fun isValid(token: String, secret: String): Boolean {
            return payloadOrNull(token, secret) != null
        }
    }
}