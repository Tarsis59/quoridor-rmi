package client;

import client.ui.DialogoModo;
import client.ui.GraphicUI;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Sessao;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {

    /** No modo automático com GUI, pausa antes de cada jogada do bot para dar para acompanhar. */
    private static final long ATRASO_JOGADA_GUI_MS = 1000;

    public static void main(String[] args) throws Exception {
        String nome = "Jogador";
        String host = "localhost";
        String hostnameLocal = "localhost";
        int porta = 1099;
        boolean bot = false;
        boolean gui = false;
        String modo = null; // "manual" | "auto"
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nome" -> { if (i + 1 < args.length) nome = args[++i]; }
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--hostname" -> { if (i + 1 < args.length) hostnameLocal = args[++i]; }
                case "--porta" -> { if (i + 1 < args.length) porta = Integer.parseInt(args[++i]); }
                case "--bot" -> bot = true;
                case "--gui" -> gui = true;
                case "--modo" -> { if (i + 1 < args.length) modo = args[++i]; }
                default -> System.out.println("[CLIENTE] Argumento ignorado: " + args[i]);
            }
        }
        // Endereço em que o servidor alcança o callback deste cliente (IP da máquina, em rede).
        System.setProperty("java.rmi.server.hostname", hostnameLocal);

        GameServer server = conectar(host, porta);

        JogadorUI ui;
        boolean automatico;
        if (gui) {
            String modoFinal = modo;
            if (modoFinal == null) {
                modoFinal = DialogoModo.perguntar(null);
                if (modoFinal == null) {
                    System.out.println("[CLIENTE] Nenhum modo escolhido. Encerrando.");
                    System.exit(0);
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
        Sessao sessao;
        try {
            sessao = server.registrar(callback, nome);
        } catch (RemoteException e) {
            System.out.println("[CLIENTE] Não foi possível entrar na partida: " + causaRaiz(e));
            System.exit(1);
            return;
        }
        ui.setSessao(sessao);
        int id = sessao.idJogador();
        System.out.println("[CLIENTE] Registrado como Jogador " + id + " (" + nome + ").");

        if (gui) {
            ((GraphicUI) ui).abrir();
        }

        if (automatico) {
            jogarAutomaticamente(server, ui, sessao, gui);
            EstadoJogo fim = ui.getEstadoAtual();
            int vencedor = fim.getVencedor();
            System.out.println("[BOT " + id + "] FIM vencedor=" + vencedor
                    + " nome=" + (vencedor == 0 ? "-" : fim.getNome(vencedor)));
            if (!gui) System.exit(0);
            // Com GUI a janela fica aberta mostrando o resultado até o usuário fechar.
        } else if (!gui) {
            ((ConsoleUI) ui).loop(server);
            System.exit(0);
        }
        // GUI em modo manual: a EDT (Event Dispatch Thread) mantém o app vivo
        // até a janela ser fechada (EXIT_ON_CLOSE).
    }

    /**
     * Laço do bot: espera cada novo estado vindo do servidor (callback) e joga quando é a
     * vez dele. Age no máximo uma vez por estado recebido, então nunca repete jogada com
     * informação velha.
     */
    private static void jogarAutomaticamente(GameServer server, JogadorUI ui, Sessao sessao, boolean gui)
            throws InterruptedException {
        int id = sessao.idJogador();
        BotJogador botJogador = new BotJogador(sessao);
        System.out.println("[BOT " + id + "] Modo automático ativo.");
        EstadoJogo visto = null;
        while (true) {
            // Dorme até chegar um estado novo (ou 1 s): sem espera ativa.
            EstadoJogo e = ui.aguardarEstadoDiferenteDe(visto, 1000);
            if (e == null || e == visto) continue;
            visto = e;
            if (e.getStatus() == EstadoJogo.Status.FINALIZADO) return;
            if (e.getStatus() != EstadoJogo.Status.EM_ANDAMENTO || e.getJogadorDaVez() != id) continue;
            if (gui) Thread.sleep(ATRASO_JOGADA_GUI_MS);
            try {
                botJogador.executarJogada(server, e);
            } catch (JogadaInvalidaException ex) {
                System.out.println("[BOT " + id + "] Jogada recusada: " + ex.getMessage());
                visto = null; // reavalia com o estado atual
                Thread.sleep(200);
            } catch (RemoteException ex) {
                System.out.println("[BOT " + id + "] Conexão com o servidor perdida: " + causaRaiz(ex));
                System.exit(1);
            } catch (Exception ex) {
                System.out.println("[BOT " + id + "] Erro ao jogar: " + ex.getMessage());
                visto = null;
                Thread.sleep(200);
            }
        }
    }

    static String causaRaiz(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
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
