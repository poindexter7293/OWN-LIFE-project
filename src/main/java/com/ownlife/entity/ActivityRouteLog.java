package com.ownlife.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "activity_route_log")
@Getter
@Setter
@NoArgsConstructor
public class ActivityRouteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_log_id")
    private Long routeLogId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_log_id", nullable = false, unique = true)
    private ExerciseLog exerciseLog;

    @Column(name = "start_place", length = 255)
    private String startPlace;

    @Column(name = "end_place", length = 255)
    private String endPlace;

    @Column(name = "start_lat", precision = 10, scale = 7)
    private BigDecimal startLat;

    @Column(name = "start_lng", precision = 10, scale = 7)
    private BigDecimal startLng;

    @Column(name = "end_lat", precision = 10, scale = 7)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 10, scale = 7)
    private BigDecimal endLng;

    @Column(name = "route_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal routeDistanceKm;

    @Column(name = "route_duration_min")
    private Integer routeDurationMin;

    @Column(name = "map_provider", length = 50)
    private String mapProvider;
}
