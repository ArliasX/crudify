package com.arlias.quarkus_crudify.exception;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomException extends RuntimeException implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(CustomException.class);

    @Getter
    private final ErrorCode errorCode;

    private final Object[] data;

    public CustomException(ErrorCode errorCode, String errorMessage, Object[] data) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.data = data;
    }

    public CustomException(ErrorCode errorCode, Throwable t) {
        super(t);
        if (t instanceof CustomException) {
            this.data = ((CustomException) t).data;
            this.errorCode = ((CustomException) t).errorCode;
        } else {
            this.data = null;
            this.errorCode = errorCode;
        }
    }

    public Map<String, Object> getExtensions() {
        Map<String, Object> customAttributes = new LinkedHashMap<>();
        String parsedErrorMessage = getParsedErrorMessage();
        String stackTrace = getCauseStackTrace();

        customAttributes.put("errorCode", this.errorCode.code);
        customAttributes.put("classification", this.errorCode.toString());
        customAttributes.put("errorMessage", parsedErrorMessage);
        customAttributes.put("verboseErrorMessage", this.toString());
        customAttributes.put("causedBy", stackTrace);

        log.error(parsedErrorMessage + ": " + stackTrace, this);

        return customAttributes;
    }

    private String getCauseStackTrace() {
        StringBuilder causedBy = new StringBuilder("");
        Throwable t = this.getCause();
        while (t != null) {
            causedBy.append(t);
            t = t.getCause();
        }
        return causedBy.toString().isBlank() ? "No Cause!" : causedBy.toString();
    }

    public static CustomException get(ErrorCode errorCode, Throwable e) {
        return new CustomException(errorCode, e);
    }

    public static CustomException get(ErrorCode errorCode, String errorMessage) {
        return new CustomException(errorCode, errorMessage, null);
    }

    public static CustomException get(ErrorCode errorCode, String errorMessage, Object... data) {
        return new CustomException(errorCode, errorMessage, data);
    }


    public String getParsedErrorMessage() {
        String finalErrMessage = this.getMessage();
        if (data != null && data.length > 0) {
            for (Object d : data) {
                try {
                    finalErrMessage = finalErrMessage.replaceFirst(Pattern.quote("{}"), ((d == null) ? "null" : Matcher.quoteReplacement(d.toString())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return finalErrMessage;
    }

    public void boom() throws CustomException {
        throw this;
    }

    public enum ErrorCode {

        NOT_FOUND(404),
        UNVAILABLE(503),
        SQL(999),
        SQL_DDL(998),
        INTERNAL(500),
        BAD_REQUEST(400),
        VALIDATION(505),
        DELETE(506);

        public final int code;

        ErrorCode(int label) {
            this.code = label;
        }
    }

    public static CustomException unimplemented() {
        return CustomException.get(ErrorCode.UNVAILABLE, "Unimplemented method or service invoked!");
    }

}
