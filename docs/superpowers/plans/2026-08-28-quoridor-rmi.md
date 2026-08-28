# Quoridor Distribuído (RMI) — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o jogo Quoridor para 4 jogadores de forma distribuída com Java RMI puro, com servidor central (registry embutido na porta 1099), 4 clientes com callback, engine de regras 100% testada, e validação E2E de uma partida completa.

**Architecture:** Pacote `common` (interfaces remotas + modelos serializáveis), pacote `engine` (regras puras, sem RMI, testáveis com JUnit), pacote `server` (GameServerImpl + ServerMain com registry embutido), pacote `client` (ClientCallbackImpl + ConsoleUI + ClientMain + BotJogador). Chamadas de jogo são RMI síncronas; broadcast de estado via callback.

**Tech Stack:** Java 21 (Temurin), Maven 3.9.x, JUnit 5 (Jupiter), RMI puro (`java.rmi`). Zero dependências de runtime além da JDK.

**Ambiente:** Windows 11, projeto raiz = `C:\Users\User\Downloads\PROJETO JOGO SD`.

---

## Estrutura de arquivos (resultado final)

```
PROJETO JOGO SD/
├── pom.xml
├── .gitignore
├── PLANO.md                      (já criado)
├── docs/
│   ├── superpowers/specs/2026-08-28-quoridor-rmi-design.md   (já criado)
│   └── relatorio.md
├── src/main/java/
│   ├── common/
│   │   ├── Posicao.java
│   │   ├── Orientacao.java
│   │   ├── Cerca.java
│   │   ├── JogadaInvalidaException.java
│   │   ├── EstadoJogo.java
│   │   ├── GameServer.java
│   │   └── ClientCallback.java
│   ├── engine/
│   │   ├── Tabuleiro.java
│   │   └── Partida.java
│   ├── server/
│   │   ├── GameServerImpl.java
│   │   └── ServerMain.java
│   └── client/
│       ├── UI.java
│       ├── ClientCallbackImpl.java
│       ├── ConsoleUI.java
│       ├── BotJogador.java
│       └── ClientMain.java
└── src/test/java/
    ├── engine/
    │   ├── TabuleiroTest.java
    │   └── PartidaTest.java
    └── e2e/
        ├── SemSocketTest.java
        └── E2ETest.java
```

---

### Task 1: Inicializar projeto Maven, .gitignore e git

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: pastas `src/main/java/common`, `src/main/java/engine`, `src/main/java/server`, `src/main/java/client`, `src/test/java/engine`, `src/test/java/e2e`, `docs/`
- Git init no diretório raiz

- [ ] **Step 1: Criar `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>br.edu.sd</groupId>
  <artifactId>quoridor-rmi</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>
  <name>Quoridor Distribuído (RMI)</name>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.10.2</junit.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>${junit.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Criar `.gitignore`**

```
target/
*.class
*.log
.idea/
.vscode/
*.iml
.env
Captura*.png
captura_rgb.png
captura_jpeg.jpg
run-deepseek.bat
Start-Claude-DeepSeek.ps1
```

- [ ] **Step 3: Criar as pastas**

Run:
```
mkdir -p src/main/java/common src/main/java/engine src/main/java/server src/main/java/client src/test/java/engine src/test/java/e2e docs
```
(PowerShell: `New-Item -ItemType Directory -Force -Path src/main/java/common, src/main/java/engine, src/main/java/server, src/main/java/client, src/test/java/engine, src/test/java/e2e, docs`)

- [ ] **Step 4: Inicializar git e commit inicial**

```
git init
git add -A
git commit -m "chore: inicializa projeto Maven do Quoridor RMI"
```

---

### Task 2: common — Posicao, Orientacao, Cerca

**Files:**
- Create: `src/main/java/common/Posicao.java`
- Create: `src/main/java/common/Orientacao.java`
- Create: `src/main/java/common/Cerca.java`

- [ ] **Step 1: Criar `Posicao.java`**

```java
package common;

import java.io.Serializable;

public record Posicao(int linha, int coluna) implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 2: Criar `Orientacao.java`**

```java
package common;

import java.io.Serializable;

public enum Orientacao implements Serializable {
    HORIZONTAL, VERTICAL
}
```

- [ ] **Step 3: Criar `Cerca.java`**

```java
package common;

import java.io.Serializable;

public record Cerca(Posicao base, Orientacao orientacao) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Cerca {
        if (base == null || orientacao == null) {
            throw new IllegalArgumentException("Cerca exige base e orientação.");
        }
    }
}
```

- [ ] **Step 4: Compilar para validar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS (sem erros).

---

### Task 3: common — JogadaInvalidaException e EstadoJogo

**Files:**
- Create: `src/main/java/common/JogadaInvalidaException.java`
- Create: `src/main/java/common/EstadoJogo.java`

- [ ] **Step 1: Criar `JogadaInvalidaException.java`**

```java
package common;

import java.io.Serializable;

public class JogadaInvalidaException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;

    public JogadaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
```

- [ ] **Step 2: Criar `EstadoJogo.java`**

```java
package common;

import java.io.Serializable;
import java.util.List;

public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { AGUARDANDO, EM_ANDAMENTO, FINALIZADO }

    private final Posicao[] posicoes;
    private final List<Cerca> cercas;
    private final int[] cercasRestantes;
    private final int jogadorDaVez;
    private final Status status;
    private final int vencedor;
    private final String[] nomes;

    public EstadoJogo(Posicao[] posicoes, List<Cerca> cercas, int[] cercasRestantes,
                      int jogadorDaVez, Status status, int vencedor, String[] nomes) {
        this.posicoes = posicoes.clone();
        this.cercas = List.copyOf(cercas);
        this.cercasRestantes = cercasRestantes.clone();
        this.jogadorDaVez = jogadorDaVez;
        this.status = status;
        this.vencedor = vencedor;
        this.nomes = nomes.clone();
    }

    public Posicao getPosicao(int idJogador) { return posicoes[idJogador - 1]; }
    public Posicao[] getPosicoes() { return posicoes.clone(); }
    public List<Cerca> getCercas() { return cercas; }
    public int[] getCercasRestantes() { return cercasRestantes.clone(); }
    public int getCercasRestantes(int idJogador) { return cercasRestantes[idJogador - 1]; }
    public int getJogadorDaVez() { return jogadorDaVez; }
    public Status getStatus() { return status; }
    public int getVencedor() { return vencedor; }
    public String getNome(int idJogador) { return nomes[idJogador - 1]; }
    public String[] getNomes() { return nomes.clone(); }
}
```

- [ ] **Step 3: Compilar para validar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 4: common — GameServer e ClientCallback

**Files:**
- Create: `src/main/java/common/GameServer.java`
- Create: `src/main/java/common/ClientCallback.java`

- [ ] **Step 1: Criar `GameServer.java`**

