package com.example.cellex.services.address;

import com.example.cellex.models.address.Province;
import com.example.cellex.models.address.Commune;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ObjectMapper objectMapper;
    private Map<String, Province> provinceMap;
    private Map<String, Commune> communeMap;

    @PostConstruct
    public void init() throws IOException {
        loadProvinces();
        loadCommunes();
    }

    private void loadProvinces() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/provinces.json");
        List<Province> provinces = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Province>>() {}
        );
        provinceMap = provinces.stream()
                .collect(Collectors.toMap(Province::getCode, province -> province));
    }

    private void loadCommunes() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/communes.json");
        List<Commune> communes = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<Commune>>() {}
        );
        communeMap = communes.stream()
                .collect(Collectors.toMap(Commune::getCode, commune -> commune));
    }

    public Province getProvinceByCode(String code) {
        return provinceMap.get(code);
    }

    public Commune getCommuneByCode(String code) {
        return communeMap.get(code);
    }

    public String getFullAddress(String provinceCode, String communeCode, String detailAddress) {
        Province province = getProvinceByCode(provinceCode);
        Commune commune = getCommuneByCode(communeCode);

        StringBuilder fullAddress = new StringBuilder();

        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            fullAddress.append(detailAddress.trim()).append(", ");
        }

        if (commune != null) {
            fullAddress.append(commune.getName()).append(", ");
        }

        if (province != null) {
            fullAddress.append(province.getName());
        }

        return fullAddress.toString();
    }
}
