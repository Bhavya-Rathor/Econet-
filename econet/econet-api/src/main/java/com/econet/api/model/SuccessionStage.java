package com.econet.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "succession_stages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stageName;

    private Integer stageOrder;

    @Column(length = 2000)
    private String description;

    private LocalDate estimatedStartDate;

    private Integer durationInYears;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecosystem_id")
    private Ecosystem ecosystem;
}
