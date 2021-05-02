package dev.azn9.wankilhunter.injector;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.FieldInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Injector {

    private static final Map<Class<?>, Object> CLASSES = new HashMap<>();

    public void registerInjection(Object o) {
        CLASSES.put(o.getClass(), o);
    }

    public void startInjection() {
        CLASSES.put(Injector.class, this);

        new ClassGraph().enableAllInfo().scan().getClassesWithAnnotation(ToInject.class.getName()).loadClasses().forEach(aClass -> {
            try {
                Object o = aClass.getDeclaredConstructor().newInstance();
                CLASSES.put(aClass, o);
            } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        new ClassGraph().enableAllInfo().scan().getClassesWithFieldAnnotation(Inject.class.getName()).forEach(classInfo -> {
            for (FieldInfo fieldInfo : classInfo.getDeclaredFieldInfo()) {
                if (fieldInfo.hasAnnotation(Inject.class.getName())) {
                    Field field = fieldInfo.loadClassAndGetField();
                    field.setAccessible(true);

                    if (CLASSES.containsKey(field.getType())) {
                        try {
                            field.set(null, field.getType().cast(CLASSES.get(field.getType())));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

}