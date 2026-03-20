/*

package main.divers_services.encodage_automatique_services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GoogleSearchSimple {

    // On utilise uniquement des agents "Ordinateur" pour plus de stabilité
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
    );

    public static String getRandomUserAgent() {
        return USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));
    }

    public static String getPremierLien(String query, String userAgent) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-agent=" + userAgent);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");

        // CONSEIL : Si ça échoue encore, mets cette ligne en commentaire pour VOIR ce qui bloque
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);
        String resultat = "Aucun lien trouvé";

        try {
            String urlRecherche = "https://www.google.com/search?q=" + query.replace(" ", "+");
            driver.get(urlRecherche);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 1. Gestion des Cookies (on tente de cliquer si le bouton est là)
            try {
                WebElement boutonCookie = wait.until(ExpectedConditions.elementToBeClickable(By.id("L2AGLb")));
                boutonCookie.click();
                Thread.sleep(1000);
            } catch (Exception e) {
                // Pas de bouton cookie, on continue
            }

            // 2. Attente de n'importe quel titre h3 (plus fiable que By.id("search"))
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h3")));

            // 3. Récupération des liens
            List<WebElement> results = driver.findElements(By.cssSelector("#search a h3"));

            if (!results.isEmpty()) {
                WebElement linkElement = results.get(0).findElement(By.xpath("./.."));
                resultat = linkElement.getAttribute("href");
            } else {
                System.out.println("⚠️ Google n'a renvoyé aucun résultat pour : " + query + " (Vérifie si un Captcha est apparu)");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de chargement pour : " + query);
        } finally {
            driver.quit();
        }

        return resultat;
    }
}

 */

