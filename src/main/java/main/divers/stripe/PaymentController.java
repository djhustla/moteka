



package main.divers.stripe;

import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostMapping("/create-payment-intent")
    public Map<String, String> createPaymentIntent(@RequestParam double montant) {
        Stripe.apiKey = stripeSecretKey;

        try {
            long amountInCents = Math.round(montant * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            return response;

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping("/create-onboarding-link")
    public Map<String, String> createStripeAccount() {
        Stripe.apiKey = stripeSecretKey;

        try {
            // A. Création du compte Stripe Connect (type Express)
            AccountCreateParams accountParams = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                                    .build()
                    )
                    .build();

            Account account = Account.create(accountParams);

            // B. Création du lien d'inscription (Onboarding)
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("https://votre-site.com/reessayer") // Si le lien expire
                    .setReturnUrl("https://votre-site.com/succes-inscription") // Retour après saisie IBAN
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);

            // On retourne l'URL à ouvrir et l'ID du compte pour le stocker en BDD
            Map<String, String> response = new HashMap<>();
            response.put("url", accountLink.getUrl());
            response.put("accountId", account.getId());
            return response;

        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }
    }

}