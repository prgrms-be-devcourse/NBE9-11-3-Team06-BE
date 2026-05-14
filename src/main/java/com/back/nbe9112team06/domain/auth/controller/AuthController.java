package com.back.nbe9112team06.domain.auth.controller;

import com.back.nbe9112team06.domain.auth.dto.LoginRequest;
import com.back.nbe9112team06.domain.auth.dto.LoginResponse;
import com.back.nbe9112team06.domain.auth.dto.LoginResult;
import com.back.nbe9112team06.domain.auth.service.AuthService;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.rq.Rq;
import com.back.nbe9112team06.global.springDoc.annotation.AuthErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.AuthSpecificErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.back.nbe9112team06.global.springDoc.example.AuthApiExamples.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final Rq rq;


    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = """
            이메일과 비밀번호로 인증을 수행하고, 액세스 토큰을 **HttpOnly 쿠키**에 발급합니다.
            
            ### 🍪 쿠키 설정
            - `accessToken`: JWT 토큰 (HttpOnly, Secure, SameSite=Strict)
            - 브라우저가 이후 요청에 자동 첨부하므로, 클라이언트는 토큰을 직접 관리할 필요 없음
            
            ### 🔐 보안 고려사항
            - CSRF 공격 방지를 위해 동일 사이트 요청만 허용 (SameSite=Strict)
            - XSS 공격 방지를 위해 JavaScript 에서 쿠키 접근 불가 (HttpOnly)
            """
    )
    @CommonErrorResponses
    @AuthSpecificErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "로그인 성공 + 쿠키에 토큰 발급",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = LOGIN_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request){

       LoginResult result = authService.login(request);
        rq.issueAccessTokenCookie(result.accessToken());

        return new ApiResponse<>("201-1","로그인 성공",
                new LoginResponse(result.nickname())
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "현재 인증된 사용자의 기본 정보를 반환합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = GET_MY_INFO_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<LoginResponse> getMyInfo() {
        Member actor = rq.getActor();
        return new ApiResponse<>(
                "200-1","조회 성공", new LoginResponse(actor.getNickname())
        ); // 추후에 개인정보 추가
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "클라이언트 측 액세스 토큰 쿠키를 제거합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = LOGOUT_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<Void> logout() {
        rq.clearAccessTokenCookie();
        return new ApiResponse<>("200-1", "로그아웃 성공",null);
    }
}