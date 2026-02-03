package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
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
 * TC-107: Validar que el panel "Últimas Notificaciones" se muestre sin cambios funcionales.
 *
 * Tipo: Regresión | Severidad: Media
 *
 * Criterio de aceptación:
 * - El panel se comporta igual que la versión anterior (se muestra correctamente).
 *
 * Validaciones automáticas (regresión visual/estructura):
 * - Existe el panel por su encabezado "Últimas Notificaciones"
 * - Existe el badge "Nuevas" (ej: "10 Nuevas")
 * - Existe la lista de notificaciones (.list-group-item) (>=1) o, si no hay, al menos existe el contenedor list-group
 * - Existe el botón "Marcar todas como leídas"
 * - No hay errores visibles (toast/alert/modal con ERROR)
 */
class RF08_5TC_107Test {

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
        screenshot(d, "S6_RF085_TC107_01_login_ok");
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

        // Esperar que cargue algo del dashboard (cards/containers)
        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("dashboard"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".container-fluid, .animated.fadeIn, .card"))
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF085_TC107_02_dashboard_cargado");

        // Validar que no haya errores visibles
        failSiHayErrorVisible(d, w, "S6_RF085_TC107_02A");
    }

    private WebElement obtenerPanelUltimasNotificaciones(WebDriver d, WebDriverWait w) {
        // Header exacto del panel según HTML:
        // <div class="bg-warning ... card-header"><strong> ... Últimas Notificaciones</strong><span class="badge ...">10 Nuevas</span></div>
        By panelBy = By.xpath(
            "//div[contains(@class,'card')]" +
            "[.//div[contains(@class,'card-header') and contains(@class,'bg-warning')]" +
            "   //strong[contains(translate(normalize-space(.),'ÚLTIMAS NOTIFICACIONES','últimas notificaciones'),'últimas notificaciones')]" +
            "]"
        );

        WebElement panel = w.until(ExpectedConditions.presenceOfElementLocated(panelBy));
        w.until(ExpectedConditions.visibilityOf(panel));
        scrollCenter(d, panel);

        return panel;
    }

    private void validarEstructuraPanelNotificaciones(WebDriver d, WebDriverWait w) {
        log("Validar panel 'Últimas Notificaciones' (regresión)");

        WebElement panel = obtenerPanelUltimasNotificaciones(d, w);
        screenshot(d, "S6_RF085_TC107_03_panel_visible");

        // 1) Encabezado presente
        WebElement header = panel.findElement(By.xpath(
            ".//div[contains(@class,'card-header')]//strong[contains(translate(normalize-space(.),'ÚLTIMAS NOTIFICACIONES','últimas notificaciones'),'últimas notificaciones')]"
        ));
        assertTrue(header.isDisplayed(), "❌ No se muestra el título 'Últimas Notificaciones'.");

        // 2) Badge "Nuevas" presente (ej. "10 Nuevas")
        WebElement badgeNuevas = panel.findElement(By.xpath(
            ".//div[contains(@class,'card-header')]//span[contains(@class,'badge') and " +
            "contains(translate(normalize-space(.),'NUEVAS','nuevas'),'nuevas')]"
        ));
        assertTrue(badgeNuevas.isDisplayed(), "❌ No se muestra el badge de 'Nuevas'.");

        // 3) Contenedor list-group presente
        WebElement listGroup = panel.findElement(By.cssSelector(".card-body .list-group"));
        assertTrue(listGroup.isDisplayed(), "❌ No se muestra el contenedor de notificaciones (list-group).");

        // 4) Items (puede variar cantidad). Validación flexible:
        //    - Si hay items: >= 1
        //    - Si no hay, al menos debe existir el contenedor (ya validado)
        List<WebElement> items = panel.findElements(By.cssSelector(".card-body .list-group .list-group-item"));
        if (items != null && !items.isEmpty()) {
            // Validar que al menos el primero trae texto y el ícono chevron (según HTML)
            WebElement first = items.get(0);
            scrollCenter(d, first);

            String txt = first.getText() == null ? "" : first.getText().trim();
            assertTrue(txt.length() > 0, "❌ El primer item de notificación está vacío.");

            // Chevron derecho (fa-chevron-right) (si existe, perfecto; si no, no hacemos fallar fuerte)
            try {
                WebElement chevron = first.findElement(By.cssSelector("i.fa.fa-chevron-right"));
                assertTrue(chevron.isDisplayed(), "❌ No se muestra el chevron en el item.");
            } catch (Exception ignore) {}

            screenshot(d, "S6_RF085_TC107_04_items_ok");
        } else {
            screenshot(d, "S6_RF085_TC107_04_sin_items");
        }

        // 5) Botón "Marcar todas como leídas"
        WebElement btnMarcarLeidas = panel.findElement(By.xpath(
            ".//button[@type='button' and contains(@class,'btn-link') and " +
            "contains(translate(normalize-space(.),'MARCAR TODAS COMO LEÍDAS','marcar todas como leídas'),'marcar todas como leídas')]"
        ));
        assertTrue(btnMarcarLeidas.isDisplayed(), "❌ No se muestra el botón 'Marcar todas como leídas'.");

        // Extra: que el body tenga scroll (overflow-y auto) como en HTML (validación suave)
        try {
            WebElement body = panel.findElement(By.cssSelector(".card-body"));
            String style = body.getAttribute("style") == null ? "" : body.getAttribute("style").toLowerCase();
            assertTrue(style.contains("overflow-y") || style.contains("overflow"),
                "⚠️ Observación: el card-body no refleja estilo de scroll (overflow).");
        } catch (Exception ignore) {}

        // Asegurar sin errores visibles
        failSiHayErrorVisible(d, w, "S6_RF085_TC107_05A");

        System.out.println("✅ TC-107 OK: Panel 'Últimas Notificaciones' se muestra correctamente.");
    }

    // ===================== TEST =====================
    @Test
    void RF085_TC107_PanelUltimasNotificaciones_SinCambiosFuncionales() {

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

            validarEstructuraPanelNotificaciones(driver, wait);

        } catch (Exception e) {
            screenshot(driver, "S6_RF085_TC107_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
