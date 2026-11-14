package cl.udec.cocinavecina.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OfferAllergenId implements Serializable {
    @Serial
    private static final long serialVersionUID = 4449627178233873007L;
    @NotNull
    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Size(max = 30)
    @NotNull
    @Column(name = "allergen", nullable = false, length = 30)
    private String allergen;

}
