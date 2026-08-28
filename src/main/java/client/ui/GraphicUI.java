package client.ui;

import client.JogadorUI;
import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;

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

    private final boolean modoAutomatico;
    private GameServer servidor;
    private int meuId;
    private volatile EstadoJogo estado;

    public GraphicUI(String modo) {
        this.modoAutomatico = "auto".equalsIgnoreCase(modo);
        montarJanela();
        tabuleiro.setListener(new TabuleiroPanel.JogadaListener() {
            @Override public void onMover(Posicao d) { mover(d); }
            @Override public void onColocarCerca(Cerca c) { colocarCerca(c); }
            @Override public void onAviso(String m) { barraStatus.setErro(m); }
        });
    }

    public boolean modoAutomatico() { return modoAutomatico; }
    public void setServidor(GameServer s) { this.servidor = s; }

    @Override
    public void novoEstado(EstadoJogo estado) {
        this.estado = estado;
        SwingUtilities.invokeLater(this::atualizarPainel);
    }

    @Override
    public void finalizar(int idVencedor) {
        SwingUtilities.invokeLater(() -> {
            EstadoJogo e = this.estado;
            String nome = e == null ? "Jogador " + idVencedor : e.getNome(idVencedor);
            barraStatus.setMensagem("FIM — vencedor: Jogador " + idVencedor + " (" + nome + ")");
            JOptionPane.showMessageDialog(janela,
                    "O vencedor é o Jogador " + idVencedor + " (" + nome + ")!",
                    "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override public synchronized EstadoJogo getEstadoAtual() { return estado; }

    @Override
    public void setMeuId(int id) {
        this.meuId = id;
        tabuleiro.setMeuId(id);
        janela.setTitle("Quoridor — Jogador " + id);
        identidade.setText("Você é o Jogador " + id);
    }

    public void abrir() {
        if (GraphicsEnvironment.isHeadless()) return;
        janela.pack();
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
    }

    private void montarJanela() {
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.getContentPane().setBackground(EstiloUI.FUNDO_JANELA);
        janela.setLayout(new BorderLayout());

        JPanel topo = new JPanel(new BorderLayout());
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
        topo.add(identidade, BorderLayout.EAST);
        janela.add(topo, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBackground(EstiloUI.FUNDO_JANELA);
        centro.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        centro.add(montarBarraFerramentas(), BorderLayout.NORTH);
        centro.add(tabuleiro, BorderLayout.CENTER);
        janela.add(centro, BorderLayout.CENTER);

        painelJogadores.setPreferredSize(new Dimension(260, 0));
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
        barra.add(btnMover);
        barra.add(btnCercaH);
        barra.add(btnCercaV);
        return barra;
    }

    private void atualizarPainel() {
        EstadoJogo e = this.estado;
        tabuleiro.setEstado(e);
        painelJogadores.atualizar(e, meuId);
        if (e.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            barraStatus.setMensagem("Aguardando os 4 jogadores entrarem...");
        } else if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
            barraStatus.setMensagem("FIM — vencedor: " + e.getNome(e.getVencedor()));
        } else if (e.getJogadorDaVez() == meuId) {
            barraStatus.setMensagem("Sua vez! Clique em uma casa para mover ou use a barra para colocar cerca.");
        } else {
            barraStatus.setMensagem("Vez do " + e.getNome(e.getJogadorDaVez()) + " — aguardando jogada...");
        }
    }

    private void mover(Posicao destino) {
        try {
            servidor.mover(meuId, destino);
        } catch (JogadaInvalidaException | RemoteException ex) {
            barraStatus.setErro(ex.getMessage());
        }
    }

    private void colocarCerca(Cerca cerca) {
        try {
            servidor.colocarCerca(meuId, cerca);
        } catch (JogadaInvalidaException | RemoteException ex) {
            barraStatus.setErro(ex.getMessage());
        }
    }
}
