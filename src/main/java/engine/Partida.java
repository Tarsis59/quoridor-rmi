package engine;

import common.Cerca;
import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;

import java.util.ArrayList;
import java.util.List;

/** Estado autoritativo de uma partida de Quoridor com 4 jogadores (vive só no servidor). */
public class Partida {
    public static final int NUM_JOGADORES = 4;
    /** Regra oficial: com 4 jogadores as 20 cercas são divididas — 5 para cada. */
    public static final int CERCAS_INICIAIS = 5;
    public static final int TAMANHO_MAX_NOME = 20;

    private final Tabuleiro tabuleiro = new Tabuleiro();
    private final String[] nomes = new String[NUM_JOGADORES];
    private final int[] cercasRestantes = new int[NUM_JOGADORES];
    private final boolean[] ativos = new boolean[NUM_JOGADORES];
    private final List<Cerca> cercasColocadas = new ArrayList<>();
    private final List<Integer> donosCercas = new ArrayList<>();
    private int jogadorDaVez = 1;
    private int registrados = 0;
    private EstadoJogo.Status status = EstadoJogo.Status.AGUARDANDO;
    private int vencedor = 0;

    public Partida() {
        for (int i = 0; i < NUM_JOGADORES; i++) {
            nomes[i] = "Jogador " + (i + 1);
            cercasRestantes[i] = CERCAS_INICIAIS;
            ativos[i] = true;
        }
    }

    public synchronized void setNome(int idJogador, String nome) {
        validarId(idJogador);
        nomes[idJogador - 1] = sanitizarNome(nome, idJogador);
    }

    /** Remove caracteres de controle, limita o tamanho e usa um nome padrão se vier vazio. */
    public static String sanitizarNome(String nome, int idJogador) {
        String limpo = nome == null ? "" : nome.replaceAll("\\p{Cntrl}", "").strip();
        if (limpo.length() > TAMANHO_MAX_NOME) limpo = limpo.substring(0, TAMANHO_MAX_NOME);
        return limpo.isEmpty() ? "Jogador " + idJogador : limpo;
    }

    /** Quantos jogadores já entraram na sala (exibido enquanto a partida aguarda). */
    public synchronized void setRegistrados(int n) { registrados = n; }

    public synchronized void iniciar() {
        if (status != EstadoJogo.Status.AGUARDANDO) return;
        status = EstadoJogo.Status.EM_ANDAMENTO;
        if (verificarVitoriaPorAbandono()) return;
        if (!podeJogar(jogadorDaVez)) avancarTurno();
    }

    public synchronized void mover(int idJogador, Posicao destino) throws JogadaInvalidaException {
        validarJogada(idJogador);
        if (destino == null) throw new JogadaInvalidaException("Destino não informado.");
        if (!tabuleiro.movimentosValidos(idJogador).contains(destino)) {
            throw new JogadaInvalidaException("Movimento inválido para (" + destino.linha() + ","
                    + destino.coluna() + ").");
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
        validarJogada(idJogador);
        if (cerca == null) throw new JogadaInvalidaException("Cerca não informada.");
        if (cercasRestantes[idJogador - 1] <= 0) {
            throw new JogadaInvalidaException("Você não possui mais cercas (cada jogador tem "
                    + CERCAS_INICIAIS + ").");
        }
        if (!tabuleiro.podeColocarCerca(cerca)) {
            throw new JogadaInvalidaException(
                    "Cerca inválida: fora do tabuleiro, sobreposição, cruzamento ou bloqueio total de caminho.");
        }
        tabuleiro.colocarCerca(cerca);
        cercasColocadas.add(cerca);
        donosCercas.add(idJogador);
        cercasRestantes[idJogador - 1]--;
        avancarTurno();
    }

    private void validarJogada(int idJogador) throws JogadaInvalidaException {
        if (status != EstadoJogo.Status.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("O jogo ainda não começou ou já terminou.");
        }
        if (idJogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Não é a sua vez: agora joga o Jogador " + jogadorDaVez + ".");
        }
    }

    private static void validarId(int idJogador) {
        if (idJogador < 1 || idJogador > NUM_JOGADORES) {
            throw new IllegalArgumentException("Id de jogador inválido: " + idJogador);
        }
    }

    /**
     * Passa a vez para o próximo jogador que pode jogar. São pulados jogadores desconectados
     * e jogadores "travados" (sem movimento legal e sem cerca possível — raro, mas pode
     * acontecer com 4 peões e várias cercas). Se ninguém puder jogar, a vez não muda.
     */
    public synchronized void avancarTurno() {
        int candidato = jogadorDaVez;
        for (int i = 0; i < NUM_JOGADORES; i++) {
            candidato = candidato % NUM_JOGADORES + 1;
            if (podeJogar(candidato)) {
                jogadorDaVez = candidato;
                return;
            }
        }
    }

    private boolean podeJogar(int id) {
        if (!ativos[id - 1]) return false;
        if (!tabuleiro.movimentosValidos(id).isEmpty()) return true;
        return cercasRestantes[id - 1] > 0 && tabuleiro.existeCercaPossivel();
    }

    /**
     * Jogador saiu (queda de conexão ou janela fechada). A vez dele passa a ser pulada;
     * se sobrar só um jogador conectado com a partida em andamento, ele vence por W.O.
     */
    public synchronized void removerJogador(int idJogador) {
        validarId(idJogador);
        if (!ativos[idJogador - 1]) return;
        ativos[idJogador - 1] = false;
        if (status != EstadoJogo.Status.EM_ANDAMENTO) return;
        if (verificarVitoriaPorAbandono()) return;
        if (jogadorDaVez == idJogador) avancarTurno();
    }

    private boolean verificarVitoriaPorAbandono() {
        int restantes = 0;
        int ultimo = 0;
        for (int i = 0; i < NUM_JOGADORES; i++) {
            if (ativos[i]) {
                restantes++;
                ultimo = i + 1;
            }
        }
        if (restantes <= 1) {
            vencedor = ultimo; // 0 se todos saíram
            status = EstadoJogo.Status.FINALIZADO;
            return true;
        }
        return false;
    }

    public synchronized EstadoJogo gerarEstado() {
        Posicao[] pos = new Posicao[NUM_JOGADORES];
        for (int i = 0; i < NUM_JOGADORES; i++) pos[i] = tabuleiro.getPosicao(i + 1);
        return new EstadoJogo(pos, new ArrayList<>(cercasColocadas),
                donosCercas.stream().mapToInt(Integer::intValue).toArray(),
                cercasRestantes.clone(), jogadorDaVez, status, vencedor, nomes.clone(),
                ativos.clone(), registrados);
    }

    public synchronized EstadoJogo.Status getStatus() { return status; }
    public synchronized int getJogadorDaVez() { return jogadorDaVez; }
    public synchronized int getVencedor() { return vencedor; }
    public synchronized boolean isAtivo(int idJogador) { return ativos[idJogador - 1]; }
}
