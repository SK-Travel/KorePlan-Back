package com.koreplan.service.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.koreplan.category.entity.CategoryEntity;
import com.koreplan.category.repository.CategoryRepository;
import com.koreplan.data.entity.DataEntity;
import com.koreplan.data.repository.DataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatFilterDataService {
	private final CategoryRepository categoryRepository;
	private final DataRepository dataRepository;
	//대분류 찾기 기능
	public List<DataEntity> findTopCategoryCode(String categoryname) {
		//정상적으로 메소드 시작되는지 여부 확인하기 위해 사용
		log.info("메서드 시작 - categoryname: {}", categoryname);
		//아직 받아온 값X 숙박에 대한 코드 확인하기 위해 하드코딩으로 처리.
		String categoryname_ = "숙박";
		log.info("검색할 카테고리명: {}", categoryname_);

		// 전체 카테고리 조회 -> 현재 기준으로 가장 상위 카테고리 전부 뽑아옴 -> EX) AC : 숙박 >> AC01~AC14까지
		//카테고리 이름을 받고 그 값에 대한 코드를 DB에서 가져와서 리스트에 저장한다.
		List<CategoryEntity> categories = categoryRepository.findByC1Name(categoryname_);//리스트에 숙박 기준 14개의 하위 카테고리 넣어짐.
		log.info("조회된 카테고리 개수: {}", categories.size()); //숙박 기준 14개 로그 찍힘

		if (categories.isEmpty()) { //만약 카테고리가 없으면
			log.warn("카테고리를 찾을 수 없음: {}", categoryname_);//카테고리 이름을 로그 출력
			return List.of();
		}

		

		// 각 카테고리에 대해 데이터 조회
		// for (CategoryEntity category : categories) {
		// 어차피 각 대분류에 대한 모든 중분류는 대분류의 코드값과 동일하기에 첫번째만 써도 무관함.
		CategoryEntity category = categories.getFirst(); //categories 리스트의 모든 원소는 categoryname값의 모든 하위 카테고리 값이 된다.
		log.info("처리 중인 카테고리: {}, 코드: {}", category.getC1Name(), category.getC1Code()); //해당 이름과 코드 로그 출력
		
		//카테고리 코드 값의 C1코드 즉, 대분류 값을 가져와서 저장한다.
		String categorycode = category.getC1Code();
		//카테고리 코드 값에 대한 데이터를 DB에서 가져오게 한다.
		List<DataEntity> results = dataRepository.findByC1Code(categorycode);
		
		//결과가 제대로 들어왔나 확인하는 로그 출력 -> 여기서 출력나온 개수와 DB에서 Select where 코드 해서 나온 개수와 일치하는지 확인한다.
		log.info("카테고리 코드 {} 에 대한 결과 개수: {}", categorycode, results.size());
		for (DataEntity result : results) {
			log.info("결과 {}",result);
		}
		return results;
	}
	//중분류 찾기 기능
	public List<DataEntity> findMiddleCategoryCode(String categoryname){
		String categoryname_ = "호텔"; //하드코딩 
		log.info("검색할 중분류명: {}", categoryname_);
		
		//중분류에 대한 이름값을 받으면 그에 해당하는 중분류 코드값을 DB에서 조회
		List<CategoryEntity> categories = categoryRepository.findByC2Name(categoryname_);
		
		//해당 중분류의 모든 하위값은 하나의 중분류를 갖기에 첫번째거로 사용한다.
		CategoryEntity category = categories.getFirst();
		log.info("처리 중인 카테고리: {}, 코드: {}", category.getC2Name(), category.getC2Code());
		
		//카테고리의 엔티티의 C2코드를 변수에 담는다.
		String categorycode = category.getC2Code();
	    log.info("조회된 카테고리 개수: {}", categories.size());//하위 카테고리 몇개 있는지 확인
	    
	    
	  
	    List<DataEntity> results = dataRepository.findByC2Code(categorycode);
	    log.info("카테고리 코드 {} 에 대한 결과 개수: {}", categorycode, results.size());
	    for (DataEntity result : results) {
			log.info("결과 {}",result.getTitle());
		}
		return results;
		
	}
	//소분류 찾기 기능
	public List<DataEntity> findBotCategoryCode(String categoryname){
		String categoryname_ = "호텔"; //하드코딩 
		log.info("검색할 소분류명: {}", categoryname_);
		
		//소분류에 대한 이름값을 받으면 그에 해당하는 중분류 코드값을 DB에서 조회
		CategoryEntity category= categoryRepository.findByC3Name(categoryname_);
		log.info("처리 중인 카테고리: {}, 코드: {}", category.getC2Name(), category.getC2Code());
		
		//카테고리의 엔티티의 C2코드를 변수에 담는다.
		String categorycode = category.getC3Code();
	    List<DataEntity> results = dataRepository.findByC3Code(categorycode);
	    log.info("카테고리 코드 {} 에 대한 결과 개수: {}", categorycode, results.size());
	    for (DataEntity result : results) {
			log.info("결과 {}",result.getTitle());
		}
		return results;
	}
	
	//입력받은 카테고리 별 하위 카테고리 리스트 반환하는 함수
	public List<String> findSubCategoryName(String categoryname, int level) {
	    // 현재 상태에 관한 하위 카테고리 이름들을 저장할 리스트 선언
	    List<String> subcategories = new ArrayList<>();
	    
	    log.info("=== findSubCategoryName 시작 ===");
	    log.info("입력 파라미터 - categoryname: {}, level: {}", categoryname, level);

	    // 레벨이 1일 때: 최상위 카테고리 선택됨 -> 중간 카테고리 이름들 반환
	    if (level == 1) {
	        log.info("레벨 1 처리: 최상위 카테고리 '{}' 의 중간 카테고리들 조회", categoryname);
	        List<CategoryEntity> categories = categoryRepository.findByC1Name(categoryname);
	        log.info("DB에서 조회된 카테고리 개수: {}", categories.size());
	        
	        for (CategoryEntity category : categories) {
	            log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", 
	                     category.getC1Name(), category.getC2Name(), category.getC3Name());
	            
	            // 이미 리스트에 있으면 건너뛰기 (중복 방지)
	            if (subcategories.contains(category.getC2Name())) {
	                log.debug("중복된 C2 카테고리 건너뛰기: {}", category.getC2Name());
	                continue;
	            }
	            subcategories.add(category.getC2Name());
	            log.debug("C2 카테고리 추가됨: {}", category.getC2Name());
	        }
	        log.info("레벨 1 처리 완료. 반환할 중간 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
	        return subcategories;
	    }
	    
	    // 레벨이 2일 때: 중간 카테고리 선택됨 -> 최하위 카테고리 이름들 반환
	    if (level == 2) {
	        log.info("레벨 2 처리: 중간 카테고리 '{}' 의 최하위 카테고리들 조회", categoryname);
	        List<CategoryEntity> categories = categoryRepository.findByC2Name(categoryname);
	        log.info("DB에서 조회된 카테고리 개수: {}", categories.size());
	        
	        for (CategoryEntity category : categories) {
	            log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", 
	                     category.getC1Name(), category.getC2Name(), category.getC3Name());
	            
	            // 이미 리스트에 있으면 건너뛰기 (중복 방지)
	            if (subcategories.contains(category.getC3Name())) {
	                log.debug("중복된 C3 카테고리 건너뛰기: {}", category.getC3Name());
	                continue;
	            }
	            subcategories.add(category.getC3Name());
	            log.debug("C3 카테고리 추가됨: {}", category.getC3Name());
	        }
	        log.info("레벨 2 처리 완료. 반환할 최하위 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
	        return subcategories;
	    }
	    
	    // 레벨이 0일 때: 아무것도 선택 안됨 -> 최상위 카테고리 이름들 반환
	    log.info("레벨 0 처리: 모든 최상위 카테고리들 조회");
	    List<CategoryEntity> categories = categoryRepository.findAll();
	    log.info("DB에서 조회된 전체 카테고리 개수: {}", categories.size());
	    
	    for (CategoryEntity category : categories) {
	        log.debug("현재 처리 중인 카테고리: C1={}, C2={}, C3={}", 
	                 category.getC1Name(), category.getC2Name(), category.getC3Name());
	        
	        // 이미 리스트에 있으면 건너뛰기 (중복 방지)
	        if (subcategories.contains(category.getC1Name())) {
	            log.debug("중복된 C1 카테고리 건너뛰기: {}", category.getC1Name());
	            continue;
	        }
	        subcategories.add(category.getC1Name());
	        log.debug("C1 카테고리 추가됨: {}", category.getC1Name());
	    }
	    log.info("레벨 0 처리 완료. 반환할 최상위 카테고리 개수: {}, 목록: {}", subcategories.size(), subcategories);
	    log.info("=== findSubCategoryName 종료 ===");
	    return subcategories;
	}
	
	//테스트용 함수.
//	@PostConstruct
//	public void init() {
//		String a ="1";
//		List<DataEntity> top = findTopCategoryCode(a);
//		List<DataEntity> middle = findMiddleCategoryCode(a);
//		List<DataEntity> bot = findBotCategoryCode(a);
//		//List<String> firstCt = findSubCategoryName("음식",1);
//		//List<String> SecondCt = findSubCategoryName("한식",2);
//		//List<String> AllCt = findSubCategoryName("",0);
//	}
}
