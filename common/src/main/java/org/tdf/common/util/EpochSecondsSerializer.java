package org.tdf.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * convert unix epoch seconds to "2011-12-03T10:15:30+01:00" as like
 */
public class EpochSecondsSerializer extends JsonSerializer<Long> {
    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(FORMATTER.format(Instant.ofEpochMilli(
            TimeUnit.MILLISECONDS.convert(value, TimeUnit.SECONDS))
        ));
    }
}
