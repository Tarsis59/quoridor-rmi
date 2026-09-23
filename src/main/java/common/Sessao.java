package common;

import java.io.Serializable;

/**
 * Credencial devolvida pelo servidor no registro. Toda jogada precisa apresentar a
 * sessão: o servidor confere o token secreto do jogador, impedindo que um cliente
 * jogue em nome de outro (autorização por recurso, não só "está conectado").
 */
public record Sessao(int idJogador, String token) implements Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        // Nunca expõe o token em logs.
        return "Sessao[idJogador=" + idJogador + "]";
    }
}
