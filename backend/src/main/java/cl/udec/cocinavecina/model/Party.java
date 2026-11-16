package cl.udec.cocinavecina.model;

public enum Party {
    BUYER,
    SELLER;

    public Party other() {
        return this == BUYER ? SELLER : BUYER;
    }
}
