package br.com.hahn.toxicbet.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class AuthWebClientConfig {

    @Bean("authWebClient")
    public WebClient authWebClient(AuthIntegrationProperties properties) {
        DnsAddressResolverGroup dnsResolver = new DnsAddressResolverGroup(
                new DnsNameResolverBuilder()
                        .queryTimeoutMillis(5000)
                        .negativeTtl(10)
                        .ttl(60, 300)
        );

        ConnectionProvider connectionProvider = ConnectionProvider.builder("auth-connection-pool")
                .maxConnections(100)
                .maxIdleTime(java.time.Duration.ofMinutes(30))
                .pendingAcquireMaxCount(1000)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .resolver(dnsResolver)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(java.time.Duration.ofSeconds(30))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                );

        log.info("AuthWebClient initialized with baseUrl: {}", properties.getBaseUrl());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
