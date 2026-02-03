package e2e.sprint_6;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
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
 * Sprint 6 - RF08.3
 * TC-069: Validar que, al marcar la revisión como No Conforme – Subsanable,
 *         el estado pase a Subsanable.
 *
 * Nota (realidad actual):
 * - Al dar "Solicitud Subsanable" lanza error y NO cambia a "Subsanable"
 * - Por lo tanto, esta prueba actualmente será FALLIDA (y se guardan evidencias).
 */
class RF08_3TC_069Test {

    private static final String BASE_URL = "http://3.228.164.208/#/login";
    private static final String REVISION_HREF = "#/inspeccion/solicitudes/revisionDocumental";
    private static final String REVISION_URL_ABS = "http://3.228.164.208/#/inspeccion/solicitudes/revisionDocumental";

    private static final String USUARIO = "directorcuarentena@yopmail.com";
    private static final String PASSWORD = "director1";
    private static final String ESTADO_ENVIADA_VALUE = "1794"; // Enviada

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

    private void jsSetValueAndChange(WebDriver d, WebElement selectEl, String value) {
        ((JavascriptExecutor) d).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            selectEl, value
        );
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

    private void clickRobusto(WebDriver d, WebDriverWait w, By by) {
        RuntimeException last = null;

        for (int i = 1; i <= 3; i++) {
            try {
                WebElement el = w.until(ExpectedConditions.presenceOfElementLocated(by));
                w.until(ExpectedConditions.visibilityOf(el));
                scrollCenter(d, el);

                try {
                    w.until(ExpectedConditions.elementToBeClickable(el));
                    el.click();
                } catch (Exception e) {
                    jsClick(d, el);
                }
                return;

            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                last = new RuntimeException("Click interceptado/stale en " + by + " intento " + i, e);
            } catch (TimeoutException te) {
                last = new RuntimeException("Timeout esperando " + by + " intento " + i, te);
            }
        }
        if (last != null) throw last;
        throw new RuntimeException("No se pudo click: " + by);
    }

    /**
     * ✅ Igual que en TC-070:
     * Si aparece toast/alert de error (o notificación de no autorizado / no se pudo actualizar),
     * forza FAIL para que Surefire/Meiven lo marque como FALLIDO.
     */
    private void failSiHayToastError(WebDriver d, WebDriverWait w, String shotName) {
        try { Thread.sleep(600); } catch (InterruptedException ignore) {}

        By toastError = By.xpath(
            "//*[contains(@class,'toast') or contains(@class,'Toastify') or contains(@class,'alert') or " +
            "  contains(@class,'notification') or contains(@class,'notificacion') or contains(@class,'toast-container')]" +
            "[" +
            "  contains(translate(normalize-space(.),'ERROR','error'),'error')" +
            "  or contains(translate(normalize-space(.),'NOTIFICACIÓN','notificación'),'notificación')" +
            "  or contains(translate(normalize-space(.),'NO ESTAS AUTORIZADO','no estas autorizado'),'no estas autorizado')" +
            "  or contains(translate(normalize-space(.),'NO SE PUDO ACTUALIZAR EL ESTADO DOCUMENTAL','no se pudo actualizar el estado documental'),'no se pudo actualizar el estado documental')" +
            "]"
        );

        try {
            WebElement err = new WebDriverWait(d, Duration.ofSeconds(4))
                .until(ExpectedConditions.visibilityOfElementLocated(toastError));

            String msg = "";
            try { msg = err.getText(); } catch (Exception ignore) {}

            screenshot(d, shotName + "_TOAST_ERROR");
            throw new AssertionError("❌ Se mostró error en UI: " + (msg == null ? "(sin texto)" : msg.trim()));

        } catch (TimeoutException ignore) {
            // No salió toast de error -> continuar
        }
    }

