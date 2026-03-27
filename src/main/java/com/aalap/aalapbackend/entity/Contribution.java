package com.aalap.aalapbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "contributions")
@Data
public class Contribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "nool_id", nullable = false)
    private Nool nool;
    @Column(nullable = false)
    private String role;
    @Column
    private String description;
    @Column(name = "file_path")
    private String filePath;
    @ManyToOne
    @JoinColumn(name = "carried_from")
    private Contribution carriedFrom;
    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }
}
