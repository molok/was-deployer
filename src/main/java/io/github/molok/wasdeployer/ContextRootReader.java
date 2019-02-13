package io.github.molok.wasdeployer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ContextRootReader {
    static public Optional<String> getContextRoot(String path) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(path);
            ZipEntry webExt = zipFile.getEntry("WEB-INF/ibm-web-ext.xml");
            if(webExt != null) {
                InputStream webExtContent = zipFile.getInputStream(webExt);

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(webExtContent);

                Element contextRoot = (Element) doc.getElementsByTagName("context-root").item(0);
                String uri = contextRoot.getAttribute("uri");
                uri = uri.startsWith("/") ? uri : "/" + uri;
                return Optional.of(uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }

}
