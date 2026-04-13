package edu.classproject.review;

import java.util.List;

public interface ReviewService {
    Review submit(Review review);

    List<Review> listByRestaurant(String restaurantId);

    /**
     * Calculate the average rating for a restaurant across all customer reviews.
     * If the same customer has multiple reviews for the same restaurant (from different orders),
     * each review is counted separately in the calculation.
     *
     * @param restaurantId the restaurant identifier
     * @return the average rating (0.0 if no reviews exist)
     */
    double getAverageRating(String restaurantId);
}
