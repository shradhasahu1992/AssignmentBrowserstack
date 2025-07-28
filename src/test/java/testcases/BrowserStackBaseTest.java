package testcases;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import java.net.URL;
import java.util.HashMap;
import java.net.URI;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

public class BrowserStackBaseTest {

    protected RemoteWebDriver driver;

    // --- Replace with your credentials ---
    private static final String USERNAME = "shradhasahu_a3d5pl"; 
    private static final String ACCESS_KEY = "NxWFRntdVvY8EuLGPpTy";
    //private static final String USERNAME = "shradhasahu_qvg73C";
    //private static final String ACCESS_KEY = "eh1HJGLb3YaDXyv5H3Qz";
    private static final String BS_URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@hub-cloud.browserstack.com/wd/hub";

    @BeforeMethod
    @Parameters({"os", "os_version", "browser", "browser_version"})
    public void setup(String os, String osVersion, String browser, String browserVersion) throws Exception {

        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("browserName", browser);

        HashMap<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("os", os);
        bstackOptions.put("osVersion", osVersion);
        bstackOptions.put("browserVersion", browserVersion);
        bstackOptions.put("buildName", "Final Clean Build");
        bstackOptions.put("sessionName", getClass().getSimpleName());
        bstackOptions.put("debug", "true");
        
        bstackOptions.put("debug", "true");
        bstackOptions.put("networkLogs", "true"); // Captures network traffic on the remote machine
        bstackOptions.put("consoleLogs", "info");

        capabilities.setCapability("bstack:options", bstackOptions);
        
        if (capabilities.getBrowserName().equalsIgnoreCase("chrome")) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        }

        // For Mozilla Firefox
        if (capabilities.getBrowserName().equalsIgnoreCase("firefox")) {
            FirefoxOptions options = new FirefoxOptions();
            options.addPreference("dom.webnotifications.enabled", false);
            capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, options);
        }
        System.out.println("DEBUG CAPS >>> " + capabilities.asMap());

        driver = new RemoteWebDriver(new URI(BS_URL).toURL(), capabilities);
        
        String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        System.out.println("Session URL: https://automate.browserstack.com/sessions/" + sessionId);

    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}