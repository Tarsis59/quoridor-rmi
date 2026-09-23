package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;
import common.Sessao;
import engine.Tabuleiro;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Interface orientada a caracteres (terminal). */
public class ConsoleUI implements JogadorUI {
    private final CaixaEstado caixa = new CaixaEstado();
    private volatile Sessao sessao;
    private volatile int meuId = 0;

    @Override
    public void novoEstado(EstadoJogo estado) {
        caixa.publicar(estado);
        imprimirEstado(estado);
    }

    @Override
    public synchronized void finalizar(int idVencedor) {
        EstadoJogo e = caixa.atual();
        System.out.println("=== FIM DE JOGO ===");
        if (idVencedor == 0) {
            System.out.println("Partida encerrada sem vencedor (todos saíram).");
        } else {
            System.out.println("O vencedor é o Jogador " + idVencedor
                    + " (" + (e == null ? "Jogador " + idVencedor : e.getNome(idVencedor)) + ")."
                    + (idVencedor == meuId ? " Parabéns, você venceu!" : ""));
        }
    }

    @Override
    public EstadoJogo getEstadoAtual() { return caixa.atual(); }

    @Override
    public EstadoJogo aguardarEstadoDiferenteDe(EstadoJogo anterior, long prazoMs) throws InterruptedException {
        return caixa.aguardarDiferenteDe(anterior, prazoMs);
    }

    @Override
    public void setSessao(Sessao sessao) {
        this.sessao = sessao;
        this.meuId = sessao.idJogador();
    }

