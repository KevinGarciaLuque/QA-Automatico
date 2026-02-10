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
 * TC-032: Validar acción del botón "Recepcionar Solicitud".
 *
 * Criterio Crítico:
 * - Al hacer click en "Recepcionar" se abre modal de registro de fecha y hora.
 *
 * Botón:
 * <button type="button" class="btn btn-outline-success btn-sm"> ... Recepcionar</button>
 */
public class RF2_01_03_1_TC_032Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Credenciales (Recepción)
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    // Número "Enviada" por defecto (puedes sobreescribir con -DnumAsignacion=...)
    private static final String DEFAULT_NUM_ASIGNACION = "PCO-IMP-2026-02-00007";

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

    private String lowerXpathContains(String text) {
        return "contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'" +
                text.toLowerCase() + "')";
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC032_ClickRecepcionar_AbrirModalFechaHora() throws InterruptedException {

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
            screenshot(driver, "S7_RF201031_TC032_01_login_ok");

            // ====== IR A RECEPCIÓN DE SOLICITUD ======
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
            screenshot(driver, "S7_RF201031_TC032_02_modulo_recepcion");

            // ====== FORMULARIO BÚSQUEDA ======
            WebElement titulo = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h5[normalize-space()='Búsqueda de Solicitud']")
            ));
            assertTrue(titulo.isDisplayed(), "❌ No se visualiza 'Búsqueda de Solicitud'.");

            WebElement inputAsignacion = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]")
            ));

            WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]")
            ));

            // ====== BUSCAR POR NÚMERO (ENVIADA) ======
            clearAndType(inputAsignacion, numAsignacion);
            screenshot(driver, "S7_RF201031_TC032_03_num_asignacion_ingresado");

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(150);
            jsClick(driver, btnBuscar);

            Thread.sleep(700);
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC032_04_post_buscar");

            // ====== VALIDAR FILA + CLICK RECEPCIONAR ======
            By tablaBy = By.cssSelector("table.table.table-sm.table-bordered.table-striped.table-hover");
            wait.until(ExpectedConditions.visibilityOfElementLocated(tablaBy));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));

            By filaBy = By.xpath(
                "//table[contains(@class,'table')]//tbody//tr[" +
                    ".//span[contains(@class,'text-success') and contains(normalize-space(.),'" + numAsignacion + "')]" +
                "]"
            );
            WebElement fila = wait.until(ExpectedConditions.visibilityOfElementLocated(filaBy));

            // Validar estado Enviada en fila (para asegurarnos que es candidato a recepción)
            WebElement estadoBadge = fila.findElement(By.xpath(".//span[contains(@class,'badge') and contains(@class,'badge-primary')]"));
            String estadoTxt = safe(estadoBadge.getText()).toLowerCase();
            assertTrue(estadoTxt.contains("enviad"),
                "❌ La solicitud buscada no está en estado Enviada/Enviado. Estado actual: '" + safe(estadoBadge.getText()) + "'");

            WebElement btnRecepcionar = fila.findElement(By.xpath(
                ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(@class,'btn-sm') and contains(.,'Recepcionar')]"
            ));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
            Thread.sleep(150);
            jsClick(driver, btnRecepcionar);

            Thread.sleep(450);
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC032_05_click_recepcionar");

            // ====== VALIDAR MODAL (FECHA Y HORA) ======
            // Modal bootstrap típico: div.modal.show / role=dialog
            By modalBy = By.cssSelector("div.modal.show, div.modal[role='dialog'], .modal-dialog");
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(modalBy));

            // Validación robusta: dentro del modal debe existir referencia a "fecha" y "hora"
            // (o input datetime-local como alternativa).
            boolean tieneFecha = !modal.findElements(By.xpath(".//*[ " + lowerXpathContains("fecha") + " ]")).isEmpty();
            boolean tieneHora  = !modal.findElements(By.xpath(".//*[ " + lowerXpathContains("hora") + " ]")).isEmpty();
            boolean tieneDateTimeLocal = !modal.findElements(By.cssSelector("input[type='datetime-local']")).isEmpty();

            // Debe cumplir: (fecha y hora) OR (datetime-local)
            assertTrue((tieneFecha && tieneHora) || tieneDateTimeLocal,
                "❌ El modal no parece ser de registro de fecha/hora. " +
                "tieneFecha=" + tieneFecha + ", tieneHora=" + tieneHora + ", datetime-local=" + tieneDateTimeLocal);

            screenshot(driver, "S7_RF201031_TC032_06_modal_fecha_hora_visible");

            System.out.println("✅ TC-032 OK: Al presionar 'Recepcionar' se abre modal de registro de fecha y hora.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC032_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC032_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
