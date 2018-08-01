package org.kapott.hbci.exceptions;

import lombok.Data;

@Data
public class MissingTanMediaException extends RuntimeException {

    private String tanMediaNames;

    public MissingTanMediaException(String tanMediaNames) {
        super();
        this.tanMediaNames = tanMediaNames;
    }
}
