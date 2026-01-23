package org.ohdsi.webapi.unittests.resourceutilities;

import static org.assertj.core.api.Assertions.*;

import org.ohdsi.webapi.exceptions.FatalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;

public class StringResourceReaderTests {
    @ParameterizedTest
    @ValueSource(strings={"No Such Resource", " ", ""})
    public void getText_GivenInvalidResourceNameAndPath_Throws(String resourceFileNameAndPath)
    {
        var stringResourceReader = new StringResourceReader();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {stringResourceReader.getText(resourceFileNameAndPath);});
    }

    @Test
    public void getText_GivenNullResourceNameAndPath_Throws()
    {
        var stringResourceReader = new StringResourceReader();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {stringResourceReader.getText(null);});
    }

    @Test
    public void getText_GivenValidResourceNameAndPath_ReturnsResource()
    {
        var stringResourceReader = new StringResourceReader();

        var text = stringResourceReader.getText("resourceutilities/SimpleResource.json");
        assertThat(text).isNotNull();
        assertThat(text).isEqualTo("{\"simple\": \"resource\"}");
    }
}
