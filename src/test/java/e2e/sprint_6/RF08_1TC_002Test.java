package e2e.sprint_6;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;

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
 * Sprint 6 - RF08.1
 * TC-004: Filtrar por estado INICIADA -> Buscar -> Tomar primera fila -> Editar -> Siguiente x3
 *        -> Documentos (llenar + subir PDFs) -> Guardar documentos -> Siguiente -> Enviar -> Esperar -> Continuar
 *
 * Nota: La clase NO es public para permitir que el archivo se llame "8.1_TC_002Test.java".
 */
class RF08_1TC_002Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    // Sidebar link exacto
    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Filtro de estado
    private static final String ESTADO_INICIADA_VALUE = "1793"; // <option value="1793">Iniciada</option>

    // Descargas\pdf_pruebas
    private static final String DOWNLOADS = System.getProperty("user.home") + "\\Downloads\\pdf_pruebas\\";
    private static final String PDF_1 = DOWNLOADS + "Prueba 1.pdf"; // DUCA
    private static final String PDF_2 = DOWNLOADS + "Prueba 2.pdf"; // Factura
    private static final String PDF_3 = DOWNLOADS + "Prueba 3.pdf"; // Manifiesto
    private static final String PDF_4 = DOWNLOADS + "Prueba 4.pdf"; // Cert. Exportación
    private static final String PDF_5 = DOWNLOADS + "Prueba 5.pdf"; // Cert. Origen
    private static final String PDF_6 = DOWNLOADS + "Prueba 6.pdf"; // CITES

    // Datos de texto
    private static final String NUM_DUCA = "DUCA-TEST-001";
    private static final String NUM_FACTURA = "FAC-TEST-001"; // requerido
    private static final String MANIFIESTO = "MANIFIESTO-TEST-001";
    private static final String CERT_EXPORT = "CERT-EXP-TEST-001";
    private static final String CERT_ORIGEN = "CERT-ORIG-TEST-001";
    private static final String CITES = "CITES-TEST-001";

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

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(120);
    }

    private void acceptIfAlertPresent(WebDriver driver, long seconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
            Alert a = shortWait.until(ExpectedConditions.alertIsPresent());
            a.accept();
        } catch (Exception ignore) {}
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

    private void filtrarEstadoIniciadaYBuscar(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement selectEstado = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));
        Select sel = new Select(selectEstado);

        boolean selected = false;
        try {
            sel.selectByValue(ESTADO_INICIADA_VALUE);
            selected = true;
        } catch (Exception ignore) {}

        if (!selected) {
            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText() == null ? "" : opt.getText().trim().toLowerCase();
                if (t.contains("iniciada")) {
                    opt.click();
                    selected = true;
                    break;
                }
            }
        }

        if (!selected) throw new NoSuchElementException("No se pudo seleccionar Estado = Iniciada en el filtro.");

        WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='button' and contains(@class,'btn-info') and contains(.,'Buscar')]")
        ));
        btnBuscar.click();

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 2);

        // esperar al menos 1 fila (si no hay, fallará con timeout)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody/tr")));
    }

    private WebElement findPrimeraFilaDisponible(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]")));

        WebElement row1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//table[contains(@class,'table')]//tbody/tr[1]")
        ));

        String txt = row1.getText() == null ? "" : row1.getText().trim().toLowerCase();
        if (txt.contains("no hay datos para mostrar") || txt.contains("no hay datos")) {
            throw new NoSuchElementException("El filtro devolvió una tabla sin registros (fila dice: 'No hay datos').");
        }

        return row1;
    }

    private void clickEditarEnFila(WebDriver driver, WebDriverWait wait, WebElement fila) {
        WebElement btnEditar = null;

        try {
            btnEditar = fila.findElement(By.xpath(".//button[@type='button' and contains(.,'Editar')]"));
        } catch (Exception ignore) {}

        if (btnEditar == null) {
            btnEditar = fila.findElement(By.xpath(".//button[@type='button' and .//i[contains(@class,'fa-edit')]]"));
        }

        if (btnEditar == null) throw new NoSuchElementException("No se encontró el botón Editar dentro de la primera fila.");

        jsClick(driver, btnEditar);

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//button[@type='button' and normalize-space()='Siguiente']")));
    }

    private void clickSiguiente(WebDriver driver, WebDriverWait wait, String shot) throws InterruptedException {
        WebElement btnSiguiente = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='button' and contains(@class,'btn') and contains(@class,'btn-primary') and normalize-space()='Siguiente']")));
        jsClick(driver, btnSiguiente);
        Thread.sleep(900);
        acceptIfAlertPresent(driver, 2);
        if (shot != null) screenshot(driver, shot);
    }

    private void assertFileExists(String path) {
        File f = new File(path);
        if (!f.exists()) throw new IllegalStateException("❌ No se encontró el archivo: " + path);
    }

    /**
     * Llenar una fila de Documentos localizando el row por label.
     * Dentro del row:
     *  - input[type=text] si existe
     *  - input[type=file] si existe
     */
    private void fillDocRow(WebDriver driver, WebDriverWait wait,
                            String labelContains, String textValue, String filePath,
                            String shotName) throws InterruptedException {

        By rowBy = By.xpath("//div[contains(@class,'row') and .//label[contains(normalize-space(),'" + labelContains + "')]]");
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowBy));

        try {
            WebElement txt = row.findElement(By.xpath(".//input[@type='text']"));
            if (textValue != null) clearAndType(txt, textValue);
        } catch (Exception ignore) {}

        if (filePath != null) {
            assertFileExists(filePath);
            WebElement fileInput = row.findElement(By.xpath(".//input[@type='file']"));
            fileInput.sendKeys(filePath);
            Thread.sleep(500);
        }

        if (shotName != null) screenshot(driver, shotName);
    }

    private void clickGuardarDocumentos(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        By btnBy = By.xpath("//button[@type='button' and contains(normalize-space(),'Guardar documentos')]");
        wait.until(ExpectedConditions.presenceOfElementLocated(btnBy));

        wait.until(d -> {
            try {
                WebElement b = d.findElement(btnBy);
                String cls = b.getAttribute("class");
                boolean disabledAttr = b.getAttribute("disabled") != null;
                boolean disabledCls = cls != null && cls.toLowerCase().contains("disabled");
                return !disabledAttr && !disabledCls && b.isDisplayed() && b.isEnabled();
            } catch (Exception e) {
                return false;
            }
        });

        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(btnBy));
        jsClick(driver, btn);

        Thread.sleep(1200);
        acceptIfAlertPresent(driver, 3);
    }

    private void clickEnviar(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        By enviarBy = By.xpath("//button[@type='button' and contains(@class,'btn-primary') and normalize-space()='Enviar']");
        WebElement btnEnviar = wait.until(ExpectedConditions.elementToBeClickable(enviarBy));
        jsClick(driver, btnEnviar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 6);
    }

    // ================== TEST ==================
    @Test
    void RF081_TC004_FiltrarIniciada_PrimeraFila_Editar_Documentos_Guardar_Siguiente_Enviar() throws InterruptedException {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(70));
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
            screenshot(driver, "S6_RF081_TC004_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S6_RF081_TC004_02_listado");

            // ====== FILTRAR INICIADA + BUSCAR ======
            filtrarEstadoIniciadaYBuscar(driver, wait);
            screenshot(driver, "S6_RF081_TC004_03_filtrado_iniciada");

            // ====== TOMAR PRIMERA FILA + EDITAR ======
            WebElement fila = findPrimeraFilaDisponible(driver, wait);
            screenshot(driver, "S6_RF081_TC004_04_primera_fila");

            clickEditarEnFila(driver, wait, fila);
            screenshot(driver, "S6_RF081_TC004_05_en_edicion");

            // ====== SIGUIENTE x 3 -> DOCUMENTOS ======
            clickSiguiente(driver, wait, "S6_RF081_TC004_06_siguiente_1");
            clickSiguiente(driver, wait, "S6_RF081_TC004_07_siguiente_2");
            clickSiguiente(driver, wait, "S6_RF081_TC004_08_siguiente_3_documentos");

            // ====== DOCUMENTOS ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(.,'Número de factura')]")
            ));
            screenshot(driver, "S6_RF081_TC004_09_documentos_inicio");

            fillDocRow(driver, wait, "Número de DUCA", NUM_DUCA, PDF_1, "S6_RF081_TC004_10_duca_ok");
            fillDocRow(driver, wait, "Número de factura", NUM_FACTURA, PDF_2, "S6_RF081_TC004_11_factura_ok");
            fillDocRow(driver, wait, "Manifiesto/Conocimiento embarque", MANIFIESTO, PDF_3, "S6_RF081_TC004_12_manifiesto_ok");
            fillDocRow(driver, wait, "Certificado de Exportación", CERT_EXPORT, PDF_4, "S6_RF081_TC004_13_cert_export_ok");
            fillDocRow(driver, wait, "Certificado de Origen", CERT_ORIGEN, PDF_5, "S6_RF081_TC004_14_cert_origen_ok");
            fillDocRow(driver, wait, "CITES", CITES, PDF_6, "S6_RF081_TC004_15_cites_ok");

            clickGuardarDocumentos(driver, wait);
            screenshot(driver, "S6_RF081_TC004_16_guardar_documentos_click");

            // Validación suave: que NO siga el alert rojo de factura requerida
            boolean okFacturaReq = true;
            try {
                WebElement alert = driver.findElement(By.xpath(
                    "//div[contains(@class,'alert') and contains(.,'El número de factura es requerido')]"
                ));
                okFacturaReq = !alert.isDisplayed();
            } catch (Exception ignore) {}
            assertTrue(okFacturaReq, "❌ Sigue el mensaje: 'El número de factura es requerido para avanzar.'");

            // ====== SIGUIENTE ======
            clickSiguiente(driver, wait, "S6_RF081_TC004_17_siguiente_post_documentos");

            // ====== ENVIAR ======
            clickEnviar(driver, wait);
            screenshot(driver, "S6_RF081_TC004_18_click_enviar");

            // ====== ESPERAR + CONTINUAR ======
            Thread.sleep(6000);

            WebElement btnContinuar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Continuar']")));
            jsClick(driver, btnContinuar);

            Thread.sleep(1000);
            screenshot(driver, "S6_RF081_TC004_19_continuar_final");

            System.out.println("✅ TC-004 OK: Filtra Iniciada, edita primera fila, sube documentos, guarda, avanza, envía y continúa.");

        } catch (TimeoutException te) {
            screenshot(driver, "S6_RF081_TC004_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S6_RF081_TC004_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
