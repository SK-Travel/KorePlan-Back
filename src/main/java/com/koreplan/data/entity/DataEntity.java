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
	private String contentId;
	private String addr1;
	@Column(nullable = true)
	private String addr2;
//	private String regioncode;
//
//	private String wardcode;
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
	
	@Column(name = "view_count")
	private int viewCount = 0;
	@Column(name = "review_count")
    private Integer reviewCount = 0;
    @Column(name = "rating")
    private Double rating = 0.0;
    @Column(name = "score")
    private Double score = 0.0;
	// regioncode → 연관관계 설정
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "regioncodeId", referencedColumnName = "id")
	private RegionCodeEntity regionCodeEntity;
	// wardcode → 연관관계 설정
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "wardcodeId", referencedColumnName = "id")
	private WardCodeEntity wardCodeEntity;
	
}