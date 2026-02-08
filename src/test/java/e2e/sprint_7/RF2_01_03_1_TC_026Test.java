package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;

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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.03.1
 * TC-026: Validar visualización del campo Número de Asignación y botones Buscar/Limpiar
 * en el módulo Recepción de Solicitud, y su interacción (Buscar y Limpiar).
 */
public class RF2_01_03_1_TC_026Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Credenciales TC-026
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    private static final String NUM_ASIGNACION = "GLS-EXP-2026-02-00020";

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

    // ================== TEST ==================
    @Test
    void RF201031_TC026_RecepcionSolicitud_BuscarYLimpiar() throws InterruptedException {

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
            screenshot(driver, "S7_RF201031_TC026_01_login_ok");

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
            screenshot(driver, "S7_RF201031_TC026_02_modulo_recepcion");

            // ====== VALIDAR FORMULARIO "BÚSQUEDA DE SOLICITUD" ======
            WebElement titulo = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h5[normalize-space()='Búsqueda de Solicitud']")
            ));
            assertTrue(titulo.isDisplayed(), "❌ No se visualiza el título 'Búsqueda de Solicitud'.");

            WebElement inputAsignacion = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]")
            ));
            String placeholder = safe(inputAsignacion.getAttribute("placeholder"));
            assertTrue(placeholder.contains("Ej: INS-2024-001"),
                "❌ Placeholder incorrecto. Esperado que contenga 'Ej: INS-2024-001' y fue: '" + placeholder + "'");

            WebElement btnBuscar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]")
            ));
            WebElement btnLimpiar = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and contains(.,'Limpiar')]")
            ));

            assertTrue(btnBuscar.isDisplayed(), "❌ No se visualiza el botón 'Buscar'.");
            assertTrue(btnLimpiar.isDisplayed(), "❌ No se visualiza el botón 'Limpiar'.");

            screenshot(driver, "S7_RF201031_TC026_03_formulario_visible");

            // ====== INGRESAR NÚMERO ASIGNACIÓN ======
            clearAndType(inputAsignacion, NUM_ASIGNACION);
            screenshot(driver, "S7_RF201031_TC026_04_num_asignacion_ingresado");

            // ====== CLICK BUSCAR ======
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(150);
            jsClick(driver, btnBuscar);

            Thread.sleep(700);
            acceptIfAlertPresent(driver, 3);

            // Intento suave: si aparecen resultados/tabla, bien; si no, no fallamos (TC pide solo flujo + botones)
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(8));
                shortWait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(.,'No hay datos') or contains(.,'No hay resultados')]"))
                ));
            } catch (Exception ignore) {}

            screenshot(driver, "S7_RF201031_TC026_05_post_buscar");

            // ====== CLICK LIMPIAR ======
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnLimpiar);
            Thread.sleep(150);
            jsClick(driver, btnLimpiar);

            Thread.sleep(400);
            acceptIfAlertPresent(driver, 3);

            // Validar que el input quedó vacío
            String valueAfterClear = safe(inputAsignacion.getAttribute("value"));
            assertTrue(valueAfterClear.isEmpty(), "❌ El botón 'Limpiar' no vació el campo. Valor actual: '" + valueAfterClear + "'");

            screenshot(driver, "S7_RF201031_TC026_06_post_limpiar");

            System.out.println("✅ TC-026 OK: Campo Número de Asignación y botones Buscar/Limpiar se muestran y funcionan.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC026_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC026_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
