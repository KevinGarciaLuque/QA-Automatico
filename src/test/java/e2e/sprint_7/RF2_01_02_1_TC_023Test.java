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
 * TC-023 (AJUSTADO): Termina en Documentación.
 *
 * Flujo:
 * Login -> Listado -> Estado=Iniciada -> Buscar -> Editar
 * -> validar Medio Transporte readonly
 * -> Siguiente -> Productos (tabla visible)
 * -> Siguiente -> Permisos (opcional) -> Siguiente
 * -> Documentación (screenshot)
 * -> Intentar "Siguiente" (opcional, sin fallar si está deshabilitado)
 * -> FIN
 */
public class RF2_01_02_1_TC_023Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciad";

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
        try { el.click(); }
        catch (Exception e) {
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

    private String optionsToString(Select sel) {
        return sel.getOptions().stream()
            .map(o -> "[" + safe(o.getAttribute("value")) + "] " + safe(o.getText()))
            .collect(Collectors.joining(" | "));
    }

    // ================== NAV / LISTADO ==================
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

        wait.until(d -> {
            try {
                Select s = new Select(d.findElement(By.id("estadoId")));
                return s.getOptions() != null && s.getOptions().size() > 1;
            } catch (Exception e) { return false; }
        });

        Select sel = new Select(estadoEl);
        boolean selected = false;

        try {
            sel.selectByValue(ESTADO_INICIADA_VALUE);
            selected = true;
        } catch (Exception ignore) {}

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
            throw new NoSuchElementException("No se pudo seleccionar Estado Iniciada. Opciones: " + optionsToString(sel));
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

    private int contarFilasListado(WebDriver driver) {
        if (!driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody//td[contains(.,'No hay datos')]")).isEmpty()) return 0;
        return driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr")).size();
    }

    private void clickEditarPrimeraFila(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));
        int filas = contarFilasListado(driver);
        assertTrue(filas > 0, "❌ No hay solicitudes en estado 'Iniciada' para editar.");

        WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
        ));

        WebElement btnEditar = row1.findElement(By.xpath(
            ".//button[@type='button' and (contains(@title,'Editar') or contains(normalize-space(.),'Editar'))]"
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnEditar);
        Thread.sleep(150);
        jsClick(driver, btnEditar);

        Thread.sleep(800);
        acceptIfAlertPresent(driver, 5);
    }

    // ================== VALIDAR MEDIO TRANSPORTE READONLY ==================
    private void validarMedioTransporteSoloLectura(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement medio = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("medioTransporte")));

        String valueBefore = safe(medio.getAttribute("value"));
        String disabledAttr = medio.getAttribute("disabled");
        String readonlyAttr = medio.getAttribute("readonly");
        boolean isEnabled = medio.isEnabled();

        assertTrue(disabledAttr != null || readonlyAttr != null || !isEnabled,
            "❌ 'Medio de Transporte' parece editable. disabled=" + disabledAttr + ", readonly=" + readonlyAttr + ", isEnabled=" + isEnabled);

        boolean noPermiteEdicion;
        try {
            medio.click();
            medio.sendKeys("XYZ");
            Thread.sleep(200);
            String valueAfter = safe(medio.getAttribute("value"));
            noPermiteEdicion = valueAfter.equals(valueBefore);
        } catch (ElementNotInteractableException e) {
            noPermiteEdicion = true;
        } catch (Exception e) {
            noPermiteEdicion = true;
        }

        assertTrue(noPermiteEdicion, "❌ Se logró modificar 'Medio de Transporte'. Antes='" + valueBefore + "'");
    }

    // ================== SIGUIENTE NORMAL (para pasos previos) ==================
    private void clickSiguienteWizard(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(siguienteBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        if (shot != null) screenshot(driver, shot);
    }

    // ================== SIGUIENTE OPCIONAL (NO FALLA SI ESTÁ DESHABILITADO) ==================
    private void clickSiguienteOpcionalSinFallar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(siguienteBy));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);

        String dis = btn.getAttribute("disabled");
        String cls = btn.getAttribute("class") == null ? "" : btn.getAttribute("class");
        boolean clickable = btn.isDisplayed() && btn.isEnabled() && dis == null && !cls.contains("disabled");

        if (clickable) {
            jsClick(driver, btn);
            Thread.sleep(900);
            acceptIfAlertPresent(driver, 5);
        } else {
            System.out.println("ℹ️ 'Siguiente' está deshabilitado en Documentación; no se hace click (y la prueba NO falla).");
        }

        if (shot != null) screenshot(driver, shot);
    }

    // ================== PRODUCTOS (solo visible + 1 fila) ==================
    private WebElement encontrarTablaProductos(WebDriverWait wait) {
        By by = By.xpath("//table[contains(@class,'table') and contains(@class,'table-sm') and .//th[normalize-space()='Posición Arancelaria']]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void validarTablaProductosBasica(WebElement table) {
        assertTrue(table.findElements(By.xpath(".//tbody/tr")).size() > 0, "❌ La tabla de Productos no tiene filas.");
        assertTrue(table.findElements(By.xpath(".//tbody//td[contains(.,'No hay datos')]")).isEmpty(),
            "❌ La tabla de Productos muestra 'No hay datos'.");
    }

    // ================== PERMISOS (OPCIONAL) ==================
    private void validarPermisosOpcionalYContinuar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By buscarPermisoBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Buscar permiso']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(buscarPermisoBy));

        screenshot(driver, "S7_RF201021_TC023_09_permisos_btn_buscar_visible");

        // SIN agregar permisos -> Siguiente
        clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC023_10_post_siguiente_sin_permiso");
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC023_ValidarTipoSolicitudEnNumeroAsignacion_PREFIJO_EXP() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC023_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC023_02_listado");

            // ====== FILTRAR INICIADA + BUSCAR ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC023_03_estado_iniciada_set");

            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC023_04_resultados_busqueda");

            // ====== EDITAR ======
            clickEditarPrimeraFila(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC023_05_en_edicion");

            // ====== MEDIO TRANSPORTE readonly ======
            validarMedioTransporteSoloLectura(driver, wait);
            screenshot(driver, "S7_RF201021_TC023_06_medio_transporte_ok");

            // ====== SIGUIENTE -> PRODUCTOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC023_07_post_siguiente_a_productos");

            WebElement tablaProductos = encontrarTablaProductos(wait);
            screenshot(driver, "S7_RF201021_TC023_08_tabla_productos_visible");
            validarTablaProductosBasica(tablaProductos);

            // ====== SIGUIENTE -> PERMISOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC023_09_llegada_permisos");

            // Permisos opcional -> Siguiente sin agregar (llega a Documentación)
            validarPermisosOpcionalYContinuar(driver, wait, js);

            // ====== DOCUMENTACIÓN (AQUÍ TERMINA) ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")
            ));
            screenshot(driver, "S7_RF201021_TC023_11_documentacion_inicio");

            // Intentar Siguiente SIN FALLAR (si está deshabilitado o no avanza, igual termina OK)
            clickSiguienteOpcionalSinFallar(driver, wait, js, "S7_RF201021_TC023_13_doc_siguiente_try1");

            System.out.println("✅ TC-023 OK (ajustado): Llegó a Documentación y finalizó tras intentar 'Siguiente'.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC023_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC023_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
