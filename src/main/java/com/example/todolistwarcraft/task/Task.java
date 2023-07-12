package com.example.todolistwarcraft.task;

import com.example.todolistwarcraft.user.User;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "tasks")
public class Task extends PanacheEntity {
    @Column(nullable = false)
    public String name;

    public Integer priority;

    @ManyToOne(optional = false)
    public User user;

    public boolean complete;

    public ZonedDateTime due;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    public ZonedDateTime created;

    @Version
    public int version;
}