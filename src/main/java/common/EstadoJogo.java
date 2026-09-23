package common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum Status { AGUARDANDO, EM_ANDAMENTO, FINALIZADO }

    private final Posicao[] posicoes;
    private final List<Cerca> cercas;
    private final int[] donosCercas;
    private final int[] cercasRestantes;
    private final int jogadorDaVez;
    private final Status status;
    private final int vencedor;
    private final String[] nomes;
    private final boolean[] ativos;
    private final int registrados;

    public EstadoJogo(Posicao[] posicoes, List<Cerca> cercas, int[] donosCercas,
                      int[] cercasRestantes,
                      int jogadorDaVez, Status status, int vencedor, String[] nomes) {
        this(posicoes, cercas, donosCercas, cercasRestantes, jogadorDaVez, status, vencedor, nomes,
                todosAtivos(posicoes.length), posicoes.length);
    }

    public EstadoJogo(Posicao[] posicoes, List<Cerca> cercas, int[] donosCercas,
                      int[] cercasRestantes,
                      int jogadorDaVez, Status status, int vencedor, String[] nomes,
                      boolean[] ativos, int registrados) {
        this.posicoes = posicoes.clone();
        this.cercas = List.copyOf(cercas);
        this.donosCercas = donosCercas == null ? null : donosCercas.clone();
        this.cercasRestantes = cercasRestantes.clone();
        this.jogadorDaVez = jogadorDaVez;
        this.status = status;
        this.vencedor = vencedor;
        this.nomes = nomes.clone();
        this.ativos = ativos.clone();
        this.registrados = registrados;
    }

    private static boolean[] todosAtivos(int n) {
        boolean[] a = new boolean[n];
        Arrays.fill(a, true);
        return a;
    }

    public Posicao getPosicao(int idJogador) { return posicoes[idJogador - 1]; }
    public Posicao[] getPosicoes() { return posicoes.clone(); }
    public List<Cerca> getCercas() { return cercas; }
    /** Dono (id 1–4) de cada cerca em {@link #getCercas()}; pode ser null se não informado. */
    public int[] getDonosCercas() { return donosCercas == null ? null : donosCercas.clone(); }
    public int getDonoCerca(int indice) {
        return donosCercas == null || indice >= donosCercas.length ? 0 : donosCercas[indice];
    }
    public int[] getCercasRestantes() { return cercasRestantes.clone(); }
    public int getCercasRestantes(int idJogador) { return cercasRestantes[idJogador - 1]; }
    public int getJogadorDaVez() { return jogadorDaVez; }
    public Status getStatus() { return status; }
    public int getVencedor() { return vencedor; }
    public String getNome(int idJogador) { return nomes[idJogador - 1]; }
    public String[] getNomes() { return nomes.clone(); }
    /** false se o jogador se desconectou (a vez dele passa a ser pulada). */
    public boolean isAtivo(int idJogador) { return ativos[idJogador - 1]; }
    /** Quantos jogadores já entraram na sala (0–4). */
    public int getRegistrados() { return registrados; }
}
