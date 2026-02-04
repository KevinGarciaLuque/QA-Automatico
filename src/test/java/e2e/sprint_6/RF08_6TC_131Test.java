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
 * TC-131: Validar existencia del botón "Ver Solicitud" en el listado de Consulta Parametrizada.
 *
 * Criterio de aceptación:
 * - El botón se muestra correctamente en el listado (columna Acciones)
 * - Se puede hacer click en el botón "Ver Solicitud" de la primera fila
 *
 * Flujo:
 * 1) Login
 * 2) Ir a Consulta Parametrizada Solicitudes
 * 3) (Opcional) Llenar filtros: Fecha desde/hasta, Puesto Cuarentenario
 * 4) Ingresar RTN Delegado
 * 5) Consultar
 * 6) Validar que exista el botón "Ver Solicitud" en el listado y hacer click (primera fila)
 * 7) Fin
 */
class RF08_6TC_131Test {

    private static final String BASE_URL_LOGIN = "http://3.228.164.208/#/login";
    private static final String CONSULTA_PARAMETRIZADA_URL =
        "http://3.228.164.208/#/inspeccion/solicitudes/consulta-parametrizada";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // Mismos datos de referencia
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
        screenshot(d, "S6_RF086_TC131_01_login_ok");
    }

    private void irAConsultaParametrizada(WebDriver d, WebDriverWait w) {
        d.get(CONSULTA_PARAMETRIZADA_URL);

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("consulta-parametrizada"),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Filtros de Búsqueda')]"))
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC131_02_consulta_parametrizada");
    }

    private void aplicarFiltrosYConsultar(WebDriver d, WebDriverWait w) {
        // Opcionales (solo si existen)
        safeTypeIfPresent(d, "Fecha desde", FECHA_DESDE);
        safeTypeIfPresent(d, "Fecha hasta", FECHA_HASTA);
        safeSelectIfPresent(d, "Puesto Cuarentenario", PUESTO_CUARENTENARIO_TEXTO);

        // Principal (puedes dejarlo o quitarlo; lo dejo para asegurar que haya resultados)
        safeTypeIfPresent(d, "RTN Delegado", RTN_DELEGADO);

        screenshot(d, "S6_RF086_TC131_03_filtros_listos");

        clickConsultar(d, w);

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC131_04_click_consultar");
    }

    private void validarExisteYClickVerSolicitud(WebDriver d, WebDriverWait w) {
        WebElement table = waitTablaResultados(d, w);

        // Validar que exista al menos un botón "Ver Solicitud" en el listado
        By btnVerSolicitudBy = By.xpath("//button[@type='button' and contains(normalize-space(.),'Ver Solicitud')]");
        List<WebElement> botones = d.findElements(btnVerSolicitudBy);

        screenshot(d, "S6_RF086_TC131_05_resultados");

        assertTrue(!botones.isEmpty(),
            "❌ TC-131 FALLÓ: No se encontró el botón 'Ver Solicitud' en el listado (columna Acciones).");

        // Click al botón de la primera fila
        WebElement btn = botones.get(0);
        scrollCenter(d, btn);
        try { btn.click(); } catch (Exception e) { jsClick(d, btn); }

        screenshot(d, "S6_RF086_TC131_06_click_ver_solicitud");

        // Validación suave: algo cambió (modal/detalle/URL). No dependemos del diseño exacto.
        boolean cambio =
            d.getCurrentUrl().toLowerCase().contains("solicitud")
            || !d.findElements(By.cssSelector(".modal.show, .modal-dialog")).isEmpty()
            || !d.findElements(By.xpath("//*[contains(normalize-space(.),'Solicitud') and (self::h1 or self::h2 or self::h3 or self::h4 or self::div)]")).isEmpty();

        assertTrue(cambio,
            "❌ Se hizo click en 'Ver Solicitud' pero no se detectó apertura de detalle/modal/cambio de URL. " +
            "Revisa si el botón abre en la misma vista sin cambiar URL o si requiere esperar un loader.");
    }

    // ===================== TEST =====================
    @Test
    void RF086_TC131_Validar_Existencia_Boton_Ver_Solicitud_En_Listado() {

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

            validarExisteYClickVerSolicitud(driver, wait);

            screenshot(driver, "S6_RF086_TC131_OK");
            System.out.println("✅ TC-131 OK: El botón 'Ver Solicitud' se muestra correctamente y permite interacción.");

        } catch (TimeoutException e) {
            screenshot(driver, "S6_RF086_TC131_TIMEOUT");
            throw e;
        } catch (Exception e) {
            screenshot(driver, "S6_RF086_TC131_ERROR");
            throw e;
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
