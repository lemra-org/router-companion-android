package org.rm3l.router_companion.tasker.exception;

/**
 * Created by rm3l on 11/09/16.
 */
public class DDWRTCompanionPackageVersionRequiredNotFoundException
        extends DDWRTCompanionTaskerException {

    public DDWRTCompanionPackageVersionRequiredNotFoundException() {
    }

    public DDWRTCompanionPackageVersionRequiredNotFoundException(String message) {
        super(message);
    }

    public DDWRTCompanionPackageVersionRequiredNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DDWRTCompanionPackageVersionRequiredNotFoundException(Throwable cause) {
        super(cause);
    }
}
