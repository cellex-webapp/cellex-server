package com.example.cellex.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Cellex API", version = "1.0", description = "API Documentation for Cellex Application"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag().name("01. Authentication").description("Endpoints for user authentication and registration"));
        tags.add(new Tag().name("02. User Management").description("APIs for managing user accounts and profiles"));
        tags.add(new Tag().name("03. Address Management").description("APIs for managing Vietnam address data"));
        tags.add(new Tag().name("04. Customer Segments").description("API quản lý phân khúc khách hàng"));
        tags.add(new Tag().name("05. Segment Coupons").description("API quản lý coupon theo phân khúc khách hàng với cấu hình lịch phát linh hoạt"));
        tags.add(new Tag().name("06. User Coupons").description("API quản lý coupon của người dùng"));
        tags.add(new Tag().name("07. Customer Segmentation Operations").description("API quản lý phân khúc và nâng hạng khách hàng"));
        tags.add(new Tag().name("08. Shop Management").description("APIs for shop management"));
        tags.add(new Tag().name("09. Category Management").description("APIs for creating, reading, updating, and deleting product categories."));
        tags.add(new Tag().name("10. Category Attributes").description("APIs quản lý thuộc tính danh mục sản phẩm - Định nghĩa các thuộc tính riêng cho từng danh mục (VD: RAM, CPU cho laptop)"));
        tags.add(new Tag().name("11. Products").description("APIs quản lý sản phẩm"));
        
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Cellex API")
                        .version("1.0")
                        .description("API Documentation for Cellex Application"))
                .tags(tags);
    }
}