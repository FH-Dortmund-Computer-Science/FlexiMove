package de.fleximove.vehicle.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "userService")
public interface UserServiceClient {

    @GetMapping("${userService.providerName.endpoint}")
    String getProviderCompanyName(@PathVariable("id") Long providerId);
}

