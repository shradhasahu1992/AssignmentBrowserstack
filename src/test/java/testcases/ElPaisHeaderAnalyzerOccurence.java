package testcases;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// The class now correctly relies on BrowserStackBaseTest for driver setup and teardown.
public class ElPaisHeaderAnalyzerOccurence extends BrowserStackBaseTest {
    private static final String OPINION_URL = "https://elpais.com/opinion/";
    private static final String TRANSLATE_API = "https://rapid-translate-multi-traduction.p.rapidapi.com/t";
    // IMPORTANT: Replace with your actual RapidAPI Key
    private static final String API_KEY = "ac6f3f259amshc7ae7ed9ff7b746p1fe807jsncf307d9b9d7c";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    private final List<String> allTranslatedWords = new ArrayList<>();

    // Removed the @BeforeClass and @AfterClass methods and local driver setup.

    @Test
    public void analyzeTranslatedHeaders() throws Exception {
        // The 'driver' is inherited. Initialize WebDriverWait here.
        WebDriverWait wait = new WebDriverWait(driver, 10);

        driver.get(OPINION_URL);
        
        // Call the corrected cookie handler method.
        acceptCookiePopup(wait);

        List<WebElement> headers = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("article h2 a")
            )
        );

        System.out.println("\n=== Translating and Analyzing Headers ===");
        List<CompletableFuture<String>> translations = new ArrayList<>();

        for (int i = 0; i < Math.min(5, headers.size()); i++) {
            String originalText = headers.get(i).getText();
            System.out.println("\n🇪🇸 Original: " + originalText);

            CompletableFuture<String> translation = translateText(originalText)
                .thenApply(translated -> {
                    System.out.println("🇬🇧 English: " + translated);
                    return translated;
                });
            translations.add(translation);
        }

        // Wait for all translations to complete before analyzing
        CompletableFuture.allOf(translations.toArray(new CompletableFuture[0])).join();

        // Analyze word frequencies from all collected words
        analyzeWordFrequency();
    }

    // This method uses the robust JavascriptExecutor click strategy.
    private void acceptCookiePopup(WebDriverWait wait) {
        try {
            By acceptButtonLocator = By.xpath("//button[normalize-space()='Accept' or normalize-space()='Aceptar']");
            WebElement acceptButton = wait.until(ExpectedConditions.presenceOfElementLocated(acceptButtonLocator));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", acceptButton);
            System.out.println("🍪 Cookie popup accepted via JavaScript click.");
        } catch (Exception e) {
            System.out.println("Cookie popup not found or could not be clicked. Proceeding...");
        }
    }

    private CompletableFuture<String> translateText(String text) {
        String requestBody = String.format(
            "{\"from\":\"es\",\"to\":\"en\",\"q\":\"%s\"}",
            text.replace("\"", "\\\"")
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TRANSLATE_API))
            .header("Content-Type", "application/json")
            .header("X-RapidAPI-Key", API_KEY)
            .header("X-RapidAPI-Host", "rapid-translate-multi-traduction.p.rapidapi.com")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    JsonElement jsonElement = gson.fromJson(response.body(), JsonElement.class);
                    String translatedText = parseTranslation(jsonElement);

                    // Store words for analysis (convert to lowercase and remove punctuation)
                    // The 'synchronized' block ensures thread-safe addition to the list.
                    synchronized (allTranslatedWords) {
                        String[] words = translatedText.toLowerCase()
                            .replaceAll("[^a-zA-Z ]", "")
                            .split("\\s+");
                        allTranslatedWords.addAll(Arrays.asList(words));
                    }

                    return translatedText;
                } catch (Exception e) {
                    return "⚠️ Translation error: " + e.getMessage();
                }
            });
    }

    private String parseTranslation(JsonElement jsonElement) {
        if (jsonElement.isJsonArray()) {
            JsonArray array = jsonElement.getAsJsonArray();
            if (array.size() > 0) return array.get(0).getAsString();
        } else if (jsonElement.isJsonObject()) {
            JsonObject obj = jsonElement.getAsJsonObject();
            if (obj.has("translatedText")) {
                JsonElement translated = obj.get("translatedText");
                if (translated.isJsonArray() && translated.getAsJsonArray().size() > 0) {
                    return translated.getAsJsonArray().get(0).getAsString();
                } else if (translated.isJsonPrimitive()) {
                    return translated.getAsString();
                }
            }
        }
        return "No translation available";
    }

    private void analyzeWordFrequency() {
        System.out.println("\n=== Word Frequency Analysis ===");

        Map<String, Integer> wordCounts = new HashMap<>();
        for (String word : allTranslatedWords) {
            if (!word.trim().isEmpty()) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        boolean foundRepeats = false;
        // Create a sorted list of map entries for consistent output
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(wordCounts.entrySet());
        sortedList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Map.Entry<String, Integer> entry : sortedList) {
            if (entry.getValue() > 1) { // Report any word that appears more than once
                System.out.println("• '" + entry.getKey() + "' appears " + entry.getValue() + " times");
                foundRepeats = true;
            }
        }

        if (!foundRepeats) {
            System.out.println("No words were repeated across all translated headers.");
        }
    }
}
