package com.eazybytes.gatewayserver;

import org.bouncycastle.oer.its.ieee1609dot2.basetypes.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
		return routeLocatorBuilder.routes()
						.route(p -> p
								.path("/eazybank/accounts/**")
								.filters( f -> f.rewritePath("/eazybank/accounts/(?<segment>.*)","/${segment}")
										.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
										.circuitBreaker(config -> config.setName("accountsCircuitBreaker")
												.setFallbackUri("forward:/contactSupport")))
								.uri("lb://ACCOUNTS"))
					.route(p -> p
							.path("/eazybank/loans/**")
							.filters( f -> f.rewritePath("/eazybank/loans/(?<segment>.*)","/${segment}")
									.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
									.retry(retryConfig -> retryConfig.setRetries(3).setMethods(HttpMethod.GET)
											.setBackoff(java.time.Duration.ofMillis(100), java.time.Duration.ofMillis(1000),2,true)))

								.uri("lb://LOANS"))
					.route(p -> p
							.path("/eazybank/cards/**")
							.filters( f -> f.rewritePath("/eazybank/cards/(?<segment>.*)","/${segment}")
									.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
									.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(useKeyResolver())))
							.uri("lb://CARDS")).build();


	}


	@Bean
	public RedisRateLimiter redisRateLimiter(){
		return new RedisRateLimiter(1,1,1);
	}

	@Bean
	KeyResolver useKeyResolver(){
		return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("name")).defaultIfEmpty("anonymous");

	}

}
