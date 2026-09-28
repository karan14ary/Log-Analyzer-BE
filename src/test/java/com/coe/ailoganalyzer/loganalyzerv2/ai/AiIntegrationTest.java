package com.coe.ailoganalyzer.loganalyzerv2.ai;

import com.coe.ailoganalyzer.loganalyzerv2.exception.GlobalExceptionHandler;
import com.coe.ailoganalyzer.loganalyzerv2.model.ApiError;
import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparison;
import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparisonSummary;
import com.coe.ailoganalyzer.loganalyzerv2.model.ComparisonStatus;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorComparison;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyComparison;
import com.coe.ailoganalyzer.loganalyzerv2.model.MetricComparison;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiIntegrationTest {

    @Test
    void ollamaClientSendsJsonModeAndTemperatureAndReturnsGeneratedText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaClient client = new OllamaClient(builder,
                new OllamaConfig("http://localhost:11434", "llama3.1", 0.15));
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.format").value("json"))
                .andExpect(jsonPath("$.options.temperature").value(0.15))
                .andRespond(withSuccess("{\"model\":\"llama3.1\",\"response\":\"{}\",\"done\":true}",
                        MediaType.APPLICATION_JSON));

        assertEquals("{}", client.generate("analyze"));
        server.verify();
    }

    @Test
    void ollamaClientExplainsMissingModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaClient client = new OllamaClient(builder,
                new OllamaConfig("http://localhost:11434", "missing-model", 0.1));
        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.generate("prompt"));

        assertTrue(exception.getMessage().contains("ollama pull missing-model"));
    }

    @Test
    void ollamaClientRejectsEmptyResponseAndWrapsServerFailures() {
        RestClient.Builder emptyBuilder = RestClient.builder();
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBuilder).build();
        OllamaClient emptyClient = new OllamaClient(emptyBuilder,
                new OllamaConfig("http://localhost:11434", "llama3.1", 0.1));
        emptyServer.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withSuccess("{\"model\":\"llama3.1\",\"response\":null,\"done\":true}",
                        MediaType.APPLICATION_JSON));
        IllegalStateException emptyException = assertThrows(IllegalStateException.class,
                () -> emptyClient.generate("prompt"));
        assertTrue(emptyException.getMessage().contains("Failed to communicate"));
        assertEquals("Ollama returned an empty response", emptyException.getCause().getMessage());

        RestClient.Builder failedBuilder = RestClient.builder();
        MockRestServiceServer failedServer = MockRestServiceServer.bindTo(failedBuilder).build();
        OllamaClient failedClient = new OllamaClient(failedBuilder,
                new OllamaConfig("http://localhost:11434", "llama3.1", 0.1));
        failedServer.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertTrue(assertThrows(IllegalStateException.class,
                () -> failedClient.generate("prompt")).getMessage().contains("http://localhost:11434"));
    }

    @Test
    void healthServiceReturnsFalseWhenOllamaIsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaHealthService healthService = new OllamaHealthService(builder,
                new OllamaConfig("http://localhost:11434", "llama3.1", 0.1));
        server.expect(requestTo("http://localhost:11434/"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertFalse(healthService.isAvailable());
    }

    @Test
    void promptBuildersSerializeInputsAndIncludeInstructions() {
        var comparison = new DeploymentComparison(
                new DeploymentComparisonSummary(ComparisonStatus.IMPROVED, "improved", 1, 0),
                new MetricComparison(1, 1, 0, 1, 0, -1, 1, 0, -1, 1, 1, 1, 0),
                new ErrorComparison(List.of(), List.of("old"), List.of(), List.of(), List.of()),
                new AnomalyComparison(List.of(), List.of(), List.of()), List.of("review"));
        String deploymentPrompt = new DeploymentComparisonPromptBuilder(new ObjectMapper()).build(comparison);
        String rcaPrompt = new RcaPromptBuilder().build("evidence block");

        assertTrue(deploymentPrompt.contains("DEPLOYMENT COMPARISON:"));
        assertTrue(deploymentPrompt.contains("\"status\":\"IMPROVED\""));
        assertTrue(rcaPrompt.contains("Return ONLY valid JSON."));
        assertTrue(rcaPrompt.endsWith("evidence block"));
    }

    @Test
    void globalHandlerReturnsStableApiErrorEnvelope() {
        var response = new GlobalExceptionHandler().handle(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiError error = response.getBody();
        assertEquals("LOG_ANALYSIS_ERROR", error.code());
        assertEquals("bad input", error.message());
    }
}
