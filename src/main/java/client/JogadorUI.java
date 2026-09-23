package client;

import common.EstadoJogo;
import common.Sessao;

/** UI que também carrega a identidade local do jogador e o estado atual (usada no modo bot). */
public interface JogadorUI extends UI {
    void setSessao(Sessao sessao);
    EstadoJogo getEstadoAtual();

    /**
     * Bloqueia até chegar um estado diferente (outra instância) de {@code anterior} ou até o
     * prazo; devolve o estado atual (pode ser o mesmo, se o prazo esgotar).
     */
    EstadoJogo aguardarEstadoDiferenteDe(EstadoJogo anterior, long prazoMs) throws InterruptedException;
}
