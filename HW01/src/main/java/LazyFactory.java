import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

public class LazyFactory {
    public static <T> Lazy<T> createLazyOneThread(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private boolean executed = false;// is needed here since supplier can return null
            private T result;

            public T get() {
                if(!executed) {
                    result = supplier.get();
                    executed = true;
                }
                return result;
            }
        };
    }

    public static <T> Lazy<T> createLazyMultithread(final Supplier<T> supplier) {
        return new Lazy<T>() {
            private boolean executed = false;// is needed here since supplier can return null
            private T result;// could be volatile, but not essentially

            public T get() {
                synchronized(this) {
                    if(!executed) {
                        synchronized (this) {
                            if(!executed) {
                                result = supplier.get();
                                executed = true;
                            }
                        }
                    }
                    return result;
                }
            }
        };
    }

    public static <T> Lazy<T> createLazyLockfree(final Supplier<T> supplier) {
        class LazyLockfree<T> implements Lazy {

            private volatile T result = null;
            //cannot be placed outside becuase of T
            private AtomicReferenceFieldUpdater<LazyLockfree, Object> resultUpdater =
                    AtomicReferenceFieldUpdater.newUpdater(LazyLockfree.class, Object.class, "result");

            public T get() {
                resultUpdater.compareAndSet(this, null, supplier.get());
                return result;
            }
        }

        return new LazyLockfree<T>();
    }

}
