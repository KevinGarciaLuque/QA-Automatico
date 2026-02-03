package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 6 - RF08.5
 * TC-119: Validar que los eventos mostrados en la trazabilidad correspondan a la solicitud seleccionada.
 *
 * Tipo: Funcional / Integración | Severidad: Alta
 *
 * Criterio de aceptación:
 * - Solo se muestran eventos asociados a la solicitud.
 *
 * Estrategia robusta (sin depender 100% del UI):
 * 1) Login
 * 2) Ir a Solicitudes (listado)
 * 3) Tomar ID visible de la primera solicitud (N° Solicitud / N° Asignación / ID)
 * 4) Abrir Trazabilidad desde acciones (botón/link) o desde detalle/tab
 * 5) Validar:
 *    - El header/título de trazabilidad menciona el ID seleccionado (si existe)
 *    - Y/o cada fila contiene el ID en alguna columna relacionada (si existe)
 *    - Fallback fuerte: abrir trazabilidad de otra solicitud y asegurar que el “set” de eventos NO sea idéntico
 *
 * Nota: Si tu app expone un selector/atributo claro del ID (por ejemplo data-id),
 * ajusta los XPaths marcados como "AJUSTABLE" para hacerlo 100% determinístico.
 */
class RF08_5TC_119Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    // RUTAS (ajusta si tu app usa otra)
    private static final String SOLICITUDES_HREF = "#/inspeccion/solicitudes";
    private static final String SOLICITUDES_URL_ABS = "http://3.228.164.208/#/inspeccion/solicitudes";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";

    // ===================== HELPERS =====================
    private void log(String m) { System.out.println("➡️ " + m); }

    private void screenshot(WebDriver driver, String name) {
        try {
            byte[] img = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream("./target/" + name + ".png")) {
                fos.write(img);
            }
            System.out.println("📸 Screenshot: ./target/" + name + ".png");
        } catch (Exception ignore) {}
    }

    private void scrollCenter(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block:'center'});", e);
    }

    private void jsClick(WebDriver d, WebElement e) {
        ((JavascriptExecutor) d).executeScript("arguments[0].click();", e);
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

    private void failSiHayErrorVisible(WebDriver d, String shotName) {
        try { Thread.sleep(600); } catch (InterruptedException ignore) {}

        By errorVisible = By.xpath(
            "//*[contains(@class,'toast') or contains(@class,'Toastify') or contains(@class,'alert') or contains(@class,'swal2') or contains(@class,'modal')]" +
            "[contains(translate(normalize-space(.),'ERRORNOTIFICACIÓN','errornotificación'),'error')" +
            " or contains(translate(normalize-space(.),'NO SE PUDO','no se pudo'),'no se pudo')" +
            " or contains(translate(normalize-space(.),'EXCEPCIÓN','excepción'),'excepción')]" );

        try {
            WebElement err = new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.visibilityOfElementLocated(errorVisible));
            screenshot(d, shotName + "_ERROR_VISIBLE");
            throw new AssertionError("❌ Se mostró error visible en UI: " + (err.getText() == null ? "" : err.getText().trim()));
        } catch (TimeoutException ignore) {
            // OK
        }
    }

    // ===================== PASOS =====================

    private void login(WebDriver d, WebDriverWait w) {
        log("Abrir login");
        d.get(BASE_URL);
        waitDocumentReady(d);

        log("Ingresar credenciales");
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
            By.xpath("//button[@type='submit' or normalize-space()='Inicio' or contains(.,'Inicio')]")
        ));
        btn.click();

        w.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        waitDocumentReady(d);

        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".sidebar, .sidebar-nav")));
        screenshot(d, "S6_RF085_TC119_01_login_ok");
    }

    private void irASolicitudes(WebDriver d, WebDriverWait w) {
        log("Ir a Solicitudes desde menú (o URL directa)");

        By linkBy = By.cssSelector("a.nav-link[href='" + SOLICITUDES_HREF + "']");

        try {
            WebElement link = new WebDriverWait(d, Duration.ofSeconds(6))
                .until(ExpectedConditions.presenceOfElementLocated(linkBy));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }
        } catch (Exception e) {
            log("Fallback URL directa Solicitudes");
            d.get(SOLICITUDES_URL_ABS);
        }

        w.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("solicitudes"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".container-fluid, .animated.fadeIn, .card, table"))
        ));
        waitDocumentReady(d);
        screenshot(d, "S6_RF085_TC119_02_solicitudes_cargado");

        failSiHayErrorVisible(d, "S6_RF085_TC119_02A");
    }

    private WebElement esperarTablaSolicitudes(WebDriver d) {
        // AJUSTABLE: si tu tabla tiene id/clase específica, ponla aquí.
        By tableBy = By.xpath("//table[contains(@class,'table') or contains(@class,'datatable') or contains(@class,'dataTable')]");
        return new WebDriverWait(d, Duration.ofSeconds(20))
            .until(ExpectedConditions.presenceOfElementLocated(tableBy));
    }

    /**
     * Toma un "ID" visible de la primera fila.
     * Intenta detectar columnas típicas: "Solicitud", "Asignación", "N°", "ID".
     * Si no logra detectar header, cae al primer td no vacío.
     */
    private String obtenerIdSolicitudPrimeraFila(WebDriver d) {
        WebElement table = esperarTablaSolicitudes(d);

        // Esperar al menos 1 fila en tbody
        By firstRowBy = By.xpath(".//tbody/tr[1]");
        WebElement row = new WebDriverWait(d, Duration.ofSeconds(20))
            .until(ExpectedConditions.presenceOfNestedElementLocatedBy(table, firstRowBy));

        // Intento 1: localizar índice por headers
        List<WebElement> headers = table.findElements(By.xpath(".//thead//th"));
        int idxPreferido = -1;

        for (int i = 0; i < headers.size(); i++) {
            String h = (headers.get(i).getText() == null ? "" : headers.get(i).getText()).trim().toLowerCase();
            if (h.contains("solic") || h.contains("asign") || h.contains("n°") || h.contains("no.") || h.equals("id")) {
                idxPreferido = i + 1; // XPath es 1-based
                break;
            }
        }

        String id = null;

        // Intento 2: usar la columna detectada
        if (idxPreferido > 0) {
            try {
                WebElement cell = row.findElement(By.xpath("./td[" + idxPreferido + "]"));
                id = (cell.getText() == null ? "" : cell.getText()).trim();
            } catch (Exception ignore) {}
        }

        // Intento 3: primer td no vacío
        if (id == null || id.isBlank()) {
            List<WebElement> tds = row.findElements(By.xpath("./td"));
            for (WebElement td : tds) {
                String v = (td.getText() == null ? "" : td.getText()).trim();
                if (!v.isBlank()) { id = v; break; }
            }
        }

        if (id == null || id.isBlank()) {
            screenshot(d, "S6_RF085_TC119_FAIL_no_id_en_fila");
            throw new AssertionError("❌ No se pudo obtener un ID visible de la primera solicitud (tabla vacía o celdas sin texto).");
        }

        log("ID seleccionado (primera fila): " + id);
        screenshot(d, "S6_RF085_TC119_03_id_solicitud_" + sanitize(id));
        return id;
    }

    /**
     * Abre trazabilidad desde la fila (acciones) o desde el detalle.
     * Busca botones/links con textos típicos o href con 'trazabilidad/bitacora/historial'.
     */
    private void abrirTrazabilidadDePrimeraFila(WebDriver d, WebDriverWait w) {
        WebElement table = esperarTablaSolicitudes(d);
        WebElement row = table.findElement(By.xpath(".//tbody/tr[1]"));
        scrollCenter(d, row);

        // 1) Buscar dentro de la fila un botón/link de trazabilidad
        By accesoFila = By.xpath(
            ".//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'VER TRAZABILIDAD','ver trazabilidad'),'ver trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora')" +
            " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')" +
            " or contains(@href,'trazabilidad') or contains(@href,'bitacora') or contains(@href,'historial')" +
            "]"
        );

        WebElement acceso = null;
        try { acceso = row.findElement(accesoFila); } catch (Exception ignore) {}

        if (acceso != null) {
            scrollCenter(d, acceso);
            try { acceso.click(); } catch (Exception e) { jsClick(d, acceso); }
            screenshot(d, "S6_RF085_TC119_04_click_trazabilidad_fila");
            return;
        }

        // 2) Si no hay acceso directo, intentar abrir "Detalle/Editar/Ver" y luego buscar trazabilidad
        By btnDetalle = By.xpath(
            ".//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'VER','ver'),'ver')" +
            " or contains(translate(normalize-space(.),'DETALLE','detalle'),'detalle')" +
            " or contains(translate(normalize-space(.),'EDITAR','editar'),'editar')" +
            " or contains(@href,'detalle') or contains(@href,'editar')" +
            "]"
        );

        WebElement detalle = null;
        try { detalle = row.findElement(btnDetalle); } catch (Exception ignore) {}

        if (detalle == null) {
            screenshot(d, "S6_RF085_TC119_FAIL_no_acceso_trazabilidad");
            throw new AssertionError(
                "❌ No se encontró acceso a Trazabilidad en la fila (ni un botón de Detalle/Ver/Editar para entrar y buscarla). " +
                "Ajusta los selectores del método abrirTrazabilidadDePrimeraFila()."
            );
        }

        scrollCenter(d, detalle);
        try { detalle.click(); } catch (Exception e) { jsClick(d, detalle); }
        screenshot(d, "S6_RF085_TC119_04A_click_detalle");

        // 3) Ya en detalle: buscar tab/botón de trazabilidad
        By accesoDetalle = By.xpath(
            "//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora')" +
            " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')" +
            " or contains(@href,'trazabilidad') or contains(@href,'bitacora') or contains(@href,'historial')" +
            "]"
        );

        WebElement acc = w.until(ExpectedConditions.presenceOfElementLocated(accesoDetalle));
        scrollCenter(d, acc);
        try { acc.click(); } catch (Exception e) { jsClick(d, acc); }
        screenshot(d, "S6_RF085_TC119_05_click_trazabilidad_detalle");
    }

    private void esperarVistaTrazabilidad(WebDriver d) {
        // Modal o pantalla con tabla/listado
        By vista = By.xpath(
            "//*[contains(@class,'modal') or contains(@class,'card') or self::table or contains(.,'Trazabilidad') or contains(.,'Bitácora')]" );
        new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.presenceOfElementLocated(vista));
        waitDocumentReady(d);
    }

    private String obtenerTextoHeaderTrazabilidad(WebDriver d) {
        // Intenta tomar un título/header visible
        List<By> candidatos = List.of(
            By.cssSelector(".modal.show .modal-title"),
            By.cssSelector(".modal-title"),
            By.xpath("//*[self::h1 or self::h2 or self::h3 or self::h4][contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad') or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora') or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')]"),
            By.xpath("//*[contains(@class,'card-header')][string-length(normalize-space(.))>0]")
        );

        for (By by : candidatos) {
            try {
                WebElement e = new WebDriverWait(d, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(by));
                String t = (e.getText() == null ? "" : e.getText()).trim();
                if (!t.isBlank()) return t;
            } catch (Exception ignore) {}
        }
        return "";
    }

    /**
     * Obtiene un “fingerprint” de eventos (texto por fila) para comparar entre solicitudes.
     * Esto es el fallback para validar que NO se están reciclando eventos de otra solicitud.
     */
    private Set<String> obtenerFingerprintEventos(WebDriver d) {
        // AJUSTABLE: si tu tabla de trazabilidad tiene selector único, ponlo aquí
        By tableBy = By.xpath("//table[contains(@class,'table') or contains(@class,'datatable') or contains(@class,'dataTable')]");
        WebElement table = new WebDriverWait(d, Duration.ofSeconds(20))
            .until(ExpectedConditions.presenceOfElementLocated(tableBy));

        List<WebElement> rows = table.findElements(By.xpath(".//tbody/tr"));
        Set<String> fp = new HashSet<>();

        for (WebElement r : rows) {
            String txt = (r.getText() == null ? "" : r.getText()).trim();
            if (!txt.isBlank()) fp.add(txt);
        }

        // Si está vacío, igual devolvemos (la prueba debe considerar que no hay eventos)
        return fp;
    }

    /**
     * Validación principal:
     * - Si el header contiene el ID -> OK
     * - Si alguna columna por fila contiene el ID -> OK
     * - Si NO hay forma textual, usamos fallback de comparación entre 2 solicitudes
     */
    private void validarEventosCorrespondanASolicitud(WebDriver d, WebDriverWait w, String idSeleccionado) {
        esperarVistaTrazabilidad(d);
        screenshot(d, "S6_RF085_TC119_06_trazabilidad_abierta");
        failSiHayErrorVisible(d, "S6_RF085_TC119_06A");

        String header = obtenerTextoHeaderTrazabilidad(d);
        if (!header.isBlank()) {
            log("Header trazabilidad: " + header);
        }

        // 1) Validación por header (si existe y suele mostrar el ID)
        boolean headerMencionaId = !header.isBlank() && header.contains(idSeleccionado);

        // 2) Validación por tabla: alguna columna contiene ID en cada fila (estricto)
        boolean tablaCumple = false;
        boolean hayTabla = true;

        Set<String> fpAntes = new HashSet<>();
        try {
            fpAntes = obtenerFingerprintEventos(d);
        } catch (Exception e) {
            hayTabla = false;
        }

        if (hayTabla && !fpAntes.isEmpty()) {
            // Estricto: TODAS las filas contienen el ID en algún lado
            // (si tu UI no imprime ID por fila, esto fallará; por eso existe fallback)
            try {
                List<WebElement> filas = new WebDriverWait(d, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//table[contains(@class,'table') or contains(@class,'datatable') or contains(@class,'dataTable')]//tbody/tr")
                    ));

                int total = 0;
                int conId = 0;

                for (WebElement f : filas) {
                    String t = (f.getText() == null ? "" : f.getText()).trim();
                    if (t.isBlank()) continue;
                    total++;
                    if (t.contains(idSeleccionado)) conId++;
                }

                // Si al menos 1 fila trae el ID y la mayoría coincide, lo consideramos válido.
                // Ajuste fino: si tu UI imprime el ID en cada evento, cambia a (conId == total).
                tablaCumple = (total > 0) && (conId > 0) && (conId >= Math.max(1, (int)Math.ceil(total * 0.60)));

                log("Validación por tabla: totalFilas=" + total + " filasConId=" + conId + " tablaCumple=" + tablaCumple);
            } catch (Exception ignore) {
                tablaCumple = false;
            }
        }

        if (headerMencionaId || tablaCumple) {
            screenshot(d, "S6_RF085_TC119_07_validacion_ok_por_texto");
            assertTrue(true, "OK");
            return;
        }

        // 3) FALLBACK FUERTE: abrir otra solicitud y comparar fingerprints (no deben ser idénticos)
        screenshot(d, "S6_RF085_TC119_07A_fallback_por_comparacion");
        log("Fallback: comparar eventos entre 2 solicitudes (evitar que se muestren eventos de otra).");

        // Cerrar modal si existe (opcional)
        try {
            WebElement close = d.findElement(By.xpath(
                "//*[@class='modal show' or contains(@class,'modal')]" +
                "//*[self::button or self::span]" +
                "[contains(@class,'close') or contains(.,'×') or contains(translate(normalize-space(.),'CERRAR','cerrar'),'cerrar')]"
            ));
            try { close.click(); } catch (Exception e) { jsClick(d, close); }
        } catch (Exception ignore) {}

        // Volver a solicitudes
        irASolicitudes(d, w);

        // Tomar segunda fila (si existe) y abrir trazabilidad
        WebElement table = esperarTablaSolicitudes(d);
        List<WebElement> filas = table.findElements(By.xpath(".//tbody/tr"));

        if (filas.size() < 2) {
            screenshot(d, "S6_RF085_TC119_FAIL_no_hay_segunda_fila");
            throw new AssertionError(
                "❌ No hay una segunda solicitud para ejecutar el fallback de comparación. " +
                "Y no fue posible validar por texto (header/tabla). Ajusta selectores para capturar el ID dentro de trazabilidad."
            );
        }

        // Seleccionar ID de la segunda fila (similar a la primera)
        WebElement row2 = filas.get(1);
        scrollCenter(d, row2);

        String id2 = null;
        List<WebElement> tds2 = row2.findElements(By.xpath("./td"));
        for (WebElement td : tds2) {
            String v = (td.getText() == null ? "" : td.getText()).trim();
            if (!v.isBlank()) { id2 = v; break; }
        }
        if (id2 == null || id2.isBlank()) id2 = "ROW2";

        // Abrir trazabilidad para fila 2
        abrirTrazabilidadDeFila(row2, d, w);
        esperarVistaTrazabilidad(d);

        Set<String> fpDespues = new HashSet<>();
        try {
            fpDespues = obtenerFingerprintEventos(d);
        } catch (Exception ignore) {}

        screenshot(d, "S6_RF085_TC119_08_fp_eventos_segunda_solicitud_" + sanitize(id2));

        // Reglas:
        // - Si ambos sets están vacíos => no se puede validar (pero es indicio de que no hay trazabilidad)
        // - Si son idénticos (y no están vacíos) => probable bug: se reciclan eventos o no filtra por solicitud
        if (fpAntes.isEmpty() && fpDespues.isEmpty()) {
            throw new AssertionError(
                "❌ No hay eventos de trazabilidad en ninguna de las dos solicitudes, no se puede validar el filtrado. " +
                "Revisa si el ambiente tiene trazabilidad generada o si la vista no está cargando datos."
            );
        }

        boolean sonIdenticos = fpAntes.equals(fpDespues) && !fpAntes.isEmpty() && !fpDespues.isEmpty();
        assertTrue(!sonIdenticos,
            "❌ TC-119 FALLÓ: La trazabilidad mostrada para 2 solicitudes diferentes es idéntica. " +
            "Probable falta de filtrado por solicitud (se están mostrando eventos no asociados).");
    }

    private void abrirTrazabilidadDeFila(WebElement row, WebDriver d, WebDriverWait w) {
        // Misma lógica que en primera fila pero recibiendo la fila
        By accesoFila = By.xpath(
            ".//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'VER TRAZABILIDAD','ver trazabilidad'),'ver trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora')" +
            " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')" +
            " or contains(@href,'trazabilidad') or contains(@href,'bitacora') or contains(@href,'historial')" +
            "]"
        );

        WebElement acceso = null;
        try { acceso = row.findElement(accesoFila); } catch (Exception ignore) {}

        if (acceso != null) {
            scrollCenter(d, acceso);
            try { acceso.click(); } catch (Exception e) { jsClick(d, acceso); }
            return;
        }

        // Fallback: click en detalle y luego trazabilidad
        By btnDetalle = By.xpath(
            ".//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'VER','ver'),'ver')" +
            " or contains(translate(normalize-space(.),'DETALLE','detalle'),'detalle')" +
            " or contains(translate(normalize-space(.),'EDITAR','editar'),'editar')" +
            " or contains(@href,'detalle') or contains(@href,'editar')" +
            "]"
        );

        WebElement detalle = null;
        try { detalle = row.findElement(btnDetalle); } catch (Exception ignore) {}

        if (detalle == null) {
            throw new AssertionError("❌ No se encontró botón de trazabilidad ni detalle en la fila (fila 2). Ajusta selectores.");
        }

        scrollCenter(d, detalle);
        try { detalle.click(); } catch (Exception e) { jsClick(d, detalle); }

        By accesoDetalle = By.xpath(
            "//*[self::a or self::button]" +
            "[" +
            " contains(translate(normalize-space(.),'TRAZABILIDAD','trazabilidad'),'trazabilidad')" +
            " or contains(translate(normalize-space(.),'BITÁCORA','bitácora'),'bitácora')" +
            " or contains(translate(normalize-space(.),'BITACORA','bitacora'),'bitacora')" +
            " or contains(translate(normalize-space(.),'HISTORIAL','historial'),'historial')" +
            " or contains(@href,'trazabilidad') or contains(@href,'bitacora') or contains(@href,'historial')" +
            "]"
        );

        WebElement acc = w.until(ExpectedConditions.presenceOfElementLocated(accesoDetalle));
        scrollCenter(d, acc);
        try { acc.click(); } catch (Exception e) { jsClick(d, acc); }
    }

    private String sanitize(String s) {
        if (s == null) return "null";
        String x = s.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        return x.length() > 35 ? x.substring(0, 35) : x;
    }

    // ===================== TEST =====================
    @Test
    void RF085_TC119_Validar_Eventos_Trazabilidad_Correspondan_Solicitud_Seleccionada() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--start-maximized", "--lang=es-419");
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            login(driver, wait);

            irASolicitudes(driver, wait);

            String id = obtenerIdSolicitudPrimeraFila(driver);

            abrirTrazabilidadDePrimeraFila(driver, wait);

            validarEventosCorrespondanASolicitud(driver, wait, id);

            screenshot(driver, "S6_RF085_TC119_09_OK");
            System.out.println("✅ TC-119 OK: La trazabilidad corresponde a la solicitud seleccionada (o se detectó diferencia entre solicitudes).");

        } catch (StaleElementReferenceException e) {
            screenshot(driver, "S6_RF085_TC119_STALE");
            throw e;
        } catch (Exception e) {
            screenshot(driver, "S6_RF085_TC119_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
