package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
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
 * TC-025: Validar acceso a "Recepción de solicitud" y visualizar formulario "Búsqueda de Solicitud".
 *
 * Flujo:
 * Login (SIN delegado) -> Módulo Recepción de solicitud -> Validar formulario de búsqueda
 *
 * Elementos esperados:
 * - Título: "Búsqueda de Solicitud"
 * - Label: "Número de Asignación"
 * - Input placeholder: "Ej: INS-2024-001"
 * - Botones: "Buscar" (btn-success) y "Limpiar" (btn-secondary)
 */
public class RF2_01_03_1_TC_025Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = " jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

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
        // Si existe el switch de delegado y está activo, lo apagamos.
        try {
            WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
            if (delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }
        } catch (NoSuchElementException ignore) {
            // No existe el switch en esta pantalla; no pasa nada.
        } catch (Exception ignore) {}
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC025_ValidarFormularioBusqueda_RecepcionSolicitud() throws InterruptedException {

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

            // Asegurar que NO esté activado delegado (si aparece el switch)
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

            // Esperar salir del login / cargar menú
            wait.until(ExpectedConditions.or(
                ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
                ExpectedConditions.not(ExpectedConditions.urlContains("#/login")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link"))
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC025_01_login_ok");

            // ====== IR A MÓDULO: RECEPCIÓN DE SOLICITUD ======
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
            screenshot(driver, "S7_RF201031_TC025_02_modulo_recepcion");

            // ====== VALIDAR FORMULARIO "BÚSQUEDA DE SOLICITUD" ======
            // Título
            By tituloBy = By.xpath("//h5[normalize-space()='Búsqueda de Solicitud']");
            WebElement titulo = wait.until(ExpectedConditions.visibilityOfElementLocated(tituloBy));
            assertTrue(titulo.isDisplayed(), "❌ No se visualiza el título 'Búsqueda de Solicitud'.");

            // Label + Input + Placeholder
            By inputAsignacionBy = By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]");
            WebElement inputAsignacion = wait.until(ExpectedConditions.visibilityOfElementLocated(inputAsignacionBy));

            String placeholder = safe(inputAsignacion.getAttribute("placeholder"));
            assertTrue(placeholder.contains("Ej: INS-2024-001"),
                "❌ Placeholder incorrecto. Se esperaba que contenga 'Ej: INS-2024-001' y fue: '" + placeholder + "'");

            // Botón Buscar
            By btnBuscarBy = By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]");
            WebElement btnBuscar = wait.until(ExpectedConditions.visibilityOfElementLocated(btnBuscarBy));
            assertTrue(btnBuscar.isDisplayed(), "❌ No se visualiza el botón 'Buscar'.");

            // Botón Limpiar
            By btnLimpiarBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and contains(.,'Limpiar')]");
            WebElement btnLimpiar = wait.until(ExpectedConditions.visibilityOfElementLocated(btnLimpiarBy));
            assertTrue(btnLimpiar.isDisplayed(), "❌ No se visualiza el botón 'Limpiar'.");

            screenshot(driver, "S7_RF201031_TC025_03_formulario_busqueda_visible");

            System.out.println("✅ TC-025 OK: Se visualiza el formulario 'Búsqueda de Solicitud' en Recepción de solicitud.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC025_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC025_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
