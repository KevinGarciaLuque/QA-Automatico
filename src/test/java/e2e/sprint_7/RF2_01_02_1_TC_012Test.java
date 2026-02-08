package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
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
 * TC-012: Validar que el campo "Medio de Transporte" se muestre como solo lectura en el paso correspondiente.
 *
 * Flujo:
 * Login -> Solicitudes -> Estado=Iniciada -> Buscar -> Editar (primera fila)
 * -> Validar input#medioTransporte readonly/disabled y que no permita edición manual.
 */
public class RF2_01_02_1_TC_012Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Estado Iniciada: intentamos por value si existe; si no, por texto
    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciad"; // iniciada / iniciado

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

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String optionsToString(Select sel) {
        return sel.getOptions().stream()
            .map(o -> "[" + safe(o.getAttribute("value")) + "] " + safe(o.getText()))
            .collect(Collectors.joining(" | "));
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

    private void seleccionarEstadoIniciada(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement estadoEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));

        // Esperar a que cargue el combo (>1 opción)
        wait.until(d -> {
            try {
                Select s = new Select(d.findElement(By.id("estadoId")));
                return s.getOptions() != null && s.getOptions().size() > 1;
            } catch (Exception e) { return false; }
        });

        Select sel = new Select(estadoEl);
        boolean selected = false;

        // 1) Por value
        try {
            sel.selectByValue(ESTADO_INICIADA_VALUE);
            selected = true;
        } catch (Exception ignore) {}

        // 2) Fallback por texto
        if (!selected) {
            for (WebElement opt : sel.getOptions()) {
                String txt = safe(opt.getText()).toLowerCase();
                if (txt.contains(ESTADO_INICIADA_TEXT_TOKEN)) {
                    opt.click();
                    selected = true;
                    break;
                }
            }
        }

        if (!selected) {
            throw new NoSuchElementException(
                "No se pudo seleccionar Estado 'Iniciada/Iniciado'. Opciones: " + optionsToString(sel)
            );
        }

        Thread.sleep(250);
    }

    private void clickBuscar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By buscarBy = By.xpath("//button[@type='button' and contains(@class,'btn-info') and contains(normalize-space(.),'Buscar')]");
        WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(buscarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        jsClick(driver, btnBuscar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 3);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));
    }

    private int contarFilasTabla(WebDriver driver) {
        // Si hay mensaje de "No hay datos"
        if (!driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody//td[contains(.,'No hay datos')]")).isEmpty()) {
            return 0;
        }
        return driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr")).size();
    }

    private void clickEditarPrimeraFila(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Precondición: hay al menos 1 fila
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));
        int filas = contarFilasTabla(driver);
        assertTrue(filas > 0, "❌ No hay solicitudes en el listado filtrado por estado 'Iniciada' para poder editar.");

        WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
        ));

        // Botón Editar: <button title="Editar solicitud" ...>Editar</button>
        WebElement btnEditar = row1.findElement(By.xpath(
            ".//button[@type='button' and (contains(@title,'Editar') or contains(normalize-space(.),'Editar'))]"
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnEditar);
        Thread.sleep(150);
        jsClick(driver, btnEditar);

        Thread.sleep(800);
        acceptIfAlertPresent(driver, 5);
    }

    private void validarMedioTransporteSoloLectura(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Esperar el input específico
        WebElement medio = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("medioTransporte")));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", medio);
        Thread.sleep(150);

        String valueBefore = safe(medio.getAttribute("value"));
        String disabledAttr = medio.getAttribute("disabled");   // si existe, no es null
        String readonlyAttr = medio.getAttribute("readonly");   // si existe, no es null

        boolean isEnabled = medio.isEnabled();
        boolean hasDisabled = disabledAttr != null;
        boolean hasReadonly = readonlyAttr != null;

        // Validación principal: debe ser no editable
        // (en tu caso viene disabled y readonly)
        assertTrue(hasDisabled || hasReadonly || !isEnabled,
            "❌ El campo 'Medio de Transporte' parece editable. " +
            "Atributos => disabled=" + disabledAttr + ", readonly=" + readonlyAttr + ", isEnabled=" + isEnabled);

        // Validación extra: intentar escribir y confirmar que NO cambia
        boolean noPermiteEdicion = false;
        try {
            medio.click();
            medio.sendKeys("XYZ");
            Thread.sleep(200);
            String valueAfter = safe(medio.getAttribute("value"));
            noPermiteEdicion = valueAfter.equals(valueBefore);
        } catch (ElementNotInteractableException e) {
            noPermiteEdicion = true; // si no se puede interactuar, cumple
        } catch (Exception e) {
            noPermiteEdicion = true; // cualquier bloqueo real de edición también cumple
        }

        // Si estuviera enabled + no readonly, se podría editar; esto lo detecta
        assertTrue(noPermiteEdicion,
            "❌ Se logró modificar el campo 'Medio de Transporte'. " +
            "Antes='" + valueBefore + "'");

        // Evidencia final
        screenshot(driver, "S7_RF201021_TC012_05_medio_transporte_solo_lectura_ok");
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC012_MedioTransporte_SoloLectura() throws InterruptedException {

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
                Thread.sleep(200);
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
            screenshot(driver, "S7_RF201021_TC012_01_login_ok");

            // ====== IR A LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC012_02_listado");

            // ====== FILTRAR ESTADO INICIADA ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC012_03_estado_iniciada_set");

            // ====== BUSCAR ======
            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC012_04_resultados_busqueda");

            // ====== EDITAR PRIMERA FILA ======
            clickEditarPrimeraFila(driver, wait, js);

            // ====== VALIDAR MEDIO DE TRANSPORTE SOLO LECTURA ======
            validarMedioTransporteSoloLectura(driver, wait, js);

            System.out.println("✅ TC-012 OK: El campo 'Medio de Transporte' se muestra solo lectura (no editable).");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC012_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC012_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
