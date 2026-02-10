package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
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
 * Sprint 7 - RF2.01.03.1
 * TC-035: Validar cambio de estado a "Recepcionado" al confirmar la recepción.
 *
 * Criterio Crítico:
 * - El estado de la solicitud cambia correctamente a “Recepcionado/Recepcionada”.
 *
 * Flujo:
 * Login -> Recepción de solicitudes -> buscar por número asignación (Enviada)
 * -> Recepcionar -> modal Registrar Recepción -> ingresar fecha + hora válidas
 * -> Confirmar -> validar en tabla que estado ahora es Recepcionada(o).
 */
public class RF2_01_03_1_TC_035Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Credenciales (Recepción)
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    // Asignación por defecto (cambiar con -DnumAsignacion=...)
    private static final String DEFAULT_NUM_ASIGNACION = "PCO-IMP-2026-02-00009";

    // Valores válidos para el modal
    private static final String FECHA_VALIDA = "2026-02-07";
    private static final String HORA_VALIDA  = "22:35";

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

    private WebElement obtenerFilaPorAsignacion(WebDriverWait wait, String numAsignacion) {
        By filaBy = By.xpath(
            "//table[contains(@class,'table')]//tbody//tr[" +
                ".//span[contains(@class,'text-success') and contains(normalize-space(.),'" + numAsignacion + "')]" +
            "]"
        );
        return waitVisible(wait, filaBy);
    }

    private void buscarPorAsignacion(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numAsignacion) throws InterruptedException {
        WebElement inputAsignacion = waitVisible(wait,
            By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
        WebElement btnBuscar = waitVisible(wait,
            By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]"));

        clearAndType(inputAsignacion, numAsignacion);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        Thread.sleep(120);
        jsClick(driver, btnBuscar);

        Thread.sleep(500);
        acceptIfAlertPresent(driver, 2);

        // Esperar tabla
        waitVisible(wait, By.cssSelector("table.table"));
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC035_ConfirmarRecepcion_CambiaEstadoARecepcionado() throws InterruptedException {

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
            screenshot(driver, "S7_RF201031_TC035_01_login_ok");

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
            screenshot(driver, "S7_RF201031_TC035_02_modulo_recepcion");

            // ====== BUSCAR ======
            buscarPorAsignacion(driver, wait, js, numAsignacion);
            screenshot(driver, "S7_RF201031_TC035_03_resultados_busqueda");

            // ====== FILA + CLICK RECEPCIONAR ======
            WebElement fila = obtenerFilaPorAsignacion(wait, numAsignacion);

            WebElement estadoBadgeAntes = fila.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
            String estadoAntes = safe(estadoBadgeAntes.getText()).toLowerCase();
            assertTrue(estadoAntes.contains("enviad"),
                "❌ La solicitud no está en estado Enviada/Enviado para recepcionar. Estado actual: '" + safe(estadoBadgeAntes.getText()) + "'");
            screenshot(driver, "S7_RF201031_TC035_04_estado_enviada_ok");

            WebElement btnRecepcionar = fila.findElement(By.xpath(
                ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(.,'Recepcionar')]"
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
            Thread.sleep(120);
            jsClick(driver, btnRecepcionar);

            Thread.sleep(350);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC035_05_modal_abierto");

            // ====== MODAL: INGRESAR FECHA/HORA + CONFIRMAR ======
            WebElement inputFechaModal = waitVisible(wait,
                By.cssSelector("div.modal.show input[type='date'].form-control, div[role='dialog'] input[type='date'].form-control"));
            WebElement inputHoraModal = waitVisible(wait,
                By.cssSelector("div.modal.show input[type='time'].form-control, div[role='dialog'] input[type='time'].form-control"));

            setInputValueJS(js, inputFechaModal, FECHA_VALIDA);
            setInputValueJS(js, inputHoraModal,  HORA_VALIDA);

            assertTrue(FECHA_VALIDA.equals(safe(inputFechaModal.getAttribute("value"))),
                "❌ No se pudo setear Fecha. Esperado=" + FECHA_VALIDA + " Actual=" + safe(inputFechaModal.getAttribute("value")));
            assertTrue(HORA_VALIDA.equals(safe(inputHoraModal.getAttribute("value"))),
                "❌ No se pudo setear Hora. Esperado=" + HORA_VALIDA + " Actual=" + safe(inputHoraModal.getAttribute("value")));

            screenshot(driver, "S7_RF201031_TC035_06_fecha_hora_set");

            WebElement btnConfirmar = waitVisible(wait, By.xpath(
                "//div[contains(@class,'modal') and contains(@class,'show')]//button[normalize-space()='Confirmar'] | " +
                "//div[@role='dialog']//button[normalize-space()='Confirmar']"
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnConfirmar);
            Thread.sleep(120);
            jsClick(driver, btnConfirmar);

            // Puede cerrar modal y refrescar tabla (evitar stale)
            Thread.sleep(700);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC035_07_post_confirmar");

            // ====== RE-BUSCAR Y VALIDAR ESTADO RECEPCIONADA ======
            // Re-ejecutamos buscar para ver el estado actualizado
            buscarPorAsignacion(driver, wait, js, numAsignacion);

            WebElement filaDespues = obtenerFilaPorAsignacion(wait, numAsignacion);
            WebElement estadoBadgeDespues = filaDespues.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
            String estadoDespues = safe(estadoBadgeDespues.getText()).toLowerCase();

            // En tu UI se ve "Recepcionada"
            assertTrue(estadoDespues.contains("recepcionad"),
                "❌ El estado NO cambió a Recepcionado/Recepcionada. Actual: '" + safe(estadoBadgeDespues.getText()) + "'");

            screenshot(driver, "S7_RF201031_TC035_08_estado_recepcionado_ok");

            System.out.println("✅ TC-035 OK: Estado cambió a 'Recepcionado/Recepcionada' para " + numAsignacion);

        } catch (StaleElementReferenceException se) {
            screenshot(driver, "S7_RF201031_TC035_STALE");
            throw se;

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC035_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC035_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
