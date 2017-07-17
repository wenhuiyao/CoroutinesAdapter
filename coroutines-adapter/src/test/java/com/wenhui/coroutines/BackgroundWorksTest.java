package com.wenhui.coroutines;


import com.wenhui.coroutines.functions.ConsumeAction;
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

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class BackgroundWorksTest {

    @Test
    public void testCreateBackgroundWork_simpleAction() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        BackgroundWorks.createBackgroundWork(
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

        assertThat(got.get()).isEqualTo(0);

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get()).isEqualTo(1100);
    }

    @Test
    public void testCreateBackgroundWork_simpleAction_cancelByhWorkManager() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        final WorkManager workManager = new WorkManager();

        BackgroundWorks.createBackgroundWork(
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

        assertThat(got.get()).isEqualTo(0);
        assertThat(workManager.hasActiveWork()).isEqualTo(true);
        workManager.cancelAllWorks();

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(workManager.hasActiveWork()).isEqualTo(false);
        assertThat(got.get()).isEqualTo(0);
    }

    @Test
    public void testCreateBackgroundWork_multipleWorks_cancelByhWorkManager() throws Exception {
        final int count = 10;
        final CountDownLatch doneSignal = new CountDownLatch(count);
        final WorkManager workManager = new WorkManager();
        final AtomicInteger counter = new AtomicInteger(0);

        for(int i = 0; i < count; i++) {
            BackgroundWorks.createBackgroundWork(
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

        assertThat(counter.get()).isEqualTo(0);
        assertThat(workManager.hasActiveWork()).isEqualTo(true);

        // deliberately wait for several seconds for all the job to be finished
        doneSignal.await(4, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();

        assertThat(counter.get()).isEqualTo(count);
        assertThat(workManager.hasActiveWork()).isEqualTo(false);
    }

    @Test
    public void testCreateBackgroundWork_actionWithParam() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        BackgroundWorks.createBackgroundWork("1000",
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

        assertThat(got.get()).isEqualTo(0);

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get()).isEqualTo(1100);
    }

    @Test
    public void testCreateBackgroundWorks_multipleActions() throws Exception {
        final AtomicInteger got = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(1);
        BackgroundWorks.createBackgroundWorks(
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

        assertThat(got.get()).isEqualTo(0);

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        assertThat(got.get()).isEqualTo(3000);
    }


    @Test
    public void testMergeBackgroundWorks() throws Exception {
        final CountDownLatch doneSignal = new CountDownLatch(2);
        final AtomicReference<String> got = new AtomicReference<>();
        BackgroundWorks.mergeBackgroundWorks(
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
        }).consume(new ConsumeAction<String>() {
            @Override
            public void invoke(String s) {
                doneSignal.countDown();
            }
        }).onSuccess(new ConsumeAction<String>() {
            @Override
            public void invoke(String s) {
                got.set(s);
                doneSignal.countDown();
            }
        }).start();

        assertThat(got.get()).isNull();

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get()).isEqualTo("Merge 100");
    }

    @Test
    public void testCreateBackgroundWork() throws Exception {
        final CountDownLatch doneSignal = new CountDownLatch(1);
        final AtomicReference<String> got = new AtomicReference<>();
        BackgroundWorks.createBackgroundWork(new BaseAction<Integer>() {
            @Override
            public Integer onPerform() throws Exception {
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

        assertThat(got.get()).isNull();

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get()).isEqualTo("transform 1000");
    }
}
