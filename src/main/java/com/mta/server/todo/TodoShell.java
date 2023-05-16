package com.mta.server.todo;

import java.util.Objects;

public class TodoShell {
    public String title;
    public String content;
    public long dueDate;

    public TodoShell(String title, String content, long dueDate) {
        this.title = title;
        this.content = content;
        this.dueDate = dueDate;
    }

    public TodoShell(TodoShell todoShell) {
        this.title = todoShell.title;
        this.content = todoShell.content;
        this.dueDate = todoShell.dueDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof TodoShell)) return false;
        TodoShell todoShell = (TodoShell) o;
        return Objects.equals(title, todoShell.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}
