package com.com4energy.processor.api;

public final class ApiMessages {

    private ApiMessages() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final String SUCCESS = "Operation completed successfully.";
    public static final String ERROR = "An error occurred while processing the request.";
    public static final String WARNING = "Warning: The operation completed with some issues.";
    public static final String FILE_ALREADY_EXISTS = "The file already exists in the system.";
    public static final String FILE_NOT_FOUND = "The requested file was not found.";
    public static final String FILE_RECEIVED = "File received successfully.";
    public static final String FILE_UPLOADED_SUCCESSFULLY = "File uploaded successfully.";
    public static final String FILE_ERROR = "There was an error with the file upload.";
    public static final String INVALID_FILE_FORMAT = "The uploaded file format is invalid.";
    public static final String UPLOAD_SUCCESS = "File uploaded successfully.";
    public static final String UPLOAD_FAILURE = "File upload failed due to server error.";
    public static final String PROCESSING_ERROR = "Error occurred while processing the file.";

}
