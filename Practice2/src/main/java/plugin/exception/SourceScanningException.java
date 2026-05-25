package plugin.exception;

public class SourceScanningException extends RuntimeException {
    public SourceScanningException(String message) {
        super(message);
    }

    public SourceScanningException(String message, Throwable cause) {
        super(message, cause);
    }
}
