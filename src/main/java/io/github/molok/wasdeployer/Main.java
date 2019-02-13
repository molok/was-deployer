package io.github.molok.wasdeployer;

import org.apache.commons.cli.*;
import org.openqa.selenium.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;

import static io.github.molok.wasdeployer.CliOptions.*;

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

                            ServerParams serverParams = new ServerParams(cli);

                            String warLocation = cli.getOptionValue(LOCAL_FILE.getOpt(), cli.getOptionValue(REMOTE_FILE.getOpt()));

                            String appName = cli.getOptionValue(APP_NAME_DEPLOY.getOpt(), toAppName(warLocation));
                            String contextRoot = cli.getOptionValue(CONTEXT_ROOT.getOpt(), appName);

                            new App().deploy(
                                    serverParams.server,
                                    serverParams.user,
                                    serverParams.password,
                                    warLocation,
                                    contextRoot,
                                    cli.hasOption(LOCAL_FILE.getOpt()),
                                    appName,
                                    cli.hasOption(INSTALL.getOpt()),
                                    serverParams.serverNames,
                                    cli.hasOption(GUI.getOpt()),
                                    verbosityLevel(cli)
                            );

                            return RetCode.SUCCESS.code;
                        } catch (MissingOptionException e) {
                            printUsage();
                            // FIXME
                            System.out.println("\n\nMissing required options: " + missingArguments(e, deployOptions()));
                            return RetCode.INVALID_ARGS.code;
                        }
                    }
                    case "list": {
                        try {
                            CommandLine cli = new DefaultParser().parse(listOptions(), args);

                            ServerParams serverParams = new ServerParams(cli);

                            new App().list( serverParams.server, serverParams.user, serverParams.password,
                                            verbosityLevel(cli), cli.getOptionValue(APP_NAME.getOpt()));

                            return RetCode.SUCCESS.code;
                        } catch (MissingOptionException e) {
                            printUsage();
                            System.out.println("Missing required options: " + missingArguments(e, listOptions()));
                            return RetCode.INVALID_ARGS.code;
                        }
                    }
                    case "start": {
                        CommandLine cli = new DefaultParser().parse(lifecycleOptions(), args);
                        ServerParams serverParams = new ServerParams(cli);
                        new App().start(serverParams.server, serverParams.user, serverParams.password, verbosityLevel(cli), cli.getOptionValue(APP_NAME_LIFECYCLE.getOpt()));
                        return RetCode.SUCCESS.code;
                    }
                    case "stop": {
                        CommandLine cli = new DefaultParser().parse(lifecycleOptions(), args);
                        ServerParams serverParams = new ServerParams(cli);
                        new App().stop(serverParams.server, serverParams.user, serverParams.password, verbosityLevel(cli), cli.getOptionValue(APP_NAME_LIFECYCLE.getOpt()));
                        return RetCode.SUCCESS.code;
                    }
                    case "uninstall": {
                        CommandLine cli = new DefaultParser().parse(lifecycleOptions(), args);
                        ServerParams serverParams = new ServerParams(cli);
                        new App().uninstall(serverParams.server, serverParams.user, serverParams.password, verbosityLevel(cli), cli.getOptionValue(APP_NAME_LIFECYCLE.getOpt()));
                        return RetCode.SUCCESS.code;
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
        return cli.hasOption(VERY_VERBOSE.getOpt()) ? 2 : cli.hasOption(VERBOSE.getOpt()) ? 1 : 0;
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
        hf.printHelp("java -jar was-deployer.jar deploy|start|stop|uninstall|list|config-sample [<args>]\n" +
                                 "\ndeploy",
                          "", deployOptions(), "", false);
        hf.printHelp("\nlist", "", listOptions(), "");
        hf.printHelp("\nstart", "", lifecycleOptions(), "");
        hf.printHelp("\nstop", "", lifecycleOptions(), "");
        hf.printHelp("\nuninstall", "", lifecycleOptions(), "");
        System.out.println("\nconfig-sample\n");
    }

    private static Options lifecycleOptions() {
        Options opts = new Options();

        opts.addOption(APP_NAME_LIFECYCLE);
        opts.addOption(CliOptions.VERBOSE);
        opts.addOption(VERY_VERBOSE);
        opts.addOption(PROFILE);
        opts.addOption(CliOptions.SERVER);
        opts.addOption(CliOptions.AUTH);

        return opts;
    }

    private static Options listOptions() {
        Options opts = new Options();
        opts.addOption(CliOptions.APP_NAME);
        opts.addOption(CliOptions.VERBOSE);
        opts.addOption(VERY_VERBOSE);
        opts.addOption(CliOptions.PROFILE);
        opts.addOption(CliOptions.SERVER);
        opts.addOption(CliOptions.AUTH);

        return opts;
    }

    private static Options deployOptions() {
        Options opts = new Options();

        opts.addOption(CliOptions.VERBOSE);
        opts.addOption(VERY_VERBOSE);
        opts.addOption(CliOptions.PROFILE_DEPLOY);
        opts.addOption(CliOptions.SERVER);
        opts.addOption(CliOptions.AUTH);
        opts.addOption(CliOptions.GUI);

        OptionGroup installOrUpdate = new OptionGroup();
        installOrUpdate.setRequired(true);
        installOrUpdate.addOption(CliOptions.INSTALL);
        installOrUpdate.addOption(CliOptions.UPDATE);

        opts.addOptionGroup(installOrUpdate);

        OptionGroup localOrRemote = new OptionGroup();
        localOrRemote.setRequired(true);
        localOrRemote.addOption(CliOptions.LOCAL_FILE);
        localOrRemote.addOption(CliOptions.REMOTE_FILE);

        opts.addOptionGroup(localOrRemote);

        opts.addOption(CliOptions.CONTEXT_ROOT);
        opts.addOption(CliOptions.APP_NAME_DEPLOY);
        opts.addOption(CliOptions.SERVER_NAMES);

        return opts;
    }

    private static class ServerParams {
        public String user;
        public String password;
        public String server;
        public String serverNames;

        public ServerParams(CommandLine cli) {
            if (cli.hasOption(PROFILE.getOpt())) {
                ConfigManager.Profile profile = ConfigManager.readProfile(cli.getOptionValue(PROFILE.getOpt()));
                server = profile.server;
                user = profile.user;
                password = profile.password;
                serverNames = profile.serverNames;
            } else {
                server = Objects.requireNonNull(cli.getOptionValue(SERVER.getOpt()), "--server or --profile required");
                String[] split = Objects.requireNonNull(cli.getOptionValue(AUTH.getOpt(), "--auth or --profile required"))
                                 .split(":");
                if (split.length != 2) {
                    throw new InvalidParameterException("for auth expected --auth user:password");
                }
                user = split[0];
                password = split[1];
                serverNames = cli.getOptionValue(SERVER_NAMES.getOpt(), "");
            }
        }
    }
}
