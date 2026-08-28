package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServer extends Remote {
    String NOME_SERVICO = "QuoridorServer";

    int registrar(ClientCallback callback, String nomeJogador) throws RemoteException;
    void mover(int idJogador, Posicao destino) throws RemoteException, JogadaInvalidaException;
    void colocarCerca(int idJogador, Cerca cerca) throws RemoteException, JogadaInvalidaException;
    EstadoJogo obterEstado() throws RemoteException;
}
