package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
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
 * TC-137: Validar que la pantalla "Trazabilidad" muestre información correctamente.
 *
 * Flujo:
 * 1) Login
 * 2) Ir a Consulta Parametrizada Solicitudes
 * 3) Estado de Solicitud = Subsanable
 * 4) Consultar
 * 5) Click en "Trazabilidad" en la primera fila (columna acciones)
 * 6) Validar que se muestren eventos asociados (tabla/listado con al menos 1 evento)
 */
class RF08_6TC_137Test {

    private static final String BASE_URL_LOGIN = "http://3.228.164.208/#/login";
    private static final String CONSULTA_PARAMETRIZADA_URL =
        "http://3.228.164.208/#/inspeccion/solicitudes/consulta-parametrizada";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // (Opcionales) si existen por label
    private static final String RTN_DELEGADO = "0512199300511";
    private static final String FECHA_DESDE = "01/01/2025";
    private static final String FECHA_HASTA = "31/12/2026";
    private static final String PUESTO_CUARENTENARIO_TEXTO = "El Florido";

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

    private void safeSelectByLabelIfPresent(WebDriver d, String labelText, String visibleTextContains) {
        By by = By.xpath("//label[contains(normalize-space(.),'" + labelText + "')]/following::*[self::select][1]");
        List<WebElement> els = d.findElements(by);
        if (els.isEmpty()) return;

        WebElement selectEl = els.get(0);
        scrollCenter(d, selectEl);

        Select sel = new Select(selectEl);
        for (WebElement opt : sel.getOptions()) {
            String t = (opt.getText() == null ? "" : opt.getText()).trim();
            if (!t.isEmpty() && t.toLowerCase().contains(visibleTextContains.toLowerCase())) {
                sel.selectByVisibleText(opt.getText());
                return;
            }
        }
    }

    /** Selecciona Estado=Subsanable buscando el <select> que tenga esa opción. */
    private void seleccionarEstadoSubsanable(WebDriver d) {
        By selectEstado = By.xpath("//select[.//option[contains(normalize-space(.),'Subsanable')]]");
        WebElement selEl = new WebDriverWait(d, Duration.ofSeconds(12))
            .until(ExpectedConditions.presenceOfElementLocated(selectEstado));

        scrollCenter(d, selEl);
        new Select(selEl).selectByVisibleText("Subsanable");
    }

