package client;

import common.EstadoJogo;

public interface UI {
    void novoEstado(EstadoJogo estado);
    void finalizar(int idVencedor);
}
