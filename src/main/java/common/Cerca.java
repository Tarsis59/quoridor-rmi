package common;

import java.io.Serializable;

public record Cerca(Posicao base, Orientacao orientacao) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Cerca {
        if (base == null || orientacao == null) {
            throw new IllegalArgumentException("Cerca exige base e orientação.");
        }
    }
}
