package edu.classproject.review;

public record Review(String reviewId, String orderId, String customerId, String restaurantId, int rating, String comment) {
}
