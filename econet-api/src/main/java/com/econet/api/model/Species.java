package com.econet.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "species")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String commonName;

    private String scientificName;

    @Enumerated(EnumType.STRING)
    private SpeciesType type;

    private Integer populationCount;

    private String conservationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecosystem_id")
    private Ecosystem ecosystem;

    public enum SpeciesType {
        PRODUCER, PRIMARY_CONSUMER, SECONDARY_CONSUMER, DECOMPOSER, PIONEER
    }
}
