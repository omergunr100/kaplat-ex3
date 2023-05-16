package com.mta.server.todo;

import com.mta.server.data.Status;

public class Todo extends TodoShell {
    public static int idCounter = 1;
    public int id;
    public Status status;

    public Todo(TodoShell todoShell){
        super(todoShell);
        this.id = idCounter++;
        this.status = Status.PENDING;
    }
}