    // ===================== PASOS (IGUAL QUE TC-068) =====================

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
        screenshot(d, "S6_RF083_TC069_01_login_ok");
    }

    private void irARevisionDocumental(WebDriver d, WebDriverWait w) {
        log("Ir a Revisión Documental por href");

        By linkBy = By.cssSelector("a.nav-link[href='" + REVISION_HREF + "']");

        try {
            WebElement link = w.until(ExpectedConditions.presenceOfElementLocated(linkBy));
            w.until(ExpectedConditions.visibilityOf(link));
            scrollCenter(d, link);
            try { link.click(); } catch (Exception e) { jsClick(d, link); }

            new WebDriverWait(d, Duration.ofSeconds(15)).until(ExpectedConditions.or(
                ExpectedConditions.urlContains("revisionDocumental"),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            ));

        } catch (Exception e) {
            log("Fallback URL directa del módulo");
            d.get(REVISION_URL_ABS);
            new WebDriverWait(d, Duration.ofSeconds(20)).until(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]"))
            );
        }

        waitDocumentReady(d);
        screenshot(d, "S6_RF083_TC069_02_modulo_revision_documental");
    }

    private void filtrarEstadoEnviadaSiExiste(WebDriver d, WebDriverWait w) {
        log("Filtrar Estado=Enviada (si existe el select)");

        WebElement selectEl = null;

        try {
            selectEl = new WebDriverWait(d, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(By.id("estadoId")));
        } catch (Exception ignore) {}

        if (selectEl == null) {
            try {
                selectEl = new WebDriverWait(d, Duration.ofSeconds(8)).until(
                    ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//label[contains(normalize-space(),'Estado')]/following::select[1]")
                    )
                );
            } catch (Exception ignore) {}
        }

        if (selectEl == null) {
            log("No existe filtro estadoId en esta vista -> continuar sin filtrar");
            return;
        }

        scrollCenter(d, selectEl);
        jsSetValueAndChange(d, selectEl, ESTADO_ENVIADA_VALUE);
        screenshot(d, "S6_RF083_TC069_03_estado_enviada");

        clickRobusto(d, w, By.xpath("//button[@type='button' and (normalize-space()='Filtrar' or contains(.,'Filtrar'))]"));

        w.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]//tbody/tr")),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(translate(.,'NOHAYDATOS','nohaydatos'),'no hay datos')]"))
        ));
        screenshot(d, "S6_RF083_TC069_04_filtrado");
    }

    private WebElement encontrarBotonIniciarRevisionEnFilaEnviada(WebDriver d, WebDriverWait w) {
        w.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table[contains(@class,'table')]")));

        List<WebElement> filas = d.findElements(By.xpath("//table[contains(@class,'table')]//tbody/tr"));
        if (filas == null || filas.isEmpty()) throw new NoSuchElementException("No hay filas en la tabla.");

        String first = filas.get(0).getText() == null ? "" : filas.get(0).getText().toLowerCase();
        if (first.contains("no hay datos")) throw new NoSuchElementException("La tabla devolvió 'No hay datos'.");

        for (WebElement fila : filas) {
            String estado = "";
            try { estado = fila.findElement(By.xpath(".//td[5]")).getText().trim().toLowerCase(); } catch (Exception ignore) {}

            if (!estado.contains("enviada")) continue;

            try {
                WebElement btn = fila.findElement(By.xpath(".//button[@type='button' and normalize-space()='Iniciar Revisión']"));
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            } catch (Exception ignore) {}
        }

        try {
            return w.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and normalize-space()='Iniciar Revisión']")
            ));
        } catch (Exception e) {
            throw new NoSuchElementException("No se encontró botón 'Iniciar Revisión' habilitado.");
        }
    }

    private void iniciarRevision(WebDriver d, WebDriverWait w) {
        log("Click Iniciar Revisión (en listado)");

        WebElement btn = encontrarBotonIniciarRevisionEnFilaEnviada(d, w);
        scrollCenter(d, btn);
        try { btn.click(); } catch (Exception e) { jsClick(d, btn); }

        waitDocumentReady(d);

        new WebDriverWait(d, Duration.ofSeconds(20)).until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(.,'Comprobación de Documentos')]")),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//h5[contains(.,'Documentos de la Solicitud')]")),
            ExpectedConditions.presenceOfElementLocated(By.xpath("//th[normalize-space()='Acciones' or normalize-space()='ACCIONES']"))
        ));

        screenshot(d, "S6_RF083_TC069_05_comprobacion_documentos");
    }

    private void abrirModalCriteriosConRevisar(WebDriver d, WebDriverWait w) {
        log("Click Revisar (abre modal criterios)");

        By revisarBy = By.xpath("//table[contains(@class,'table')]//button[@type='button' and normalize-space()='Revisar']");
        WebElement revisar = w.until(ExpectedConditions.elementToBeClickable(revisarBy));
        scrollCenter(d, revisar);
        try { revisar.click(); } catch (Exception e) { jsClick(d, revisar); }

        w.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'modal-content')]//h5[contains(@class,'modal-title') and contains(.,'Criterios de Cumplimiento')]")
        ));

        screenshot(d, "S6_RF083_TC069_06_modal_criterios");
    }

    private void completarCriteriosYGuardar(WebDriver d, WebDriverWait w) {
        log("Completar criterios: Conforme=Sí y Motivo=Ninguno");

        WebElement modal = w.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'modal-content')]")
        ));

        List<WebElement> filas = modal.findElements(By.xpath(".//table//tbody/tr"));
        if (filas == null || filas.isEmpty()) throw new NoSuchElementException("No hay filas de criterios en el modal.");

        for (WebElement fila : filas) {
            WebElement selectConforme = fila.findElement(By.xpath(".//td[2]//select"));
            scrollCenter(d, selectConforme);

            jsSetValueAndChange(d, selectConforme, "true");

            WebElement selectMotivo = fila.findElement(By.xpath(".//td[3]//select"));
            if (selectMotivo.isEnabled()) {
                jsSetValueAndChange(d, selectMotivo, "2797"); // Ninguno
            }
        }

        screenshot(d, "S6_RF083_TC069_07_criterios_llenos");

        By guardarBy = By.xpath("//div[contains(@class,'modal-content')]//button[contains(.,'Guardar y Definir Conformidad')]");
        WebElement guardar = new WebDriverWait(d, Duration.ofSeconds(25)).until(driver -> {
            try {
                WebElement b = driver.findElement(guardarBy);
                return (b.isDisplayed() && b.isEnabled()) ? b : null;
            } catch (Exception e) {
                return null;
            }
        });

        scrollCenter(d, guardar);
        try { guardar.click(); } catch (Exception e) { jsClick(d, guardar); }

        screenshot(d, "S6_RF083_TC069_08_click_guardar_definir");

        try {
            new WebDriverWait(d, Duration.ofSeconds(10)).until(
                ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'modal-content')]"))
            );
        } catch (Exception ignore) {}

        waitDocumentReady(d);
    }

    // ===================== TC-069 (NO CONFORME - SUBSANABLE) =====================

    private void marcarNoConforme(WebDriver d, WebDriverWait w) {
        log("Click botón principal: No Conforme (btn-danger btn-md)");

        By noConformeMain = By.xpath("//button[@type='button' and contains(@class,'btn-danger') and contains(@class,'btn-md') and normalize-space()='No Conforme']");
        clickRobusto(d, w, noConformeMain);

        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".modal.show, .modal-dialog, .modal-content")));
        screenshot(d, "S6_RF083_TC069_09_no_conforme_main_click");
    }

    private void confirmarNoConformeModal(WebDriver d, WebDriverWait w) {
        log("Modal confirmación: click No Conforme (btn-danger)");

        By noConformeModal = By.xpath("//div[contains(@class,'modal') and contains(@class,'show')]//button[@type='button' and contains(@class,'btn-danger') and normalize-space()='No Conforme']");
        clickRobusto(d, w, noConformeModal);

        w.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".modal.show, .modal-dialog, .modal-content")));
        screenshot(d, "S6_RF083_TC069_10_no_conforme_modal_click");
    }

    private void decisionFinalSolicitudSubsanable(WebDriver d, WebDriverWait w) {
        log("Decisión Final: llenar textarea y click Solicitud Subsanable");

        By textarea = By.xpath("//div[contains(@class,'modal') and contains(@class,'show')]//textarea[contains(@class,'form-control')]");
        WebElement txt = w.until(ExpectedConditions.visibilityOfElementLocated(textarea));
        scrollCenter(d, txt);
        txt.clear();
        txt.sendKeys("TC-069 - Motivo general automático (subsanable)");

        screenshot(d, "S6_RF083_TC069_11_textarea_llena");

        By btnSubsanable = By.xpath("//div[contains(@class,'modal') and contains(@class,'show')]//button[@type='button' and contains(@class,'btn-warning') and normalize-space()='Solicitud Subsanable']");
        clickRobusto(d, w, btnSubsanable);

        screenshot(d, "S6_RF083_TC069_12_click_solicitud_subsanable");

        // ✅ NUEVO: si aparecen toasts rojos, esto fuerza FAIL (como TC-070)
        failSiHayToastError(d, w, "S6_RF083_TC069_12A");

        waitDocumentReady(d);
    }

    private boolean estadoEsSubsanableEnPantalla(WebDriver d) {
        try {
            List<WebElement> hits = d.findElements(By.xpath(
                "//*[self::td or self::span or self::div or self::p or self::small]" +
                "[contains(translate(normalize-space(.),'SUBSANABLE','subsanable'),'subsanable')]"
            ));
            for (WebElement h : hits) {
                if (h.isDisplayed()) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    private void validarCambioAEstadoSubsanable(WebDriver d, WebDriverWait w) {
        log("Validar que el estado cambie a 'Subsanable' (esperado).");

        boolean subsanable = false;
        try {
            subsanable = new WebDriverWait(d, Duration.ofSeconds(8)).until(x -> estadoEsSubsanableEnPantalla(x));
        } catch (Exception ignore) {
            subsanable = estadoEsSubsanableEnPantalla(d);
        }

        screenshot(d, "S6_RF083_TC069_13_validacion_estado");

        assertTrue(subsanable,
            "❌ TC-069 FALLÓ: El estado NO cambió a 'Subsanable' tras 'Solicitud Subsanable'. (Se esperaba Subsanable)");
    }

    // ===================== TEST =====================
    @Test
    void RF083_TC069_MarcarNoConforme_Subsanable_DebeCambiarEstadoASubsanable() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--start-maximized", "--lang=es-419");
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            login(driver, wait);

            irARevisionDocumental(driver, wait);

            filtrarEstadoEnviadaSiExiste(driver, wait);

            iniciarRevision(driver, wait);

            abrirModalCriteriosConRevisar(driver, wait);

            completarCriteriosYGuardar(driver, wait);

            // ====== TC-069 específico ======
            marcarNoConforme(driver, wait);

            confirmarNoConformeModal(driver, wait);

            decisionFinalSolicitudSubsanable(driver, wait);

            // Si no hubo toast error, entonces validamos estado (para cuando lo arreglen)
            validarCambioAEstadoSubsanable(driver, wait);

            System.out.println("✅ TC-069 OK: estado cambió a Subsanable.");

        } catch (AssertionError ae) {
            // Para que quede evidencia también cuando falle por toast error o por estado
            screenshot(driver, "S6_RF083_TC069_ASSERT_FAIL");
            throw ae;
        } catch (Exception e) {
            screenshot(driver, "S6_RF083_TC069_ERROR");
            throw e;
        } finally {
            // driver.quit();
        }
    }
}
