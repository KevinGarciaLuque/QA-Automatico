package e2e.sprint_7;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
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
 * TC-024 (AJUSTADO): Validar formato completo del Número de Asignación.
 *
 * Formato esperado: XXX-(EXP|IMP)-YYYY-MM-#####  (ej: GLS-EXP-2026-02-00024 / AMT-IMP-2026-02-00031)
 *
 * Flujo (igual TC-023 hasta documentación):
 * Login -> Listado -> filtrar estado Iniciada -> Buscar -> Editar
 * -> validar Medio de Transporte solo lectura
 * -> Siguiente -> Productos
 * -> Siguiente -> Permisos (opcional) -> Siguiente
 * -> Documentación:
 *      - SI ya están los docs: SOLO "Siguiente"
 *      - SI no están: llenar + adjuntar + Guardar documentos -> "Siguiente"
 * -> Enviar
 * -> Modal: validar número de asignación (display-4 text-success) con regex (IMP o EXP)
 * -> Continuar
 */
public class RF2_01_02_1_TC_024Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Estado Iniciada
    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciad";

    // PDFs base dir
    private static final String PDF_BASE_DIR = "C:\\Users\\kevin\\Downloads\\pdf_pruebas";

    // Nombres de PDFs (ajusta si cambian)
    private static final String PDF_1 = "Prueba 1.pdf";
    private static final String PDF_2 = "Prueba 2.pdf";
    private static final String PDF_3 = "Prueba 3.pdf";
    private static final String PDF_4 = "Prueba 4.pdf";
    private static final String PDF_5 = "Prueba 5.pdf";
    private static final String PDF_6 = "Prueba 6.pdf";

    // Documentación (texto “cualquier dato”)
    private static final String DOC_FACTURA_NUM = "654654313";
    private static final String DOC_DUCA_NUM = "f65sd4fd56";
    private static final String DOC_MANIFIESTO_NUM = "fdsfdsf";
    private static final String DOC_CERT_EXP = "fdsfdsfd";
    private static final String DOC_CERT_ORI = "sdfdsfdsf";
    private static final String DOC_CITES = "dsfdsf";

    // Regex formato asignación: XXX-(EXP|IMP)-YYYY-MM-#####
    // - Prefijo: 3 letras
    // - Tipo: EXP o IMP
    // - Año 4 dígitos
    // - Mes 01-12
    // - Correlativo 5 dígitos
    private static final Pattern ASIGNACION_PATTERN =
        Pattern.compile("^[A-Z]{3}-(EXP|IMP)-\\d{4}-(0[1-9]|1[0-2])-\\d{5}$");

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

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(120);
    }

    private String lowerXpathContains(String text) {
        return "contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'" +
                text.toLowerCase() + "')";
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

    // ================== SIGUIENTE (ROBUSTO) ==================
    private boolean tryClickSiguienteFast(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(siguienteBy));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(120);

        String dis = btn.getAttribute("disabled");
        String cls = btn.getAttribute("class") == null ? "" : btn.getAttribute("class");
        boolean clickable = btn.isDisplayed() && btn.isEnabled() && dis == null && !cls.contains("disabled");

        if (clickable) {
            jsClick(driver, btn);
            Thread.sleep(900);
            acceptIfAlertPresent(driver, 5);
            if (shot != null) screenshot(driver, shot);
            return true;
        }

        if (shot != null) screenshot(driver, shot);
        return false;
    }

    private void clickSiguienteWizard(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shot) throws InterruptedException {
        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");

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

    // ================== PERMISOS (OPCIONAL) ==================
    private void permisosOpcionalContinuar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By buscarPermisoBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Buscar permiso']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(buscarPermisoBy));
        screenshot(driver, "S7_RF201021_TC024_09_permisos_btn_buscar_visible");

        clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC024_10_post_siguiente_sin_permiso");
    }

    // ================== DOCUMENTACIÓN (CON FALLBACK) ==================
    private boolean estaEnDocumentacion(WebDriver driver) {
        return !driver.findElements(By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")).isEmpty();
    }

    private String buildPdfPath(String fileName) {
        String full = PDF_BASE_DIR + File.separator + fileName;
        File f = new File(full);
        if (!f.exists()) throw new RuntimeException("No se encontró el PDF: " + fileName + " | Ruta: " + full);
        return f.getAbsolutePath();
    }

    private WebElement getFilaDocumentacionPorLabel(WebDriverWait wait, String labelContains) {
        By filaBy = By.xpath(
            "//div[contains(@class,'row') and contains(@class,'align-items-end') and .//label[" + lowerXpathContains(labelContains) + "]]"
        );
        return wait.until(ExpectedConditions.visibilityOfElementLocated(filaBy));
    }

    private boolean filaPareceYaTenerPdf(WebElement fila) {
        String tr = "translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ')";
        List<WebElement> indicios = fila.findElements(By.xpath(
            ".//*[self::a or self::span or self::div or self::small or self::strong]" +
            "[contains(" + tr + " , '.pdf') or contains(" + tr + ", 'ver') or contains(" + tr + ", 'cargado') or contains(" + tr + ", 'adjunt')]"
        ));
        return indicios != null && !indicios.isEmpty();
    }

    private void safeUploadFile(JavascriptExecutor js, WebElement fileInput, String absPath) {
        try {
            fileInput.sendKeys(absPath);
        } catch (ElementNotInteractableException e) {
            js.executeScript(
                "arguments[0].style.display='block';" +
                "arguments[0].style.visibility='visible';" +
                "arguments[0].style.opacity=1;" +
                "arguments[0].removeAttribute('hidden');",
                fileInput
            );
            fileInput.sendKeys(absPath);
        }
    }

    private void llenarFilaDocTextoYArchivoSiHaceFalta(WebDriverWait wait, JavascriptExecutor js,
                                                       String label, String valorTexto, String pdfName) throws InterruptedException {

        WebElement fila = getFilaDocumentacionPorLabel(wait, label);

        WebElement inputText = fila.findElement(By.xpath(".//input[@type='text' and contains(@class,'form-control')]"));
        String actual = safe(inputText.getAttribute("value"));
        if (actual.isEmpty() && valorTexto != null) clearAndType(inputText, valorTexto);

        if (pdfName != null && !filaPareceYaTenerPdf(fila)) {
            WebElement inputFile = fila.findElement(By.xpath(".//input[@type='file']"));
            safeUploadFile(js, inputFile, buildPdfPath(pdfName));
            Thread.sleep(250);
        }
    }

    private void llenarDocumentacionYGuardar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")
        ));
        screenshot(driver, "S7_RF201021_TC024_11_documentacion_inicio");

        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "Número de factura", DOC_FACTURA_NUM, PDF_1);
        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "Número de DUCA", DOC_DUCA_NUM, PDF_2);
        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "Manifiesto", DOC_MANIFIESTO_NUM, PDF_3);
        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "Certificado de Exportación", DOC_CERT_EXP, PDF_4);
        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "Certificado de Origen", DOC_CERT_ORI, PDF_5);
        llenarFilaDocTextoYArchivoSiHaceFalta(wait, js, "CITES", DOC_CITES, PDF_6);

        By guardarDocsBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Guardar documentos']");
        if (!driver.findElements(guardarDocsBy).isEmpty()) {
            WebElement btnGuardar = wait.until(ExpectedConditions.elementToBeClickable(guardarDocsBy));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnGuardar);
            Thread.sleep(150);
            jsClick(driver, btnGuardar);

            Thread.sleep(900);
            acceptIfAlertPresent(driver, 5);

            screenshot(driver, "S7_RF201021_TC024_12_documentacion_guardada");
        } else {
            screenshot(driver, "S7_RF201021_TC024_12_documentacion_sin_btn_guardar");
        }
    }

    private void avanzarDesdeDocumentacionConFallback(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")
        ));
        screenshot(driver, "S7_RF201021_TC024_11_documentacion_inicio");

        // Intento 1: SOLO siguiente
        tryClickSiguienteFast(driver, wait, js, "S7_RF201021_TC024_13_doc_siguiente_try1");

        Thread.sleep(600);
        if (!estaEnDocumentacion(driver)) return; // ya avanzó

        // Fallback: completar + guardar
        screenshot(driver, "S7_RF201021_TC024_13_doc_aun_no_avanza_fallback");
        llenarDocumentacionYGuardar(driver, wait, js);

        // Intento 2: siguiente luego de guardar
        clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC024_14_doc_siguiente_try2_post_guardar");

        // asegurar salida
        wait.until(d -> !estaEnDocumentacion(d));
    }

    // ================== ENVIAR + VALIDAR ASIGNACIÓN ==================
    private void clickEnviar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By enviarBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Enviar']");
        WebElement btnEnviar = wait.until(ExpectedConditions.elementToBeClickable(enviarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnEnviar);
        Thread.sleep(150);
        jsClick(driver, btnEnviar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        screenshot(driver, "S7_RF201021_TC024_15_click_enviar");
    }

    private void validarNumeroAsignacionEnModal(WebDriver driver, WebDriverWait wait) {
        By asignacionBy = By.cssSelector("div.display-4.text-success");
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(asignacionBy));

        String asignacion = safe(el.getText()).toUpperCase();
        assertTrue(ASIGNACION_PATTERN.matcher(asignacion).matches(),
            "❌ Número de asignación no cumple formato XXX-(EXP|IMP)-YYYY-MM-#####. Actual: " + asignacion);

        screenshot(driver, "S7_RF201021_TC024_16_modal_asignacion_ok");
    }

    private void clickContinuarModal(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By continuarBy = By.xpath("//button[@type='button' and (contains(@class,'btn-success') or contains(@class,'btn-primary')) and normalize-space()='Continuar']");
        WebElement btnContinuar = wait.until(ExpectedConditions.elementToBeClickable(continuarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnContinuar);
        Thread.sleep(150);
        jsClick(driver, btnContinuar);

        Thread.sleep(800);
        acceptIfAlertPresent(driver, 5);

        screenshot(driver, "S7_RF201021_TC024_17_post_continuar");
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC024_FormatoNumeroAsignacion_IMP_O_EXP() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC024_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC024_02_listado");

            // ====== FILTRAR INICIADA + BUSCAR ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC024_03_estado_iniciada_set");

            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC024_04_resultados_busqueda");

            // ====== EDITAR ======
            clickEditarPrimeraFila(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC024_05_en_edicion");

            // ====== VALIDAR MEDIO TRANSPORTE SOLO LECTURA ======
            validarMedioTransporteSoloLectura(driver, wait);
            screenshot(driver, "S7_RF201021_TC024_06_medio_transporte_ok");

            // ====== SIGUIENTE -> PRODUCTOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC024_07_post_siguiente_a_productos");

            // ====== SIGUIENTE -> PERMISOS ======
            clickSiguienteWizard(driver, wait, js, "S7_RF201021_TC024_08_llegada_permisos");

            // Permisos opcional -> Siguiente sin agregar
            permisosOpcionalContinuar(driver, wait, js);

            // ====== DOCUMENTACIÓN: si ya están los docs -> solo Siguiente; si no -> llenar/guardar y Siguiente ======
            avanzarDesdeDocumentacionConFallback(driver, wait, js);

            // ====== ENVIAR -> MODAL VALIDAR ASIGNACIÓN (IMP o EXP) -> CONTINUAR ======
            clickEnviar(driver, wait, js);
            validarNumeroAsignacionEnModal(driver, wait);
            clickContinuarModal(driver, wait, js);

            System.out.println("✅ TC-024 OK: Número de Asignación cumple formato XXX-(EXP|IMP)-YYYY-MM-#####.");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC024_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC024_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
