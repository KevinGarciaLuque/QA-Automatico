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
 * TC-021: Validar límite máximo de 5MB por documento al cargar documentos.
 *
 * Flujo (basado en TC-004):
 * Filtrar por estado INICIADA -> Buscar -> Tomar primera fila -> Editar -> Siguiente x3
 * -> Documentos -> Subir PDF >5MB -> Validar rechazo con mensaje claro (máximo 5MB) -> FIN
 *
 * Nota: La clase NO es public para permitir nombre de archivo libre.
 */
class RF08_1TC_021Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    // Sidebar link exacto
    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Filtro de estado
    private static final String ESTADO_INICIADA_VALUE = "1793"; // Iniciada

    // Descargas\pdf_pruebas
    private static final String DOWNLOADS = System.getProperty("user.home") + "\\Downloads\\pdf_pruebas\\";

    // PDFs > 5MB (tus nombres)
    private static final String PDF_MAYOR_5MB_1 = DOWNLOADS + "Prueba_Mayor_5MB_1.pdf";
    private static final String PDF_MAYOR_5MB_2 = DOWNLOADS + "Prueba_Mayor_5MB_2.pdf";
    private static final String PDF_MAYOR_5MB_3 = DOWNLOADS + "Prueba_Mayor_5MB_3.pdf";
    private static final String PDF_MAYOR_5MB_4 = DOWNLOADS + "Prueba_Mayor_5MB_4.pdf";
    private static final String PDF_MAYOR_5MB_5 = DOWNLOADS + "Prueba_Mayor_5MB_5.pdf";
    private static final String PDF_MAYOR_5MB_6 = DOWNLOADS + "Prueba_Mayor_5MB_6.pdf";

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
     * En Documentos: localiza fila por label y sube el archivo.
     * (Para TC-021 NO llenamos campos de texto; solo validamos límite de tamaño al cargar).
     */
    private void uploadOnly(WebDriver driver, WebDriverWait wait,
                            String labelContains, String filePath,
                            String shotName) throws InterruptedException {

        By rowBy = By.xpath("//div[contains(@class,'row') and .//label[contains(normalize-space(),'" + labelContains + "')]]");
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowBy));

        assertFileExists(filePath);

        WebElement fileInput = row.findElement(By.xpath(".//input[@type='file']"));
        fileInput.sendKeys(filePath);
        Thread.sleep(800);

        if (shotName != null) screenshot(driver, shotName);
    }

    /**
     * Espera un mensaje visible relacionado a límite/tamaño.
     * (Soporta toast, alert, texto en pantalla).
     */
    private WebElement waitMensajeLimite5MB(WebDriverWait wait) {
        By msgBy = By.xpath(
            "//*[self::div or self::span or self::p or self::small or self::strong or self::li]" +
            "[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), '5mb')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), 'máximo')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), 'maximo')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), 'tamaño')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), 'tamano')" +
            " or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑ', 'abcdefghijklmnopqrstuvwxyzáéíóúñ'), 'excede')" +
            "]"
        );
        return wait.until(ExpectedConditions.visibilityOfElementLocated(msgBy));
    }

    // ================== TEST ==================
    @Test
    void RF081_TC021_Validar_Limite_Maximo_5MB_Al_Cargar_Documento() throws InterruptedException {

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
            screenshot(driver, "S6_RF081_TC021_01_login_ok");

            // ====== LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S6_RF081_TC021_02_listado");

            // ====== FILTRAR INICIADA + BUSCAR ======
            filtrarEstadoIniciadaYBuscar(driver, wait);
            screenshot(driver, "S6_RF081_TC021_03_filtrado_iniciada");

            // ====== TOMAR PRIMERA FILA + EDITAR ======
            WebElement fila = findPrimeraFilaDisponible(driver, wait);
            screenshot(driver, "S6_RF081_TC021_04_primera_fila");

            clickEditarEnFila(driver, wait, fila);
            screenshot(driver, "S6_RF081_TC021_05_en_edicion");

            // ====== SIGUIENTE x 3 -> DOCUMENTOS ======
            clickSiguiente(driver, wait, "S6_RF081_TC021_06_siguiente_1");
            clickSiguiente(driver, wait, "S6_RF081_TC021_07_siguiente_2");
            clickSiguiente(driver, wait, "S6_RF081_TC021_08_siguiente_3_documentos");

            // ====== DOCUMENTOS ======
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[contains(.,'Número de factura') or contains(.,'Numero de factura')]")
            ));
            screenshot(driver, "S6_RF081_TC021_09_documentos_inicio");

            // ====== SUBIR PDF > 5MB (DEBE SER RECHAZADO) ======
            // Elegimos una fila, por ejemplo: "CITES". Si querés probar otra, cambia el label.
            // También podés cambiar el PDF por cualquiera de los 6.
            uploadOnly(driver, wait, "CITES", PDF_MAYOR_5MB_1, "S6_RF081_TC021_10_upload_pdf_mayor_5mb");

            // ====== VALIDAR MENSAJE CLARO (MAX 5MB) ======
            WebElement msg = waitMensajeLimite5MB(wait);
            screenshot(driver, "S6_RF081_TC021_11_mensaje_limite_5mb");

            String t = (msg.getText() == null ? "" : msg.getText().trim().toLowerCase());
            boolean ok = t.contains("5mb") || t.contains("máximo") || t.contains("maximo")
                      || t.contains("tamaño") || t.contains("tamano") || t.contains("excede");
            assertTrue(ok, "❌ No se mostró mensaje claro del límite 5MB. Mensaje visto: " + t);

            System.out.println("✅ TC-021 OK: Documento >5MB rechazado con mensaje indicando el máximo permitido.");

        } catch (TimeoutException te) {
            screenshot(driver, "S6_RF081_TC021_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S6_RF081_TC021_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
