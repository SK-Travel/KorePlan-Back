package com.koreplan.entity.theme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="theme")
@Getter
@Setter
public class ThemeEntity {
	@Id
    @Column(name = "contentTypeID")
    private Integer contentTypeId;
    
    @Column(name = "ThemeName", length = 50)
    private String themeName;
}
