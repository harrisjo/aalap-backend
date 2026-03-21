package com.aalap.aalapbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "nools")
@Data
public class Nool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String title;
    @Column
    private String description;
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column
    private Date createdAt;
    @ManyToOne
    @JoinColumn(name = "forked_from")
    private Nool forkedFrom;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }
}