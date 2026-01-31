package main.divers.stripe;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SubscriptionController {


    @Value("${stripe.secret-key}")
    private String stripeSecretKey;



    @PostMapping("/create-subscription")
    public Map<String, String> createSubscription() {
        // Ta clé secrète LIVE
            Stripe.apiKey = stripeSecretKey;

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION) // Indispensable pour l'abonnement
                    .setSuccessUrl("http://localhost:8080/success.html")
                    .setCancelUrl("http://localhost:8080/index.html")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    // Ton ID de prix que tu viens de créer
                                    .setPrice("price_1Sqb2kJOvQnra8JXtcXSJFCi")
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            Map<String, String> response = new HashMap<>();
            // On renvoie l'URL de la page sécurisée Stripe
            response.put("url", session.getUrl());
            return response;

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }
}