package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
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
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.02.1
 * TC-002: Validar que al generar el Número de Asignación el estado pase a "Enviada".
 *
 * Flujo:
 * Login -> Solicitudes de inspección -> filtrar por:
 *  - Fecha: 2026-02-07
 *  - Estado: Enviada (1794)
 * -> Buscar
 * -> Validar que en la tabla exista el badge: <span class="badge badge-primary">Enviada</span>
 */
public class RF2_01_02_1_TC_002Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Filtros
    private static final String FECHA_HOY = "07/02/2026"; // input type="date"
    private static final String ESTADO_ENVIADA_VALUE = "1794";

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

    private void jsClick(WebDriver driver, WebElement el) {
        try {
            el.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void irARegistroSolicitudes(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        String url = driver.getCurrentUrl();
        if (url != null && (url.contains("#/inspeccion/solicitudes") || url.contains("/inspeccion/solicitudes"))) return;

        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("a.nav-link[href='" + REGISTRO_HREF + "']")));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        jsClick(driver, link);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/inspeccion/solicitudes"),
            ExpectedConditions.urlContains("#/inspeccion/solicitudes")
        ));
    }

    private void setDateInput(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String id, String yyyyMMdd) {
        WebElement date = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));

        // Más estable que sendKeys en algunos navegadores / UIs
        js.executeScript(
            "var el=arguments[0];" +
            "el.value=arguments[1];" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
            "el.dispatchEvent(new Event('change',{bubbles:true}));",
            date, yyyyMMdd
        );
    }

    private void selectByIdAndValue(WebDriverWait wait, String selectId, String value) {
        WebElement selectEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(selectId)));
        new Select(selectEl).selectByValue(value);
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC002_ValidarEstadoEnviadaEnListado() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.get(BASE_URL);

            // ====== LOGIN ======
            WebElement identificador = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='text'], input#identificador, input[name='identificador']")));
            identificador.clear();
            identificador.sendKeys(IDENTIFICADOR);

            WebElement delegadoSwitch = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("esUsuarioDelegado")));
            if (!delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }

            WebElement usuarioDelegado = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Ingrese el usuario delegado/regente' or @type='email']")));
            usuarioDelegado.clear();
            usuarioDelegado.sendKeys(USUARIO);

            WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='password'], input#password, input[name='password']")));
            password.clear();
            password.sendKeys(PASSWORD);

            WebElement botonInicio = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Inicio' or @type='submit']")));
            botonInicio.click();

            wait.until(ExpectedConditions.or(
                ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link[href='" + REGISTRO_HREF + "']"))
            ));
            screenshot(driver, "S7_RF201021_TC002_01_login_ok");

            // ====== IR A SOLICITUDES ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC002_02_listado");

            // ====== FILTROS ======
            // Fecha de solicitud (input id fechaCreacion)
            setDateInput(driver, wait, js, "fechaCreacion", FECHA_HOY);

            // Estado Enviada (select id estadoId, value 1794)
            selectByIdAndValue(wait, "estadoId", ESTADO_ENVIADA_VALUE);

            screenshot(driver, "S7_RF201021_TC002_03_filtros_set");

            // ====== BUSCAR ======
            By buscarBy = By.xpath("//button[@type='button' and contains(@class,'btn-info') and contains(normalize-space(.),'Buscar')]");
            WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(buscarBy));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            jsClick(driver, btnBuscar);

            acceptIfAlertPresent(driver, 3);

            // ====== VALIDAR BADGE "ENVIADA" EN LA TABLA ======
            // El estado se muestra como: <span class="badge badge-primary">Enviada</span>
            By badgeEnviada = By.xpath(
                "//table[contains(@class,'table')]//tbody//span[contains(@class,'badge') and contains(@class,'badge-primary') and normalize-space()='Enviada']"
            );

            WebElement badge = wait.until(ExpectedConditions.visibilityOfElementLocated(badgeEnviada));
            screenshot(driver, "S7_RF201021_TC002_04_resultado_enviada");

            assertTrue(badge.isDisplayed(), "❌ No se encontró en la tabla el estado 'Enviada' (badge badge-primary).");

            System.out.println("✅ TC-002 OK: Se encontró el estado 'Enviada' en el listado al filtrar por fecha y estado.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC002_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC002_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
