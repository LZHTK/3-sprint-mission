package com.sprint.mission.discodeit.exception.readstatus;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import java.util.Map;
import java.util.Objects;

public class ReadStatusException extends DiscodeitException {

    public ReadStatusException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReadStatusException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
