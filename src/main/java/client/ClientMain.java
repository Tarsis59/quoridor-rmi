package client;

import client.ui.DialogoModo;
import client.ui.GraphicUI;
import common.EstadoJogo;
import common.GameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {

    /** Batida de ritmo no modo automático com GUI: cada bot checa o estado ~1x a cada 2s. */
    private static final long ATRASO_AUTO_MS = 2000;
    /** Pausa de quem joga (3x a batida): dá janela para os outros 3 jogadores agirem
     *  e o jogo anda em ~1 jogada/2s até alguém vencer (~1 min no total). */
    private static final long ATRASO_MOVER_MS = 6000;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        String nome = "Jogador";
        String host = "localhost";
        int porta = 1099;
        boolean bot = false;
        boolean gui = false;
        String modo = null; // "manual" | "auto"
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nome" -> { if (i + 1 < args.length) nome = args[++i]; }
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--porta" -> { if (i + 1 < args.length) porta = Integer.parseInt(args[++i]); }
                case "--bot" -> bot = true;
                case "--gui" -> gui = true;
                case "--modo" -> { if (i + 1 < args.length) modo = args[++i]; }
                default -> { }
            }
        }

        GameServer server = conectar(host, porta);

        JogadorUI ui;
        boolean automatico;
        if (gui) {
            String modoFinal = modo;
            if (modoFinal == null) {
                modoFinal = DialogoModo.perguntar(null);
                if (modoFinal == null) {
                    System.out.println("[CLIENTE] Nenhum modo escolhido. Encerrando.");
                    return;
                }
            }
            GraphicUI graphic = new GraphicUI(modoFinal);
            graphic.setServidor(server);
            ui = graphic;
            automatico = graphic.modoAutomatico();
        } else {
            ui = new ConsoleUI();
            automatico = bot;
        }

        ClientCallbackImpl callback = new ClientCallbackImpl(ui);
        int id = server.registrar(callback, nome);
        ui.setMeuId(id);
        System.out.println("[CLIENTE] Registrado como Jogador " + id + " (" + nome + ").");

        if (gui) {
            ((GraphicUI) ui).abrir();
        }

        if (automatico) {
            BotJogador botJogador = new BotJogador(id);
            System.out.println("[BOT " + id + "] Modo automático ativo.");
            while (true) {
                EstadoJogo e = ui.getEstadoAtual();
                if (e == null) {
                    Thread.sleep(200);
                    continue;
                }
                if (e.getStatus() == EstadoJogo.Status.FINALIZADO) break;
                if (gui) {
                    Thread.sleep(ATRASO_AUTO_MS);
                    e = ui.getEstadoAtual();
                    if (e == null) continue;
                    if (e.getStatus() == EstadoJogo.Status.FINALIZADO) break;
                }
                if (e.getJogadorDaVez() == id) {
                    botJogador.executarJogada(server, e);
                    // Pausa de quem joga: dá aos outros 3 uma janela de ~2s cada
                    // para agir, mantendo o ritmo uniforme de ~1 jogada/2s.
                    if (gui) Thread.sleep(ATRASO_MOVER_MS);
                } else if (!gui) {
                    Thread.sleep(100);
                }
            }
            EstadoJogo fim = ui.getEstadoAtual();
            System.out.println("[BOT " + id + "] FIM vencedor=" + fim.getVencedor()
                    + " nome=" + fim.getNome(fim.getVencedor()));
            System.exit(0);
        } else if (!gui) {
            ((ConsoleUI) ui).loop(server);
            System.exit(0);
        }
        // GUI em modo manual: a EDT (Event Dispatch Thread) mantém o app vivo
        // até a janela ser fechada (EXIT_ON_CLOSE).
    }

    private static GameServer conectar(String host, int porta) throws Exception {
        Exception ultimoErro = null;
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            try {
                Registry registry = LocateRegistry.getRegistry(host, porta);
                GameServer server = (GameServer) registry.lookup(GameServer.NOME_SERVICO);
                System.out.println("[CLIENTE] Conectado em rmi://" + host + ":" + porta + "/" + GameServer.NOME_SERVICO);
                return server;
            } catch (Exception e) {
                ultimoErro = e;
                System.out.println("[CLIENTE] Servidor não encontrado, tentando novamente em 1s... ("
                        + (tentativa + 1) + "/10)");
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("Não foi possível conectar ao servidor.", ultimoErro);
    }
}
