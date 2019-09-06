package io.github.molok.wasdeployer;

import org.junit.Test;

import static io.github.molok.wasdeployer.Main.toAppName;
import static org.assertj.core.api.Assertions.assertThat;

public class MainTest {

    @Test
    public void appName() {
        assertThat(toAppName("foo")).isEqualTo("foo");
        assertThat(toAppName("foo.war")).isEqualTo("foo");
        assertThat(toAppName("foo.ear")).isEqualTo("foo");
        assertThat(toAppName("foo-bar.war")).isEqualTo("foo-bar");
        assertThat(toAppName("FOO-bar.war")).isEqualTo("FOO-bar");
        assertThat(toAppName("war.war")).isEqualTo("war");
        assertThat(toAppName("foo-bar.war")).isEqualTo("foo-bar");
        assertThat(toAppName("certificazionemorosita-api-0.0.8.war")).isEqualTo("certificazionemorosita-api");
    }

}
