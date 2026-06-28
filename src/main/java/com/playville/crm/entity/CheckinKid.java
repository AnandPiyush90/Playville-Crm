package com.playville.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pv_checkin_kids")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckinKid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private Checkin checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kid_id", nullable = false)
    private Kid kid;

    @Column(name = "session_used")
    @Builder.Default
    private boolean sessionUsed = false;
}