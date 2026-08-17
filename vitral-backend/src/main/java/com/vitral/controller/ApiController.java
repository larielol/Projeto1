package com.vitral.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @GetMapping
    public Map<String, String> status() {
        return Map.of(
                "name", "vitral-backend",
                "status", "ok",
                "version", "v1");
    }
}
