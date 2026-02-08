package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.03.1
 * TC-034: Validar obligatoriedad de fecha y hora en el modal de recepción.
 *
 * Criterio Alta:
 * - El sistema impide continuar si faltan datos (fecha/hora).
 *
 * Validación robusta:
 * - Intenta detectar toast: "Fecha y hora son obligatorias" (si aparece).
 * - Verifica que la solicitud NO cambió a Recepcionada: permanece Enviada y sigue habilitada para Recepcionar.
 */
public class RF2_01_03_1_TC_034Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";
    private static final String DEFAULT_NUM_ASIGNACION = "PCO-IMP-2026-01-00024";

    private static final String TOAST_MSG = "Fecha y hora son obligatorias";

    // ================== HELPERS ==================
    private void screenshot(WebDriver driver, String nombreArchivo) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] img = ts.getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream("./target/" + nombreArchivo + ".png")) {
                fos.write(img);
            }
            System.out.println("📸 Screenshot: ./target/" + nombreArchivo + ".png");
        } catch (Exception e) {
            System.out.println("No se pudo guardar screenshot: " + e.getMessage());
        }
    }

    private void jsClick(WebDriver driver, WebElement el) {
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void acceptIfAlertPresent(WebDriver driver, long seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            Alert a = shortWait.until(ExpectedConditions.alertIsPresent());
            a.accept();
        } catch (Exception ignore) {}
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void asegurarDelegadoOFF(WebDriver driver, JavascriptExecutor js) {
        try {
            WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
            if (delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }
        } catch (NoSuchElementException ignore) {
        } catch (Exception ignore) {}
    }

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(150);
    }

    private void setInputValueJS(JavascriptExecutor js, WebElement input, String value) {
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            input, value
        );
    }

    private WebElement waitVisible(WebDriverWait wait, By by) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(by)));
    }

    private boolean isModalOpen(WebDriver driver) {
        // Variantes comunes (React-Bootstrap / Bootstrap)
        if (!driver.findElements(By.cssSelector("div.modal.show")).isEmpty()) return true;
        if (!driver.findElements(By.cssSelector("div[role='dialog'] .modal-content")).isEmpty()) return true;
        if (!driver.findElements(By.cssSelector(".modal-dialog .modal-content")).isEmpty()) return true;

        // body.modal-open (muy confiable cuando está activo)
        try {
            Object res = ((JavascriptExecutor) driver).executeScript(
                "return (document.body && document.body.classList.contains('modal-open')) ? true : false;"
            );
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean bodyContainsText(WebDriver driver, String tokenLower) {
        try {
            Object res = ((JavascriptExecutor) driver).executeScript(
                "const c = document.querySelector('.notification-container');" +
                "const t = (c ? c.innerText : (document.body ? document.body.innerText : ''));" +
                "return (t || '').toLowerCase().includes(arguments[0]);",
                tokenLower
            );
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean esperarToastOpcional(WebDriver driver, int seconds, String token) {
        String tokenLower = token.toLowerCase();
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        try {
            w.until(d -> {
                try {
                    return bodyContainsText(d, tokenLower);
                } catch (StaleElementReferenceException se) {
                    return false;
                }
            });
            return true;
        } catch (Exception ignore) {
            return false; // opcional: no rompemos el test si no aparece
        }
    }

    private WebElement obtenerFilaPorAsignacion(WebDriverWait wait, String numAsignacion) {
        By filaBy = By.xpath(
            "//table[contains(@class,'table')]//tbody//tr[" +
                ".//span[contains(@class,'text-success') and contains(normalize-space(.),'" + numAsignacion + "')]" +
            "]"
        );
        return waitVisible(wait, filaBy);
    }

    private void validarNoRecepcionada(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numAsignacion) throws InterruptedException {
        // Re-ejecuta buscar para refrescar tabla (evita stale y asegura estado actual)
        WebElement inputAsignacion = waitVisible(wait,
            By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
        WebElement btnBuscar = waitVisible(wait,
            By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]"));

        clearAndType(inputAsignacion, numAsignacion);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        Thread.sleep(120);
        jsClick(driver, btnBuscar);

        Thread.sleep(450);
        acceptIfAlertPresent(driver, 2);

        // Fila
        WebElement fila = obtenerFilaPorAsignacion(wait, numAsignacion);

        // Estado (columna 4) badge
        WebElement estadoBadge = fila.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
        String estadoTxt = safe(estadoBadge.getText()).toLowerCase();

        // Debe seguir "Enviada/Enviado" (token enviad)
        assertTrue(estadoTxt.contains("enviad"),
            "❌ La solicitud cambió de estado. Se esperaba seguir 'Enviada'. Actual: '" + safe(estadoBadge.getText()) + "'");

        // Debe seguir existiendo botón Recepcionar
        List<WebElement> btnRecepcionar = fila.findElements(By.xpath(
            ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(.,'Recepcionar')]"
        ));
        assertTrue(!btnRecepcionar.isEmpty() && btnRecepcionar.get(0).isDisplayed(),
            "❌ Ya no está disponible el botón 'Recepcionar' (no debería haberse recepcionado).");
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC034_ObligatoriedadFechaHora_ImpideConfirmar() throws InterruptedException {

        final String numAsignacion = System.getProperty("numAsignacion", DEFAULT_NUM_ASIGNACION);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.get(BASE_URL);

            // ====== LOGIN (SIN DELEGADO) ======
            WebElement identificador = waitVisible(wait,
                By.cssSelector("input[type='text'], input#identificador, input[name='identificador']"));
            identificador.clear();
            identificador.sendKeys(IDENTIFICADOR);

            asegurarDelegadoOFF(driver, js);
            Thread.sleep(150);

            WebElement password = waitVisible(wait,
                By.cssSelector("input[type='password'], input#password, input[name='password']"));
            password.clear();
            password.sendKeys(PASSWORD);

            WebElement botonInicio = waitVisible(wait,
                By.xpath("//button[normalize-space()='Inicio' or @type='submit']"));
            botonInicio.click();

            wait.until(ExpectedConditions.or(
                ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
                ExpectedConditions.not(ExpectedConditions.urlContains("#/login")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link"))
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC034_01_login_ok");

            // ====== IR A RECEPCIÓN ======
            WebElement linkRecepcion = waitVisible(wait, By.cssSelector("a.nav-link[href='" + RECEPCION_HREF + "']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", linkRecepcion);
            Thread.sleep(150);
            jsClick(driver, linkRecepcion);

            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/inspeccion/solicitudes/recepcion"),
                ExpectedConditions.urlContains("#/inspeccion/solicitudes/recepcion")
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC034_02_modulo_recepcion");

            // ====== BUSCAR Y ABRIR MODAL ======
            waitVisible(wait, By.cssSelector("table.table"));

            // Buscar por asignación
            WebElement inputAsignacion = waitVisible(wait,
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
            WebElement btnBuscar = waitVisible(wait,
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]"));

            clearAndType(inputAsignacion, numAsignacion);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(120);
            jsClick(driver, btnBuscar);

            Thread.sleep(450);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC034_03_post_buscar");

            WebElement fila = obtenerFilaPorAsignacion(wait, numAsignacion);

            WebElement btnRecepcionar = fila.findElement(By.xpath(
                ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(.,'Recepcionar')]"
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
            Thread.sleep(120);
            jsClick(driver, btnRecepcionar);

            Thread.sleep(350);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC034_04_modal_abierto");

            // ====== BORRAR FECHA/HORA ======
            // Re-localizar dentro del modal (evita stale)
            WebElement inputFechaModal = waitVisible(wait, By.cssSelector("div.modal.show input[type='date'].form-control, div[role='dialog'] input[type='date'].form-control"));
            WebElement inputHoraModal  = waitVisible(wait, By.cssSelector("div.modal.show input[type='time'].form-control, div[role='dialog'] input[type='time'].form-control"));

            setInputValueJS(js, inputFechaModal, "");
            setInputValueJS(js, inputHoraModal, "");

            assertTrue(safe(inputFechaModal.getAttribute("value")).isEmpty(), "❌ No se pudo borrar la Fecha.");
            assertTrue(safe(inputHoraModal.getAttribute("value")).isEmpty(), "❌ No se pudo borrar la Hora.");
            screenshot(driver, "S7_RF201031_TC034_05_fecha_hora_vacias");

            // ====== CONFIRMAR ======
            WebElement btnConfirmar = waitVisible(wait, By.xpath(
                "//div[contains(@class,'modal') and contains(@class,'show')]//button[normalize-space()='Confirmar'] | " +
                "//div[@role='dialog']//button[normalize-space()='Confirmar']"
            ));

            // Si está deshabilitado: bloqueo OK
            boolean disabledAttr = btnConfirmar.getAttribute("disabled") != null;
            boolean enabled = btnConfirmar.isEnabled();

            if (!disabledAttr && enabled) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnConfirmar);
                Thread.sleep(120);
                jsClick(driver, btnConfirmar);
            }
            screenshot(driver, "S7_RF201031_TC034_06_post_confirmar_click");

            // ====== VALIDAR TOAST (OPCIONAL PERO RECOMENDADO) ======
            boolean toastVisto = esperarToastOpcional(driver, 8, TOAST_MSG);
            if (toastVisto) {
                screenshot(driver, "S7_RF201031_TC034_07_toast_detectado");
            } else {
                screenshot(driver, "S7_RF201031_TC034_07_toast_no_detectado");
            }

            // ====== VALIDACIÓN CRÍTICA REAL: NO DEBE RECEPCIONAR ======
            // Si el modal sigue abierto, lo cerramos con Cancelar para poder validar la tabla sin overlay.
            if (isModalOpen(driver)) {
                List<WebElement> btnCancelar = driver.findElements(By.xpath(
                    "//div[contains(@class,'modal') and contains(@class,'show')]//button[normalize-space()='Cancelar'] | " +
                    "//div[@role='dialog']//button[normalize-space()='Cancelar']"
                ));
                if (!btnCancelar.isEmpty()) {
                    jsClick(driver, btnCancelar.get(0));
                    Thread.sleep(350);
                }
            }

            // Verificar que la solicitud sigue Enviada y sigue con Recepcionar
            validarNoRecepcionada(driver, wait, js, numAsignacion);
            screenshot(driver, "S7_RF201031_TC034_08_estado_permanece_enviada");

            assertTrue(true, "✅ TC-034 OK");

            System.out.println("✅ TC-034 OK: Sin fecha/hora, el sistema bloquea (no recepciona). Toast visto=" + toastVisto);

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC034_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC034_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
