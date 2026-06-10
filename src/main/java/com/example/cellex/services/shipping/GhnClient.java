package com.example.cellex.services.shipping;

import com.example.cellex.config.GhnConfig;
import com.example.cellex.dtos.request.shipping.GhnCreateOrderRequest;
import com.example.cellex.dtos.response.shipping.GhnCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnClient {
    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public GhnCreateOrderResponse createOrder(GhnCreateOrderRequest request) {
        String url = ghnConfig.getBaseUrl() + "/shipping-order/create";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getApiToken());
        headers.set("ShopId", ghnConfig.getShopId());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GhnCreateOrderRequest> entity = new HttpEntity<>(request, headers);

        log.info("Calling GHN create order API: {}", url);
        try {
            ResponseEntity<GhnCreateOrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GhnCreateOrderResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to call GHN API: ", e);
            throw new RuntimeException("Lỗi khi kết nối đến GHN API: " + e.getMessage());
        }
    }
}
