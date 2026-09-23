package server;

import common.Cerca;
import common.ClientCallback;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import common.Sessao;
import engine.Partida;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Objeto remoto do servidor. Toda a lógica de regras fica em {@link Partida}; aqui ficam
 * registro dos 4 jogadores, autorização por sessão, notificação por callback (RMI) e
 * detecção de desconexão por batimento (ping).
 */
public class GameServerImpl extends UnicastRemoteObject implements GameServer {
    private static final long serialVersionUID = 1L;
    public static final int MAX_JOGADORES = Partida.NUM_JOGADORES;
    /** Intervalo do batimento que detecta clientes que caíram / fecharam a janela. */
    static final long INTERVALO_PING_MS = 1500;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Partida partida = new Partida();
    // Acesso sempre sob o lock do servidor (this); LinkedHashMap preserva a ordem 1..4.
    private final Map<Integer, ClientCallback> callbacks = new LinkedHashMap<>();
    private final Map<Integer, String> tokens = new LinkedHashMap<>();
    private final Map<Integer, Boolean> conectados = new LinkedHashMap<>();
    private final Map<Integer, Boolean> inicioNotificado = new LinkedHashMap<>();
    private final Map<Integer, Boolean> fimNotificado = new LinkedHashMap<>();
    private boolean fimRegistradoNoLog = false;

    public GameServerImpl() throws RemoteException {
        this(true);
    }

    /** @param vigiar false em testes que controlam a detecção de desconexão manualmente. */
    GameServerImpl(boolean vigiar) throws RemoteException {
        super();
        if (vigiar) {
            Thread vigia = new Thread(this::vigiarDesconexoes, "vigia-desconexoes");
            vigia.setDaemon(true);
            vigia.start();
        }
    }

    @Override
    public synchronized Sessao registrar(ClientCallback callback, String nomeJogador) throws RemoteException {
        if (callback == null) throw new RemoteException("Callback do cliente é obrigatório.");
        if (callbacks.size() >= MAX_JOGADORES) {
            throw new RemoteException("Sala cheia: limite de " + MAX_JOGADORES + " jogadores atingido.");
        }
        int id = callbacks.size() + 1;
        String token = gerarToken();
        callbacks.put(id, callback);
        tokens.put(id, token);
        conectados.put(id, true);
        partida.setNome(id, nomeJogador);
        partida.setRegistrados(callbacks.size());
        String nome = partida.gerarEstado().getNome(id);
        System.out.println("[SERVIDOR] Jogador " + id + " (" + nome + ") registrado. "
                + callbacks.size() + "/" + MAX_JOGADORES);
        if (callbacks.size() == MAX_JOGADORES) {
            partida.iniciar();
            System.out.println("[SERVIDOR] Todos os jogadores conectados. A partida começou!");
        }
        notificarTodos();
        return new Sessao(id, token);
    }

    @Override
    public synchronized void mover(Sessao sessao, Posicao destino)
            throws RemoteException, JogadaInvalidaException {
        int id = autenticar(sessao);
        partida.mover(id, destino);
        System.out.println("[SERVIDOR] Jogador " + id + " moveu para (" + destino.linha() + ","
                + destino.coluna() + ")");
        notificarTodos();
    }

    @Override
    public synchronized void colocarCerca(Sessao sessao, Cerca cerca)
            throws RemoteException, JogadaInvalidaException {
        int id = autenticar(sessao);
        partida.colocarCerca(id, cerca);
        System.out.println("[SERVIDOR] Jogador " + id + " colocou cerca " + cerca.orientacao()
                + " em (" + cerca.base().linha() + "," + cerca.base().coluna() + ")");
        notificarTodos();
    }

    @Override
    public EstadoJogo obterEstado() {
        return partida.gerarEstado();
    }

