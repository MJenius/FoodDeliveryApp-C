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
        
        // Register the restaurant ID if using InMemoryReviewRepository
        if (reviewRepository instanceof InMemoryReviewRepository) {
            ((InMemoryReviewRepository) reviewRepository).registerOrderRestaurant(
                    review.orderId(),
                    order.restaurantId()
            );
        }
        
        Review reviewWithId = new Review(
                IdGenerator.nextId("REV"),
                review.orderId(),
                review.customerId(),
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

    private Order validateExistingOrder(String orderId) {
        Order order = orderService.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order;
    }
}
