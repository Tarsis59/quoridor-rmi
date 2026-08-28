package engine;

import common.Cerca;
import common.Orientacao;
import common.Posicao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Tabuleiro {
    public static final int TAMANHO = 9;
    public static final int NUM_JOGADORES = 4;

    private static final int[][] DIRECOES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final boolean[][] paredesH; // [8][9]: aresta horizontal entre (r,c) e (r+1,c)
    private final boolean[][] paredesV; // [9][8]: aresta vertical entre (r,c) e (r,c+1)
    private final Posicao[] posicoes;

    public Tabuleiro() {
        paredesH = new boolean[8][9];
        paredesV = new boolean[9][8];
        posicoes = new Posicao[NUM_JOGADORES];
        posicoes[0] = new Posicao(8, 4); // J1 embaixo -> meta linha 0
        posicoes[1] = new Posicao(0, 4); // J2 topo   -> meta linha 8
        posicoes[2] = new Posicao(4, 8); // J3 direita-> meta coluna 0
        posicoes[3] = new Posicao(4, 0); // J4 esquerda-> meta coluna 8
    }

    private Tabuleiro(Tabuleiro outro) {
        paredesH = new boolean[8][9];
        paredesV = new boolean[9][8];
        for (int r = 0; r < 8; r++) System.arraycopy(outro.paredesH[r], 0, paredesH[r], 0, 9);
        for (int r = 0; r < 9; r++) System.arraycopy(outro.paredesV[r], 0, paredesV[r], 0, 8);
        posicoes = new Posicao[NUM_JOGADORES];
        System.arraycopy(outro.posicoes, 0, posicoes, 0, NUM_JOGADORES);
    }

    public Tabuleiro copiar() { return new Tabuleiro(this); }

    public static Tabuleiro aPartirDe(Posicao[] posicoes, List<Cerca> cercas) {
        Tabuleiro t = new Tabuleiro();
        for (int i = 0; i < NUM_JOGADORES; i++) t.posicoes[i] = posicoes[i];
        for (Cerca c : cercas) {
            int r = c.base().linha();
            int col = c.base().coluna();
            if (c.orientacao() == Orientacao.HORIZONTAL) {
                t.paredesH[r][col] = true;
                t.paredesH[r][col + 1] = true;
            } else {
                t.paredesV[r][col] = true;
                t.paredesV[r + 1][col] = true;
            }
        }
        return t;
    }

    public boolean dentro(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    public Posicao getPosicao(int idJogador) { return posicoes[idJogador - 1]; }

    public boolean estaOcupada(Posicao p) {
        for (Posicao q : posicoes) {
            if (q.linha() == p.linha() && q.coluna() == p.coluna()) return true;
        }
        return false;
    }

    public boolean temParedeEntre(Posicao a, Posicao b) {
        int dr = b.linha() - a.linha();
        int dc = b.coluna() - a.coluna();
        if (dr == -1 && dc == 0) return paredesH[b.linha()][a.coluna()];
        if (dr == 1 && dc == 0) return paredesH[a.linha()][a.coluna()];
        if (dr == 0 && dc == -1) return paredesV[a.linha()][b.coluna()];
        if (dr == 0 && dc == 1) return paredesV[a.linha()][a.coluna()];
        return false;
    }

    public List<Posicao> movimentosValidos(int idJogador) {
        Posicao atual = getPosicao(idJogador);
        List<Posicao> resultado = new ArrayList<>();
        for (int[] d : DIRECOES) {
            Posicao vizinho = new Posicao(atual.linha() + d[0], atual.coluna() + d[1]);
            if (!dentro(vizinho.linha(), vizinho.coluna())) continue;
            if (temParedeEntre(atual, vizinho)) continue;
            if (!estaOcupada(vizinho)) {
                resultado.add(vizinho);
                continue;
            }
            Posicao depois = new Posicao(vizinho.linha() + d[0], vizinho.coluna() + d[1]);
            boolean depoisLivre = dentro(depois.linha(), depois.coluna())
                    && !temParedeEntre(vizinho, depois) && !estaOcupada(depois);
            if (depoisLivre) {
                resultado.add(depois);
            } else {
                Posicao lateral1 = new Posicao(vizinho.linha() + d[1], vizinho.coluna() + d[0]);
                if (dentro(lateral1.linha(), lateral1.coluna())
                        && !temParedeEntre(vizinho, lateral1) && !estaOcupada(lateral1)) {
                    resultado.add(lateral1);
                }
                Posicao lateral2 = new Posicao(vizinho.linha() - d[1], vizinho.coluna() - d[0]);
                if (dentro(lateral2.linha(), lateral2.coluna())
                        && !temParedeEntre(vizinho, lateral2) && !estaOcupada(lateral2)) {
                    resultado.add(lateral2);
                }
            }
        }
        return resultado;
    }

    public boolean podeColocarCerca(Cerca cerca) {
        Posicao base = cerca.base();
        int r = base.linha();
        int c = base.coluna();
        if (r < 0 || r > 7 || c < 0 || c > 7) return false;
        Tabuleiro sim = copiar();
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            if (paredesH[r][c] || paredesH[r][c + 1]) return false;
            if (paredesV[r][c] && paredesV[r + 1][c]) return false;
            sim.paredesH[r][c] = true;
            sim.paredesH[r][c + 1] = true;
        } else {
            if (paredesV[r][c] || paredesV[r + 1][c]) return false;
            if (paredesH[r][c] && paredesH[r][c + 1]) return false;
            sim.paredesV[r][c] = true;
            sim.paredesV[r + 1][c] = true;
        }
        return sim.todosTemCaminho();
    }

    public void moverPeao(int idJogador, Posicao destino) {
        posicoes[idJogador - 1] = destino;
    }

    public void colocarCerca(Cerca cerca) {
        int r = cerca.base().linha();
        int c = cerca.base().coluna();
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            paredesH[r][c] = true;
            paredesH[r][c + 1] = true;
        } else {
            paredesV[r][c] = true;
            paredesV[r + 1][c] = true;
        }
    }

    public boolean temCaminho(int idJogador) {
        return temCaminho(idJogador, getPosicao(idJogador));
    }

    public boolean temCaminho(int idJogador, Posicao origem) {
        Queue<Posicao> fila = new LinkedList<>();
        Set<Posicao> visitados = new HashSet<>();
        fila.add(origem);
        visitados.add(origem);
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            if (chegouAoDestino(idJogador, atual)) return true;
            for (Posicao vizinho : vizinhosAlcancaveis(atual)) {
                if (visitados.add(vizinho)) fila.add(vizinho);
            }
        }
        return false;
    }

    public boolean todosTemCaminho() {
        for (int id = 1; id <= NUM_JOGADORES; id++) {
            if (!temCaminho(id)) return false;
        }
        return true;
    }

    public int distanciaMinimaAteAlvo(int idJogador) {
        return distanciaMinimaAteAlvo(idJogador, getPosicao(idJogador));
    }

    public int distanciaMinimaAteAlvo(int idJogador, Posicao origem) {
        Queue<Posicao> fila = new LinkedList<>();
        Map<Posicao, Integer> dist = new HashMap<>();
        fila.add(origem);
        dist.put(origem, 0);
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            if (chegouAoDestino(idJogador, atual)) return dist.get(atual);
            for (Posicao vizinho : vizinhosAlcancaveis(atual)) {
                if (!dist.containsKey(vizinho)) {
                    dist.put(vizinho, dist.get(atual) + 1);
                    fila.add(vizinho);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    public boolean chegouAoDestino(int idJogador) {
        return chegouAoDestino(idJogador, getPosicao(idJogador));
    }

    private boolean chegouAoDestino(int idJogador, Posicao p) {
        return switch (idJogador) {
            case 1 -> p.linha() == 0;
            case 2 -> p.linha() == 8;
            case 3 -> p.coluna() == 0;
            case 4 -> p.coluna() == 8;
            default -> false;
        };
    }

    private List<Posicao> vizinhosAlcancaveis(Posicao p) {
        List<Posicao> vizinhos = new ArrayList<>();
        for (int[] d : DIRECOES) {
            Posicao q = new Posicao(p.linha() + d[0], p.coluna() + d[1]);
            if (!dentro(q.linha(), q.coluna())) continue;
            if (temParedeEntre(p, q)) continue;
            if (estaOcupada(q)) continue;
            vizinhos.add(q);
        }
        return vizinhos;
    }
}
