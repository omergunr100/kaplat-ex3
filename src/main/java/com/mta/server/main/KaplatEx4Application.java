package com.mta.server.main;

import com.mta.server.data.Message;
import com.mta.server.data.Status;
import com.mta.server.todo.Todo;
import com.mta.server.todo.TodoShell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@SpringBootApplication
@RestController
public class KaplatEx4Application {
    static List<TodoShell> todos = new ArrayList<>();
    static int requestCounter = 1;
    static final Logger requestLogger = LogManager.getLogger("request-logger");
    static final Logger todoLogger = LogManager.getLogger("todo-logger");

    public static void main(String[] args) {
        SpringApplication.run(KaplatEx4Application.class, args);
    }

    private void logRequest(Instant start, String resource, String verb){
        requestLogger.info("Incoming request | request #" + requestCounter + " | resource: /todo" + resource + " | HTTP Verb " + verb);
        Duration duration = Duration.between(start, Instant.now());
        requestLogger.debug("request #" + requestCounter + " duration: " + duration.toMillis() + " ms");
        requestCounter++;
    }

    @GetMapping("/health")
    public String getHealth() {
        Instant start = Instant.now();
        logRequest(start, "/health", "GET");
        return "OK";
    }

    @PostMapping(value = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Message> postTodo(@RequestBody TodoShell todoShell){
        Instant start = Instant.now();
        if(todos.contains(todoShell)){
            Message message = new Message();
            message.errorMessage = "Error: TODO with the title [" + todoShell.title + "] already exists in the system";
            logRequest(start, "/", "POST");
            return ResponseEntity.status(409).body(message);
        }
        else if(new Date(todoShell.dueDate).before(new Date())) {
            Message message = new Message();
            message.errorMessage = "Error: Can't create new TODO that its due date is in the past";
            logRequest(start, "/", "POST");
            return ResponseEntity.status(409).body(message);
        }
        Todo todo = new Todo(todoShell);
        todoLogger.info("Creating new TODO with Title ["+todo.title+"]");
        todoLogger.debug("Currently there are "+todos.size()+" Todos in the system. New TODO will be assigned with id "+todo.id);
        todos.add(todo);
        Message message = new Message();
        message.result = todo.id;
        logRequest(start, "/", "POST");
        return ResponseEntity.status(200).body(message);
    }

    @GetMapping("/size")
    public ResponseEntity<Message> getSize(@RequestParam String status){
        Instant start = Instant.now();
        Message message = new Message();

        if(status.equals("ALL")){
            message.result = todos.size();
            logRequest(start, "/size", "GET");
            todoLogger.info("Total TODOs count for state ALL is " + message.result);
            return ResponseEntity.status(200).body(message);
        }

        try{
            final Status stat = Status.valueOf(status);
            message.result = (int)todos.stream().filter(todo -> ((Todo)todo).status.equals(stat)).count();
            logRequest(start, "/size", "GET");
            todoLogger.info("Total TODOs count for state "+stat.toString()+" is " + message.result);
            return ResponseEntity.status(200).body(message);
        } catch (IllegalArgumentException e){
            message.errorMessage = "Error: Invalid status";
            logRequest(start, "/size", "GET");
            return ResponseEntity.status(400).body(message);
        }
    }

    @GetMapping("/content")
    public ResponseEntity<Message> getContent(@RequestParam String status, @RequestParam(required = false) String sortBy){
        Instant start = Instant.now();
        Todo[] todoArray;
        Message message = new Message();
//        if(todos.size() == 0){
//            message.result = "[]";
//            logRequest(start, "/content", "GET");
//            return ResponseEntity.status(200).body(message);
//        }

        Object[] temp;
        switch (status){
            case "ALL":
                todoArray = Arrays.copyOf(todos.toArray(), todos.size(), Todo[].class);
                break;
            case "PENDING":
                temp = todos.stream().filter(todo -> ((Todo)todo).status == Status.PENDING).toArray();
                todoArray = Arrays.copyOf(temp, temp.length, Todo[].class);
                break;
            case "LATE":
                temp = todos.stream().filter(todo -> ((Todo)todo).status == Status.LATE).toArray();
                todoArray = Arrays.copyOf(temp, temp.length, Todo[].class);
                break;
            case "DONE":
                temp = todos.stream().filter(todo -> ((Todo)todo).status == Status.DONE).toArray();
                todoArray = Arrays.copyOf(temp, temp.length, Todo[].class);
                break;
            default:
                message.errorMessage = "Error: Invalid status";
                logRequest(start, "/content", "GET");
                return ResponseEntity.status(400).body(message);
        }

        if(sortBy == null)
            sortBy = "ID";

        switch (sortBy){
            case "ID":
                Arrays.sort(todoArray, (todo1, todo2) -> ((Todo)todo1).id - ((Todo)todo2).id);
                break;
            case "DUE_DATE":
                Arrays.sort(todoArray, (todo1, todo2) -> (int)(todo1.dueDate - todo2.dueDate));
                break;
            case "TITLE":
                Arrays.sort(todoArray, (todo1, todo2) -> todo1.title.compareTo(todo2.title));
                break;
            default:
                message.errorMessage = "Error: Invalid sortBy";
                logRequest(start, "/content", "GET");
                return ResponseEntity.status(400).body(message);
        }

        List<Todo> list = new ArrayList<>(Arrays.asList(todoArray));
        message.result = list;
        logRequest(start, "/content", "GET");
        todoLogger.info("Extracting todos content. Filter: "+status+" | Sorting by: "+sortBy);
        todoLogger.debug("There are a total of "+todos.size()+" todos in the system. The result holds "+list.size()+" todos");
        return ResponseEntity.status(200).body(message);
    }

    @PutMapping("/")
    public ResponseEntity<Message> putTodoStatus(@RequestParam int id, @RequestParam String status){
        Instant start = Instant.now();
        Message message = new Message();
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
            logRequest(start, "/", "PUT");
            return ResponseEntity.status(404).body(message);
        }
        Todo todo = (Todo)todos.stream().filter(todo1 -> ((Todo)todo1).id == id).toArray()[0];
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
                logRequest(start, "/", "PUT");
                return ResponseEntity.status(400).body(message);
        }
        message.result = oldStatus.toString();
        logRequest(start, "/", "PUT");
        todoLogger.info("Update TODO id ["+todo.id+"] state to "+status);
        todoLogger.debug("Todo id ["+todo.id+"] state change: "+oldStatus.toString()+" --> "+status);
        return ResponseEntity.status(200).body(message);
    }

    @DeleteMapping("/")
    public ResponseEntity<Message> deleteTodo(@RequestParam int id){
        Instant start = Instant.now();
        Message message = new Message();
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
            logRequest(start, "/", "DELETE");
            return ResponseEntity.status(404).body(message);
        }
        Todo todo = (Todo)todos.stream().filter(todo1 -> ((Todo)todo1).id == id).toArray()[0];
        todos.remove(todo);
        message.result = todos.size();
        logRequest(start, "/", "DELETE");
        todoLogger.info("Removing todo id " + id);
        todoLogger.debug("After removing todo id ["+id+"] there are "+todos.size()+" TODOs in the system");
        return ResponseEntity.status(200).body(message);
    }

    @GetMapping("/logs/level")
    public ResponseEntity<Message> getLoggerLevel(@RequestParam String loggerName){
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
    public ResponseEntity<Message> setLoggerLevel(@RequestParam String loggerName, @RequestParam String level){
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
                return ResponseEntity.status(400).body(message);
        }

        switch (level){
            case "DEBUG":
                LogManager.getLogger(loggerName).setLevel(Level.DEBUG);
                break;
            case "INFO":
                LogManager.getLogger(loggerName).setLevel(Level.INFO);
                break;
            case "ERROR":
                LogManager.getLogger(loggerName).setLevel(Level.ERROR);
                break;
            default:
                message.errorMessage = "Error: Invalid level!";
                logRequest(start, "/logs/level", "PUT");
                return ResponseEntity.status(400).body(message);
        }
        message.result = logger.getLevel().toString();
        logRequest(start, "/logs/level", "PUT");
        return ResponseEntity.status(200).body(message);
    }
}
