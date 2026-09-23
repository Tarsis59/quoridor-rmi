package client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;

/** Tema visual "Noite" — paleta, fontes e medidas centralizadas. */
public final class EstiloUI {
    // Superfícies (escuro azulado, do fundo para a frente)
    public static final Color FUNDO_JANELA = new Color(0x0B1220);
    public static final Color SUPERFICIE = new Color(0x111A2E);
    public static final Color CARTAO = new Color(0x172239);
    public static final Color BORDA = new Color(0x26344F);

    // Tabuleiro
    public static final Color MOLDURA = new Color(0x0D1526);
    public static final Color CASA = new Color(0x223250);
    public static final Color CASA_BRILHO = new Color(0x2B3E63);
    public static final Color CASA_HOVER = new Color(0x36507E);
    public static final Color CERCA = new Color(0x94A3B8);

    // Texto e estados
    public static final Color TEXTO_PRINCIPAL = new Color(0xE6EDF7);
    public static final Color TEXTO_SECUNDARIO = new Color(0x8B9BB6);
    public static final Color DESTAQUE = new Color(0xF5C451);
    public static final Color ERRO = new Color(0xF87171);
    public static final Color VERDE_OK = new Color(0x34D399);
    public static final Color VERMELHO_ERRADO = new Color(0xF87171);
    /** Fundo do cartão do próprio jogador ("você"). */
    public static final Color VOCE_FUNDO = new Color(0x1B2B4A);
    public static final Color VEZ_FUNDO = new Color(0x2A2A1E);

    public static final Color[] COR_PEAO = {
            new Color(0xF05252), // J1 vermelho
            new Color(0x4F8EF7), // J2 azul
            new Color(0x2FCB7F), // J3 verde
            new Color(0xF5A524)  // J4 âmbar
    };
    public static final String[] NOME_COR = {"Vermelho", "Azul", "Verde", "Âmbar"};
    /** Meta de cada jogador, em texto curto. */
    public static final String[] META = {"chegar ao topo", "chegar à base", "chegar à esquerda", "chegar à direita"};

    public static final int CELULA = 52;
    /** Margem ao redor das casas: abriga coordenadas e as faixas coloridas das metas. */
    public static final int MARGEM = 30;
    public static final int LADO = 2 * MARGEM + 9 * CELULA;
    public static final int ESPESSURA_CERCA = 8;
    /** Metade da largura do sulco entre casas (onde as cercas encaixam). */
    public static final int SULCO = 3;

    private static final String FAMILIA = escolherFamilia();
    public static final Font FONTE_TITULO = new Font(FAMILIA, Font.BOLD, 18);
    public static final Font FONTE_NORMAL = new Font(FAMILIA, Font.PLAIN, 13);
    public static final Font FONTE_NEGRITO = new Font(FAMILIA, Font.BOLD, 13);
    public static final Font FONTE_PEAO = new Font(FAMILIA, Font.BOLD, 17);
    public static final Font FONTE_ROTULO = new Font(FAMILIA, Font.BOLD, 10);
    public static final Font FONTE_PEQUENA = new Font(FAMILIA, Font.PLAIN, 11);
    public static final Font FONTE_AVISO = new Font(FAMILIA, Font.BOLD, 26);

    private EstiloUI() {}

    /** Segoe UI no Windows; senão uma sans-serif moderna comum; por fim a lógica "Dialog". */
    private static String escolherFamilia() {
        try {
            String[] disponiveis = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String f : new String[]{"Segoe UI", "Inter", "Helvetica Neue", "Noto Sans", "DejaVu Sans"}) {
                if (Arrays.asList(disponiveis).contains(f)) return f;
            }
        } catch (RuntimeException | Error ignorada) {
            // ambiente sem fontes (headless mínimo): usa a fonte lógica
        }
        return Font.DIALOG;
    }

    public static Color comAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa)));
    }

    public static Color misturar(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static void suavizar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
