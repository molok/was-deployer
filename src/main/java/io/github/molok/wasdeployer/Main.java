package io.github.molok.wasdeployer;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
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
            if (args.length > 0) {
                String action = args[0];
                switch (action) {
                    case "config-sample": {
                        ConfigManager.configSample();
                        return RetCode.SUCCESS.code;
                    }
                    case "deploy": {
                        try {
                            CommandLine cli = new DefaultParser().parse(deployOptions(), args);

                            String user;
                            String password;
                            String server;
                            String serverNames;

                            if (cli.hasOption("p")) {
                                ConfigManager.Profile profile = ConfigManager.readProfile(cli.getOptionValue("p"));
                                server = profile.server;
                                user = profile.user;
                                password = profile.password;
                                serverNames = profile.serverNames;
                            } else {
                                server = cli.getOptionValue("s");
                                user = cli.getOptionValue("a").split(":")[0];
                                password = cli.getOptionValue("a").split(":")[1];
                                serverNames = cli.getOptionValue("sn", "");
                            }
                            String warLocation = cli.getOptionValue("f", cli.getOptionValue("r"));

                            String appName = cli.getOptionValue("n", toAppName(warLocation));
                            String contextRoot = cli.getOptionValue("c", appName);

                            new App().deploy(
                                    server,
                                    user,
                                    password,
                                    warLocation,
                                    contextRoot,
                                    cli.hasOption("f"),
                                    appName,
                                    cli.hasOption("i"),
                                    serverNames,
                                    cli.hasOption("g"),
                                    verbosityLevel(cli)
                            );

                            return RetCode.SUCCESS.code;
                        } catch (MissingOptionException e) {
                            printUsage();
                            System.out.println("Missing required options: " + missingArguments(e, deployOptions()));
                            return RetCode.INVALID_ARGS.code;
                        }
                    }
                    case "list": {
                        try {
                            CommandLine cli = new DefaultParser().parse(listOptions(), args);

                            String server;
                            String user;
                            String password;

                            if (cli.hasOption("p")) {
                                ConfigManager.Profile profile = ConfigManager.readProfile(cli.getOptionValue("p"));
                                server = profile.server;
                                user = profile.user;
                                password = profile.password;
                            } else {
                                server = cli.getOptionValue("s");
                                user = cli.getOptionValue("a").split(":")[0];
                                password = cli.getOptionValue("a").split(":")[1];
                            }

                            new App().list( server, user, password, verbosityLevel(cli));

                            return RetCode.SUCCESS.code;
                        } catch (MissingOptionException e) {
                            printUsage();
                            System.out.println("Missing required options: " + missingArguments(e, listOptions()));
                            return RetCode.INVALID_ARGS.code;
                        }
                    }
                }
            }
            printUsage();
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

    private static int verbosityLevel(CommandLine cli) {
        return cli.hasOption("vv") ? 2 : cli.hasOption("v") ? 1 : 0;
    }

    static String toAppName(String warLocation) {
        String res = Paths.get(warLocation).toFile().getName();
        if (res.indexOf(".") >= 0) {
            res = res.substring(0, res.indexOf("."));
        }
        return res.replaceAll("[\\/,#$@:;\"*?<>|=+&%\\]]", "_");
    }

    private static String missingArguments(MissingOptionException e, Options options) {
        String missingOpts = ((List<Object>)
                 e.getMissingOptions()).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(o -> options.getOptions().stream().filter(f -> f.getOpt().equals(o)))
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
        return "[" + o.getOptions().stream()
                      .map(f -> f.getLongOpt())
                      .reduce((a, b) -> "--" + a + " | --" + b)
                      .orElse("") + "]";
    }

    private static int terminalWidth() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"bash", "-c", "tput cols 2> /dev/tty" });
            p.waitFor();
            byte[] output = new byte[10];
            p.getInputStream().read(output);
            return Integer.parseInt(new String(output).trim());
        } catch (Exception e) {
            return 100;
        }
    }

    private static void printUsage() {
        System.out.println("WAS-Deployer v" + Main.class.getPackage().getImplementationVersion() + "\n"
                          + "\nusage:\n");
        HelpFormatter hf = new HelpFormatter();
        hf.setSyntaxPrefix("");
        hf.setWidth(terminalWidth());
        hf.printHelp("java -jar was-deployer.jar deploy|list|config-sample [<args>]\n" +
                                 "\ndeploy        (deploy a war on a WAS instance)",
                          "", deployOptions(), "", false);
        hf.printHelp("\nlist          (list installed applications and their current state)", "", listOptions(), "");
        System.out.println("\nconfig-sample (prints a configuration example, to be placed in " + ConfigManager.DEFAULT_LOCATION + ")");
    }

    private static Options listOptions() {
        Options opts = new Options();

        opts.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Increase verbosity")
                .build());

        opts.addOption(Option.builder("vv")
                .longOpt("very-verbose")
                .desc("Increase verbosity more")
                .build());

        opts.addOption(Option.builder("p")
                .longOpt("profile")
                .hasArg()
                .argName("profile_name")
                .desc("Name of the profile set in " + ConfigManager.DEFAULT_LOCATION + ", overrides --server, --auth")
                .build());

        opts.addOption(Option.builder("s")
                .longOpt("server")
                .hasArg()
                .argName("server_url")
                .desc("URL of the server, e.g. https://example.org:9043")
                .build());

        opts.addOption(Option.builder("a")
                .longOpt("auth")
                .hasArg()
                .valueSeparator(':')
                .argName("user:password")
                .desc("WAS username and password, e.g. wsadmin:secret")
                .build());

        return opts;
    }

    private static Options deployOptions() {
        Options opts = new Options();

        opts.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Increase verbosity")
                .build());

        opts.addOption(Option.builder("vv")
                .longOpt("very-verbose")
                .desc("Increase verbosity more")
                .build());

        opts.addOption(Option.builder("p")
                .longOpt("profile")
                .hasArg()
                .argName("profile_name")
                .desc("Name of the profile set in " + ConfigManager.DEFAULT_LOCATION + ", overrides --server, --auth and --servernames")
                .build());

        opts.addOption(Option.builder("s")
                .longOpt("server")
                .hasArg()
                .argName("server_url")
                .desc("URL of the server, e.g. https://example.org:9043")
                .build());

        opts.addOption(Option.builder("a")
                .longOpt("auth")
                .hasArg()
                .valueSeparator(':')
                .argName("user:password")
                .desc("WAS username and password, e.g. wsadmin:secret")
                .build());

        opts.addOption(Option.builder("g")
                .longOpt("gui")
                .desc("the browser is shown, by the default it is run headless")
                .build());

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

        opts.addOption(Option.builder("c")
                .longOpt("contextroot")
                .hasArg()
                .argName("context_root")
                .desc("context-root (if not supplied the name of the application is used)")
                .build());

        opts.addOption(Option.builder("n")
                .longOpt("name")
                .hasArg()
                .argName("app_name")
                .desc("name of the application (if not supplied the name it is derived from the war filename")
                .build());

        opts.addOption(Option.builder("sn")
                .longOpt("servernames")
                .argName("server_name")
                .hasArg()
                .desc("Java regex of the server where the WAR will be installed, e.g. (.*AppCluster.*|.*webserver.*)")
                .build());

        return opts;
    }
}
