package com.wenhui.coroutines;


import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class WorkersTest {

    @Test
    public void testCreateBackgroundWork_simpleAction() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        Workers.createBackgroundWork(
                new Function0<Integer>() {
                    @Override
                    public Integer invoke() {
                        TestUtils.sleep(100);
                        return 1000;
                    }
                }
        ).transform(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer integer) {
                return integer + 100;
            }
        }).onSuccess(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer integer) {
                got.set(integer);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), equalTo(0));

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get(), equalTo(1100));
    }

    @Test
    public void testCreateBackgroundWork_simpleAction_cancelByhWorkManager() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        final WorkManager workManager = new WorkManager();

        Workers.createBackgroundWork(
                new Function0<Integer>() {
                    @Override
                    public Integer invoke() {
                        TestUtils.sleep(100);
                        return 1000;
                    }
                }
        ).transform(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer integer) {
                return integer + 100;
            }
        }).onSuccess(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer integer) {
                got.set(integer);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start().manageBy(workManager);

        assertThat(got.get(), equalTo(0));
        assertThat(workManager.hasActiveWork(), is(true));
        workManager.cancelAllWorks();

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(workManager.hasActiveWork(), is(false));
        assertThat(got.get(), equalTo(0));
    }

    @Test
    public void testCreateBackgroundWork_multipleWorks_cancelByhWorkManager() throws Exception {
        final int count = 10;
        final CountDownLatch doneSignal = new CountDownLatch(count);
        final WorkManager workManager = new WorkManager();
        final AtomicInteger counter = new AtomicInteger(0);

        for(int i = 0; i < count; i++) {
            Workers.createBackgroundWork(
                    new Function0<Integer>() {
                        @Override
                        public Integer invoke() {
                            TestUtils.sleep(200);
                            counter.incrementAndGet();
                            return 1000;
                        }
                    }
            ).onSuccess(new Function1<Integer, Unit>() {
                @Override
                public Unit invoke(Integer integer) {
                    doneSignal.countDown();
                    return null;
                }
            }).start().manageBy(workManager);
        }

        assertThat(counter.get(), is(0));
        assertThat(workManager.hasActiveWork(), is(true));

        // deliberately wait for several seconds for all the job to be finished
        doneSignal.await(4, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();

        assertThat(counter.get(), is(count));
        assertThat(workManager.hasActiveWork(), is(false));
    }

    @Test
    public void testCreateBackgroundWork_actionWithParam() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        Workers.createBackgroundWork("1000",
                new Function1<String, Integer>() {
                    @Override
                    public Integer invoke(String integer) {
                        return Integer.parseInt(integer);
                    }
                }
        ).transform(new Function1<Integer, Integer>() {
            @Override
            public Integer invoke(Integer integer) {
                return integer + 100;
            }
        }).onSuccess(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer integer) {
                got.set(integer);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), equalTo(0));

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get(), equalTo(1100));
    }

    @Test
    public void testCreateBackgroundWorks_multipleActions() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        Workers.createBackgroundWorks(
                new Function0<Integer>() {
                    @Override
                    public Integer invoke() {
                        return 1000;
                    }
                }
                , new Function0<Integer>() {
                    @Override
                    public Integer invoke() {
                        TestUtils.sleep(200);
                        return 2000;
                    }
                }

        ).transform(new Function1<List<Integer>, Integer>() {
            @Override
            public Integer invoke(List<Integer> integers) {
                int result = 0;
                for (Integer integer : integers) {
                    result += integer;
                }
                return result;
            }
        }).onSuccess(new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer integer) {
                got.set(integer);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), equalTo(0));

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        assertThat(got.get(), equalTo(3000));
    }


    @Test
    public void testMergeBackgroundWorks() throws Exception {
        final CountDownLatch doneSignal = new CountDownLatch(2);
        final AtomicReference<String> got = new AtomicReference<>();
        Workers.mergeBackgroundWorks(
                new Function0<String>() {
                    @Override
                    public String invoke() {
                        return "Merge ";
                    }
                }, new Function0<Integer>() {
                    @Override
                    public Integer invoke() {
                        TestUtils.sleep(200);
                        return 100;
                    }
                }
        ).merge(new Function2<String, Integer, String>() {
            @Override
            public String invoke(String s, Integer integer) {
                return s + integer;
            }
        }).consume(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).onSuccess(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                got.set(s);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), nullValue());

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get(), equalTo("Merge 100"));
    }

    @Test
    public void testCreateBackgroundWork() throws Exception {
        final CountDownLatch doneSignal = new CountDownLatch(1);
        final AtomicReference<String> got = new AtomicReference<>();
        Workers.createBackgroundWork(new BaseExecutor<Integer>() {
            @Override
            public Integer onExecute() throws Exception {
                TestUtils.sleep(200);
                return 1000;
            }
        }).transform(CoroutineContexts.BACKGROUND, new Function1<Integer, String>() {
            @Override
            public String invoke(Integer integer) {
                return "transform " + integer;
            }
        }).onSuccess(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                got.set(s);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), nullValue());

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get(), equalTo("transform 1000"));
    }
}
