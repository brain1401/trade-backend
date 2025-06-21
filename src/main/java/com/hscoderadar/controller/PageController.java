package com.hscoderadar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 테스트용 정적 HTML 페이지를 서빙하는 컨트롤러
 */
@Controller // @RestController가 아님에 주의! HTML 파일 이름을 반환합니다.
public class PageController {

    /**
     * 로그인 페이지를 반환합니다.
     */
    @GetMapping("/login-page")
    public String loginPage() {
        return "login.html";
    }

    /**
     * 회원가입 페이지를 반환합니다.
     */
    @GetMapping("/signup-page")
    public String signupPage() {
        return "signup.html";
    }
    
    /**
     * 로그인 성공 후 메인 페이지를 반환합니다.
     */
    @GetMapping("/main-page")
    public String mainPage() {
        return "main.html";
    }
}