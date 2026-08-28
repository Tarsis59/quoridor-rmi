# Interface Gráfica Swing — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar uma interface gráfica Swing (estilo Moderno Plano) ao cliente Quoridor, com dois modos de uso — Manual (cliques) e Automático (bots ilustrativos) — sem alterar o RMI.

**Architecture:** Nova implementação `GraphicUI` da interface local `UI`, dentro de um novo pacote `client.ui`. O servidor, o engine e as classes comuns ficam intactos. A GUI reutiliza `Tabuleiro.aPartirDe(estado)` (mesmo padrão do `BotJogador`) para destacar jogadas legais e validar cercas localmente. `ClientMain` ganha `--gui` e `--modo manual|auto`.

**Tech Stack:** Java 21, Swing/AWT (java.desktop, já no JDK), Maven, JUnit 5. Zero dependências novas. RMI inalterado (`SemSocketTest` deve continuar verde).

**Contexto de memória:** máquina com pouca RAM — usar `MAVEN_OPTS="-Xmx512m"`; se OOM, adicionar `-DargLine="-Xmx256m"`. O `E2ETest` já limita as JVMs filhas.

---

### Task 1: Geometria do tabuleiro (pura, testável)

**Files:**
- Create: `src/main/java/client/ui/Geometria.java`
- Test: `src/test/java/client/ui/GeometriaTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/client/ui/GeometriaTest.java`:

```java
package client.ui;

import common.Cerca;
import common.Orientacao;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeometriaTest {

    @Test
    void casaEmCentroDeCadaCelula() {
        assertEquals(new Posicao(0, 0), Geometria.casaEm(Geometria.MARGEM + 26, Geometria.MARGEM + 26));
        assertEquals(new Posicao(4, 4), Geometria.casaEm(
                Geometria.MARGEM + 4 * Geometria.CELULA + 26,
                Geometria.MARGEM + 4 * Geometria.CELULA + 26));
        assertEquals(new Posicao(8, 8), Geometria.casaEm(
                Geometria.LADO - Geometria.MARGEM - 1,
                Geometria.LADO - Geometria.MARGEM - 1));
    }

    @Test
    void casaEmForaDoTabuleiroRetornaNull() {
        assertNull(Geometria.casaEm(0, Geometria.MARGEM + 26));
        assertNull(Geometria.casaEm(Geometria.LADO - 1, Geometria.LADO - 1));
    }

    @Test
    void centroDaCasa() {
        Point c = Geometria.centroDaCasa(2, 3);
        assertEquals(Geometria.MARGEM + 3 * Geometria.CELULA + 26, c.x);
        assertEquals(Geometria.MARGEM + 2 * Geometria.CELULA + 26, c.y);
    }

    @Test
    void cercaHorizontalNaLinhaEntre2e3() {
        int y = Geometria.MARGEM + 3 * Geometria.CELULA; // linha de grade entre linhas 2 e 3
        Cerca c = Geometria.cercaHorizontalEm(Geometria.MARGEM + 4 * Geometria.CELULA + 26, y);
        assertEquals(new Cerca(new Posicao(2, 4), Orientacao.HORIZONTAL), c);
    }

    @Test
    void cercaVerticalNaLinhaEntre5e6() {
        int x = Geometria.MARGEM + 6 * Geometria.CELULA; // linha de grade entre colunas 5 e 6
        Cerca c = Geometria.cercaVerticalEm(x, Geometria.MARGEM + 4 * Geometria.CELULA + 26);
        assertEquals(new Cerca(new Posicao(4, 5), Orientacao.VERTICAL), c);
    }

    @Test
    void cercaForaDoLimiteRetornaNull() {
        assertNull(Geometria.cercaHorizontalEm(Geometria.MARGEM + 26, Geometria.MARGEM));
        assertNull(Geometria.cercaVerticalEm(Geometria.MARGEM, Geometria.MARGEM + 26));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=GeometriaTest test`
Expected: FAIL — `Geometria` não existe (erro de compilação).

- [ ] **Step 3: Write minimal implementation**

Create `src/main/java/client/ui/Geometria.java`:

```java
package client.ui;

import common.Cerca;
import common.Orientacao;
import common.Posicao;

import java.awt.Point;
import java.awt.Rectangle;

/** Geometria pura (pixel <-> casa/aresta). Não toca em componentes Swing, permitindo testes unitários. */
public final class Geometria {
    public static final int CELULA = EstiloUI.CELULA;
    public static final int MARGEM = EstiloUI.MARGEM;
    public static final int LADO = EstiloUI.LADO;
    public static final int TAM = 9;

    private Geometria() {}

    public static Rectangle rectDaCasa(int linha, int coluna) {
        return new Rectangle(MARGEM + coluna * CELULA, MARGEM + linha * CELULA, CELULA, CELULA);
    }

    public static Point centroDaCasa(int linha, int coluna) {
        Rectangle r = rectDaCasa(linha, coluna);
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    public static Posicao casaEm(int x, int y) {
        int col = (x - MARGEM) / CELULA;
        int lin = (y - MARGEM) / CELULA;
        if (col < 0 || col >= TAM || lin < 0 || lin >= TAM) return null;
        return new Posicao(lin, col);
    }

    /** Cerca horizontal de base (r,c): linha entre r e r+1 cobrindo as colunas c..c+1. */
    public static Cerca cercaHorizontalEm(int x, int y) {
        int i = Math.round((y - MARGEM) / (float) CELULA);
        if (i < 1 || i > 8) return null;
        int r = i - 1;
        int c = Math.max(0, Math.min(7, (x - MARGEM) / CELULA));
        return new Cerca(new Posicao(r, c), Orientacao.HORIZONTAL);
    }

    /** Cerca vertical de base (r,c): linha entre c e c+1 cobrindo as linhas r..r+1. */
    public static Cerca cercaVerticalEm(int x, int y) {
        int i = Math.round((x - MARGEM) / (float) CELULA);
        if (i < 1 || i > 8) return null;
        int c = i - 1;
        int r = Math.max(0, Math.min(7, (y - MARGEM) / CELULA));
        return new Cerca(new Posicao(r, c), Orientacao.VERTICAL);
    }
}
```

