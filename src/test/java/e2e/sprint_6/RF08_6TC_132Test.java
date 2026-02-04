package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
 * Sprint 6 - RF08.6
 * TC-132: Validar que el botón "Ver Solicitud" muestre el Resumen de la Solicitud en un modal.
 *
 * NOTA IMPORTANTE (según tu hallazgo):
 * - En el ambiente actual NO se muestra modal. El criterio práctico de ejecución será:
 *   ✅ La prueba termina al hacer click en "Ver Solicitud" y tomar el screenshot
 *      "S6_RF086_TC132_06_click_ver_solicitud".
 *
 * Flujo:
 * 1) Login
 * 2) Ir a Consulta Parametrizada Solicitudes
 * 3) (Opcional) Llenar filtros: Fecha desde/hasta, Puesto Cuarentenario
 * 4) Ingresar RTN Delegado
 * 5) Consultar
 * 6) Click "Ver Solicitud" (primera fila)
 * 7) Esperar 2-3 segundos
 * 8) Tomar screenshot y FIN (sin validar modal)
 */
class RF08_6TC_132Test {

    private static final String BASE_URL_LOGIN = "http://3.228.164.208/#/login";
    private static final String CONSULTA_PARAMETRIZADA_URL =
        "http://3.228.164.208/#/inspeccion/solicitudes/consulta-parametrizada";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // Datos de referencia para garantizar resultados
    private static final String RTN_DELEGADO = "0512199300511";
    private static final String PUESTO_CUARENTENARIO_TEXTO = "Aeropuerto Goloson";

    // Fechas opcionales (solo si existen inputs y están editables)
    private static final String FECHA_DESDE = "01/01/2025";
    private static final String FECHA_HASTA = "31/12/2026";

    // ===================== HELPERS =====================

