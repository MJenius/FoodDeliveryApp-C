package edu.classproject.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryReviewRepository implements ReviewRepository {
    private final List<Review> reviews = new ArrayList<>();

    public InMemoryReviewRepository() {
    }

    @Override
    public Review save(Review review) {
        reviews.add(review);
        return review;
    }

    @Override
    public Optional<Review> findByOrderId(String orderId) {
        return reviews.stream()
                .filter(review -> review.orderId().equals(orderId))
                .findFirst();
    }

    @Override
    public List<Review> findByRestaurantId(String restaurantId) {
        return reviews.stream()
                .filter(review -> restaurantId.equals(review.restaurantId()))
                .toList();
    }
}
