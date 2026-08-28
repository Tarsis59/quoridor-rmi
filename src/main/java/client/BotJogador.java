package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;

import java.util.ArrayList;
import java.util.List;

public class BotJogador {
    private final int id;
    private int turnos = 0;

    public BotJogador(int id) {
        this.id = id;
    }

    public void executarJogada(GameServer server, EstadoJogo estado) throws Exception {
        Tabuleiro tabuleiro = Tabuleiro.aPartirDe(estado.getPosicoes(), estado.getCercas());
        turnos++;

        if (turnos % 4 == 0 && estado.getCercasRestantes(id) > 0
                && tentarColocarCerca(server, tabuleiro)) {
            return;
        }

        List<Posicao> validos = tabuleiro.movimentosValidos(id);
        Posicao atual = tabuleiro.getPosicao(id);
        Posicao melhor = null;
        int melhorDistancia = tabuleiro.distanciaMinimaAteAlvo(id, atual);
        for (Posicao p : validos) {
            int dist = tabuleiro.distanciaMinimaAteAlvo(id, p);
            if (dist < melhorDistancia) {
                melhorDistancia = dist;
                melhor = p;
            }
        }
        if (melhor != null) {
            server.mover(id, melhor);
            return;
        }
        if (tentarColocarCerca(server, tabuleiro)) return;
        if (!validos.isEmpty()) {
            server.mover(id, validos.get(0));
            return;
        }
        throw new IllegalStateException("Bot " + id + " sem jogadas possíveis.");
    }

    private boolean tentarColocarCerca(GameServer server, Tabuleiro tabuleiro) throws Exception {
        int alvo = jogadorMaisProximoDoAlvo(tabuleiro);
        List<Posicao> bases = gerarBasesOrdenadas(tabuleiro.getPosicao(alvo));
        int limite = Math.min(24, bases.size());
        for (int i = 0; i < limite; i++) {
            Posicao base = bases.get(i);
            for (Orientacao o : new Orientacao[]{Orientacao.HORIZONTAL, Orientacao.VERTICAL}) {
                Cerca cerca = new Cerca(base, o);
                try {
                    server.colocarCerca(id, cerca);
                    return true;
                } catch (JogadaInvalidaException ignorada) {
                    // tenta a próxima base/orientação
                }
            }
        }
        return false;
    }

    private int jogadorMaisProximoDoAlvo(Tabuleiro tabuleiro) {
        int alvo = -1;
        int melhorDistancia = Integer.MAX_VALUE;
        for (int i = 1; i <= 4; i++) {
            if (i == id) continue;
            int dist = tabuleiro.distanciaMinimaAteAlvo(i);
            if (dist < melhorDistancia) {
                melhorDistancia = dist;
                alvo = i;
            }
        }
        return alvo;
    }

    private List<Posicao> gerarBasesOrdenadas(Posicao foco) {
        List<Posicao> bases = new ArrayList<>();
        for (int r = 0; r <= 7; r++) {
            for (int c = 0; c <= 7; c++) {
                bases.add(new Posicao(r, c));
            }
        }
        bases.sort((a, b) -> Integer.compare(
                distManhattan(a, foco), distManhattan(b, foco)));
        return bases;
    }

    private int distManhattan(Posicao a, Posicao b) {
        return Math.abs(a.linha() - b.linha()) + Math.abs(a.coluna() - b.coluna());
    }
}
