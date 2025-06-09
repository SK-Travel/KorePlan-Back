package com.koreplan.data.dto;

import lombok.Data;

@Data
public class DataStatsDto {
    private int viewCount;
    private int likeCount;
    private int reviewCount;
    private double rating;
}
