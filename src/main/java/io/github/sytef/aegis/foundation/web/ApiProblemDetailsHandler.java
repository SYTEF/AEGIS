package io.github.sytef.aegis.foundation.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public final class ApiProblemDetailsHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiProblemDetailsHandler.class);
    private static final String AEGIS_PACKAGE_PREFIX = "io.github.sytef.aegis.";

    private final SafeRequestPathPolicy safeRequestPathPolicy;

    public ApiProblemDetailsHandler(SafeRequestPathPolicy safeRequestPathPolicy) {
        this.safeRequestPathPolicy = safeRequestPathPolicy;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        LoggingEventBuilder logEvent = LOGGER.atError()
                .addKeyValue("event", "http.request.failed")
                .addKeyValue("exceptionType", exception.getClass().getName())
                .addKeyValue("failureCategory", "unexpected_exception");
        firstAegisFrame(exception).ifPresent(frame -> logEvent
                .addKeyValue("failureOriginClass", frame.getClassName())
                .addKeyValue("failureOriginMethod", frame.getMethodName())
                .addKeyValue("failureOriginFile", frame.getFileName() == null ? "unknown" : frame.getFileName())
                .addKeyValue("failureOriginLine", frame.getLineNumber()));
        logEvent.log("Unexpected HTTP request failure");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.");
        problem.setTitle("Erro interno do servidor");
        enrich(problem, "UNEXPECTED_ERROR", request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        if (body instanceof ProblemDetail problem && request instanceof ServletWebRequest servletWebRequest) {
            String code = codeFor(statusCode);
            applySafeClientMessage(problem, statusCode);
            enrich(problem, code, servletWebRequest.getRequest());
            responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }
        return super.createResponseEntity(body, responseHeaders, statusCode, request);
    }

    private void enrich(ProblemDetail problem, String code, HttpServletRequest request) {
        problem.setType(URI.create("urn:aegis:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setInstance(safeRequestPathPolicy.forProblemInstance(request));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", effectiveCorrelationId(request));
        problem.setProperty("timestamp", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
    }

    private static void applySafeClientMessage(ProblemDetail problem, HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            problem.setTitle("Erro HTTP");
            problem.setDetail("A solicitação não pôde ser processada.");
            return;
        }

        switch (status) {
            case BAD_REQUEST -> {
                problem.setTitle("Solicitação inválida");
                problem.setDetail("A solicitação não pôde ser processada.");
            }
            case NOT_FOUND -> {
                problem.setTitle("Recurso não encontrado");
                problem.setDetail("O recurso solicitado não foi encontrado.");
            }
            case METHOD_NOT_ALLOWED -> {
                problem.setTitle("Método não permitido");
                problem.setDetail("O método HTTP não é suportado para este recurso.");
            }
            case UNSUPPORTED_MEDIA_TYPE -> {
                problem.setTitle("Tipo de mídia não suportado");
                problem.setDetail("O tipo de mídia da solicitação não é suportado.");
            }
            case NOT_ACCEPTABLE -> {
                problem.setTitle("Representação não disponível");
                problem.setDetail("A representação solicitada não está disponível.");
            }
            default -> {
                if (status.is5xxServerError()) {
                    problem.setTitle("Erro interno do servidor");
                    problem.setDetail("Ocorreu um erro inesperado.");
                }
            }
        }
    }

    private static String codeFor(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? "HTTP_" + statusCode.value() : status.name();
    }

    private static String effectiveCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        if (value instanceof String correlationId) {
            return correlationId;
        }
        String fromMdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        return fromMdc == null ? "unavailable" : fromMdc;
    }

    private static java.util.Optional<StackTraceElement> firstAegisFrame(Exception exception) {
        return Arrays.stream(exception.getStackTrace())
                .filter(frame -> frame.getClassName().startsWith(AEGIS_PACKAGE_PREFIX))
                .findFirst();
    }
}
