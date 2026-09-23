package engine;

import common.Cerca;
import common.Orientacao;
import common.Posicao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * Regras do tabuleiro 9x9 do Quoridor (4 jogadores).
 *
 * <p>Uma cerca ocupa 2 arestas e o ponto central entre elas. Com base (r,c):
 * <ul>
 *   <li>HORIZONTAL: bloqueia (r,c)/(r+1,c) e (r,c+1)/(r+1,c+1); centro no cruzamento (r,c).</li>
 *   <li>VERTICAL: bloqueia (r,c)/(r,c+1) e (r+1,c)/(r+1,c+1); centro no cruzamento (r,c).</li>
 * </ul>
 * Duas cercas não podem compartilhar aresta (sobreposição) nem centro (cruzamento).
 */
public class Tabuleiro {
    public static final int TAMANHO = 9;
    public static final int NUM_JOGADORES = 4;
    /** Bases válidas de cerca: 0..7 em linha e coluna (8x8 cruzamentos internos). */
    public static final int MAX_BASE = TAMANHO - 2;

    private static final int[][] DIRECOES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final boolean[][] paredesH; // [8][9]: aresta horizontal entre (r,c) e (r+1,c)
    private final boolean[][] paredesV; // [9][8]: aresta vertical entre (r,c) e (r,c+1)
    private final boolean[][] centros;  // [8][8]: cruzamento (r,c) já ocupado pelo meio de uma cerca
    private final Posicao[] posicoes;

    public Tabuleiro() {
        paredesH = new boolean[TAMANHO - 1][TAMANHO];
        paredesV = new boolean[TAMANHO][TAMANHO - 1];
        centros = new boolean[TAMANHO - 1][TAMANHO - 1];
        posicoes = new Posicao[NUM_JOGADORES];
        posicoes[0] = new Posicao(8, 4); // J1 embaixo -> meta linha 0
        posicoes[1] = new Posicao(0, 4); // J2 topo   -> meta linha 8
        posicoes[2] = new Posicao(4, 8); // J3 direita-> meta coluna 0
        posicoes[3] = new Posicao(4, 0); // J4 esquerda-> meta coluna 8
    }

    private Tabuleiro(Tabuleiro outro) {
        paredesH = copia(outro.paredesH);
        paredesV = copia(outro.paredesV);
        centros = copia(outro.centros);
        posicoes = outro.posicoes.clone();
    }

    private static boolean[][] copia(boolean[][] m) {
        boolean[][] r = new boolean[m.length][];
        for (int i = 0; i < m.length; i++) r[i] = m[i].clone();
        return r;
    }

    public Tabuleiro copiar() { return new Tabuleiro(this); }

    /** Reconstrói um tabuleiro a partir de um estado recebido (usado por clientes e bots). */
    public static Tabuleiro aPartirDe(Posicao[] posicoes, List<Cerca> cercas) {
        Tabuleiro t = new Tabuleiro();
        for (int i = 0; i < NUM_JOGADORES; i++) t.posicoes[i] = posicoes[i];
        for (Cerca c : cercas) {
            if (baseValida(c)) t.aplicar(c);
        }
        return t;
    }

    public boolean dentro(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    private boolean dentro(Posicao p) { return dentro(p.linha(), p.coluna()); }

    private static boolean baseValida(Cerca c) {
        int r = c.base().linha();
        int col = c.base().coluna();
        return r >= 0 && r <= MAX_BASE && col >= 0 && col <= MAX_BASE;
    }

    public Posicao getPosicao(int idJogador) { return posicoes[idJogador - 1]; }

    public boolean estaOcupada(Posicao p) {
        for (Posicao q : posicoes) {
            if (q.equals(p)) return true;
        }
        return false;
    }

    /** Há cerca entre duas casas ortogonalmente vizinhas? */
    public boolean temParedeEntre(Posicao a, Posicao b) {
        int dr = b.linha() - a.linha();
        int dc = b.coluna() - a.coluna();
        if (dr == -1 && dc == 0) return paredesH[b.linha()][a.coluna()];
        if (dr == 1 && dc == 0) return paredesH[a.linha()][a.coluna()];
        if (dr == 0 && dc == -1) return paredesV[a.linha()][b.coluna()];
        if (dr == 0 && dc == 1) return paredesV[a.linha()][a.coluna()];
        return false;
    }

    /** Casa vizinha acessível: dentro do tabuleiro e sem cerca no caminho. */
    private boolean passagemLivre(Posicao de, Posicao para) {
        return dentro(para) && !temParedeEntre(de, para);
    }

    /**
     * Movimentos legais do peão, conforme as regras oficiais:
     * passo ortogonal; pulo reto sobre um peão adjacente; se atrás dele houver cerca,
     * borda ou outro peão (não se pula 2 peões), pulo diagonal para os lados livres.
     */
    public List<Posicao> movimentosValidos(int idJogador) {
        Posicao atual = getPosicao(idJogador);
        List<Posicao> resultado = new ArrayList<>();
        for (int[] d : DIRECOES) {
            Posicao vizinho = new Posicao(atual.linha() + d[0], atual.coluna() + d[1]);
            if (!passagemLivre(atual, vizinho)) continue;
            if (!estaOcupada(vizinho)) {
                adicionarSemRepetir(resultado, vizinho);
                continue;
            }
            Posicao depois = new Posicao(vizinho.linha() + d[0], vizinho.coluna() + d[1]);
            if (passagemLivre(vizinho, depois) && !estaOcupada(depois)) {
                adicionarSemRepetir(resultado, depois);
                continue;
            }
            // Pulo diagonal: perpendicular à direção do pulo, a partir do peão pulado.
            Posicao lado1 = new Posicao(vizinho.linha() + d[1], vizinho.coluna() + d[0]);
            Posicao lado2 = new Posicao(vizinho.linha() - d[1], vizinho.coluna() - d[0]);
            for (Posicao lado : new Posicao[]{lado1, lado2}) {
                if (passagemLivre(vizinho, lado) && !estaOcupada(lado)) {
                    adicionarSemRepetir(resultado, lado);
                }
            }
        }
        return resultado;
    }

    private static void adicionarSemRepetir(List<Posicao> lista, Posicao p) {
        if (!lista.contains(p)) lista.add(p);
    }

    /**
     * Cerca legal: base dentro de 0..7, não sobrepõe nem cruza outra cerca e não deixa
     * nenhum jogador sem caminho até sua meta (peões não contam como bloqueio de caminho).
     */
    public boolean podeColocarCerca(Cerca cerca) {
        if (!baseValida(cerca)) return false;
        int r = cerca.base().linha();
        int c = cerca.base().coluna();
        if (centros[r][c]) return false; // cruzaria (ou coincidiria com) outra cerca no mesmo centro
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            if (paredesH[r][c] || paredesH[r][c + 1]) return false;
        } else {
            if (paredesV[r][c] || paredesV[r + 1][c]) return false;
        }
        Tabuleiro sim = copiar();
        sim.aplicar(cerca);
        return sim.todosTemCaminho();
    }

