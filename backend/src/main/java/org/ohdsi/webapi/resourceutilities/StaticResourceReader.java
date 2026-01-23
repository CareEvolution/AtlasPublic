package org.ohdsi.webapi.resourceutilities;

import org.ohdsi.webapi.exceptions.FatalException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class StaticResourceReader {
    public static String getText(String resourcePath) {
        if (!StringUtils.hasText(resourcePath)) {
            throw new IllegalArgumentException("Resource path is empty");
        }

        var resource = new ClassPathResource(resourcePath);
        try {
            var s = resource.getInputStream();
            var text = StreamUtils.copyToString(s, Charset.defaultCharset());
            s.close();
            return text;
        } catch (IOException e) {
            throw new FatalException("Cannot read resource " + resourcePath);
        }
    }
}
