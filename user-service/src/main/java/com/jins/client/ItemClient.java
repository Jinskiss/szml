package com.jins.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("permission-service")
public interface ItemClient {

    @GetMapping("/permission/bindDefaultRole")
    void bindDefaultRole(@RequestParam Long userId);
}