    /** Confere id + token da sessão. Comparação em tempo constante para não vazar o token. */
    private int autenticar(Sessao sessao) throws JogadaInvalidaException {
        if (sessao == null || sessao.token() == null) {
            throw new JogadaInvalidaException("Sessão inválida.");
        }
        String esperado = tokens.get(sessao.idJogador());
        if (esperado == null || !MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), sessao.token().getBytes(StandardCharsets.UTF_8))) {
            System.out.println("[SEGURANCA] Jogada rejeitada: sessão inválida para o id " + sessao.idJogador());
            throw new JogadaInvalidaException("Sessão inválida.");
        }
        if (!Boolean.TRUE.equals(conectados.get(sessao.idJogador()))) {
            throw new JogadaInvalidaException("Jogador desconectado não pode mais jogar.");
        }
        return sessao.idJogador();
    }

    private static String gerarToken() {
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Envia o estado atual a todos os clientes conectados (deve ser chamado sob o lock).
     * Cliente que falhar é marcado como desconectado e sai da partida; como isso muda o
     * estado (vez pulada / vitória por W.O.), repete o envio até não haver novas falhas.
     */
    private void notificarTodos() {
        while (true) {
            EstadoJogo estado = partida.gerarEstado();
            boolean finalizado = estado.getStatus() == EstadoJogo.Status.FINALIZADO;
            if (finalizado && !fimRegistradoNoLog) {
                fimRegistradoNoLog = true;
                System.out.println("[FIM] vencedor=" + estado.getVencedor()
                        + " nome=" + (estado.getVencedor() == 0 ? "-" : estado.getNome(estado.getVencedor())));
            }
            List<Integer> falhas = new ArrayList<>();
            for (Map.Entry<Integer, ClientCallback> entrada : callbacks.entrySet()) {
                int id = entrada.getKey();
                if (!Boolean.TRUE.equals(conectados.get(id))) continue;
                ClientCallback cb = entrada.getValue();
                try {
                    if (estado.getStatus() != EstadoJogo.Status.AGUARDANDO
                            && !Boolean.TRUE.equals(inicioNotificado.get(id))) {
                        inicioNotificado.put(id, true);
                        cb.aoIniciarJogo(estado);
                    } else {
                        cb.aoAtualizarEstado(estado);
                    }
                    // Estado final chega antes do aviso de fim, assim a UI já mostra o tabuleiro final.
                    if (finalizado && !Boolean.TRUE.equals(fimNotificado.get(id))) {
                        fimNotificado.put(id, true);
                        cb.aoFinalizarJogo(estado.getVencedor());
                    }
                } catch (RemoteException | RuntimeException e) {
                    falhas.add(id);
                }
            }
            if (falhas.isEmpty()) return;
            for (int id : falhas) marcarDesconectado(id);
        }
    }

    private void marcarDesconectado(int id) {
        if (!Boolean.TRUE.equals(conectados.get(id))) return;
        conectados.put(id, false);
        boolean emAndamento = partida.getStatus() == EstadoJogo.Status.EM_ANDAMENTO;
        partida.removerJogador(id);
        System.out.println("[SERVIDOR] Jogador " + id + " desconectado."
                + (emAndamento ? " Sua vez passa a ser pulada." : ""));
    }

    /** Chamado pelo vigia (e por testes): verifica com ping quem ainda responde. */
    void verificarConexoes() {
        Map<Integer, ClientCallback> copia;
        synchronized (this) {
            copia = new LinkedHashMap<>();
            for (Map.Entry<Integer, ClientCallback> e : callbacks.entrySet()) {
                if (Boolean.TRUE.equals(conectados.get(e.getKey()))) copia.put(e.getKey(), e.getValue());
            }
        }
        // Os pings acontecem fora do lock: um cliente lento não trava as jogadas dos outros.
        List<Integer> caidos = new ArrayList<>();
        for (Map.Entry<Integer, ClientCallback> e : copia.entrySet()) {
            try {
                e.getValue().ping();
            } catch (RemoteException | RuntimeException ex) {
                caidos.add(e.getKey());
            }
        }
        if (caidos.isEmpty()) return;
        synchronized (this) {
            boolean jaTerminada = partida.getStatus() == EstadoJogo.Status.FINALIZADO;
            for (int id : caidos) marcarDesconectado(id);
            // Após o fim, clientes saindo é o esperado: não há o que avisar.
            if (!jaTerminada) notificarTodos();
        }
    }

    private void vigiarDesconexoes() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(INTERVALO_PING_MS);
            } catch (InterruptedException e) {
                return;
            }
            verificarConexoes();
        }
    }
}
