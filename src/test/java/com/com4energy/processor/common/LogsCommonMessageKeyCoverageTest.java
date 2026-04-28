package com.com4energy.processor.common;

import com.com4energy.i18n.core.Messages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogsCommonMessageKeyCoverageTest {

    @Test
    void allLogsCommonMessageKeysMustExistInMessagesBundle() {
        for (LogsCommonMessageKey key : LogsCommonMessageKey.values()) {
            String resolved = Messages.get(key);
            assertNotNull(resolved, () -> "Missing value for key: " + key.key());
            assertFalse(resolved.isBlank(), () -> "Blank value for key: " + key.key());
        }
    }
}

