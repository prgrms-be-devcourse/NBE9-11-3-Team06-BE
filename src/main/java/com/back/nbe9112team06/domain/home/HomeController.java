package com.back.nbe9112team06.domain.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@Tag(name="Home", description = "홈 컨트롤러, API서버 메인 페이지로 사용")
public class HomeController {

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "메인 페이지", description = "API 서버의 메인 페이지입니다. 서버의 IP 주소와 호스트 이름을 표시합니다.")
    public String home() throws UnknownHostException {

        InetAddress localhost = InetAddress.getLocalHost();

        return """
                <h1>Welcome to Let's schedule it</h1>
                <div>
                    <a href="swagger-ui/index.html">API 문서로 이동</a>
                </div>
                """.formatted(localhost.getHostAddress(), localhost.getHostName());
    }
}