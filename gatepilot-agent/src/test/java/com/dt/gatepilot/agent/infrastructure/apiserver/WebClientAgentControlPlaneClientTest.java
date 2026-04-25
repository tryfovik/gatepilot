package com.dt.gatepilot.agent.infrastructure.apiserver;

import com.dt.gatepilot.agent.api.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.api.properties.GatePilotAgentProperties;
import com.getboot.exception.api.exception.BusinessException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WebClient apiserver 客户端测试。
 */
class WebClientAgentControlPlaneClientTest {

    @Test
    void shouldRelyOnProvidedWebClientBuilderForTracePropagation() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClientAgentControlPlaneClient client = newClient(capturedRequest, successBody(),
                (request, next) -> next.exchange(ClientRequest.from(request)
                        .header("X-Trace-Id", "trace-from-getboot-filter")
                        .build()));

        client.heartbeat(new AgentHeartbeatSnapshot());

        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://apiserver.test/api/gatepilot/v1/agents/heartbeat");
        assertThat(capturedRequest.get().headers().get("X-Trace-Id"))
                .containsExactly("trace-from-getboot-filter");
    }

    @Test
    void shouldThrowGetbootBusinessExceptionWhenApiResponseFailed() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClientAgentControlPlaneClient client = newClient(
                capturedRequest,
                "{\"status\":\"fail\",\"code\":409,\"message\":\"发布正在处理中\",\"data\":null}"
        );

        assertThatThrownBy(() -> client.heartbeat(new AgentHeartbeatSnapshot()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("发布正在处理中")
                .extracting("errorCodeValue")
                .isEqualTo(409);
    }

    private WebClientAgentControlPlaneClient newClient(AtomicReference<ClientRequest> capturedRequest,
                                                       String responseBody,
                                                       org.springframework.web.reactive.function.client.ExchangeFilterFunction... filters) {
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseBody)
                    .build());
        };
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        List.of(filters).forEach(builder::filter);
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        properties.setApiserverBaseUrl("http://apiserver.test");
        return new WebClientAgentControlPlaneClient(builder, properties);
    }

    private String successBody() {
        return "{\"status\":\"success\",\"code\":200,\"message\":\"ok\",\"data\":{}}";
    }
}
