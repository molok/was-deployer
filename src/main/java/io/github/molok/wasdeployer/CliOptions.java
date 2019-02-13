package io.github.molok.wasdeployer;

import org.apache.commons.cli.Option;

public class CliOptions {
    public static final Option AUTH = Option.builder("a")
            .longOpt("auth")
            .hasArg()
            .valueSeparator(':')
            .argName("user:password")
            .desc("WAS username and password, e.g. wsadmin:secret")
            .build();

    public static final Option SERVER = Option.builder("s")
            .longOpt("server")
            .hasArg()
            .argName("server_url")
            .desc("URL of the server, e.g. https://example.org:9043")
            .build();

    public static final Option PROFILE = Option.builder("p")
            .longOpt("profile")
            .hasArg()
            .argName("profile_name")
            .desc("Name of the profile set in " + ConfigManager.DEFAULT_LOCATION + ", overrides --server, --auth")
            .build();

    public static final Option VERY_VERBOSE = Option.builder("vv")
            .longOpt("very-verbose")
            .desc("Increase verbosity more")
            .build();

    public static final Option VERBOSE = Option.builder("v")
            .longOpt("verbose")
            .desc("Increase verbosity")
            .build();

    public static final Option PROFILE_DEPLOY = Option.builder("p")
            .longOpt("profile")
            .hasArg()
            .argName("profile_name")
            .desc("Name of the profile set in " + ConfigManager.DEFAULT_LOCATION + ", overrides --server, --auth and --servernames")
            .build();

    public static final Option APP_NAME = Option.builder("n")
            .longOpt("name")
            .hasArg()
            .argName("app_name")
            .desc("name of the application, if not supplied all installed applications will be listed")
            .build();

    public static final Option APP_NAME_LIFECYCLE = Option.builder("n")
            .longOpt("name")
            .hasArg()
            .required()
            .argName("app_name")
            .desc("name of the application, if not supplied all installed applications will be listed")
            .build();

    public static final Option GUI = Option.builder("g")
            .longOpt("gui")
            .desc("the browser is shown, by the default it is run headless")
            .build();

    public static final Option INSTALL = Option.builder("i")
            .longOpt("install")
            .desc("install the application")
            .build();

    public static final Option UPDATE = Option.builder("u")
            .longOpt("update")
            .desc("update the application")
            .build();

    public static final Option REMOTE_FILE = Option.builder("r")
            .longOpt("remote")
            .hasArg()
            .argName("war_remote_path")
            .desc("remote location of the WAR to deploy")
            .build();

    public static final Option CONTEXT_ROOT = Option.builder("c")
            .longOpt("contextroot")
            .hasArg()
            .argName("context_root")
            .desc("context-root (if not supplied the name of the application is used)")
            .build();

    public static final Option APP_NAME_DEPLOY = Option.builder("n")
            .longOpt("name")
            .hasArg()
            .argName("app_name")
            .desc("name of the application (if not supplied the name it is derived from the war filename)")
            .build();

    public static final Option SERVER_NAMES = Option.builder("sn")
            .longOpt("servernames")
            .argName("server_name")
            .hasArg()
            .desc("Java regex of the server where the WAR will be installed, e.g. (.*AppCluster.*|.*webserver.*)")
            .build();

    public static final Option LOCAL_FILE = Option.builder("f")
            .longOpt("file")
            .hasArg()
            .argName("war_path")
            .desc("location of the WAR to deploy, it needs to be accessible by the server")
            .build();
}