```java
package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServer extends Remote {
    String NOME_SERVICO = "QuoridorServer";

    int registrar(ClientCallback callback, String nomeJogador) throws RemoteException;
    void mover(int idJogador, Posicao destino) throws RemoteException, JogadaInvalidaException;
    void colocarCerca(int idJogador, Cerca cerca) throws RemoteException, JogadaInvalidaException;
    EstadoJogo obterEstado() throws RemoteException;
}
```

- [ ] **Step 2: Criar `ClientCallback.java`**

```java
package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void aoIniciarJogo(EstadoJogo estado) throws RemoteException;
    void aoAtualizarEstado(EstadoJogo estado) throws RemoteException;
    void aoFinalizarJogo(int idVencedor) throws RemoteException;
}
```

- [ ] **Step 3: Compilar para validar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 5: engine — Tabuleiro (movimento, pulo, cercas, BFS)

**Files:**
- Create: `src/main/java/engine/Tabuleiro.java`

Este é o arquivo mais importante (regras do jogo). Código completo:

```java
package engine;

import common.Cerca;
import common.Orientacao;
import common.Posicao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Tabuleiro {
    public static final int TAMANHO = 9;
    public static final int NUM_JOGADORES = 4;

    private static final int[][] DIRECOES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final boolean[][] paredesH; // [8][9]: aresta horizontal entre (r,c) e (r+1,c)
    private final boolean[][] paredesV; // [9][8]: aresta vertical entre (r,c) e (r,c+1)
    private final Posicao[] posicoes;

    public Tabuleiro() {
        paredesH = new boolean[8][9];
        paredesV = new boolean[9][8];
        posicoes = new Posicao[NUM_JOGADORES];
        posicoes[0] = new Posicao(8, 4); // J1 embaixo -> meta linha 0
        posicoes[1] = new Posicao(0, 4); // J2 topo   -> meta linha 8
        posicoes[2] = new Posicao(4, 8); // J3 direita-> meta coluna 0
        posicoes[3] = new Posicao(4, 0); // J4 esquerda-> meta coluna 8
    }

    private Tabuleiro(Tabuleiro outro) {
        paredesH = new boolean[8][9];
        paredesV = new boolean[9][8];
        for (int r = 0; r < 8; r++) System.arraycopy(outro.paredesH[r], 0, paredesH[r], 0, 9);
        for (int r = 0; r < 9; r++) System.arraycopy(outro.paredesV[r], 0, paredesV[r], 0, 8);
        posicoes = new Posicao[NUM_JOGADORES];
        System.arraycopy(outro.posicoes, 0, posicoes, 0, NUM_JOGADORES);
    }

    public Tabuleiro copiar() { return new Tabuleiro(this); }

    public static Tabuleiro aPartirDe(Posicao[] posicoes, List<Cerca> cercas) {
        Tabuleiro t = new Tabuleiro();
        for (int i = 0; i < NUM_JOGADORES; i++) t.posicoes[i] = posicoes[i];
        for (Cerca c : cercas) {
            int r = c.base().linha();
            int col = c.base().coluna();
            if (c.orientacao() == Orientacao.HORIZONTAL) {
                t.paredesH[r][col] = true;
                t.paredesH[r][col + 1] = true;
            } else {
                t.paredesV[r][col] = true;
                t.paredesV[r + 1][col] = true;
            }
        }
        return t;
    }

    public boolean dentro(int linha, int coluna) {
        return linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO;
    }

    public Posicao getPosicao(int idJogador) { return posicoes[idJogador - 1]; }

    public boolean estaOcupada(Posicao p) {
        for (Posicao q : posicoes) {
            if (q.linha() == p.linha() && q.coluna() == p.coluna()) return true;
        }
        return false;
    }

    public boolean temParedeEntre(Posicao a, Posicao b) {
        int dr = b.linha() - a.linha();
        int dc = b.coluna() - a.coluna();
        if (dr == -1 && dc == 0) return paredesH[b.linha()][a.coluna()];
        if (dr == 1 && dc == 0) return paredesH[a.linha()][a.coluna()];
        if (dr == 0 && dc == -1) return paredesV[a.linha()][b.coluna()];
        if (dr == 0 && dc == 1) return paredesV[a.linha()][a.coluna()];
        return false;
    }

    public List<Posicao> movimentosValidos(int idJogador) {
        Posicao atual = getPosicao(idJogador);
        List<Posicao> resultado = new ArrayList<>();
        for (int[] d : DIRECOES) {
            Posicao vizinho = new Posicao(atual.linha() + d[0], atual.coluna() + d[1]);
            if (!dentro(vizinho.linha(), vizinho.coluna())) continue;
            if (temParedeEntre(atual, vizinho)) continue;
            if (!estaOcupada(vizinho)) {
                resultado.add(vizinho);
                continue;
            }
            Posicao depois = new Posicao(vizinho.linha() + d[0], vizinho.coluna() + d[1]);
            boolean depoisLivre = dentro(depois.linha(), depois.coluna())
                    && !temParedeEntre(vizinho, depois) && !estaOcupada(depois);
            if (depoisLivre) {
                resultado.add(depois);
            } else {
                Posicao lateral1 = new Posicao(vizinho.linha() + d[1], vizinho.coluna() + d[0]);
                if (dentro(lateral1.linha(), lateral1.coluna())
                        && !temParedeEntre(vizinho, lateral1) && !estaOcupada(lateral1)) {
                    resultado.add(lateral1);
                }
                Posicao lateral2 = new Posicao(vizinho.linha() - d[1], vizinho.coluna() - d[0]);
                if (dentro(lateral2.linha(), lateral2.coluna())
                        && !temParedeEntre(vizinho, lateral2) && !estaOcupada(lateral2)) {
                    resultado.add(lateral2);
                }
            }
        }
        return resultado;
    }

    public boolean podeColocarCerca(Cerca cerca) {
        Posicao base = cerca.base();
        int r = base.linha();
        int c = base.coluna();
        if (r < 0 || r > 7 || c < 0 || c > 7) return false;
        Tabuleiro sim = copiar();
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            if (paredesH[r][c] || paredesH[r][c + 1]) return false;
            if (paredesV[r][c] && paredesV[r + 1][c]) return false;
            sim.paredesH[r][c] = true;
            sim.paredesH[r][c + 1] = true;
        } else {
            if (paredesV[r][c] || paredesV[r + 1][c]) return false;
            if (paredesH[r][c] && paredesH[r][c + 1]) return false;
            sim.paredesV[r][c] = true;
            sim.paredesV[r + 1][c] = true;
        }
        return sim.todosTemCaminho();
    }

    public void moverPeao(int idJogador, Posicao destino) {
        posicoes[idJogador - 1] = destino;
    }

    public void colocarCerca(Cerca cerca) {
        int r = cerca.base().linha();
        int c = cerca.base().coluna();
        if (cerca.orientacao() == Orientacao.HORIZONTAL) {
            paredesH[r][c] = true;
            paredesH[r][c + 1] = true;
        } else {
            paredesV[r][c] = true;
            paredesV[r + 1][c] = true;
        }
    }

    public boolean temCaminho(int idJogador) {
        return temCaminho(idJogador, getPosicao(idJogador));
    }

    public boolean temCaminho(int idJogador, Posicao origem) {
        Queue<Posicao> fila = new LinkedList<>();
        Set<Posicao> visitados = new HashSet<>();
        fila.add(origem);
        visitados.add(origem);
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            if (chegouAoDestino(idJogador, atual)) return true;
            for (Posicao vizinho : vizinhosAlcancaveis(atual)) {
                if (visitados.add(vizinho)) fila.add(vizinho);
            }
        }
        return false;
    }

    public boolean todosTemCaminho() {
        for (int id = 1; id <= NUM_JOGADORES; id++) {
            if (!temCaminho(id)) return false;
        }
        return true;
    }

    public int distanciaMinimaAteAlvo(int idJogador) {
        return distanciaMinimaAteAlvo(idJogador, getPosicao(idJogador));
    }

    public int distanciaMinimaAteAlvo(int idJogador, Posicao origem) {
        Queue<Posicao> fila = new LinkedList<>();
        Map<Posicao, Integer> dist = new HashMap<>();
        fila.add(origem);
        dist.put(origem, 0);
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            if (chegouAoDestino(idJogador, atual)) return dist.get(atual);
            for (Posicao vizinho : vizinhosAlcancaveis(atual)) {
                if (!dist.containsKey(vizinho)) {
                    dist.put(vizinho, dist.get(atual) + 1);
                    fila.add(vizinho);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    public boolean chegouAoDestino(int idJogador) {
        return chegouAoDestino(idJogador, getPosicao(idJogador));
    }

    private boolean chegouAoDestino(int idJogador, Posicao p) {
        return switch (idJogador) {
            case 1 -> p.linha() == 0;
            case 2 -> p.linha() == 8;
            case 3 -> p.coluna() == 0;
            case 4 -> p.coluna() == 8;
            default -> false;
        };
    }

    private List<Posicao> vizinhosAlcancaveis(Posicao p) {
        List<Posicao> vizinhos = new ArrayList<>();
        for (int[] d : DIRECOES) {
            Posicao q = new Posicao(p.linha() + d[0], p.coluna() + d[1]);
            if (!dentro(q.linha(), q.coluna())) continue;
            if (temParedeEntre(p, q)) continue;
            if (estaOcupada(q)) continue;
            vizinhos.add(q);
        }
        return vizinhos;
    }
}
```

