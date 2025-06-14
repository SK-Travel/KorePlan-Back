package com.koreplan.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class SessionDebugController {

    @GetMapping("/session")
    public Map<String, Object> checkSession(HttpSession session) {
        Map<String, Object> sessionInfo = new HashMap<>();

        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("userId", session.getAttribute("userId"));
        sessionInfo.put("email", session.getAttribute("email"));
        sessionInfo.put("name", session.getAttribute("name"));
        sessionInfo.put("isNew", session.isNew());
        sessionInfo.put("creationTime", new Date(session.getCreationTime()));
        sessionInfo.put("lastAccessedTime", new Date(session.getLastAccessedTime()));

        System.out.println("=== 세션 디버깅 ===");
        System.out.println("Session ID: " + session.getId());
        System.out.println("User ID: " + session.getAttribute("userId"));
        System.out.println("Email: " + session.getAttribute("email"));
        System.out.println("Name: " + session.getAttribute("name"));

        return sessionInfo;
    }
}