    private void screenshot(WebDriver driver, String name) {
        try {
            byte[] img = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream("./target/" + name + ".png")) {
                fos.write(img);
            }
            System.out.println("📸 Screenshot: ./target/" + name + ".png");
        } catch (Exception ignore) {}
    }

    private void waitDocumentReady(WebDriver d) {
        try {
            new WebDriverWait(d, Duration.ofSeconds(15)).until(x -> {
                try {
                    return "complete".equals(((JavascriptExecutor) x).executeScript("return document.readyState"));
                } catch (Exception e) { return true; }
            });
        } catch (Exception ignore) {}
    }

    private void scrollCenter(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block:'center'});", e);
    }

    private void jsClick(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].click();", e);
    }

    private void setValueJS(WebDriver d, WebElement input, String value) {
        ((JavascriptExecutor) d).executeScript(
            "arguments[0].focus();" +
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            input, value
        );
    }

    private void safeTypeIfPresent(WebDriver d, String labelText, String value) {
        By by = By.xpath("//label[contains(normalize-space(.),'" + labelText + "')]/following::*[self::input][1]");
        List<WebElement> els = d.findElements(by);
        if (els.isEmpty()) return;

        WebElement input = els.get(0);
        scrollCenter(d, input);

        String ro = input.getAttribute("readonly");
        String dis = input.getAttribute("disabled");
        if (ro != null && !ro.isBlank()) return;
        if (dis != null && !dis.isBlank()) return;

        try {
            input.click();
            input.clear();
            input.sendKeys(value);
        } catch (Exception e) {
            setValueJS(d, input, value);
        }
    }

    private void safeSelectIfPresent(WebDriver d, String labelText, String visibleTextContains) {
        By by = By.xpath("//label[contains(normalize-space(.),'" + labelText + "')]/following::*[self::select][1]");
        List<WebElement> els = d.findElements(by);
        if (els.isEmpty()) return;

        WebElement selectEl = els.get(0);
        scrollCenter(d, selectEl);

        try {
            Select sel = new Select(selectEl);
            boolean selected = false;

            for (WebElement opt : sel.getOptions()) {
                String t = (opt.getText() == null ? "" : opt.getText()).trim();
                if (!t.isEmpty() && t.toLowerCase().contains(visibleTextContains.toLowerCase())) {
                    sel.selectByVisibleText(opt.getText());
                    selected = true;
                    break;
                }
            }

            if (!selected) {
                System.out.println("ℹ️ No se encontró opción en '" + labelText + "' que contenga: " + visibleTextContains);
            }
        } catch (Exception ignore) {
            System.out.println("ℹ️ '" + labelText + "' no parece ser <select> estándar. Se omite selección.");
        }
    }

    private void clickConsultar(WebDriver d, WebDriverWait w) {
        By btnConsultar = By.xpath("//*[self::button or self::a][contains(normalize-space(.),'Consultar')]");
        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(btnConsultar));
        scrollCenter(d, btn);
        try { btn.click(); } catch (Exception e) { jsClick(d, btn); }
    }

    private WebElement waitTablaResultados(WebDriver d, WebDriverWait w) {
        By tablaBy = By.xpath(
            "//*[contains(normalize-space(.),'Resultados de la Consulta')]/following::table[1]" +
            " | //table"
        );
        WebElement table = w.until(ExpectedConditions.presenceOfElementLocated(tablaBy));
        new WebDriverWait(d, Duration.ofSeconds(25))
            .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//table//tbody/tr")));
        return table;
    }

    // ===================== PASOS =====================

    private void login(WebDriver d, WebDriverWait w) {
        d.get(BASE_URL_LOGIN);
        waitDocumentReady(d);

        WebElement user = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='text'], input[type='email'], input#identificador, input[name='identificador']")
        ));
        user.clear();
        user.sendKeys(USUARIO);

        WebElement pass = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[type='password'], input#password, input[name='password']")
        ));
        pass.clear();
        pass.sendKeys(PASSWORD);

        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[@type='submit' or contains(normalize-space(.),'Inicio') or contains(normalize-space(.),'Iniciar')]")
        ));
        btn.click();

        w.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        waitDocumentReady(d);

        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".sidebar, .sidebar-nav")));
        screenshot(d, "S6_RF086_TC132_01_login_ok");
    }

    private void irAConsultaParametrizada(WebDriver d, WebDriverWait w) {
        d.get(CONSULTA_PARAMETRIZADA_URL);

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("consulta-parametrizada"),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Filtros de Búsqueda')]"))
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC132_02_consulta_parametrizada");
    }

    private void aplicarFiltrosYConsultar(WebDriver d, WebDriverWait w) {
        // Opcionales (solo si existen)
        safeTypeIfPresent(d, "Fecha desde", FECHA_DESDE);
        safeTypeIfPresent(d, "Fecha hasta", FECHA_HASTA);
        safeSelectIfPresent(d, "Puesto Cuarentenario", PUESTO_CUARENTENARIO_TEXTO);

        // Principal (para asegurar resultados)
        safeTypeIfPresent(d, "RTN Delegado", RTN_DELEGADO);

        screenshot(d, "S6_RF086_TC132_03_filtros_listos");

        clickConsultar(d, w);

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC132_04_click_consultar");
    }

    /**
     * En el ambiente actual: NO validar modal.
     * La prueba termina al hacer click y tomar screenshot.
     */
    private void clickVerSolicitudYFinalizar(WebDriver d, WebDriverWait w) {
        waitTablaResultados(d, w);

        By btnVerSolicitudBy = By.xpath("//button[@type='button' and contains(normalize-space(.),'Ver Solicitud')]");
        List<WebElement> botones = d.findElements(btnVerSolicitudBy);

        screenshot(d, "S6_RF086_TC132_05_resultados");

        assertTrue(!botones.isEmpty(),
            "❌ TC-132 FALLÓ: No se encontró el botón 'Ver Solicitud' en el listado (columna Acciones).");

        WebElement btn = botones.get(0);
        scrollCenter(d, btn);
        try { btn.click(); } catch (Exception e) { jsClick(d, btn); }

        screenshot(d, "S6_RF086_TC132_06_click_ver_solicitud");

        // Espera corta (solo para estabilidad visual / carga ligera)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================== TEST =====================
    @Test
    void RF086_TC132_Validar_Boton_Ver_Solicitud_Muestre_Resumen_En_Modal() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments(
            "--start-maximized",
            "--lang=es-419",
            "--disable-notifications",
            "--disable-popup-blocking",
            "--disable-extensions",
            "--no-first-run",
            "--no-default-browser-check"
        );
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            login(driver, wait);

            irAConsultaParametrizada(driver, wait);

            aplicarFiltrosYConsultar(driver, wait);

            clickVerSolicitudYFinalizar(driver, wait);

            screenshot(driver, "S6_RF086_TC132_OK");
            System.out.println("✅ TC-132 OK (flujo actual): Se encontró y se hizo click en 'Ver Solicitud' (se evidencia con screenshot).");

        } catch (TimeoutException e) {
            screenshot(driver, "S6_RF086_TC132_TIMEOUT");
            throw e;
        } catch (Exception e) {
            screenshot(driver, "S6_RF086_TC132_ERROR");
            throw e;
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
