package com.rd.backend.config;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    /**
     * apiKey, 用于调用ai接口
     */
    private String apiKey;

    private int connectTimeout = 30;     // 连接超时时间（秒）
    private int readTimeout = 120;       // 读取超时时间（秒）
    private int writeTimeout = 60;       // 写入超时时间（秒）
    private int requestTimeOut = 120;

    @PostConstruct
    public void init() {
        log.info("AI Configuration Loaded: apiKey={}, connectTimeout={}, readTimeout={}, writeTimeout={}",
                // 为了安全，不打印完整的 apiKey
                apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, 4) + "****" : "N/A",
                connectTimeout, readTimeout, writeTimeout);
    }

    @Bean
    public ClientV4 getClientV4() {
        // 使用智谱AI SDK提供的 .networkConfig() 方法来设置超时
        return new ClientV4.Builder(apiKey)
                .networkConfig(
                        requestTimeOut,
                        connectTimeout,
                        readTimeout,
                        writeTimeout,
                        TimeUnit.SECONDS
                )
                .build();
    }
}
