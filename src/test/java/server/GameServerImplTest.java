package server;

import common.Cerca;
import common.ClientCallback;
import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;
import common.Sessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa o servidor chamando-o diretamente (sem registry), com callbacks locais simulados. */
class GameServerImplTest {

    /** Callback falso: guarda o que recebeu e pode "cair" sob comando. */
    static class CallbackFalso implements ClientCallback {
        final List<EstadoJogo> estados = new ArrayList<>();
        int inicios = 0;
        final List<Integer> fins = new ArrayList<>();
        volatile boolean caiu = false;

        private void verificar() throws RemoteException {
            if (caiu) throw new RemoteException("cliente caiu");
        }
        @Override public void aoIniciarJogo(EstadoJogo e) throws RemoteException { verificar(); inicios++; estados.add(e); }
        @Override public void aoAtualizarEstado(EstadoJogo e) throws RemoteException { verificar(); estados.add(e); }
        @Override public void aoFinalizarJogo(int v) throws RemoteException { verificar(); fins.add(v); }
        @Override public void ping() throws RemoteException { verificar(); }
        EstadoJogo ultimo() { return estados.get(estados.size() - 1); }
    }

    private GameServerImpl servidor;
    private final CallbackFalso[] cbs = new CallbackFalso[4];
    private final Sessao[] sessoes = new Sessao[4];

    @BeforeEach
    void setUp() throws Exception {
        servidor = new GameServerImpl(false);
    }

