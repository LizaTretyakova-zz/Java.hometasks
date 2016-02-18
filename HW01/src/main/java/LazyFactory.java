import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

public class LazyFactory {

    public static final Object NONE = new Object();

    public static class LazyOneThread<T> implements Lazy<T> {
        private T result;
        private Supplier<T> supplier;

        public LazyOneThread(Supplier<T> s) {
            supplier = s;
            result = (T)NONE;
        }

        @Override
        public T get() {
            if(result == NONE) {
                result = supplier.get();
                supplier = null;
            }
            return result;
        }
    }


    public static <T> Lazy<T> createLazyOneThread(Supplier<T> supplier) {
        return new LazyOneThread<T>(supplier);
    }

    public static class LazyMultithread<T> implements Lazy<T> {

        private volatile T result;
        private Supplier<T> supplier;

        public LazyMultithread (Supplier<T> s) {
            supplier = s;
            result = (T)NONE;
        }

        @Override
        public T get() {
            if(result == NONE) {
                    synchronized (this) {
                        if(result == NONE) {
                            result = supplier.get();
                            supplier = null;
                        }
                    }
                }
            return result;
        }
    }

    public static <T> Lazy<T> createLazyMultithread(Supplier<T> supplier) {
        return new LazyMultithread<T>(supplier);
    }

    public static class LazyLockfree<T> implements Lazy<T> {

        private static final AtomicReferenceFieldUpdater<LazyLockfree, Object> updater =
                AtomicReferenceFieldUpdater.newUpdater(LazyLockfree.class, Object.class, "result");
        private volatile T result;
        private Supplier<T> supplier;

        public LazyLockfree(Supplier<T> s) {
            supplier = s;
            result = (T)NONE;
        }

        @Override
        public T get() {
            if(result == NONE) {
                Supplier<T> local = supplier;
                if(local != null) {
                    if(updater.compareAndSet(this, NONE, local.get())) {
                        supplier = null;
                    }
                }
            }
            return result;
        }
    }

    public static <T> Lazy<T> createLazyLockfree(Supplier<T> supplier) {
        return new LazyLockfree<>(supplier);
    }
}
