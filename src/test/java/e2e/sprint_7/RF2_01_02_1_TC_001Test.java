package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
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
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.02.1
 * TC-001: Validar que una nueva solicitud de Exportación inicie con estado "Iniciado".
 *
 * Flujo:
 * Listado -> Crear Solicitud -> Iniciar -> Transporte -> Agregar transporte -> Siguiente
 * -> Productos -> Agregar producto -> Siguiente
 * -> Buscar permiso -> Buscar -> Seleccionar -> Siguiente
 * -> Volver a listado -> Validar estado "Iniciado" (primera fila).
 */
public class RF2_01_02_1_TC_001Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Crear solicitud
    private static final String TIPO_SOLICITUD_EXPORTACION_VALUE = "3836";
    private static final String MEDIO_TRANSPORTE_AEREO_VALUE = "34";
    private static final String PUESTO_CUARENTENARIO_GOLOSON_VALUE = "2820";

    // Transporte
    private static final String TIPO_MEDIO_TRANSPORTE_AVION_CARGA = "Avión de carga";
    private static final String IDENT_MEDIO_TRANSPORTE = "AVION-TEST-123";
    private static final String MARCHAMO_PRECINTO = "MARCHAMO-999";

    // Productos
    private static final String POS_ARANCELARIA = "0801.11.00.00";
    private static final String DESCRIPCION_PRODUCTO = "Producto QA Test";
    private static final String BULTOS = "1";
    private static final String PESO_NETO = "1.00";
    private static final String NOMBRE_EXPORTADOR = "EXPORTADOR QA";
    private static final String PAIS_ORIGEN = "Honduras";
    private static final String PAIS_PROCEDENCIA = "Honduras";

    // Permiso
    private static final String NUM_PERMISO = "SA-2022-039441";

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

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(120);
    }

    private WebElement findFirstClickable(WebDriverWait wait, List<By> candidates) {
        for (By by : candidates) {
            try {
                WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
                if (el != null) return el;
            } catch (Exception ignore) {}
        }
        return null;
    }

    private boolean isButtonEnabled(WebDriver driver, By by) {
        try {
            WebElement b = driver.findElement(by);
            if (!b.isDisplayed()) return false;
            if (!b.isEnabled()) return false;
            if (b.getAttribute("disabled") != null) return false;
            String cls = b.getAttribute("class");
            return cls == null || !cls.toLowerCase().contains("disabled");
        } catch (Exception e) {
            return false;
        }
    }

    private void waitButtonEnabled(WebDriver driver, WebDriverWait wait, By by) {
        wait.until(d -> isButtonEnabled(d, by));
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

    // ================== LISTADO -> CREAR SOLICITUD ==================
    private void clickCrearSolicitud(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        List<By> candidates = Arrays.asList(
            By.cssSelector("button.btn.btn-primary[title='Crear nueva solicitud']"),
            By.xpath("//button[@type='button' and @title='Crear nueva solicitud' and contains(normalize-space(.),'Crear Solicitud')]"),
            By.xpath("//button[@type='button' and contains(@class,'btn-primary') and .//i[contains(@class,'fa-plus')] and contains(.,'Crear Solicitud')]")
        );

        WebElement btn = findFirstClickable(wait, candidates);
        if (btn == null) throw new NoSuchElementException("No se encontró el botón: Crear Solicitud (title='Crear nueva solicitud').");

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 3);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tipoSolicitudId")));
    }

    private void selectByIdAndValue(WebDriverWait wait, String selectId, String value) throws InterruptedException {
        WebElement selectEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(selectId)));
        Select sel = new Select(selectEl);
        sel.selectByValue(value);
        Thread.sleep(150);
    }

    private void llenarCrearSolicitudExportacion(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        selectByIdAndValue(wait, "tipoSolicitudId", TIPO_SOLICITUD_EXPORTACION_VALUE);
        selectByIdAndValue(wait, "medioTransporteId", MEDIO_TRANSPORTE_AEREO_VALUE);
        selectByIdAndValue(wait, "puestoCuarentenarioId", PUESTO_CUARENTENARIO_GOLOSON_VALUE);

        String t1 = new Select(driver.findElement(By.id("tipoSolicitudId"))).getFirstSelectedOption().getText().trim().toLowerCase();
        String t2 = new Select(driver.findElement(By.id("medioTransporteId"))).getFirstSelectedOption().getText().trim().toLowerCase();
        String t3 = new Select(driver.findElement(By.id("puestoCuarentenarioId"))).getFirstSelectedOption().getText().trim().toLowerCase();

        if (!t1.contains("export")) throw new NoSuchElementException("No quedó seleccionado 'Exportación' en Tipo de solicitud.");
        if (!t2.contains("aéreo") && !t2.contains("aereo")) throw new NoSuchElementException("No quedó seleccionado 'Aéreo' en Medio de transporte.");
        if (!t3.contains("goloson")) throw new NoSuchElementException("No quedó seleccionado 'Aeropuerto Goloson' en Puesto Cuarentenario.");
    }

    private void clickIniciarSolicitud(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By btnBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Iniciar solicitud']");
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(btnBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 6);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("react-select-2-input")),
            ExpectedConditions.presenceOfElementLocated(By.id("identificacion")),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//button[contains(.,'Agregar transporte')]"))
        ));
    }

    // ================== REACT-SELECT HELPERS ==================
    private WebElement getReactSelectInputByLabel(WebDriverWait wait, String labelContains) {
        By groupBy = By.xpath(
            "//label[contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'" +
            labelContains.toLowerCase() +
            "')]/ancestor::div[contains(@class,'form-group') or contains(@class,'mb-3')][1]"
        );

        WebElement group = wait.until(ExpectedConditions.visibilityOfElementLocated(groupBy));

        try {
            return group.findElement(By.xpath(".//input[contains(@id,'react-select') and contains(@id,'-input')]"));
        } catch (Exception ignore) {}

        return group.findElement(By.xpath(".//input[@role='combobox' or @aria-autocomplete='list']"));
    }

    private void reactSelectTypeAndConfirmByInputId(WebDriverWait wait, String inputId, String valueToType) throws InterruptedException {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(inputId)));

        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(120);

        input.sendKeys(valueToType);
        Thread.sleep(500);

        try {
            input.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(120);
            input.sendKeys(Keys.ENTER);
        } catch (Exception ignore) {}

        Thread.sleep(250);
        input.sendKeys(Keys.TAB);
        Thread.sleep(200);
    }

    private void reactSelectTypeAndConfirmByLabel(WebDriverWait wait, String labelContains, String valueToType) throws InterruptedException {
        WebElement input = getReactSelectInputByLabel(wait, labelContains);

        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(120);

        input.sendKeys(valueToType);
        Thread.sleep(500);

        try {
            input.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(120);
            input.sendKeys(Keys.ENTER);
        } catch (Exception ignore) {}

        Thread.sleep(250);
        input.sendKeys(Keys.TAB);
        Thread.sleep(200);
    }

    // ================== TRANSPORTE ==================
    private void llenarTransporte(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("react-select-2-input")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("identificacion")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("marchamo")));

        reactSelectTypeAndConfirmByInputId(wait, "react-select-2-input", TIPO_MEDIO_TRANSPORTE_AVION_CARGA);

        clearAndType(driver.findElement(By.id("identificacion")), IDENT_MEDIO_TRANSPORTE);
        clearAndType(driver.findElement(By.id("marchamo")), MARCHAMO_PRECINTO);
    }

    private void clickAgregarTransporte(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By btnBy = By.xpath("//button[@type='submit' and contains(@class,'btn-primary') and contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'agregar transporte')]");
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(btnBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 5);

        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        waitButtonEnabled(driver, wait, siguienteBy);

        Thread.sleep(200);
    }

    private void clickSiguiente(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        By by = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        waitButtonEnabled(driver, wait, by);

        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(by));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);
        if (shot != null) screenshot(driver, shot);
    }

    // ================== PRODUCTOS ==================
    private void llenarProductoYAgregar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Productos')]")
        ));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("posicionArancelaria")));

        clearAndType(driver.findElement(By.id("posicionArancelaria")), POS_ARANCELARIA);
        clearAndType(driver.findElement(By.id("descripcionProducto")), DESCRIPCION_PRODUCTO);
        clearAndType(driver.findElement(By.id("bultos")), BULTOS);
        clearAndType(driver.findElement(By.id("pesoNeto")), PESO_NETO);
        clearAndType(driver.findElement(By.id("nombreExportador")), NOMBRE_EXPORTADOR);

        // Como pediste: escribir cualquier país en ambos
        reactSelectTypeAndConfirmByLabel(wait, "país de origen", PAIS_ORIGEN);
        reactSelectTypeAndConfirmByLabel(wait, "país de procedencia", PAIS_PROCEDENCIA);

        // Agregar Producto
        By btnAgregarProductoBy = By.xpath("//button[@type='submit' and contains(@class,'btn-primary') and normalize-space()='Agregar Producto']");
        WebElement btnAgregar = wait.until(ExpectedConditions.elementToBeClickable(btnAgregarProductoBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnAgregar);
        Thread.sleep(150);
        jsClick(driver, btnAgregar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        // Esperar que ya NO diga "No hay datos para mostrar"
        By noDataBy = By.xpath("//td[contains(@class,'text-muted') and contains(.,'No hay datos para mostrar')]");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(noDataBy));
    }

    // ================== PERMISO ==================
    private void buscarYSeleccionarPermiso(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // 1) Click "Buscar permiso"
        By btnBuscarPermisoBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Buscar permiso']");
        WebElement btnBuscarPermiso = wait.until(ExpectedConditions.elementToBeClickable(btnBuscarPermisoBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscarPermiso);
        Thread.sleep(150);
        jsClick(driver, btnBuscarPermiso);

        Thread.sleep(600);
        acceptIfAlertPresent(driver, 3);

        // 2) Ingresar número de permiso
        WebElement inputPermiso = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroPermiso")));
        clearAndType(inputPermiso, NUM_PERMISO);

        // 3) Click "Buscar" (dentro del mismo contenedor del input para evitar confusiones)
        WebElement cont = inputPermiso.findElement(By.xpath("./ancestor::div[contains(@class,'modal') or contains(@class,'card') or contains(@class,'row')][1]"));
        WebElement btnBuscar = cont.findElement(By.xpath(".//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Buscar']"));
        jsClick(driver, btnBuscar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        // 4) Click "Seleccionar"
        By btnSeleccionarBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and contains(@class,'btn-sm') and normalize-space()='Seleccionar']");
        WebElement btnSeleccionar = wait.until(ExpectedConditions.elementToBeClickable(btnSeleccionarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnSeleccionar);
        Thread.sleep(150);
        jsClick(driver, btnSeleccionar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 5);

        // 5) Esperar que el botón Siguiente esté habilitado
        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        waitButtonEnabled(driver, wait, siguienteBy);
    }

    // ================== VALIDACIÓN ESTADO ==================
    private boolean existsEstadoIniciadoEnPrimeraFilaListado(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]")));
            WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
            ));
            String txt = row1.getText() == null ? "" : row1.getText().trim().toLowerCase();
            return txt.contains("iniciado") || txt.contains("iniciada");
        } catch (Exception ignore) {
            return false;
        }
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC001_RegistroExportacion_EstadoIniciado() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC001_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_02_listado");

            // ====== CREAR SOLICITUD ======
            clickCrearSolicitud(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_03_pantalla_crear");

            // ====== LLENAR CREACIÓN ======
            llenarCrearSolicitudExportacion(driver, wait);
            screenshot(driver, "S7_RF201021_TC001_04_campos_creacion_ok");

            // ====== INICIAR ======
            clickIniciarSolicitud(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_05_post_iniciar");

            // ====== TRANSPORTE ======
            llenarTransporte(driver, wait);
            screenshot(driver, "S7_RF201021_TC001_06_transporte_llenado");

            clickAgregarTransporte(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_07_transporte_agregado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC001_08_siguiente_a_productos");

            // ====== PRODUCTOS ======
            screenshot(driver, "S7_RF201021_TC001_09_productos_inicio");

            llenarProductoYAgregar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_10_producto_agregado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC001_11_siguiente_post_productos");

            // ====== PERMISO ======
            // Buscar permiso -> Buscar -> Seleccionar -> Siguiente
            buscarYSeleccionarPermiso(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_12_permiso_seleccionado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC001_13_siguiente_post_permiso");

            // ====== VALIDAR ESTADO "INICIADO" EN LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC001_14_listado_validacion");

            boolean estadoOK = existsEstadoIniciadoEnPrimeraFilaListado(driver, wait);
            assertTrue(estadoOK, "❌ La solicitud no se observa con estado 'Iniciado' en el listado (primera fila).");

            System.out.println("✅ TC-001 OK: Flujo Exportación completado (Transporte + Productos + Permiso) y estado 'Iniciado' validado.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC001_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC001_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
