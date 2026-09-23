package client;

import common.ClientCallback;
import common.EstadoJogo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private static final long serialVersionUID = 1L;

    private final UI ui;

    public ClientCallbackImpl(UI ui) throws RemoteException {
        super();
        this.ui = ui;
    }

    @Override
    public void aoIniciarJogo(EstadoJogo estado) throws RemoteException {
        ui.novoEstado(estado);
    }

    @Override
    public void aoAtualizarEstado(EstadoJogo estado) throws RemoteException {
        ui.novoEstado(estado);
    }

    @Override
    public void aoFinalizarJogo(int idVencedor) throws RemoteException {
        ui.finalizar(idVencedor);
    }

    @Override
    public void ping() {
        // Responder já basta: prova ao servidor que este processo continua vivo.
    }
}