Nota: `Geometria` referencia `EstiloUI` (criado na Task 2). Para compilar a Task 1 isoladamente, crie primeiro o `EstiloUI` mínimo da Task 2 (apenas as constantes `CELULA`, `MARGEM`, `LADO` já existirão lá).

- [ ] **Step 4: Run test to verify it passes**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=GeometriaTest test`
Expected: PASS (6 testes).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/client/ui/Geometria.java src/test/java/client/ui/GeometriaTest.java
git commit -m "feat: geometria do tabuleiro (pixel <-> casa/aresta) com testes"
```

---

### Task 2: Tema Moderno Plano (constantes de estilo)

**Files:**
- Create: `src/main/java/client/ui/EstiloUI.java`

- [ ] **Step 1: Write implementation**

Create `src/main/java/client/ui/EstiloUI.java`:

```java
package client.ui;

import java.awt.Color;
import java.awt.Font;

/** Tema visual "Moderno Plano" — paleta, fontes e medidas centralizadas. */
public final class EstiloUI {
    public static final Color FUNDO_JANELA = new Color(0xEEF1F6);
    public static final Color FUNDO_TABULEIRO = new Color(0xE9EDF2);
    public static final Color CASA = Color.WHITE;
    public static final Color BORDA_CASA = new Color(0xDFE4EA);
    public static final Color CASA_DESTAQUE = new Color(0xEAF7EC);
    public static final Color CERCA = new Color(0x334155);
    public static final Color TOPO = new Color(0x2B3442);
    public static final Color TITULO = new Color(0xF7C948);
    public static final Color TEXTO_PRINCIPAL = new Color(0x2B3442);
    public static final Color TEXTO_SECUNDARIO = new Color(0x7B8794);
    public static final Color BADGE_VEZ_FUNDO = new Color(0xFFF7E0);
    public static final Color BADGE_VEZ_BORDA = new Color(0xF3D16B);
    public static final Color BADGE_VEZ_TEXTO = new Color(0x6B5200);
    public static final Color VOCE_FUNDO = new Color(0xEEF6FF);
    public static final Color VOCE_BORDA = new Color(0xBCD9F7);
    public static final Color ERRO = new Color(0xDC2626);
    public static final Color VERDE_OK = new Color(0x22C55E);
    public static final Color VERMELHO_ERRADO = new Color(0xEF4444);

    public static final Color[] COR_PEAO = {
            new Color(0xEF4444), // J1 vermelho
            new Color(0x3B82F6), // J2 azul
            new Color(0x22C55E), // J3 verde
            new Color(0xF59E0B)  // J4 âmbar
    };
    public static final String[] NOME_COR = {"Vermelho", "Azul", "Verde", "Âmbar"};

    public static final int CELULA = 52;
    public static final int MARGEM = 12;
    public static final int LADO = 2 * MARGEM + 9 * CELULA;
    public static final int ESPESSURA_CERCA = 8;

    public static final Font FONTE_TITULO = new Font("Dialog", Font.BOLD, 16);
    public static final Font FONTE_NORMAL = new Font("Dialog", Font.PLAIN, 13);
    public static final Font FONTE_PEAO = new Font("Dialog", Font.BOLD, 18);
    public static final Font FONTE_ROTULO = new Font("Dialog", Font.BOLD, 10);

    private EstiloUI() {}
}
```

- [ ] **Step 2: Compile**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/client/ui/EstiloUI.java
git commit -m "feat: tema Moderno Plano com paleta e medidas centralizadas"
```

---

### Task 3: TabuleiroPanel — estado, cliques e jogadas (com testes)

**Files:**
- Create: `src/main/java/client/ui/TabuleiroPanel.java`
- Test: `src/test/java/client/ui/TabuleiroPanelTest.java`

Nesta task o painel ainda não desenha (a pintura é a Task 4); foca em: manter o estado, computar movimentos legais, tratar cliques e notificar via `JogadaListener`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/client/ui/TabuleiroPanelTest.java`:

```java
package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.MouseEvent;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuleiroPanelTest {
    private final List<Posicao> movidos = new ArrayList<>();
    private final List<Cerca> cercas = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private TabuleiroPanel panel;

    @BeforeEach
    void montaPanel() {
        panel = new TabuleiroPanel();
        panel.setListener(new TabuleiroPanel.JogadaListener() {
            @Override public void onMover(Posicao d) { movidos.add(d); }
            @Override public void onColocarCerca(Cerca c) { cercas.add(c); }
            @Override public void onAviso(String m) { avisos.add(m); }
        });
    }

    private EstadoJogo estadoJ1() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        return new EstadoJogo(pos, List.of(), new int[]{10, 10, 10, 10},
                1, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
    }

    private void clique(int linha, int coluna) {
        Point c = Geometria.centroDaCasa(linha, coluna);
        MouseEvent ev = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, c.x, c.y, 1, false, MouseEvent.BUTTON1);
        panel.getMouseListeners()[0].mouseClicked(ev);
    }

    @Test
    void cliqueEmCasaLegalMove() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        clique(7, 4); // vizinho acima de (8,4)
        assertEquals(List.of(new Posicao(7, 4)), movidos);
        assertTrue(avisos.isEmpty());
    }

    @Test
    void cliqueEmCasaIlegalAvisa() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        clique(0, 0);
        assertTrue(movidos.isEmpty());
        assertEquals(1, avisos.size());
    }

    @Test
    void cliqueEmModoCercaColoca() {
        Cerca alvo = new Cerca(new Posicao(2, 4), Orientacao.HORIZONTAL);
        assertTrue(new Tabuleiro().podeColocarCerca(alvo), "pré-condição: cerca deve ser válida");
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        panel.setModo(TabuleiroPanel.Modo.CERCA_H);
        Point c = Geometria.centroDaCasa(2, 4);
        MouseEvent ev = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, c.x, Geometria.MARGEM + 3 * Geometria.CELULA, 1, false, MouseEvent.BUTTON1);
        panel.getMouseListeners()[0].mouseClicked(ev);
        assertEquals(List.of(alvo), cercas);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=TabuleiroPanelTest test`
Expected: FAIL — `TabuleiroPanel` não existe.

- [ ] **Step 3: Write minimal implementation**

Create `src/main/java/client/ui/TabuleiroPanel.java`:

```java
package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TabuleiroPanel extends JPanel {
    public enum Modo { MOVER, CERCA_H, CERCA_V }

    public interface JogadaListener {
        void onMover(Posicao destino);
        void onColocarCerca(Cerca cerca);
        void onAviso(String mensagem);
    }

    private EstadoJogo estado;
    private Tabuleiro modelo;
    private int meuId;
    private Modo modo = Modo.MOVER;
    private List<Posicao> legais = List.of();
    private Cerca preview;
    private boolean previewValida;
    private JogadaListener listener;

    public TabuleiroPanel() {
        setPreferredSize(new Dimension(Geometria.LADO, Geometria.LADO));
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { atualizarPreview(e.getX(), e.getY()); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { alternarOrientacao(); return; }
                clique(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setListener(JogadaListener l) { this.listener = l; }
    public void setMeuId(int id) { this.meuId = id; recomputarLegais(); }
    public void setModo(Modo m) { this.modo = m; this.preview = null; repaint(); }
    public Modo getModo() { return modo; }

    public void setEstado(EstadoJogo e) {
        this.estado = e;
        this.modelo = Tabuleiro.aPartirDe(e.getPosicoes(), e.getCercas());
        recomputarLegais();
    }

    private void recomputarLegais() {
        this.legais = (estado != null && modelo != null && meuId > 0)
                ? modelo.movimentosValidos(meuId) : List.of();
        repaint();
    }

    private void atualizarPreview(int x, int y) {
        if (estado == null || modelo == null) { preview = null; repaint(); return; }
        preview = cercaCandidata(x, y);
        previewValida = (preview == null) || modelo.podeColocarCerca(preview);
        repaint();
    }

    private Cerca cercaCandidata(int x, int y) {
        if (modo == Modo.CERCA_H) return Geometria.cercaHorizontalEm(x, y);
        if (modo == Modo.CERCA_V) return Geometria.cercaVerticalEm(x, y);
        return null;
    }

    private void clique(int x, int y) {
        if (estado == null || modelo == null || listener == null) return;
        if (modo == Modo.MOVER) {
            Posicao casa = Geometria.casaEm(x, y);
            if (casa == null) return;
            if (legais.contains(casa)) listener.onMover(casa);
            else listener.onAviso("Movimento inválido para (" + casa.linha() + "," + casa.coluna() + ").");
        } else {
            Cerca c = cercaCandidata(x, y);
            if (c == null) return;
            if (modelo.podeColocarCerca(c)) {
                listener.onColocarCerca(c);
                setModo(Modo.MOVER);
            } else {
                listener.onAviso("Cerca inválida: sobreposição ou bloqueia um caminho.");
            }
        }
    }

    private void alternarOrientacao() {
        if (modo == Modo.CERCA_H) setModo(Modo.CERCA_V);
        else if (modo == Modo.CERCA_V) setModo(Modo.CERCA_H);
    }

    // Acesso para inspeção.
    Cerca getPreview() { return preview; }
    boolean isPreviewValida() { return previewValida; }
    EstadoJogo getEstado() { return estado; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=TabuleiroPanelTest test`
