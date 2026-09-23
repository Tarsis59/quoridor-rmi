package e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class E2ETest {
    private final List<Process> processos = new ArrayList<>();
    private final Map<Process, StringBuilder> logs = new ConcurrentHashMap<>();

    /** Porta alta aleatória, para o teste não colidir com um servidor do jogo já aberto na 1099. */
    private static String portaLivre() {
        return String.valueOf(20000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(20000));
    }

    @Test
    void partidaCompletaComQuatroBots() throws Exception {
        String classpath = System.getProperty("java.class.path");
        String porta = portaLivre();

        Process servidor = iniciar(classpath, "server.ServerMain", "--porta", porta);
        Thread.sleep(4000);

        List<Process> clientes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            clientes.add(iniciar(classpath, "client.ClientMain", "--nome", "Bot" + i, "--bot", "--porta", porta));
            Thread.sleep(300);
        }

        long prazo = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        String logServidor = "";
        while (System.currentTimeMillis() < prazo) {
            logServidor = logDe(servidor);
            if (logServidor.contains("[FIM]")) break;
            if (!servidor.isAlive()) break;
            Thread.sleep(500);
        }

        System.out.println("===== LOG SERVIDOR =====");
        System.out.println(logServidor);

        // A partida terminou no servidor; dá uma janela curta para os 4 clientes
        // receberem o estado final via callback e imprimirem "FIM vencedor=" antes do teardown.
        long prazoFim = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < prazoFim
                && !clientes.stream().allMatch(c -> logDe(c).contains("FIM vencedor="))) {
            Thread.sleep(200);
        }

        for (Process p : processos) p.destroy();

        assertTrue(logServidor.contains("[FIM]"), "A partida deveria terminar. Log do servidor:\n" + logServidor);
        assertTrue(logServidor.contains("Jogador 4 ("), "Deveriam registrar 4 jogadores. Log:\n" + logServidor);

        String linhaVencedor = logServidor.lines()
                .filter(l -> l.contains("[FIM]"))
                .findFirst()
                .orElse("");
        assertTrue(linhaVencedor.matches(".*vencedor=[1-4].*"), "Deveria haver um vencedor 1-4: " + linhaVencedor);
        int vencedor = extrairVencedor(linhaVencedor);

        for (int i = 0; i < clientes.size(); i++) {
            String log = logDe(clientes.get(i));
            assertTrue(log.contains("FIM vencedor="), "Cliente " + (i + 1) + " deveria receber o fim:\n" + log);
            assertTrue(log.contains("vencedor=" + vencedor),
                    "Cliente " + (i + 1) + " deveria ver o mesmo vencedor (" + vencedor + "):\n" + log);
        }
        System.out.println("E2E OK: partida completa terminou com vencedor=" + vencedor
                + " e os 4 clientes receberam o estado final via callback.");
    }

    @Test
    void partidaContinuaQuandoUmClienteCai() throws Exception {
        String classpath = System.getProperty("java.class.path");
        String porta = portaLivre();

        Process servidor = iniciar(classpath, "server.ServerMain", "--porta", porta);
        Thread.sleep(4000);
        List<Process> clientes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            clientes.add(iniciar(classpath, "client.ClientMain", "--nome", "Bot" + i, "--bot", "--porta", porta));
            Thread.sleep(300);
        }
        // Espera a partida começar e derruba o processo do Jogador 2 à força (queda de rede / janela fechada).
        long prazoInicio = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (System.currentTimeMillis() < prazoInicio && !logDe(servidor).contains("A partida começou")) {
            Thread.sleep(200);
        }
        clientes.get(1).destroyForcibly();

        long prazo = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        String logServidor = "";
        while (System.currentTimeMillis() < prazo) {
            logServidor = logDe(servidor);
            if (logServidor.contains("[FIM]") || !servidor.isAlive()) break;
            Thread.sleep(500);
        }
        System.out.println("===== LOG SERVIDOR (queda) =====");
        System.out.println(logServidor);
        for (Process p : processos) p.destroy();

        assertTrue(logServidor.contains("Jogador 2 desconectado"),
                "O servidor deveria detectar a queda do J2. Log:\n" + logServidor);
        assertTrue(logServidor.contains("[FIM]"), "A partida deveria terminar mesmo sem o J2. Log:\n" + logServidor);
        assertTrue(!logServidor.contains("[FIM] vencedor=2"), "Quem caiu não pode vencer. Log:\n" + logServidor);
    }

    private int extrairVencedor(String linha) {
        String[] partes = linha.split("vencedor=");
        return Integer.parseInt(partes[1].trim().split("\\s+")[0]);
    }

    private Process iniciar(String classpath, String classe, String... extra) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        // Low-RAM adaptation: keep each spawned JVM tiny (heap, metaspace, code cache
        // and C1-only JIT) so 5 concurrent processes fit on machines with little RAM
        // and do not hit the Windows paging-file commit limit (DOS error 1455).
        cmd.add("-Xmx64m");
        cmd.add("-Xms16m");
        cmd.add("-Xss512k");
        cmd.add("-XX:+UseSerialGC");
        cmd.add("-XX:MaxMetaspaceSize=64m");
        cmd.add("-XX:ReservedCodeCacheSize=16m");
        cmd.add("-XX:TieredStopAtLevel=1");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(classe);
        for (String a : extra) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        processos.add(p);
        StringBuilder log = new StringBuilder();
        logs.put(p, log);
        Thread leitor = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    synchronized (log) {
                        log.append(linha).append("\n");
                    }
                }
            } catch (IOException ignorada) {
                // processo terminou
            }
        });
        leitor.setDaemon(true);
        leitor.start();
        return p;
    }

    private String logDe(Process p) {
        StringBuilder sb = logs.get(p);
        if (sb == null) return "";
        synchronized (sb) {
            return sb.toString();
        }
    }

    @AfterEach
    void limpar() {
        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
                if (p.isAlive()) p.destroyForcibly();
            }
        }
        processos.clear();
    }
}
