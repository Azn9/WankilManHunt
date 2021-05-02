package dev.azn9.wankilhunter.util.reflect;

import com.google.common.base.Preconditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author roro1506_HD
 */
public class ReflectionUtil {

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target - lookup name of the class.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, String name) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(name);

        try {
            Field field = target.getDeclaredField(name);
            field.setAccessible(true);

            return new FieldAccessor<T>() {

                @Override
                @SuppressWarnings("unchecked")
                public T get(Object target) {
                    try {
                        return (T) field.get(target);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Cannot access reflection.", e);
                    }
                }

                @Override
                public void set(Object target, T value) {
                    try {
                        field.set(target, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Cannot access reflection.", e);
                    }
                }

                @Override
                public boolean hasField(Object target) {
                    // target instanceof DeclaringClass
                    return field.getDeclaringClass().isAssignableFrom(target.getClass());
                }
            };
        } catch (Exception ignored) {
        }

        // Search in parent classes
        if (target.getSuperclass() != null) {
            return getField(target.getSuperclass(), name);
        }

        throw new IllegalArgumentException("Cannot find field with name " + name);
    }

    /**
     * Search for the first publically and privately defined constructor of the given name and parameter count.
     *
     * @param clazz  - a class to start with.
     * @param params - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static ConstructorInvoker getConstructor(Class<?> clazz, Class<?>... params) {
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), params)) {
                constructor.setAccessible(true);

                return arguments -> {
                    try {
                        return constructor.newInstance(arguments);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                    }
                };
            }
        }

        throw new IllegalStateException(String.format("Unable to find constructor for %s (%s).", clazz, Arrays.asList(params)));
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param returnType - the expected return type, or NULL to ignore.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static <T, R> MethodInvoker<T, R> getTypedMethod(Class<? super T> clazz, String methodName, Class<? super R> returnType, Class<?>... params) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if ((methodName == null || method.getName().equals(methodName)) && (returnType == null || method.getReturnType().equals(returnType)) && Arrays.equals(method.getParameterTypes(), params)) {
                method.setAccessible(true);

                return (target, arguments) -> {
                    try {
                        // noinspection unchecked
                        return (R) method.invoke(target, arguments);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot invoke method " + method, e);
                    }
                };
            }
        }

        // Search in every superclass
        if (clazz.getSuperclass() != null) {
            return getTypedMethod(clazz.getSuperclass(), methodName, returnType, params);
        }

        throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
    }
}
