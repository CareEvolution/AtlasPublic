package org.ohdsi.webapi.util;

import org.apache.commons.io.IOUtils;

import java.io.*;

public class TempFileUtils {

    public static File copyResourceToTempFile(String resource, String prefix, String suffix) throws IOException {
        var tempFile = File.createTempFile(prefix, suffix);
        try (var in = TempFileUtils.class.getResourceAsStream(resource)) {
            try (var out = new FileOutputStream(tempFile)) {
                IOUtils.copy(in, out);
            }
        }
        return tempFile;
    }
}