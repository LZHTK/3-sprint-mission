package com.sprint.mission.discodeit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping("/auth/callback")
    public String forwardAuthCallback() {
        return "forward:/index.html";
    }
}
