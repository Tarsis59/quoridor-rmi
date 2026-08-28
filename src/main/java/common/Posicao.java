package common;

import java.io.Serializable;

public record Posicao(int linha, int coluna) implements Serializable {
    private static final long serialVersionUID = 1L;
}
