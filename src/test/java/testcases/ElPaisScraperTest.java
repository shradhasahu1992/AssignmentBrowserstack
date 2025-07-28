package testcases;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// The class now correctly relies on BrowserStackBaseTest for driver setup and teardown.
public class ElPaisScraperTest extends BrowserStackBaseTest {
    private static final String OPINION_URL = "https://elpais.com/opinion/";

    // Removed the @BeforeClass and @AfterClass methods.
    // The driver is now inherited from BrowserStackBaseTest.

    @Test
    public void scrapeOpinionArticles() throws Exception {
        // The 'driver' is inherited. Initialize WebDriverWait here.
        WebDriverWait wait = new WebDriverWait(driver, 10);

        driver.get(OPINION_URL);
        
        // Call the corrected cookie handler method.
        acceptCookiePopup(wait);

        // Wait for articles to load
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("article h2 a")
        ));

        // Get article links
        List<WebElement> articleLinks = driver.findElements(By.cssSelector("article h2 a"));
        int articleCount = Math.min(5, articleLinks.size());
        
        // Store URLs to avoid stale element issues
        List<String> urlsToVisit = new ArrayList<>();
        for (int i = 0; i < articleCount; i++) {
            urlsToVisit.add(articleLinks.get(i).getAttribute("href"));
        }

        for (String articleUrl : urlsToVisit) {
            try {
                // Open in new tab to avoid navigation issues
                openNewTab(articleUrl);
                acceptCookiePopup(wait); // Handle cookie popups on article pages too

                // Scrape article
                String title = getArticleTitle(wait);
                String content = getArticleContent();
                String imgUrl = getArticleImage(wait);

                // Print and save results
                System.out.println("\n📰 Title: " + title);
                System.out.println("📄 Content (first 500 chars):\n" +
                    (content.length() > 500 ? content.substring(0, 500) + "..." : content));
                saveContentToFile(title, content);

                if (imgUrl != null && !imgUrl.isEmpty()) {
                    downloadImage(title, imgUrl);
                    System.out.println("🖼 Image downloaded");
                }

                // Close tab and return to main page
                closeCurrentTab();
            } catch (Exception e) {
                System.err.println("❌ Error processing article " + articleUrl + ": " + e.getMessage());
                closeCurrentTab(); // Ensure we close the failed tab
            }
        }
    }

    // This method is now corrected to use the JavascriptExecutor click.
    private void acceptCookiePopup(WebDriverWait wait) {
        try {
            // Use a robust XPath to find the button by its text.
            By acceptButtonLocator = By.xpath("//button[normalize-space()='Accept' or normalize-space()='Aceptar']");
            WebElement acceptButton = wait.until(ExpectedConditions.presenceOfElementLocated(acceptButtonLocator));
            
            // Use JavascriptExecutor to perform the click, same as in VerifySpanishLanguage.
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", acceptButton);
            System.out.println("🍪 Cookie popup accepted via JavaScript click.");
        } catch (Exception e) {
            // Popup not found or not needed, which is not an error.
            System.out.println("Cookie popup not found or could not be clicked. Proceeding...");
        }
    }

    // Helper methods now accept 'wait' as a parameter.
    private String getArticleTitle(WebDriverWait wait) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("h1")
            )).getText();
        } catch (Exception e) {
            System.err.println("⚠️ Title not found");
            return "Untitled Article";
        }
    }

    private String getArticleContent() {
        try {
            // Scroll to trigger lazy loading
            ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 500)");

            List<WebElement> contentElements = driver.findElements(
                By.cssSelector("div[data-dtm-region='articulo_cuerpo'] p, div.a_cuerpo p, .a_c p, article p")
            );

            StringBuilder content = new StringBuilder();
            for (WebElement p : contentElements) {
                String text = p.getText().trim();
                if (!text.isEmpty()) {
                    content.append(text).append("\n\n");
                }
            }
            return content.toString().trim();
        } catch (Exception e) {
            System.err.println("⚠️ Content not found");
            return "";
        }
    }

    private String getArticleImage(WebDriverWait wait) {
        try {
            WebElement img = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("figure img, picture img, img[itemprop='contentUrl']")
            ));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", img);
            return img.getAttribute("src");
        } catch (Exception e) {
            System.err.println("⚠️ Image not found");
            return null;
        }
    }

    private void openNewTab(String url) {
        ((JavascriptExecutor) driver).executeScript("window.open(arguments[0])", url);
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabs.size() - 1));
    }

    private void closeCurrentTab() {
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.close();
        // Switch back to the original tab (first one)
        if (tabs.size() > 1) {
            driver.switchTo().window(tabs.get(0));
        }
    }

    private void saveContentToFile(String title, String content) {
        try {
            Path dir = Paths.get("articles");
            Files.createDirectories(dir);
            String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
            Path file = dir.resolve(safeTitle + ".txt");
            Files.write(file, content.getBytes(), StandardOpenOption.CREATE);
        } catch (Exception e) {
            System.err.println("⚠️ Error saving article: " + e.getMessage());
        }
    }

    private void downloadImage(String title, String imageUrl) {
        try {
            Path dir = Paths.get("images");
            Files.createDirectories(dir);
            String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
            Path file = dir.resolve(safeTitle + ".jpg");

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error saving image: " + e.getMessage());
        }
    }
}
