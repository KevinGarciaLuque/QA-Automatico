package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Sprint 6 - RF08.4
 * TC-102: Validar que al AUTORIZAR se registre evento en TRAZABILIDAD.
 *
 * Flujo:
 * - Login (mismo TC-070)
 * - Ir a Autorizaciones
 * - En la tabla: primera fila -> columna Acciones -> click Autorizar (btn-success btn-sm ml-1)
 * - (si existe) confirmar modal
 * - Validar en trazabilidad: evento contiene "autoriz", usuario y fecha (hoy)
 *
 * NOTA: Si tu módulo de trazabilidad tiene href exacto, colócalo en TRAZABILIDAD_HREF/URL.
 */
class RF08_4TC_102Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // ======= AUTORIZACIONES =======
    private static final String AUTORIZACIONES_HREF = "#/inspeccion/solicitudes/autorizaciones/listar";
    private static final String AUTORIZACIONES_URL_ABS = "http://3.228.164.208/#/inspeccion/solicitudes/autorizaciones/listar";

    // ======= TRAZABILIDAD =======
    // Si no existe este endpoint, el código usa fallback por menú (texto Trazabilidad/Bitácora/Actividad).
    private static final String TRAZABILIDAD_URL_ABS = "http://3.228.164.208/#/trazabilidad"; // <-- ajusta si aplica

    // ======= CREDENCIALES (MISMO TC-070) =======
    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // ======= EVENTO (FLEXIBLE) =======
    private static final String EVENTO_TOKEN = "autoriz"; // coincide con autorización/autorizar/autorizado

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

    private void failSiHayToastError(WebDriver d, WebDriverWait w, String shotName) {
        try { Thread.sleep(600); } catch (InterruptedException ignore) {}

        By toastError = By.xpath(
            "//*[contains(@class,'toast') or contains(@class,'Toastify') or contains(@class,'alert') or contains(@class,'notification')]" +
            "[contains(translate(normalize-space(.),'ERRORNOTIFICACIÓN','errornotificación'),'error')" +
            " or contains(translate(normalize-space(.),'ERRORNOTIFICACIÓN','errornotificación'),'notificación')" +
            " or contains(translate(normalize-space(.),'NO ESTAS AUTORIZADO','no estas autorizado'),'no estas autorizado')" +
            " or contains(translate(normalize-space(.),'NO SE PUDO','no se pudo'),'no se pudo')" +
            " or contains(translate(normalize-space(.),'FALLÓ','falló'),'falló')" +
            "]"
        );

        try {
            WebElement err = new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.visibilityOfElementLocated(toastError));

            String msg = "";
            try { msg = err.getText(); } catch (Exception ignore) {}

            screenshot(d, shotName + "_TOAST_ERROR");
            throw new AssertionError("❌ Se mostró error en UI: " + (msg == null ? "(sin texto)" : msg.trim()));

        } catch (TimeoutException ignore) {
            // OK: no salió toast de error
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
        screenshot(d, "S6_RF084_TC102_01_login_ok");
    }

    private void irAAutorizaciones(WebDriver d, WebDriverWait w) {
        log("Ir a Autorizaciones por href");

        By linkBy = By.cssSelector("a.nav-link[href='" + AUTORIZACIONES_HREF + "']");

        try {
            WebElement link = w.until(ExpectedConditions.presenceOfElementLocated(linkBy));
            w.until(ExpectedConditions.visibilityOf(link));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }

            new WebDriverWait(d, Duration.ofSeconds(15)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("autorizaciones"),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            ));

        } catch (Exception e) {
            log("Fallback URL directa de Autorizaciones");
            d.get(AUTORIZACIONES_URL_ABS);
            new WebDriverWait(d, Duration.ofSeconds(20)).until(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            );
        }

        waitDocumentReady(d);
        screenshot(d, "S6_RF084_TC102_02_modulo_autorizaciones");
    }

    /**
     * DOM que nos diste:
     * //table.../tbody/tr/td[last()] -> <div class="text-nowrap">...<button class="ml-1 btn btn-success btn-sm">Autorizar</button>
     */
    private WebElement encontrarBotonAutorizarEnAccionesPrimeraFila(WebDriver d, WebDriverWait w) {

        // Espera que exista al menos una fila
        w.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//table[contains(@class,'table') and contains(@class,'table-sm')]//tbody/tr")
        ));

        // Primera fila
        WebElement fila1 = d.findElement(
            By.xpath("(//table[contains(@class,'table') and contains(@class,'table-sm')]//tbody/tr)[1]")
        );

        String txt = "";
        try { txt = fila1.getText(); } catch (Exception ignore) {}
        if (txt != null && txt.toLowerCase().contains("no hay datos")) {
            throw new NoSuchElementException("La tabla devolvió 'No hay datos'.");
        }

        // Última columna (Acciones) -> botón Autorizar
        return fila1.findElement(
            By.xpath(".//td[last()]//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Autorizar']")
        );
    }

    private void autorizarDesdeColumnaAcciones(WebDriver d, WebDriverWait w) {
        log("Click Autorizar (columna Acciones - primera fila)");

        WebElement btn = encontrarBotonAutorizarEnAccionesPrimeraFila(d, w);
        scrollCenter(d, btn);

        try {
            w.until(ExpectedConditions.elementToBeClickable(btn));
            btn.click();
        } catch (Exception e) {
            jsClick(d, btn);
        }

        screenshot(d, "S6_RF084_TC102_03_click_autorizar_acciones");

        // ✅ Si salen errores, fallar
        failSiHayToastError(d, w, "S6_RF084_TC102_03A");

        // Modal confirmación opcional
        confirmarAutorizarSiHayModal(d, w);

        // ✅ Si luego de confirmar sale error, fallar
        failSiHayToastError(d, w, "S6_RF084_TC102_03B");

        waitDocumentReady(d);
    }

    private void confirmarAutorizarSiHayModal(WebDriver d, WebDriverWait w) {
        log("Confirmación (si hay modal)");

        // Botones típicos de confirmación en modal
        By btnConfirm = By.xpath(
            "//div[contains(@class,'modal') and contains(@class,'show')]//button[@type='button' and (" +
            "normalize-space()='Confirmar' or normalize-space()='Aceptar' or normalize-space()='Sí' or normalize-space()='Si' or normalize-space()='Autorizar'" +
            " or contains(.,'Confirmar') or contains(.,'Aceptar') or contains(.,'Autorizar')" +
            ")]"
        );

        try {
            WebElement confirmar = new WebDriverWait(d, Duration.ofSeconds(8))
                .until(ExpectedConditions.elementToBeClickable(btnConfirm));

            scrollCenter(d, confirmar);
            try { confirmar.click(); } catch (Exception e) { jsClick(d, confirmar); }

            screenshot(d, "S6_RF084_TC102_04_confirmacion_modal");

            // Esperar que el modal desaparezca (si aplica)
            try {
                new WebDriverWait(d, Duration.ofSeconds(10)).until(
                    ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'modal') and contains(@class,'show')]"))
                );
            } catch (Exception ignore) {}

        } catch (TimeoutException ignore) {
            // No había modal
        }
    }

    private void irATrazabilidad(WebDriver d, WebDriverWait w) {
        log("Ir a Trazabilidad (por menú si existe / fallback URL directa)");

        // Intento por menú: texto típico
        By linkBy = By.xpath(
            "//a[contains(@class,'nav-link') and (" +
            "contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'ACTIVIDAD','actividad'),'actividad')" +
            ")]"
        );

        try {
            WebElement link = new WebDriverWait(d, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(linkBy));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }

            new WebDriverWait(d, Duration.ofSeconds(15)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("trazabilidad"),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            ));

        } catch (Exception e) {
            log("Fallback URL directa de Trazabilidad");
            d.get(TRAZABILIDAD_URL_ABS);
            new WebDriverWait(d, Duration.ofSeconds(20)).until(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            );
        }

        waitDocumentReady(d);
        screenshot(d, "S6_RF084_TC102_05_modulo_trazabilidad");
    }

    private boolean existeEventoAutorizacionEnTabla(WebDriver d, String usuario, String fechaIso) {
        try {
            List<WebElement> filas = d.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));
            if (filas == null || filas.isEmpty()) return false;

            for (WebElement tr : filas) {
                if (!tr.isDisplayed()) continue;

                String t = "";
                try { t = tr.getText(); } catch (Exception ignore) {}
                if (t == null) t = "";

                String tl = t.toLowerCase();
                if (tl.contains(EVENTO_TOKEN) &&
                    tl.contains(usuario.toLowerCase()) &&
                    tl.contains(fechaIso)) {
                    return true;
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    private void validarEventoEnTrazabilidad(WebDriver d, WebDriverWait w) {
        log("Validar evento en trazabilidad: usuario + fecha + tipo evento (autorización)");

        String fechaHoy = LocalDate.now().toString();

        boolean ok = false;
        try {
            ok = new WebDriverWait(d, Duration.ofSeconds(15)).until(x ->
                existeEventoAutorizacionEnTabla(x, USUARIO, fechaHoy)
            );
        } catch (Exception ignore) {
            ok = existeEventoAutorizacionEnTabla(d, USUARIO, fechaHoy);
        }

        screenshot(d, "S6_RF084_TC102_06_validacion_evento");

        assertTrue(ok,
            "❌ TC-102 FALLÓ: No se encontró en trazabilidad un evento que contenga '" + EVENTO_TOKEN +
            "' con usuario '" + USUARIO + "' y fecha '" + fechaHoy + "'.");
    }

    // ===================== TEST =====================
    @Test
    void RF084_TC102_Autorizar_DebeRegistrarEventoEnTrazabilidad() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--start-maximized", "--lang=es-419");
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            login(driver, wait);

            irAAutorizaciones(driver, wait);

            // ✅ CLICK EXACTO en columna Acciones
            autorizarDesdeColumnaAcciones(driver, wait);

            irATrazabilidad(driver, wait);

            validarEventoEnTrazabilidad(driver, wait);

            System.out.println("✅ TC-102 OK: Se registró evento de autorización en trazabilidad.");

        } catch (Exception e) {
            screenshot(driver, "S6_RF084_TC102_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
