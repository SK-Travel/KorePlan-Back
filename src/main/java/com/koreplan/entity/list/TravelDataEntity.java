package com.koreplan.entity.list;

import com.koreplan.data.entity.DataEntity;

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
@Table(name = "travel_data")
@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TravelDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DataId 추출해서 데이터 정보 등록
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataId", referencedColumnName = "id", nullable = false)
    private DataEntity dataEntity;

    private Integer day;
    
    @Column(name="`order`")
    private Integer order;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="travelPlanId", referencedColumnName = "id", nullable = false)
    private TravelPlanEntity travelPlan;
	
	
}