package main.divers_services.encodage_automatique_services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class GoogleSearchSimple {

    // ═══════════════════════════════════════════════════════
    // CONFIG
    // ═══════════════════════════════════════════════════════

    // true  = Chrome invisible (production)
    // false = Chrome visible   (debug)
    private static final boolean HEADLESS = true;

    // Nb de tentatives max par requête avant abandon
    private static final int MAX_RETRIES = 3;

    // Pauses min/max entre tentatives (ms)
    private static final int PAUSE_MIN = 3000;
    private static final int PAUSE_MAX = 7000;

    // ═══════════════════════════════════════════════════════
    // USER AGENTS — rotation large
    // ═══════════════════════════════════════════════════════
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:123.0) Gecko/20100101 Firefox/123.0"
    );

    // Résolutions d'écran simulées
    private static final List<String> WINDOW_SIZES = Arrays.asList(
            "--window-size=1920,1080",
            "--window-size=1440,900",
            "--window-size=1366,768",
            "--window-size=1536,864",
            "--window-size=1280,800"
    );

    private static final Random random = new Random();

    // ═══════════════════════════════════════════════════════
    // POINT D'ENTRÉE PRINCIPAL
    // ═══════════════════════════════════════════════════════

    /**
     * Retourne le premier lien pertinent pour une requête.
     * Stratégie : DuckDuckGo d'abord, Google en fallback.
     */
    public static String getPremierLien(String query, String userAgent) {

        // 1. Essai DuckDuckGo (très permissif, pas de captcha)
        String lien = rechercherDuckDuckGo(query, userAgent, MAX_RETRIES);

        // 2. Fallback Google si DDG échoue
        if (lien == null || lien.equals("Aucun lien trouvé")) {
            System.out.println("⚠️ DDG sans résultat → fallback Google pour : " + query);
            lien = rechercherGoogle(query, userAgent, MAX_RETRIES);
        }

        return lien != null ? lien : "Aucun lien trouvé";
    }

    // ═══════════════════════════════════════════════════════
    // DUCKDUCKGO
    // ═══════════════════════════════════════════════════════

    private static String rechercherDuckDuckGo(String query, String userAgent, int retriesRestants) {
        if (retriesRestants <= 0) return "Aucun lien trouvé";

        WebDriver driver = null;
        try {
            driver = creerDriver(userAgent);
            String url = "https://duckduckgo.com/?q=" + query.replace(" ", "+") + "&kl=fr-fr";
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

            // Attendre les résultats DDG
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[data-testid='result-title-a'], .result__a, a[href*='http']")
            ));

            pause(1000, 2000);

            // Sélecteurs DDG (plusieurs tentatives car DDG change parfois son HTML)
            List<String> selecteurs = Arrays.asList(
                    "[data-testid='result-title-a']",
                    ".result__a",
                    "article a[href^='http']",
                    "h2 a[href^='http']"
            );

            for (String sel : selecteurs) {
                List<WebElement> elements = driver.findElements(By.cssSelector(sel));
                for (WebElement el : elements) {
                    String href = el.getAttribute("href");
                    if (href != null && href.startsWith("http") && !href.contains("duckduckgo.com")) {
                        System.out.println("✅ DDG trouvé : " + href);
                        return href;
                    }
                }
            }

            System.out.println("⚠️ DDG : aucun lien valide pour → " + query);
            return "Aucun lien trouvé";

        } catch (Exception e) {
            System.err.println("❌ DDG erreur (tentative " + (MAX_RETRIES - retriesRestants + 1) + ") : " + e.getMessage());
            pause(PAUSE_MIN, PAUSE_MAX);
            // Réessai avec un autre user agent
            return rechercherDuckDuckGo(query, getRandomUserAgent(), retriesRestants - 1);
        } finally {
            if (driver != null) try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════
    // GOOGLE (fallback)
    // ═══════════════════════════════════════════════════════

    private static String rechercherGoogle(String query, String userAgent, int retriesRestants) {
        if (retriesRestants <= 0) return "Aucun lien trouvé";

        WebDriver driver = null;
        try {
            driver = creerDriver(userAgent);
            String url = "https://www.google.com/search?q=" + query.replace(" ", "+") + "&hl=fr";
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

            // Gestion cookie Google
            try {
                WebElement cookie = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button#L2AGLb, button#W0wltc, [aria-label='Tout accepter']")
                ));
                cookie.click();
                pause(800, 1500);
            } catch (Exception ignored) {}

            // Détection captcha
            if (estCaptcha(driver)) {
                System.err.println("🔒 Captcha Google détecté → changement de profil");
                pause(PAUSE_MIN, PAUSE_MAX);
                return rechercherGoogle(query, getRandomUserAgent(), retriesRestants - 1);
            }

            // Attente résultats
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h3")));

            // Récupération lien
            List<WebElement> results = driver.findElements(By.cssSelector("#search a h3"));
            if (!results.isEmpty()) {
                WebElement parent = results.get(0).findElement(By.xpath("./.."));
                String href = parent.getAttribute("href");
                if (href != null && href.startsWith("http")) {
                    System.out.println("✅ Google trouvé : " + href);
                    return href;
                }
            }

            // Sélecteur alternatif Google
            List<WebElement> alt = driver.findElements(By.cssSelector("div.g a[href^='http']"));
            for (WebElement el : alt) {
                String href = el.getAttribute("href");
                if (href != null && !href.contains("google.com")) {
                    System.out.println("✅ Google (alt) trouvé : " + href);
                    return href;
                }
            }

            return "Aucun lien trouvé";

        } catch (Exception e) {
            System.err.println("❌ Google erreur (tentative " + (MAX_RETRIES - retriesRestants + 1) + ") : " + e.getMessage());
            pause(PAUSE_MIN, PAUSE_MAX);
            return rechercherGoogle(query, getRandomUserAgent(), retriesRestants - 1);
        } finally {
            if (driver != null) try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════

    /**
     * Crée un ChromeDriver furtif avec le user agent donné.
     */
    private static WebDriver creerDriver(String userAgent) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // User agent + résolution aléatoire
        options.addArguments("--user-agent=" + userAgent);
        options.addArguments(WINDOW_SIZES.get(random.nextInt(WINDOW_SIZES.size())));

        // Anti-détection
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-infobars");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=fr-FR");
        options.addArguments("--disable-extensions");

        // Masquer le flag "navigator.webdriver"
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        if (HEADLESS) {
            options.addArguments("--headless=new"); // nouveau mode headless Chrome 112+
        }

        ChromeDriver driver = new ChromeDriver(options);

        // Script JS pour masquer webdriver
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );

        return driver;
    }

    /**
     * Détecte si Google a affiché un Captcha.
     */
    private static boolean estCaptcha(WebDriver driver) {
        String source = driver.getPageSource().toLowerCase();
        return source.contains("captcha")
                || source.contains("unusual traffic")
                || source.contains("not a robot")
                || source.contains("recaptcha");
    }

    /**
     * Pause aléatoire entre min et max ms.
     */
    private static void pause(int min, int max) {
        try {
            Thread.sleep(min + random.nextInt(max - min));
        } catch (InterruptedException ignored) {}
    }

    public static String getRandomUserAgent() {
        return USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
    }

    // ═══════════════════════════════════════════════════════
    // MAIN DE TEST
    // ═══════════════════════════════════════════════════════

    public static void main(String[] args) {
        String[] queries = {
                "rick ross spotify",
                "rick ross instagram",
                "rick ross tiktok"
        };

        String identity = getRandomUserAgent();
        System.out.println("User Agent utilisé : " + identity);

        for (String q : queries) {
            System.out.println("\n🔍 Recherche : " + q);
            String lien = getPremierLien(q, identity);
            System.out.println("🔗 Résultat  : " + lien);
        }
    }
}