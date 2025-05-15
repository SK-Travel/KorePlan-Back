package com.koreplan.place.controller;

import com.koreplan.place.dto.PlaceDTO;
import com.koreplan.place.service.PlaceService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/place")
public class PlaceRestController {

    private final PlaceService placeService;

    public PlaceRestController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public ResponseEntity<?> getPlaces(@RequestParam String keyword) {
        List<PlaceDTO> places = placeService.getPlacesByKeyword(keyword);

        if (places.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 장소를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(places);
    }
}
