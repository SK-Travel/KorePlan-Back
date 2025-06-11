package com.koreplan.entity.festival;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.koreplan.area.entity.RegionCodeEntity;
import com.koreplan.area.entity.WardCodeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "festival")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalEntity {
    
    @Id
    @Column(name = "content_id")
    private String contentId;
    
    @Column(name = "content_type_id", nullable = false)
    private Integer contentTypeId; // 항상 15로 고정
    
    // 6번 API (소개 정보)에서 가져오는 필드들
    @Column(name = "event_start_date", nullable = false)
    private LocalDate eventStartDate;
    
    @Column(name = "event_end_date", nullable = false)
    private LocalDate eventEndDate;
    
    // 5번 API (공통 정보)에서 가져오는 필드들
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "first_image", length = 500)
    private String firstimage;
    
    @Column(name = "first_image2", length = 500)
    private String firstimage2;
    
    // 지역 연관관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code_id", referencedColumnName = "id")
    private RegionCodeEntity regionCodeEntity;
    
    // 시군구 연관관계 설정  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code_id", referencedColumnName = "id")
    private WardCodeEntity wardCodeEntity;
    
    private String c1Code;
	private String c2Code;
	private String c3Code;
    
    @Column(name = "addr1", length = 200)
    private String addr1;
    
    @Column(name = "addr2", length = 100)
    private String addr2;
    
    @Column(name = "mapx", length = 20)
    private String mapx;
    
    @Column(name = "mapy", length = 20)
    private String mapy;
    
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;
    
    @Column(name = "view_count")
	private int viewCount = 0;
    
//    // 데이터 관리용 필드들
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//    
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
    
    // 축제 상태 확인 유틸리티 메서드들
    public boolean isOngoing() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(eventStartDate) && !now.isAfter(eventEndDate);
    }
    
    public boolean isUpcoming() {
        LocalDate now = LocalDate.now();
        return now.isBefore(eventStartDate);
    }
}
