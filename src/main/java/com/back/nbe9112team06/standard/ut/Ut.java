package com.back.nbe9112team06.standard.ut;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class Ut {
    private Ut() {
        throw new UnsupportedOperationException("유틸 클래스는 인스턴스화할 수 없습니다.");
    }

    public static String toString(String secret, long expireSeconds, Map<String, Object> body) {
        SecretKey secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();

        return Jwts.builder()
                .claims(body)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 1000L * expireSeconds))
                .signWith(secretKey)
                .compact();
    }

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // parseSignedClaims로 서명 검증 + 만료 검증 자동 처리
    public static Claims payloadOrNull(String token, String secret) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token)   // 서명 검증 + 만료 자동 검증
                    .getPayload();
        }  catch (ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    // validate는 payloadOrNull 재사용으로 단순화
    public static boolean isValid(String token, String secret) {
        return payloadOrNull(token, secret) != null;
    }
}