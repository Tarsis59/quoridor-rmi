package client.ui;

import client.CaixaEstado;
import client.JogadorUI;
import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import common.Sessao;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Janela do jogador: cabeçalho, ferramentas, tabuleiro, jogadores, histórico e status. */
public class GraphicUI implements JogadorUI {
    private final JFrame janela = new JFrame("Quoridor");
    private final TabuleiroPanel tabuleiro = new TabuleiroPanel();
    private final PainelJogadores painelJogadores = new PainelJogadores();
    private final PainelHistorico painelHistorico = new PainelHistorico();
    private final BarraStatus barraStatus = new BarraStatus();
    private final Chip chipModo = new Chip();
    private final Chip chipIdentidade = new Chip();
    private final JLabel dica = new JLabel();
    private final BotaoModo btnMover = new BotaoModo("Mover", "M", true);
    private final BotaoModo btnCercaH = new BotaoModo("Cerca H", "H", false);
    private final BotaoModo btnCercaV = new BotaoModo("Cerca V", "V", false);

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
    /** Último estado já desenhado (só na EDT): base para descrever o que mudou no histórico. */
    private EstadoJogo exibido;
    private boolean fimMostrado;

    public GraphicUI(String modo) {
        this.modoAutomatico = "auto".equalsIgnoreCase(modo);
        montarJanela();
        tabuleiro.setListener(new TabuleiroPanel.JogadaListener() {
            @Override public void onMover(Posicao d) { enviar(() -> servidor.mover(sessao, d)); }
            @Override public void onColocarCerca(Cerca c) { enviar(() -> servidor.colocarCerca(sessao, c)); }
            @Override public void onAviso(String m) { barraStatus.setErro(m); }
        });
        tabuleiro.setAoMudarModo(this::aoMudarModo);
        // No modo automático quem joga é o bot; o tabuleiro fica só de exibição.
        tabuleiro.setInterativo(!modoAutomatico);
        barraStatus.setMensagem("Conectando ao servidor...", EstiloUI.TEXTO_SECUNDARIO);
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
            if (fimMostrado) return;
            fimMostrado = true;
            EstadoJogo e = caixa.atual();
            String msg;
            if (idVencedor == 0) {
                msg = "Partida encerrada sem vencedor (todos saíram).";
            } else {
                String nome = e == null ? "Jogador " + idVencedor : e.getNome(idVencedor);
                msg = idVencedor == meuId
                        ? "Parabéns, você venceu! (" + nome + ")"
                        : "O vencedor é " + nome + " (Jogador " + idVencedor + ").";
            }
            // No modo automático o aviso sobre o tabuleiro basta; no manual, um diálogo confirma.
            if (janela.isShowing() && !modoAutomatico) {
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
            chipIdentidade.configurar("Você: Jogador " + id + " · " + EstiloUI.NOME_COR[id - 1],
                    EstiloUI.COR_PEAO[id - 1]);
            janela.setIconImage(icone(EstiloUI.COR_PEAO[id - 1]));
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
            janela.setMinimumSize(janela.getSize());
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);
        });
    }

    // ------------------------------------------------------------------ montagem

    private void montarJanela() {
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setIconImage(icone(EstiloUI.DESTAQUE));
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(EstiloUI.FUNDO_JANELA);
        janela.setContentPane(raiz);

        raiz.add(montarCabecalho(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 12));
        centro.add(montarBarraFerramentas(), BorderLayout.NORTH);
        JPanel moldura = new JPanel(new GridBagLayout()); // centraliza o tabuleiro se a janela crescer
        moldura.setOpaque(false);
        moldura.add(tabuleiro);
        centro.add(moldura, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        JPanel lateral = new JPanel();
        lateral.setOpaque(false);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 20));
        painelJogadores.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        painelHistorico.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        lateral.add(painelJogadores);
        lateral.add(Box.createVerticalStrut(18));
        lateral.add(painelHistorico);
        lateral.add(Box.createVerticalGlue());
        lateral.setPreferredSize(new Dimension(330, 0));
        raiz.add(lateral, BorderLayout.EAST);

        raiz.add(barraStatus, BorderLayout.SOUTH);
        registrarAtalhos(raiz);
    }

    private JComponent montarCabecalho() {
        JPanel topo = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(EstiloUI.SUPERFICIE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(EstiloUI.BORDA);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                // faixa fina com as 4 cores dos jogadores
                int q = getWidth() / 4;
                for (int i = 0; i < 4; i++) {
                    g.setColor(EstiloUI.COR_PEAO[i]);
                    g.fillRect(i * q, 0, i == 3 ? getWidth() - 3 * q : q, 3);
                }
                g.dispose();
            }
        };
        topo.setBorder(BorderFactory.createEmptyBorder(14, 20, 12, 20));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("QUORIDOR");
        titulo.setFont(EstiloUI.FONTE_TITULO);
        titulo.setForeground(EstiloUI.DESTAQUE);
        JLabel subtitulo = new JLabel("4 jogadores · Java RMI distribuído");
        subtitulo.setFont(EstiloUI.FONTE_PEQUENA);
        subtitulo.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        subtitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        marca.add(titulo);
        marca.add(subtitulo);
        topo.add(marca, BorderLayout.WEST);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        chips.setOpaque(false);
        chipModo.configurar(modoAutomatico ? "Modo automático" : "Modo manual",
                modoAutomatico ? EstiloUI.COR_PEAO[1] : EstiloUI.VERDE_OK);
        chipIdentidade.configurar("Entrando na partida...", EstiloUI.TEXTO_SECUNDARIO);
        chips.add(chipModo);
        chips.add(chipIdentidade);
        topo.add(chips, BorderLayout.EAST);
        return topo;
    }

    private JComponent montarBarraFerramentas() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botoes.setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        for (BotaoModo b : new BotaoModo[]{btnMover, btnCercaH, btnCercaV}) {
            grupo.add(b);
            botoes.add(b);
            b.setEnabled(!modoAutomatico);
        }
        btnMover.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.MOVER));
        btnCercaH.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.CERCA_H));
        btnCercaV.addActionListener(e -> tabuleiro.setModo(TabuleiroPanel.Modo.CERCA_V));
        btnCercaH.setToolTipText("Cerca horizontal: aponte para a linha entre duas fileiras (R ou botão direito gira)");
        btnCercaV.setToolTipText("Cerca vertical: aponte para a linha entre duas colunas (R ou botão direito gira)");
        barra.add(botoes, BorderLayout.WEST);

        dica.setFont(EstiloUI.FONTE_PEQUENA);
        dica.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        dica.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        dica.setText(modoAutomatico ? "Bots jogando — só assista" : "R: girar cerca");
        barra.add(dica, BorderLayout.EAST);
        return barra;
    }

    private void registrarAtalhos(JComponent raiz) {
        atalho(raiz, "M", () -> selecionarModo(TabuleiroPanel.Modo.MOVER));
        atalho(raiz, "H", () -> selecionarModo(TabuleiroPanel.Modo.CERCA_H));
        atalho(raiz, "V", () -> selecionarModo(TabuleiroPanel.Modo.CERCA_V));
        atalho(raiz, "R", () -> { if (!modoAutomatico) tabuleiro.alternarOrientacao(); });
        atalho(raiz, "ESCAPE", () -> selecionarModo(TabuleiroPanel.Modo.MOVER));
    }

    private void atalho(JComponent raiz, String tecla, Runnable acao) {
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla), tecla);
        raiz.getActionMap().put(tecla, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { acao.run(); }
        });
    }

    private void selecionarModo(TabuleiroPanel.Modo modo) {
        if (modoAutomatico) return;
        BotaoModo b = modo == TabuleiroPanel.Modo.MOVER ? btnMover
                : modo == TabuleiroPanel.Modo.CERCA_H ? btnCercaH : btnCercaV;
        if (b.isEnabled()) tabuleiro.setModo(modo);
    }

    private void aoMudarModo(TabuleiroPanel.Modo modo) {
        btnMover.setSelected(modo == TabuleiroPanel.Modo.MOVER);
        btnCercaH.setSelected(modo == TabuleiroPanel.Modo.CERCA_H);
        btnCercaV.setSelected(modo == TabuleiroPanel.Modo.CERCA_V);
    }

    // ------------------------------------------------------------------ atualização

    private void atualizarPainel() {
        EstadoJogo e = caixa.atual();
        if (e == null) return;
        if (e != exibido) {
            painelHistorico.adicionar(Historico.diferencas(exibido, e));
            exibido = e;
        }
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
        int vez = e.getJogadorDaVez();
        if (e.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            barraStatus.setMensagem("Aguardando os 4 jogadores entrarem... (" + e.getRegistrados() + "/4)",
                    EstiloUI.DESTAQUE);
        } else if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
            int v = e.getVencedor();
            barraStatus.setMensagem(v == 0 ? "Fim — partida encerrada sem vencedor"
                            : "Fim — " + e.getNome(v) + " (Jogador " + v + ") venceu!",
                    v == 0 ? EstiloUI.TEXTO_SECUNDARIO : EstiloUI.COR_PEAO[v - 1]);
        } else if (vez == meuId) {
            int restantes = e.getCercasRestantes(meuId);
            barraStatus.setMensagem(modoAutomatico ? "Sua vez — o bot está pensando..."
                            : "Sua vez! Clique numa casa marcada para andar, ou use Cerca H/V ("
                            + restantes + (restantes == 1 ? " cerca restante)." : " cercas restantes)."),
                    EstiloUI.COR_PEAO[vez - 1]);
        } else {
            barraStatus.setMensagem("Vez de " + e.getNome(vez) + " (Jogador " + vez + ") — aguardando a jogada...",
                    EstiloUI.COR_PEAO[vez - 1]);
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

    // Acesso para testes.
    BarraStatus status() { return barraStatus; }

    List<String> textosHistorico() {
        List<String> r = new ArrayList<>();
        for (Historico.Evento ev : painelHistorico.itens()) r.add(ev.texto());
        return r;
    }

    /** Ícone da janela: 4 peões nas cores dos jogadores sobre um quadrado escuro. */
    private static Image icone(Color destaque) {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        EstiloUI.suavizar(g);
        g.setColor(EstiloUI.MOLDURA);
        g.fillRoundRect(0, 0, 64, 64, 16, 16);
        g.setColor(destaque);
        g.drawRoundRect(1, 1, 61, 61, 16, 16);
        int[][] pos = {{32, 50}, {32, 14}, {50, 32}, {14, 32}};
        for (int i = 0; i < 4; i++) {
            g.setColor(EstiloUI.COR_PEAO[i]);
            g.fillOval(pos[i][0] - 8, pos[i][1] - 8, 16, 16);
        }
        g.dispose();
        return img;
    }

    /** Etiqueta em forma de pílula com uma bolinha colorida (cabeçalho). */
    static final class Chip extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color cor = EstiloUI.TEXTO_SECUNDARIO;

        Chip() {
            setFont(EstiloUI.FONTE_PEQUENA);
            setForeground(EstiloUI.TEXTO_PRINCIPAL);
            setBorder(BorderFactory.createEmptyBorder(6, 26, 6, 12));
            setOpaque(false);
        }

        void configurar(String texto, Color cor) {
            setText(texto);
            this.cor = cor;
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            EstiloUI.suavizar(g);
            int h = getHeight();
            g.setColor(EstiloUI.CARTAO);
            g.fillRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            g.setColor(EstiloUI.BORDA);
            g.drawRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            g.setColor(cor);
            g.fillOval(11, h / 2 - 4, 8, 8);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