- [ ] **Step 1: Criar o arquivo com o conteúdo acima**
- [ ] **Step 2: Compilar para validar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 6: Tests da engine — TabuleiroTest (movimento e pulo)

**Files:**
- Create: `src/test/java/engine/TabuleiroTest.java`

- [ ] **Step 1: Criar o teste (failing primeiro)**

```java
package engine;

import common.Cerca;
import common.Orientacao;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuleiroTest {

    @Test
    void movimentoSimplesNasQuatroDirecoes() {
        Tabuleiro t = new Tabuleiro();
        List<Posicao> validos = t.movimentosValidos(1); // J1 em (8,4)
        assertTrue(validos.contains(new Posicao(7, 4)), "deve poder subir");
        assertTrue(validos.contains(new Posicao(8, 3)), "deve poder ir para a esquerda");
        assertTrue(validos.contains(new Posicao(8, 5)), "deve poder ir para a direita");
        assertFalse(validos.contains(new Posicao(9, 4)), "não pode sair do tabuleiro");
    }

    @Test
    void bordaDoTabuleiroNaoPermiteSair() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(0, 0));
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(1, 0)));
        assertTrue(validos.contains(new Posicao(0, 1)));
        assertFalse(validos.contains(new Posicao(-1, 0)));
        assertFalse(validos.contains(new Posicao(0, -1)));
    }

    @Test
    void paredeBloqueiaMovimento() {
        Tabuleiro t = new Tabuleiro();
        assertTrue(t.podeColocarCerca(new Cerca(new Posicao(7, 4), Orientacao.HORIZONTAL)));
        t.colocarCerca(new Cerca(new Posicao(7, 4), Orientacao.HORIZONTAL));
        List<Posicao> validos = t.movimentosValidos(1); // J1 em (8,4)
        assertFalse(validos.contains(new Posicao(7, 4)), "parede H(7,4) bloqueia subida do J1");
        assertTrue(validos.contains(new Posicao(8, 3)));
        assertTrue(validos.contains(new Posicao(8, 5)));
    }

    @Test
    void puloRetoSobrePeao() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(5, 4));
        t.moverPeao(2, new Posicao(4, 4));
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(3, 4)), "deve pular direto para trás do adversário");
    }

    @Test
    void puloLateralQuandoAtrasBloqueadoPorParede() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(5, 4));
        t.moverPeao(2, new Posicao(4, 4));
        assertTrue(t.podeColocarCerca(new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL)));
        t.colocarCerca(new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL)); // bloqueia casa atrás
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(4, 3)), "pulo lateral esquerdo");
        assertTrue(validos.contains(new Posicao(4, 5)), "pulo lateral direito");
        assertFalse(validos.contains(new Posicao(3, 4)), "pulo reto bloqueado");
    }

    @Test
    void puloBloqueadoPorParedesDosDoisLados() {
        Posicao[] posicoes = {
            new Posicao(5, 4), // J1
            new Posicao(4, 4), // J2
            new Posicao(4, 8), // J3
            new Posicao(4, 0)  // J4
        };
        // Cercas montadas como cenário (aPartirDe), não via podeColocarCerca:
        // bloquear as 3 opções de pulo do J1 isolaria o J2, e a garantia de caminho
        // (Task 7) corretamente rejeitaria a colocação da última cerca.
        Tabuleiro t = Tabuleiro.aPartirDe(posicoes, List.of(
            new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL),
            new Cerca(new Posicao(4, 3), Orientacao.VERTICAL),
            new Cerca(new Posicao(4, 4), Orientacao.VERTICAL)
        ));
        List<Posicao> validos = t.movimentosValidos(1);
        assertFalse(validos.contains(new Posicao(4, 3)), "lateral esquerda bloqueada");
        assertFalse(validos.contains(new Posicao(4, 5)), "lateral direita bloqueada");
        assertFalse(validos.contains(new Posicao(3, 4)), "pulo reto bloqueado");
        assertFalse(validos.contains(new Posicao(4, 4)), "casa do adversário não é destino");
    }

    @Test
    void vitoriaPorLadoParaOsQuatroJogadores() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(0, 3)); assertTrue(t.chegouAoDestino(1));
        t.moverPeao(2, new Posicao(8, 5)); assertTrue(t.chegouAoDestino(2));
        t.moverPeao(3, new Posicao(0, 0)); assertTrue(t.chegouAoDestino(3));
        t.moverPeao(4, new Posicao(0, 8)); assertTrue(t.chegouAoDestino(4));
    }
}
```

