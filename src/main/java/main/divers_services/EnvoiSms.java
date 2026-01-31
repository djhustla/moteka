package main.divers_services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvoiSms {

    private static String accountSid;
    private static String authToken;
    private static String twilioNumber;

    @Value("${twilio.account.sid}")
    private String sid;

    @Value("${twilio.auth.token}")
    private String token;

    @Value("${twilio.phone.number}")
    private String phone;

    @PostConstruct
    public void init() {
        EnvoiSms.accountSid = this.sid;
        EnvoiSms.authToken = this.token;
        EnvoiSms.twilioNumber = this.phone;
        // Initialisation globale de Twilio
        Twilio.init(accountSid, authToken);
    }

    public static void envoyerSms(String numeroDestinataire, String messagePourEnvoyer) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(numeroDestinataire),
                    new PhoneNumber(twilioNumber),
                    messagePourEnvoyer
            ).create();
            System.out.println("SMS envoyé ! ID : " + message.getSid());
        } catch (Exception e) {
            System.err.println("Erreur Twilio : " + e.getMessage());
        }
    }
}