package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;
import common.Sessao;
import engine.Partida;
import engine.Tabuleiro;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Árbitro independente: as regras do Quoridor reescritas do zero, direto da lista de cercas
 * (sem usar {@link Tabuleiro}). Partidas inteiras de bots são conferidas jogada a jogada:
 * a engine precisa concordar com o árbitro e toda jogada aceita precisa ser legal.
 */
class ArbitroIndependenteTest {
    private static final int N = 9;
    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private static boolean dentro(int r, int c) { return r >= 0 && r < N && c >= 0 && c < N; }

    private static boolean bloqueado(List<Cerca> cercas, int r, int c, int r2, int c2) {
        for (Cerca k : cercas) {
            int br = k.base().linha();
            int bc = k.base().coluna();
            if (k.orientacao() == Orientacao.HORIZONTAL) {
                if (c == c2 && (c == bc || c == bc + 1) && Math.min(r, r2) == br && Math.abs(r - r2) == 1) return true;
            } else if (r == r2 && (r == br || r == br + 1) && Math.min(c, c2) == bc && Math.abs(c - c2) == 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean ocupada(Posicao[] pos, int r, int c) {
        for (Posicao p : pos) if (p.linha() == r && p.coluna() == c) return true;
        return false;
    }

    static Set<Posicao> movimentos(Posicao[] pos, List<Cerca> cercas, int id) {
        Set<Posicao> res = new HashSet<>();
        Posicao a = pos[id - 1];
        for (int[] d : DIRS) {
            int vr = a.linha() + d[0];
            int vc = a.coluna() + d[1];
            if (!dentro(vr, vc) || bloqueado(cercas, a.linha(), a.coluna(), vr, vc)) continue;
            if (!ocupada(pos, vr, vc)) { res.add(new Posicao(vr, vc)); continue; }
            int jr = vr + d[0];
            int jc = vc + d[1];
            if (dentro(jr, jc) && !bloqueado(cercas, vr, vc, jr, jc) && !ocupada(pos, jr, jc)) {
                res.add(new Posicao(jr, jc));
                continue;
            }
            for (int s : new int[]{-1, 1}) {
                int lr = vr + s * d[1];
                int lc = vc + s * d[0];
                if (dentro(lr, lc) && !bloqueado(cercas, vr, vc, lr, lc) && !ocupada(pos, lr, lc)) {
                    res.add(new Posicao(lr, lc));
                }
            }
        }
        return res;
    }

    private static boolean meta(int id, int r, int c) {
        return switch (id) { case 1 -> r == 0; case 2 -> r == 8; case 3 -> c == 0; default -> c == 8; };
    }

    private static boolean caminho(List<Cerca> cercas, int id, Posicao o) {
        boolean[][] visto = new boolean[N][N];
        Deque<int[]> fila = new ArrayDeque<>();
        fila.add(new int[]{o.linha(), o.coluna()});
        visto[o.linha()][o.coluna()] = true;
        while (!fila.isEmpty()) {
            int[] p = fila.poll();
            if (meta(id, p[0], p[1])) return true;
            for (int[] d : DIRS) {
                int r = p[0] + d[0];
                int c = p[1] + d[1];
                if (dentro(r, c) && !visto[r][c] && !bloqueado(cercas, p[0], p[1], r, c)) {
                    visto[r][c] = true;
                    fila.add(new int[]{r, c});
                }
            }
        }
        return false;
    }

    static boolean cercaLegal(Posicao[] pos, List<Cerca> cercas, Cerca nova) {
        int r = nova.base().linha();
        int c = nova.base().coluna();
        if (r < 0 || r > 7 || c < 0 || c > 7) return false;
        for (Cerca k : cercas) {
            int kr = k.base().linha();
            int kc = k.base().coluna();
            if (kr == r && kc == c) return false; // mesmo centro: igual ou cruzada
            if (k.orientacao() == nova.orientacao()) {
                if (nova.orientacao() == Orientacao.HORIZONTAL && kr == r && Math.abs(kc - c) == 1) return false;
                if (nova.orientacao() == Orientacao.VERTICAL && kc == c && Math.abs(kr - r) == 1) return false;
            }
        }
        List<Cerca> todas = new ArrayList<>(cercas);
        todas.add(nova);
        for (int id = 1; id <= 4; id++) if (!caminho(todas, id, pos[id - 1])) return false;
        return true;
    }

    private record ServidorLocal(Partida partida) implements GameServer {
        @Override public Sessao registrar(common.ClientCallback c, String n) { throw new UnsupportedOperationException(); }
        @Override public void mover(Sessao s, Posicao d) throws JogadaInvalidaException { partida.mover(s.idJogador(), d); }
        @Override public void colocarCerca(Sessao s, Cerca c) throws JogadaInvalidaException { partida.colocarCerca(s.idJogador(), c); }
        @Override public EstadoJogo obterEstado() { return partida.gerarEstado(); }
    }

    @Test
    void partidasDeBotsSeguemAsRegrasOficiais() throws Exception {
        Random sementes = new Random(2026);
        for (int jogo = 0; jogo < 25; jogo++) {
            Partida partida = new Partida();
            partida.iniciar();
            GameServer servidor = new ServidorLocal(partida);
            BotJogador[] bots = new BotJogador[4];
            for (int i = 0; i < 4; i++) bots[i] = new BotJogador(new Sessao(i + 1, "t"), new Random(sementes.nextLong()));
            int jogadas = 0;
            while (partida.getStatus() == EstadoJogo.Status.EM_ANDAMENTO) {
                assertTrue(jogadas < 1000, "a partida precisa terminar");
                EstadoJogo antes = partida.gerarEstado();
                int vez = antes.getJogadorDaVez();
                Posicao[] pos = antes.getPosicoes();
                List<Cerca> cercas = antes.getCercas();
                Tabuleiro t = Tabuleiro.aPartirDe(pos, cercas);
                Set<Posicao> legais = movimentos(pos, cercas, vez);
                assertEquals(legais, new HashSet<>(t.movimentosValidos(vez)), "engine x árbitro: movimentos");
                if (jogadas % 10 == 0) {
                    for (int r = 0; r < 8; r++) {
                        for (int c = 0; c < 8; c++) {
                            for (Orientacao o : Orientacao.values()) {
                                Cerca k = new Cerca(new Posicao(r, c), o);
                                assertEquals(cercaLegal(pos, cercas, k), t.podeColocarCerca(k), "engine x árbitro: " + k);
                            }
                        }
                    }
                }

                bots[vez - 1].executarJogada(servidor, antes);

                EstadoJogo depois = partida.gerarEstado();
                int novas = depois.getCercas().size() - cercas.size();
                if (novas == 1) {
                    Cerca nova = depois.getCercas().get(depois.getCercas().size() - 1);
                    assertTrue(cercaLegal(pos, cercas, nova), "cerca ilegal aceita: " + nova);
                    assertEquals(pos[vez - 1], depois.getPosicao(vez), "não pode mover e pôr cerca na mesma jogada");
                    assertEquals(antes.getCercasRestantes(vez) - 1, depois.getCercasRestantes(vez));
                } else {
                    assertEquals(0, novas, "no máximo uma cerca por jogada");
                    assertTrue(legais.contains(depois.getPosicao(vez)), "movimento ilegal: " + depois.getPosicao(vez));
                }
                for (int j = 1; j <= 4; j++) {
                    if (j != vez) assertEquals(pos[j - 1], depois.getPosicao(j), "só o jogador da vez se move");
                    assertTrue(depois.getCercasRestantes(j) >= 0 && depois.getCercasRestantes(j) <= 5);
                }
                if (depois.getStatus() == EstadoJogo.Status.FINALIZADO) {
                    Posicao pv = depois.getPosicao(vez);
                    assertEquals(vez, depois.getVencedor());
                    assertTrue(meta(vez, pv.linha(), pv.coluna()), "vencedor precisa estar na meta");
                }
                jogadas++;
            }
            assertTrue(depoisDoFimNinguemJoga(partida));
        }
    }

    private static boolean depoisDoFimNinguemJoga(Partida p) {
        try {
            p.mover(p.getJogadorDaVez(), new Posicao(4, 4));
            return false;
        } catch (JogadaInvalidaException esperado) {
            return true;
        }
    }
}
