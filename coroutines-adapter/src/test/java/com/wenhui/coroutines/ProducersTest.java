package com.wenhui.coroutines;


import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ProducersTest {

    @Test
    public void testConsumeBy() throws Exception {
        final AtomicReference<String> got = new AtomicReference<>();
        final CountDownLatch doneSignal = new CountDownLatch(1);

        Producer<Integer> producer = Producers.consumeBy(new Function1<Integer, String>() {
            @Override
            public String invoke(Integer integer) {
                TestUtils.sleep(200);
                return String.valueOf(integer);
            }
        }).transform(new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                return "Consume " + s;
            }
        }).onSuccess(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                assertThat(got.get(), nullValue());
                got.set(s);
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get(), nullValue());

        for (int i = 0; i < 10; i++) {
            producer.produce(i);
        }

        doneSignal.await(1, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        assertThat(got.get(), equalTo("Consume 9"));
    }

    @Ignore
    @Test
    public void testConsumeByPool() throws Exception {
        final int count = 10;
        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch doneSignal = new CountDownLatch(count);
        final AtomicReference<ArrayList<String>> got = new AtomicReference<>(new ArrayList<String>());

        Producer<Integer> producer = Producers.consumeByPool(new Function1<Integer, String>() {
            @Override
            public String invoke(Integer integer) {
                TestUtils.sleep(100);
                return String.valueOf(integer);
            }
        }).transform(new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                return "Consume " + s;
            }
        }).onSuccess(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String s) {
                got.get().add(s);
                counter.incrementAndGet();
                doneSignal.countDown();
                return Unit.INSTANCE;
            }
        }).start();

        assertThat(got.get().isEmpty(), is(true));
        for (int i = 0; i < 10; i++) {
            producer.produce(i);
        }

        doneSignal.await(10, TimeUnit.SECONDS);
        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();

        assertThat(counter.get(), equalTo(count));
        final ArrayList<String> gots = got.get();
        assertThat(gots.size(), equalTo(count));

        for (int i = 0; i < count; i++) {
            assertThat(gots, hasItem("Consume " + i));
        }
    }
}
