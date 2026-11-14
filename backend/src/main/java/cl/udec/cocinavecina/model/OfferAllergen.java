package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "offer_allergen")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OfferAllergen {
    @EmbeddedId
    private OfferAllergenId id;

    @MapsId("offerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;


}
