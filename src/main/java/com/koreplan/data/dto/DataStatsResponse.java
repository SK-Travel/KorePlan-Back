package com.koreplan.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataStatsResponse {
    private String contentId;     // 컨텐츠 ID
    private int viewCount;        // 조회수
    private double rating;        // 평점
    private int reviewCount;      // 리뷰 수
    private double score;         // 종합 점수
}