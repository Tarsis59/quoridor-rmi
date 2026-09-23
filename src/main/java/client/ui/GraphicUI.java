package client.ui;

import client.CaixaEstado;
import client.JogadorUI;
import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import common.Sessao;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GraphicUI implements JogadorUI {
    private final JFrame janela = new JFrame("Quoridor");
    private final TabuleiroPanel tabuleiro = new TabuleiroPanel();
    private final PainelJogadores painelJogadores = new PainelJogadores();
    private final BarraStatus barraStatus = new BarraStatus();
    private final JLabel titulo = new JLabel("QUORIDOR");
    private final JLabel modoLabel = new JLabel();
    private final JLabel identidade = new JLabel();
    private final JToggleButton btnMover = new JToggleButton("Mover", true);
    private final JToggleButton btnCercaH = new JToggleButton("Cerca H");
    private final JToggleButton btnCercaV = new JToggleButton("Cerca V");

    /** Chamadas RMI saem da EDT: a janela nunca congela esperando o servidor. */
    private final ExecutorService envios = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "envio-jogadas");
        t.setDaemon(true);
        return t;
    });
    private final CaixaEstado caixa = new CaixaEstado();
    private final boolean modoAutomatico;
    private volatile GameServer servidor;
    private volatile Sessao sessao;
    private volatile int meuId;

    public GraphicUI(String modo) {
        this.modoAutomatico = "auto".equalsIgnoreCase(modo);
        montarJanela();
        tabuleiro.setListener(new TabuleiroPanel.JogadaListener() {
            @Override public void onMover(Posicao d) { enviar(() -> servidor.mover(sessao, d)); }
            @Override public void onColocarCerca(Cerca c) { enviar(() -> servidor.colocarCerca(sessao, c)); }
            @Override public void onAviso(String m) { barraStatus.setErro(m); }
        });
        tabuleiro.setAoMudarModo(this::sincronizarBotoes);
        // No modo automático quem joga é o bot; cliques no tabuleiro ficam desativados.
        tabuleiro.setInterativo(!modoAutomatico);
    }

    public boolean modoAutomatico() { return modoAutomatico; }
    public void setServidor(GameServer s) { this.servidor = s; }

    @Override
    public void novoEstado(EstadoJogo estado) {
        caixa.publicar(estado);
        SwingUtilities.invokeLater(this::atualizarPainel);
    }

    @Override
    public void finalizar(int idVencedor) {
        SwingUtilities.invokeLater(() -> {
            atualizarPainel();
            EstadoJogo e = caixa.atual();
            String msg;
            if (idVencedor == 0) {
                msg = "Partida encerrada sem vencedor (todos saíram).";
            } else {
                String nome = e == null ? "Jogador " + idVencedor : e.getNome(idVencedor);
                msg = idVencedor == meuId
                        ? "Parabéns, você venceu! (" + nome + ")"
                        : "O vencedor é o Jogador " + idVencedor + " (" + nome + ").";
            }
            barraStatus.setMensagem("FIM — " + msg);
            if (janela.isShowing()) {
                JOptionPane.showMessageDialog(janela, msg, "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    @Override public EstadoJogo getEstadoAtual() { return caixa.atual(); }

    @Override
    public EstadoJogo aguardarEstadoDiferenteDe(EstadoJogo anterior, long prazoMs) throws InterruptedException {
        return caixa.aguardarDiferenteDe(anterior, prazoMs);
    }

    @Override
    public void setSessao(Sessao sessao) {
        this.sessao = sessao;
        this.meuId = sessao.idJogador();
        int id = meuId;
        Runnable aplicar = () -> {
            tabuleiro.setMeuId(id);
            janela.setTitle("Quoridor — Jogador " + id + " (" + EstiloUI.NOME_COR[id - 1] + ")");
            identidade.setText("Você: Jogador " + id + " (" + EstiloUI.NOME_COR[id - 1] + ")");
            // O estado inicial pode ter chegado por callback antes de sabermos nosso id.
            if (caixa.atual() != null) atualizarPainel();
        };
        if (SwingUtilities.isEventDispatchThread()) aplicar.run();
        else SwingUtilities.invokeLater(aplicar);
    }

    public void abrir() {
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeLater(() -> {
            janela.pack();
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
        });
    }

    private void montarJanela() {
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.getContentPane().setBackground(EstiloUI.FUNDO_JANELA);
        janela.setLayout(new BorderLayout());

        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setBackground(EstiloUI.TOPO);
        topo.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        titulo.setFont(EstiloUI.FONTE_TITULO);
        titulo.setForeground(EstiloUI.TITULO);
        topo.add(titulo, BorderLayout.WEST);
        modoLabel.setText(modoAutomatico ? "Modo: Automático" : "Modo: Manual");
        modoLabel.setFont(EstiloUI.FONTE_ROTULO);
        modoLabel.setForeground(Color.WHITE);
        modoLabel.setBackground(new Color(0x3A4556));
        modoLabel.setOpaque(true);
        modoLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        topo.add(modoLabel, BorderLayout.CENTER);
        identidade.setForeground(new Color(0xC7D2E0));
        identidade.setFont(EstiloUI.FONTE_NORMAL);
        identidade.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        topo.add(identidade, BorderLayout.EAST);
        janela.add(topo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBackground(EstiloUI.FUNDO_JANELA);
        centro.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        centro.add(montarBarraFerramentas(), BorderLayout.NORTH);
        centro.add(tabuleiro, BorderLayout.CENTER);
        janela.add(centro, BorderLayout.CENTER);

        painelJogadores.setPreferredSize(new Dimension(280, 0));
        janela.add(painelJogadores, BorderLayout.EAST);
        janela.add(barraStatus, BorderLayout.SOUTH);
    }

    private JPanel montarBarraFerramentas() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setBackground(EstiloUI.FUNDO_JANELA);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(btnMover);
        grupo.add(btnCercaH);
        grupo.add(btnCercaV);
        btnMover.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.MOVER));
        btnCercaH.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.CERCA_H));
        btnCercaV.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.CERCA_V));
        btnCercaH.setToolTipText("Clique na linha entre duas fileiras. Botão direito alterna H/V.");
        btnCercaV.setToolTipText("Clique na linha entre duas colunas. Botão direito alterna H/V.");
        for (JToggleButton botao : new JToggleButton[]{btnMover, btnCercaH, btnCercaV}) {
            // Folga fixa: o texto nunca é truncado ("Cerca..."), qualquer que seja a fonte do sistema.
            Dimension d = botao.getPreferredSize();
            botao.setPreferredSize(new Dimension(Math.max(d.width + 16, 96), d.height));
            barra.add(botao);
        }
        boolean manual = !modoAutomatico;
        btnMover.setEnabled(manual);
        btnCercaH.setEnabled(manual);
        btnCercaV.setEnabled(manual);
        return barra;
    }

    private void sincronizarBotoes(TabuleiroPanel.Modo modo) {
        btnMover.setSelected(modo == TabuleiroPanel.Modo.MOVER);
        btnCercaH.setSelected(modo == TabuleiroPanel.Modo.CERCA_H);
        btnCercaV.setSelected(modo == TabuleiroPanel.Modo.CERCA_V);
    }

    private void atualizarPainel() {
        EstadoJogo e = caixa.atual();
        if (e == null) return;
        tabuleiro.setEstado(e);
        painelJogadores.atualizar(e, meuId);
        boolean temCercas = meuId > 0 && e.getCercasRestantes(meuId) > 0;
        if (!modoAutomatico) {
            btnCercaH.setEnabled(temCercas);
            btnCercaV.setEnabled(temCercas);
            if (!temCercas && tabuleiro.getModo() != TabuleiroPanel.Modo.MOVER) {
                tabuleiro.setModo(TabuleiroPanel.Modo.MOVER);
            }
        }
        if (e.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            barraStatus.setMensagem("Aguardando os 4 jogadores entrarem... (" + e.getRegistrados() + "/4)");
        } else if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
            barraStatus.setMensagem(e.getVencedor() == 0 ? "FIM — sem vencedor"
                    : "FIM — vencedor: Jogador " + e.getVencedor() + " (" + e.getNome(e.getVencedor()) + ")");
        } else if (e.getJogadorDaVez() == meuId) {
            barraStatus.setMensagem(modoAutomatico ? "Sua vez — o bot está pensando..."
                    : "Sua vez! Clique numa casa destacada para mover ou use Cerca H/V ("
                    + e.getCercasRestantes(meuId) + " restantes).");
        } else {
            barraStatus.setMensagem("Vez do Jogador " + e.getJogadorDaVez() + " ("
                    + e.getNome(e.getJogadorDaVez()) + ") — aguardando jogada...");
        }
    }

    private interface ChamadaRemota {
        void executar() throws RemoteException, JogadaInvalidaException;
    }

    private void enviar(ChamadaRemota chamada) {
        if (servidor == null || sessao == null) return;
        envios.submit(() -> {
            try {
                chamada.executar();
            } catch (JogadaInvalidaException ex) {
                SwingUtilities.invokeLater(() -> barraStatus.setErro(ex.getMessage()));
            } catch (RemoteException ex) {
                SwingUtilities.invokeLater(() -> barraStatus.setErro(
                        "Erro de comunicação com o servidor: " + ex.getMessage()));
            }
        });
    }
}
