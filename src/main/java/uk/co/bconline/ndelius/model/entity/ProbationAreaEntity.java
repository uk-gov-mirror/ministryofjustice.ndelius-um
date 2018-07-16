package uk.co.bconline.ndelius.model.entity;

import java.io.Serializable;

import javax.persistence.*;

import lombok.Data;

@Entity
@Table(name = "PROBATION_AREA")
@Data
public class ProbationAreaEntity  implements Serializable {
    @Id
    @GeneratedValue(generator = "probation_area_seq")
    @SequenceGenerator(name = "probation_area_seq", sequenceName = "PROBATION_AREA_ID_SEQ", allocationSize = 1)
    @Column(name = "PROBATION_AREA_ID")
    private Long probationAreaId;

    @Column(name = "CODE")
    private String code;

    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToOne
    @JoinColumn(name = "DIVISION_ID", updatable = false, insertable = false)
    private ProbationAreaEntity division;

    @ManyToOne
    @JoinColumn(name = "ORGANISATION_ID", insertable = false, updatable = false)
    private OrganisationEntity organisation;
}