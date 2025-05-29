package de.fleximove.vehicle.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userService", path = "${userService.base-path}")
public interface UserServiceClient {

    @GetMapping("/provider/{id}/company-name")
    String getProviderCompanyName(@PathVariable("id") Long providerId);
}

