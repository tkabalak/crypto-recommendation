package com.example.crypto.adapters.in.web.error;

import com.example.crypto.domain.exception.InvalidDateRangeException;
import com.example.crypto.domain.exception.NoDataForPeriodException;
import com.example.crypto.domain.exception.RateLimitExceededException;
import com.example.crypto.domain.exception.UnsupportedCryptoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

/**
 * Global exception handler that maps domain and validation errors to RFC7807 Problem Details
 * ({@code application/problem+json}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maps {@link UnsupportedCryptoException} to HTTP 404.
     */
    @ExceptionHandler(UnsupportedCryptoException.class)
    public ResponseEntity<ProblemDetail> handleUnsupported(UnsupportedCryptoException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        pd.setTitle("Unsupported crypto symbol");
        pd.setType(URI.create("https://test-task.example.com/problems/unsupported-crypto"));
        pd.setProperty("symbol", ex.getSymbol());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Maps {@link NoDataForPeriodException} to HTTP 404.
     */
    @ExceptionHandler(NoDataForPeriodException.class)
    public ResponseEntity<ProblemDetail> handleNoData(NoDataForPeriodException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        pd.setTitle("No data for requested period");
        pd.setType(URI.create("https://test-task.example.com/problems/no-data"));

        pd.setProperty("symbol", ex.getSymbol());
        pd.setProperty("fromInclusive", ex.getFromInclusive());
        pd.setProperty("toExclusive", ex.getToExclusive());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Maps {@link InvalidDateRangeException} to HTTP 400.
     */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRange(InvalidDateRangeException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

        pd.setTitle("Invalid date range");
        pd.setType(URI.create("https://test-task.example.com/problems/invalid-date-range"));
        pd.setProperty("from", ex.getFrom());
        pd.setProperty("to", ex.getTo());

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }


    /**
     * Maps bean validation constraint violations to HTTP 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Bad request");

        pd.setTitle("Bad request");
        pd.setType(URI.create("https://example.com/problems/bad-request"));
        pd.setProperty("error", ex.getClass().getSimpleName());
        pd.setProperty("violations", ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList());

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Maps request binding errors to HTTP 400.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Bad request");

        pd.setTitle("Bad request");
        pd.setType(URI.create("https://test-task.example.com/problems/bad-request"));
        pd.setProperty("error", ex.getClass().getSimpleName());
        pd.setProperty("detail", ex.getMessage());

        if (ex instanceof MethodArgumentNotValidException manv) {
            List<String> violations = manv.getBindingResult().getFieldErrors().stream()

                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .toList();
            pd.setProperty("violations", violations);
        }

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Ensures clients calling an invalid path do not get HTTP 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        // Happens when client calls a non-existing path like "/best" instead of "/api/v1/cryptos/best"
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No handler/resource found for requested path");

        pd.setTitle("Not Found");
        pd.setType(URI.create("https://test-task.example.com/problems/not-found"));
        pd.setProperty("error", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Maps {@link RateLimitExceededException} to HTTP 429.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

        pd.setTitle("Rate limit exceeded");
        pd.setType(URI.create("https://example.com/problems/rate-limit-exceeded"));
        pd.setProperty("clientIp", ex.getClientIp());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }


    /**
     * Fallback handler mapped to HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOther(Exception ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://test-task.example.com/problems/internal-error"));
        pd.setProperty("error", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
