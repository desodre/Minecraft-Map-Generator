package org.learn.minecraftmap.generator.pool;

import com.sun.jna.Pointer;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CubiomesPoolConfig {

    @Bean(destroyMethod = "close")
    public GenericObjectPool<Pointer> cubiomesGeneratorPool() {
        CubiomesGeneratorFactory factory = new CubiomesGeneratorFactory();
        GenericObjectPoolConfig<Pointer> config = new GenericObjectPoolConfig<>();

        // Bounded by number of CPU cores on the host machine to optimize concurrent CPU scaling
        int cores = Runtime.getRuntime().availableProcessors();
        config.setMaxTotal(cores);
        config.setMaxIdle(cores);
        config.setMinIdle(Math.max(1, cores / 4)); // Keep at least some idle generators ready

        // If the pool is exhausted under heavy load, block up to 5 seconds before throwing an exception
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofMillis(5000));

        return new GenericObjectPool<>(factory, config);
    }
}
