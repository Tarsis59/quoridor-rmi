package client;

import common.EstadoJogo;
import common.GameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        String nome = "Jogador";
        String host = "localhost";
        int porta = 1099;
        boolean bot = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nome" -> { if (i + 1 < args.length) nome = args[++i]; }
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--porta" -> { if (i + 1 < args.length) porta = Integer.parseInt(args[++i]); }
                case "--bot" -> bot = true;
                default -> { }
            }
        }

        GameServer server = conectar(host, porta);
        ConsoleUI ui = new ConsoleUI();
        ClientCallbackImpl callback = new ClientCallbackImpl(ui);
        int id = server.registrar(callback, nome);
        ui.setMeuId(id);
        System.out.println("[CLIENTE] Registrado como Jogador " + id + " (" + nome + ").");

        if (bot) {
            BotJogador botJogador = new BotJogador(id);
            System.out.println("[BOT " + id + "] Modo automático ativo.");
            while (true) {
                EstadoJogo e = ui.getEstadoAtual();
                if (e == null) {
                    Thread.sleep(200);
                    continue;
                }
                if (e.getStatus() == EstadoJogo.Status.FINALIZADO) break;
                if (e.getJogadorDaVez() == id) {
                    botJogador.executarJogada(server, e);
                } else {
                    Thread.sleep(100);
                }
            }
            EstadoJogo fim = ui.getEstadoAtual();
            System.out.println("[BOT " + id + "] FIM vencedor=" + fim.getVencedor()
                    + " nome=" + fim.getNome(fim.getVencedor()));
            System.exit(0);
        } else {
            ui.loop(server);
            System.exit(0);
        }
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
