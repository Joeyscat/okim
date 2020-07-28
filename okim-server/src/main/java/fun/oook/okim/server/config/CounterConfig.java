package fun.oook.okim.server.config;

import fun.oook.okim.common.constant.Constants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhouyu
 */
@Configuration
public class CounterConfig {

    @Bean
    public Counter serverPushCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return registry.counter(Constants.COUNTER_SERVER_PUSH_COUNT);
    }
}
