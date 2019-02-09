package io.github.molok.wasdeployer;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.cli.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class Main {
    static Logger log = LoggerFactory.getLogger(Main.class);

    private enum RetCode {
        SUCCESS(0), ERROR(1), INVALID_ARGS(2);
        final int code;
        RetCode(int code) { this.code = code;}
    }

    public static void main(String[] args) {
        System.exit(doMain(args));
    }

    private static int doMain(String[] args) {
        try {
            CommandLine cli = new DefaultParser().parse(cliOptions(), args);

            new Main().deploy(
                    cli.getOptionValue("s"),
                    cli.getOptionValue("a").split(":")[0],
                    cli.getOptionValue("a").split(":")[1],
                    cli.getOptionValue("f", cli.getOptionValue("r")),
                    cli.getOptionValue("c", cli.getOptionValue("n")),
                    cli.hasOption("f"),
                    cli.getOptionValue("n"),
                    cli.hasOption("i"),
                    cli.getOptionValue("sn", ""),
                    cli.hasOption("g"));

            return RetCode.SUCCESS.code;
        } catch (MissingOptionException e) {
            printUsage();

            System.out.println("Missing required options: " + missingArguments(e));
            return RetCode.INVALID_ARGS.code;
        } catch (ParseException e) {
            printUsage();
            e.printStackTrace();
            return RetCode.INVALID_ARGS.code;
        } catch (Exception e) {
            e.printStackTrace();
            return RetCode.ERROR.code;
        }
    }

    private static String missingArguments(MissingOptionException e) {
        String missingOpts = ((List<Object>)
                 e.getMissingOptions()).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(o -> cliOptions().getOptions().stream().filter(f -> f.getOpt().equals(o)))
                .map(o -> "--" + o.getLongOpt())
                .reduce((a, b) -> a + ", " + b).orElse("");

        String missingGroups = ((List<Object>)
                e.getMissingOptions()).stream()
                .filter(OptionGroup.class::isInstance)
                .map(OptionGroup.class::cast)
                .map(o -> optGroupToString(o))
                .reduce("", (a, b) -> a + ", " + b);
        return missingOpts + missingGroups;
    }

    private static String optGroupToString(OptionGroup o) {
        return "[" + o.getOptions().stream().map(f -> f.getLongOpt()).reduce((a, b) -> "--" + a + " | --" + b).orElse("") + "]";
    }

    private static void printUsage() {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(80);
        hf.printHelp("java -jar was-deployer.jar ", "", cliOptions(),
                    "\nversion " + Main.class.getPackage().getImplementationVersion() + "\n"
                        + "\nexample: java -jar was-deployer -i -f ./app.war -n app -s 'https://localhost:9043' -u wsadmin:secret\n\n", true);
    }

    private static Options cliOptions() {
        Options opts = new Options();

        OptionGroup installOrUpdate = new OptionGroup();
        installOrUpdate.setRequired(true);
        installOrUpdate.addOption(
                Option.builder("i")
                        .longOpt("install")
                        .hasArg(false)
                        .desc("install the application")
                        .build());

        installOrUpdate.addOption(
                Option.builder("u")
                        .longOpt("update")
                        .hasArg(false)
                        .desc("update the application")
                        .build());

        opts.addOptionGroup(installOrUpdate);

        OptionGroup localOrRemote = new OptionGroup();
        localOrRemote.setRequired(true);
        localOrRemote.addOption( Option.builder("f")
                .longOpt("file")
                .hasArg()
                .argName("war_path")
                .desc("location of the WAR to deploy, it needs to be accessible by the server")
                .build());
        localOrRemote.addOption(Option.builder("r")
                .longOpt("remote")
                .hasArg()
                .argName("war_remote_path")
                .desc("remote location of the WAR to deploy")
                .build());

        opts.addOptionGroup(localOrRemote);

        opts.addOption(Option.builder("s")
                .longOpt("server")
                .hasArg()
                .argName("server_url")
                .required()
                .desc("URL of the server, e.g. https://localhost:9043")
                .build());

        opts.addOption(Option.builder("a")
                .longOpt("auth")
                .hasArg()
                .valueSeparator(':')
                .argName("user:password")
                .required()
                .desc("WAS username and password, e.g. wsadmin:secret")
                .build());

        opts.addOption(Option.builder("n")
                .longOpt("name")
                .hasArg()
                .required()
                .argName("app_name")
                .desc("name of the application")
                .build());

        opts.addOption(Option.builder("c")
                .longOpt("contextroot")
                .hasArg()
                .argName("app_context_root")
                .desc("context-root (if not supplied the name of the application is used)")
                .build());

        opts.addOption(Option.builder("sn")
                .longOpt("servernames")
                .argName("server_name")
                .hasArg()
                .desc("Java regex of the server where the WAR will be installed, e.g. (.*AppCluster.*|.*webserver.*)")
                .build());

        opts.addOption(Option.builder("g")
                .longOpt("gui")
                .desc("the browser is shown, by the default it is run headless")
                .build());

        return opts;
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
                       boolean showGui ) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (!showGui) {
            options.addArguments("--headless");
        }
        WebDriver driver = new ChromeDriver(options);
        try {
            doDeploy(baseUrl, user, password, warLocation, contextRoot, isLocalFile, appName, newInstall, serverNamesRegex, driver);
        } finally {
            driver.quit();
        }
    }

    private void doDeploy(String baseUrl, String user, String password, String warLocation, String contextRoot, boolean isLocalFile, String appName, boolean newInstall, String serverNamesRegex, WebDriver driver) {
        goToConsole(baseUrl, user, password, driver);

        if (newInstall) {
            driver.findElement(By.id("button.installApplicationDeployment")).click();
            if (isLocalFile) {
                driver.findElement(By.id("local")).click();
                driver.findElement(By.id("localFilepath")).sendKeys(warLocation);
            } else {
                driver.findElement(By.id("server")).click();
                driver.findElement(By.id("remoteFilepath")).sendKeys(warLocation);
            }
        } else {
            driver.findElement(By.id(appName + ".ear/deployments/" + appName)).click();
            driver.findElement(By.id("button.updateApplicationDeployment")).click();
            if (isLocalFile) {
                driver.findElement(By.id("earUpdate_localFilepath")).sendKeys(warLocation);
            } else {
                driver.findElement(By.id("earUpdate_server")).click();
                driver.findElement(By.id("earUpdate_remoteFilepath")).sendKeys(warLocation);
            }
        }

        driver.findElement(By.id("nextAction")).click();
        printMessages("uploaded war", driver);

        driver.findElement(By.id("enxt")).click(); // not a typo (at least not mine :)
        printMessages("fast path", driver);

        if (newInstall) {
            /* settings the application name, wonky logic but the id of the input field changes from version to version... */
            String defaultName = Paths.get(warLocation).getFileName().toString().replace(".", "_");
            driver.findElement(By.xpath("//input[@value='"+ defaultName + "']")).clear();
            driver.findElement(By.xpath("//input[@value='"+ defaultName + "']")).sendKeys(appName);
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

        if (newInstall) {
            assertStep(driver, "appmanagement.virtualhosts.towebmodules");
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

        assertStep(driver, "appmanagement.summary");
        // finish button
        driver.findElement(By.xpath("//td[contains(@class, 'wizard-button-section')]/input[2]")).click();

        printMessages("reached summary", driver);

        new WebDriverWait(driver, 180)
                .pollingEvery(Duration.ofSeconds(1))
                .until(drv -> {
                    drv.findElements(By.xpath("//p[@class='status-text']")).forEach(t -> log.info(t.getText()));
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

    private void printMessages(String page, WebDriver driver) {
        log.info(page);
        List<WebElement> msgPostStart = driver.findElements(By.xpath("//tbody[@id='com_ibm_ws_inlineMessages']"));
        if (msgPostStart.size() > 0) {
            log.info(msgPostStart.get(0).getText());
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
        printMessages("opened applications", driver);
    }

    private void assertStep(WebDriver driver, String expected) {
        String currStep = driver.findElement(By.xpath("//input[@name='currentStep']")).getAttribute("value");
        if (!currStep.equalsIgnoreCase(expected)) {
            throw new RuntimeException("Wrong step " + expected + " found " + currStep);
        }
    }

}
