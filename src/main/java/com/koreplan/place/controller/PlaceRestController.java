package com.koreplan.place.controller;

import com.koreplan.place.dto.PlaceDTO;
import com.koreplan.place.service.PlaceService;
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
    public ResponseEntity<?> getPlace(@RequestParam String keyword) {
        PlaceDTO place = placeService.getPlaceByKeyword(keyword);

        if (place == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("해당 장소를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(place);
    }
}
