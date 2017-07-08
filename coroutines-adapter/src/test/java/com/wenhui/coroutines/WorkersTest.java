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
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class WorkersTest {

    @Test
    public void testCreateBackgroundWorks_multipleActions() throws Exception {
        final AtomicReference<Integer> got = new AtomicReference<>();
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

        assertThat(got.get(), nullValue());

        doneSignal.await(1000, TimeUnit.MILLISECONDS);
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

        doneSignal.await(1000, TimeUnit.MILLISECONDS);
        Robolectric.flushForegroundThreadScheduler();

        assertThat(got.get(), equalTo("Merge 100"));
    }
}
