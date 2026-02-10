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
 * TC-033: Validar que el modal permita registrar fecha y hora de recepción.
 *
 * Criterio Crítico:
 * - El usuario puede ingresar fecha y hora válidas (inputs type='date' y type='time').
 *
 * Modal esperado:
 * - Título: "Registrar Recepción"
 * - Inputs: <input type="date" ...> y <input type="time" ...>
 * - Botones: "Cancelar" y "Confirmar"
 */
public class RF2_01_03_1_TC_033Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Credenciales (Recepción)
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    private static final String DEFAULT_NUM_ASIGNACION = "PCO-IMP-2026-02-00006";

    // Valores válidos a ingresar en el modal
    // (evitamos formatos raros: date = YYYY-MM-DD, time = HH:mm)
    private static final String FECHA_VALIDA = "2026-02-07";
    private static final String HORA_VALIDA  = "22:25";

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
        // Setea value + dispara eventos para React/Bootstrap
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            input, value
        );
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC033_RegistrarFechaHoraEnModal() throws InterruptedException {

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
            WebElement identificador = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='text'], input#identificador, input[name='identificador']")
            ));
            identificador.clear();
            identificador.sendKeys(IDENTIFICADOR);

            asegurarDelegadoOFF(driver, js);
            Thread.sleep(200);

            WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='password'], input#password, input[name='password']")
            ));
            password.clear();
            password.sendKeys(PASSWORD);

            WebElement botonInicio = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Inicio' or @type='submit']")
            ));
            botonInicio.click();

            wait.until(ExpectedConditions.or(
                ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
                ExpectedConditions.not(ExpectedConditions.urlContains("#/login")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link"))
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC033_01_login_ok");

            // ====== IR A RECEPCIÓN ======
            By recepcionLinkBy = By.cssSelector("a.nav-link[href='" + RECEPCION_HREF + "']");
            WebElement linkRecepcion = wait.until(ExpectedConditions.presenceOfElementLocated(recepcionLinkBy));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", linkRecepcion);
            Thread.sleep(150);
            jsClick(driver, linkRecepcion);

            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/inspeccion/solicitudes/recepcion"),
                ExpectedConditions.urlContains("#/inspeccion/solicitudes/recepcion")
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC033_02_modulo_recepcion");

            // ====== BUSCAR POR NÚMERO (ENVIADA) ======
            WebElement inputAsignacion = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]")
            ));
            WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]")
            ));

            clearAndType(inputAsignacion, numAsignacion);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(150);
            jsClick(driver, btnBuscar);

            Thread.sleep(700);
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC033_03_post_buscar");

            // ====== UBICAR FILA + CLICK RECEPCIONAR ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("table.table.table-sm.table-bordered.table-striped.table-hover")
            ));

            By filaBy = By.xpath(
                "//table[contains(@class,'table')]//tbody//tr[" +
                    ".//span[contains(@class,'text-success') and contains(normalize-space(.),'" + numAsignacion + "')]" +
                "]"
            );
            WebElement fila = wait.until(ExpectedConditions.visibilityOfElementLocated(filaBy));

            WebElement btnRecepcionar = fila.findElement(By.xpath(
                ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(@class,'btn-sm') and contains(.,'Recepcionar')]"
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
            Thread.sleep(150);
            jsClick(driver, btnRecepcionar);

            Thread.sleep(450);
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC033_04_click_recepcionar");

            // ====== VALIDAR MODAL "Registrar Recepción" ======
            WebElement modalContent = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.modal.show div.modal-content")
            ));

            WebElement tituloModal = modalContent.findElement(By.xpath(".//div[contains(@class,'modal-header')]//div[contains(@class,'fw-bold') and normalize-space()='Registrar Recepción']"));
            assertTrue(tituloModal.isDisplayed(), "❌ No se encontró el título 'Registrar Recepción' en el modal.");

            // Validar que el modal muestre "Asignación: <num>"
            WebElement asignacionSmall = modalContent.findElement(By.xpath(".//small[contains(normalize-space(.),'Asignación:')]"));
            String asignacionTxt = safe(asignacionSmall.getText());
            assertTrue(asignacionTxt.contains(numAsignacion),
                "❌ El modal no muestra la asignación esperada. Actual: '" + asignacionTxt + "'");

            // Inputs exactos del modal
            WebElement inputFecha = modalContent.findElement(By.cssSelector("input[type='date'].form-control"));
            WebElement inputHora  = modalContent.findElement(By.cssSelector("input[type='time'].form-control"));

            // ====== INGRESAR FECHA/HORA VÁLIDAS ======
            // Usamos JS para garantizar que React capture el cambio (input+change)
            setInputValueJS(js, inputFecha, FECHA_VALIDA);
            setInputValueJS(js, inputHora,  HORA_VALIDA);

            // Confirmar que el value se seteo
            String fechaActual = safe(inputFecha.getAttribute("value"));
            String horaActual  = safe(inputHora.getAttribute("value"));

            assertTrue(FECHA_VALIDA.equals(fechaActual),
                "❌ No se pudo setear Fecha de Recepción. Esperado=" + FECHA_VALIDA + " Actual=" + fechaActual);

            assertTrue(HORA_VALIDA.equals(horaActual),
                "❌ No se pudo setear Hora de Recepción. Esperado=" + HORA_VALIDA + " Actual=" + horaActual);

            // Botón Confirmar visible y habilitado (sin ejecutar cambio de estado en este TC)
            WebElement btnConfirmar = modalContent.findElement(By.xpath(".//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Confirmar']"));
            assertTrue(btnConfirmar.isDisplayed(), "❌ No se visualiza el botón 'Confirmar' en el modal.");
            assertTrue(btnConfirmar.isEnabled(), "❌ El botón 'Confirmar' está deshabilitado (no debería con fecha/hora válidas).");

            screenshot(driver, "S7_RF201031_TC033_05_fecha_hora_ingresadas_ok");

            // Opcional: cerrar modal con "Cancelar" para no cambiar estado
            WebElement btnCancelar = modalContent.findElement(By.xpath(".//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Cancelar']"));
            jsClick(driver, btnCancelar);
            Thread.sleep(400);

            screenshot(driver, "S7_RF201031_TC033_06_modal_cerrado");

            System.out.println("✅ TC-033 OK: El modal permite ingresar fecha y hora válidas.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC033_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC033_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
