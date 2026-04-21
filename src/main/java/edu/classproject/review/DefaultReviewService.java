package edu.classproject.review;

import edu.classproject.common.IdGenerator;
import edu.classproject.order.Order;
import edu.classproject.order.OrderService;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultReviewService implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderService orderService;

    public DefaultReviewService(ReviewRepository reviewRepository, OrderService orderService) {
        this.reviewRepository = reviewRepository;
        this.orderService = orderService;
    }

    @Override
    public Review submit(Review review) {
        Order order = validateExistingOrder(review.orderId());

        // Validate that only the customer who created the order can review it
        if (!order.customerId().equals(review.customerId())) {
            throw new IllegalArgumentException(
                    "Only the customer who placed the order can review it. " +
                    "Order customer: " + order.customerId() + ", Reviewer: " + review.customerId()
            );
        }

        Review reviewWithId = new Review(
                IdGenerator.nextId("REV"),
                review.orderId(),
                review.customerId(),
                order.restaurantId(),
                review.rating(),
                review.comment()
        );

        return reviewRepository.save(reviewWithId);
    }

    @Override
    public List<Review> listByRestaurant(String restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId)
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public double getAverageRating(String restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream()
                .mapToInt(Review::rating)
                .sum();

        return sum / (double) reviews.size();
    }

    private Order validateExistingOrder(String orderId) {
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order;
    }
}
