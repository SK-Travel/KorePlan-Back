package com.koreplan.dto.search;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionDataResponseDto {
	    
	    private String regionName;
	    private String wardName;
	    private Integer totalCount;
	    private List<DataResponseDto> dataList;
	    
//	    // 페이징 정보 (필요시 사용)
//	    private Integer page;
//	    private Integer size;
//	    private Boolean hasNext;
}

