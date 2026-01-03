package com.office.taskmonitor.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tasks")
@Data
public class Task {

    @Id
    private String id;

    private String title;
    private String description;

    private TaskStatus status;

    private String assignedTo;

    private Instant createdAt = Instant.now();
}
