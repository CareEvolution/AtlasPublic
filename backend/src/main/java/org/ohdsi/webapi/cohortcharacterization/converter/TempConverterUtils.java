package org.ohdsi.webapi.cohortcharacterization.converter;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TempConverterUtils {
    private ConversionService conversionService;

    public TempConverterUtils(@Lazy ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public <S, R> List<R> convertList(List<S> source, Class<R> targetClass) {
        return convertList(source, targetClass, null);
    }

    public <S, T> Set<T> convertSet(Collection<S> source, Class<T> targetClass) {
        return convertCollection(source, targetClass, null, Collectors.toSet());
    }

    public <S, T> List<T> convertList(List<S> source, Class<T> targetClass, Comparator<T> comparator) {
        return convertCollection(source, targetClass, comparator, Collectors.toList());
    }

    public <S, T, A, R> R convertCollection(Collection<S> source, Class<T> targetClass, Comparator<T> comparator, Collector<T, A, R> collector) {
        Stream<T> intermediateStream = source.stream()
                .map(item -> conversionService.convert(item, targetClass));
        if (comparator != null) {
            intermediateStream = intermediateStream.sorted(comparator);
        }
        return intermediateStream.collect(collector);
    }
}