- [ ] **Step 2: Rodar o teste e verificar que passa**

Run: `mvn -q test -Dtest=TabuleiroTest`
Expected: PASS (todas as assertivas verdes).

---

### Task 7: Tests da engine — TabuleiroTest (cercas e caminho garantido)

Adicionar ao mesmo arquivo `TabuleiroTest.java` (após a última chave, dentro da classe):

```java
    @Test
    void cercaSobrepostaDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        Cerca c1 = new Cerca(new Posicao(4, 4), Orientacao.HORIZONTAL);
        assertTrue(t.podeColocarCerca(c1));
        t.colocarCerca(c1);
        assertFalse(t.podeColocarCerca(c1), "mesma cerca de novo");
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(4, 3), Orientacao.HORIZONTAL)),
                "sobrepõe o segmento H(4,4)");
    }

    @Test
    void cercaQueCruzaOutraDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        Cerca h = new Cerca(new Posicao(4, 4), Orientacao.HORIZONTAL);
        assertTrue(t.podeColocarCerca(h));
        t.colocarCerca(h);
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(4, 4), Orientacao.VERTICAL)),
                "horizontal e vertical na mesma base cruzam");
    }

    @Test
    void cercaForaDoTabuleiroDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(8, 0), Orientacao.HORIZONTAL)));
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(0, 8), Orientacao.VERTICAL)));
    }

    @Test
    void cercaQueFechariaOPocketDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(4, 4));
        Cerca cima = new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL);   // fecha saída superior
        Cerca esquerda = new Cerca(new Posicao(4, 3), Orientacao.VERTICAL); // fecha saída esquerda
        Cerca direita = new Cerca(new Posicao(4, 5), Orientacao.VERTICAL);  // fecha saída direita
        Cerca baixo = new Cerca(new Posicao(5, 4), Orientacao.HORIZONTAL);  // fecharia o pocket por baixo
        assertTrue(t.podeColocarCerca(cima)); t.colocarCerca(cima);
        assertTrue(t.podeColocarCerca(esquerda)); t.colocarCerca(esquerda);
        assertTrue(t.podeColocarCerca(direita)); t.colocarCerca(direita);
        assertTrue(t.temCaminho(1), "J1 ainda escapa por baixo");
        assertFalse(t.podeColocarCerca(baixo), "fecharia o pocket e isolaria o J1 (último caminho)");
    }

    @Test
    void cercaValidaPreservaCaminhoDeTodos() {
        Tabuleiro t = new Tabuleiro();
        Cerca c = new Cerca(new Posicao(4, 4), Orientacao.VERTICAL);
        assertTrue(t.podeColocarCerca(c));
        t.colocarCerca(c);
        for (int id = 1; id <= 4; id++) {
            assertTrue(t.temCaminho(id), "jogador " + id + " deve ter caminho após cerca válida");
        }
    }
```

- [ ] **Step 1: Adicionar os métodos acima à classe `TabuleiroTest`**
- [ ] **Step 2: Rodar e verificar que passa**

Run: `mvn -q test -Dtest=TabuleiroTest`
Expected: PASS (10 testes verdes).

---

### Task 8: engine — Partida (turnos, vitória, estado)

**Files:**
- Create: `src/main/java/engine/Partida.java`

```java
package engine;

import common.Cerca;
import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;

import java.util.ArrayList;
import java.util.List;

public class Partida {
    public static final int NUM_JOGADORES = 4;
    public static final int CERCAS_INICIAIS = 10;

    private final Tabuleiro tabuleiro = new Tabuleiro();
    private final String[] nomes = new String[NUM_JOGADORES];
    private final int[] cercasRestantes = new int[NUM_JOGADORES];
    private final List<Cerca> cercasColocadas = new ArrayList<>();
    private int jogadorDaVez = 1;
    private EstadoJogo.Status status = EstadoJogo.Status.AGUARDANDO;
    private int vencedor = 0;

    public Partida() {
        for (int i = 0; i < NUM_JOGADORES; i++) {
            nomes[i] = "Jogador " + (i + 1);
            cercasRestantes[i] = CERCAS_INICIAIS;
        }
    }

    public synchronized void setNome(int idJogador, String nome) {
        nomes[idJogador - 1] = nome;
    }

    public synchronized void iniciar() {
        status = EstadoJogo.Status.EM_ANDAMENTO;
    }

    public synchronized void mover(int idJogador, Posicao destino) throws JogadaInvalidaException {
        if (status != EstadoJogo.Status.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("O jogo ainda não começou ou já terminou.");
        }
        if (idJogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Não é a vez do jogador " + idJogador + ".");
        }
        boolean valido = tabuleiro.movimentosValidos(idJogador).stream().anyMatch(destino::equals);
        if (!valido) {
            throw new JogadaInvalidaException("Movimento inválido para " + destino + ".");
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
        if (status != EstadoJogo.Status.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("O jogo ainda não começou ou já terminou.");
        }
        if (idJogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Não é a vez do jogador " + idJogador + ".");
        }
        if (cercasRestantes[idJogador - 1] <= 0) {
            throw new JogadaInvalidaException("Jogador " + idJogador + " não possui mais cercas.");
        }
        if (!tabuleiro.podeColocarCerca(cerca)) {
            throw new JogadaInvalidaException("Cerca inválida: sobreposição, cruzamento ou bloqueio total de caminho.");
        }
        tabuleiro.colocarCerca(cerca);
        cercasColocadas.add(cerca);
        cercasRestantes[idJogador - 1]--;
        avancarTurno();
    }

    public synchronized void avancarTurno() {
        jogadorDaVez = jogadorDaVez % NUM_JOGADORES + 1;
    }

    public synchronized EstadoJogo gerarEstado() {
        Posicao[] pos = new Posicao[NUM_JOGADORES];
        for (int i = 0; i < NUM_JOGADORES; i++) pos[i] = tabuleiro.getPosicao(i + 1);
        return new EstadoJogo(pos, new ArrayList<>(cercasColocadas), cercasRestantes.clone(),
                jogadorDaVez, status, vencedor, nomes.clone());
    }

    public synchronized EstadoJogo.Status getStatus() { return status; }
    public synchronized int getJogadorDaVez() { return jogadorDaVez; }
    public synchronized int getVencedor() { return vencedor; }
}
```

- [ ] **Step 1: Criar o arquivo**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 9: Tests da engine — PartidaTest (turnos e vitória)

**Files:**
- Create: `src/test/java/engine/PartidaTest.java`

