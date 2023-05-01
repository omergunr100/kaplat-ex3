package com.mta.server.main;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@RestController
public class KaplatEx3Application {
    static List<TodoShell> todos = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(KaplatEx3Application.class, args);
    }

    @GetMapping("/health")
    public String getHealth() {
        return "OK";
    }

    @PostMapping(value = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Message> postTodo(@RequestBody TodoShell todoShell){
        if(todos.contains(todoShell)){
            Message message = new Message();
            message.errorMessage = "Error: TODO with the title [" + todoShell.title + "] already exists in the system";
            return ResponseEntity.status(409).body(message);
        }
        else if(new Date(todoShell.dueDate).before(new Date())) {
            Message message = new Message();
            message.errorMessage = "Error: Can't create new TODO that its due date is in the past";
            return ResponseEntity.status(409).body(message);
        }
        Todo todo = new Todo(todoShell);
        todos.add(todo);
        Message message = new Message();
        message.result = todo.id;
        return ResponseEntity.status(200).body(message);
    }

    @GetMapping("/size")
    public ResponseEntity<Message> getSize(@RequestParam String status){
        Message message = new Message();
        switch (status){
            case "ALL":
                message.result = todos.size();
                return ResponseEntity.status(200).body(message);
            case "PENDING":
                message.result = (int)todos.stream().filter(todo -> ((Todo)todo).status == Status.PENDING).count();
                return ResponseEntity.status(200).body(message);
            case "LATE":
                message.result = (int)todos.stream().filter(todo -> ((Todo)todo).status == Status.LATE).count();
                return ResponseEntity.status(200).body(message);
            case "DONE":
                message.result = (int)todos.stream().filter(todo -> ((Todo)todo).status == Status.DONE).count();
                return ResponseEntity.status(200).body(message);
            default:
                message.errorMessage = "Error: Invalid status";
                return ResponseEntity.status(400).body(message);
        }
    }

    @GetMapping("/content")
    public ResponseEntity<Message> getContent(@RequestParam String status, @RequestParam(required = false) String sortBy){
        Todo[] todoArray;
        Message message = new Message();
        if(todos.size() == 0){
            message.result = "[]";
            return ResponseEntity.status(200).body(message);
        }

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
                return ResponseEntity.status(400).body(message);
        }

        List<Todo> list = new ArrayList<>(Arrays.asList(todoArray));
        message.result = list;
        return ResponseEntity.status(200).body(message);
    }

    @PutMapping("/")
    public ResponseEntity<Message> putTodoStatus(@RequestParam int id, @RequestParam String status){
        Message message = new Message();
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
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
                return ResponseEntity.status(400).body(message);
        }
        message.result = oldStatus.toString();
        return ResponseEntity.status(200).body(message);
    }

    @DeleteMapping("/")
    public ResponseEntity<Message> deleteTodo(@RequestParam int id){
        Message message = new Message();
        if(todos.stream().noneMatch(todo -> ((Todo)todo).id == id)){
            message.errorMessage = "Error: no such TODO with id " + id;
            return ResponseEntity.status(404).body(message);
        }
        Todo todo = (Todo)todos.stream().filter(todo1 -> ((Todo)todo1).id == id).toArray()[0];
        todos.remove(todo);
        message.result = todos.size();
        return ResponseEntity.status(200).body(message);
    }
}
