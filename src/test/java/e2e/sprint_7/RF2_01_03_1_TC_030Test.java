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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.03.1
 * TC-030: Validar que solo solicitudes en estado Enviado/Enviada sean candidatas a Recepción.
 *
 * Criterio Crítico:
 * - El botón "Recepcionar" solo está disponible para filas con estado Enviada/Enviado.
 *   <button type="button" class="btn btn-outline-success btn-sm">... Recepcionar</button>
 */
public class RF2_01_03_1_TC_030Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Credenciales Recepción
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
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
        try {
            WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
            if (delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }
        } catch (NoSuchElementException ignore) {
        } catch (Exception ignore) {}
    }

    private void clearInput(WebElement el) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(120);
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC030_SoloEnviadaTieneBotonRecepcionar() throws InterruptedException {

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
            screenshot(driver, "S7_RF201031_TC030_01_login_ok");

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
            screenshot(driver, "S7_RF201031_TC030_02_modulo_recepcion");

            // ====== FORMULARIO + EJECUTAR BÚSQUEDA (para poblar tabla) ======
            WebElement titulo = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h5[normalize-space()='Búsqueda de Solicitud']")
            ));
            assertTrue(titulo.isDisplayed(), "❌ No se visualiza 'Búsqueda de Solicitud'.");

            WebElement inputAsignacion = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]")
            ));
            clearInput(inputAsignacion);

            WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]")
            ));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(150);
            jsClick(driver, btnBuscar);

            Thread.sleep(700);
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC030_03_post_buscar");

            // ====== ESPERAR TABLA ======
            By tablaBy = By.cssSelector("table.table.table-sm.table-bordered.table-striped.table-hover");
            wait.until(ExpectedConditions.visibilityOfElementLocated(tablaBy));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));

            List<WebElement> filas = driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));

            // Si el sistema puede devolver "No hay datos", lo tratamos como fallo para este TC crítico.
            boolean hayNoDatos = !driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody//td[contains(.,'No hay datos')]")).isEmpty();
            assertTrue(!hayNoDatos, "❌ La tabla muestra 'No hay datos'. No se puede validar TC-030.");
            assertTrue(filas.size() > 0, "❌ No hay filas en la tabla para validar TC-030.");

            // ====== VALIDACIÓN CRÍTICA: "Recepcionar" solo si estado es Enviada/Enviado ======
            int enviadas = 0;
            int noEnviadas = 0;

            for (int i = 0; i < filas.size(); i++) {
                WebElement row = filas.get(i);

                // Estado: <span class="badge badge-primary">Enviada</span>
                WebElement estadoBadge = row.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
                String estadoTxt = safe(estadoBadge.getText()).toLowerCase();

                // Botón Recepcionar (si existe)
                List<WebElement> btnsRecepcionar = row.findElements(By.xpath(
                    ".//button[@type='button' and contains(@class,'btn-outline-success') and contains(@class,'btn-sm') and contains(.,'Recepcionar')]"
                ));

                boolean tieneRecepcionar = !btnsRecepcionar.isEmpty() && btnsRecepcionar.get(0).isDisplayed();

                if (estadoTxt.contains("enviad")) {
                    enviadas++;
                    assertTrue(tieneRecepcionar,
                        "❌ Fila #" + (i + 1) + " está en estado '" + safe(estadoBadge.getText()) + "' pero NO muestra 'Recepcionar'.");
                } else {
                    noEnviadas++;
                    assertTrue(!tieneRecepcionar,
                        "❌ Fila #" + (i + 1) + " está en estado '" + safe(estadoBadge.getText()) + "' y NO debería mostrar 'Recepcionar'.");
                }
            }

            screenshot(driver, "S7_RF201031_TC030_04_validacion_por_estado_ok");
            System.out.println("✅ TC-030 OK: 'Recepcionar' solo aparece en estado Enviada/Enviado. (enviadas=" + enviadas + ", otras=" + noEnviadas + ")");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC030_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC030_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
