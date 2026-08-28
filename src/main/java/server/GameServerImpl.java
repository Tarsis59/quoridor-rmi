package server;

import common.Cerca;
import common.ClientCallback;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import engine.Partida;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServerImpl extends UnicastRemoteObject implements GameServer {
    private static final long serialVersionUID = 1L;
    public static final int MAX_JOGADORES = 4;

    private final Partida partida = new Partida();
    private final Map<Integer, ClientCallback> callbacks = new ConcurrentHashMap<>();
    private final Map<Integer, String> nomes = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> conectados = new ConcurrentHashMap<>();

    public GameServerImpl() throws RemoteException {
        super();
        Thread vigia = new Thread(this::vigiarDesconexoes);
        vigia.setDaemon(true);
        vigia.start();
    }

    @Override
    public synchronized int registrar(ClientCallback callback, String nomeJogador) throws RemoteException {
        if (callbacks.size() >= MAX_JOGADORES) {
            throw new RemoteException("Limite de " + MAX_JOGADORES + " jogadores atingido.");
        }
        int id = callbacks.size() + 1;
        callbacks.put(id, callback);
        nomes.put(id, nomeJogador);
        conectados.put(id, true);
        partida.setNome(id, nomeJogador);
        System.out.println("[SERVIDOR] Jogador " + id + " (" + nomeJogador + ") registrado. "
                + callbacks.size() + "/" + MAX_JOGADORES);
        if (callbacks.size() == MAX_JOGADORES) {
            partida.iniciar();
            System.out.println("[SERVIDOR] Todos os jogadores conectados. A partida começou!");
            EstadoJogo estado = partida.gerarEstado();
            broadcast(cb -> cb.aoIniciarJogo(estado));
        }
        return id;
    }

    @Override
    public synchronized void mover(int idJogador, Posicao destino)
            throws RemoteException, JogadaInvalidaException {
        partida.mover(idJogador, destino);
        System.out.println("[SERVIDOR] Jogador " + idJogador + " moveu para " + destino);
        aposJogada();
    }

    @Override
    public synchronized void colocarCerca(int idJogador, Cerca cerca)
            throws RemoteException, JogadaInvalidaException {
        partida.colocarCerca(idJogador, cerca);
        System.out.println("[SERVIDOR] Jogador " + idJogador + " colocou cerca " + cerca);
        aposJogada();
    }

    @Override
    public EstadoJogo obterEstado() throws RemoteException {
        return partida.gerarEstado();
    }

    private void aposJogada() {
        EstadoJogo estado = partida.gerarEstado();
        if (estado.getStatus() == EstadoJogo.Status.FINALIZADO) {
            System.out.println("[FIM] vencedor=" + estado.getVencedor()
                    + " nome=" + estado.getNome(estado.getVencedor()));
            broadcast(cb -> cb.aoFinalizarJogo(estado.getVencedor()));
            broadcast(cb -> cb.aoAtualizarEstado(estado));
        } else {
            broadcast(cb -> cb.aoAtualizarEstado(estado));
        }
    }

    private void vigiarDesconexoes() {
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            synchronized (this) {
                if (partida.getStatus() == EstadoJogo.Status.EM_ANDAMENTO
                        && !Boolean.TRUE.equals(conectados.get(partida.getJogadorDaVez()))) {
                    System.out.println("[SERVIDOR] Jogador " + partida.getJogadorDaVez()
                            + " está desconectado. Avançando o turno.");
                    partida.avancarTurno();
                    broadcast(cb -> cb.aoAtualizarEstado(partida.gerarEstado()));
                }
            }
        }
    }

    private interface OperacaoCallback {
        void executar(ClientCallback cb) throws RemoteException;
    }

    private void broadcast(OperacaoCallback operacao) {
        for (Map.Entry<Integer, ClientCallback> entrada : callbacks.entrySet()) {
            try {
                operacao.executar(entrada.getValue());
            } catch (RemoteException e) {
                conectados.put(entrada.getKey(), false);
                System.out.println("[SERVIDOR] Jogador " + entrada.getKey()
                        + " desconectado (RemoteException): " + e.getMessage());
            }
        }
    }
}
