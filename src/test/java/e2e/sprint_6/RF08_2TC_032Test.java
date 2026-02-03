package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

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
 * Sprint 6 - RF08.2
 * TC-032: Asignación de Inspector
 * - Filtrar Estado Solicitud = Enviada
 * - Buscar
 * - Encontrar fila con Estado Asignación = Sin asignar y click "Asignar"
 * - Click "Seleccionar"
 * - Click "Finalizar Asignación"
 *
 * Login (SIN switch delegado):
 *  - Usuario/Identificador: jefecuarentena@yopmail.com
 *  - Contraseña: cuarentena1
 */
class RF08_2TC_032Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    private static final String USUARIO = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String ASIGNAR_HREF = "#/inspeccion/asignar";

    // Estado Solicitud = Enviada
    private static final String ESTADO_ENVIADA_VALUE = "1794";
    private static final String ESTADO_ENVIADA_TEXTO = "enviada";

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

    private void irAAsignacionInspector(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        String url = driver.getCurrentUrl();
        if (url != null && (url.contains("#/inspeccion/asignar") || url.contains("/inspeccion/asignar"))) return;

        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("a.nav-link[href='" + ASIGNAR_HREF + "']")));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        jsClick(driver, link);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/inspeccion/asignar"),
            ExpectedConditions.urlContains("#/inspeccion/asignar")
        ));
    }

    private WebElement encontrarSelectEstado(WebDriverWait wait) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));
        } catch (Exception ignore) {}

        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("select[name='estadoId']")));
        } catch (Exception ignore) {}

        By by = By.xpath("//label[contains(normalize-space(),'Estado')]/following::select[1]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void seleccionarEstadoEnviada(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        WebElement selectEl = encontrarSelectEstado(wait);
        boolean selected = false;

        // A) value
        try {
            new Select(selectEl).selectByValue(ESTADO_ENVIADA_VALUE);
            selected = true;
        } catch (Exception ignore) {}

        // B) texto
        if (!selected) {
            try {
                Select sel = new Select(selectEl);
                for (WebElement opt : sel.getOptions()) {
                    String t = opt.getText() == null ? "" : opt.getText().trim().toLowerCase();
                    if (t.contains(ESTADO_ENVIADA_TEXTO)) {
                        opt.click();
                        selected = true;
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }

        // C) JS + change
        if (!selected) {
            try {
                js.executeScript(
                    "arguments[0].value = arguments[1];" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                    selectEl, ESTADO_ENVIADA_VALUE
                );
                selected = true;
            } catch (Exception ignore) {}
        }

        if (!selected) {
            throw new NoSuchElementException("No se pudo seleccionar 'Enviada' en el select Estado.");
        }
    }

    private void clickBuscar(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='button' and contains(@class,'btn-info') and normalize-space()='Buscar']")
        ));
        btnBuscar.click();

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 2);

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//table[contains(@class,'table')]//tbody/tr")
        ));
    }

    /**
     * Valida:
     *  - Estado Solicitud (td[5]) sea "Enviada" en todas las filas.
     *  - Encuentra la primera fila con:
     *      Estado Asignación (td[6]) = "Sin asignar"
     *      y botón "Asignar"
     */
    private WebElement validarEnviadaYTomarFilaSinAsignar(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]")));

        List<WebElement> filas = driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));
        if (filas == null || filas.isEmpty()) throw new NoSuchElementException("No hay filas para validar.");

        String firstRowText = filas.get(0).getText() == null ? "" : filas.get(0).getText().trim().toLowerCase();
        if (firstRowText.contains("no hay datos")) throw new NoSuchElementException("El filtro devolvió 'No hay datos'.");

        boolean okEstadosSolicitud = true;
        StringBuilder detalle = new StringBuilder();
        WebElement filaParaAsignar = null;

        for (int i = 0; i < filas.size(); i++) {
            WebElement fila = filas.get(i);

            String estadoSolicitud = "";
            try {
                estadoSolicitud = fila.findElement(By.xpath(".//td[5]")).getText().trim().toLowerCase();
            } catch (Exception e) {
                okEstadosSolicitud = false;
                detalle.append("\n- Fila ").append(i + 1).append(": No se pudo leer Estado Solicitud (td[5]).");
            }

            if (!estadoSolicitud.contains("enviada")) {
                okEstadosSolicitud = false;
                detalle.append("\n- Fila ").append(i + 1)
                        .append(": Estado Solicitud != Enviada (").append(estadoSolicitud).append(")");
            }

            if (filaParaAsignar == null) {
                String estadoAsignacion = "";
                try {
                    estadoAsignacion = fila.findElement(By.xpath(".//td[6]")).getText().trim().toLowerCase();
                } catch (Exception ignore) {}

                if (estadoAsignacion.contains("sin asignar")) {
                    try {
                        WebElement btnAsignar = fila.findElement(By.xpath(
                            ".//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Asignar']"
                        ));
                        if (btnAsignar.isDisplayed() && btnAsignar.isEnabled()) {
                            filaParaAsignar = fila;
                        }
                    } catch (Exception ignore) {}
                }
            }
        }

        assertTrue(okEstadosSolicitud,
            "❌ El filtro NO devolvió solo 'Enviada' en la columna Estado Solicitud. Detalle:" + detalle);

        if (filaParaAsignar == null) {
            throw new NoSuchElementException("No se encontró ninguna fila con Estado Asignación='Sin asignar' y botón 'Asignar' disponible.");
        }

        return filaParaAsignar;
    }

    private void clickAsignarEnFila(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, WebElement fila) throws InterruptedException {
        WebElement btnAsignar = fila.findElement(By.xpath(
            ".//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Asignar']"
        ));
        jsClick(driver, btnAsignar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 2);

        // Esperar que aparezca el botón "Seleccionar" (pantalla/modal de asignación)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//button[@type='button' and contains(@class,'btn-outline-info') and normalize-space()='Seleccionar']")
        ));
    }

    private void seleccionarInspector(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement btnSeleccionar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='button' and contains(@class,'btn-outline-info') and normalize-space()='Seleccionar']")
        ));
        jsClick(driver, btnSeleccionar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 2);
    }

    private void finalizarAsignacion(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement btnFinalizar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='button' and contains(@class,'btn-success') and normalize-space()='Finalizar Asignación']")
        ));
        jsClick(driver, btnFinalizar);

        Thread.sleep(1200);
        acceptIfAlertPresent(driver, 3);

        // Espera suave por confirmación / refresco (sin amarrar a texto específico)
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(@class,'alert') and (contains(.,'éxito') or contains(.,'Éxito') or contains(.,'asign'))]")),
                ExpectedConditions.urlContains("/inspeccion/asignar"),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody/tr"))
            ));
        } catch (Exception ignore) {}
    }

    // ================== TEST ==================
    @Test
    void RF082_TC032_Filtrar_Enviada_Validar_SinAsignar_Asignar_Seleccionar_Finalizar() throws InterruptedException {

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
            WebElement userInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='text'], input[type='email'], input#identificador, input[name='identificador']")
            ));
            userInput.clear();
            userInput.sendKeys(USUARIO);

            WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='password'], input#password, input[name='password']")
            ));
            password.clear();
            password.sendKeys(PASSWORD);

            WebElement botonInicio = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Inicio' or @type='submit']")
            ));
            botonInicio.click();

            wait.until(ExpectedConditions.or(
                ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link[href='" + ASIGNAR_HREF + "']"))
            ));
            screenshot(driver, "S6_RF082_TC032_01_login_ok");

            // ====== IR AL MÓDULO ======
            irAAsignacionInspector(driver, wait, js);
            screenshot(driver, "S6_RF082_TC032_02_modulo_asignacion");

            // ====== FILTRAR = ENVIADA ======
            seleccionarEstadoEnviada(driver, wait, js);
            screenshot(driver, "S6_RF082_TC032_03_estado_enviada_seleccionado");

            // ====== BUSCAR ======
            clickBuscar(driver, wait);
            screenshot(driver, "S6_RF082_TC032_04_resultados_busqueda");

            // ====== TOMAR FILA SIN ASIGNAR + CLICK ASIGNAR ======
            WebElement fila = validarEnviadaYTomarFilaSinAsignar(driver, wait);
            screenshot(driver, "S6_RF082_TC032_05_fila_sin_asignar_encontrada");

            clickAsignarEnFila(driver, wait, js, fila);
            screenshot(driver, "S6_RF082_TC032_06_pantalla_asignacion");

            // ====== SELECCIONAR ======
            seleccionarInspector(driver, wait, js);
            screenshot(driver, "S6_RF082_TC032_07_click_seleccionar");

            // ====== FINALIZAR ASIGNACIÓN ======
            finalizarAsignacion(driver, wait, js);
            screenshot(driver, "S6_RF082_TC032_08_finalizar_asignacion");

            System.out.println("✅ TC-032 OK: Filtra Enviada, valida Estado Solicitud, asigna (Seleccionar + Finalizar).");

        } catch (TimeoutException te) {
            screenshot(driver, "S6_RF082_TC032_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S6_RF082_TC032_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
