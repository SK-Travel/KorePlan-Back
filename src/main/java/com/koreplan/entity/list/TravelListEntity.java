package com.koreplan.entity.list;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "travel_list")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TravelListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travelPlanId", nullable = false)
    @JsonBackReference
    private TravelPlanEntity travelPlan;

    private String addr1;
    private String addr2;

    @Column(name = "c1Code")
    private String c1Code;

    @Column(name = "c2Code")
    private String c2Code;

    @Column(name = "c3Code")
    private String c3Code;

    @Column(name = "contentId")
    private String contentId;

    @Column(name = "contentTypeId")
    private Integer contentTypeId;

    private Integer day;

    @Column(name = "`order`")
    private Integer order;

    private String firstimage;
    private String firstimage2;

    @Column(name = "likeCount")
    private Integer likeCount;

    private String mapx;
    private String mapy;

    private Double rating;

    @Column(name = "regionCode")
    private Integer regionCode;

    @Column(name = "regionName")
    private String regionName;

    @Column(name = "reviewCount")
    private Integer reviewCount;

    private String tel;
    private Integer theme;
    private String title;

    @Column(name = "viewCount")
    private Integer viewCount;

    @Column(name = "wardCode")
    private Integer wardCode;

    @Column(name = "wardName")
    private String wardName;
}