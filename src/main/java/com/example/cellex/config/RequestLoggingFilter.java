package com.example.cellex.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Request Logging Filter (currently disabled)
 * Used for debugging request body issues
 * Uncomment @Component to enable
 */
// @Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Only cache and log vendor-response endpoint
        if (httpRequest.getRequestURI().contains("/vendor-response") && "POST".equals(httpRequest.getMethod())) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
            
            System.out.println("🔍 ===== VENDOR RESPONSE REQUEST =====");
            System.out.println("🔍 Request URI: " + httpRequest.getRequestURI());
            System.out.println("🔍 Request Method: " + httpRequest.getMethod());
            System.out.println("🔍 Content-Type: " + httpRequest.getContentType());
            System.out.println("🔍 Content-Length: " + httpRequest.getContentLength());
            System.out.println("🔍 Raw Request Body: '" + cachedRequest.getBody() + "'");
            System.out.println("🔍 Body length: " + cachedRequest.getBody().length());
            System.out.println("🔍 ====================================");
            
            chain.doFilter(cachedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
