package com.sentimospadel.backend.coach.entity;

import com.sentimospadel.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coaches")
public class Coach extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "club_name", nullable = false, length = 160)
    private String clubName;

    @Column(name = "current_rating", nullable = false, precision = 4, scale = 2)
    private BigDecimal currentRating;

    @Column(name = "reviews_count", nullable = false)
    private int reviewsCount;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "hourly_rate_uyu", nullable = false)
    private int hourlyRateUyu;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