    @SuppressWarnings("resource") // Scanner sobre System.in: não deve ser fechado.
    public void loop(GameServer server) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao Quoridor Distribuído! Você é o Jogador " + meuId
                + " (meta: " + descreverMeta(meuId) + ").");
        imprimirAjuda();
        boolean avisouEspera = false;
        EstadoJogo anunciado = null;
        try {
            while (true) {
                EstadoJogo e = caixa.atual();
                if (e == null || e.getStatus() == EstadoJogo.Status.AGUARDANDO) {
                    if (!avisouEspera) {
                        System.out.println("[CLIENTE] Aguardando o início da partida (precisa de 4 jogadores)...");
                        avisouEspera = true;
                    }
                    caixa.aguardarDiferenteDe(e, 1000);
                    continue;
                }
                if (e.getStatus() == EstadoJogo.Status.FINALIZADO) break;
                if (e.getJogadorDaVez() != meuId) {
                    if (e != anunciado) {
                        System.out.println("[CLIENTE] Vez do Jogador " + e.getJogadorDaVez()
                                + " (" + e.getNome(e.getJogadorDaVez()) + "). Aguardando...");
                        anunciado = e;
                    }
                    caixa.aguardarDiferenteDe(e, 1000);
                    continue;
                }
                Tabuleiro tabuleiro = Tabuleiro.aPartirDe(e.getPosicoes(), e.getCercas());
                if (e != anunciado) {
                    System.out.println("Movimentos válidos: " + formatar(tabuleiro.movimentosValidos(meuId))
                            + " | Cercas restantes: " + e.getCercasRestantes(meuId));
                    anunciado = e;
                }
                System.out.print("Sua vez (Jogador " + meuId + "). Comando: ");
                if (!scanner.hasNextLine()) break;
                String linha = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                if (linha.isEmpty()) continue;
                if (linha.equals("sair")) break;
                if (linha.equals("ajuda")) { imprimirAjuda(); continue; }
                if (linha.equals("tabuleiro")) { imprimirEstado(e); continue; }
                executarComando(server, linha, e, tabuleiro);
            }
        } catch (RemoteException ex) {
            System.out.println("[ERRO DE REDE] Conexão com o servidor perdida: " + ClientMain.causaRaiz(ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void executarComando(GameServer server, String linha, EstadoJogo e, Tabuleiro tabuleiro)
            throws RemoteException {
        try {
            if (linha.startsWith("mover") || linha.startsWith("m ")) {
                Posicao destino = interpretarDestino(linha, e, tabuleiro);
                if (destino == null) {
                    System.out.println("Uso: mover cima|baixo|esquerda|direita  ou  mover <linha> <coluna>");
                    return;
                }
                server.mover(sessao, destino);
            } else if (linha.startsWith("cerca") || linha.startsWith("c ")) {
                Cerca cerca = interpretarCerca(linha);
                if (cerca == null) {
                    System.out.println("Uso: cerca <linha> <coluna> <h|v>   (linha e coluna de 0 a 7)");
                    return;
                }
                server.colocarCerca(sessao, cerca);
            } else {
                System.out.println("Comando desconhecido. Digite ajuda para ver os comandos.");
            }
        } catch (JogadaInvalidaException ex) {
            System.out.println("[JOGADA INVÁLIDA] " + ex.getMessage());
        }
    }

    /**
     * Direção: vai para a casa vizinha; se ela estiver ocupada por um peão e o pulo reto for
     * legal, pula. Pulos diagonais são feitos informando a casa: "mover linha coluna".
     */
    Posicao interpretarDestino(String linha, EstadoJogo e, Tabuleiro tabuleiro) {
        String[] partes = linha.split("\\s+");
        Posicao atual = e.getPosicao(meuId);
        if (partes.length == 2) {
            int[] d = switch (partes[1]) {
                case "cima", "c", "w" -> new int[]{-1, 0};
                case "baixo", "b", "s" -> new int[]{1, 0};
                case "esquerda", "e", "a" -> new int[]{0, -1};
                case "direita", "d" -> new int[]{0, 1};
                default -> null;
            };
            if (d == null) return null;
            Posicao passo = new Posicao(atual.linha() + d[0], atual.coluna() + d[1]);
            Posicao pulo = new Posicao(atual.linha() + 2 * d[0], atual.coluna() + 2 * d[1]);
            List<Posicao> validos = tabuleiro.movimentosValidos(meuId);
            if (!validos.contains(passo) && validos.contains(pulo)) return pulo;
            return passo;
        }
        if (partes.length == 3) {
            try {
                return new Posicao(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    static Cerca interpretarCerca(String linha) {
        String[] partes = linha.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (partes.length != 4) return null;
        Orientacao o = switch (partes[3]) {
            case "h", "horizontal" -> Orientacao.HORIZONTAL;
            case "v", "vertical" -> Orientacao.VERTICAL;
            default -> null;
        };
        if (o == null) return null;
        try {
            return new Cerca(new Posicao(Integer.parseInt(partes[1]), Integer.parseInt(partes[2])), o);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatar(List<Posicao> posicoes) {
        if (posicoes.isEmpty()) return "(nenhum — coloque uma cerca)";
        return posicoes.stream().map(p -> "(" + p.linha() + "," + p.coluna() + ")")
                .collect(Collectors.joining(" "));
    }

    private static String descreverMeta(int id) {
        return switch (id) {
            case 1 -> "chegar à linha 0, no topo";
            case 2 -> "chegar à linha 8, embaixo";
            case 3 -> "chegar à coluna 0, à esquerda";
            case 4 -> "chegar à coluna 8, à direita";
            default -> "?";
        };
    }

    public synchronized void imprimirEstado(EstadoJogo e) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- TABULEIRO 9x9 ---\n");
        sb.append("   ");
        for (int c = 0; c < 9; c++) sb.append(" ").append(c).append("  ");
        sb.append("\n");
        boolean[][] mapH = new boolean[8][9];
        boolean[][] mapV = new boolean[9][8];
        for (Cerca cer : e.getCercas()) {
            int r = cer.base().linha();
            int c = cer.base().coluna();
            if (cer.orientacao() == Orientacao.HORIZONTAL) {
                mapH[r][c] = true;
                mapH[r][c + 1] = true;
            } else {
                mapV[r][c] = true;
                mapV[r + 1][c] = true;
            }
        }
        for (int r = 0; r < 9; r++) {
            sb.append(r).append("  ");
            for (int c = 0; c < 9; c++) {
                sb.append("[").append(charPeao(e, r, c)).append("]");
                if (c < 8) sb.append(mapV[r][c] ? "|" : " ");
            }
            sb.append("\n");
            if (r < 8) {
                sb.append("   ");
                for (int c = 0; c < 9; c++) {
                    sb.append(mapH[r][c] ? "===" : "   ");
                    if (c < 8) sb.append(" ");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
        if (e.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            sb.append("Aguardando jogadores: ").append(e.getRegistrados()).append("/4\n");
        }
        for (int i = 1; i <= 4; i++) {
            Posicao p = e.getPosicao(i);
            sb.append("J").append(i).append(" ").append(e.getNome(i))
              .append(" em (").append(p.linha()).append(",").append(p.coluna()).append(")")
              .append(" | Cercas: ").append(e.getCercasRestantes(i))
              .append(e.isAtivo(i) ? "" : " | DESCONECTADO")
              .append(i == meuId ? " (você)" : "")
              .append(e.getStatus() == EstadoJogo.Status.EM_ANDAMENTO && i == e.getJogadorDaVez()
                      ? "  << VEZ" : "")
              .append("\n");
        }
        if (e.getStatus() == EstadoJogo.Status.FINALIZADO && e.getVencedor() > 0) {
            sb.append("VENCEDOR: Jogador ").append(e.getVencedor())
              .append(" (").append(e.getNome(e.getVencedor())).append(")\n");
        }
        System.out.print(sb);
    }

    private String charPeao(EstadoJogo e, int r, int c) {
        for (int i = 1; i <= 4; i++) {
            Posicao p = e.getPosicao(i);
            if (p.linha() == r && p.coluna() == c) return String.valueOf(i);
        }
        return ".";
    }

    private void imprimirAjuda() {
        System.out.println("Comandos:");
        System.out.println("  mover cima|baixo|esquerda|direita   (pula o peão à frente se possível)");
        System.out.println("  mover <linha> <coluna>              (ex.: mover 7 4 — use para pulos diagonais)");
        System.out.println("  cerca <linha> <coluna> <h|v>        (base de 0 a 7; cada jogador tem 5 cercas)");
        System.out.println("      h: cerca abaixo das casas (l,c) e (l,c+1)");
        System.out.println("      v: cerca à direita das casas (l,c) e (l+1,c)");
        System.out.println("  tabuleiro | ajuda | sair");
    }
}
