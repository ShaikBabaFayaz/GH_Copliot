package com.inventory.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.inventory.product.ProductDataConflictException;
import com.inventory.product.ProductDuplicateSkuException;
import com.inventory.product.ProductException;
import com.inventory.product.ProductInactiveException;
import com.inventory.product.ProductNotFoundException;
import com.inventory.product.ProductValidationException;
import com.inventory.stock.StockValidationException;
import com.inventory.stock.StockVersionConflictException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({ProductValidationException.class, StockValidationException.class,
            MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ProblemDetail badRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, exception instanceof ProductException
                ? exception.getMessage() : "Request validation failed.");
    }

    @ExceptionHandler(ProductDuplicateSkuException.class)
    ProblemDetail duplicateSku(ProductDuplicateSkuException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({ProductDataConflictException.class, ProductInactiveException.class,
            StockVersionConflictException.class})
    ProblemDetail conflict(ProductException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}