package org.codingmatters.poom.ci.dependency.api.service.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class WithLock<T, R> implements Function<T, R> {

    static private Map<Object, Lock> locks = Collections.synchronizedMap(new HashMap<>());

    static public synchronized <T, R> Function<T, R> locked(Function<T, R> function, Object on) {
        Lock lock = locks.getOrDefault(on, new ReentrantLock(true));
        return new WithLock<>(lock, function);
    }

    private final Lock lock;
    private final Function<T, R> deleguate;

    private WithLock(Lock lock, Function<T, R> deleguate) {
        this.lock = lock;
        this.deleguate = deleguate;
    }

    @Override
    public R apply(T t) {
        this.lock.lock();
        try {
            return this.deleguate.apply(t);
        } finally {
            this.lock.unlock();
        }
    }
}
