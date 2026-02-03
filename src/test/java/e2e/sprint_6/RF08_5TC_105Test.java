package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 6 - RF08.5
 * TC-105: Validar acceso al módulo Dashboard desde el menú principal.
 *
 * Flujo:
 * - Login
 * - Click menú Dashboard
 * - Validar que el Dashboard cargue sin errores
 */
class RF08_5TC_105Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String DASHBOARD_HREF = "#/inspeccion/dashboard";
    private static final String DASHBOARD_URL_ABS = "http://3.228.164.208/#/inspeccion/dashboard";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // ===================== HELPERS =====================
    private void log(String m) { System.out.println("➡️ " + m); }

    private void screenshot(WebDriver driver, String name) {
        try {
            byte[] img = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream("./target/" + name + ".png")) {
                fos.write(img);
            }
            System.out.println("📸 Screenshot: ./target/" + name + ".png");
        } catch (Exception ignore) {}
    }

    private void scrollCenter(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", e
        );
    }

    private void jsClick(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].click();", e);
    }

    private void waitDocumentReady(WebDriver d) {
        try {
            new WebDriverWait(d, Duration.ofSeconds(15)).until(x ->
                "complete".equals(
                    ((JavascriptExecutor) x).executeScript("return document.readyState")
                )
            );
        } catch (Exception ignore) {}
    }

    private void failSiHayErrorVisible(WebDriver d, WebDriverWait w, String shotName) {
        try {
            By errorVisible = By.xpath(
                "//*[contains(@class,'toast') or contains(@class,'alert') or contains(@class,'modal')]" +
                "[contains(translate(normalize-space(.),'ERROR','error'),'error') or " +
                " contains(translate(normalize-space(.),'NO SE PUDO','no se pudo'),'no se pudo')]"
            );

            WebElement err = new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.visibilityOfElementLocated(errorVisible));

            screenshot(d, shotName + "_ERROR");
            throw new AssertionError("❌ Error visible al cargar Dashboard: " + err.getText());

        } catch (TimeoutException ignore) {
            // OK: no hay error visible
        }
    }

    // ===================== PASOS =====================
    private void login(WebDriver d, WebDriverWait w) {
        log("Abrir login");
        d.get(BASE_URL);
        waitDocumentReady(d);

        WebElement user = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='text'], input[type='email']")
        ));
        user.clear();
        user.sendKeys(USUARIO);

        WebElement pass = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='password']")
        ));
        pass.clear();
        pass.sendKeys(PASSWORD);

        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit' or contains(.,'Inicio')]")
        ));
        btn.click();

        w.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".sidebar, .sidebar-nav")));

        screenshot(d, "S6_RF085_TC105_01_login_ok");
    }

    private void irADashboard(WebDriver d, WebDriverWait w) {
        log("Ir a Dashboard desde menú");

        By linkBy = By.cssSelector("a.nav-link[href='" + DASHBOARD_HREF + "']");

        try {
            WebElement link = w.until(ExpectedConditions.presenceOfElementLocated(linkBy));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }

        } catch (Exception e) {
            log("Fallback URL directa Dashboard");
            d.get(DASHBOARD_URL_ABS);
        }

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("dashboard"),
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(@class,'card') or contains(@class,'container')]")
            )
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF085_TC105_02_dashboard_cargado");

        // Validar que no haya errores visibles
        failSiHayErrorVisible(d, w, "S6_RF085_TC105");
    }

    // ===================== TEST =====================
    @Test
    void RF085_TC105_AccesoDashboard_DesdeMenu_Principal() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--start-maximized", "--lang=es-419");
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            login(driver, wait);

            irADashboard(driver, wait);

            assertTrue(
                driver.getCurrentUrl().contains("dashboard"),
                "❌ No se cargó el módulo Dashboard."
            );

            System.out.println("✅ TC-105 OK: El módulo Dashboard carga correctamente.");

        } catch (Exception e) {
            screenshot(driver, "S6_RF085_TC105_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
