package com.mta.server.main;

import com.mta.server.data.Message;
import com.mta.server.data.Status;
import com.mta.server.todo.Todo;
import com.mta.server.todo.TodoShell;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class KaplatEx4Application {
    static List<TodoShell> todos = new ArrayList<>();
    static Integer requestCounter = 1;
    static Logger requestLogger = LogManager.getLogger("request-logger");
    static Logger todoLogger = LogManager.getLogger("todo-logger");

    public static void main(String[] args) {
        SpringApplication.run(KaplatEx4Application.class, args);
    }

    private void logRequest(Instant start, String resource, String verb){
        ThreadContext.put("requestCounter", requestCounter.toString());
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: " + resource + " | HTTP Verb " + verb);
        Duration duration = Duration.between(start, Instant.now());
        requestLogger.debug("request #" + requestCounter.toString() + " duration: " + duration.toMillis() + " ms");
        requestCounter++;
    }

    @GetMapping("/todo/health")
    public String getHealth() {
        Instant start = Instant.now();
        logRequest(start, "/health", "GET");
        return "OK";
    }

    @PostMapping(value = "/todo", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Message> postTodo(@RequestBody TodoShell todoShell){
        Instant start = Instant.now();
        if(todos.contains(todoShell)){
            Message message = new Message();
            message.errorMessage = "Error: TODO with the title [" + todoShell.title + "] already exists in the system";
            todoLogger.error(message.errorMessage);
            logRequest(start, "/todo", "POST");
            return ResponseEntity.status(409).body(message);
        }
        else if(new Date(todoShell.dueDate).before(new Date())) {
            Message message = new Message();
            message.errorMessage = "Error: Can't create new TODO that its due date is in the past";
            todoLogger.error(message.errorMessage);
            logRequest(start, "/todo", "POST");
            return ResponseEntity.status(409).body(message);
        }
        Todo todo = new Todo(todoShell);
        Message message = new Message();
        message.result = todo.id;
        logRequest(start, "/todo", "POST");
        todoLogger.info("Creating new TODO with Title ["+todo.title+"]");
        todoLogger.debug("Currently there are "+todos.size()+" Todos in the system. New TODO will be assigned with id "+todo.id);
        todos.add(todo);
        return ResponseEntity.status(200).body(message);
    }

    @GetMapping("/todo/size")
    public ResponseEntity<Message> getSize(@RequestParam String status){
        Instant start = Instant.now();
        Message message = new Message();

        if(status.equals("ALL")){
            message.result = todos.size();
            logRequest(start, "/todo/size", "GET");
            todoLogger.info("Total TODOs count for state ALL is " + message.result);
            return ResponseEntity.status(200).body(message);
        }

        try{
            final Status stat = Status.valueOf(status);
            message.result = (int)todos.stream().filter(todo -> ((Todo)todo).status.equals(stat)).count();
            logRequest(start, "/todo/size", "GET");
            todoLogger.info("Total TODOs count for state "+stat.toString()+" is " + message.result);
            return ResponseEntity.status(200).body(message);
        } catch (IllegalArgumentException e){
            message.errorMessage = "Error: Invalid status";
            todoLogger.error(message.errorMessage);
            logRequest(start, "/todo/size", "GET");
            return ResponseEntity.status(400).body(message);
        }
    }

    @GetMapping("/todo/content")
    public ResponseEntity<Message> getContent(@RequestParam String status, @RequestParam(required = false) String sortBy){
        Instant start = Instant.now();
        List<Todo> todoCopy;
        Message message = new Message();

        switch (status){
            case "ALL":
                todoCopy = todos.stream().map(todo -> (Todo)todo).collect(Collectors.toList());
                break;
            case "PENDING":
                todoCopy = todos.stream().map(todo -> (Todo)todo).filter(todo -> todo.status.equals(Status.PENDING)).collect(Collectors.toList());
                break;
            case "LATE":
                todoCopy = todos.stream().map(todo -> (Todo)todo).filter(todo -> todo.status.equals(Status.LATE)).collect(Collectors.toList());
                break;
            case "DONE":
                todoCopy = todos.stream().map(todo -> (Todo)todo).filter(todo -> todo.status.equals(Status.DONE)).collect(Collectors.toList());
                break;
            default:
                message.errorMessage = "Error: Invalid status";
                todoLogger.error(message.errorMessage);
                logRequest(start, "/todo/content", "GET");
                return ResponseEntity.status(400).body(message);
        }

        if(sortBy == null)
            sortBy = "ID";

        switch (sortBy){
            case "ID":
                todoCopy.sort((todo1, todo2) -> todo1.id - todo2.id);
                break;
            case "DUE_DATE":
                todoCopy.sort((todo1, todo2) -> (int)(todo1.dueDate - todo2.dueDate));
                break;
            case "TITLE":
                todoCopy.sort((todo1, todo2) -> todo1.title.compareTo(todo2.title));
                break;
            default:
                message.errorMessage = "Error: Invalid sortBy";
                todoLogger.error(message.errorMessage);
                logRequest(start, "/todo/content", "GET");
                return ResponseEntity.status(400).body(message);
        }

        message.result = todoCopy;
        logRequest(start, "/todo/content", "GET");
        todoLogger.info("Extracting todos content. Filter: "+status+" | Sorting by: "+sortBy);
        todoLogger.debug("There are a total of "+todos.size()+" todos in the system. The result holds "+todoCopy.size()+" todos");
        return ResponseEntity.status(200).body(message);
    }

    @PutMapping("/todo")
    public ResponseEntity<Message> putTodoStatus(@RequestParam int id, @RequestParam String status){
        Instant start = Instant.now();
        Message message = new Message();
        todoLogger.info("Update TODO id ["+id+"] state to "+status);
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
            todoLogger.error(message.errorMessage);
            logRequest(start, "/todo", "PUT");
            return ResponseEntity.status(404).body(message);
        }
        Todo todo = (Todo)todos.stream().filter(todo1 -> ((Todo)todo1).id == id).findFirst().get();
        Status oldStatus = todo.status;
        switch (status){
            case "PENDING":
                todo.status = Status.PENDING;
                break;
            case "LATE":
                todo.status = Status.LATE;
                break;
            case "DONE":
                todo.status = Status.DONE;
                break;
            default:
                message.errorMessage = "Error: Invalid status";
                todoLogger.error(message.errorMessage);
                logRequest(start, "/todo", "PUT");
                return ResponseEntity.status(400).body(message);
        }
        message.result = oldStatus.toString();
        logRequest(start, "/todo", "PUT");
        todoLogger.debug("Todo id ["+todo.id+"] state change: "+oldStatus.toString()+" --> "+status);
        return ResponseEntity.status(200).body(message);
    }

    @DeleteMapping("/todo")
    public ResponseEntity<Message> deleteTodo(@RequestParam int id){
        Instant start = Instant.now();
        Message message = new Message();
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
            todoLogger.error(message.errorMessage);
            logRequest(start, "/todo", "DELETE");
            return ResponseEntity.status(404).body(message);
        }
        Todo todo = (Todo)todos.stream().filter(todo1 -> ((Todo)todo1).id == id).toArray()[0];
        todos.remove(todo);
        message.result = todos.size();
        logRequest(start, "/todo", "DELETE");
        todoLogger.info("Removing todo id " + id);
        todoLogger.debug("After removing todo id ["+id+"] there are "+todos.size()+" TODOs in the system");
        return ResponseEntity.status(200).body(message);
    }

    @GetMapping("/logs/level")
    public ResponseEntity<Message> getLoggerLevel(@RequestParam(name="logger-name") String loggerName){
        Instant start = Instant.now();
        Message message = new Message();
        Logger logger = null;

        switch (loggerName){
            case "todo-logger":
                logger = todoLogger;
                break;
            case "request-logger":
                logger = requestLogger;
                break;
            default:
                message.errorMessage = "Error: Invalid logger name!";
                logRequest(start, "/logs/level", "GET");
                return ResponseEntity.status(400).body(message);
        }
        message.result = LogManager.getLogger(loggerName).getLevel().toString();
        logRequest(start, "/logs/level", "GET");
        return ResponseEntity.status(200).body(message);
    }

    @PutMapping("/logs/level")
    public ResponseEntity<Message> setLoggerLevel(@RequestParam(name="logger-name") String loggerName, @RequestParam(name="logger-level") String level){
        Instant start = Instant.now();
        Message message = new Message();

        Logger logger = null;
        switch (loggerName){
            case "todo-logger":
                logger = todoLogger;
                break;
            case "request-logger":
                logger = requestLogger;
                break;
            default:
                message.errorMessage = "Error: Invalid logger name!";
                logRequest(start, "/logs/level", "PUT");
                return ResponseEntity.status(400).body(message);
        }

        Level setLevel = null;
        switch (level){
            case "INFO":
                setLevel = Level.INFO;
                break;
            case "DEBUG":
                setLevel = Level.DEBUG;
                break;
            case "ERROR":
                setLevel = Level.ERROR;
                break;
            default:
                message.errorMessage = "Error: Invalid level!";
                logRequest(start, "/logs/level", "PUT");
                return ResponseEntity.status(400).body(message);
        }

        Configurator.setLevel(logger, setLevel);

        message.result = setLevel.toString();
        logRequest(start, "/logs/level", "PUT");
        return ResponseEntity.status(200).body(message);
    }
}
