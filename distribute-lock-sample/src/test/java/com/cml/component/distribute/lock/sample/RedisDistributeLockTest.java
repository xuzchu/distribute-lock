package com.cml.component.distribute.lock.sample;


import com.cml.component.distribute.lock.core.DistributeLockService;
import com.cml.component.distribute.lock.sample.service.LockTestService;
import com.cml.component.distribute.lock.starter.DistributeLockAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessorRegistrar;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {"spring.config.location = classpath:application-redis.yaml"})
@EnableConfigurationProperties
@EnableAspectJAutoProxy
@ContextConfiguration(classes = {LockTestService.class,
        LockListener.class,
        PropertyPlaceholderAutoConfiguration.class,
        PropertySourcesPlaceholderConfigurer.class,
        ConfigurationPropertiesBindingPostProcessorRegistrar.class,
        DistributeLockAutoConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class RedisDistributeLockTest {

    @Autowired
    private LockTestService lockTestService;

    @Autowired
    private Redisson redisson;
    @Autowired
    private DistributeLockService distributeLockService;
    @Autowired
    private LockListener lockListener;

    @Test
    public void testLock() {
        String key = "testKey";

        String result = lockTestService.testLock(key);

        assert "getLockSuccess".equals(result);
    }

    @Test
    public void testManualLock() {
        String key = "testKey";

        String result = lockTestService.testLock2(key);

        assert "getLockSuccess".equals(result);
    }

    @Test
    public void testMultiThreadLock() throws InterruptedException {
        String key = "testKey";

        int sampleCount = 3;
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failCounter = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(sampleCount);

        CountDownLatch countDownLatch = new CountDownLatch(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            executorService.submit(() -> {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                    String result = lockTestService.testLock(key);
                    lockTestService.testLock(key);
                    successCounter.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    failCounter.incrementAndGet();
                }
            });
        }

        System.out.println("--------------execute end-----------");
        executorService.shutdown();

        try {
            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert successCounter.get() == 1;
        assert failCounter.get() == sampleCount - 1;
    }

}
