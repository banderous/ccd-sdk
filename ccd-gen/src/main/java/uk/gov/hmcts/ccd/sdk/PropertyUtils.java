package uk.gov.hmcts.ccd.sdk;

import de.cronn.reflection.util.TypedPropertyGetter;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

public class PropertyUtils implements uk.gov.hmcts.ccd.sdk.types.PropertyUtils {
    @Override
    public <T, A extends Annotation> A getAnnotationOfProperty(Class<T> entityType, TypedPropertyGetter<T, ?> propertyGetter, Class<A> annotationClass) {
        return de.cronn.reflection.util.PropertyUtils.getAnnotationOfProperty(entityType, propertyGetter, annotationClass);
    }

    @Override
    public <U> Class<U> getPropertyType(Class<U> c, TypedPropertyGetter<U, ?> getter) {
        PropertyDescriptor descriptor = de.cronn.reflection.util.PropertyUtils.getPropertyDescriptor(c, getter);
        return (Class<U>) descriptor.getPropertyType();
    }

    @Override
    public <U> String getPropertyName(Class<U> c, TypedPropertyGetter<U, ?> getter) {
        return de.cronn.reflection.util.PropertyUtils.getPropertyName(c, getter);
    }
}
