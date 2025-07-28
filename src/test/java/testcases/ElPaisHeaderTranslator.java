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
import java.util.List;
import java.util.concurrent.CompletableFuture;

// The class now correctly relies on BrowserStackBaseTest for driver setup and teardown.
public class ElPaisHeaderTranslator extends BrowserStackBaseTest {
    private static final String OPINION_URL = "https://elpais.com/opinion/";
    private static final String TRANSLATE_API = "https://rapid-translate-multi-traduction.p.rapidapi.com/t";
    // IMPORTANT: Replace with your actual RapidAPI Key
    private static final String API_KEY = "ac6f3f259amshc7ae7ed9ff7b746p1fe807jsncf307d9b9d7c";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    // Removed the @BeforeClass and @AfterClass methods.
    // The driver is now inherited from BrowserStackBaseTest.

    @Test
    public void translateArticleHeaders() throws Exception {
        // The 'driver' is inherited. Initialize WebDriverWait here.
        WebDriverWait wait = new WebDriverWait(driver, 60);

        driver.get(OPINION_URL);

        // Call the corrected cookie handler method.
        acceptCookiePopup(wait);

        List<WebElement> headers = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("article h2 a")
            )
        );

        System.out.println("\n=== Translated Article Headers ===");
        for (int i = 0; i < Math.min(5, headers.size()); i++) {
            String originalText = headers.get(i).getText();
            System.out.println("\n🇪🇸 Original: " + originalText);

            try {
                String translatedText = translateText(originalText).get(); // .get() waits for the async result
                System.out.println("🇬🇧 English: " + translatedText);
            } catch (Exception e) {
                System.out.println("⚠️ Translation failed: " + e.getMessage());
            }
        }
    }

    // This method uses the robust JavascriptExecutor click strategy.
    private void acceptCookiePopup(WebDriverWait wait) {
        try {
            // Use a robust XPath to find the button by its text.
            By acceptButtonLocator = By.xpath("//button[normalize-space()='Accept' or normalize-space()='Aceptar']");
            WebElement acceptButton = wait.until(ExpectedConditions.presenceOfElementLocated(acceptButtonLocator));

            // Use JavascriptExecutor to perform the click.
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", acceptButton);
            System.out.println("🍪 Cookie popup accepted via JavaScript click.");
        } catch (Exception e) {
            // Popup not found or not needed, which is not an error.
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

                    // Handle both array and object response formats
                    if (jsonElement.isJsonArray()) {
                        JsonArray array = jsonElement.getAsJsonArray();
                        if (array.size() > 0) {
                            return array.get(0).getAsString();
                        }
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
                    return "⚠️ No translation found in response";
                } catch (Exception e) {
                    return "⚠️ Failed to parse response: " + e.getMessage();
                }
            })
            .exceptionally(e -> "⚠️ Connection error: " + e.getMessage());
    }
}