    public void moverPeao(int idJogador, Posicao destino) {
        posicoes[idJogador - 1] = destino;
    }

    /** Coloca a cerca sem validar regras (a validação é feita por {@link #podeColocarCerca}). */
    public void colocarCerca(Cerca cerca) {
        if (!baseValida(cerca)) throw new IllegalArgumentException("Cerca fora do tabuleiro: " + cerca);
        aplicar(cerca);
    }

    private void aplicar(Cerca cerca) {
        int r = cerca.base().linha();
        int c = cerca.base().coluna();
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            paredesH[r][c] = true;
            paredesH[r][c + 1] = true;
        } else {
            paredesV[r][c] = true;
            paredesV[r + 1][c] = true;
        }
        centros[r][c] = true;
    }

    public boolean temCaminho(int idJogador) {
        return distanciaMinimaAteAlvo(idJogador) != Integer.MAX_VALUE;
    }

    public boolean temCaminho(int idJogador, Posicao origem) {
        return distanciaMinimaAteAlvo(idJogador, origem) != Integer.MAX_VALUE;
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

    /**
     * Menor número de passos até a meta considerando só as cercas (BFS). Peões são ignorados:
     * pela regra oficial eles não bloqueiam caminho, pois se movem e podem ser pulados.
     * Devolve {@link Integer#MAX_VALUE} se não houver caminho.
     */
    public int distanciaMinimaAteAlvo(int idJogador, Posicao origem) {
        int[][] dist = new int[TAMANHO][TAMANHO];
        for (int[] linha : dist) Arrays.fill(linha, -1);
        Queue<Posicao> fila = new ArrayDeque<>();
        fila.add(origem);
        dist[origem.linha()][origem.coluna()] = 0;
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            int d = dist[atual.linha()][atual.coluna()];
            if (chegouAoDestino(idJogador, atual)) return d;
            for (int[] dir : DIRECOES) {
                Posicao q = new Posicao(atual.linha() + dir[0], atual.coluna() + dir[1]);
                if (!passagemLivre(atual, q) || dist[q.linha()][q.coluna()] >= 0) continue;
                dist[q.linha()][q.coluna()] = d + 1;
                fila.add(q);
            }
        }
        return Integer.MAX_VALUE;
    }

    public boolean chegouAoDestino(int idJogador) {
        return chegouAoDestino(idJogador, getPosicao(idJogador));
    }

    private static boolean chegouAoDestino(int idJogador, Posicao p) {
        return switch (idJogador) {
            case 1 -> p.linha() == 0;
            case 2 -> p.linha() == TAMANHO - 1;
            case 3 -> p.coluna() == 0;
            case 4 -> p.coluna() == TAMANHO - 1;
            default -> false;
        };
    }

    /** Todas as cercas que poderiam ser colocadas agora (usado pelos bots). */
    public List<Cerca> cercasPossiveis() {
        List<Cerca> possiveis = new ArrayList<>();
        for (int r = 0; r <= MAX_BASE; r++) {
            for (int c = 0; c <= MAX_BASE; c++) {
                for (Orientacao o : Orientacao.values()) {
                    Cerca cerca = new Cerca(new Posicao(r, c), o);
                    if (podeColocarCerca(cerca)) possiveis.add(cerca);
                }
            }
        }
        return possiveis;
    }

    /** Existe ao menos uma cerca legal? (detecta jogador sem nenhuma jogada possível) */
    public boolean existeCercaPossivel() {
        for (int r = 0; r <= MAX_BASE; r++) {
            for (int c = 0; c <= MAX_BASE; c++) {
                for (Orientacao o : Orientacao.values()) {
                    if (podeColocarCerca(new Cerca(new Posicao(r, c), o))) return true;
                }
            }
        }
        return false;
    }
}
