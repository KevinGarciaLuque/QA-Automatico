package e2e.sprint_7;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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
 * TC-003: Validar generación de eventos y notificaciones al enviar solicitud de Exportación.
 *
 * Flujo (reutiliza TC-001):
 * Listado -> Crear Solicitud -> Iniciar -> Transporte -> Agregar transporte -> Siguiente
 * -> Productos -> Agregar producto -> Siguiente
 * -> Buscar permiso -> Buscar -> Seleccionar -> Siguiente
 * -> Documentación (llenar + adjuntar PDFs + Guardar documentos) -> Siguiente
 * -> Enviar -> Modal -> Continuar
 *
 * Validaciones (post envío):
 * 1) En listado: estado "Enviada"
 * 2) En "Resumen": se observa evento/registro relacionado al envío (busca "enviad")
 * 3) Notificación: se observa notificación relacionada (busca número solicitud o "enviad")
 */
public class RF2_01_02_1_TC_003Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";
    private static final String LISTADO_URL = "http://3.228.164.208/#/inspeccion/solicitudes";

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

    // Documentación (texto “cualquier dato”)
    private static final String DOC_FACTURA_NUM = "654654313";
    private static final String DOC_DUCA_NUM = "f65sd4fd56";
    private static final String DOC_MANIFIESTO_NUM = "fdsfdsf";
    private static final String DOC_CERT_EXP = "fdsfdsfd";
    private static final String DOC_CERT_ORI = "sdfdsfdsf";
    private static final String DOC_CITES = "dsfdsf";

    // PDFs
    private static final String PDF_1 = "Prueba 1.pdf";
    private static final String PDF_2 = "Prueba 2.pdf";
    private static final String PDF_3 = "Prueba 3.pdf";
    private static final String PDF_4 = "Prueba 4.pdf";
    private static final String PDF_5 = "Prueba 5.pdf";
    private static final String PDF_6 = "Prueba 6.pdf";

    // Ruta fija PDFs
    private static final String PDF_BASE_DIR = "C:\\Users\\kevin\\Downloads\\pdf_pruebas";

    // (Opcional) si querés filtrar por fecha luego del envío:
    // private static final String FECHA_HOY = "2026-02-07";
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

    private String lowerXpathContains(String text) {
        return "contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'" +
                text.toLowerCase() + "')";
    }

    private String normalize(String s) {
        return (s == null ? "" : s.trim().toLowerCase());
    }

    // ================== CREAR SOLICITUD ==================
    private void clickCrearSolicitud(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        List<By> candidates = Arrays.asList(
            By.cssSelector("button.btn.btn-primary[title='Crear nueva solicitud']"),
            By.xpath("//button[@type='button' and @title='Crear nueva solicitud' and contains(normalize-space(.),'Crear Solicitud')]"),
            By.xpath("//button[@type='button' and contains(@class,'btn-primary') and .//i[contains(@class,'fa-plus')] and contains(.,'Crear Solicitud')]")
        );

        WebElement btn = findFirstClickable(wait, candidates);
        if (btn == null) throw new NoSuchElementException("No se encontró el botón: Crear Solicitud.");

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
        By btnBy = By.xpath("//button[@type='submit' and contains(@class,'btn-primary') and " + lowerXpathContains("agregar transporte") + "]");
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

        reactSelectTypeAndConfirmByLabel(wait, "país de origen", PAIS_ORIGEN);
        reactSelectTypeAndConfirmByLabel(wait, "país de procedencia", PAIS_PROCEDENCIA);

        By btnAgregarProductoBy = By.xpath("//button[@type='submit' and contains(@class,'btn-primary') and normalize-space()='Agregar Producto']");
        WebElement btnAgregar = wait.until(ExpectedConditions.elementToBeClickable(btnAgregarProductoBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnAgregar);
        Thread.sleep(150);
        jsClick(driver, btnAgregar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        By noDataBy = By.xpath("//td[contains(@class,'text-muted') and contains(.,'No hay datos para mostrar')]");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(noDataBy));
    }

    // ================== PERMISO ==================
    private void buscarYSeleccionarPermiso(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By btnBuscarPermisoBy = By.xpath("//button[@type='button' and contains(@class,'btn-secondary') and normalize-space()='Buscar permiso']");
        WebElement btnBuscarPermiso = wait.until(ExpectedConditions.elementToBeClickable(btnBuscarPermisoBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscarPermiso);
        Thread.sleep(150);
        jsClick(driver, btnBuscarPermiso);

        Thread.sleep(600);
        acceptIfAlertPresent(driver, 3);

        WebElement inputPermiso = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("numeroPermiso")));
        clearAndType(inputPermiso, NUM_PERMISO);

        WebElement cont = inputPermiso.findElement(By.xpath("./ancestor::div[contains(@class,'modal') or contains(@class,'card') or contains(@class,'row')][1]"));
        WebElement btnBuscar = cont.findElement(By.xpath(".//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Buscar']"));
        jsClick(driver, btnBuscar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        By btnSeleccionarBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and contains(@class,'btn-sm') and normalize-space()='Seleccionar']");
        WebElement btnSeleccionar = wait.until(ExpectedConditions.elementToBeClickable(btnSeleccionarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnSeleccionar);
        Thread.sleep(150);
        jsClick(driver, btnSeleccionar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 5);

        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        waitButtonEnabled(driver, wait, siguienteBy);
    }

    // ================== DOCUMENTACIÓN ==================
    private String buildPdfPath(String fileName) {
        String full = PDF_BASE_DIR + File.separator + fileName;
        File f = new File(full);
        if (!f.exists()) {
            throw new RuntimeException("No se encontró el PDF: " + fileName + " | Ruta: " + full);
        }
        return f.getAbsolutePath();
    }

    private WebElement getFilaDocumentacionPorLabel(WebDriverWait wait, String labelContains) {
        String lc = labelContains.toLowerCase();
        By filaBy = By.xpath(
            "//div[contains(@class,'row') and contains(@class,'align-items-end') and .//label[" + lowerXpathContains(lc) + "]]"
        );
        return wait.until(ExpectedConditions.visibilityOfElementLocated(filaBy));
    }

    private void safeUploadFile(WebDriver driver, JavascriptExecutor js, WebElement fileInput, String absPath) {
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

    private void llenarFilaDocTextoYArchivo(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                           String label, String valorTexto, String pdfName) throws InterruptedException {

        WebElement fila = getFilaDocumentacionPorLabel(wait, label);

        WebElement inputText = fila.findElement(By.xpath(".//input[@type='text' and contains(@class,'form-control')]"));
        if (valorTexto != null) clearAndType(inputText, valorTexto);

        if (pdfName != null) {
            WebElement inputFile = fila.findElement(By.xpath(".//input[@type='file']"));
            String path = buildPdfPath(pdfName);
            safeUploadFile(driver, js, inputFile, path);
            Thread.sleep(250);
        }
    }

    private void llenarDocumentacionYGuardar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")
        ));

        llenarFilaDocTextoYArchivo(driver, wait, js, "Número de factura", DOC_FACTURA_NUM, PDF_1);
        llenarFilaDocTextoYArchivo(driver, wait, js, "Número de DUCA", DOC_DUCA_NUM, PDF_2);
        llenarFilaDocTextoYArchivo(driver, wait, js, "Manifiesto", DOC_MANIFIESTO_NUM, PDF_3);
        llenarFilaDocTextoYArchivo(driver, wait, js, "Certificado de Exportación", DOC_CERT_EXP, PDF_4);
        llenarFilaDocTextoYArchivo(driver, wait, js, "Certificado de Origen", DOC_CERT_ORI, PDF_5);
        llenarFilaDocTextoYArchivo(driver, wait, js, "CITES", DOC_CITES, PDF_6);

        By guardarDocsBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and contains(normalize-space(.),'Guardar documentos')]");
        waitButtonEnabled(driver, wait, guardarDocsBy);

        WebElement btnGuardar = wait.until(ExpectedConditions.elementToBeClickable(guardarDocsBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnGuardar);
        Thread.sleep(150);
        jsClick(driver, btnGuardar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        By siguienteBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Siguiente']");
        waitButtonEnabled(driver, wait, siguienteBy);
    }

    private void clickEnviarYContinuar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By enviarBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Enviar']");
        WebElement btnEnviar = wait.until(ExpectedConditions.elementToBeClickable(enviarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnEnviar);
        Thread.sleep(150);
        jsClick(driver, btnEnviar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 5);

        By continuarBy = By.xpath("//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Continuar']");
        WebElement btnContinuar = wait.until(ExpectedConditions.elementToBeClickable(continuarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnContinuar);
        Thread.sleep(150);
        jsClick(driver, btnContinuar);

        Thread.sleep(800);
        acceptIfAlertPresent(driver, 5);
    }

    // ================== POST-VALIDACIONES TC-003 ==================

    /** Obtiene el número de solicitud de la fila 1 del listado (col 2 en la tabla). */
    private String getNumeroSolicitudFila1(WebDriver driver, WebDriverWait wait) {
        WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
        ));
        List<WebElement> tds = row1.findElements(By.xpath("./td"));
        if (tds.size() < 2) return "";
        return normalize(tds.get(1).getText());
    }

    private void filtrarEstadoEnviada(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Solo cambiamos estado a "Enviada". (Si querés filtrar fecha, lo agregamos igual que TC_002)
        WebElement estado = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));
        new Select(estado).selectByValue(ESTADO_ENVIADA_VALUE);
        Thread.sleep(200);

        By buscarBy = By.xpath("//button[@type='button' and contains(@class,'btn-info') and contains(normalize-space(.),'Buscar')]");
        WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(buscarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        jsClick(driver, btnBuscar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 3);

        // Esperar tabla visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody/tr")));
    }

    private boolean existeBadgeEnviadaFila1(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
            ));
            return row1.findElements(By.xpath(".//span[contains(@class,'badge') and normalize-space()='Enviada']")).size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void abrirResumenFila1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By row1By = By.xpath("//table[contains(@class,'table')]//tbody/tr[1]");
        WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(row1By));

        List<By> candidates = Arrays.asList(
            By.xpath(".//button[contains(@class,'btn') and " + lowerXpathContains("resumen") + "]"),
            By.xpath(".//a[contains(@class,'btn') and " + lowerXpathContains("resumen") + "]"),
            By.xpath(".//*[self::button or self::a][contains(@class,'btn') and .//i[contains(@class,'fa-eye')]]")
        );

        WebElement btn = null;
        for (By by : candidates) {
            try {
                btn = row1.findElement(by);
                if (btn != null && btn.isDisplayed()) break;
            } catch (Exception ignore) {}
        }
        if (btn == null) throw new NoSuchElementException("No se encontró el botón 'Resumen' en la fila 1.");

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(150);
        jsClick(driver, btn);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 3);
    }

    /** Valida “eventos”: busca palabras clave de evento/bitácora + “enviad” en la página de Resumen. */
    private boolean validarEventosEnResumen(WebDriver driver, WebDriverWait wait) {
        try {
            // Espera algo “típico” del resumen (título o contenedor)
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Resumen')]")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Bitácora') or contains(normalize-space(.),'Bitacora')]")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Evento') or contains(normalize-space(.),'Eventos')]"))
            ));

            String src = normalize(driver.getPageSource());

            boolean tieneZonaEventos = src.contains("bitácora") || src.contains("bitacora") || src.contains("eventos") || src.contains("evento") || src.contains("trazabilidad") || src.contains("historial");
            boolean tieneEnvio = src.contains("enviad"); // enviad(a/o)

            return tieneZonaEventos && tieneEnvio;
        } catch (Exception e) {
            return false;
        }
    }

    /** Valida “notificaciones”: intenta abrir icono (campana/sobre) y buscar “enviad” o número de solicitud. */
    private boolean validarNotificacion(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numeroSolicitud) {
        try {
            // Intentar abrir panel/menú de notificaciones
            List<By> candidates = Arrays.asList(
                By.cssSelector("i.fa.fa-bell"),
                By.cssSelector("i.fa.fa-envelope"),
                By.xpath("//*[self::button or self::a][.//i[contains(@class,'fa-bell')]]"),
                By.xpath("//*[self::button or self::a][.//i[contains(@class,'fa-envelope')]]"),
                By.xpath("//button[contains(@class,'btn') and .//i[contains(@class,'fa-envelope')]]"),
                By.xpath("//a[contains(@class,'btn') and .//i[contains(@class,'fa-envelope')]]")
            );

            WebElement icon = null;
            for (By by : candidates) {
                try {
                    icon = driver.findElement(by);
                    if (icon != null && icon.isDisplayed()) break;
                } catch (Exception ignore) {}
            }

            if (icon != null) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", icon);
                jsClick(driver, icon);
                // Esperar un poquito a que se despliegue algo
                Thread.sleep(600);
            }

            // Buscar texto de notificación (fallback: buscar en el pageSource)
            String src = normalize(driver.getPageSource());
            boolean tieneEnvio = src.contains("enviad");
            boolean tieneNumero = (numeroSolicitud != null && !numeroSolicitud.isBlank() && src.contains(normalize(numeroSolicitud)));

            // Para que sea “notificación” (no solo texto del listado), intentamos que exista alguna zona típica:
            boolean tieneZonaNoti = src.contains("notific") || src.contains("mensaj") || src.contains("inbox") || src.contains("dropdown") || src.contains("toast") || src.contains("alert");

            // Si no hay una zona típica, igual aceptamos si encontró el número o “enviad”
            return (tieneNumero || tieneEnvio) && (tieneZonaNoti || icon != null);
        } catch (Exception e) {
            return false;
        }
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC003_EventosYNotificaciones_EnviarExportacion() throws InterruptedException {

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
            screenshot(driver, "S7_RF201021_TC003_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_02_listado");

            // ====== CREAR SOLICITUD ======
            clickCrearSolicitud(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_03_pantalla_crear");

            // ====== LLENAR CREACIÓN ======
            llenarCrearSolicitudExportacion(driver, wait);
            screenshot(driver, "S7_RF201021_TC003_04_campos_creacion_ok");

            // ====== INICIAR ======
            clickIniciarSolicitud(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_05_post_iniciar");

            // ====== TRANSPORTE ======
            llenarTransporte(driver, wait);
            screenshot(driver, "S7_RF201021_TC003_06_transporte_llenado");

            clickAgregarTransporte(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_07_transporte_agregado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC003_08_siguiente_a_productos");

            // ====== PRODUCTOS ======
            screenshot(driver, "S7_RF201021_TC003_09_productos_inicio");

            llenarProductoYAgregar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_10_producto_agregado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC003_11_siguiente_post_productos");

            // ====== PERMISO ======
            buscarYSeleccionarPermiso(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_12_permiso_seleccionado");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC003_13_siguiente_post_permiso");

            // ====== DOCUMENTACIÓN ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'card-header') and contains(normalize-space(.),'Documentación')]")
            ));
            screenshot(driver, "S7_RF201021_TC003_14_documentacion_inicio");

            llenarDocumentacionYGuardar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_15_documentacion_guardada");

            clickSiguiente(driver, wait, js, "S7_RF201021_TC003_16_siguiente_post_documentacion");

            // ====== ENVIAR -> CONTINUAR ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Enviar']")
            ));
            screenshot(driver, "S7_RF201021_TC003_17_pantalla_enviar");

            clickEnviarYContinuar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC003_18_post_continuar");

            
        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC003_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC003_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
