package io.github.molok.wasdeployer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigManager {
    public static final String DEFAULT_LOCATION = System.getProperty("user.home") + "/.config/was-deployer.yaml";

    public static void configSample() {
        System.out.println(
            "# edit and put this in " + DEFAULT_LOCATION + "\n" +
            "profiles:\n" +
            "    - name: \"dev\"\n" +
            "      user: \"wsadmin\"\n" +
            "      password: \"secret\"\n" +
            "      server: \"https://localhost:9443\" \n" +
            "      serverNames: \"\"\n" +
            "\n" +
            "    - name: \"test\"\n" +
            "      user: \"wsadmin\"\n" +
            "      password: \"secret\"\n" +
            "      server: \"https://example.org:10001\" \n" +
            "      serverNames: \"(.*AppCluster.*|.*webserver.*)\"\n"
        );
    }

    static Profile readProfile(String profileName) {
        Yaml profiles = new Yaml(new Constructor(Config.class));
        Path path = Paths.get(DEFAULT_LOCATION);
        if (!path.toFile().isFile()) {
            throw new RuntimeException("No configuration file found");
        }

        try {
            return profiles.<Config>load(new FileInputStream(path.toFile()))
                    .profiles.stream()
                    .filter(p -> p.name.equals(profileName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Configuration not found for profile " + profileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class Config {
        public List<Profile> profiles;

        public Config() {
        }

        @Override
        public String toString() {
            return "Config{" + "profiles=" + profiles + '}';
        }
    }

    static class Profile {
        public String name;
        public String user;
        public String password;
        public String server;
        public String serverNames;

        public Profile() {
        }

        @Override
        public String toString() {
            return "Profile{" +
                    "name='" + name + '\'' +
                    ", user='" + user + '\'' +
                    ", password='" + password + '\'' +
                    ", server='" + server + '\'' +
                    ", serverNames='" + serverNames + '\'' +
                    '}';
        }
    }

}
