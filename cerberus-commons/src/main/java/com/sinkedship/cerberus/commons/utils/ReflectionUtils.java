package com.sinkedship.cerberus.commons.utils;

import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Derrick Guan
 */
public class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static <T extends Annotation> Set<Class<?>> getEffectiveClassByAnnotation(
            Class<?> type, Class<T> annotation) {
        // If current class is directly annotated, it is considered the only annotation
        if (type.isAnnotationPresent(annotation)) {
            return ImmutableSet.of(type);
        }
        // otherwise find all the annotations from super classes and interfaces
        ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
        addEffectiveClassByAnnotation(type, annotation, builder);
        return builder.build();
    }

    private static <T extends Annotation> void addEffectiveClassByAnnotation(
            Class<?> type, Class<T> annotation, ImmutableSet.Builder<Class<?>> builder) {
        if (type.isAnnotationPresent(annotation)) {
            builder.add(type);
            return;
        }
        if (type.getSuperclass() != null) {
            addEffectiveClassByAnnotation(type.getSuperclass(), annotation, builder);
        }
        for (Class<?> anInterface : type.getInterfaces()) {
            addEffectiveClassByAnnotation(anInterface, annotation, builder);
        }
    }
}
