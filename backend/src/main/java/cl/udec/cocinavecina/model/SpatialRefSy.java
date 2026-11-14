package cl.udec.cocinavecina.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "spatial_ref_sys")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SpatialRefSy {
    @Id
    @Column(name = "srid", nullable = false)
    private Integer id;

    @Size(max = 256)
    @Column(name = "auth_name", length = 256)
    private String authName;

    @Column(name = "auth_srid")
    private Integer authSrid;

    @Size(max = 2048)
    @Column(name = "srtext", length = 2048)
    private String srtext;

    @Size(max = 2048)
    @Column(name = "proj4text", length = 2048)
    private String proj4text;


}
