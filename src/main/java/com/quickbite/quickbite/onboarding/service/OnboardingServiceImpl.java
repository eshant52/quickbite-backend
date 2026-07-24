package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantRequest;
import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantResponse;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.model.RefreshToken;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocument;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationHistory;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.model.MenuItemImage;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderItem;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantDocument;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import com.quickbite.quickbite.restaurant.model.RestaurantHours;
import com.quickbite.quickbite.restaurant.model.RestaurantImage;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatusHistory;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocument;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipStatusHistory;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import com.quickbite.quickbite.auth.repository.RefreshTokenRepository;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OnboardingServiceImpl implements OnboardingService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public OnboardingServiceImpl(UserRepository userRepository,
                                 RestaurantRepository restaurantRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public OnboardRestaurantResponse submitRestaurantApplication(OnboardRestaurantRequest request) {
        // 1. Add restaurant information
        // 2. Restaurant address
        // 3. Restaurant document
        // 4. Restaurant hours
        // 5. Restaurant image upload

        // 6. Send verification details to admins. (Optional)
        return new OnboardRestaurantResponse(null, "Application submitted; pending admin review");
    }

    @Override
    public void approveApplication(UUID applicationId, String reviewerRemarks) {

    }
}