```java
package engine;

import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidaTest {

    private void moverOutrosParaOndeDa(Partida p) throws Exception {
        for (int outro = 2; outro <= 4; outro++) {
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            List<Posicao> validos = t.movimentosValidos(outro);
            assertTrue(!validos.isEmpty(), "Jogador " + outro + " deveria ter movimento");
            int id = outro; // effectively final p/ uso no lambda
            Posicao escolha = validos.stream()
                    .max(Comparator.comparingInt(pos -> t.distanciaMinimaAteAlvo(id, pos)))
                    .orElseThrow();
            p.mover(outro, escolha);
        }
    }

    @Test
    void jogadorForaDaVezDeveSerRejeitado() {
        Partida p = new Partida();
        p.iniciar();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(2, new Posicao(1, 4)));
        assertThrows(JogadaInvalidaException.class, () -> p.colocarCerca(3, new common.Cerca(
                new Posicao(4, 4), common.Orientacao.HORIZONTAL)));
    }

    @Test
    void turnosAvancamEmCiclo() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        for (int i = 0; i < 4; i++) {
            int vez = p.getJogadorDaVez();
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            p.mover(vez, t.movimentosValidos(vez).get(0));
        }
        assertEquals(1, p.getJogadorDaVez());
    }

    @Test
    void jogadorQueAlcancaAValaLinhaVence() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        int rodadas = 0;
        while (p.getStatus() != EstadoJogo.Status.FINALIZADO && rodadas < 30) {
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            Posicao melhor = t.movimentosValidos(1).stream()
                    .min(Comparator.comparingInt(Posicao::linha))
                    .orElseThrow(() -> new AssertionError("J1 sem movimentos"));
            p.mover(1, melhor);
            if (p.getStatus() == EstadoJogo.Status.FINALIZADO) break;
            moverOutrosParaOndeDa(p);
            rodadas++;
        }
        assertEquals(EstadoJogo.Status.FINALIZADO, p.getStatus(), "J1 deveria vencer ao alcançar a linha 0");
        assertEquals(1, p.getVencedor());
        assertThrows(JogadaInvalidaException.class, () -> p.mover(2, new Posicao(8, 4)),
                "jogada após o fim deve ser rejeitada");
    }

    @Test
    void movimentoInvalidoDeveSerRejeitado() {
        Partida p = new Partida();
        p.iniciar();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, new Posicao(8, 4)), "mesma casa");
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, new Posicao(9, 4)), "fora do tabuleiro");
    }

    @Test
    void estadoInicialTemQuatroJogadoresComDezCercas() {
        Partida p = new Partida();
        EstadoJogo e = p.gerarEstado();
        assertEquals(EstadoJogo.Status.AGUARDANDO, e.getStatus());
        for (int i = 1; i <= 4; i++) {
            assertEquals(10, e.getCercasRestantes(i));
        }
        assertEquals(new Posicao(8, 4), e.getPosicao(1));
        assertEquals(new Posicao(0, 4), e.getPosicao(2));
        assertEquals(new Posicao(4, 8), e.getPosicao(3));
        assertEquals(new Posicao(4, 0), e.getPosicao(4));
    }
}
```

> **Atenção:** o teste `turnosAvancamEmCiclo` acima tem código propositalmente feio. Substitua o corpo do laço por uma versão limpa:

```java
    @Test
    void turnosAvancamEmCiclo() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        for (int i = 0; i < 4; i++) {
            int vez = p.getJogadorDaVez();
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            p.mover(vez, t.movimentosValidos(vez).get(0));
        }
        assertEquals(1, p.getJogadorDaVez());
    }
```

- [ ] **Step 1: Criar `PartidaTest.java` com a versão limpa de `turnosAvancamEmCiclo`**
- [ ] **Step 2: Rodar os testes da engine**

Run: `mvn -q test -Dtest='TabuleiroTest,PartidaTest'`
Expected: PASS (todos os testes da engine verdes).

---

### Task 10: server — GameServerImpl

**Files:**
- Create: `src/main/java/server/GameServerImpl.java`

```java
package server;

import common.Cerca;
import common.ClientCallback;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Posicao;
import engine.Partida;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServerImpl extends UnicastRemoteObject implements GameServer {
    private static final long serialVersionUID = 1L;
    public static final int MAX_JOGADORES = 4;

    private final Partida partida = new Partida();
    private final Map<Integer, ClientCallback> callbacks = new ConcurrentHashMap<>();
    private final Map<Integer, String> nomes = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> conectados = new ConcurrentHashMap<>();

    public GameServerImpl() throws RemoteException {
        super();
        Thread vigia = new Thread(this::vigiarDesconexoes);
        vigia.setDaemon(true);
        vigia.start();
    }

    @Override
    public synchronized int registrar(ClientCallback callback, String nomeJogador) throws RemoteException {
        if (callbacks.size() >= MAX_JOGADORES) {
            throw new RemoteException("Limite de " + MAX_JOGADORES + " jogadores atingido.");
        }
        int id = callbacks.size() + 1;
        callbacks.put(id, callback);
        nomes.put(id, nomeJogador);
        conectados.put(id, true);
        partida.setNome(id, nomeJogador);
        System.out.println("[SERVIDOR] Jogador " + id + " (" + nomeJogador + ") registrado. "
                + callbacks.size() + "/" + MAX_JOGADORES);
        if (callbacks.size() == MAX_JOGADORES) {
            partida.iniciar();
            System.out.println("[SERVIDOR] Todos os jogadores conectados. A partida começou!");
            EstadoJogo estado = partida.gerarEstado();
            broadcast(cb -> cb.aoIniciarJogo(estado));
        }
        return id;
    }

    @Override
    public synchronized void mover(int idJogador, Posicao destino)
            throws RemoteException, JogadaInvalidaException {
        partida.mover(idJogador, destino);
        System.out.println("[SERVIDOR] Jogador " + idJogador + " moveu para " + destino);
        aposJogada();
    }

    @Override
    public synchronized void colocarCerca(int idJogador, Cerca cerca)
            throws RemoteException, JogadaInvalidaException {
        partida.colocarCerca(idJogador, cerca);
        System.out.println("[SERVIDOR] Jogador " + idJogador + " colocou cerca " + cerca);
        aposJogada();
    }

    @Override
    public EstadoJogo obterEstado() throws RemoteException {
        return partida.gerarEstado();
    }

    private void aposJogada() {
        EstadoJogo estado = partida.gerarEstado();
        if (estado.getStatus() == EstadoJogo.Status.FINALIZADO) {
            System.out.println("[FIM] vencedor=" + estado.getVencedor()
                    + " nome=" + estado.getNome(estado.getVencedor()));
            broadcast(cb -> cb.aoFinalizarJogo(estado.getVencedor()));
            broadcast(cb -> cb.aoAtualizarEstado(estado));
        } else {
            broadcast(cb -> cb.aoAtualizarEstado(estado));
        }
    }

    private void vigiarDesconexoes() {
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            synchronized (this) {
                if (partida.getStatus() == EstadoJogo.Status.EM_ANDAMENTO
                        && !Boolean.TRUE.equals(conectados.get(partida.getJogadorDaVez()))) {
                    System.out.println("[SERVIDOR] Jogador " + partida.getJogadorDaVez()
                            + " está desconectado. Avançando o turno.");
                    partida.avancarTurno();
                    broadcast(cb -> cb.aoAtualizarEstado(partida.gerarEstado()));
                }
            }
        }
    }

    private interface OperacaoCallback {
        void executar(ClientCallback cb) throws RemoteException;
    }

    private void broadcast(OperacaoCallback operacao) {
        for (Map.Entry<Integer, ClientCallback> entrada : callbacks.entrySet()) {
            try {
                operacao.executar(entrada.getValue());
            } catch (RemoteException e) {
                conectados.put(entrada.getKey(), false);
                System.out.println("[SERVIDOR] Jogador " + entrada.getKey()
                        + " desconectado (RemoteException): " + e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 1: Criar o arquivo**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 11: server — ServerMain (registry embutido)

**Files:**
- Create: `src/main/java/server/ServerMain.java`

```java
package server;

