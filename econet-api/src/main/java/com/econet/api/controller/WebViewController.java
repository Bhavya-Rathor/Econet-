package com.econet.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping(value = {
        "/",
        "/dashboard",
        "/air",
        "/water",
        "/soil",
        "/pollution",
        "/alerts",
        "/report",
        "/reports",
        "/forecast",
        "/ml"
    })
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
