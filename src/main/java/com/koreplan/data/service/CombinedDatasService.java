package com.koreplan.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.area.repository.RegionCodeRepository;
import com.koreplan.area.repository.WardCodeRepository;
import com.koreplan.data.repository.DataRepository;

@Service
public class CombinedDatasService {
	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private RegionCodeRepository regionCodeRepository;
	
	@Autowired
	private WardCodeRepository wardCodeRepository;
	
	
	public void updateData(String regioncode, String wardcode) {
		//데이터, 지역, 카테고리 데이터 베이스 조회
		//int regionId = regionCodeRepository.getByRegioncode(Integer.valueOf(regioncode));
	}
}