    private void clickConsultar(WebDriver d, WebDriverWait w) {
        By btnConsultar = By.xpath("//*[self::button or self::a][contains(normalize-space(.),'Consultar')]");
        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(btnConsultar));
        scrollCenter(d, btn);
        try { btn.click(); } catch (Exception e) { jsClick(d, btn); }
    }

    private void esperarAlMenosUnaFila(WebDriver d) {
        By firstRow = By.xpath("//table//tbody/tr[1]");
        new WebDriverWait(d, Duration.ofSeconds(25))
            .until(ExpectedConditions.presenceOfElementLocated(firstRow));
    }

    private void clickConReintento(WebDriver d, By by, int intentos) {
        RuntimeException last = null;

        for (int i = 1; i <= intentos; i++) {
            try {
                WebElement el = new WebDriverWait(d, Duration.ofSeconds(12))
                    .until(ExpectedConditions.elementToBeClickable(by));
                scrollCenter(d, el);
                try { el.click(); } catch (Exception e) { jsClick(d, el); }
                return;
            } catch (StaleElementReferenceException se) {
                last = se;
                try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (RuntimeException re) {
                last = re;
                try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new AssertionError("❌ No se pudo hacer click luego de " + intentos + " intentos.", last);
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
        screenshot(d, "S6_RF086_TC137_01_login_ok");
    }

    private void irAConsultaParametrizada(WebDriver d, WebDriverWait w) {
        d.get(CONSULTA_PARAMETRIZADA_URL);

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("consulta-parametrizada"),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.),'Filtros de Búsqueda')]"))
        ));

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC137_02_consulta_parametrizada");
    }

    private void aplicarFiltrosYConsultar(WebDriver d, WebDriverWait w) {
        // Opcionales (si existen por label)
        safeTypeIfPresent(d, "Fecha desde", FECHA_DESDE);
        safeTypeIfPresent(d, "Fecha hasta", FECHA_HASTA);
        safeSelectByLabelIfPresent(d, "Puesto Cuarentenario", PUESTO_CUARENTENARIO_TEXTO);
        safeTypeIfPresent(d, "RTN Delegado", RTN_DELEGADO);

        // Requerido: Estado = Subsanable
        seleccionarEstadoSubsanable(d);

        screenshot(d, "S6_RF086_TC137_03_filtros_listos");

        clickConsultar(d, w);

        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC137_04_click_consultar");
    }

    private void abrirTrazabilidadYValidarEventos(WebDriver d, WebDriverWait w) {
        esperarAlMenosUnaFila(d);
        screenshot(d, "S6_RF086_TC137_05_resultados");

        // (Opcional) capturar el "ID" de la solicitud para trazabilidad (sirve como referencia en logs)
        try {
            String idSolicitud = d.findElement(By.xpath("//table//tbody/tr[1]/td[1]")).getText().trim();
            if (!idSolicitud.isBlank()) {
                System.out.println("🧾 Referencia: primera fila ID/col1 = " + idSolicitud);
            }
        } catch (Exception ignore) {}

        // Botón/enlace "Trazabilidad" solo en primera fila, columna acciones (última td)
        By trazabilidadEnPrimeraFila = By.xpath(
            "//table//tbody/tr[1]//td[last()]//*[self::button or self::a]" +
            "[contains(normalize-space(.),'Trazabilidad') " +
            " or contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')]"
        );

        // Esperar que exista
        try {
            new WebDriverWait(d, Duration.ofSeconds(12))
                .until(ExpectedConditions.presenceOfElementLocated(trazabilidadEnPrimeraFila));
        } catch (TimeoutException te) {
            screenshot(d, "S6_RF086_TC137_FAIL_no_btn_trazabilidad");
            throw new AssertionError("❌ TC-137 FALLÓ: No se encontró 'Trazabilidad' en la primera fila (acciones).", te);
        }

        assertTrue(!d.findElements(trazabilidadEnPrimeraFila).isEmpty(),
            "❌ TC-137 FALLÓ: No se encontró el botón/enlace 'Trazabilidad' en el listado (columna Acciones).");

        // Click robusto
        clickConReintento(d, trazabilidadEnPrimeraFila, 3);
        screenshot(d, "S6_RF086_TC137_06_click_trazabilidad");

        // Esperar que cargue la vista/modal de trazabilidad
        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("trazabilidad"),
            ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//*[contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')]"
            ))
        ));
        waitDocumentReady(d);
        screenshot(d, "S6_RF086_TC137_07_trazabilidad_abierta");

        // Validar que se muestren eventos (tabla/listado con al menos 1 fila/elemento)
        // 1) Preferido: tabla con encabezados tipo Evento/Acción/Descripción/Fecha
        By filasEventosEnTabla = By.xpath(
            "//table[" +
                ".//th[contains(translate(normalize-space(.),'EVENTO','evento'),'evento')]" +
                " or .//th[contains(translate(normalize-space(.),'ACCIÓN','acción'),'acción')]" +
                " or .//th[contains(translate(normalize-space(.),'ACCION','accion'),'accion')]" +
                " or .//th[contains(translate(normalize-space(.),'DESCRIPCIÓN','descripción'),'descripción')]" +
                " or .//th[contains(translate(normalize-space(.),'DESCRIPCION','descripcion'),'descripcion')]" +
                " or .//th[contains(translate(normalize-space(.),'FECHA','fecha'),'fecha')]" +
            "]//tbody/tr"
        );

        boolean hayEventos = false;

        try {
            new WebDriverWait(d, Duration.ofSeconds(18))
                .until(ExpectedConditions.presenceOfElementLocated(filasEventosEnTabla));
            hayEventos = !d.findElements(filasEventosEnTabla).isEmpty();
        } catch (Exception ignore) {}

        // 2) Fallback: listado tipo <li> (si la UI no usa tabla)
        if (!hayEventos) {
            By itemsListado = By.xpath(
                "//*[contains(@class,'modal') or contains(@class,'container') or contains(@class,'content') or self::body]" +
                "//*[self::li or contains(@class,'list-group-item') or contains(@class,'timeline-item')]" +
                "[normalize-space(.)!='' and string-length(normalize-space(.))>5]"
            );
            try {
                new WebDriverWait(d, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(itemsListado));
                hayEventos = !d.findElements(itemsListado).isEmpty();
            } catch (Exception ignore) {}
        }

        if (!hayEventos) {
            screenshot(d, "S6_RF086_TC137_FAIL_sin_eventos");
            throw new AssertionError("❌ TC-137 FALLÓ: Se abrió 'Trazabilidad' pero no se encontró listado de eventos (tabla/lista vacía o inexistente).");
        }

        screenshot(d, "S6_RF086_TC137_08_eventos_ok");
        System.out.println("✅ TC-137 OK: 'Trazabilidad' muestra eventos asociados a la solicitud.");
    }

    // ===================== TEST =====================
    @Test
    void RF086_TC137_Validar_Pantalla_Trazabilidad_Muestre_Info_Correctamente() {

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
            abrirTrazabilidadYValidarEventos(driver, wait);

            screenshot(driver, "S6_RF086_TC137_OK");

        } catch (TimeoutException e) {
            screenshot(driver, "S6_RF086_TC137_TIMEOUT");
            throw e;
        } catch (Exception e) {
            screenshot(driver, "S6_RF086_TC137_ERROR");
            throw e;
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
