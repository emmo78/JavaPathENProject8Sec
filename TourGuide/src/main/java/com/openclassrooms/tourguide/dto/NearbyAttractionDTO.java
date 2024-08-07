package com.openclassrooms.tourguide.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.openclassrooms.tourguide.service.RewardsService;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class NearbyAttractionDTO {
    @JsonIgnore
    private final Attraction attraction;
    private final String attractionName;
    private final double attractionLongitude;
    private final double attractionLatitude;
    private final double userLongitude;
    private final double userLatitude;
    private double distanceUserAttractionMiles;
    private int visitingRewardAttractionPoints;

    public void calculateDistanceUserAttractionMiles(RewardsService rewardsService, VisitedLocation visitedLocation) {
        distanceUserAttractionMiles = rewardsService.getDistance(attraction, visitedLocation.location);
    }
}
