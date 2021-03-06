package uk.gov.hmcts.ccd.sdk;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import net.jodah.typetools.TypeResolver;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import uk.gov.hmcts.ccd.sdk.generator.*;
import uk.gov.hmcts.ccd.sdk.types.CCDConfig;
import uk.gov.hmcts.ccd.sdk.types.Event;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigGenerator {
    private final Reflections reflections;

    public ConfigGenerator(Reflections reflections) {
        this.reflections = reflections;
    }

    public void resolveConfig(File outputFolder) {
        Set<Class<? extends CCDConfig>> configTypes =
                reflections.getSubTypesOf(CCDConfig.class).stream()
                .filter(x -> !Modifier.isAbstract(x.getModifiers())).collect(Collectors.toSet());

        if (configTypes.isEmpty()) {
            throw new RuntimeException("Expected at least one CCDConfig implementation but none found ");
        }

        for (Class<? extends CCDConfig> configType : configTypes) {
            Objenesis objenesis = new ObjenesisStd();
            CCDConfig config = objenesis.newInstance(configType);
            ResolvedCCDConfig resolved = resolveConfig(config);
            File destination = Strings.isNullOrEmpty(resolved.environment) ? outputFolder : new File(outputFolder, resolved.environment);
            writeConfig(destination, resolved);
        }
    }

    public ResolvedCCDConfig resolveConfig(CCDConfig config) {
        Class<?>[] typeArgs = TypeResolver.resolveRawArguments(CCDConfig.class, config.getClass());
        ConfigBuilderImpl builder = new ConfigBuilderImpl(typeArgs[0]);
        config.configure(builder);
        List<Event> events = builder.getEvents();
        Map<Class, Integer> types = resolve(typeArgs[0], getPackageName(config.getClass()));
        return new ResolvedCCDConfig(typeArgs[0], builder, events, types, builder.environment);
    }

    public void writeConfig(File outputfolder, ResolvedCCDConfig config) {
        outputfolder.mkdirs();
        CaseEventGenerator.writeEvents(outputfolder, config.builder.caseType, config.events);
        CaseEventToFieldsGenerator.writeEvents(outputfolder, config.events);
        ComplexTypeGenerator.generate(outputfolder, config.builder.caseType, config.types);
        CaseEventToComplexTypesGenerator.writeEvents(outputfolder, config.events);
        AuthorisationCaseEventGenerator.generate(outputfolder, config.events, config.builder);
        CaseFieldGenerator.generateCaseFields(outputfolder, config.builder.caseType, config.typeArg, config.events, config.builder);
        FixedListGenerator.generate(outputfolder, config.types);
    }

    // Copied from jdk 9.
    public static String getPackageName(Class<?> c) {
        String pn;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        if (c.isPrimitive()) {
            pn = "java.lang";
        } else {
            String cn = c.getName();
            int dot = cn.lastIndexOf('.');
            pn = (dot != -1) ? cn.substring(0, dot).intern() : "";
        }
        return pn;
    }

    public static Map<Class, Integer> resolve(Class dataClass, String basePackage) {
        Map<Class, Integer> result = Maps.newHashMap();
        resolve(dataClass, result, 0);
        System.out.println(result.size());
        System.out.println(basePackage);
        result = Maps.filterKeys(result, x -> getPackageName(x).startsWith(basePackage));
        return result;
    }

    private static void resolve(Class dataClass, Map<Class, Integer> result, int level) {
        for (java.lang.reflect.Field field : ReflectionUtils.getFields(dataClass)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Class c = getComplexType(dataClass, field);
            if (null != c && !c.equals(dataClass)) {
                if (!result.containsKey(c) || result.get(c) < level) {
                    result.put(c, level);
                }
                resolve(c, result, level + 1);
            }
        }
    }

    public static Class getComplexType(Class c, Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType pType = (ParameterizedType) TypeResolver.reify(field.getGenericType(), c);
            if (pType.getActualTypeArguments()[0] instanceof ParameterizedType) {
                pType = (ParameterizedType) pType.getActualTypeArguments()[0];
            }
            return (Class) pType.getActualTypeArguments()[0];
        }
        return field.getType();
    }
}
