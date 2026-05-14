package com.back.nbe9112team06.global.security;

import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.testutil.AuthTokenHelper;
import com.back.nbe9112team06.testutil.MemberTestFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트
 * - Spring Context 필요 (@Value 주입 때문)
 * - DB 불필요 → @Transactional 없음
 * - JWT 생성 → 파싱 → 페이로드 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthTokenHelper authTokenHelper;
    @Autowired
    private MemberTestFactory memberTestFactory;

    // 테스트용 경량 Member (DB 저장 불필요)
    private Member testMember() {
        Member m = new Member(
                "jwt-test@example.com",
                "hashedPw",
                "JWT테스터",
                TimezoneType.ASIA_SEOUL
        );
        // id 설정 (BaseEntity setId 필요 시 reflection 또는 생성자 활용)
        ReflectionTestUtils.setField(m, "id", 99);
        return m;
    }

    @Nested
    @DisplayName("generateAccessToken - 토큰 생성")
    class GenerateAccessToken {

        @Test
        @DisplayName("Member로 토큰 생성 → 빈 문자열 아님")
        void t1_generateToken_notBlank() {
            // when
            String token = jwtTokenProvider.generateAccessToken(testMember());

            // then
            assertThat(token).isNotBlank();
            System.out.println("accessToken = " + token);
        }

        @Test
        @DisplayName("같은 Member로 두 번 생성 → 발급 시간 다르므로 토큰 다름")
        void t2_sameMemeber_differentToken() throws InterruptedException {
            Member m = testMember();
            String token1 = jwtTokenProvider.generateAccessToken(m);
            Thread.sleep(1000); // iat(발급시간) 차이 유발
            String token2 = jwtTokenProvider.generateAccessToken(m);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("getPayload - 페이로드 추출")
    class GetPayload {

        @Test
        @DisplayName("생성한 토큰의 payload에 id 포함")
        void t3_payload_containsId() {
            Member m = testMember();
            String token = jwtTokenProvider.generateAccessToken(m);

            Claims payload = jwtTokenProvider.getPayload(token);

            assertThat(payload).isNotNull();
            assertThat(payload.get("id", Integer.class)).isEqualTo(m.getId());
        }

        @Test
        @DisplayName("생성한 토큰의 payload에 nickname 포함")
        void t4_payload_containsNickname() {
            Member m = testMember();
            String token = jwtTokenProvider.generateAccessToken(m);

            Claims payload = jwtTokenProvider.getPayload(token);

            assertThat(payload).isNotNull();
            assertThat(payload.get("nickname", String.class)).isEqualTo("JWT테스터");
        }

        @Test
        @DisplayName("payload에 email 미포함 (민감정보 보호)")
        void t5_payload_notContainEmail() {
            String token = jwtTokenProvider.generateAccessToken(testMember());
            Claims payload = jwtTokenProvider.getPayload(token);

            assertThat(payload).isNotNull();
            assertThat(payload.containsKey("email")).isFalse();
            assertThat(payload.containsKey("passwordHash")).isFalse();
        }

        @Test
        @DisplayName("위조 토큰 → null 반환")
        void t6_tamperedToken_returnsNull() {
            Claims payload = jwtTokenProvider.getPayload("forged.token.value");
            assertThat(payload).isNull();
        }

        @Test
        @DisplayName("다른 secret으로 서명된 토큰 → null 반환")
        void t7_differentSecretToken_returnsNull() {
            Member m = testMember();
            String fakeToken = authTokenHelper.createForgedToken(
                    m, "attacker-secret-completely-different!!"
            );

            Claims payload = jwtTokenProvider.getPayload(fakeToken);
            assertThat(payload).isNull();
        }

        @Test
        @DisplayName("만료 토큰 → ExpiredJwtException 발생")
        void t8_expiredToken_throwsException() {
            Member m = testMember();
            String expiredToken = authTokenHelper.createToken(m, -1L);

            assertThatThrownBy(() -> jwtTokenProvider.getPayload(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}
