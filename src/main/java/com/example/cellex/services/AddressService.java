package com.example.cellex.services;

import com.example.cellex.models.Commune;
import com.example.cellex.models.Province;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final ObjectMapper objectMapper;
    private List<Province> provinces;
    private List<Commune> communes;

    @PostConstruct
    public void loadData() {
        try {
            loadProvinces();
            loadCommunes();
            log.info("Successfully loaded {} provinces and {} communes",
                    provinces.size(), communes.size());
        } catch (Exception e) {
            log.error("Failed to load address data: {}", e.getMessage());
            throw new RuntimeException("Failed to load address data", e);
        }
    }

    private void loadProvinces() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/provinces.json");
        try (InputStream inputStream = resource.getInputStream()) {
            provinces = objectMapper.readValue(inputStream, new TypeReference<List<Province>>() {});
        }
    }

    private void loadCommunes() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/communes.json");
        try (InputStream inputStream = resource.getInputStream()) {
            communes = objectMapper.readValue(inputStream, new TypeReference<List<Commune>>() {});
        }
    }

    public List<Province> getAllProvinces() {
        return provinces;
    }

    public List<Commune> getCommunesByProvinceCode(String provinceCode) {
        return communes.stream()
                .filter(commune -> commune.getProvinceCode().equals(provinceCode))
                .collect(Collectors.toList());
    }

    public Province getProvinceByCode(String code) {
        return provinces.stream()
                .filter(province -> province.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public Commune getCommuneByCode(String code) {
        return communes.stream()
                .filter(commune -> commune.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