Expected: PASS (3 testes).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/client/ui/TabuleiroPanel.java src/test/java/client/ui/TabuleiroPanelTest.java
git commit -m "feat: TabuleiroPanel com cliques (mover/cerca) e jogadas legais"
```

---

### Task 4: TabuleiroPanel — pintura (peões, cercas, preview, destaque)

**Files:**
- Modify: `src/main/java/client/ui/TabuleiroPanel.java`

Sem teste unitário (visual); a validação é feita rodando a demo (Task 8). Adiciona o desenho ao `paintComponent`.

- [ ] **Step 1: Add painting to TabuleiroPanel**

Substitua o corpo atual de `TabuleiroPanel` pelos imports e métodos de pintura abaixo. Os campos e métodos da Task 3 permanecem; apenas acrescente imports e métodos:

Adicione aos imports existentes:

```java
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
```

(`java.util.List` já está importado da Task 3; `AlphaComposite` e `BasicStroke` são usados com nome completo.)

Adicione o método de pintura e seus auxiliares dentro da classe:

```java
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(EstiloUI.FUNDO_TABULEIRO);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        desenharCasas(g);
        desenharCercas(g);
        desenharPreview(g);
        desenharPeoes(g);
        desenharCoordenadas(g);

        g.dispose();
    }

    private void desenharCasas(Graphics2D g) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                var rec = Geometria.rectDaCasa(r, c);
                Color cor = corMeta(r, c);
                if (casaDestaque(r, c)) cor = EstiloUI.CASA_DESTAQUE;
                g.setColor(cor);
                g.fillRoundRect(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2, 6, 6);
                g.setColor(EstiloUI.BORDA_CASA);
                g.drawRoundRect(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2, 6, 6);
            }
        }
    }

    private Color corMeta(int r, int c) {
        if (r == 0) return new Color(0xFFF1F1);
        if (r == 8) return new Color(0xF1F6FF);
        if (c == 0) return new Color(0xF2FBF3);
        return new Color(0xFFF9EC);
    }

    private boolean casaDestaque(int r, int c) {
        return legais != null && legais.stream().anyMatch(p -> p.linha() == r && p.coluna() == c);
    }

    private void desenharCercas(Graphics2D g) {
        if (estado == null) return;
        g.setColor(EstiloUI.CERCA);
        for (Cerca c : estado.getCercas()) desenharParede(g, c);
    }

    private void desenharPreview(Graphics2D g) {
        if (preview == null) return;
        g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 0.45f));
        g.setColor(previewValida ? EstiloUI.VERDE_OK : EstiloUI.VERMELHO_ERRADO);
        desenharParede(g, preview);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private void desenharPeoes(Graphics2D g) {
        if (estado == null) return;
        for (int id = 1; id <= 4; id++) {
            Posicao p = estado.getPosicao(id);
            var c = Geometria.centroDaCasa(p.linha(), p.coluna());
            int raio = Geometria.CELULA / 3;
            g.setColor(EstiloUI.COR_PEAO[id - 1]);
            g.fillOval(c.x - raio, c.y - raio, raio * 2, raio * 2);
            if (estado.getJogadorDaVez() == id) {
                g.setColor(EstiloUI.BADGE_VEZ_BORDA);
                g.setStroke(new java.awt.BasicStroke(3f));
                g.drawOval(c.x - raio - 4, c.y - raio - 4, raio * 2 + 8, raio * 2 + 8);
                g.setStroke(new java.awt.BasicStroke(1f));
            }
            g.setColor(Color.WHITE);
            g.setFont(EstiloUI.FONTE_PEAO);
            String numero = String.valueOf(id);
            var fm = g.getFontMetrics();
            int tx = c.x - fm.stringWidth(numero) / 2;
            int ty = c.y + fm.getAscent() / 2 - 2;
            g.drawString(numero, tx, ty);
        }
    }

    private void desenharParede(Graphics2D g, Cerca c) {
        int r = c.base().linha();
        int col = c.base().coluna();
        int t = EstiloUI.ESPESSURA_CERCA;
        if (c.orientacao() == Orientacao.HORIZONTAL) {
            int y = Geometria.MARGEM + (r + 1) * Geometria.CELULA - t / 2;
            int x = Geometria.MARGEM + col * Geometria.CELULA;
            g.fillRoundRect(x, y, 2 * Geometria.CELULA, t, 4, 4);
        } else {
            int x = Geometria.MARGEM + (col + 1) * Geometria.CELULA - t / 2;
            int y = Geometria.MARGEM + r * Geometria.CELULA;
            g.fillRoundRect(x, y, t, 2 * Geometria.CELULA, 4, 4);
        }
    }

    private void desenharCoordenadas(Graphics2D g) {
        g.setFont(EstiloUI.FONTE_ROTULO);
        g.setColor(EstiloUI.TEXTO_SECUNDARIO);
        for (int i = 0; i < 9; i++) {
            var col = Geometria.rectDaCasa(0, i);
            g.drawString(String.valueOf(i), col.x + col.width / 2 - 3, Geometria.MARGEM - 4);
            var lin = Geometria.rectDaCasa(i, 0);
            g.drawString(String.valueOf(i), Geometria.MARGEM - 14, lin.y + lin.height / 2 + 4);
        }
    }
