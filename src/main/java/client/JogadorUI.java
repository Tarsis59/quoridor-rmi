package client;

import common.EstadoJogo;

/** UI que também carrega a identidade local do jogador e o estado atual (usada no modo bot). */
public interface JogadorUI extends UI {
    void setMeuId(int id);
    EstadoJogo getEstadoAtual();
}
