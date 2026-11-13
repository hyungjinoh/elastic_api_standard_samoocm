package com.rayful.elastic.api.standard.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping(value = "/api")
public class LicenseController {

    @Value("${raysearch.license}")
    private String license;

    @GetMapping("/license")
    public ResponseEntity<Object> getLicense() {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("license", license);
        return ResponseEntity.ok(resultMap);
    }
}
