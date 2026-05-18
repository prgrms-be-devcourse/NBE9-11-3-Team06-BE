package com.back.nbe9112team06.domain.member.service

import com.back.nbe9112team06.domain.member.dto.SignupRequest
import com.back.nbe9112team06.domain.member.dto.request.CheckEmailRequest
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@DisplayName("MemberService 단위 테스트")

class MemberServiceTest {

    private val memberRepository: MemberRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()

    private val memberService = MemberService(memberRepository, passwordEncoder)

    private val testEmail = "user@example.com"
    private val testPassword = "password123!"
    private val testNickname = "테스터"
    private val hashedPassword = "hashed-password"

    private fun signupRequest(
        email: String = testEmail,
        password: String = testPassword,
        nickname: String = testNickname,
        timezone: TimezoneType = TimezoneType.ASIA_SEOUL,
    ) = SignupRequest(email, password, nickname, timezone)

    /**
     * BusinessException의 status / code / message 세 가지를 모두 검증하는 헬퍼
     * (단순 메시지 포함 체크가 아닌, 정확한 일치를 강제)
     */
    private fun assertBusinessException(ex: Throwable, expected: ErrorCode) {
        assertThat(ex).isInstanceOf(BusinessException::class.java)
        val be = ex as BusinessException
        assertThat(be.httpStatus).isEqualTo(expected.status)
        // 객체와 code을 비교해서 에러발셍 -> 객체의 코드와 코드를 비교로 수정
        assertThat(be.errorCode.code).isEqualTo(expected.code)
        assertThat(be.message).isEqualTo(expected.message)
    }

