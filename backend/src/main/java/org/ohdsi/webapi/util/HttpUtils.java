package org.ohdsi.webapi.util;

import java.io.ByteArrayOutputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class HttpUtils {
    public static ResponseEntity<ByteArrayResource> respondBinary(ByteArrayOutputStream stream, String filename) {
            var data = stream.toByteArray();

            var resource = new ByteArrayResource(data);

            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length));

            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
