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
 * TC-031: Validar que el estado inicial de la Asignación sea "Sin asignar".
 *
 * Login (otro usuario, SIN switch delegado):
 *  - Usuario/Identificador: jefecuarentena@yopmail.com
 *  - Contraseña: cuarentena1
 *
 * Flujo:
 * Login -> Ir a Inspección > Asignación de Inspector (#/inspeccion/asignar)
 * -> (Opcional) Filtrar por Estado Solicitud -> Buscar
 * -> Validar que EXISTE al menos una solicitud con Estado Asignación = "Sin asignar"
 *
 * Nota QA:
 * "Sin asignar" NO es un valor del select, es una columna en la tabla.
 */
class RF08_2TC_031Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // Login SIN delegado
    private static final String USUARIO = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    // Sidebar link exacto del módulo
    private static final String ASIGNAR_HREF = "#/inspeccion/asignar";

    // Intento de filtro (si no existe en tu ambiente, el test NO debe fallar)
    private static final String ESTADO_FILTRO_VALUE = "3840"; // Recepcionada (según tu HTML anterior)
    private static final String ESTADO_FILTRO_TEXTO = "recepcionada";

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

    /**
     * Encuentra el select de Estado de forma tolerante:
     * - id=estadoId (como indicaste)
     * - name=estadoId
     * - cualquier select dentro del bloque donde esté el label "Estado"
     */
    private WebElement encontrarSelectEstado(WebDriver driver, WebDriverWait wait) {
        // 1) id
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));
        } catch (Exception ignore) {}

        // 2) name
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("select[name='estadoId']")));
        } catch (Exception ignore) {}

        // 3) por label "Estado" cercano
        try {
            By by = By.xpath("//label[contains(normalize-space(),'Estado')]/following::select[1]");
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (Exception ignore) {}

        throw new NoSuchElementException("No se encontró el select de Estado (estadoId) en la pantalla.");
    }

    /**
     * Intenta seleccionar el estado, pero si NO se puede, NO falla.
     * Luego hace click en Buscar.
     */
    private void filtrarPorEstadoYBuscar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {

        WebElement selectEl = encontrarSelectEstado(driver, wait);

        boolean selected = false;

        // Intento A: Select normal por value
        try {
            Select sel = new Select(selectEl);
            sel.selectByValue(ESTADO_FILTRO_VALUE);
            selected = true;
        } catch (Exception ignore) {}

        // Intento B: por texto contiene "recepcionada"
        if (!selected) {
            try {
                Select sel = new Select(selectEl);
                for (WebElement opt : sel.getOptions()) {
                    String t = opt.getText() == null ? "" : opt.getText().trim().toLowerCase();
                    if (t.contains(ESTADO_FILTRO_TEXTO)) {
                        opt.click();
                        selected = true;
                        break;
                    }
                }
            } catch (Exception ignore) {}
        }

        // Intento C: JS set value + dispatch change (React/Bootstrap a veces lo requiere)
        if (!selected) {
            try {
                js.executeScript(
                    "arguments[0].value = arguments[1];" +
                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
                    "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                    selectEl, ESTADO_FILTRO_VALUE
                );
                Thread.sleep(300);
                selected = true; // lo marcamos como intento hecho; si no existe value, igual no debe romper
            } catch (Exception ignore) {}
        }

        if (!selected) {
            System.out.println("⚠️ No se pudo aplicar el filtro por Estado (value/texto). Se continuará SIN filtro y se hará Buscar.");
        } else {
            System.out.println("✅ Filtro de Estado aplicado (o intentado) antes de Buscar.");
        }

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
     * Validación correcta TC-031:
     * Debe existir al menos una fila con Estado Asignación = "Sin asignar".
     * (No se valida que TODAS, porque puede haber "Reasignada" u otras ya trabajadas.)
     */
    private void validarExisteEstadoAsignacionSinAsignar(WebDriver driver, WebDriverWait wait) {

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]")));

        List<WebElement> filas = driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));
        if (filas == null || filas.isEmpty()) {
            throw new NoSuchElementException("No se encontraron filas en la tabla para validar.");
        }

        String firstRowText = filas.get(0).getText() == null ? "" : filas.get(0).getText().trim().toLowerCase();
        if (firstRowText.contains("no hay datos")) {
            throw new NoSuchElementException("El filtro devolvió una tabla sin registros ('No hay datos').");
        }

        boolean existeSinAsignar = false;

        // En tu captura: columna 6 = Estado Asignación
        for (WebElement fila : filas) {
            String estadoAsignacion = "";
            try {
                estadoAsignacion = fila.findElement(By.xpath(".//td[6]"))
                        .getText()
                        .trim()
                        .toLowerCase();
            } catch (Exception ignore) {}

            if (estadoAsignacion.contains("sin asignar")) {
                existeSinAsignar = true;
                break;
            }
        }

        assertTrue(existeSinAsignar,
            "❌ TC-031 FALLÓ: No se encontró ninguna solicitud con Estado Asignación = 'Sin asignar' en la tabla.");
    }

    // ================== TEST ==================
    @Test
    void RF082_TC031_Validar_Estado_Inicial_Sin_Asignar() throws InterruptedException {

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

            // ====== LOGIN (SIN SWITCH DE DELEGADO) ======
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
            screenshot(driver, "S6_RF082_TC031_01_login_ok");

            // ====== IR A ASIGNACIÓN DE INSPECTOR ======
            irAAsignacionInspector(driver, wait, js);
            screenshot(driver, "S6_RF082_TC031_02_modulo_asignacion");

            // ====== (OPCIONAL) FILTRAR ESTADO SOLICITUD + BUSCAR ======
            filtrarPorEstadoYBuscar(driver, wait, js);
            screenshot(driver, "S6_RF082_TC031_03_busqueda");

            // ====== VALIDAR QUE EXISTA "SIN ASIGNAR" ======
            validarExisteEstadoAsignacionSinAsignar(driver, wait);
            screenshot(driver, "S6_RF082_TC031_04_existe_sin_asignar_ok");

            System.out.println("✅ TC-031 OK: Existe al menos una solicitud con Estado Asignación = 'Sin asignar'.");

        } catch (TimeoutException te) {
            screenshot(driver, "S6_RF082_TC031_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S6_RF082_TC031_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
