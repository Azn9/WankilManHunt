package dev.azn9.wankilhunter.util.reflect;

/**
 * An interface for invoking a specific method.
 */
public interface MethodInvoker<T, R> {
    /**
     * Invoke a method on a specific target object.
     *
     * @param target    - the target object, or NULL for a static method.
     * @param arguments - the arguments to pass to the method.
     * @return The return value, or NULL if is void.
     */
    R invoke(T target, Object... arguments);
}