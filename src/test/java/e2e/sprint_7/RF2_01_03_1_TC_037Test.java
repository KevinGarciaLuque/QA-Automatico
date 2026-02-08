package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Sprint 7 - RF2.01.03.1
 * TC-037: Validar generación de evento al recepcionar la solicitud (Trazabilidad).
 *
 * Criterio Alta:
 * - Se registra el evento correspondiente en trazabilidad.
 *
 * Nota de estabilidad:
 * - Se usan 2 sesiones: una para Recepcionar (y validar estado), otra para Trazabilidad.
 */
public class RF2_01_03_1_TC_037Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String HOST = "http://3.228.164.208/";

    // Credenciales (Recepción)
    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    // Asignación solicitada
    private static final String NUM_ASIGNACION = "GLS-EXP-2026-02-00024";

    // ================== HELPERS ==================
    private WebDriver buildDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);
        return new ChromeDriver(options);
    }

    private WebDriverWait buildWait(WebDriver driver, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

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
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
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

    private void asegurarDelegadoOFF(WebDriver driver, JavascriptExecutor js) {
        try {
            WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
            if (delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }
        } catch (Exception ignore) {}
    }

    private WebElement waitVisible(WebDriverWait wait, By by) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(by)));
    }

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(150);
    }

    private void setInputValueJS(JavascriptExecutor js, WebElement input, String value) {
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            input, value
        );
    }

    private String fechaHoy() {
        return LocalDate.now().toString(); // yyyy-MM-dd
    }

    private String horaAhoraHHmm() {
        return LocalTime.now().withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String lowerXpathContains(String text) {
        return "contains(translate(normalize-space(.),'ÁÉÍÓÚÜÑABCDEFGHIJKLMNOPQRSTUVWXYZ','áéíóúüñabcdefghijklmnopqrstuvwxyz'),'" +
                text.toLowerCase() + "')";
    }

    // ================== LOGIN ==================
    private void login(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String shotPrefix) throws InterruptedException {
        driver.get(BASE_URL);

        WebElement identificador = waitVisible(wait,
            By.cssSelector("input[type='text'], input#identificador, input[name='identificador']"));
        identificador.clear();
        identificador.sendKeys(IDENTIFICADOR);

        asegurarDelegadoOFF(driver, js);
        Thread.sleep(150);

        WebElement password = waitVisible(wait,
            By.cssSelector("input[type='password'], input#password, input[name='password']"));
        password.clear();
        password.sendKeys(PASSWORD);

        WebElement botonInicio = waitVisible(wait,
            By.xpath("//button[normalize-space()='Inicio' or @type='submit']"));
        botonInicio.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.not(ExpectedConditions.urlContains("/login")),
            ExpectedConditions.not(ExpectedConditions.urlContains("#/login")),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-link"))
        ));
        acceptIfAlertPresent(driver, 3);
        screenshot(driver, shotPrefix + "_01_login_ok");
    }

    // ================== RECEPCIÓN (Sesión A) ==================
    private void irARecepcion(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Click menú si existe, si no: fallback directo
        try {
            WebElement link = waitVisible(wait, By.cssSelector("a.nav-link[href='" + RECEPCION_HREF + "']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
            Thread.sleep(120);
            jsClick(driver, link);
        } catch (Exception e) {
            driver.get(HOST + RECEPCION_HREF);
        }

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/inspeccion/solicitudes/recepcion"),
            ExpectedConditions.urlContains("#/inspeccion/solicitudes/recepcion")
        ));
        acceptIfAlertPresent(driver, 2);
    }

    private void buscarEnRecepcion(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numAsignacion) throws InterruptedException {
        WebElement inputAsignacion = waitVisible(wait,
            By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
        WebElement btnBuscar = waitVisible(wait,
            By.xpath("//button[@type='button' and contains(@class,'btn-success') and " + lowerXpathContains("buscar") + "]"));

        clearAndType(inputAsignacion, numAsignacion);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        Thread.sleep(120);
        jsClick(driver, btnBuscar);

        Thread.sleep(600);
        acceptIfAlertPresent(driver, 2);

        waitVisible(wait, By.cssSelector("table.table"));
    }

    private WebElement obtenerFilaPorAsignacionRecepcion(WebDriverWait wait, String numAsignacion) {
        By filaBy = By.xpath(
            "//table[contains(@class,'table')]//tbody//tr[" +
              ".//*[contains(normalize-space(.),'" + numAsignacion + "')]" +
            "]"
        );
        return waitVisible(wait, filaBy);
    }

    private void recepcionarYValidarEstado(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numAsignacion) throws InterruptedException {
        irARecepcion(driver, wait, js);
        screenshot(driver, "S7_RF201031_TC037A_02_modulo_recepcion");

        buscarEnRecepcion(driver, wait, js, numAsignacion);
        screenshot(driver, "S7_RF201031_TC037A_03_post_buscar");

        WebElement fila = obtenerFilaPorAsignacionRecepcion(wait, numAsignacion);

        WebElement estadoAntesEl = fila.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
        String estadoAntes = safe(estadoAntesEl.getText()).toLowerCase();
        assertTrue(estadoAntes.contains("enviad"),
            "❌ Se esperaba estado 'Enviada'. Actual: '" + safe(estadoAntesEl.getText()) + "'");
        screenshot(driver, "S7_RF201031_TC037A_04_estado_enviada_ok");

        WebElement btnRecepcionar = fila.findElement(By.xpath(
            ".//button[@type='button' and contains(@class,'btn') and contains(.,'Recepcionar')]"
        ));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
        Thread.sleep(120);
        jsClick(driver, btnRecepcionar);

        Thread.sleep(350);
        acceptIfAlertPresent(driver, 2);
        screenshot(driver, "S7_RF201031_TC037A_05_modal_abierto");

        // Modal inputs
        WebElement inputFecha = waitVisible(wait,
            By.cssSelector("div.modal.show input[type='date'].form-control, div[role='dialog'] input[type='date'].form-control"));
        WebElement inputHora  = waitVisible(wait,
            By.cssSelector("div.modal.show input[type='time'].form-control, div[role='dialog'] input[type='time'].form-control"));

        String fecha = fechaHoy();
        String hora  = horaAhoraHHmm();

        setInputValueJS(js, inputFecha, fecha);
        setInputValueJS(js, inputHora,  hora);

        assertTrue(fecha.equals(safe(inputFecha.getAttribute("value"))),
            "❌ No se pudo setear Fecha. Esperado=" + fecha + " Actual=" + safe(inputFecha.getAttribute("value")));
        assertTrue(hora.equals(safe(inputHora.getAttribute("value"))),
            "❌ No se pudo setear Hora. Esperado=" + hora + " Actual=" + safe(inputHora.getAttribute("value")));

        screenshot(driver, "S7_RF201031_TC037A_06_fecha_hora_set");

        WebElement btnConfirmar = waitVisible(wait, By.xpath(
            "//div[contains(@class,'modal') and contains(@class,'show')]//button[normalize-space()='Confirmar'] | " +
            "//div[@role='dialog']//button[normalize-space()='Confirmar']"
        ));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnConfirmar);
        Thread.sleep(120);
        jsClick(driver, btnConfirmar);

        Thread.sleep(900);
        acceptIfAlertPresent(driver, 2);
        screenshot(driver, "S7_RF201031_TC037A_07_post_confirmar");

        // Re-buscar y validar estado "Recepcionada"
        buscarEnRecepcion(driver, wait, js, numAsignacion);

        WebElement filaDespues = obtenerFilaPorAsignacionRecepcion(wait, numAsignacion);
        WebElement estadoDespuesEl = filaDespues.findElement(By.xpath(".//td[4]//span[contains(@class,'badge')]"));
        String estadoDespues = safe(estadoDespuesEl.getText()).toLowerCase();

        assertTrue(estadoDespues.contains("recepcionad"),
            "❌ Se esperaba estado 'Recepcionada/Recepcionado'. Actual: '" + safe(estadoDespuesEl.getText()) + "'");
        screenshot(driver, "S7_RF201031_TC037A_08_estado_recepcionada_ok");
    }

    // ================== TRAZABILIDAD (Sesión B) ==================
    private void irATrazabilidad(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        // Intento 1: click por texto
        By linkBy = By.xpath("//a[contains(@class,'nav-link') and " + lowerXpathContains("trazabilidad") + "]");
        List<WebElement> links = driver.findElements(linkBy);
        if (!links.isEmpty()) {
            WebElement link = links.get(0);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
            Thread.sleep(120);
            jsClick(driver, link);
        } else {
            // Intento 2: rutas candidatas
            List<String> candidates = Arrays.asList(
                "#/inspeccion/trazabilidad",
                "#/dashboard/trazabilidad",
                "#/inspeccion/dashboard/trazabilidad",
                "#/trazabilidad"
            );
            boolean opened = false;
            for (String href : candidates) {
                try {
                    driver.get(HOST + href);
                    Thread.sleep(250);
                    if (safe(driver.getCurrentUrl()).toLowerCase().contains("traz")) {
                        opened = true;
                        break;
                    }
                } catch (Exception ignore) {}
            }
            if (!opened) driver.get(HOST + "#/dashboard/trazabilidad");
        }

        // Espera mínima: que exista texto trazabilidad o url contenga traz
        wait.until(d -> {
            try {
                String url = safe(d.getCurrentUrl()).toLowerCase();
                boolean urlOk = url.contains("traz");
                boolean textoOk = !d.findElements(By.xpath("//*[" + lowerXpathContains("trazabilidad") + "]")).isEmpty();
                return urlOk || textoOk;
            } catch (NoSuchSessionException ns) {
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        acceptIfAlertPresent(driver, 2);
    }

    private void buscarEnTrazabilidad(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String numAsignacion) throws InterruptedException {
        // Detectar un input visible (muy flexible)
        WebElement input = null;
        List<By> inputCandidates = Arrays.asList(
            By.xpath("//label[" + lowerXpathContains("número de asignación") + "]/following::input[1]"),
            By.xpath("//label[" + lowerXpathContains("numero de asignacion") + "]/following::input[1]"),
            By.xpath("//input[contains(@placeholder,'Asign') or contains(@placeholder,'asig') or contains(@placeholder,'Ej')]"),
            By.cssSelector("input[type='text']")
        );

        for (By by : inputCandidates) {
            List<WebElement> els = driver.findElements(by);
            for (WebElement e : els) {
                try {
                    if (e.isDisplayed() && e.isEnabled()) { input = e; break; }
                } catch (Exception ignore) {}
            }
            if (input != null) break;
        }
        assertTrue(input != null, "❌ No se encontró input de búsqueda en Trazabilidad.");

        // Botón buscar
        WebElement btnBuscar = waitVisible(wait, By.xpath("//button[@type='button' and " + lowerXpathContains("buscar") + "]"));

        clearAndType(input, numAsignacion);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        Thread.sleep(120);
        jsClick(driver, btnBuscar);

        Thread.sleep(800);
        acceptIfAlertPresent(driver, 2);
    }

    private void validarEventoRecepcionEnTrazabilidad(WebDriver driver, String numAsignacion) {
        // Espera (integración): hasta 30s a que aparezca asignación + token "recepcion"
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(30));

        w.until(d -> {
            try {
                String body = safe(d.findElement(By.tagName("body")).getText()).toLowerCase();
                return body.contains(numAsignacion.toLowerCase()) && body.contains("recepcion");
            } catch (StaleElementReferenceException se) {
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        // Validación fuerte: si hay tabla, intentar validar en la misma fila
        List<WebElement> filas = driver.findElements(By.xpath("//tr[.//*[contains(normalize-space(.),'" + numAsignacion + "')]]"));
        if (!filas.isEmpty()) {
            String rowText = safe(filas.get(0).getText()).toLowerCase();
            assertTrue(rowText.contains("recepcion"),
                "❌ Se encontró la asignación, pero la fila no refleja evento/acción de recepción. Fila: " + rowText);
        }
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC037_GenerarEventoTrazabilidad_Recepcionar() throws InterruptedException {

        WebDriverManager.chromedriver().setup();

        WebDriver driverA = null;
        WebDriver driverB = null;

        try {
            // ========= SESIÓN A: Recepcionar y validar estado =========
            driverA = buildDriver();
            WebDriverWait waitA = buildWait(driverA, 90);
            JavascriptExecutor jsA = (JavascriptExecutor) driverA;

            login(driverA, waitA, jsA, "S7_RF201031_TC037A");
            recepcionarYValidarEstado(driverA, waitA, jsA, NUM_ASIGNACION);

            // Cerramos sesión A para evitar caídas al cambiar de módulo
            try { driverA.quit(); } catch (Exception ignore) {}
            driverA = null;

            // ========= SESIÓN B: Trazabilidad y validar evento =========
            driverB = buildDriver();
            WebDriverWait waitB = buildWait(driverB, 90);
            JavascriptExecutor jsB = (JavascriptExecutor) driverB;

            login(driverB, waitB, jsB, "S7_RF201031_TC037B");
            irATrazabilidad(driverB, waitB, jsB);
            screenshot(driverB, "S7_RF201031_TC037B_02_trazabilidad_abierta");

            buscarEnTrazabilidad(driverB, waitB, jsB, NUM_ASIGNACION);
            screenshot(driverB, "S7_RF201031_TC037B_03_post_buscar");

            validarEventoRecepcionEnTrazabilidad(driverB, NUM_ASIGNACION);
            screenshot(driverB, "S7_RF201031_TC037B_04_evento_recepcion_ok");

            System.out.println("✅ TC-037 OK: Recepción + Evento en Trazabilidad para " + NUM_ASIGNACION);

        } catch (TimeoutException te) {
            if (driverA != null) screenshot(driverA, "S7_RF201031_TC037_TIMEOUT_A");
            if (driverB != null) screenshot(driverB, "S7_RF201031_TC037_TIMEOUT_B");
            throw te;

        } catch (NoSuchElementException ne) {
            if (driverA != null) screenshot(driverA, "S7_RF201031_TC037_NO_SUCH_ELEMENT_A");
            if (driverB != null) screenshot(driverB, "S7_RF201031_TC037_NO_SUCH_ELEMENT_B");
            throw ne;

        } finally {
            try { if (driverA != null) driverA.quit(); } catch (Exception ignore) {}
            try { if (driverB != null) driverB.quit(); } catch (Exception ignore) {}
        }
    }
}
