package e2e.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    // Puedes sobreescribir con -DbaseUrl, -Drole, -Duser, -Dpass
    protected final String baseUrl = System.getProperty("baseUrl", "http://3.228.164.208/#");
    protected final String role    = System.getProperty("role", "funcionario"); // "funcionario" | "importador"
    protected final String user    = System.getProperty("user",
            role.equalsIgnoreCase("importador") ? "rrivasmelhado@gmail.com" : "08011972008565");
    protected final String pass    = System.getProperty("pass",
            role.equalsIgnoreCase("importador") ? "senasapalladium2" : "Senasa2025");

    @BeforeEach
    void setUp() {
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments(
                "--incognito",
                "--disable-extensions",
                "--proxy-server=direct://",
                "--proxy-bypass-list=*",
                "--start-maximized"
        );
        driver = new ChromeDriver(opt);

        // Sin espera implícita; todo explícito
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        login(); // login antes de cada test
    }

    @AfterEach
    void tearDown() {
        try { takeArtifacts("afterEach"); } catch (Exception ignored) {}
        if (driver != null) driver.quit();
    }

    /* ========= Login robusto ========= */

    protected void login() {
        // Navega a la ruta de login cuidando el hash
        driver.get(go("/login"));

        waitDocumentReady(Duration.ofSeconds(30));
        waitOverlaysGone(Duration.ofSeconds(15),
                By.cssSelector(".modal-backdrop"),
                By.cssSelector(".spinner, .loading, .preloader"),
                By.id("loader"), By.id("loading")
        );

        // Posibles localizadores del campo usuario (incluye 'identificador' que salió en tus logs)
        List<By> userLocators = List.of(
                By.id("identificador"),
                By.id("usuario"),
                By.id("username"),
                By.cssSelector("input[name='usuario']"),
                By.cssSelector("input[name='username']"),
                By.cssSelector("input[type='email']")
        );

        // Si el input está dentro de iframe, cambiamos al frame que lo contenga
        if (!switchToFrameContaining(userLocators, Duration.ofSeconds(3))) {
            driver.switchTo().defaultContent();
        }

        WebElement userInput = firstVisible(userLocators, Duration.ofSeconds(20));
        WebElement passInput = firstVisible(List.of(
                By.id("clave"),
                By.id("password"),
                By.cssSelector("input[name='clave']"),
                By.cssSelector("input[name='password']"),
                By.cssSelector("input[type='password']")
        ), Duration.ofSeconds(20));

        userInput.clear(); userInput.sendKeys(user);
        passInput.clear(); passInput.sendKeys(pass);

        By submitBy = firstClickableLocator(List.of(
                By.cssSelector("button[type='submit']"),
                By.xpath("//button[normalize-space()='Ingresar' or contains(.,'Login') or contains(.,'Entrar') or contains(.,'Acceder')]")
        ), Duration.ofSeconds(10));

        safeClick(submitBy, Duration.ofSeconds(20));

        // Espera a que cargue el layout principal
        waitUntilAnyVisible(Duration.ofSeconds(30),
                By.cssSelector("[data-testid='main-layout']"),
                By.cssSelector("#root"),
                By.xpath("//*[contains(@class,'layout') or contains(@id,'layout') or self::main]")
        );

        System.out.println("[DEBUG] Login OK como " + role + " | URL: " + driver.getCurrentUrl() + " | Title: " + driver.getTitle());
    }

    /* ========= Helpers de URL para SPA con hash ========= */

    /** Construye una URL respetando si baseUrl trae hash o no. */
    protected String go(String route) {
        // normaliza route
        String r = route == null ? "" : route.trim();
        if (!r.isEmpty() && !r.startsWith("/")) r = "/" + r;

        String b = baseUrl.trim();

        // casos:
        // 1) base sin hash: http://host -> http://host/login
        // 2) base con '#' al final: http://host/# -> http://host/#/login
        // 3) base con '#/': http://host/#/ -> http://host/#/login
        if (b.endsWith("#")) {
            return b + r; // .../# + /login => .../#/login
        } else if (b.contains("#/")) {
            // asegura que no dupliquemos slash
            if (b.endsWith("/")) return b + trimLeftSlash(r);
            return b + r;
        } else {
            // sin hash
            if (b.endsWith("/")) return b.substring(0, b.length()-1) + r;
            return b + r;
        }
    }

    private String trimLeftSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s.substring(1) : s;
    }

    /* ========= Esperas/acciones avanzadas ========= */

    protected void waitDocumentReady(Duration timeout) {
        new WebDriverWait(driver, timeout).until(d ->
                "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    protected void waitOverlaysGone(Duration timeout, By... overlays) {
        WebDriverWait w = new WebDriverWait(driver, timeout);
        for (By by : overlays) {
            w.until(d -> d.findElements(by).isEmpty() ||
                    d.findElements(by).stream().noneMatch(WebElement::isDisplayed));
        }
    }

    protected WebElement firstVisible(List<By> locators, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            for (By by : locators) {
                List<WebElement> els = driver.findElements(by);
                if (!els.isEmpty()) {
                    for (WebElement el : els) {
                        if (el.isDisplayed()) return el;
                    }
                }
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        throw new TimeoutException("Ningún locator visible: " + locators);
    }

    protected By firstClickableLocator(List<By> locators, Duration timeout) {
        for (By by : locators) {
            try {
                new WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(by));
                return by;
            } catch (TimeoutException ignored) {}
        }
        throw new TimeoutException("Ningún locator clickable: " + locators);
    }

    protected void safeClick(By locator, Duration timeout) {
        WebElement el = new WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(locator));
        scrollIntoView(el);
        try {
            el.click();
        } catch (ElementClickInterceptedException ex) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    protected void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    /** Cambia al iframe que contenga cualquiera de los locators; devuelve true si encontró alguno. */
    protected boolean switchToFrameContaining(List<By> innerLocators, Duration timeout) {
        driver.switchTo().defaultContent();
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            for (int i = 0; i < iframes.size(); i++) {
                try {
                    driver.switchTo().defaultContent();
                    driver.switchTo().frame(i);
                    for (By inner : innerLocators) {
                        if (!driver.findElements(inner).isEmpty()) return true;
                    }
                } catch (NoSuchFrameException ignored) {}
            }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        driver.switchTo().defaultContent();
        return false;
    }

    protected void waitUntilAnyVisible(Duration timeout, By... locators) {
        List<By> list = Arrays.asList(locators);
        new WebDriverWait(driver, timeout).until(d -> {
            for (By by : list) {
                try {
                    WebElement el = d.findElement(by);
                    if (el.isDisplayed()) return true;
                } catch (NoSuchElementException ignored) {}
            }
            return false;
        });
    }

    /* ========= Artefactos ========= */

    protected void takeArtifacts(String tag) throws Exception {
        // Screenshot
        if (driver instanceof TakesScreenshot) {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path out = Path.of("target", "surefire-reports", "screenshot-" + tag + "-" + System.currentTimeMillis() + ".png");
            Files.createDirectories(out.getParent());
            Files.write(out, png);
        }
        // PageSource
        Path ps = Path.of("target", "surefire-reports", "pagesource-" + tag + "-" + System.currentTimeMillis() + ".html");
        Files.createDirectories(ps.getParent());
        Files.write(ps, driver.getPageSource().getBytes());
        System.out.println("[ARTIFACTS] Guardados screenshot y pageSource (" + tag + ")");
    }
}
