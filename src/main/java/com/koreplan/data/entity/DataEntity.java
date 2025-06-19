package com.koreplan.data.entity;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "data")
@Getter
@Setter
public class DataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ===== 기본 정보 =====
    private String contentId;
    private String addr1;
    
    @Column(nullable = true)
    private String addr2;
    
    private String mapx;
    private String mapy;
    private String title;
    private String c1Code;
    private String c2Code;
    private String c3Code;
    private String firstimage;
    private String firstimage2;
    
    @Column(nullable = true)
    private String tel;
    
    private int theme;
    
    // ===== 통계 정보 (실시간 업데이트) =====
    @Column(name = "view_count")
    private int viewCount = 0;
    
    @Column(name = "like_count")
    private int likeCount = 0;
    
    @Column(name = "review_count") 
    private int reviewCount = 0;
    
    @Column(name = "rating")
    private double rating = 0.0;
    
    @Column(name = "score")
    private double score = 0.0;  
    
    // ===== 연관관계 =====
    // regioncode → 연관관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regioncodeId", referencedColumnName = "id")
    private RegionCodeEntity regionCodeEntity;
    
    // wardcode → 연관관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wardcodeId", referencedColumnName = "id")
    private WardCodeEntity wardCodeEntity;
    
    // ===== Score 계산 공식 메모 =====
    /**
     * Score 계산 공식:
     * score = (조회수 ÷ 100 × 1) + (찜수 × 3) + (리뷰수 × 3) + (평점 × 3)
     * 
     * 실시간 업데이트 시점:
     * - 좋아요 토글 → likeCount 변경 → score 재계산
     * - 리뷰 추가/수정/삭제 → reviewCount, rating 변경 → score 재계산
     * - 조회수 증가 → viewCount 변경 → score 재계산
     */
}