package io.github.molok.wasdeployer;

import ch.qos.logback.classic.Level;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    static Logger log = LoggerFactory.getLogger(App.class);

    public void uninstall(String server, String user, String password, int verbosity, String appName) {
        setVerbosity(verbosity);
        WebDriver driver = webDriver(true);
        try {
            goToConsole(server, user, password, driver);
            driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
            driver.findElement(By.id("button." + "uninstall" + "ApplicationDeployment")).click();
            driver.findElement(By.xpath("//input[@id='ok']")).click();
            driver.findElement(By.xpath("//a[contains(@href, 'directsave=true')]")).click();
            printMessages("console", driver);
        } finally {
            driver.quit();
        }
    }


    public void start(String server, String user, String password, int verbosity, String appName) {
        setVerbosity(verbosity);
        WebDriver driver = webDriver(true);
        try {
            goToConsole(server, user, password, driver);
            driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
            driver.findElement(By.id("button." + "start" + "ApplicationDeployment")).click();
            printMessages("console", driver);
        } finally {
            driver.quit();
        }
    }

    public void stop(String server, String user, String password, int verbosity, String appName) {
        setVerbosity(verbosity);
        WebDriver driver = webDriver(true);
        try {
            goToConsole(server, user, password, driver);
            driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
            driver.findElement(By.id("button." + "stop" + "ApplicationDeployment")).click();
            printMessages("console", driver);
        } finally {
            driver.quit();
        }

    }

    public void list(String server, String user, String password, int verbosity, String appName) {
        setVerbosity(verbosity);
        WebDriver driver = webDriver(true);
        try {
            goToConsole(server, user, password, driver);
            List<String> apps = driver.findElements(By.xpath("//img[contains(@name, 'statusIconApplicationDeployment')]"))
                    .stream()
                    .map(img -> img.getAttribute("onmouseover"))
                    .filter(script -> appName == null || script.contains("name=" + appName + "\""))
                    .collect(Collectors.toList());

            apps.stream().map(
                    app -> {
                        String url = app.substring(app.indexOf("\"") + 1, app.lastIndexOf("\""));
                        driver.get(server + url);
                        String status = driver.findElement(By.xpath("//body")).getText();
                        String appName_ = url.split("name=")[1];
                        return new AppStatus(appName_, status);
                    }).collect(Collectors.toList())
                    .forEach(as -> System.out.println(String.format("%-40s", as.app) + ": " + as.status));
        } finally {
            driver.quit();
        }
    }

    private WebDriver webDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(
                new ChromeOptions()
                        .setAcceptInsecureCerts(true)
                        .setHeadless(headless));
    }

    private void setVerbosity(int verbosity) {
        if (verbosity == 0) {
            shutup();
        } else if (verbosity == 1) {
            changeLogLevel(Level.INFO);
        } else {
            changeLogLevel(Level.TRACE);
        }
    }

    private void shutup() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        System.setProperty("webdriver.chrome.args", "--disable-logging");
        System.setProperty("webdriver.chrome.silentOutput", "true");
    }

    public void deploy(String baseUrl,
                       String user,
                       String password,
                       String warLocation,
                       String contextRoot,
                       boolean isLocalFile,
                       String appName,
                       boolean newInstall,
                       String serverNamesRegex,
                       boolean showGui,
                       int verbosity) {

        setVerbosity(verbosity);

        WebDriver driver = webDriver(!showGui);
        try {
            doDeploy(baseUrl, user, password, warLocation, contextRoot, isLocalFile, appName, newInstall, serverNamesRegex, driver);
        } finally {
            driver.quit();
        }
    }

    private void changeLogLevel(Level info) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(info);
    }

    private void doDeploy(String baseUrl, String user, String password, String warLocation, String contextRoot, boolean isLocalFile, String appName, boolean newInstall, String serverNamesRegex, WebDriver driver) {
        String absolutePathLocation = canonicalizePath(warLocation, isLocalFile);

        goToConsole(baseUrl, user, password, driver);

        if (newInstall) {
            driver.findElement(By.id("button.installApplicationDeployment")).click();
            if (isLocalFile) {
                driver.findElement(By.id("local")).click();
                driver.findElement(By.id("localFilepath")).sendKeys(absolutePathLocation);
            } else {
                driver.findElement(By.id("server")).click();
                driver.findElement(By.id("remoteFilepath")).sendKeys(absolutePathLocation);
            }
        } else {
            driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
            driver.findElement(By.id("button.updateApplicationDeployment")).click();
            if (isLocalFile) {
                driver.findElement(By.id("earUpdate_localFilepath")).sendKeys(absolutePathLocation);
            } else {
                driver.findElement(By.id("earUpdate_server")).click();
                driver.findElement(By.id("earUpdate_remoteFilepath")).sendKeys(absolutePathLocation);
            }
        }

        printMessages("uploading war, this may take a while...", driver);
        driver.findElement(By.id("nextAction")).click();
        printMessages("uploaded war", driver);

        driver.findElement(By.id("enxt")).click(); // not a typo (at least not mine :)
        printMessages("fast path", driver);

        if (newInstall) {
            /* settings the application name, wonky logic but the id of the input field changes from version to version... */
            String defaultName = Paths.get(absolutePathLocation).getFileName().toString().replace(".", "_");
            driver.findElement(By.xpath("//input[@value='" + defaultName + "']")).clear();
            driver.findElement(By.xpath("//input[@value='" + defaultName + "']")).sendKeys(appName);
        }

        // select installation options
        assertStep(driver, "appmanagement.install.options");
        driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[1]")).click();
        printMessages("install options", driver);

        assertStep(driver, "appmanagement.modules.toappservers");
        List<WebElement> servers = driver.findElements(By.xpath("//select[@id='selectedList']/option"));
        Select combo = new Select(driver.findElement(By.xpath("//select[@id='selectedList']")));
        if (servers.size() > 1) {
            driver.findElement(By.xpath("//input[@id='1']")).click();
            servers.forEach(s -> {
                if (s.getAttribute("value").matches(serverNamesRegex)) {
                    combo.selectByValue(s.getAttribute("value"));
                }
            });
            driver.findElement(By.xpath("//input[@id='other']")).click();
            printMessages("console", driver);
        } else if (servers.size() == 0) {
            throw new RuntimeException("Error, can't locate select");
        }

        // next button
        driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();
        printMessages("mapped servers", driver);

        if ("appmanagement.virtualhosts.towebmodules".equalsIgnoreCase(
                currentStep(driver))) {
            // next button
            driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();
            printMessages("mapped web-modules", driver);
        }

        assertStep(driver, "appmanagement.contxtroot.forwebmodules");
        driver.findElement(By.className("noIndentTextEntry")).clear();
        driver.findElement(By.className("noIndentTextEntry")).sendKeys(contextRoot);
        // next button
        driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();

        printMessages("mapped context-root", driver);

        if ("appmanagement.metadatacomplete.formodules".equalsIgnoreCase(currentStep(driver))) {
            // next button
            driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();
            printMessages("mapped metadata-complete", driver);
        }

        assertStep(driver, "appmanagement.summary");
        // finish button
        driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();

        printMessages("reached summary", driver);

        new WebDriverWait(driver, 180)
                .pollingEvery(Duration.ofSeconds(1))
                .until(drv -> {
                    drv.findElements(By.xpath("//p[@class='status-text']")).forEach(t -> System.out.println("WAS: " + t.getText().replace("\n", "\nWAS: ")));
                    return drv.findElement(By.xpath("//a[contains(@href, 'directsave=true')]"));
                })
                .click();

        if (driver.findElements(By.xpath("//input[@name='statusDone']")).size() > 0) {
            driver.findElement(By.xpath("//input[@name='statusDone']")).click();
        }

        printMessages("installation completed", driver);

        driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
        driver.findElement(By.id("button.startApplicationDeployment")).click();

        printMessages("start completed", driver);

        log.info("done!");
    }

    private String currentStep(WebDriver driver) {
        return driver.findElement(By.xpath("//input[@name='currentStep']")).getAttribute("value");
    }

    private String canonicalizePath(String warLocation, boolean isLocalFile) {
        String absolutePathLocation;
        Path path = Paths.get(warLocation);
        if (isLocalFile) {
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("WAR (" + warLocation + ") not found or unreadable");
            } else {
                absolutePathLocation = path.toAbsolutePath().toString();
            }
        } else {
            absolutePathLocation = warLocation;
        }
        return absolutePathLocation;
    }

    private void printMessages(String page, WebDriver driver) {
        log.info(page);
        List<WebElement> msgPostStart = driver.findElements(By.xpath("//tbody[@id='com_ibm_ws_inlineMessages']"));
        if (msgPostStart.size() > 0) {
            System.out.println("WAS: " + msgPostStart.get(0).getText().replace("\n", "\nWAS: "));
        }
    }

    private void goToConsole(String baseUrl, String user, String password, WebDriver driver) {
        String url = baseUrl + "/ibm/console/login.do?action=secure";
        driver.manage().window().maximize();
        driver.get(url);

        // login
        driver.findElement(By.id("j_username")).sendKeys(user);
        driver.findElement(By.id("j_password")).sendKeys(password);
        driver.findElement(By.id("other")).click();

        // old session or another client logged in, force login
        if (driver.findElements(By.xpath("//input[@id='forceradio']")).size() > 0) {
            driver.findElement(By.className("loginButton")).click();
            printMessages("Log out other user", driver);
        }

        // previous session timedout, just start from scratch
        if (driver.findElements(By.id("aradiorestore")).size() > 0) {
            driver.findElement(By.id("aradiorestore")).click();
            driver.findElement(By.className("loginbutton")).click();
            printMessages("Discarded previous session", driver);
        }

        // websphere uses frames, I check if I'm actually logged-in
        driver.get(baseUrl + "/ibm/console/secure/isclite/tiles/bannerframe.jsp");
        new WebDriverWait(driver, 1000).pollingEvery(Duration.ofSeconds(1)).until(
                drv -> drv.findElement(By.id("ibm-banner-content")).getText().contains(user)
        );

        printMessages("logged in", driver);

        // console frame
        driver.get(baseUrl + "/ibm/console/nsc.do");

        String appsUrl = driver.findElement(By.xpath("//a[contains(@href, 'ApplicationDeployment.content.main')]")).getAttribute("href");
        driver.get(appsUrl);
        driver.findElement(By.xpath("//a[@aria-controls='com_ibm_ws_prefsTable']")).click();
        driver.findElement(By.xpath("//td[@class='find-filter']/input[@type='text']")).clear();
        driver.findElement(By.xpath("//td[@class='find-filter']/input[@type='text']")).sendKeys("10000");
        driver.findElement(By.xpath("//*[@name='submit2' and @type='submit']")).click();
        printMessages("opened applications", driver);
    }

    private void assertStep(WebDriver driver, String expected) {
        String currStep = currentStep(driver);
        if (!currStep.equalsIgnoreCase(expected)) {
            log.debug(driver.getPageSource());
            throw new RuntimeException("Wrong step " + expected + " found " + currStep);
        }
    }

    private static class AppStatus {
        public final String app;
        public final String status;

        public AppStatus(String app, String status) {
            this.app = app;
            this.status = status;
        }
    }

}
