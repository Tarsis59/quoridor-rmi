package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;

import java.util.ArrayList;
import java.util.List;

/**
 * Descreve em texto o que mudou entre dois estados recebidos do servidor
 * (movimento, cerca, saída, início e fim). Função pura, sem Swing — fácil de testar.
 */
public final class Historico {

    /** Um acontecimento: quem fez (0 = o sistema) e a descrição. */
    public record Evento(int idJogador, String texto) { }

    private Historico() {}

    public static List<Evento> diferencas(EstadoJogo antes, EstadoJogo depois) {
        List<Evento> eventos = new ArrayList<>();
        if (depois == null) return eventos;
        if (antes == null || antes.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            if (depois.getStatus() == EstadoJogo.Status.AGUARDANDO) {
                eventos.add(new Evento(0, "Sala de espera: " + depois.getRegistrados() + "/4 jogadores"));
                return eventos;
            }
            eventos.add(new Evento(0, "A partida começou!"));
            if (antes == null) {
                adicionarFim(eventos, null, depois);
                return eventos;
            }
        }
        for (int id = 1; id <= 4; id++) {
            Posicao de = antes.getPosicao(id);
            Posicao para = depois.getPosicao(id);
            if (!de.equals(para)) {
                eventos.add(new Evento(id, nome(depois, id) + " moveu " + casa(de) + " → " + casa(para)));
            }
        }
        List<Cerca> cercasAntes = antes.getCercas();
        List<Cerca> cercasDepois = depois.getCercas();
        for (int i = cercasAntes.size(); i < cercasDepois.size(); i++) {
            Cerca c = cercasDepois.get(i);
            int dono = depois.getDonoCerca(i);
            String quem = dono >= 1 && dono <= 4 ? nome(depois, dono) : "Alguém";
            eventos.add(new Evento(dono, quem + " pôs cerca " + (c.orientacao() == Orientacao.HORIZONTAL ? "H" : "V")
                    + " em " + casa(c.base())));
        }
        for (int id = 1; id <= 4; id++) {
            if (antes.isAtivo(id) && !depois.isAtivo(id)) {
                eventos.add(new Evento(id, nome(depois, id) + " saiu da partida"));
            }
        }
        adicionarFim(eventos, antes, depois);
        return eventos;
    }

    private static void adicionarFim(List<Evento> eventos, EstadoJogo antes, EstadoJogo depois) {
        boolean acabouAgora = depois.getStatus() == EstadoJogo.Status.FINALIZADO
                && (antes == null || antes.getStatus() != EstadoJogo.Status.FINALIZADO);
        if (!acabouAgora) return;
        int v = depois.getVencedor();
        eventos.add(new Evento(v, v == 0 ? "Partida encerrada sem vencedor" : nome(depois, v) + " venceu a partida!"));
    }

    private static String nome(EstadoJogo e, int id) {
        return e.getNome(id);
    }

    private static String casa(Posicao p) {
        return "(" + p.linha() + "," + p.coluna() + ")";
    }
}
