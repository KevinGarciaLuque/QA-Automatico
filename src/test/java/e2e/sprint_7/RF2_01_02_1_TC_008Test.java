package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
 * Sprint 7 - RF2.01.02.1
 * TC-008: Validar que el botón "Crear Solicitud" se inhabilite cuando existan
 * más de 10 solicitudes en estado "Iniciado/Iniciada".
 *
 * Regla esperada:
 * - Si total Iniciadas > 10  => Botón Crear Solicitud DESHABILITADO
 * - Si total Iniciadas <= 10 => Botón Crear Solicitud HABILITADO
 */
public class RF2_01_02_1_TC_008Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String IDENTIFICADOR = "05019001049230";
    private static final String USUARIO = "importador_inspeccion@yopmail.com";
    private static final String PASSWORD = "admin123";

    private static final String REGISTRO_HREF = "#/inspeccion/solicitudes";

    // Intento por value (si existe). Si no, cae a selección por texto.
    private static final String ESTADO_INICIADA_VALUE = "1793";
    private static final String ESTADO_INICIADA_TEXT_TOKEN = "iniciad"; // iniciada / iniciado

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

    private boolean isButtonEnabled(WebDriver driver, By by) {
        try {
            WebElement b = driver.findElement(by);
            if (!b.isDisplayed()) return false;
            if (!b.isEnabled()) return false;
            if (b.getAttribute("disabled") != null) return false;

            String aria = b.getAttribute("aria-disabled");
            if (aria != null && aria.trim().equalsIgnoreCase("true")) return false;

            String cls = b.getAttribute("class");
            return cls == null || !cls.toLowerCase().contains("disabled");
        } catch (Exception e) {
            return false;
        }
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

    private String optionsToString(Select sel) {
        return sel.getOptions().stream()
            .map(o -> "[" + safe(o.getAttribute("value")) + "] " + safe(o.getText()))
            .collect(Collectors.joining(" | "));
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    // ================== LISTADO HELPERS ==================
    private void seleccionarEstadoIniciada(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement estadoEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("estadoId")));

        // Esperar opciones cargadas (>1)
        wait.until(d -> {
            try {
                Select s = new Select(d.findElement(By.id("estadoId")));
                return s.getOptions() != null && s.getOptions().size() > 1;
            } catch (Exception e) { return false; }
        });

        Select sel = new Select(estadoEl);

        boolean selected = false;

        // 1) Por value si existe
        try {
            sel.selectByValue(ESTADO_INICIADA_VALUE);
            selected = true;
        } catch (Exception ignore) {}

        // 2) Fallback por texto
        if (!selected) {
            for (WebElement opt : sel.getOptions()) {
                String txt = safe(opt.getText()).toLowerCase();
                if (txt.contains(ESTADO_INICIADA_TEXT_TOKEN)) {
                    opt.click();
                    selected = true;
                    break;
                }
            }
        }

        if (!selected) {
            throw new NoSuchElementException(
                "No se pudo seleccionar Estado 'Iniciada/Iniciado'. Opciones: " + optionsToString(sel)
            );
        }

        Thread.sleep(250);
    }

    private void clickBuscar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        By buscarBy = By.xpath("//button[@type='button' and contains(@class,'btn-info') and contains(normalize-space(.),'Buscar')]");
        WebElement btnBuscar = wait.until(ExpectedConditions.elementToBeClickable(buscarBy));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
        jsClick(driver, btnBuscar);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 3);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody")));
    }

    private int contarFilasTabla(WebDriver driver) {
        List<WebElement> noData = driver.findElements(
            By.xpath("//table[contains(@class,'table')]//tbody//td[contains(.,'No hay datos para mostrar') or contains(.,'No hay datos')]")
        );
        if (noData != null && !noData.isEmpty()) return 0;

        List<WebElement> filas = driver.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));
        return (filas == null) ? 0 : filas.size();
    }

    private WebElement findFirstPresent(WebDriver driver, List<By> candidates) {
        for (By by : candidates) {
            try {
                List<WebElement> els = driver.findElements(by);
                if (els != null && !els.isEmpty()) return els.get(0);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private WebElement getLinkPagina(WebDriver driver, String numero) {
        List<By> candidates = Arrays.asList(
            By.xpath("//ul[contains(@class,'pagination')]//a[normalize-space()='" + numero + "']"),
            By.xpath("//ul[contains(@class,'pagination')]//button[normalize-space()='" + numero + "']"),
            By.xpath("//nav[@aria-label='pagination']//a[normalize-space()='" + numero + "']"),
            By.xpath("//nav[@aria-label='pagination']//button[normalize-space()='" + numero + "']")
        );
        return findFirstPresent(driver, candidates);
    }

    private boolean existePagina2Habilitada(WebDriver driver) {
        WebElement p2 = getLinkPagina(driver, "2");
        if (p2 == null) return false;
        if (!p2.isDisplayed()) return false;
        if (!p2.isEnabled()) return false;

        String cls = p2.getAttribute("class");
        if (cls != null && cls.toLowerCase().contains("disabled")) return false;

        try {
            WebElement li = p2.findElement(By.xpath("./ancestor::li[1]"));
            String clsLi = li.getAttribute("class");
            if (clsLi != null && clsLi.toLowerCase().contains("disabled")) return false;
        } catch (Exception ignore) {}

        return true;
    }

    private void irAPagina2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement p2 = getLinkPagina(driver, "2");
        if (p2 == null) throw new NoSuchElementException("No se encontró la paginación hacia la página 2.");

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", p2);
        Thread.sleep(150);
        jsClick(driver, p2);

        Thread.sleep(700);
        acceptIfAlertPresent(driver, 3);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody/tr")));
    }

    private By getCrearSolicitudButtonLocator() {
        return By.xpath("//button[@type='button' and (contains(normalize-space(.),'Crear Solicitud') or .//i[contains(@class,'fa-plus')])]");
    }

    // ================== TEST ==================
    @Test
    void RF201021_TC008_ValidarRegla_BotonCrearSolicitud_SegunTotalIniciadas() throws InterruptedException {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
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
            screenshot(driver, "S7_RF201021_TC008_01_login_ok");

            // ====== IR A LISTADO ======
            irARegistroSolicitudes(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC008_02_listado");

            // ====== FILTRAR ESTADO INICIADA ======
            seleccionarEstadoIniciada(driver, wait);
            screenshot(driver, "S7_RF201021_TC008_03_estado_iniciada_set");

            // ====== BUSCAR ======
            clickBuscar(driver, wait, js);
            screenshot(driver, "S7_RF201021_TC008_04_resultados_busqueda");

            // ====== CONTAR TOTAL APROXIMADO ======
            int filasPag1 = contarFilasTabla(driver);
            boolean hayPagina2 = existePagina2Habilitada(driver);

            int filasPag2 = 0;
            if (hayPagina2) {
                irAPagina2(driver, wait, js);
                filasPag2 = contarFilasTabla(driver);
                screenshot(driver, "S7_RF201021_TC008_05_pagina2_iniciadas");
            }

            int totalAprox = filasPag1 + filasPag2;
            boolean esperadoDeshabilitado = totalAprox > 10;

            // Volver a página 1 para validar como en tu UI (botón arriba)
            try {
                WebElement p1 = getLinkPagina(driver, "1");
                if (p1 != null && p1.isDisplayed()) {
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", p1);
                    jsClick(driver, p1);
                    Thread.sleep(600);
                }
            } catch (Exception ignore) {}

            // ====== VALIDAR BOTÓN SEGÚN REGLA ======
            By crearSolicitudBy = getCrearSolicitudButtonLocator();
            WebElement btnCrear = wait.until(ExpectedConditions.presenceOfElementLocated(crearSolicitudBy));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnCrear);
            Thread.sleep(300);

            boolean estaHabilitado = isButtonEnabled(driver, crearSolicitudBy);
            screenshot(driver, "S7_RF201021_TC008_06_crear_solicitud_estado");

            // Regla:
            // total >10 => NO habilitado
            // total <=10 => SI habilitado
            if (esperadoDeshabilitado) {
                assertTrue(!estaHabilitado,
                    "❌ Regla incumplida: Hay más de 10 'Iniciada' (total aprox=" + totalAprox + ") " +
                    "pero el botón 'Crear Solicitud' está HABILITADO.");
            } else {
                assertTrue(estaHabilitado,
                    "❌ Regla incumplida: Hay 10 o menos 'Iniciada' (total aprox=" + totalAprox + ") " +
                    "pero el botón 'Crear Solicitud' está DESHABILITADO.");
            }

            System.out.println("✅ TC-008 OK: total 'Iniciada' aprox=" + totalAprox +
                " => botón Crear Solicitud " + (estaHabilitado ? "HABILITADO" : "DESHABILITADO") +
                " (esperado: " + (esperadoDeshabilitado ? "DESHABILITADO" : "HABILITADO") + ").");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201021_TC008_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201021_TC008_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
