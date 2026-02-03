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
 * TC-117: Validar acceso a "Ver trazabilidad" desde el Dashboard.
 *
 * Tipo: Funcional | Severidad: Alta
 *
 * Criterio de aceptación:
 * - Se abre correctamente el modal/pantalla de trazabilidad.
 *
 * ✅ Como indicaste: en el Dashboard NO existe opción "Ver trazabilidad",
 * por lo tanto este TC debe FALLAR si no se encuentra ningún acceso.
 *
 * Estrategia:
 * - Login
 * - Ir a Dashboard
 * - Buscar algún acceso a trazabilidad por texto/href/icono (Ver trazabilidad / Trazabilidad / Bitácora / Historial)
 * - Si NO existe -> FAIL (AssertionError)
 * - Si existe -> click y validar que abra modal o pantalla con tabla/listado de eventos
 */
class RF08_5TC_117Test {

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
        ((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block:'center'});", e);
    }

    private void jsClick(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].click();", e);
    }

    private void waitDocumentReady(WebDriver d) {
        try {
            new WebDriverWait(d, Duration.ofSeconds(15)).until(x -> {
                try {
                    return "complete".equals(((JavascriptExecutor) x).executeScript("return document.readyState"));
                } catch (Exception e) { return true; }
            });
        } catch (Exception ignore) {}
    }

    private void failSiHayErrorVisible(WebDriver d, WebDriverWait w, String shotName) {
        try { Thread.sleep(600); } catch (InterruptedException ignore) {}

        By errorVisible = By.xpath(
            "//*[contains(@class,'toast') or contains(@class,'Toastify') or contains(@class,'alert') or contains(@class,'modal')]" +
            "[contains(translate(normalize-space(.),'ERRORNOTIFICACIÓN','errornotificación'),'error')" +
            " or contains(translate(normalize-space(.),'NO SE PUDO','no se pudo'),'no se pudo')" +
            " or contains(translate(normalize-space(.),'EXCEPCIÓN','excepción'),'excepción')" +
            "]"
        );

        try {
            WebElement err = new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.visibilityOfElementLocated(errorVisible));

            screenshot(d, shotName + "_ERROR_VISIBLE");
            throw new AssertionError("❌ Se mostró error visible en UI: " + (err.getText() == null ? "" : err.getText().trim()));
        } catch (TimeoutException ignore) {
            // OK
        }
    }

    // ===================== PASOS =====================

    private void login(WebDriver d, WebDriverWait w) {
        log("Abrir login");
        d.get(BASE_URL);
        waitDocumentReady(d);

        log("Ingresar credenciales");
        WebElement user = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='text'], input[type='email'], input#identificador, input[name='identificador']")
        ));
        user.clear();
        user.sendKeys(USUARIO);

        WebElement pass = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='password'], input#password, input[name='password']")
        ));
        pass.clear();
        pass.sendKeys(PASSWORD);

        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit' or normalize-space()='Inicio' or contains(.,'Inicio')]")
        ));
        btn.click();

        w.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        waitDocumentReady(d);

        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".sidebar, .sidebar-nav")));
        screenshot(d, "S6_RF085_TC117_01_login_ok");
    }

    private void irADashboard(WebDriver d, WebDriverWait w) {
        log("Ir a Dashboard desde menú");

        By linkBy = By.cssSelector("a.nav-link[href='" + DASHBOARD_HREF + "']");

        try {
            WebElement link = w.until(ExpectedConditions.presenceOfElementLocated(linkBy));
            w.until(ExpectedConditions.visibilityOf(link));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }
        } catch (Exception e) {
            log("Fallback URL directa Dashboard");
            d.get(DASHBOARD_URL_ABS);
        }

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("dashboard"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".container-fluid, .animated.fadeIn, .card"))
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF085_TC117_02_dashboard_cargado");

        failSiHayErrorVisible(d, w, "S6_RF085_TC117_02A");
    }

    /**
     * Busca cualquier acceso a trazabilidad dentro del Dashboard:
     * - Texto: "Ver trazabilidad", "Trazabilidad", "Bitácora", "Historial"
     * - href que contenga trazabilidad/bitacora/historial
     * - Botón/link que contenga esas palabras
     */
    private WebElement buscarAccesoTrazabilidadEnDashboard(WebDriver d, WebDriverWait w) {
        By byAcceso = By.xpath(
            "//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'VER TRAZABILIDAD','ver trazabilidad'),'ver trazabilidad')" +
            " or contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora')" +
            " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')" +
            " or contains(@href,'trazabilidad')" +
            " or contains(@href,'bitacora')" +
            " or contains(@href,'historial')" +
            "]"
        );

        // Espera corta: si no existe, devolvemos null
        try {
            return new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.presenceOfElementLocated(byAcceso));
        } catch (TimeoutException ignore) {
            return null;
        }
    }

    private void validarQueAbraTrazabilidad(WebDriver d, WebDriverWait w) {
        log("Buscar acceso a Trazabilidad en Dashboard");

        WebElement acceso = buscarAccesoTrazabilidadEnDashboard(d, w);

        if (acceso == null) {
            screenshot(d, "S6_RF085_TC117_03_FAIL_no_existe_acceso_trazabilidad");
            throw new AssertionError(
                "❌ TC-117 FALLÓ: No existe opción/enlace/botón para 'Ver trazabilidad' (o similar) dentro del Dashboard."
            );
        }

        // Si existiera, hacemos click y validamos apertura
        scrollCenter(d, acceso);
        try { acceso.click(); } catch (Exception e) { jsClick(d, acceso); }

        screenshot(d, "S6_RF085_TC117_04_click_acceso_trazabilidad");

        // Validar que abre modal o pantalla (tabla/listado)
        boolean abrio = false;

        try {
            abrio = new WebDriverWait(d, Duration.ofSeconds(12)).until(ExpectedConditions.or(
                // Modal visible
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal.show, .modal-dialog, .modal-content")),
                // Pantalla con tabla
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]")),
                // Pantalla con textos típicos
                ExpectedConditions.presenceOfElementLocated(By.xpath(
                    "//*[contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
                    " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
                    " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')]"
                ))
            )) != null;
        } catch (Exception ignore) {
            abrio = false;
        }

        screenshot(d, "S6_RF085_TC117_05_validacion_apertura");
        failSiHayErrorVisible(d, w, "S6_RF085_TC117_05A");

        assertTrue(abrio, "❌ TC-117 FALLÓ: Se encontró el acceso pero NO abrió modal/pantalla de trazabilidad.");
    }

    // ===================== TEST =====================
    @Test
    void RF085_TC117_Acceso_VerTrazabilidad_DesdeDashboard() {

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

            // ✅ Esta prueba debe FALLAR si no existe acceso a trazabilidad (según tu caso actual)
            validarQueAbraTrazabilidad(driver, wait);

            System.out.println("✅ TC-117 OK: Se abrió trazabilidad desde Dashboard.");

        } catch (Exception e) {
            screenshot(driver, "S6_RF085_TC117_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
