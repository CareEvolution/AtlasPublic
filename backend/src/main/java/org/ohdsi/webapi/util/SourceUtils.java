package org.ohdsi.webapi.util;

import org.apache.commons.lang3.ObjectUtils;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceDaimonEntity;

import static org.ohdsi.webapi.source.SourceDaimonEntity.DaimonType.CDM;
import static org.ohdsi.webapi.source.SourceDaimonEntity.DaimonType.Results;
import static org.ohdsi.webapi.source.SourceDaimonEntity.DaimonType.Temp;
import static org.ohdsi.webapi.source.SourceDaimonEntity.DaimonType.Vocabulary;

public class SourceUtils {
    public static String getVocabularyQualifier(SourceEntity source) {
        return source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
    }

    public static String getCdmQualifier(SourceEntity source) {
        return source.getTableQualifier(CDM);
    }

    public static String getTempQualifier(SourceEntity source, String backup) {
        return ObjectUtils.firstNonNull(source.getTableQualifierOrNull(Temp), backup);
    }

    public static String getTempQualifier(SourceEntity source) {
        return ObjectUtils.firstNonNull(source.getTableQualifierOrNull(Temp), getResultsQualifier(source));
    }

    public static String getTempQualifierOrNull(SourceEntity source) {
        return source.getTableQualifierOrNull(Temp) != null
                ? source.getTableQualifierOrNull(Temp)
                : getResultsQualifierOrNull(source);
    }

    public static String getResultsQualifier(SourceEntity source) {
        return source.getTableQualifier(Results);
    }

    public static String getResultsQualifierOrNull(SourceEntity source) {
        return source.getTableQualifierOrNull(Results);
    }

    public static String getVocabQualifierOrNull(SourceEntity source) {
        return source.getTableQualifierOrNull(Vocabulary);
    }

    public static boolean hasSourceDaimon(SourceEntity source, SourceDaimonEntity.DaimonType daimonType) {
        boolean result = source.getDaimons().stream().anyMatch(d -> daimonType.equals(d.getDaimonType()));
        if (!result && Vocabulary.equals(daimonType)) {
            result = hasSourceDaimon(source, CDM);
        }
        return result;
    }
}
