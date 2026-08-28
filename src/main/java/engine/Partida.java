package engine;

import common.Cerca;
import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;

import java.util.ArrayList;
import java.util.List;

public class Partida {
    public static final int NUM_JOGADORES = 4;
    public static final int CERCAS_INICIAIS = 10;

    private final Tabuleiro tabuleiro = new Tabuleiro();
    private final String[] nomes = new String[NUM_JOGADORES];
    private final int[] cercasRestantes = new int[NUM_JOGADORES];
    private final List<Cerca> cercasColocadas = new ArrayList<>();
    private final List<Integer> donosCercas = new ArrayList<>();
    private int jogadorDaVez = 1;
    private EstadoJogo.Status status = EstadoJogo.Status.AGUARDANDO;
    private int vencedor = 0;

    public Partida() {
        for (int i = 0; i < NUM_JOGADORES; i++) {
            nomes[i] = "Jogador " + (i + 1);
            cercasRestantes[i] = CERCAS_INICIAIS;
        }
    }

    public synchronized void setNome(int idJogador, String nome) {
        nomes[idJogador - 1] = nome;
    }

    public synchronized void iniciar() {
        status = EstadoJogo.Status.EM_ANDAMENTO;
    }

    public synchronized void mover(int idJogador, Posicao destino) throws JogadaInvalidaException {
        if (status != EstadoJogo.Status.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("O jogo ainda não começou ou já terminou.");
        }
        if (idJogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Não é a vez do jogador " + idJogador + ".");
        }
        boolean valido = tabuleiro.movimentosValidos(idJogador).stream().anyMatch(destino::equals);
        if (!valido) {
            throw new JogadaInvalidaException("Movimento inválido para " + destino + ".");
        }
        tabuleiro.moverPeao(idJogador, destino);
        if (tabuleiro.chegouAoDestino(idJogador)) {
            vencedor = idJogador;
            status = EstadoJogo.Status.FINALIZADO;
        } else {
            avancarTurno();
        }
    }

    public synchronized void colocarCerca(int idJogador, Cerca cerca) throws JogadaInvalidaException {
        if (status != EstadoJogo.Status.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("O jogo ainda não começou ou já terminou.");
        }
        if (idJogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Não é a vez do jogador " + idJogador + ".");
        }
        if (cercasRestantes[idJogador - 1] <= 0) {
            throw new JogadaInvalidaException("Jogador " + idJogador + " não possui mais cercas.");
        }
        if (!tabuleiro.podeColocarCerca(cerca)) {
            throw new JogadaInvalidaException("Cerca inválida: sobreposição, cruzamento ou bloqueio total de caminho.");
        }
        tabuleiro.colocarCerca(cerca);
        cercasColocadas.add(cerca);
        donosCercas.add(idJogador);
        cercasRestantes[idJogador - 1]--;
        avancarTurno();
    }

    public synchronized void avancarTurno() {
        jogadorDaVez = jogadorDaVez % NUM_JOGADORES + 1;
    }

    public synchronized EstadoJogo gerarEstado() {
        Posicao[] pos = new Posicao[NUM_JOGADORES];
        for (int i = 0; i < NUM_JOGADORES; i++) pos[i] = tabuleiro.getPosicao(i + 1);
        return new EstadoJogo(pos, new ArrayList<>(cercasColocadas), donosCercas.stream().mapToInt(Integer::intValue).toArray(),
                cercasRestantes.clone(), jogadorDaVez, status, vencedor, nomes.clone());
    }

    public synchronized EstadoJogo.Status getStatus() { return status; }
    public synchronized int getJogadorDaVez() { return jogadorDaVez; }
    public synchronized int getVencedor() { return vencedor; }
}
