package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
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
 * TC-017: Validar visualización del listado de productos con el formato definido.
 *
 * Versión SIMPLE:
 * - Valida que la tabla exista
 * - Valida headers esperados
 * - Valida que haya al menos 1 fila con datos (no "No hay datos para mostrar")
 *
 * NO valida valores ni formatos numéricos.
 */
public class RF2_01_02_1_TC_017Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciada";

    // Medios de Transportes (react-select)
    private static final String TIPO_MEDIO_TRANSPORTE = "Contenedor aéreo";

    private static final List<String> HEADERS_ESPERADOS = Arrays.asList(
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

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String optionsToString(Select sel) {
        return sel.getOptions().stream()
            .map(o -> "[" + safe(o.getAttribute("value")) + "] " + safe(o.getText()))
            .collect(Collectors.joining(" | "));
    }

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
        el.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(120);
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

    private int contarFilasTablaListado(WebDriver driver) {
        if (!driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody//td[contains(.,'No hay datos')]")).isEmpty()) {
            return 0;
        }
        return driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr")).size();
    }

    private void clickEditarPrimeraFila(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));
        int filas = contarFilasTablaListado(driver);
        assertTrue(filas > 0, "❌ No hay solicitudes en estado 'Iniciada' para poder editar.");

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

        boolean noPermiteEdicion = false;
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
        screenshot(driver, "S7_RF201021_TC017_05_medio_transporte_solo_lectura_ok");
    }

    // ================== MEDIOS DE TRANSPORTES (solo para desbloquear Siguiente) ==================
    private boolean esPantallaMediosTransporte(WebDriver driver) {
        return driver.findElements(By.id("medioTransporte")).size() > 0
            && driver.findElements(By.id("identificacion")).size() > 0
            && driver.findElements(By.xpath("//button[@type='submit' and contains(.,'Agregar transporte')]")).size() > 0;
    }

    private boolean tablaMediosTransporteEstaVacia(WebDriver driver) {
        By emptyTd = By.xpath(
            "//input[@id='medioTransporte']/ancestor::div[contains(@class,'card-body')]"
          + "//table[contains(@class,'table')]//tbody//td[contains(translate(normalize-space(.),"
          + " 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ','abcdefghijklmnopqrstuvwxyzáéíóúñ'),"
          + " 'no hay datos para mostrar')]"
        );
        return driver.findElements(emptyTd).size() > 0;
    }

    private void seleccionarTipoMedioTransporteContenedorAereo(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement label = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='tipoMedioTransporte']")));
        WebElement formGroup = label.findElement(By.xpath("./ancestor::div[contains(@class,'form-group')]"));

        WebElement control = formGroup.findElement(By.cssSelector("div[class*='control']"));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", control);
        jsClick(driver, control);

        WebElement input = formGroup.findElement(By.cssSelector("input[id^='react-select-'][id$='-input']"));
        input.click();

        input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
        input.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);
        Thread.sleep(120);
        input.sendKeys(TIPO_MEDIO_TRANSPORTE);
        Thread.sleep(250);

        String listboxId = input.getAttribute("aria-controls");
        if (listboxId != null && !listboxId.trim().isEmpty()) {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(listboxId)));
        } else {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id^='react-select-'][id$='-listbox']")));
        }

        // Selección tolerante (contiene "contenedor" y "aéreo")
        String tr = "translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ')";
        By opcion = (listboxId != null && !listboxId.trim().isEmpty())
            ? By.xpath("//*[@id='" + listboxId + "']//*[contains(@id,'-option-') and contains(" + tr + ",'contenedor') and contains(" + tr + ",'aéreo')]")
            : By.xpath("//*[contains(@id,'-option-') and contains(" + tr + ",'contenedor') and contains(" + tr + ",'aéreo')]");

        try {
            WebElement opt = wait.until(ExpectedConditions.elementToBeClickable(opcion));
            jsClick(driver, opt);
        } catch (Exception e) {
            input.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
            Thread.sleep(150);
            input.sendKeys(org.openqa.selenium.Keys.ENTER);
        }

        Thread.sleep(250);
    }

    private void asegurarTransporteSiTablaVacia(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shotBase) throws InterruptedException {
        if (!esPantallaMediosTransporte(driver)) return;

        if (shotBase != null) screenshot(driver, shotBase + "_MT_00_en_pantalla");

        if (tablaMediosTransporteEstaVacia(driver)) {
            seleccionarTipoMedioTransporteContenedorAereo(driver, wait, js);

            WebElement identificacion = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("identificacion")));
            if (safe(identificacion.getAttribute("value")).isEmpty()) {
                clearAndType(identificacion, "AER-" + (System.currentTimeMillis() % 1000000));
            }

            WebElement marchamo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("marchamo")));
            if (safe(marchamo.getAttribute("value")).isEmpty()) {
                clearAndType(marchamo, "MAR-" + (System.currentTimeMillis() % 1000000));
            }

            if (shotBase != null) screenshot(driver, shotBase + "_MT_01_campos_llenos");

            WebElement btnAgregar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit' and contains(.,'Agregar transporte')]")
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnAgregar);
            jsClick(driver, btnAgregar);

            Thread.sleep(900);
            acceptIfAlertPresent(driver, 5);

            wait.until(d -> !tablaMediosTransporteEstaVacia(d));

            if (shotBase != null) screenshot(driver, shotBase + "_MT_02_agregado_ok");
        } else {
            if (shotBase != null) screenshot(driver, shotBase + "_MT_01_yahabia");
        }
    }

    private void clickSiguienteWizard(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        asegurarTransporteSiTablaVacia(driver, wait, js, shot);

        By siguienteBy = By.xpath("//button[@type='button' and normalize-space()='Siguiente']");

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(siguienteBy));
        wait.until(d -> {
            WebElement b = d.findElement(siguienteBy);
            String dis = b.getAttribute("disabled");
            String cls = b.getAttribute("class") == null ? "" : b.getAttribute("class");
            return b.isDisplayed() && b.isEnabled() && dis == null && !cls.contains("disabled");
        });

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        if (shot != null) screenshot(driver, shot);
    }

    // ================== VALIDAR TABLA PRODUCTOS (SIMPLE) ==================
    private WebElement encontrarTablaProductos(WebDriver driver, WebDriverWait wait) {
        By by = By.xpath("//table[contains(@class,'table') and contains(@class,'table-sm') and .//th[normalize-space()='Posición Arancelaria']]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void validarHeadersTablaProductos(WebElement table) {
        List<WebElement> ths = table.findElements(By.xpath(".//thead//th"));
        List<String> headers = ths.stream().map(t -> safe(t.getText())).collect(Collectors.toList());

        Assertions.assertEquals(HEADERS_ESPERADOS, headers,
            "❌ Encabezados de tabla Productos no coinciden.\nEsperado: " + HEADERS_ESPERADOS + "\nActual: " + headers);
    }

    private void validarTablaProductosTieneAlMenosUnaFila(WebElement table) {
        // No debe mostrar "No hay datos para mostrar"
        boolean noHayDatos = !table.findElements(By.xpath(".//tbody//td[contains(translate(normalize-space(.),"
            + " 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ','abcdefghijklmnopqrstuvwxyzáéíóúñ'),'no hay datos')]")).isEmpty();
        assertTrue(!noHayDatos, "❌ La tabla de Productos muestra 'No hay datos para mostrar'.");

        List<WebElement> rows = table.findElements(By.xpath(".//tbody/tr"));
        assertTrue(rows != null && !rows.isEmpty(), "❌ La tabla de Productos no tiene filas.");

        // Primera fila debe tener columnas
        WebElement row1 = rows.get(0);
        List<WebElement> tds = row1.findElements(By.xpath("./td"));
        assertTrue(tds.size() >= 8, "❌ La primera fila no tiene al menos 8 columnas. Columnas=" + tds.size());

        // Solo verificamos que las celdas principales existan y tengan algún texto (sin formatos)
        for (int i = 0; i <= 6; i++) {
            String val = safe(tds.get(i).getText());
            assertTrue(!val.isEmpty(), "❌ La celda #" + i + " en la primera fila está vacía.");
        }

        // Acciones: debe existir algún botón
        boolean hayBoton = !tds.get(7).findElements(By.xpath(".//button")).isEmpty();
        assertTrue(hayBoton, "❌ No se encontró ningún botón en la columna Acciones.");

        System.out.println("✅ Tabla Productos OK (simple). Primera fila: " + row1.getText());
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC017_ValidarTablaProductos_FormatoYValores() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC017_01_login_ok");

            // ====== IR A LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC017_02_listado");

            // ====== FILTRAR ESTADO INICIADA ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC017_03_estado_iniciada_set");

            // ====== BUSCAR ======
            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC017_04_resultados_busqueda");

            // ====== EDITAR PRIMERA FILA ======
            clickEditarPrimeraFila(driver, wait, js);

            // ====== VALIDAR MEDIO TRANSPORTE SOLO LECTURA ======
            validarMedioTransporteSoloLectura(driver, wait, js);

            // ====== SIGUIENTE (ir a Productos) ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC017_06_post_siguiente_a_productos");

            // ====== VALIDAR TABLA PRODUCTOS (SIMPLE) ======
            WebElement tabla = encontrarTablaProductos(driver, wait);
            screenshot(driver, "S7_RF201021_TC017_07_tabla_productos_visible");

            validarHeadersTablaProductos(tabla);
            validarTablaProductosTieneAlMenosUnaFila(tabla);

            screenshot(driver, "S7_RF201021_TC017_08_tabla_productos_validada");
            System.out.println("✅ TC-017 OK (simple): Tabla de Productos visible con al menos 1 fila.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC017_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC017_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
