package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
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
 * TC-040: Validar que una solicitud ya Recepcionada no pueda volver a recepcionarse.
 *
 * Criterio Alta:
 * - El botón Recepcionar no está disponible o se bloquea la acción.
 *
 * Validación esperada:
 * - En la fila de la asignación, la columna ACCIONES está vacía:
 *   <td><div class="text-nowrap"></div></td>
 */
public class RF2_01_03_1_TC_040Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Login (Recepción)
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    // Solicitud ya recepcionada
    private static final String NUM_ASIGNACION = "GLS-EXP-2026-02-00022";

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

    private void acceptIfAlertPresent(WebDriver driver, long seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            Alert a = shortWait.until(ExpectedConditions.alertIsPresent());
            a.accept();
        } catch (Exception ignore) {}
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private WebElement waitVisible(WebDriverWait wait, By by) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(by)));
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
    void RF201031_TC040_NoPermiteRecepcionar_SolicitudYaRecepcionada() throws InterruptedException {

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

            // Asegurar switch delegado OFF si existe
            try {
                WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
                if (delegadoSwitch.isSelected()) {
                    js.executeScript("arguments[0].click();", delegadoSwitch);
                    Thread.sleep(150);
                }
            } catch (Exception ignore) {}

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
            screenshot(driver, "S7_RF201031_TC040_01_login_ok");

            // ====== IR A RECEPCIÓN ======
            WebElement linkRecepcion = waitVisible(wait, By.cssSelector("a.nav-link[href='" + RECEPCION_HREF + "']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", linkRecepcion);
            Thread.sleep(120);
            linkRecepcion.click();

            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/inspeccion/solicitudes/recepcion"),
                ExpectedConditions.urlContains("#/inspeccion/solicitudes/recepcion")
            ));
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC040_02_modulo_recepcion");

            // ====== BUSCAR POR NÚMERO ASIGNACIÓN ======
            WebElement inputAsignacion = waitVisible(wait,
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
            WebElement btnBuscar = waitVisible(wait,
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]"));

            clearAndType(inputAsignacion, NUM_ASIGNACION);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(120);
            btnBuscar.click();

            Thread.sleep(700);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC040_03_post_buscar");

            // ====== UBICAR FILA ======
            waitVisible(wait, By.cssSelector("table.table"));

            By filaBy = By.xpath(
                "//table[contains(@class,'table')]//tbody//tr[" +
                    ".//*[contains(normalize-space(.),'" + NUM_ASIGNACION + "')]" +
                "]"
            );
            WebElement fila = waitVisible(wait, filaBy);

            // ====== VALIDAR ESTADO RECEPCIONADA ======
            WebElement estadoBadge = fila.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
            String estadoTxt = safe(estadoBadge.getText()).toLowerCase();

            assertTrue(estadoTxt.contains("recepcionad"),
                "❌ La solicitud NO está en estado Recepcionada/Recepcionado. Estado actual: '" + safe(estadoBadge.getText()) + "'");

            screenshot(driver, "S7_RF201031_TC040_04_estado_recepcionada_ok");

            // ====== VALIDAR ACCIONES VACÍAS ======
            // La columna ACCIONES es la 5ta
            WebElement accionesTd = fila.findElement(By.xpath("./td[5]"));
            WebElement contenedor = accionesTd.findElement(By.cssSelector("div.text-nowrap"));

            String accionesText = safe(contenedor.getText());
            List<WebElement> btns = contenedor.findElements(By.xpath(".//button"));

            // No debe existir botón Recepcionar, ni ningún botón
            boolean hayBotonRecepcionar = !contenedor.findElements(By.xpath(".//button[contains(.,'Recepcionar')]")).isEmpty();

            assertTrue(btns.isEmpty() && accionesText.isEmpty() && !hayBotonRecepcionar,
                "❌ La columna ACCIONES no está vacía. Texto='" + accionesText + "', botones=" + btns.size());

            screenshot(driver, "S7_RF201031_TC040_05_acciones_vacias_ok");

            System.out.println("✅ TC-040 OK: Solicitud Recepcionada no muestra botón 'Recepcionar' (acciones vacías).");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC040_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC040_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