    @Nested
    @DisplayName("signup - 회원가입")
    inner class Signup {

        @Test
        fun `중복되지 않은 이메일이면 회원을 저장하고 저장된 Member를 반환한다`() {
            // given
            val request = signupRequest()
            val savedMember = Member(testEmail, hashedPassword, testNickname, TimezoneType.ASIA_SEOUL)

            every { memberRepository.existsByEmail(testEmail) } returns false
            every { passwordEncoder.encode(testPassword) } returns hashedPassword
            every { memberRepository.save(any<Member>()) } returns savedMember

            // when
            val result = memberService.signup(request)

            // then
            assertThat(result).isSameAs(savedMember)
            verify(exactly = 1) { memberRepository.existsByEmail(testEmail) }
            verify(exactly = 1) { passwordEncoder.encode(testPassword) }
            verify(exactly = 1) { memberRepository.save(any<Member>()) }
        }

        @Test
        fun `회원가입 시 비밀번호는 PasswordEncoder로 해싱되어 저장된다`() {
            // given
            val request = signupRequest()

            every { memberRepository.existsByEmail(testEmail) } returns false
            every { passwordEncoder.encode(testPassword) } returns hashedPassword
            every { memberRepository.save(any<Member>()) } answers { firstArg() }

            // when
            val result = memberService.signup(request)

            // then: 원본 비밀번호가 아닌 해시된 값이 저장됨
            assertThat(result.passwordHash).isEqualTo(hashedPassword)
            assertThat(result.passwordHash).isNotEqualTo(testPassword)
        }

        @Test
        fun `저장되는 Member는 요청의 모든 필드를 그대로 반영한다`() {
            // given
            val request = signupRequest(
                email = "fresh@example.com",
                nickname = "신규회원",
                timezone = TimezoneType.UTC,
            )

            every { memberRepository.existsByEmail("fresh@example.com") } returns false
            every { passwordEncoder.encode(testPassword) } returns hashedPassword
            every { memberRepository.save(any<Member>()) } answers { firstArg() }

            // when
            val result = memberService.signup(request)

            // then
            assertThat(result.email).isEqualTo("fresh@example.com")
            assertThat(result.nickname).isEqualTo("신규회원")
            assertThat(result.timezone).isEqualTo(TimezoneType.UTC)
            assertThat(result.passwordHash).isEqualTo(hashedPassword)
        }

        @Test
        fun `이미 등록된 이메일이면 DUPLICATE_EMAIL 예외를 던지고 저장하지 않는다`() {
            // given
            every { memberRepository.existsByEmail(testEmail) } returns true

            // when & then
            assertThatThrownBy { memberService.signup(signupRequest()) }
                .satisfies({ ex -> assertBusinessException(ex, ErrorCode.DUPLICATE_EMAIL) })

            // 부정 검증: 중복이면 인코딩도 저장도 일어나면 안 됨
            verify(exactly = 1) { memberRepository.existsByEmail(testEmail) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { memberRepository.save(any<Member>()) }
        }

        @Test
        fun `중복 체크는 비밀번호 인코딩보다 먼저 수행된다`() {
            // 인코딩이 비싼 작업이므로 중복 체크 후에 호출되어야 함
            every { memberRepository.existsByEmail(testEmail) } returns true

            assertThatThrownBy { memberService.signup(signupRequest()) }
                .isInstanceOf(BusinessException::class.java)

            // 인코딩이 한 번도 호출되지 않았어야 함
            verify(exactly = 0) { passwordEncoder.encode(any()) }
        }
    }

    @Nested
    @DisplayName("deleteMember - 회원 탈퇴")
    inner class DeleteMember {

        @Test
        fun `존재하는 회원이면 정상 삭제된다`() {
            // given
            val memberId = 1
            val member = Member(testEmail, hashedPassword, testNickname, TimezoneType.ASIA_SEOUL)

            every { memberRepository.findById(memberId) } returns Optional.of(member)
            every { memberRepository.delete(member) } returns Unit

            // when
            memberService.deleteMember(memberId)

            // then
            verify(exactly = 1) { memberRepository.findById(memberId) }
            verify(exactly = 1) { memberRepository.delete(member) }
        }

        @Test
        fun `존재하지 않는 회원이면 NOT_FOUND 예외를 던지고 삭제 호출이 일어나지 않는다`() {
            // given
            val memberId = 999
            every { memberRepository.findById(memberId) } returns Optional.empty()

            // when & then
            assertThatThrownBy { memberService.deleteMember(memberId) }
                .satisfies({ ex -> assertBusinessException(ex, ErrorCode.NOT_FOUND) })

            // 부정 검증: 조회 실패 시 delete 호출 X
            verify(exactly = 1) { memberRepository.findById(memberId) }
            verify(exactly = 0) { memberRepository.delete(any<Member>()) }
        }
    }

    @Nested
    @DisplayName("checkEmail - 이메일 중복 체크")
    inner class CheckEmail {

        @Test
        fun `이메일이 이미 존재하면 true를 반환한다`() {
            // given
            every { memberRepository.existsByEmail(testEmail) } returns true

            // when
            val result = memberService.checkEmail(CheckEmailRequest(testEmail))

            // then
            assertThat(result).isTrue()
            verify(exactly = 1) { memberRepository.existsByEmail(testEmail) }
        }

        @Test
        fun `이메일이 존재하지 않으면 false를 반환한다`() {
            // given
            every { memberRepository.existsByEmail(testEmail) } returns false

            // when
            val result = memberService.checkEmail(CheckEmailRequest(testEmail))

            // then
            assertThat(result).isFalse()
            verify(exactly = 1) { memberRepository.existsByEmail(testEmail) }
        }
    }

    @Nested
    @DisplayName("findById - ID로 회원 조회")
    inner class FindById {

        @Test
        fun `존재하는 ID로 조회하면 Optional에 Member가 담겨 반환된다`() {
            // given
            val memberId = 1
            val member = Member(testEmail, hashedPassword, testNickname, TimezoneType.ASIA_SEOUL)
            every { memberRepository.findById(memberId) } returns Optional.of(member)

            // when
            val result = memberService.findById(memberId)

            // then
            assertThat(result).isPresent
            assertThat(result.get()).isSameAs(member)
        }

        @Test
        fun `존재하지 않는 ID로 조회하면 빈 Optional을 반환한다`() {
            // given
            every { memberRepository.findById(any()) } returns Optional.empty()

            // when
            val result = memberService.findById(999)

            // then
            assertThat(result).isEmpty
        }
    }

    @Nested
    @DisplayName("findByEmail - 이메일로 회원 조회")
    inner class FindByEmail {

        @Test
        fun `존재하는 이메일로 조회하면 Optional에 Member가 담겨 반환된다`() {
            // given
            val member = Member(testEmail, hashedPassword, testNickname, TimezoneType.ASIA_SEOUL)
            every { memberRepository.findByEmail(testEmail) } returns Optional.of(member)

            // when
            val result = memberService.findByEmail(testEmail)

            // then
            assertThat(result).isPresent
            assertThat(result.get()).isSameAs(member)
        }

        @Test
        fun `존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다`() {
            // given
            every { memberRepository.findByEmail(any()) } returns Optional.empty()

            // when
            val result = memberService.findByEmail("ghost@example.com")

            // then
            assertThat(result).isEmpty
        }
    }
}