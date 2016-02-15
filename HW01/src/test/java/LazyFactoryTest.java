import junit.framework.AssertionFailedError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.function.Supplier;

import static junit.framework.Assert.assertTrue;

public class LazyFactoryTest {

    class SampleObject {}

    class TestClass {

        public Boolean executed = false;
        Boolean executeBool() {
            Boolean result = executed;
            executed = true;
            return result;
        }

        public SampleObject executeObject() {
            return new SampleObject();
        }
    }
    final TestClass test = new TestClass();

    class MultipleExecutionsException extends RuntimeException {}

    class SupplierReturnsNull<T> implements Supplier<T> {

        private int cnt = 0;

        @Override
        public T get() {
            if(cnt > 0) {
                throw new MultipleExecutionsException();
            }
            cnt++;
            return null;
        }
    }

    Supplier<Integer> testSupplier = new Supplier<Integer>() {

        private int cnt = 0;

        @Override
        public Integer get() {
            if(cnt > 0) {
                throw new RuntimeException();
            }
            else {
                cnt++;
            }
            return cnt;
        }
    };

    abstract class TestRunnable implements Runnable {
        MultipleExecutionsException exception = null;
    }


    private <T> void testOneThread(Lazy<T> lazy, T result, int cnt) {
        for(int i = 0; i < cnt; i++) {
            assertTrue(result == lazy.get());
        }
    }

    private <T> void testMultithread(final Lazy<T> lazy, final T result, int cnt) throws Exception {
        ArrayList<Thread> testExecutors = new ArrayList<>();
        ArrayList<TestRunnable> testRunnables = new ArrayList<>();

        for(int i = 0; i < cnt; i++) {
            testRunnables.add(new TestRunnable() {

                @Override
                public void run() {
                    try {
                        assertTrue(result == lazy.get());
                    } catch (MultipleExecutionsException e) {
                        exception = e;
                    }
                }
            });
            Thread executorThread = new Thread(testRunnables.get(i));
            testExecutors.add(executorThread);
            executorThread.start();
        }
        for(int i = 0; i < cnt; i++) {
            testExecutors.get(i).join();
            if(testRunnables.get(i).exception != null) {
                throw testRunnables.get(i).exception;
            }
        }
    }

    @Test
    public void testCreateLazyOneThreadBool() throws Exception {
        Lazy<Boolean> lazy = LazyFactory.createLazyOneThread(test::executeBool);
        testOneThread(lazy, lazy.get(), 10);
    }

    @Test
    public void testCreateLazyOneThreadObject() throws Exception {
        Lazy<SampleObject> lazy = LazyFactory.createLazyOneThread(test::executeObject);
        testOneThread(lazy, lazy.get(), 10);
    }

    @Test
    public void testCreateLazyMultithread() throws Exception {
        final Lazy<Boolean> lazy = LazyFactory.createLazyMultithread(test::executeBool);
        final Lazy<SampleObject> lazySampleObject = LazyFactory.createLazyMultithread(test::executeObject);
        testMultithread(lazy, lazy.get(), 10);
        testMultithread(lazySampleObject, lazySampleObject.get(), 10);
    }

    @Test
    public void testCreateLazyLockfree() throws Exception {
        final Lazy<Boolean> lazy = LazyFactory.createLazyLockfree(test::executeBool);
        final Lazy<SampleObject> lazySampleObject = LazyFactory.createLazyLockfree(test::executeObject);
        testMultithread(lazy, lazy.get(), 10);
        testMultithread(lazySampleObject, lazySampleObject.get(), 10);
    }

    @Test
    public void testCreateLazyOneThreadOneExecution() throws Exception {
        final Lazy<Integer> lazy = LazyFactory.createLazyOneThread(testSupplier);
        testOneThread(lazy, lazy.get(), 10);
    }

    @Test
    public void testCreateLazyMultithreadOneExecution() throws Exception {
        final Lazy<Integer> lazy = LazyFactory.createLazyMultithread(testSupplier);
        testMultithread(lazy, lazy.get(), 10);
    }

    @Test
    public void testSupplierReturnsNull() throws Exception {
        final Lazy<SampleObject> lazyOneThread =
                LazyFactory.createLazyOneThread(new SupplierReturnsNull<>());
        final Lazy<SampleObject> lazyMultithread =
                LazyFactory.createLazyMultithread(new SupplierReturnsNull<>());
        testOneThread(lazyOneThread, lazyOneThread.get(), 10);
        testMultithread(lazyMultithread, lazyMultithread.get(), 10);
    }

    @Test(expected = MultipleExecutionsException.class)
    public void testSupplierReturnsNullLockfree() throws Exception {
        final Lazy<SampleObject> lazy =
                LazyFactory.createLazyLockfree(new SupplierReturnsNull<>());
        testMultithread(lazy, lazy.get(), 10);
    }
}