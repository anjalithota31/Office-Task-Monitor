package com.office.taskmonitor.service;



import com.mongodb.client.result.DeleteResult;
import lombok.RequiredArgsConstructor;
import com.office.taskmonitor.model.Task;
import com.office.taskmonitor.model.TaskStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final MongoTemplate mongoTemplate;

    public Task createTask(Task task) {
        task.setStatus(TaskStatus.TODO);
        task.setCreatedAt(Instant.now());
        return mongoTemplate.save(task,"tasks");
    }

    public List<Task> getAllTasks() {
        return mongoTemplate.findAll(Task.class);
    }
    public Task getTaskById(String id) {
        return mongoTemplate.findById(id,Task.class,"tasks");
    }

    public Task updateStatus(String taskId, TaskStatus status) {
        return mongoTemplate.findAndModify(Query.query(Criteria.where("_id").is(taskId)),  new Update().set("status", status), Task.class, "tasks");
    }
    public Task updateTaskStatus(String id, String status) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().set("status", TaskStatus.valueOf(status));
        return mongoTemplate.findAndModify(query, update, Task.class, "tasks");
    }
    public boolean deleteTask(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, Task.class);
        return result.getDeletedCount() > 0;
    }

    public Task updateTask(String id, Task updatedTask) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        if (updatedTask.getTitle() != null) {
            update.set("title", updatedTask.getTitle());
        }
        if (updatedTask.getDescription() != null){
            update.set("description", updatedTask.getDescription());
        }
        if (updatedTask.getStatus() != null){
            update.set("status", updatedTask.getStatus());
        }
        if (updatedTask.getAssignedTo() != null) {
            update.set("assignedTo", updatedTask.getAssignedTo());
        }

        return mongoTemplate.findAndModify(query, update, Task.class);
    }

}