    @AfterEach
    void tearDown() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(servidor, true);
    }

    private void registrarQuatro() throws RemoteException {
        for (int i = 0; i < 4; i++) {
            cbs[i] = new CallbackFalso();
            sessoes[i] = servidor.registrar(cbs[i], "J" + (i + 1));
        }
    }

    @Test
    void registraQuatroJogadoresComIdsETokensDistintos() throws Exception {
        registrarQuatro();
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, sessoes[i].idJogador());
            assertEquals(1, cbs[i].inicios, "cada cliente recebe o início exatamente uma vez");
        }
        assertNotEquals(sessoes[0].token(), sessoes[1].token());
        assertEquals(EstadoJogo.Status.EM_ANDAMENTO, servidor.obterEstado().getStatus());
        assertFalse(sessoes[0].toString().contains(sessoes[0].token()), "token não pode vazar em logs");
    }

    @Test
    void quintoJogadorERecusado() throws Exception {
        registrarQuatro();
        assertThrows(RemoteException.class, () -> servidor.registrar(new CallbackFalso(), "Intruso"));
    }

    @Test
    void jogadoresRecebemEstadoDeEsperaAntesDoInicio() throws Exception {
        CallbackFalso cb = new CallbackFalso();
        servidor.registrar(cb, "Ana");
        servidor.registrar(new CallbackFalso(), "Bob");
        assertEquals(EstadoJogo.Status.AGUARDANDO, cb.ultimo().getStatus());
        assertEquals(2, cb.ultimo().getRegistrados());
        assertThrows(JogadaInvalidaException.class, () -> servidor.mover(
                new Sessao(1, "x"), new Posicao(7, 4)));
    }

    @Test
    void naoEPossivelJogarEmNomeDeOutroJogador() throws Exception {
        registrarQuatro();
        Sessao falsa = new Sessao(1, sessoes[1].token()); // id do J1 com token do J2
        assertThrows(JogadaInvalidaException.class, () -> servidor.mover(falsa, new Posicao(7, 4)));
        assertThrows(JogadaInvalidaException.class, () -> servidor.mover(new Sessao(1, null), new Posicao(7, 4)));
        assertThrows(JogadaInvalidaException.class, () -> servidor.mover(null, new Posicao(7, 4)));
        assertEquals(new Posicao(8, 4), servidor.obterEstado().getPosicao(1), "nada mudou");
    }

    @Test
    void jogadaValidaEAvisadaATodos() throws Exception {
        registrarQuatro();
        servidor.mover(sessoes[0], new Posicao(7, 4));
        for (CallbackFalso cb : cbs) {
            assertEquals(new Posicao(7, 4), cb.ultimo().getPosicao(1));
            assertEquals(2, cb.ultimo().getJogadorDaVez());
        }
        servidor.colocarCerca(sessoes[1], new Cerca(new Posicao(6, 3), Orientacao.HORIZONTAL));
        assertEquals(4, cbs[0].ultimo().getCercasRestantes(2));
        assertEquals(2, cbs[0].ultimo().getDonoCerca(0));
    }

    @Test
    void clienteQueCaiTemAVezPuladaEOsOutrosSaoAvisados() throws Exception {
        registrarQuatro();
        cbs[1].caiu = true; // J2 fecha a janela
        servidor.mover(sessoes[0], new Posicao(7, 4));
        EstadoJogo e = cbs[0].ultimo();
        assertFalse(e.isAtivo(2));
        assertEquals(3, e.getJogadorDaVez(), "vez do J2 foi pulada");
    }

    @Test
    void pingDetectaQuedaDoJogadorDaVez() throws Exception {
        registrarQuatro();
        cbs[0].caiu = true; // J1 caiu na própria vez: sem ping o jogo ficaria parado para sempre
        servidor.verificarConexoes();
        assertEquals(2, cbs[1].ultimo().getJogadorDaVez());
        assertFalse(cbs[1].ultimo().isAtivo(1));
    }

    @Test
    void ultimoConectadoVencePorWO() throws Exception {
        registrarQuatro();
        cbs[0].caiu = true;
        cbs[1].caiu = true;
        cbs[3].caiu = true;
        servidor.verificarConexoes();
        EstadoJogo e = servidor.obterEstado();
        assertEquals(EstadoJogo.Status.FINALIZADO, e.getStatus());
        assertEquals(3, e.getVencedor());
        assertEquals(List.of(3), cbs[2].fins, "o sobrevivente é avisado do fim uma única vez");
        assertTrue(cbs[2].ultimo().getStatus() == EstadoJogo.Status.FINALIZADO);
    }

    @Test
    void jogadorDesconectadoNaoPodeMaisJogar() throws Exception {
        registrarQuatro();
        cbs[0].caiu = true;
        servidor.verificarConexoes();
        cbs[0].caiu = false; // "voltou", mas a vaga já foi abandonada
        assertThrows(JogadaInvalidaException.class, () -> servidor.mover(sessoes[0], new Posicao(7, 4)));
    }

    @Test
    void vitoriaEAvisadaComEstadoFinalAntes() throws Exception {
        registrarQuatro();
        // J1 sobe a coluna 4 até a linha 0; os outros vão e voltam longe dessa coluna.
        Posicao[][] vaiEVem = {
                {new Posicao(0, 3), new Posicao(0, 2)}, // J2
                {new Posicao(3, 8), new Posicao(4, 8)}, // J3
                {new Posicao(3, 0), new Posicao(4, 0)}  // J4
        };
        int passo = 0;
        while (servidor.obterEstado().getStatus() == EstadoJogo.Status.EM_ANDAMENTO) {
            Posicao p1 = servidor.obterEstado().getPosicao(1);
            servidor.mover(sessoes[0], new Posicao(p1.linha() - 1, p1.coluna()));
            if (servidor.obterEstado().getStatus() != EstadoJogo.Status.EM_ANDAMENTO) break;
            for (int j = 1; j <= 3; j++) {
                servidor.mover(sessoes[j], vaiEVem[j - 1][passo % 2]);
            }
            passo++;
        }
        assertEquals(1, servidor.obterEstado().getVencedor());
        for (CallbackFalso cb : cbs) {
            assertEquals(List.of(1), cb.fins);
            assertEquals(EstadoJogo.Status.FINALIZADO, cb.ultimo().getStatus());
        }
    }
}
