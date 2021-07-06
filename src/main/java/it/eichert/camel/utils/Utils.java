package it.eichert.camel.utils;

import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.Map;

public class Utils {

    private Utils() {
        /*utility class*/
    }

    public static <T> T bindProperties(String prefix, Class<T> clazz, ApplicationContext applicationContext) {
        return Binder.get(
                applicationContext.getEnvironment(),
                new BindHandler() {
                    public Object onFailure(
                            ConfigurationPropertyName name,
                            Bindable<?> target,
                            BindContext context,
                            Exception error)
                            throws Exception {
                        Class<?> resolvedType = target.getType().resolve();
                        if (resolvedType.isAssignableFrom(Map.class)) {
                            return Collections.emptyMap();
                        } else {
                            throw error;
                        }
                    }
                })
                .bind(prefix, clazz)
                .get();
    }

}
