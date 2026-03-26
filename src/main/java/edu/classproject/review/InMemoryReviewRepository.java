package edu.classproject.review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReviewRepository implements ReviewRepository {
    private final Map<String, Review> reviews = new HashMap<>();
    private final Map<String, String> restaurantByOrderId = new HashMap<>();

    public InMemoryReviewRepository() {
    }

    public void registerOrderRestaurant(String orderId, String restaurantId) {
        restaurantByOrderId.put(orderId, restaurantId);
    }

    @Override
    public Review save(Review review) {
        reviews.put(review.reviewId(), review);
        return review;
    }

    @Override
    public Optional<Review> findByOrderId(String orderId) {
        return reviews.values()
                .stream()
                .filter(review -> review.orderId().equals(orderId))
                .findFirst();
    }

    @Override
    public List<Review> findByRestaurantId(String restaurantId) {
        return reviews.values()
                .stream()
                .filter(review -> restaurantId.equals(restaurantByOrderId.get(review.orderId())))
                .toList();
    }
}
