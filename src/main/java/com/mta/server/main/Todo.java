package com.mta.server.main;

import java.util.Date;
import java.util.Objects;

public class Todo extends TodoShell{
    public static int idCounter = 1;
    public int id;
    public Status status;

    Todo(TodoShell todoShell){
        super(todoShell);
        this.id = idCounter++;
        this.status = Status.PENDING;
    }
}