import common.GameServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        int porta = 1099;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--porta".equals(args[i])) porta = Integer.parseInt(args[i + 1]);
        }

        Registry registry = LocateRegistry.createRegistry(porta);
        GameServer servidor = new GameServerImpl();
        registry.rebind(GameServer.NOME_SERVICO, servidor);

        System.out.println("[SERVIDOR] Registry RMI criado na porta " + porta);
        System.out.println("[SERVIDOR] Serviço '" + GameServer.NOME_SERVICO
                + "' disponível em rmi://localhost:" + porta + "/" + GameServer.NOME_SERVICO);
        System.out.println("[SERVIDOR] Aguardando " + GameServerImpl.MAX_JOGADORES + " jogadores...");

        synchronized (ServerMain.class) {
            ServerMain.class.wait();
        }
    }
}
```

- [ ] **Step 1: Criar o arquivo**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 12: client — UI, ClientCallbackImpl, ConsoleUI

**Files:**
- Create: `src/main/java/client/UI.java`
- Create: `src/main/java/client/ClientCallbackImpl.java`
- Create: `src/main/java/client/ConsoleUI.java`

- [ ] **Step 1: Criar `UI.java`**

```java
package client;

import common.EstadoJogo;

public interface UI {
    void novoEstado(EstadoJogo estado);
    void finalizar(int idVencedor);
}
```

- [ ] **Step 2: Criar `ClientCallbackImpl.java`**

```java
package client;

import common.ClientCallback;
import common.EstadoJogo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private static final long serialVersionUID = 1L;

    private final UI ui;

    public ClientCallbackImpl(UI ui) throws RemoteException {
        super();
        this.ui = ui;
    }

    @Override
    public void aoIniciarJogo(EstadoJogo estado) throws RemoteException {
        ui.novoEstado(estado);
    }

    @Override
    public void aoAtualizarEstado(EstadoJogo estado) throws RemoteException {
        ui.novoEstado(estado);
    }

    @Override
    public void aoFinalizarJogo(int idVencedor) throws RemoteException {
        ui.finalizar(idVencedor);
    }
}
```

- [ ] **Step 3: Criar `ConsoleUI.java`**

```java
package client;

import common.Cerca;
import common.EstadoJogo;
import common.GameServer;
import common.JogadaInvalidaException;
import common.Orientacao;
import common.Posicao;

import java.rmi.RemoteException;
import java.util.Scanner;

public class ConsoleUI implements UI {
    private volatile EstadoJogo estadoAtual;
    private int meuId = 0;

    @Override
    public synchronized void novoEstado(EstadoJogo estado) {
        this.estadoAtual = estado;
        imprimirEstado(estado);
    }

    @Override
    public synchronized void finalizar(int idVencedor) {
        System.out.println("=== FIM DE JOGO ===");
        EstadoJogo e = estadoAtual;
        System.out.println("O vencedor é o Jogador " + idVencedor
                + " (" + (e == null ? "Jogador " + idVencedor : e.getNome(idVencedor)) + ").");
    }

    public synchronized EstadoJogo getEstadoAtual() { return estadoAtual; }

    public void setMeuId(int id) { this.meuId = id; }

