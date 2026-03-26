package edu.classproject.review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);

    Optional<Review> findByOrderId(String orderId);

    List<Review> findByRestaurantId(String restaurantId);
}
