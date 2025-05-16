package com.koreplan.place.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceDTO {
    private String name;
    private String address;
    private double lat;
    private double lng;
}