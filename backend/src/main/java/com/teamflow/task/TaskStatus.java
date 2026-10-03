package com.teamflow.task;
public enum TaskStatus {
    TODO("To Do"), IN_PROGRESS("In Progress"), DONE("Done");
    private final String label;
    TaskStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
