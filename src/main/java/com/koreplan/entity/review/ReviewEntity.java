package com.koreplan.entity.review;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.koreplan.data.entity.DataEntity;
import com.koreplan.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 어떤 데이터(관광지, 맛집 등)에 대한 리뷰인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_id", referencedColumnName = "id", nullable = false)
    private DataEntity dataEntity;
    
    // 리뷰 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity userEntity;
    
    // 평점 (1~5점)
    @Column(nullable = false)
    private int rating;

    // 리뷰 내용
    @Column(length = 2000, nullable = false)
    private String content;

    
    // 생성 시간
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // 수정 시간
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 편의 메서드들
    
    /**
     * 평점이 유효한 범위(1-5)인지 확인
     */
    public boolean isValidRating() {
        return rating >= 1 && rating <= 5;
    }
    
    
}