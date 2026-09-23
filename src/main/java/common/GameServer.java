package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServer extends Remote {
    String NOME_SERVICO = "QuoridorServer";

    /** Registra um jogador (máx. 4). Devolve a sessão (id 1–4 + token) usada nas jogadas. */
    Sessao registrar(ClientCallback callback, String nomeJogador) throws RemoteException;
    void mover(Sessao sessao, Posicao destino) throws RemoteException, JogadaInvalidaException;
    void colocarCerca(Sessao sessao, Cerca cerca) throws RemoteException, JogadaInvalidaException;
    EstadoJogo obterEstado() throws RemoteException;
}
