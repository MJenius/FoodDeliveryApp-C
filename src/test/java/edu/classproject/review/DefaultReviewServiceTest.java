package edu.classproject.review;

import edu.classproject.common.Money;
import edu.classproject.order.Order;
import edu.classproject.order.OrderItem;
import edu.classproject.order.OrderRequest;
import edu.classproject.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultReviewServiceTest {

    // ── Stub OrderService ──────────────────────────────────────────────────────
    /** Resolves orders from a pre-built id→order map. */
    private static class StubOrderService implements OrderService {
        private final Map<String, Order> orders;

        StubOrderService(Order... orders) {
            this.orders = new java.util.HashMap<>();
            for (Order o : orders) this.orders.put(o.orderId(), o);
        }

        @Override
        public Order placeOrder(OrderRequest request) { return null; }

        @Override
        public Order getOrder(String orderId) { return orders.get(orderId); }

        @Override
        public void updateStatus(String orderId, String status) { }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private static Order makeOrder(String orderId, String customerId, String restaurantId) {
        OrderItem item = new OrderItem("ITEM-1", "Burger", Money.of(5.00), 1);
        return new Order(orderId, customerId, restaurantId, List.of(item));
    }

    /** Builds an unsigned Review (no reviewId / restaurantId — simulating caller input). */
    private static Review unsignedReview(String orderId, String customerId, int rating, String comment) {
        return new Review(null, orderId, customerId, null, rating, comment);
    }

    private InMemoryReviewRepository repo;
    private Order defaultOrder;
    private DefaultReviewService service;

    @BeforeEach
    void setUp() {
        repo         = new InMemoryReviewRepository();
        defaultOrder = makeOrder("ORD-1", "CUST-1", "REST-1");
        service      = new DefaultReviewService(repo, new StubOrderService(defaultOrder));
    }

    // ── submit ─────────────────────────────────────────────────────────────────

    // TC-01: happy path — review is saved and returned with a generated ID
    @Test
    void submit_shouldReturnSavedReview_withGeneratedId() {
        Review result = service.submit(unsignedReview("ORD-1", "CUST-1", 5, "Great food!"));

        assertNotNull(result.reviewId());
        assertTrue(result.reviewId().startsWith("REV-"));
    }

    // TC-02: saved review carries the restaurantId resolved from the order
    @Test
    void submit_shouldAttachRestaurantId_fromOrder() {
        Review result = service.submit(unsignedReview("ORD-1", "CUST-1", 4, "Good"));

        assertEquals("REST-1", result.restaurantId());
    }

    // TC-03: saved review preserves all caller-supplied fields unchanged
    @Test
    void submit_shouldPreserveAllFields() {
        Review result = service.submit(unsignedReview("ORD-1", "CUST-1", 3, "Average"));

        assertEquals("ORD-1",   result.orderId());
        assertEquals("CUST-1",  result.customerId());
        assertEquals(3,         result.rating());
        assertEquals("Average", result.comment());
    }

    // TC-04: review is persisted — findByOrderId returns it afterwards
    @Test
    void submit_shouldPersistReview_retrievableByOrderId() {
        service.submit(unsignedReview("ORD-1", "CUST-1", 5, "Excellent"));

        assertTrue(repo.findByOrderId("ORD-1").isPresent());
    }

    // TC-05: wrong customer throws IllegalArgumentException
    @Test
    void submit_shouldThrow_whenCustomerDoesNotMatchOrder() {
        Review wrongCustomer = unsignedReview("ORD-1", "CUST-WRONG", 5, "Nice");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submit(wrongCustomer)
        );
        assertTrue(ex.getMessage().contains("Only the customer who placed the order"));
    }

    // TC-06: non-existent orderId throws IllegalArgumentException
    @Test
    void submit_shouldThrow_whenOrderDoesNotExist() {
        DefaultReviewService svc = new DefaultReviewService(repo, new StubOrderService(/* empty */));
        Review input = unsignedReview("ORD-MISSING", "CUST-1", 4, "Hmm");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> svc.submit(input)
        );
        assertTrue(ex.getMessage().contains("Order not found"));
    }

    // TC-07: two reviews for the same restaurant, both returned in list
    @Test
    void submit_shouldAllowMultipleReviews_fromDifferentOrders() {
        Order order2 = makeOrder("ORD-2", "CUST-2", "REST-1");
        DefaultReviewService svc = new DefaultReviewService(
                repo, new StubOrderService(defaultOrder, order2));

        svc.submit(unsignedReview("ORD-1", "CUST-1", 5, "Loved it"));
        svc.submit(unsignedReview("ORD-2", "CUST-2", 3, "It was ok"));

        assertEquals(2, svc.listByRestaurant("REST-1").size());
    }

    // TC-08: each submitted review gets a unique reviewId
    @Test
    void submit_shouldGenerateUniqueIds_forEachReview() {
        Order order2 = makeOrder("ORD-2", "CUST-2", "REST-1");
        DefaultReviewService svc = new DefaultReviewService(
                repo, new StubOrderService(defaultOrder, order2));

        Review r1 = svc.submit(unsignedReview("ORD-1", "CUST-1", 5, "A"));
        Review r2 = svc.submit(unsignedReview("ORD-2", "CUST-2", 4, "B"));

        assertNotEquals(r1.reviewId(), r2.reviewId());
    }

    // ── listByRestaurant ───────────────────────────────────────────────────────

    // TC-09: returns empty list when no reviews exist for a restaurant
    @Test
    void listByRestaurant_shouldReturnEmpty_whenNoReviewsExist() {
        List<Review> result = service.listByRestaurant("REST-NONE");

        assertTrue(result.isEmpty());
    }

    // TC-10: returns only reviews matching the requested restaurantId
    @Test
    void listByRestaurant_shouldReturnOnlyMatchingRestaurant() {
        Order order2 = makeOrder("ORD-2", "CUST-2", "REST-2");
        DefaultReviewService svc = new DefaultReviewService(
                repo, new StubOrderService(defaultOrder, order2));

        svc.submit(unsignedReview("ORD-1", "CUST-1", 5, "REST-1 review"));
        svc.submit(unsignedReview("ORD-2", "CUST-2", 2, "REST-2 review"));

        List<Review> rest1Reviews = svc.listByRestaurant("REST-1");
        assertEquals(1, rest1Reviews.size());
        assertEquals("REST-1", rest1Reviews.get(0).restaurantId());
    }

    // TC-11: returns all reviews when multiple orders share the same restaurant
    @Test
    void listByRestaurant_shouldReturnAll_whenMultipleOrdersSameRestaurant() {
        Order order2 = makeOrder("ORD-2", "CUST-2", "REST-1");
        Order order3 = makeOrder("ORD-3", "CUST-3", "REST-1");
        DefaultReviewService svc = new DefaultReviewService(
                repo, new StubOrderService(defaultOrder, order2, order3));

        svc.submit(unsignedReview("ORD-1", "CUST-1", 5, "A"));
        svc.submit(unsignedReview("ORD-2", "CUST-2", 4, "B"));
        svc.submit(unsignedReview("ORD-3", "CUST-3", 3, "C"));

        assertEquals(3, svc.listByRestaurant("REST-1").size());
    }

    // ── getAverageRating ───────────────────────────────────────────────────────

    // TC-12: returns 0.0 when no reviews exist
    @Test
    void getAverageRating_shouldReturnZero_whenNoReviews() {
        assertEquals(0.0, service.getAverageRating("REST-1"), 0.001);
    }

    // TC-13: returns the exact rating when only one review exists
    @Test
    void getAverageRating_shouldReturnSingleRating_whenOneReviewExists() {
        service.submit(unsignedReview("ORD-1", "CUST-1", 4, "Good"));

        assertEquals(4.0, service.getAverageRating("REST-1"), 0.001);
    }

    // TC-14: correctly averages multiple ratings
    @Test
    void getAverageRating_shouldComputeCorrectAverage_acrossMultipleReviews() {
        Order order2 = makeOrder("ORD-2", "CUST-2", "REST-1");
        Order order3 = makeOrder("ORD-3", "CUST-3", "REST-1");
        DefaultReviewService svc = new DefaultReviewService(
                repo, new StubOrderService(defaultOrder, order2, order3));

        svc.submit(unsignedReview("ORD-1", "CUST-1", 5, "A")); // 5
        svc.submit(unsignedReview("ORD-2", "CUST-2", 3, "B")); // 3
        svc.submit(unsignedReview("ORD-3", "CUST-3", 4, "C")); // 4  → avg = 4.0

        assertEquals(4.0, svc.getAverageRating("REST-1"), 0.001);
    }

    // TC-15: average is 0.0 for a restaurant with no reviews, even when others have reviews
    @Test
    void getAverageRating_shouldReturnZero_forRestaurantWithNoReviews_whenOthersHaveReviews() {
        service.submit(unsignedReview("ORD-1", "CUST-1", 5, "REST-1 has reviews"));

        assertEquals(0.0, service.getAverageRating("REST-NONE"), 0.001);
    }
}
