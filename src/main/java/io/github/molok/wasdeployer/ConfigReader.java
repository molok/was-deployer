package io.github.molok.wasdeployer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigReader {
    static Profile profile(String profileName) throws FileNotFoundException {
        Yaml profiles = new Yaml(new Constructor(Config.class));
        Path path = Paths.get(System.getProperty("user.home") + "/.config/was-deployer.yaml");
        if (!path.toFile().isFile()) {
            path = Paths.get(System.getProperty("user.home") + "/.was-deployer.yaml");
            if (!path.toFile().isFile()) {
                throw new RuntimeException("No configuration file found");
            }
        }

        return profiles.<Config>load(new FileInputStream(path.toFile()))
                .profiles.stream()
                .filter(p -> p.name.equals(profileName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Configuration not found for profile " + profileName));
    }

    static class Config {
        public List<Profile> profiles;
        public Config() { }

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

        public Profile() {}

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
