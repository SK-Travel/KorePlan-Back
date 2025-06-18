package com.koreplan.entity.list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "travel_plan")
@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TravelPlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userId", nullable = false)
    private Integer userId;

    private String region;

    private Integer days;
    
    private String title;

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true,  fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<TravelListEntity> travelLists;
	
}
