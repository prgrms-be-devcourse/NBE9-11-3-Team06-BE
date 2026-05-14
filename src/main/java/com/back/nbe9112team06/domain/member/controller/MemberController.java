package com.back.nbe9112team06.domain.member.controller;

import com.back.nbe9112team06.domain.member.dto.SignupRequest;
import com.back.nbe9112team06.domain.member.dto.SignupResponse;
import com.back.nbe9112team06.domain.member.dto.request.CheckEmailRequest;
import com.back.nbe9112team06.domain.member.dto.response.AvailabilityResponse;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.service.MemberService;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.rq.Rq;
import com.back.nbe9112team06.global.springDoc.annotation.AuthErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.MemberErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.back.nbe9112team06.global.springDoc.example.MemberApiExamples.*;


@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "사용자 관리 API")
public class MemberController {
    private final MemberService memberService;
    private final Rq rq;

    @PostMapping
    @Operation(
            summary = "회원가입",
            description = """
            새로운 사용자를 등록합니다.
            
            ### ✅ 검증 규칙
            - `email`: 올바른 이메일 형식, 필수
            - `password`: 8~20자, 필수
            - `nickname`: 2~20자, 필수
            - `timezone`: `ASIA_SEOUL`, `UTC`, `AMERICA_NEW_YORK` 중 선택
            
            ### 🔐 보안
            - 비밀번호는 암호화되어 저장됨
            - 응답에 민감정보 (비밀번호, 해시) 미포함
            """
    )
    @CommonErrorResponses
    @MemberErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = SIGNUP_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        Member member = memberService.signup(request);
        return new ApiResponse<>(
                "201-1", "회원가입에 성공하셨습니다",
                new SignupResponse(member.getEmail(),member.getNickname())
        );
    }

    @PostMapping("/check-email")
    @Operation(
            summary = "이메일 중복 체크",
            description = """
            가입 가능한 이메일인지 확인합니다.
            - 이메일 형식 검증 수행
            """
    )
    @CommonErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(name = "available", value = CHECK_EMAIL_AVAILABLE_JSON),
                            @ExampleObject(name = "duplicate", value = CHECK_EMAIL_DUPLICATE_JSON)
                    }
            )
    )
    public ApiResponse<AvailabilityResponse> checkEmail(@RequestBody @Valid CheckEmailRequest request) {
        boolean isAvailable = !memberService.checkEmail(request);

        return new ApiResponse<>(
                "200-1",
                isAvailable ? "사용 가능한 이메일입니다." : "이미 등록된 이메일입니다.",
                new AvailabilityResponse(isAvailable)
        );
    }

    @DeleteMapping
    @Operation(
            summary = "회원 탈퇴",
            description = """
            현재 로그인한 사용자의 계정을 삭제합니다.
            
            ### 🔐 보안
            - 인증 필요 (accessToken 쿠키)
            - 성공 시 `accessToken` 쿠키를 만료 처리 (maxAge=0)
            - 삭제된 계정은 복구 불가
            """
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MemberErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원 탈퇴 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = DELETE_MEMBER_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<Void> deleteMember() {
        Member actor = rq.getActor();
        memberService.deleteMember(actor.getId());
        rq.clearAccessTokenCookie();

        return new ApiResponse<>("200-1", "회원 탈퇴가 완료되었습니다.", null);
    }
}
