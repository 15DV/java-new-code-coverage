package ru.spb.coverage.coverage;

public class CoverageTaskException extends RuntimeException {
    public CoverageTaskException(Throwable cause) {
        super(cause);
    }

    public CoverageTaskException(String message) {
        super(message);
    }
}
