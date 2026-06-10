package com.example.cellex.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GhnConfig {
    @Value("${ghn.api.token}")
    private String apiToken;

    @Value("${ghn.shop.id}")
    private String shopId;

    @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api/v2}")
    private String baseUrl;

    @Value("${ghn.service-type-id:2}")
    private int serviceTypeId;
}
