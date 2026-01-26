package main.divers_services;

import java.util.Random;

public class CodeGenerator {

    // VOTRE FONCTION SIMPLE
    public static String genererCode() {
        Random rand = new Random();
        return String.format("%06d", rand.nextInt(1000000));
    }


}