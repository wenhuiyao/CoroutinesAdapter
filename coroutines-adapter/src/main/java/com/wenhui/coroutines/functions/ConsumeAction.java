package com.wenhui.coroutines.functions;

/**
 * A functional interface that consume a single item
 *
 * NOTE: this is mainly use to bypass Kotlin interface that returns {@link kotlin.Unit}
 * @param <T>
 */
public interface ConsumeAction<T> {
    void invoke(T item) throws Exception;
}