```

- [ ] **Step 2: Compile**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/client/ui/TabuleiroPanel.java
git commit -m "feat: pintura do tabuleiro (peões, cercas, preview e destaque de vez)"
```

---

### Task 5: PainelJogadores, BarraStatus e DialogoModo (+ testes)

**Files:**
- Create: `src/main/java/client/ui/PainelJogadores.java`
- Create: `src/main/java/client/ui/BarraStatus.java`
- Create: `src/main/java/client/ui/DialogoModo.java`
- Test: `src/test/java/client/ui/PainelJogadoresTest.java`
- Test: `src/test/java/client/ui/BarraStatusTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/client/ui/PainelJogadoresTest.java`:

```java
package client.ui;

import common.EstadoJogo;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PainelJogadoresTest {

    @Test
    void destacaVezEVoce() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        EstadoJogo e = new EstadoJogo(pos, List.of(), new int[]{10, 9, 8, 7},
                3, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
        PainelJogadores p = new PainelJogadores();
        p.atualizar(e, 2);
        assertEquals("VEZ", p.badge(3).getText());
        assertEquals("", p.badge(1).getText());
        assertEquals(EstiloUI.VOCE_FUNDO, p.linha(2).getBackground());
    }
}
```

Create `src/test/java/client/ui/BarraStatusTest.java`:

```java
package client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarraStatusTest {

    @Test
    void mensagemEErro() {
        BarraStatus s = new BarraStatus();
        s.setMensagem("Vez do Jogador 1");
        assertEquals("Vez do Jogador 1", s.getTexto());
        assertEquals(EstiloUI.TEXTO_PRINCIPAL, s.getForeground());
        s.setErro("Jogada inválida");
        assertEquals(EstiloUI.ERRO, s.getForeground());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=PainelJogadoresTest,BarraStatusTest test`
Expected: FAIL — classes não existem.

- [ ] **Step 3: Write minimal implementation**

Create `src/main/java/client/ui/PainelJogadores.java`:

```java
package client.ui;

import common.EstadoJogo;
import common.Posicao;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class PainelJogadores extends JPanel {
    private final List<JPanel> linhas = new ArrayList<>();
    private final List<JLabel> badges = new ArrayList<>();
    private final List<JLabel> metadados = new ArrayList<>();
    private final List<JPanel> dots = new ArrayList<>();

    public PainelJogadores() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(EstiloUI.FUNDO_JANELA);
        JLabel titulo = new JLabel("JOGADORES");
        titulo.setFont(EstiloUI.FONTE_ROTULO);
        titulo.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        titulo.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 4));
        add(titulo);
        for (int id = 1; id <= 4; id++) {
            JPanel linha = criarLinha(id);
            linhas.add(linha);
            add(linha);
        }
    }

    private JPanel criarLinha(int id) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        linha.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setBackground(EstiloUI.COR_PEAO[id - 1]);
        dots.add(dot);
        linha.add(dot);

        JLabel nome = new JLabel("Jogador " + id);
        nome.setFont(EstiloUI.FONTE_NORMAL);
        linha.add(nome);

        JLabel meta = new JLabel();
        meta.setFont(EstiloUI.FONTE_ROTULO);
        meta.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        metadados.add(meta);
        linha.add(meta);

        JLabel badge = new JLabel();
        badge.setFont(EstiloUI.FONTE_ROTULO);
        badge.setForeground(EstiloUI.BADGE_VEZ_TEXTO);
        badge.setOpaque(true);
        badges.add(badge);
        linha.add(badge);

        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return linha;
    }

    public void atualizar(EstadoJogo e, int meuId) {
        for (int id = 1; id <= 4; id++) {
            JPanel linha = linhas.get(id - 1);
            boolean vez = e.getJogadorDaVez() == id;
            boolean voce = meuId == id;
            linha.setBackground(vez ? EstiloUI.BADGE_VEZ_FUNDO
                    : voce ? EstiloUI.VOCE_FUNDO : EstiloUI.FUNDO_JANELA);
            linha.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(vez ? EstiloUI.BADGE_VEZ_BORDA
                            : voce ? EstiloUI.VOCE_BORDA : Color.WHITE),
                    BorderFactory.createEmptyBorder(5, 7, 5, 7)));
            Posicao p = e.getPosicao(id);
            metadados.get(id - 1).setText("(" + p.linha() + "," + p.coluna() + ") · "
                    + e.getCercasRestantes(id) + " cercas");
            badges.get(id - 1).setText(vez ? "VEZ" : "");
            badges.get(id - 1).setBackground(vez ? EstiloUI.BADGE_VEZ_BORDA : EstiloUI.FUNDO_JANELA);
        }
    }

    // Acesso para testes.
    JLabel badge(int idJogador) { return badges.get(idJogador - 1); }
    JPanel linha(int idJogador) { return linhas.get(idJogador - 1); }
}
```

Create `src/main/java/client/ui/BarraStatus.java`:

