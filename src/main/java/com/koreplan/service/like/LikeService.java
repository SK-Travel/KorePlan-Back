package com.koreplan.service.like;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koreplan.entity.like.LikeEntity;
import com.koreplan.repository.like.LikeRepository;

@Service
public class LikeService {
	
	@Autowired
	private LikeRepository likeRepository;
	
	public int likeToggle (Long dataId, int userId) {
		// 조회
		boolean exists = likeRepository.existsByDataIdAndUserId(dataId, userId);
				
		// 여부 => 삭제 or 추가
		if (exists) {
			// 찜 되어있으면 삭제
			likeRepository.deleteByDataIdAndUserId(dataId, userId);
			return 0; // 찜 취소
		} else {
			LikeEntity like = LikeEntity.builder()
					.dataId(dataId)
					.userId(userId)
					.build();
			likeRepository.save(like);
			return 1; // 찜 추가
		}
	}
	
}
