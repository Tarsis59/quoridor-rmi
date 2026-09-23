package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void aoIniciarJogo(EstadoJogo estado) throws RemoteException;
    void aoAtualizarEstado(EstadoJogo estado) throws RemoteException;
    void aoFinalizarJogo(int idVencedor) throws RemoteException;

    /** Batimento cardíaco: o servidor chama periodicamente para detectar clientes que caíram. */
    void ping() throws RemoteException;
}
