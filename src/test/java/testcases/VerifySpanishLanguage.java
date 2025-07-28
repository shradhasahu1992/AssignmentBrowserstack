package testcases;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.time.Duration;

public class VerifySpanishLanguage extends BrowserStackBaseTest {

    @Test
    public void verifyPageIsInSpanish() throws InterruptedException {
        driver.get("https://elpais.com/");
        WebDriverWait wait = new WebDriverWait(driver, 10);

        // --- START OF CHANGES ---
        // The locator By.id was failing. This new approach uses a more robust XPath locator
        // to find the button based on its visible text, which is less likely to change.
        try {
            System.out.println("Waiting for the cookie consent button to be present in the DOM...");
            
            // Step 1: Use a robust XPath to find the button by its text.
            // normalize-space() handles any extra whitespace around the text.
            By acceptButtonLocator = By.xpath("//button[normalize-space()='Accept']");
            WebElement acceptButton = wait.until(ExpectedConditions.presenceOfElementLocated(acceptButtonLocator));
            
            System.out.println("Cookie consent button found. Forcing click with JavaScript...");
            
            // Step 2: Use JavascriptExecutor to click. This remains a good strategy
            // to bypass issues where the button is considered obstructed.
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", acceptButton);

            System.out.println("JavaScript click executed.");

        } catch (Exception e) {
            // If the banner doesn't appear, the test won't fail.
            System.out.println("Could not find or click the cookie consent button. Proceeding with test...");
            e.printStackTrace();
        }
        // --- END OF CHANGES ---

        // Proceed with the original test logic
        System.out.println("Verifying page language...");
        // Add a small static wait to ensure the page has time to react to the banner closing.
        Thread.sleep(1000); 

        WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        String text = body.getText().toLowerCase();

        // The assertion remains the same
        Assert.assertTrue(
            text.contains("opinión") || text.contains("economía") || text.contains("sociedad"),
            "Page does not appear to be in Spanish. Could not find expected keywords."
        );
        System.out.println("Successfully verified page is in Spanish.");
    }
}
