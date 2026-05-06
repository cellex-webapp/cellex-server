package com.example.cellex.services.address;

import com.example.cellex.dtos.response.address.*;
import com.example.cellex.models.address.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing address data with dual system support (before/after 07/2025)
 * All data is cached in memory for optimal performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final ObjectMapper objectMapper;
    
    // Legacy data (current system - provinces.json, communes.json)
    private List<Province> provinces = new ArrayList<>();
    private List<Commune> communes = new ArrayList<>();
    
    // Old address system (before 07/2025) - data.json
    private List<OldProvince> oldProvinces = new ArrayList<>();
    private Map<String, OldProvince> oldProvinceMap = new ConcurrentHashMap<>(); // provinceId -> OldProvince
    private Map<String, OldDistrict> oldDistrictMap = new ConcurrentHashMap<>(); // districtId -> OldDistrict
    private Map<String, String> districtToProvinceMap = new ConcurrentHashMap<>(); // districtId -> provinceId
    private Map<String, String> oldWardToDistrictMap = new ConcurrentHashMap<>(); // wardId -> districtId
    
    // New address system (after 07/2025) - data_new.json
    private List<NewProvince> newProvinces = new ArrayList<>();
    private Map<String, NewProvince> newProvinceMap = new ConcurrentHashMap<>(); // provinceCode -> NewProvince
    private Map<String, NewWard> newWardMap = new ConcurrentHashMap<>(); // wardCode -> NewWard
    
    // Ward mappings - ward_mappings.json
    private List<WardMapping> wardMappings = new ArrayList<>();
    private Map<String, WardMapping> oldToNewWardMap = new ConcurrentHashMap<>(); // oldWardCode -> WardMapping
    private Map<String, List<WardMapping>> newToOldWardsMap = new ConcurrentHashMap<>(); // newWardCode -> List<WardMapping>

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            // Load legacy data
            loadProvinces();
            loadCommunes();
            
            // Load old address system data
            loadOldAddressData();
            
            // Load new address system data
            loadNewAddressData();
            
            // Load ward mappings
            loadWardMappings();
            
            log.info("Successfully loaded address data:");
            log.info("  - Legacy: {} provinces, {} communes", provinces.size(), communes.size());
            log.info("  - Old system: {} provinces", oldProvinces.size());
            log.info("  - New system: {} provinces", newProvinces.size());
            log.info("  - Ward mappings: {}", wardMappings.size());
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

    private void loadOldAddressData() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            oldProvinces = objectMapper.readValue(inputStream, new TypeReference<List<OldProvince>>() {});
        }
        
        // Build lookup maps
        oldProvinceMap = new ConcurrentHashMap<>();
        oldDistrictMap = new ConcurrentHashMap<>();
        districtToProvinceMap = new ConcurrentHashMap<>();
        oldWardToDistrictMap = new ConcurrentHashMap<>();
        
        for (OldProvince province : oldProvinces) {
            oldProvinceMap.put(province.getId(), province);
            if (province.getDistricts() != null) {
                for (OldDistrict district : province.getDistricts()) {
                    oldDistrictMap.put(district.getId(), district);
                    districtToProvinceMap.put(district.getId(), province.getId());
                    if (district.getWards() != null) {
                        for (OldWard ward : district.getWards()) {
                            oldWardToDistrictMap.put(ward.getId(), district.getId());
                        }
                    }
                }
            }
        }
    }

    private void loadNewAddressData() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/data_new.json");
        try (InputStream inputStream = resource.getInputStream()) {
            newProvinces = objectMapper.readValue(inputStream, new TypeReference<List<NewProvince>>() {});
        }
        
        // Build lookup maps
        newProvinceMap = new ConcurrentHashMap<>();
        newWardMap = new ConcurrentHashMap<>();
        
        for (NewProvince province : newProvinces) {
            newProvinceMap.put(province.getProvinceCode(), province);
            if (province.getWards() != null) {
                for (NewWard ward : province.getWards()) {
                    newWardMap.put(ward.getWardCode(), ward);
                }
            }
        }
    }

    private void loadWardMappings() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/ward_mappings.json");
        try (InputStream inputStream = resource.getInputStream()) {
            wardMappings = objectMapper.readValue(inputStream, new TypeReference<List<WardMapping>>() {});
        }
        
        // Build lookup maps
        oldToNewWardMap = new ConcurrentHashMap<>();
        newToOldWardsMap = new ConcurrentHashMap<>();
        
        for (WardMapping mapping : wardMappings) {
            String oldWardCode = mapping.getOld().getWardCode();
            String newWardCode = mapping.getNewWard().getWardCode();
            
            oldToNewWardMap.put(oldWardCode, mapping);
            
            newToOldWardsMap.computeIfAbsent(newWardCode, k -> new ArrayList<>()).add(mapping);
        }
    }

    // ==================== Legacy Methods ====================
    
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

    // ==================== Old Address System (Before 07/2025) ====================
    
    /**
     * Get all provinces from old address system
     */
    public List<OldProvinceResponse> getOldProvinces() {
        return oldProvinces.stream()
                .map(p -> OldProvinceResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .type(p.getType())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get districts by province ID from old address system
     */
    public List<OldDistrictResponse> getOldDistrictsByProvince(String provinceId) {
        OldProvince province = oldProvinceMap.get(provinceId);
        if (province == null || province.getDistricts() == null) {
            return Collections.emptyList();
        }
        return province.getDistricts().stream()
                .map(d -> OldDistrictResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .type(d.getType())
                        .provinceId(provinceId)
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get wards by district ID from old address system
     */
    public List<OldWardResponse> getOldWardsByDistrict(String districtId) {
        OldDistrict district = oldDistrictMap.get(districtId);
        if (district == null || district.getWards() == null) {
            return Collections.emptyList();
        }
        String provinceId = districtToProvinceMap.get(districtId);
        return district.getWards().stream()
                .map(w -> OldWardResponse.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .type(w.getType())
                        .districtId(districtId)
                        .provinceId(provinceId)
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== New Address System (After 07/2025) ====================
    
    /**
     * Get all provinces from new address system
     */
    public List<NewProvinceResponse> getNewProvinces() {
        return newProvinces.stream()
                .map(p -> NewProvinceResponse.builder()
                        .provinceCode(p.getProvinceCode())
                        .name(p.getName())
                        .shortName(p.getShortName())
                        .code(p.getCode())
                        .placeType(p.getPlaceType())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Get wards by province code from new address system (no district level)
     */
    public List<NewWardResponse> getNewWardsByProvince(String provinceCode) {
        NewProvince province = newProvinceMap.get(provinceCode);
        if (province == null || province.getWards() == null) {
            return Collections.emptyList();
        }
        return province.getWards().stream()
                .map(w -> NewWardResponse.builder()
                        .wardCode(w.getWardCode())
                        .name(w.getName())
                        .provinceCode(w.getProvinceCode())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== Ward Mapping ====================
    
    /**
     * Map ward code between old and new systems
     * Automatically detects the input type or uses the specified type
     */
    public WardMappingResponse mapWardCode(String wardCode, String codeType) {
        if (wardCode == null || wardCode.trim().isEmpty()) {
            return null;
        }
        
        // Auto-detect code type if not specified
        if (codeType == null || codeType.isEmpty()) {
            // Check if it's an old ward code
            if (oldToNewWardMap.containsKey(wardCode)) {
                codeType = "old";
            } else if (newToOldWardsMap.containsKey(wardCode)) {
                codeType = "new";
            } else {
                return null; // Not found in any system
            }
        }
        
        if ("old".equalsIgnoreCase(codeType)) {
            return mapOldToNew(wardCode);
        } else if ("new".equalsIgnoreCase(codeType)) {
            return mapNewToOld(wardCode);
        }
        
        return null;
    }
    
    private WardMappingResponse mapOldToNew(String oldWardCode) {
        WardMapping mapping = oldToNewWardMap.get(oldWardCode);
        if (mapping == null) {
            return null;
        }
        
        WardMappingResponse.OldAddressInfo oldAddress = WardMappingResponse.OldAddressInfo.builder()
                .wardCode(mapping.getOld().getWardCode())
                .wardName(mapping.getOld().getWardName())
                .districtName(mapping.getOld().getDistrictName())
                .provinceName(mapping.getOld().getProvinceName())
                .fullAddress(String.format("%s, %s, %s",
                        mapping.getOld().getWardName(),
                        mapping.getOld().getDistrictName(),
                        mapping.getOld().getProvinceName()))
                .build();
        
        WardMappingResponse.NewAddressInfo newAddress = WardMappingResponse.NewAddressInfo.builder()
                .wardCode(mapping.getNewWard().getWardCode())
                .wardName(mapping.getNewWard().getWardName())
                .provinceName(mapping.getNewWard().getProvinceName())
                .fullAddress(String.format("%s, %s",
                        mapping.getNewWard().getWardName(),
                        mapping.getNewWard().getProvinceName()))
                .build();
        
        return WardMappingResponse.builder()
                .inputType("old")
                .oldAddress(oldAddress)
                .newAddress(newAddress)
                .build();
    }
    
    private WardMappingResponse mapNewToOld(String newWardCode) {
        List<WardMapping> mappings = newToOldWardsMap.get(newWardCode);
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        
        // Get new address info from first mapping
        WardMapping firstMapping = mappings.get(0);
        WardMappingResponse.NewAddressInfo newAddress = WardMappingResponse.NewAddressInfo.builder()
                .wardCode(firstMapping.getNewWard().getWardCode())
                .wardName(firstMapping.getNewWard().getWardName())
                .provinceName(firstMapping.getNewWard().getProvinceName())
                .fullAddress(String.format("%s, %s",
                        firstMapping.getNewWard().getWardName(),
                        firstMapping.getNewWard().getProvinceName()))
                .build();
        
        // Get all old wards that merged into this new ward
        List<WardMappingResponse.OldAddressInfo> oldWardsMerged = mappings.stream()
                .map(m -> WardMappingResponse.OldAddressInfo.builder()
                        .wardCode(m.getOld().getWardCode())
                        .wardName(m.getOld().getWardName())
                        .districtName(m.getOld().getDistrictName())
                        .provinceName(m.getOld().getProvinceName())
                        .fullAddress(String.format("%s, %s, %s",
                                m.getOld().getWardName(),
                                m.getOld().getDistrictName(),
                                m.getOld().getProvinceName()))
                        .build())
                .collect(Collectors.toList());
        
        return WardMappingResponse.builder()
                .inputType("new")
                .newAddress(newAddress)
                .oldAddress(oldWardsMerged.isEmpty() ? null : oldWardsMerged.get(0))
                .oldWardsMerged(oldWardsMerged)
                .build();
    }

    // ==================== Dual Address Display ====================
    
    /**
     * Build complete dual address response from stored new ward code and detail address
     * This is used when retrieving saved addresses to display both old and new formats
     */
    public DualAddressResponse buildDualAddress(String newWardCode, String detailAddress) {
        if (newWardCode == null || newWardCode.trim().isEmpty()) {
            return null;
        }
        
        // Get new address info
        NewWard newWard = newWardMap.get(newWardCode);
        if (newWard == null) {
            return null;
        }
        
        NewProvince newProvince = newProvinceMap.get(newWard.getProvinceCode());
        String newProvinceName = newProvince != null ? newProvince.getName() : "";
        
        DualAddressResponse.NewAddressDisplay newAddressDisplay = DualAddressResponse.NewAddressDisplay.builder()
                .provinceCode(newWard.getProvinceCode())
                .provinceName(newProvinceName)
                .wardCode(newWard.getWardCode())
                .wardName(newWard.getName())
                .build();
        
        // Build full address for new format
        String fullAddressNew = buildFullAddress(detailAddress, newWard.getName(), null, newProvinceName);
        
        // Get old addresses from mapping
        List<WardMapping> mappings = newToOldWardsMap.get(newWardCode);
        List<DualAddressResponse.OldAddressDisplay> oldAddresses = new ArrayList<>();
        String fullAddressOld = null;
        
        if (mappings != null && !mappings.isEmpty()) {
            for (WardMapping mapping : mappings) {
                // Find old province and district IDs
                String oldWardCode = mapping.getOld().getWardCode();
                String districtId = oldWardToDistrictMap.get(oldWardCode);
                String provinceId = districtId != null ? districtToProvinceMap.get(districtId) : null;
                
                DualAddressResponse.OldAddressDisplay oldDisplay = DualAddressResponse.OldAddressDisplay.builder()
                        .provinceId(provinceId)
                        .provinceName(mapping.getOld().getProvinceName())
                        .districtId(districtId)
                        .districtName(mapping.getOld().getDistrictName())
                        .wardId(oldWardCode)
                        .wardName(mapping.getOld().getWardName())
                        .build();
                oldAddresses.add(oldDisplay);
            }
            
            // Use first old address for full address
            WardMapping first = mappings.get(0);
            fullAddressOld = buildFullAddress(detailAddress, 
                    first.getOld().getWardName(),
                    first.getOld().getDistrictName(),
                    first.getOld().getProvinceName());
        }
        
        return DualAddressResponse.builder()
                .newWardCode(newWardCode)
                .detailAddress(detailAddress)
                .newAddress(newAddressDisplay)
                .oldAddresses(oldAddresses)
                .fullAddressNew(fullAddressNew)
                .fullAddressOld(fullAddressOld)
                .build();
    }
    
    private String buildFullAddress(String detail, String ward, String district, String province) {
        StringBuilder sb = new StringBuilder();
        if (detail != null && !detail.trim().isEmpty()) {
            sb.append(detail.trim());
        }
        if (ward != null && !ward.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward.trim());
        }
        if (district != null && !district.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district.trim());
        }
        if (province != null && !province.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province.trim());
        }
        return sb.toString();
    }

    // ==================== Utility Methods ====================
    
    /**
     * Check if a ward code belongs to the old system
     */
    public boolean isOldWardCode(String wardCode) {
        return oldToNewWardMap.containsKey(wardCode);
    }
    
    /**
     * Check if a ward code belongs to the new system
     */
    public boolean isNewWardCode(String wardCode) {
        return newWardMap.containsKey(wardCode);
    }
    
    /**
     * Get new ward code from old ward code
     */
    public String getNewWardCodeFromOld(String oldWardCode) {
        WardMapping mapping = oldToNewWardMap.get(oldWardCode);
        return mapping != null ? mapping.getNewWard().getWardCode() : null;
    }
    
    /**
     * Get new ward info by code
     */
    public NewWard getNewWardByCode(String wardCode) {
        return newWardMap.get(wardCode);
    }
    
    /**
     * Get new province info by code
     */
    public NewProvince getNewProvinceByCode(String provinceCode) {
        return newProvinceMap.get(provinceCode);
    }
}
