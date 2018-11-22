package org.kapott.hbci.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MissingTanMediaException extends RuntimeException {

    private String tanMediaNames;

    public MissingTanMediaException(String tanMediaNames) {
        super();
        this.tanMediaNames = tanMediaNames;
    }
}
