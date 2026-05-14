package com.back.nbe9112team06.global.security;

import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.standard.ut.Ut;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire-seconds:3600}")
    private long expireSeconds;

    public String generateAccessToken(Member member) {
        Map<String, Object> body = Map.of(
                "id",       member.getId(),
                "nickname", member.getNickname()
        );
        return Ut.toString(secret, expireSeconds, body);
    }

    // Claims 타입으로 반환 → payload.get("id", Integer.class) 사용 가능
    public Claims getPayload(String token) {
        return Ut.payloadOrNull(token, secret);
    }
}