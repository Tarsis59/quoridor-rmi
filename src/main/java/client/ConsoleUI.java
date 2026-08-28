package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;

import java.rmi.RemoteException;
import java.util.Scanner;

public class ConsoleUI implements UI {
    private volatile EstadoJogo estadoAtual;
    private int meuId = 0;

    @Override
    public synchronized void novoEstado(EstadoJogo estado) {
        this.estadoAtual = estado;
        imprimirEstado(estado);
    }

    @Override
    public synchronized void finalizar(int idVencedor) {
        System.out.println("=== FIM DE JOGO ===");
        EstadoJogo e = estadoAtual;
        System.out.println("O vencedor é o Jogador " + idVencedor
                + " (" + (e == null ? "Jogador " + idVencedor : e.getNome(idVencedor)) + ").");
    }

    public synchronized EstadoJogo getEstadoAtual() { return estadoAtual; }

    public void setMeuId(int id) { this.meuId = id; }

    public void loop(GameServer server) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao Quoridor Distribuído! Você é o Jogador " + meuId + ".");
        imprimirAjuda();
        while (true) {
            EstadoJogo e = estadoAtual;
            if (e == null) {
                System.out.println("[CLIENTE] Aguardando o início da partida (precisa de 4 jogadores)...");
                dormir(500);
                continue;
            }
            if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
                System.out.println("=== FIM DE JOGO ===");
                System.out.println("O vencedor é o Jogador " + e.getVencedor()
                        + " (" + e.getNome(e.getVencedor()) + ").");
                break;
            }
            imprimirEstado(e);
            if (e.getJogadorDaVez() != meuId) {
                System.out.println("[CLIENTE] Vez do Jogador " + e.getJogadorDaVez() + ". Aguardando...");
                dormir(400);
                continue;
            }
            System.out.print("Sua vez (Jogador " + meuId + "). Comando: ");
            if (!scanner.hasNextLine()) break;
            String linha = scanner.nextLine().trim().toLowerCase();
            if (linha.isEmpty()) continue;
            if (linha.equals("sair")) break;
            if (linha.equals("ajuda")) { imprimirAjuda(); continue; }
            try {
                if (linha.startsWith("mover")) {
                    Posicao destino = interpretarDestino(linha, e);
                    if (destino == null) {
                        System.out.println("Uso: mover cima|baixo|esquerda|direita  ou  mover <linha> <coluna>");
                        continue;
                    }
                    server.mover(meuId, destino);
                } else if (linha.startsWith("cerca")) {
                    Cerca cerca = interpretarCerca(linha);
                    if (cerca == null) {
                        System.out.println("Uso: cerca <linha> <coluna> <h|v>   (linha/coluna de 0 a 7)");
                        continue;
                    }
                    server.colocarCerca(meuId, cerca);
                } else {
                    System.out.println("Comando desconhecido. Use ajuda para ver os comandos.");
                }
            } catch (JogadaInvalidaException ex) {
                System.out.println("[ERRO DE JOGADA] " + ex.getMessage());
            } catch (RemoteException ex) {
                System.out.println("[ERRO DE REDE] " + ex.getMessage());
                break;
            }
        }
        scanner.close();
    }

    private Posicao interpretarDestino(String linha, EstadoJogo e) {
        String[] partes = linha.split("\\s+");
        Posicao atual = e.getPosicao(meuId);
        if (partes.length == 2) {
            return switch (partes[1]) {
                case "cima" -> new Posicao(atual.linha() - 1, atual.coluna());
                case "baixo" -> new Posicao(atual.linha() + 1, atual.coluna());
                case "esquerda" -> new Posicao(atual.linha(), atual.coluna() - 1);
                case "direita" -> new Posicao(atual.linha(), atual.coluna() + 1);
                default -> null;
            };
        }
        if (partes.length == 3) {
            try {
                int l = Integer.parseInt(partes[1]);
                int c = Integer.parseInt(partes[2]);
                return new Posicao(l, c);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Cerca interpretarCerca(String linha) {
        String[] partes = linha.split("\\s+");
        if (partes.length != 4) return null;
        try {
            int l = Integer.parseInt(partes[1]);
            int c = Integer.parseInt(partes[2]);
            Orientacao o = partes[3].startsWith("h") ? Orientacao.HORIZONTAL : Orientacao.VERTICAL;
            return new Cerca(new Posicao(l, c), o);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public synchronized void imprimirEstado(EstadoJogo e) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- TABULEIRO 9x9 ---\n");
        sb.append("   ");
        for (int c = 0; c < 9; c++) sb.append("  ").append(c).append(" ");
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
                    sb.append(mapH[r][c] ? "---" : "   ");
                    if (c < 8) sb.append("+");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
        for (int i = 1; i <= 4; i++) {
            Posicao p = e.getPosicao(i);
            sb.append("J").append(i).append(" ").append(e.getNome(i))
              .append(" em (").append(p.linha()).append(",").append(p.coluna()).append(")")
              .append(" | Cercas: ").append(e.getCercasRestantes(i))
              .append(i == e.getJogadorDaVez() ? "  << VEZ" : "").append("\n");
        }
        if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
            sb.append("VENCEDOR: Jogador ").append(e.getVencedor()).append("\n");
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
        System.out.println("  mover cima|baixo|esquerda|direita");
        System.out.println("  mover <linha> <coluna>");
        System.out.println("  cerca <linha> <coluna> <h|v>   (cerca horizontal ou vertical)");
        System.out.println("  ajuda | sair");
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
