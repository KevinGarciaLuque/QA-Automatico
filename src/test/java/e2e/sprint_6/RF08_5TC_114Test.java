package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
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
 * TC-114: Validar que el listado muestre solo solicitudes en estados permitidos (Enviada, Asignada, etc.).
 *
 * Tipo: Funcional | Severidad: Alta
 *
 * ✅ Regla de esta automatización (según tu indicación):
 * - Si NO muestra solicitudes -> FALLA (no es posible validar estados visibles).
 * - Si muestra solicitudes -> validar que cada estado esté dentro del set permitido.
 */
class RF08_5TC_114Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String DASHBOARD_HREF = "#/inspeccion/dashboard";
    private static final String DASHBOARD_URL_ABS = "http://3.228.164.208/#/inspeccion/dashboard";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // ✅ Ajusta aquí los estados permitidos EXACTOS según negocio
    private static final Set<String> ESTADOS_PERMITIDOS = new HashSet<>(Arrays.asList(
        "enviada",
        "asignada",
        "asignado",
        "iniciada",
        "recepcionada",
        "subsanable",
        "pendiente"
    ));

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

    private void clickRobusto(WebDriver d, WebDriverWait w, By by) {
        RuntimeException last = null;

        for (int i = 1; i <= 3; i++) {
            try {
                WebElement el = w.until(ExpectedConditions.presenceOfElementLocated(by));
                w.until(ExpectedConditions.visibilityOf(el));
                scrollCenter(d, el);

                try {
                    w.until(ExpectedConditions.elementToBeClickable(el));
                    el.click();
                } catch (Exception e) {
                    jsClick(d, el);
                }
                return;

            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                last = new RuntimeException("Click interceptado/stale en " + by + " intento " + i, e);
            } catch (TimeoutException te) {
                last = new RuntimeException("Timeout esperando " + by + " intento " + i, te);
            }
        }
        if (last != null) throw last;
        throw new RuntimeException("No se pudo click: " + by);
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

    private String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u");
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
        screenshot(d, "S6_RF085_TC114_01_login_ok");
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
        screenshot(d, "S6_RF085_TC114_02_dashboard_cargado");

        failSiHayErrorVisible(d, w, "S6_RF085_TC114_02A");
    }

    private WebElement obtenerCardListadoSolicitudes(WebDriver d, WebDriverWait w) {
        By cardBy = By.xpath(
            "//div[contains(@class,'card')]" +
            "[.//div[contains(@class,'card-header')]//strong[contains(translate(normalize-space(.),'LISTADO DE SOLICITUDES','listado de solicitudes'),'listado de solicitudes')]]"
        );

        WebElement card = w.until(ExpectedConditions.presenceOfElementLocated(cardBy));
        w.until(ExpectedConditions.visibilityOf(card));
        scrollCenter(d, card);
        return card;
    }

    private void validarEstadosListado_FALLA_SiNoHaySolicitudes(WebDriver d, WebDriverWait w) {
        log("Validar listado: debe tener solicitudes y sus estados deben ser permitidos");

        WebElement card = obtenerCardListadoSolicitudes(d, w);
        screenshot(d, "S6_RF085_TC114_03_listado_card_visible");

        // ✅ Si aparece el mensaje "No hay solicitudes para mostrar" => FALLA
        boolean noHay = false;
        try {
            WebElement msg = card.findElement(By.xpath(
                ".//*[contains(translate(normalize-space(.),'NO HAY SOLICITUDES PARA MOSTRAR','no hay solicitudes para mostrar'),'no hay solicitudes para mostrar')]"
            ));
            noHay = msg.isDisplayed();
        } catch (Exception ignore) {}

        if (noHay) {
            screenshot(d, "S6_RF085_TC114_04_FAIL_no_hay_solicitudes");
            throw new AssertionError("❌ TC-114 FALLÓ: El 'Listado de Solicitudes' está vacío (No hay solicitudes para mostrar). No se puede validar la regla de estados permitidos.");
        }

        // Buscar tabla dentro del card
        List<WebElement> tablas = card.findElements(By.xpath(".//table"));
        if (tablas == null || tablas.isEmpty()) {
            screenshot(d, "S6_RF085_TC114_04_FAIL_sin_tabla");
            throw new AssertionError("❌ TC-114 FALLÓ: El listado no muestra tabla/filas para validar estados.");
        }

        WebElement tabla = tablas.get(0);

        // Buscar índice de columna "Estado"
        int idxEstado = -1;
        List<WebElement> ths = tabla.findElements(By.xpath(".//thead/tr/th"));
        for (int i = 0; i < ths.size(); i++) {
            if (norm(ths.get(i).getText()).equals("estado")) { idxEstado = i + 1; break; }
        }
        if (idxEstado == -1) {
            screenshot(d, "S6_RF085_TC114_05_FAIL_sin_columna_estado");
            throw new NoSuchElementException("No se encontró columna 'Estado' en el listado.");
        }

        List<WebElement> filas = tabla.findElements(By.xpath(".//tbody/tr"));
        if (filas == null || filas.isEmpty()) {
            screenshot(d, "S6_RF085_TC114_05_FAIL_sin_filas");
            throw new AssertionError("❌ TC-114 FALLÓ: Hay tabla pero no hay filas para validar estados.");
        }

        // Validar cada estado contra permitidos
        for (int i = 0; i < filas.size(); i++) {
            WebElement tr = filas.get(i);

            String estadoTxt = "";
            try {
                WebElement tdEstado = tr.findElement(By.xpath(".//td[" + idxEstado + "]"));
                estadoTxt = norm(tdEstado.getText());
            } catch (Exception e) {
                screenshot(d, "S6_RF085_TC114_06_FAIL_no_lee_estado_fila_" + (i + 1));
                throw new AssertionError("No se pudo leer estado en fila " + (i + 1), e);
            }

            boolean permitido = false;
            for (String allowed : ESTADOS_PERMITIDOS) {
                if (estadoTxt.contains(norm(allowed))) { permitido = true; break; }
            }

            if (!permitido) {
                screenshot(d, "S6_RF085_TC114_07_FAIL_estado_no_permitido_fila_" + (i + 1));
                throw new AssertionError("❌ Estado NO permitido en listado. Fila " + (i + 1) + " -> '" + estadoTxt + "'");
            }
        }

        screenshot(d, "S6_RF085_TC114_08_estados_ok");
        failSiHayErrorVisible(d, w, "S6_RF085_TC114_09A");
    }

    // ===================== TEST =====================
    @Test
    void RF085_TC114_Listado_SoloEstadosPermitidos() {

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

            // ✅ Ahora sí: falla si el listado está vacío
            validarEstadosListado_FALLA_SiNoHaySolicitudes(driver, wait);

            System.out.println("✅ TC-114 OK: El listado muestra solicitudes y cumple regla de estados visibles.");

        } catch (Exception e) {
            screenshot(driver, "S6_RF085_TC114_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
