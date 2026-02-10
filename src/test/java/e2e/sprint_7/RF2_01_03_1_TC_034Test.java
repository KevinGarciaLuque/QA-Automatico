package e2e.sprint_7;

import java.io.FileOutputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
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
 * Sprint 7 - RF2.01.03.1
 * TC-034: Validar obligatoriedad de fecha y hora en el modal de recepción.
 *
 * Flujo:
 * - Abrir modal "Registrar Recepción"
 * - Borrar fecha y hora (REAL, compatible con React controlado)
 * - Click Confirmar
 * - Validar toast warning: "Fecha y hora son obligatorias"
 * - FIN (NO cerrar modal, NO continuar, NO cancelar)
 */
public class RF2_01_03_1_TC_034Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";

    private static final String IDENTIFICADOR = "jefecuarentena@yopmail.com";
    private static final String PASSWORD = "cuarentena1";

    private static final String RECEPCION_HREF = "#/inspeccion/solicitudes/recepcion";

    private static final String DEFAULT_NUM_ASIGNACION = "PCO-IMP-2026-02-00005";
    private static final String TOAST_MSG = "Fecha y hora son obligatorias";

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

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private WebElement waitVisible(WebDriverWait wait, By by) {
        return wait.until(ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(by)));
    }

    private void asegurarDelegadoOFF(WebDriver driver, JavascriptExecutor js) {
        try {
            WebElement delegadoSwitch = driver.findElement(By.id("esUsuarioDelegado"));
            if (delegadoSwitch.isSelected()) {
                js.executeScript("arguments[0].click();", delegadoSwitch);
            }
        } catch (Exception ignore) {}
    }

    private void clearAndType(WebElement el, String value) throws InterruptedException {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.BACK_SPACE);
        Thread.sleep(80);
        el.sendKeys(value);
        Thread.sleep(150);
    }

    /** Lee el valor real del input (DOM property), que es lo que React usa. */
    private String domValue(WebElement el) {
        try {
            String v = el.getDomProperty("value");
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            // fallback
            String v = el.getAttribute("value");
            return v == null ? "" : v.trim();
        }
    }

    /**
     * Set value compatible con inputs controlados en React:
     * usa el setter nativo + dispara input/change.
     */
    private void reactSetValue(JavascriptExecutor js, WebElement input, String value) {
        js.executeScript(
            "const el = arguments[0];" +
            "const val = arguments[1];" +
            "const proto = Object.getPrototypeOf(el);" +
            "const desc = Object.getOwnPropertyDescriptor(proto, 'value') || " +
            "             Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');" +
            "if (desc && desc.set) { desc.set.call(el, val); } else { el.value = val; }" +
            "el.dispatchEvent(new Event('input', { bubbles: true }));" +
            "el.dispatchEvent(new Event('change', { bubbles: true }));",
            input, value
        );
    }

    private String innerText(WebDriver driver, WebElement el) {
        try {
            Object txt = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].innerText || arguments[0].textContent || '';", el
            );
            return txt == null ? "" : txt.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private WebElement esperarToastObligatorio(WebDriver driver, int seconds, String msg) {
        final String msgLower = msg.toLowerCase();

        // Exacto como tu DOM: <div class="notification notification-warning">...</div>
        By toastBy = By.cssSelector(".notification-container .notification.notification-warning");

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        return w.until(d -> {
            try {
                for (WebElement t : d.findElements(toastBy)) {
                    try {
                        if (!t.isDisplayed()) continue;
                        String txt = safe(innerText(d, t)).toLowerCase();
                        if (txt.contains(msgLower)) return t;
                    } catch (StaleElementReferenceException ignore) {}
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    // ================== TEST ==================
    @Test
    void RF201031_TC034_ObligatoriedadFechaHora_MuestraToast() throws InterruptedException {

        final String numAsignacion = System.getProperty("numAsignacion", DEFAULT_NUM_ASIGNACION);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--lang=es-419");
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            driver.get(BASE_URL);

            // ====== LOGIN (SIN DELEGADO) ======
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
            screenshot(driver, "S7_RF201031_TC034_01_login_ok");

            // ====== IR A RECEPCIÓN ======
            WebElement linkRecepcion = waitVisible(wait, By.cssSelector("a.nav-link[href='" + RECEPCION_HREF + "']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", linkRecepcion);
            Thread.sleep(150);
            jsClick(driver, linkRecepcion);

            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/inspeccion/solicitudes/recepcion"),
                ExpectedConditions.urlContains("#/inspeccion/solicitudes/recepcion")
            ));
            acceptIfAlertPresent(driver, 3);
            screenshot(driver, "S7_RF201031_TC034_02_modulo_recepcion");

            // ====== BUSCAR POR ASIGNACIÓN ======
            waitVisible(wait, By.cssSelector("table.table"));

            WebElement inputAsignacion = waitVisible(wait,
                By.xpath("//label[normalize-space()='Número de Asignación']/following::input[1]"));
            WebElement btnBuscar = waitVisible(wait,
                By.xpath("//button[@type='button' and contains(@class,'btn-success') and contains(.,'Buscar')]"));

            clearAndType(inputAsignacion, numAsignacion);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnBuscar);
            Thread.sleep(120);
            jsClick(driver, btnBuscar);

            Thread.sleep(600);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC034_03_post_buscar");

            // ====== ABRIR MODAL (Recepcionar) ======
            By filaBy = By.xpath("//table[contains(@class,'table')]//tbody//tr[.//*[contains(normalize-space(.),'" + numAsignacion + "')]]");
            WebElement fila = waitVisible(wait, filaBy);

            WebElement btnRecepcionar = fila.findElement(By.xpath(
                ".//button[@type='button' and contains(@class,'btn') and contains(.,'Recepcionar')]"
            ));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btnRecepcionar);
            Thread.sleep(120);
            jsClick(driver, btnRecepcionar);

            Thread.sleep(350);
            acceptIfAlertPresent(driver, 2);
            screenshot(driver, "S7_RF201031_TC034_04_modal_abierto");

            // ====== BORRAR FECHA/HORA (REAL) ======
            WebElement inputFechaModal = waitVisible(wait,
                By.cssSelector("div.modal.show input[type='date'].form-control, div[role='dialog'] input[type='date'].form-control"));
            WebElement inputHoraModal  = waitVisible(wait,
                By.cssSelector("div.modal.show input[type='time'].form-control, div[role='dialog'] input[type='time'].form-control"));

            // 1) Set vacío con setter nativo (React friendly)
            reactSetValue(js, inputFechaModal, "");
            reactSetValue(js, inputHoraModal, "");

            // 2) Blur para que React procese (muy importante)
            inputHoraModal.sendKeys(Keys.TAB);
            Thread.sleep(150);

            // 3) Verificación por DOM property (lo que realmente usa React)
            String vFecha = domValue(inputFechaModal);
            String vHora  = domValue(inputHoraModal);

            // Si aún no quedaron vacíos, reintento una vez (algunos inputs re-escriben)
            if (!vFecha.isEmpty() || !vHora.isEmpty()) {
                reactSetValue(js, inputFechaModal, "");
                reactSetValue(js, inputHoraModal, "");
                inputHoraModal.sendKeys(Keys.TAB);
                Thread.sleep(150);
                vFecha = domValue(inputFechaModal);
                vHora  = domValue(inputHoraModal);
            }

            assertTrue(vFecha.isEmpty(), "❌ La Fecha NO quedó vacía (DOM value). Actual: '" + vFecha + "'");
            assertTrue(vHora.isEmpty(),  "❌ La Hora NO quedó vacía (DOM value). Actual: '" + vHora + "'");
            screenshot(driver, "S7_RF201031_TC034_05_fecha_hora_vacias");

            // ====== CONFIRMAR (dispara el toast) ======
            WebElement btnConfirmar = waitVisible(wait, By.xpath(
                "//div[contains(@class,'modal') and contains(@class,'show')]//button[normalize-space()='Confirmar'] | " +
                "//div[@role='dialog']//button[normalize-space()='Confirmar']"
            ));

            // Click sin scrollear agresivo (para que se vea como tu captura)
            jsClick(driver, btnConfirmar);
            screenshot(driver, "S7_RF201031_TC034_06_click_confirmar");

            // ====== VALIDAR TOAST (OBLIGATORIO) ======
            WebElement toast = esperarToastObligatorio(driver, 15, TOAST_MSG);

            // Pequeña pausa para que el screenshot salga igual a tu imagen
            Thread.sleep(200);

            String toastText = safe(innerText(driver, toast));
            assertTrue(toastText.toLowerCase().contains(TOAST_MSG.toLowerCase()),
                "❌ No se detectó el toast esperado. Texto actual: " + toastText);

            screenshot(driver, "S7_RF201031_TC034_07_toast_ok");

            // FIN: NO hacer nada más (no continuar, no cerrar, no cancelar)
            System.out.println("✅ TC-034 OK: Se mostró el toast: '" + TOAST_MSG + "'");

        } catch (TimeoutException te) {
            screenshot(driver, "S7_RF201031_TC034_TIMEOUT");
            throw te;

        } catch (NoSuchElementException ne) {
            screenshot(driver, "S7_RF201031_TC034_NO_SUCH_ELEMENT");
            throw ne;

        } finally {
            // driver.quit();
        }
    }
}
