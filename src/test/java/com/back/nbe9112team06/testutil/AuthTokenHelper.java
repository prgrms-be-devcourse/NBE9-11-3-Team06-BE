package com.back.nbe9112team06.testutil;

import com.back.nbe9112team06.config.TestConstants;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.standard.ut.Ut;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.back.nbe9112team06.config.TestConstants.DEFAULT_EXPIRE_SECONDS;
import static com.back.nbe9112team06.config.TestConstants.JWT_SECRET;

@Component  // ✅ 테스트 컨텍스트에서 주입 가능
public class AuthTokenHelper {

    /**
     * 테스트용 JWT 토큰 생성
     * @param member 토큰에 포함할 사용자 정보
     * @param expireSeconds 만료 시간 (초). 음수면 즉시 만료
     * @param extraPayload 추가 클레임 (옵션)
     */
    public String createToken(Member member, long expireSeconds, Map<String, Object> extraPayload) {
        Map<String, Object> payload = new HashMap<>(extraPayload);
        payload.put("id", member.getId());
        payload.put("nickname", member.getNickname());

        return Ut.toString(JWT_SECRET, expireSeconds, payload);
    }

    // 오버로드: 기본 만료 시간 사용
    public String createToken(Member member, Map<String, Object> extraPayload) {
        return createToken(member, DEFAULT_EXPIRE_SECONDS, extraPayload);
    }

    // 오버로드: 추가 페이로드 없이 기본값만 사용
    public String createToken(Member member, long expireSeconds) {
        return createToken(member, expireSeconds, Map.of());
    }

    // 오버로드: 모든 기본값 사용
    public String createToken(Member member) {
        return createToken(member, DEFAULT_EXPIRE_SECONDS, Map.of());
    }

    /**
     * 위조 토큰 생성 (다른 secret 으로 서명)
     */
    public String createForgedToken(Member member, String fakeSecret) {
        return Ut.toString(fakeSecret, DEFAULT_EXPIRE_SECONDS,
                Map.of("id", member.getId(), "nickname", member.getNickname()));
    }
    /**
     * 커스텀 payload 로 테스트용 토큰 생성
     * - 존재하지 않는 회원 id 등 특수 시나리오 테스트용
     */
    public String createTokenWithPayload(Map<String, Object> customPayload) {
        return Ut.toString(TestConstants.JWT_SECRET,
                TestConstants.DEFAULT_EXPIRE_SECONDS,
                customPayload);
    }
}