package com.office.taskmonitor.service;

import com.office.taskmonitor.model.Task;
import com.office.taskmonitor.model.TaskStatus;
import com.mongodb.client.result.DeleteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private  KafkaTemplate<String, Task> kafkaTemplate;
    @Autowired
    private  SimpMessagingTemplate messagingTemplate;

    private static final String TASK_TOPIC = "task-events";

    // Create Task
    public Task createTask(Task task) {
        task.setStatus(TaskStatus.TODO);
        task.setCreatedAt(Instant.now());

        Task savedTask = mongoTemplate.save(task, "tasks");

        // Publish Kafka event
        try {
            kafkaTemplate.send(TASK_TOPIC, savedTask);
        }
        catch (Exception e){
            log.error("Execption while sending the message to the topic {}", e.getMessage());
        }
        try{
            messagingTemplate.convertAndSend("/topic/tasks", savedTask);
        }
        catch (Exception e){
            log.error("Execption while sending the websocket message {}", e.getMessage());
        }



        return savedTask;
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        return mongoTemplate.findAll(Task.class, "tasks");
    }

    // Get task by ID
    public Task getTaskById(String id) {
        return mongoTemplate.findById(id, Task.class, "tasks");
    }

    // Update task (title, description, assignedTo, status)
    public Task updateTask(String id, Task updatedTask) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();

        if (updatedTask.getTitle() != null) update.set("title", updatedTask.getTitle());
        if (updatedTask.getDescription() != null) update.set("description", updatedTask.getDescription());
        if (updatedTask.getAssignedTo() != null) update.set("assignedTo", updatedTask.getAssignedTo());
        if (updatedTask.getStatus() != null) update.set("status", updatedTask.getStatus());

        update.set("updatedAt", Instant.now());

        Task task = mongoTemplate.findAndModify(query, update, Task.class, "tasks");

        if (task != null) {
            kafkaTemplate.send(TASK_TOPIC, task);
            messagingTemplate.convertAndSend("/topic/tasks", task);
        }

        return task;
    }

    // Update only status
    public Task updateTaskStatus(String id, String status) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update()
                .set("status", TaskStatus.valueOf(status))
                .set("updatedAt", Instant.now());

        Task task = mongoTemplate.findAndModify(query, update, Task.class, "tasks");

        if (task != null) {
            kafkaTemplate.send(TASK_TOPIC, task);
            messagingTemplate.convertAndSend("/topic/tasks", task);
        }

        return task;
    }

    // Delete task
    public boolean deleteTask(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, Task.class, "tasks");

        if (result.getDeletedCount() > 0) {
            // Send a "delete" event with just ID (optional placeholder)
            Task deletedTask = new Task();
            deletedTask.setId(id);
            try{
                kafkaTemplate.send(TASK_TOPIC, deletedTask);
                messagingTemplate.convertAndSend("/topic/tasks", deletedTask);

            }
            catch (Exception e){
                log.error("Execption while sending the message to the topic {}", e.getMessage());
            }

            return true;
        }
        return false;
    }
}
