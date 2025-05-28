package com.koreplan.dto.search;

import java.util.List;
import java.util.stream.Collectors;

import com.koreplan.area.entity.RegionCodeEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegionViewDTO {
    private Long regioncode;
    private String name;
    private List<String> wardNames; // wardList 대신 이름만 포함
    
    // Entity를 DTO로 변환하는 정적 메서드
    public static RegionViewDTO from(RegionCodeEntity entity) {
        List<String> wardNames = null;
        
        // wardList가 로드되어 있을 때만 변환
        try {
            wardNames = entity.getWardList().stream()
                    .map(ward -> ward.getName())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Lazy Loading 실패 시 빈 리스트
            wardNames = List.of();
        }
        
        return RegionViewDTO.builder()
                .regioncode((entity.getRegioncode()))
                .name(entity.getName())
                .wardNames(wardNames)
                .build();
    }
    
    // Entity 리스트를 DTO 리스트로 변환
    public static List<RegionViewDTO> fromList(List<RegionCodeEntity> entities) {
        return entities.stream()
                .map(RegionViewDTO::from)
                .collect(Collectors.toList());
    }
}