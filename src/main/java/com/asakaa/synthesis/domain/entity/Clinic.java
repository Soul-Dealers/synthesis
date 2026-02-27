package com.asakaa.synthesis.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clinics")
public class Clinic extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String address;

    private String region;

    @Column(name = "registration_code", unique = true, nullable = false)
    private String registrationCode;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Provider> providers = new ArrayList<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Patient> patients = new ArrayList<>();
}
