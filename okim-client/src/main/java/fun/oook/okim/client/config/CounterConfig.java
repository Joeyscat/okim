package fun.oook.okim.client.config;

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
    public Counter clientPushCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        return registry.counter(Constants.COUNTER_CLIENT_PUSH_COUNT);
    }
}
