package server;

import common.GameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        int porta = 1099;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--porta".equals(args[i])) porta = Integer.parseInt(args[i + 1]);
        }

        Registry registry = LocateRegistry.createRegistry(porta);
        GameServer servidor = new GameServerImpl();
        registry.rebind(GameServer.NOME_SERVICO, servidor);

        System.out.println("[SERVIDOR] Registry RMI criado na porta " + porta);
        System.out.println("[SERVIDOR] Serviço '" + GameServer.NOME_SERVICO
                + "' disponível em rmi://localhost:" + porta + "/" + GameServer.NOME_SERVICO);
        System.out.println("[SERVIDOR] Aguardando " + GameServerImpl.MAX_JOGADORES + " jogadores...");

        synchronized (ServerMain.class) {
            ServerMain.class.wait();
        }
    }
}