    public void loop(GameServer server) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao Quoridor Distribuído! Você é o Jogador " + meuId + ".");
        imprimirAjuda();
        while (true) {
            EstadoJogo e = estadoAtual;
            if (e == null) {
                System.out.println("[CLIENTE] Aguardando o início da partida (precisa de 4 jogadores)...");
                dormir(500);
                continue;
            }
            if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
                System.out.println("=== FIM DE JOGO ===");
                System.out.println("O vencedor é o Jogador " + e.getVencedor()
                        + " (" + e.getNome(e.getVencedor()) + ").");
                break;
            }
            imprimirEstado(e);
            if (e.getJogadorDaVez() != meuId) {
                System.out.println("[CLIENTE] Vez do Jogador " + e.getJogadorDaVez() + ". Aguardando...");
                dormir(400);
                continue;
            }
            System.out.print("Sua vez (Jogador " + meuId + "). Comando: ");
            if (!scanner.hasNextLine()) break;
            String linha = scanner.nextLine().trim().toLowerCase();
            if (linha.isEmpty()) continue;
            if (linha.equals("sair")) break;
            if (linha.equals("ajuda")) { imprimirAjuda(); continue; }
            try {
                if (linha.startsWith("mover")) {
                    Posicao destino = interpretarDestino(linha, e);
                    if (destino == null) {
                        System.out.println("Uso: mover cima|baixo|esquerda|direita  ou  mover <linha> <coluna>");
                        continue;
                    }
                    server.mover(meuId, destino);
                } else if (linha.startsWith("cerca")) {
                    Cerca cerca = interpretarCerca(linha);
                    if (cerca == null) {
                        System.out.println("Uso: cerca <linha> <coluna> <h|v>   (linha/coluna de 0 a 7)");
                        continue;
                    }
                    server.colocarCerca(meuId, cerca);
                } else {
                    System.out.println("Comando desconhecido. Use ajuda para ver os comandos.");
                }
            } catch (JogadaInvalidaException ex) {
                System.out.println("[ERRO DE JOGADA] " + ex.getMessage());
            } catch (RemoteException ex) {
                System.out.println("[ERRO DE REDE] " + ex.getMessage());
                break;
            }
        }
        scanner.close();
    }

    private Posicao interpretarDestino(String linha, EstadoJogo e) {
        String[] partes = linha.split("\\s+");
        Posicao atual = e.getPosicao(meuId);
        if (partes.length == 2) {
            return switch (partes[1]) {
                case "cima" -> new Posicao(atual.linha() - 1, atual.coluna());
                case "baixo" -> new Posicao(atual.linha() + 1, atual.coluna());
                case "esquerda" -> new Posicao(atual.linha(), atual.coluna() - 1);
                case "direita" -> new Posicao(atual.linha(), atual.coluna() + 1);
                default -> null;
            };
        }
        if (partes.length == 3) {
            try {
                int l = Integer.parseInt(partes[1]);
                int c = Integer.parseInt(partes[2]);
                return new Posicao(l, c);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Cerca interpretarCerca(String linha) {
        String[] partes = linha.split("\\s+");
        if (partes.length != 4) return null;
        try {
            int l = Integer.parseInt(partes[1]);
            int c = Integer.parseInt(partes[2]);
            Orientacao o = partes[3].startsWith("h") ? Orientacao.HORIZONTAL : Orientacao.VERTICAL;
            return new Cerca(new Posicao(l, c), o);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public synchronized void imprimirEstado(EstadoJogo e) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- TABULEIRO 9x9 ---\n");
        sb.append("   ");
        for (int c = 0; c < 9; c++) sb.append("  ").append(c).append(" ");
        sb.append("\n");
        boolean[][] mapH = new boolean[8][9];
        boolean[][] mapV = new boolean[9][8];
        for (Cerca cer : e.getCercas()) {
            int r = cer.base().linha();
            int c = cer.base().coluna();
            if (cer.orientacao() == Orientacao.HORIZONTAL) {
                mapH[r][c] = true;
                mapH[r][c + 1] = true;
            } else {
                mapV[r][c] = true;
                mapV[r + 1][c] = true;
            }
        }
        for (int r = 0; r < 9; r++) {
            sb.append(r).append("  ");
            for (int c = 0; c < 9; c++) {
                sb.append("[").append(charPeao(e, r, c)).append("]");
                if (c < 8) sb.append(mapV[r][c] ? "|" : " ");
            }
            sb.append("\n");
            if (r < 8) {
                sb.append("   ");
                for (int c = 0; c < 9; c++) {
                    sb.append(mapH[r][c] ? "---" : "   ");
                    if (c < 8) sb.append("+");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
        for (int i = 1; i <= 4; i++) {
            Posicao p = e.getPosicao(i);
            sb.append("J").append(i).append(" ").append(e.getNome(i))
              .append(" em (").append(p.linha()).append(",").append(p.coluna()).append(")")
              .append(" | Cercas: ").append(e.getCercasRestantes(i))
              .append(i == e.getJogadorDaVez() ? "  << VEZ" : "").append("\n");
        }
        if (e.getStatus() == EstadoJogo.Status.FINALIZADO) {
            sb.append("VENCEDOR: Jogador ").append(e.getVencedor()).append("\n");
        }
        System.out.print(sb);
    }

    private String charPeao(EstadoJogo e, int r, int c) {
        for (int i = 1; i <= 4; i++) {
            Posicao p = e.getPosicao(i);
            if (p.linha() == r && p.coluna() == c) return String.valueOf(i);
        }
        return ".";
    }

    private void imprimirAjuda() {
        System.out.println("Comandos:");
        System.out.println("  mover cima|baixo|esquerda|direita");
        System.out.println("  mover <linha> <coluna>");
        System.out.println("  cerca <linha> <coluna> <h|v>   (cerca horizontal ou vertical)");
        System.out.println("  ajuda | sair");
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
```

- [ ] **Step 1: Criar os três arquivos**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 13: client — BotJogador (IA simples para E2E e demo --bot)

**Files:**
- Create: `src/main/java/client/BotJogador.java`

```java
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
```

- [ ] **Step 1: Criar o arquivo**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 14: client — ClientMain (lookup, registro, loop)

**Files:**
- Create: `src/main/java/client/ClientMain.java`

```java
package client;

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
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--nome" -> { if (i + 1 < args.length) nome = args[++i]; }
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--porta" -> { if (i + 1 < args.length) porta = Integer.parseInt(args[++i]); }
                case "--bot" -> bot = true;
                default -> { }
            }
        }

        GameServer server = conectar(host, porta);
        ConsoleUI ui = new ConsoleUI();
        ClientCallbackImpl callback = new ClientCallbackImpl(ui);
        int id = server.registrar(callback, nome);
        ui.setMeuId(id);
        System.out.println("[CLIENTE] Registrado como Jogador " + id + " (" + nome + ").");

        if (bot) {
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
        } else {
            ui.loop(server);
            System.exit(0);
        }
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

- [ ] **Step 1: Criar o arquivo**
- [ ] **Step 2: Compilar**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

---

### Task 15: e2e — SemSocketTest e E2ETest

**Files:**
- Create: `src/test/java/e2e/SemSocketTest.java`
- Create: `src/test/java/e2e/E2ETest.java`

- [ ] **Step 1: Criar `SemSocketTest.java`**

```java
package e2e;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemSocketTest {

    @Test
    void nenhumUsoDeSocketJava() throws IOException {
        Path src = Paths.get("src/main/java");
        assertTrue(Files.exists(src), "Pasta src/main/java deveria existir");
        long arquivosVerificados = Files.walk(src)
                .filter(p -> p.toString().endsWith(".java"))
                .peek(p -> {
                    String conteudo;
                    try {
                        conteudo = Files.readString(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertFalse(conteudo.contains("java.net.Socket"), "Arquivo " + p + " usa java.net.Socket");
                    assertFalse(conteudo.contains("new Socket"), "Arquivo " + p + " usa new Socket");
                    assertFalse(conteudo.contains("ServerSocket"), "Arquivo " + p + " usa ServerSocket");
                })
                .count();
        assertTrue(arquivosVerificados > 0, "Nenhum arquivo .java encontrado");
        System.out.println("OK: " + arquivosVerificados + " arquivos verificados — nenhum uso de Socket (só RMI).");
    }
}
```

- [ ] **Step 2: Criar `E2ETest.java`**

```java
package e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class E2ETest {
    private final List<Process> processos = new ArrayList<>();
    private final Map<Process, StringBuilder> logs = new ConcurrentHashMap<>();

    @Test
    void partidaCompletaComQuatroBots() throws Exception {
        String classpath = System.getProperty("java.class.path");

        Process servidor = iniciar(classpath, "server.ServerMain");
        Thread.sleep(4000);

        List<Process> clientes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            clientes.add(iniciar(classpath, "client.ClientMain", "--nome", "Bot" + i, "--bot"));
            Thread.sleep(300);
        }

        long prazo = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        String logServidor = "";
        while (System.currentTimeMillis() < prazo) {
            logServidor = logDe(servidor);
            if (logServidor.contains("[FIM]")) break;
            if (!servidor.isAlive()) break;
            Thread.sleep(500);
        }

        System.out.println("===== LOG SERVIDOR =====");
        System.out.println(logServidor);

        for (Process p : processos) p.destroy();

        assertTrue(logServidor.contains("[FIM]"), "A partida deveria terminar. Log do servidor:\n" + logServidor);
        assertTrue(logServidor.contains("Jogador 4 ("), "Deveriam registrar 4 jogadores. Log:\n" + logServidor);

        String linhaVencedor = logServidor.lines()
                .filter(l -> l.contains("[FIM]"))
                .findFirst()
                .orElse("");
        assertTrue(linhaVencedor.matches(".*vencedor=[1-4].*"), "Deveria haver um vencedor 1-4: " + linhaVencedor);
        int vencedor = extrairVencedor(linhaVencedor);

        for (int i = 0; i < clientes.size(); i++) {
            String log = logDe(clientes.get(i));
            assertTrue(log.contains("FIM vencedor="), "Cliente " + (i + 1) + " deveria receber o fim:\n" + log);
            assertTrue(log.contains("vencedor=" + vencedor),
                    "Cliente " + (i + 1) + " deveria ver o mesmo vencedor (" + vencedor + "):\n" + log);
        }
        System.out.println("E2E OK: partida completa terminou com vencedor=" + vencedor
                + " e os 4 clientes receberam o estado final via callback.");
    }

    private int extrairVencedor(String linha) {
        String[] partes = linha.split("vencedor=");
        return Integer.parseInt(partes[1].trim().split("\\s+")[0]);
    }

    private Process iniciar(String classpath, String classe, String... extra) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(classe);
        for (String a : extra) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        processos.add(p);
        StringBuilder log = new StringBuilder();
        logs.put(p, log);
        Thread leitor = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    synchronized (log) {
                        log.append(linha).append("\n");
                    }
                }
            } catch (IOException ignorada) {
                // processo terminou
            }
        });
        leitor.setDaemon(true);
        leitor.start();
        return p;
    }

    private String logDe(Process p) {
        StringBuilder sb = logs.get(p);
        if (sb == null) return "";
        synchronized (sb) {
            return sb.toString();
        }
    }

    @AfterEach
    void limpar() {
        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
                if (p.isAlive()) p.destroyForcibly();
            }
        }
        processos.clear();
    }
}
```

- [ ] **Step 3: Rodar a suíte completa (compila + unitários + e2e)**

Run: `mvn -q test`
Expected: BUILD SUCCESS — TabuleiroTest (10), PartidaTest (5), SemSocketTest (1), E2ETest (1) verdes.

> Se o E2E falhar por timeout, aumente `Thread.sleep(4000)` inicial e o prazo de 3 minutos; se falhar por porta ocupada, mate processos java pendentes (a limpeza `@AfterEach` derruba os processos filhos).

---

### Task 16: README.md e docs/relatorio.md

**Files:**
- Create: `README.md`
- Create: `docs/relatorio.md`

- [ ] **Step 1: Criar `README.md`**

```markdown
# Quoridor Distribuído (RMI)

Jogo Quoridor para 4 jogadores implementado com **Java RMI puro** (disciplina de Sistemas Distribuídos).

- Servidor central com RMI Registry embutido (porta 1099), sem depender do utilitário externo `rmiregistry`.
- 4 clientes (um por jogador) com **callback RMI** para sincronização em tempo real.
- Nenhum uso de `java.net.Socket` — apenas RMI (verificado por teste automatizado).
- Engine de regras 100% testada com JUnit (movimento, pulo, cercas, caminho garantido, vitória).

## Requisitos

- JDK 17+ (testado com JDK 21 Temurin)
- Maven 3.9+

## Como compilar

```bash
mvn clean package
```

## Como rodar

Em um terminal, inicie o servidor:

```bash
java -cp target/classes server.ServerMain
```

Em outros **4 terminais**, inicie cada jogador:

```bash
java -cp target/classes client.ClientMain --nome Jogador1
java -cp target/classes client.ClientMain --nome Jogador2
java -cp target/classes client.ClientMain --nome Jogador3
java -cp target/classes client.ClientMain --nome Jogador4
```

A partida começa automaticamente quando o 4º jogador entra.

## Comandos do jogador (modo texto)

| Comando | Exemplo | Descrição |
|---------|---------|-----------|
| `mover cima/baixo/esquerda/direita` | `mover cima` | Move o peão 1 casa (ou pula) |
| `mover <linha> <coluna>` | `mover 3 4` | Move para uma casa específica |
| `cerca <linha> <coluna> <h/v>` | `cerca 4 4 h` | Coloca uma cerca horizontal/vertical |
| `ajuda` | `ajuda` | Mostra os comandos |
| `sair` | `sair` | Sai do cliente |

> Cerca: `linha` e `coluna` vão de 0 a 7 (base da cerca no canto superior-esquerdo do segmento de 2 casas).

## Modo bot (demonstração/validação E2E)

```bash
java -cp target/classes client.ClientMain --nome Bot1 --bot
```

Cada bot decide sua jogada automaticamente (movimento em direção à meta ou cerca para atrapalhar o oponente mais próximo).

## Testes

```bash
mvn test
```

- `TabuleiroTest` / `PartidaTest`: regras do jogo (movimento, pulo, cercas, caminho garantido, vitória, turnos).
- `SemSocketTest`: garante que **nenhum** arquivo usa `java.net.Socket`/`new Socket`/`ServerSocket`.
- `E2ETest`: sobe 1 servidor + 4 clientes bot em **processos separados** e joga uma partida completa até alguém vencer, verificando que os 4 clientes recebem o estado final via callback.

## Estrutura

```
src/main/java/common/   interfaces remotas + modelos serializáveis (EstadoJogo, Posicao, Cerca, GameServer, ClientCallback)
src/main/java/engine/   regras do jogo, puras e sem RMI (Tabuleiro, Partida)
src/main/java/server/   GameServerImpl, ServerMain
src/main/java/client/   ClientCallbackImpl, ConsoleUI, BotJogador, ClientMain
```

Veja `docs/relatorio.md` para a explicação da arquitetura e decisões de design.
```

- [ ] **Step 2: Criar `docs/relatorio.md`** (escreva o relatório em texto — arquitetura, uso de RMI, decisões de design, como validar)
- [ ] **Step 3: Commit final**

```
git add -A
git commit -m "docs: adiciona README e relatório do projeto"
```

---

### Task 17: Verificação final (verification-before-completion)

- [ ] **Step 1: Rodar a suíte completa**

Run: `mvn clean test`
Expected: BUILD SUCCESS com todos os testes verdes.

- [ ] **Step 2: Rodar `SemSocketTest` isoladamente para confirmar ausência de Socket**

Run: `mvn -q test -Dtest=SemSocketTest`

- [ ] **Step 3: Rodar a demo manual (opcional, para o vídeo)**

```bash
mvn clean package
java -cp target/classes server.ServerMain          # terminal 1
java -cp target/classes client.ClientMain --nome Jogador1   # terminal 2
java -cp target/classes client.ClientMain --nome Jogador2   # terminal 3
java -cp target/classes client.ClientMain --nome Jogador3   # terminal 4
java -cp target/classes client.ClientMain --nome Jogador4   # terminal 5
```

- [ ] **Step 4: Verificar histórico git**

Run: `git log --oneline`
Expected: commits coerentes (inicialização → common → engine → testes → server → client → e2e → docs).
