package com.skillsync.skillsync.dto.request.session;

import lombok.Data;
import java.util.UUID;

@Data
public class BookSessionRequest {
    private UUID slotId;
    private String learnerNotes;
}
