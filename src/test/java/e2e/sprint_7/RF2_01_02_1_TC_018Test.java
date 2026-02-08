package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
 * TC-018: Validar que la pantalla "Permisos de Importación" sea opcional para Exportación.
 *
 * Flujo:
 * Login -> Solicitudes -> Estado=Iniciada -> Buscar -> Editar (primera fila)
 * -> Validar Medio de Transporte solo lectura
 * -> Siguiente -> Productos (tabla visible)
 * -> Siguiente -> Permisos
 * -> Validar que existe "Buscar permiso" (opcional)
 * -> Dar Siguiente SIN agregar permiso
 * -> Validar que avanzó al siguiente paso (salió de Permisos)
 */
public class RF2_01_02_1_TC_018Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Estado Iniciada: intentamos por value; si no existe, por texto
    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciad";

    // Para TC-018 no amarramos valores exactos de la fila, solo formato mínimo de productos
    private static final List<String> HEADERS_PRODUCTOS_ESPERADOS = Arrays.asList(
        "Posición Arancelaria",
        "Descripción",
        "Bultos",
        "Peso Neto (kg)",
        "País de Origen",
        "País de Procedencia",
        "Exportador",
        "Acciones"
    );

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

    private String safe(String s) { return s == null ? "" : s.trim(); }

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

    // ================== LISTADO HELPERS ==================
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

    private void validarMedioTransporteSoloLectura(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement medio = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("medioTransporte")));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", medio);
        Thread.sleep(150);

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

    // ================== PRODUCTOS ==================
    private WebElement encontrarTablaProductos(WebDriverWait wait) {
        By by = By.xpath("//table[contains(@class,'table') and contains(@class,'table-sm') and .//th[normalize-space()='Posición Arancelaria']]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void validarTablaProductosFormatoBasico(WebElement table) {
        List<WebElement> ths = table.findElements(By.xpath(".//thead//th"));
        List<String> headers = ths.stream().map(t -> safe(t.getText())).collect(Collectors.toList());

        assertTrue(headers.equals(HEADERS_PRODUCTOS_ESPERADOS),
            "❌ Encabezados de Productos no coinciden.\nEsperado: " + HEADERS_PRODUCTOS_ESPERADOS + "\nActual: " + headers);

        List<WebElement> rows = table.findElements(By.xpath(".//tbody/tr"));
        assertTrue(rows != null && !rows.isEmpty(), "❌ La tabla de Productos no tiene filas.");

        // Validar que exista la columna Acciones con botón Eliminar en primera fila
        WebElement row1 = rows.get(0);
        List<WebElement> tds = row1.findElements(By.xpath("./td"));
        assertTrue(tds.size() >= 8, "❌ La fila de Productos no tiene las 8 columnas. Columnas=" + tds.size());

        WebElement accionesTd = tds.get(7);
        boolean tieneEliminar = !accionesTd.findElements(By.xpath(".//button[contains(@class,'btn-danger') and contains(normalize-space(.),'Eliminar')]")).isEmpty();
        assertTrue(tieneEliminar, "❌ No se encontró el botón 'Eliminar' en Acciones (fila 1).");
    }

    // ================== PERMISOS OPCIONAL ==================
    private void validarPantallaPermisosYQueEsOpcional(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Botón "Buscar permiso" debe existir (pantalla permisos)
        By buscarPermisoBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Buscar permiso']");
        WebElement btnBuscarPermiso = wait.until(ExpectedConditions.visibilityOfElementLocated(buscarPermisoBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscarPermiso);
        Thread.sleep(150);

        // Evidencia: estamos en permisos y existe botón
        screenshot(driver, "S7_RF201021_TC018_09_pantalla_permisos_btn_buscar_visible");

        // SIN agregar permisos => dar Siguiente debe permitir avanzar
        clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC018_10_post_siguiente_sin_permiso");

        // Validar que ya NO estamos en permisos:
        // - botón Buscar permiso ya no debería estar visible
        //   (si sigue, es que no avanzó)
        boolean sigueEnPermisos = !driver.findElements(buscarPermisoBy).isEmpty()
            && driver.findElements(buscarPermisoBy).get(0).isDisplayed();

        assertTrue(!sigueEnPermisos,
            "❌ El flujo NO avanzó: aún se detecta el botón 'Buscar permiso' (pantalla permisos). Debería ser opcional y permitir continuar.");
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC018_PermisosOpcionales_Exportacion() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC018_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC018_02_listado");

            // ====== FILTRO ESTADO INICIADA ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC018_03_estado_iniciada_set");

            // ====== BUSCAR ======
            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC018_04_resultados_busqueda");

            // ====== EDITAR PRIMERA FILA ======
            clickEditarPrimeraFila(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC018_05_en_edicion");

            // ====== VALIDAR MEDIO TRANSPORTE SOLO LECTURA ======
            validarMedioTransporteSoloLectura(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC018_06_medio_transporte_ok");

            // ====== SIGUIENTE -> PRODUCTOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC018_07_post_siguiente_a_productos");

            // Validar tabla productos (formato mínimo)
            WebElement tablaProductos = encontrarTablaProductos(wait);
            screenshot(driver, "S7_RF201021_TC018_08_tabla_productos_visible");
            validarTablaProductosFormatoBasico(tablaProductos);

            // ====== SIGUIENTE -> PERMISOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC018_09_llegada_permisos");

            // ====== VALIDAR QUE PERMISOS ES OPCIONAL ======
            validarPantallaPermisosYQueEsOpcional(driver, wait, js);

            System.out.println("✅ TC-018 OK: Pantalla Permisos es opcional (se puede continuar sin agregar permisos).");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC018_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC018_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