```java
package client.ui;

import javax.swing.JLabel;
import javax.swing.BorderFactory;

public class BarraStatus extends JLabel {
    public BarraStatus() {
        setFont(EstiloUI.FONTE_NORMAL);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public void setMensagem(String texto) {
        setText(texto);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
    }

    public void setErro(String texto) {
        setText(texto);
        setForeground(EstiloUI.ERRO);
    }

    String getTexto() { return getText(); }
}
```

Create `src/main/java/client/ui/DialogoModo.java`:

```java
package client.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Diálogo inicial: escolha entre jogar Manual ou Automático (bots ilustrativos). */
public final class DialogoModo {
    public static final String MANUAL = "manual";
    public static final String AUTO = "auto";

    private DialogoModo() {}

    /** Retorna "manual", "auto" ou null se a janela for fechada. */
    public static String perguntar(Component parent) {
        String[] opcoes = { "Modo Manual", "Modo Automático (bots)" };
        int escolha = JOptionPane.showOptionDialog(parent,
                "Como deseja jogar neste processo?",
                "Quoridor — escolha o modo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);
        if (escolha == JOptionPane.CLOSED_OPTION) return null;
        return escolha == 1 ? AUTO : MANUAL;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=PainelJogadoresTest,BarraStatusTest test`
Expected: PASS (2 testes).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/client/ui/PainelJogadores.java src/main/java/client/ui/BarraStatus.java src/main/java/client/ui/DialogoModo.java src/test/java/client/ui/PainelJogadoresTest.java src/test/java/client/ui/BarraStatusTest.java
git commit -m "feat: painel de jogadores, barra de status e diálogo de modo"
```

---

### Task 6: JogadorUI, GraphicUI e integração no ClientMain (+ teste)

**Files:**
- Create: `src/main/java/client/ui/GraphicUI.java`
- Create: `src/main/java/client/JogadorUI.java`
- Modify: `src/main/java/client/ConsoleUI.java:13` (linha do `implements`)
- Modify: `src/main/java/client/ClientMain.java`
- Test: `src/test/java/client/ui/GraphicUITest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/client/ui/GraphicUITest.java`:

```java
package client.ui;

