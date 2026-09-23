package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import common.Sessao;
import engine.Tabuleiro;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jogador automático. Decide a jogada localmente (com o mesmo motor de regras do servidor)
 * e a envia por RMI; o servidor continua sendo quem valida tudo.
 *
 * <p>Estratégia: anda pelo menor caminho; se um adversário estiver mais perto da meta,
 * coloca a cerca que mais atrasa esse adversário sem atrasar a si mesmo.
 */
public class BotJogador {
    private final Sessao sessao;
    private final int id;
    private final Random aleatorio;

    public BotJogador(Sessao sessao) {
        this(sessao, new Random());
    }

    BotJogador(Sessao sessao, Random aleatorio) {
        this.sessao = sessao;
        this.id = sessao.idJogador();
        this.aleatorio = aleatorio;
    }

    public void executarJogada(GameServer server, EstadoJogo estado) throws Exception {
        Tabuleiro tabuleiro = Tabuleiro.aPartirDe(estado.getPosicoes(), estado.getCercas());

        Cerca cerca = escolherCerca(tabuleiro, estado);
        if (cerca != null) {
            try {
                server.colocarCerca(sessao, cerca);
                return;
            } catch (JogadaInvalidaException ignorada) {
                // Estado mudou entre a decisão e o envio: cai para o movimento.
            }
        }

        Posicao destino = escolherMovimento(tabuleiro);
        if (destino != null) {
            server.mover(sessao, destino);
            return;
        }
        // Sem movimento (cercado por peões): qualquer cerca legal serve para não perder a vez.
        if (estado.getCercasRestantes(id) > 0) {
            List<Cerca> possiveis = tabuleiro.cercasPossiveis();
            if (!possiveis.isEmpty()) {
                server.colocarCerca(sessao, possiveis.get(aleatorio.nextInt(possiveis.size())));
                return;
            }
        }
        throw new IllegalStateException("Bot " + id + " sem jogadas possíveis.");
    }

    /** Movimento que mais reduz a distância até a meta (empates sorteados, evitando ciclos). */
    Posicao escolherMovimento(Tabuleiro tabuleiro) {
        List<Posicao> melhores = new ArrayList<>();
        int melhor = Integer.MAX_VALUE;
        for (Posicao p : tabuleiro.movimentosValidos(id)) {
            int dist = tabuleiro.distanciaMinimaAteAlvo(id, p);
            if (dist < melhor) {
                melhor = dist;
                melhores.clear();
            }
            if (dist == melhor) melhores.add(p);
        }
        return melhores.isEmpty() ? null : melhores.get(aleatorio.nextInt(melhores.size()));
    }

    /**
     * Cerca contra o adversário mais adiantado, apenas se ele estiver à frente do bot (ou
     * a 2 passos de vencer) e se a cerca render ganho líquido. Devolve null se não valer a pena.
     */
    Cerca escolherCerca(Tabuleiro tabuleiro, EstadoJogo estado) {
        if (estado.getCercasRestantes(id) <= 0) return null;
        int minhaDist = tabuleiro.distanciaMinimaAteAlvo(id);
        int alvo = -1;
        int distAlvo = Integer.MAX_VALUE;
        for (int outro = 1; outro <= Tabuleiro.NUM_JOGADORES; outro++) {
            if (outro == id || !estado.isAtivo(outro)) continue;
            int d = tabuleiro.distanciaMinimaAteAlvo(outro);
            if (d < distAlvo) {
                distAlvo = d;
                alvo = outro;
            }
        }
        if (alvo < 0 || (distAlvo >= minhaDist && distAlvo > 2)) return null;

        // Economia de cercas: longe do fim só vale uma cerca que atrase bastante (>= 2 passos);
        // perto do fim (adversário a <= 3 passos) qualquer atraso já compensa.
        int ganhoMinimo = distAlvo <= 3 ? 1 : 2;
        List<Cerca> melhores = new ArrayList<>();
        int melhorGanho = 0;
        for (Cerca c : tabuleiro.cercasPossiveis()) {
            Tabuleiro sim = tabuleiro.copiar();
            sim.colocarCerca(c);
            int ganho = (sim.distanciaMinimaAteAlvo(alvo) - distAlvo)
                    - (sim.distanciaMinimaAteAlvo(id) - minhaDist);
            if (ganho > melhorGanho) {
                melhorGanho = ganho;
                melhores.clear();
            }
            if (ganho == melhorGanho && ganho > 0) melhores.add(c);
        }
        if (melhorGanho < ganhoMinimo || melhores.isEmpty()) return null;
        return melhores.get(aleatorio.nextInt(melhores.size()));
    }
}
