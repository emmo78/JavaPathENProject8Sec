package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardCentral;
	/*
	 * for Async methods, to run a corresponding execution step in another thread.
	 * instead the common fork/join pool implementation of Executor
	 * Hardware : i7 6700 4 cores HT = 8 cpu Threads so tried many values to 256 with success
	 * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html
	 * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html
	 */
	private final ExecutorService esThreadPoolRS = Executors.newFixedThreadPool(256);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public CompletableFuture<Void> calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		return CompletableFuture.supplyAsync(() -> gpsUtil.getAttractions(), esThreadPoolRS)
			.thenApply(attractions -> attractions
				.stream()
				.filter(attraction -> !user.getUserRewards().containsKey(attraction.attractionName))
				.flatMap(attraction -> userLocations
					.stream()
					.filter(userLocation -> nearAttraction(userLocation, attraction))
					.map(userLocation -> new UserReward(userLocation, attraction)))
					.toList()
			)
			.thenAcceptAsync(userRewards -> userRewards
				.parallelStream()
				.map(userReward -> userReward
					.setRewardPoints(this.getRewardPoints(userReward.attraction, user)))
				.forEach(user::addUserReward)
			, esThreadPoolRS);
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