import common.EstadoJogo;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphicUITest {

    private EstadoJogo estado() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        return new EstadoJogo(pos, List.of(), new int[]{10, 10, 10, 10},
                1, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
    }

    @Test
    void modoAutomaticoRefleteConstrutor() {
        assertTrue(new GraphicUI("auto").modoAutomatico());
        assertFalse(new GraphicUI("manual").modoAutomatico());
    }

    @Test
    void recebeEstadoEEntregaNoGetter() {
        GraphicUI g = new GraphicUI("manual");
        g.setMeuId(2);
        EstadoJogo e = estado();
        g.novoEstado(e);
        assertSame(e, g.getEstadoAtual());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=GraphicUITest test`
Expected: FAIL — `GraphicUI` não existe.

- [ ] **Step 3: Write implementation**

Create `src/main/java/client/JogadorUI.java`:

```java
package client;

import common.EstadoJogo;

/** UI que também carrega a identidade local do jogador e o estado atual (usada no modo bot). */
public interface JogadorUI extends UI {
    void setMeuId(int id);
    EstadoJogo getEstadoAtual();
}
```

Modify `src/main/java/client/ConsoleUI.java:13`:

```java
public class ConsoleUI implements JogadorUI {
```

(o restante do arquivo permanece igual; `ConsoleUI` já tem `setMeuId` e `getEstadoAtual`.)

Create `src/main/java/client/ui/GraphicUI.java`:

```java
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
```

Modify `src/main/java/client/ClientMain.java` (substitua o arquivo inteiro):

```java
package client;

import client.ui.DialogoModo;
import client.ui.GraphicUI;
import common.EstadoJogo;
import common.GameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        String nome = "Jogador";
        String host = "localhost";
        int porta = 1099;
        boolean bot = false;
        boolean gui = false;
        String modo = null; // "manual" | "auto"
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nome" -> { if (i + 1 < args.length) nome = args[++i]; }
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--porta" -> { if (i + 1 < args.length) porta = Integer.parseInt(args[++i]); }
                case "--bot" -> bot = true;
                case "--gui" -> gui = true;
                case "--modo" -> { if (i + 1 < args.length) modo = args[++i]; }
                default -> { }
            }
        }

        GameServer server = conectar(host, porta);

        JogadorUI ui;
        boolean automatico;
        if (gui) {
            String modoFinal = modo;
            if (modoFinal == null) {
                modoFinal = DialogoModo.perguntar(null);
                if (modoFinal == null) {
                    System.out.println("[CLIENTE] Nenhum modo escolhido. Encerrando.");
                    return;
                }
            }
            GraphicUI graphic = new GraphicUI(modoFinal);
            graphic.setServidor(server);
            ui = graphic;
            automatico = graphic.modoAutomatico();
        } else {
            ui = new ConsoleUI();
            automatico = bot;
        }

        ClientCallbackImpl callback = new ClientCallbackImpl(ui);
        int id = server.registrar(callback, nome);
        ui.setMeuId(id);
        System.out.println("[CLIENTE] Registrado como Jogador " + id + " (" + nome + ").");

        if (gui) {
            ((GraphicUI) ui).abrir();
        }

        if (automatico) {
            BotJogador botJogador = new BotJogador(id);
            System.out.println("[BOT " + id + "] Modo automático ativo.");
            while (true) {
                EstadoJogo e = ui.getEstadoAtual();
                if (e == null) {
                    Thread.sleep(200);
                    continue;
                }
                if (e.getStatus() == EstadoJogo.Status.FINALIZADO) break;
                if (e.getJogadorDaVez() == id) {
                    botJogador.executarJogada(server, e);
                } else {
                    Thread.sleep(100);
                }
            }
            EstadoJogo fim = ui.getEstadoAtual();
            System.out.println("[BOT " + id + "] FIM vencedor=" + fim.getVencedor()
                    + " nome=" + fim.getNome(fim.getVencedor()));
            System.exit(0);
        } else if (!gui) {
            ((ConsoleUI) ui).loop(server);
            System.exit(0);
        }
        // GUI em modo manual: a EDT (Event Dispatch Thread) mantém o app vivo
        // até a janela ser fechada (EXIT_ON_CLOSE).
    }

    private static GameServer conectar(String host, int porta) throws Exception {
        Exception ultimoErro = null;
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            try {
                Registry registry = LocateRegistry.getRegistry(host, porta);
                GameServer server = (GameServer) registry.lookup(GameServer.NOME_SERVICO);
                System.out.println("[CLIENTE] Conectado em rmi://" + host + ":" + porta + "/" + GameServer.NOME_SERVICO);
                return server;
            } catch (Exception e) {
                ultimoErro = e;
                System.out.println("[CLIENTE] Servidor não encontrado, tentando novamente em 1s... ("
                        + (tentativa + 1) + "/10)");
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("Não foi possível conectar ao servidor.", ultimoErro);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -Dtest=GraphicUITest test`
Expected: PASS (2 testes).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/client/ui/GraphicUI.java src/main/java/client/JogadorUI.java src/main/java/client/ConsoleUI.java src/main/java/client/ClientMain.java src/test/java/client/ui/GraphicUITest.java
git commit -m "feat: GraphicUI e integração no ClientMain (--gui e --modo manual|auto)"
```

---

### Task 7: Build completo e todos os testes verdes

**Files:** nenhum novo.

- [ ] **Step 1: Rodar a suíte inteira**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q test`
Expected: BUILD SUCCESS — todos os testes passam, incluindo:
- `TabuleiroTest`, `PartidaTest` (engine)
- `SemSocketTest` (garantia de ausência de Socket)
- `E2ETest` (partida completa com 4 bots em processos separados)
- `GeometriaTest`, `TabuleiroPanelTest`, `PainelJogadoresTest`, `BarraStatusTest`, `GraphicUITest`

Se `E2ETest` falhar com OOM (`errno=1455`), é pressão de memória da máquina — tentar de novo com `-DargLine="-Xmx256m"`.

- [ ] **Step 2: Empacotar o jar**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -DskipTests package`
Expected: `target/quoridor-rmi-1.0.0.jar` criado.

- [ ] **Step 3: Verificar ausência de Socket**

Run: `mvn -q -Dtest=SemSocketTest test`
Expected: PASS — confirma que a GUI não introduziu `java.net.Socket`.

- [ ] **Step 4: Commit (se houver mudanças pendentes)**

```bash
git status
# se houver arquivos não commitados de ajustes, commitar
git add -A
git commit -m "chore: build e suíte completa verdes com a interface gráfica"
```

---

### Task 8: Demo automática com GUI + screenshot

**Files:**
- Create (temporário, em `target/`, já gitignorado): `target/grab/ScreenshotGrabber.java`

- [ ] **Step 1: Compilar o projeto**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 2: Subir o servidor em background**

Run:
```bash
java -Xmx64m -Xms16m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=16m -XX:TieredStopAtLevel=1 -cp target/classes server.ServerMain > server_demo_gui.log 2>&1 &
echo $! > server_demo_gui.pid
```

- [ ] **Step 3: Subir 4 clientes com GUI em modo automático**

Run (uma linha por cliente, cada um em janela própria):
```bash
java -Xmx64m -Xms16m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=16m -XX:TieredStopAtLevel=1 -cp target/classes client.ClientMain --gui --modo auto --nome Bot1 --host localhost --porta 1099 &
java -Xmx64m -Xms16m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=16m -XX:TieredStopAtLevel=1 -cp target/classes client.ClientMain --gui --modo auto --nome Bot2 --host localhost --porta 1099 &
java -Xmx64m -Xms16m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=16m -XX:TieredStopAtLevel=1 -cp target/classes client.ClientMain --gui --modo auto --nome Bot3 --host localhost --porta 1099 &
java -Xmx64m -Xms16m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m -XX:ReservedCodeCacheSize=16m -XX:TieredStopAtLevel=1 -cp target/classes client.ClientMain --gui --modo auto --nome Bot4 --host localhost --porta 1099 &
wait
```

- [ ] **Step 4: Capturar screenshot**

Com as 4 janelas visíveis na tela, criar o capturador temporário com a ferramenta Write em `target/grab/ScreenshotGrabber.java` (o diretório `target/` é gitignorado):

```java
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ScreenshotGrabber {
    public static void main(String[] args) throws Exception {
        Thread.sleep(1500);
        Rectangle tela = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage img = new Robot().createScreenCapture(tela);
        ImageIO.write(img, "png", new File("docs/img/quoridor-gui.png"));
    }
}
```

Depois compilar e rodar:

```bash
mkdir -p docs/img
javac -d target/grab target/grab/ScreenshotGrabber.java
java -cp target/grab ScreenshotGrabber
```

(Se o capturador falhar por restrição do ambiente, usar `Win+Shift+S` para salvar manualmente em `docs/img/quoridor-gui.png`.)

- [ ] **Step 5: Conferir o screenshot**

Run: `git status` — `docs/img/quoridor-gui.png` deve existir (fora de `target/`).
Verificar visualmente que o tabuleiro renderizou (peões coloridos, cercas, painel lateral, barra de status).

- [ ] **Step 6: Encerrar a demo e limpar**

```bash
kill "$(cat server_demo_gui.pid)" 2>/dev/null || true
rm -f server_demo_gui.log server_demo_gui.pid
```

- [ ] **Step 7: Commit do screenshot**

```bash
git add docs/img/quoridor-gui.png
git commit -m "docs: screenshot da interface gráfica em modo automático"
```

---

### Task 9: Documentação (README e relatório)

**Files:**
- Modify: `README.md`
- Modify: `docs/relatorio.md`

- [ ] **Step 1: Atualizar README.md**

Adicionar após a seção de execução uma seção **Interface Gráfica** (conteúdo entre as cercas):

````markdown
## Interface Gráfica (Swing)

Além da interface por caracteres, o cliente tem uma **interface gráfica** (Java Swing,
estilo "Moderno Plano", sem dependências externas). Ela oferece **dois modos**:

- **Modo Manual** — você joga clicando no tabuleiro (mover peão) e usando a barra
  para colocar cercas (Cerca H / Cerca V).
- **Modo Automático** — os 4 bots jogam sozinhos e a janela mostra a partida evoluindo
  (ideal para apresentar o jogo).

Lançar um cliente com interface gráfica:

```bash
# Modo Manual (um processo por jogador humano)
java -cp target/classes client.ClientMain --gui --modo manual --nome SeuNome

# Modo Automático (bot ilustrativo — rode 4 desses)
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1

# Se --modo não for informado, um diálogo pergunta qual modo usar
java -cp target/classes client.ClientMain --gui
```

Exemplo de demo completa em uma máquina: suba 1 servidor + 4 clientes com `--gui --modo auto`.

![Interface gráfica do Quoridor](docs/img/quoridor-gui.png)
````

- [ ] **Step 2: Atualizar docs/relatorio.md**

Adicionar uma seção **Interface Gráfica (Swing)** descrevendo:

- Tecnologia: Swing/AWT do JDK, zero dependências (mantém o jar único e o requisito de RMI puro).
- Arquitetura: `GraphicUI implements UI` (mesma interface do `ConsoleUI`); novo pacote `client.ui` com `TabuleiroPanel`, `PainelJogadores`, `BarraStatus`, `DialogoModo`, `EstiloUI`, `Geometria`.
- Reuso do engine no cliente: `Tabuleiro.aPartirDe(estado)` para destacar movimentos legais e validar cercas antes de enviar ao servidor (mesmo padrão do `BotJogador`).
- Dois modos: Manual (cliques) e Automático (bots ilustrativos), escolhidos por `--modo` ou diálogo inicial.
- Integração com RMI: os callbacks `aoIniciarJogo`/`aoAtualizarEstado`/`aoFinalizarJogo` chegam à `GraphicUI` via `ClientCallbackImpl` (inalterado); a GUI atualiza na EDT.
- Evidência: screenshot em `docs/img/quoridor-gui.png` e log da demo automática.

- [ ] **Step 3: Compilar e validar**

Run: `MAVEN_OPTS="-Xmx512m" mvn -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/relatorio.md
git commit -m "docs: documenta a interface gráfica (modos manual e automático)"
```

---

## Verificação final (contra a spec)

| Requisito da spec | Onde está coberto |
|---|---|
| `GraphicUI implements UI`, RMI intacto | Task 6 (`GraphicUI`, `JogadorUI`); `ClientCallbackImpl` e servidor inalterados |
| Dois modos com escolha | Task 5 (`DialogoModo`) + Task 6 (`--modo`, `modoAutomatico()`) |
| Layout da janela aprovado | Task 4 (tabuleiro) + Task 5 (painéis) + Task 6 (montagem `GraphicUI`) |
| Interação 100% clique | Task 3 (`clique`, `setModo`, preview) + Task 4 (pintura/preview) |
| Modo automático ilustrativo | Task 6 (loop do bot no `ClientMain`) + Task 8 (demo) |
| Tema Moderno Plano | Task 2 (`EstiloUI`) |
| Testes (unitários + sem socket + E2E) | Tasks 1, 3, 5, 6 (testes novos) + Task 7 (suíte completa) |
| Documentação | Task 9 (README + relatório + screenshot) |
| Jar único e `mvn clean package` | Task 7 (package) + Task 9 (validação) |
