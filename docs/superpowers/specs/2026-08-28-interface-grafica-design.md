# Especificação — Interface Gráfica Swing para o Quoridor Distribuído

**Data:** 2026-08-28
**Projeto:** Quoridor 4 jogadores com Java RMI puro
**Status:** Aprovado pelo usuário

---

## 1. Objetivo

Adicionar ao cliente Quoridor uma **interface gráfica (GUI) em Java Swing** com visual **Moderno Plano** (flat, limpo, organizado), mantendo o requisito da disciplina de **RMI puro e ausência de `java.net.Socket`**. A GUI deve permitir **dois modos** de uso, escolhidos no lançamento:

- **Modo Manual** — um jogador humano joga clicando no tabuleiro (mover peão e colocar cerca).
- **Modo Automático (ilustrativo)** — o processo usa o `BotJogador` existente e a janela exibe a partida evoluindo sozinha em tempo real. Ideal para demonstrar o jogo completo sem intervenção.

A interface atual por caracteres (`ConsoleUI`) **continua disponível** como alternativa (via ausência do flag `--gui`).

---

## 2. Decisões de design (acordadas em brainstorming)

| Tema | Decisão |
|------|---------|
| Interação | 100% por clique no tabuleiro (nenhum comando de texto) |
| Modos | Manual (humano) e Automático (bots ilustrativos) |
| Estilo visual | Moderno Plano (flat) |
| Layout | Aprovado em mockup: barra superior + tabuleiro central + painel lateral + barra de status |
| Tecnologia | Java Swing, **zero dependências externas**, um único jar gerado por `mvn clean package` |
| Transporte | RMI inalterado; a GUI apenas consome a interface `UI` existente |

---

## 3. Arquitetura

### 3.1 Princípio central: não tocar no RMI

O servidor, o engine (`Tabuleiro`, `Partida`), as classes comuns (`EstadoJogo`, `Cerca`, `Posicao`, etc.) e o callback remoto **não mudam**. A GUI é apenas mais uma implementação da interface local já existente:

```java
public interface UI {
    void novoEstado(EstadoJogo estado);   // callback do servidor -> redesenha a janela
    void finalizar(int idVencedor);       // callback do servidor -> tela de fim
}
```

`ClientCallbackImpl` já encaminha os callbacks RMI para uma `UI`; basta injetar a `GraphicUI` em vez da `ConsoleUI`.

### 3.2 Componentes novos (pacote `client.ui`)

| Classe | Responsabilidade |
|--------|------------------|
| `GraphicUI` | Implementa `UI`. Janela principal (`JFrame`), agrega os painéis, orquestra estado/modo, converte eventos de callback para a EDT (`SwingUtilities.invokeLater`), expõe `getEstadoAtual()` para o loop do bot. |
| `TabuleiroPanel` | `JPanel` custom com `paintComponent`. Desenha o tabuleiro 9x9 (casas, peões, cercas, coordenadas, destaque de vez), trata cliques do mouse e desenha o preview de cerca. |
| `PainelJogadores` | Lista os 4 jogadores (cor, nome, posição, cercas restantes, badge `VEZ`, realce para "você"). |
| `BarraStatus` | Mensagens de status/dicas/erros. |
| `DialogoModo` | Diálogo inicial: escolha entre **Manual** e **Automático**. |
| `EstiloUI` | Constantes centralizadas do tema Moderno Plano (cores, fontes, espaçamentos). |

### 3.3 Alteração mínima no código existente

Somente `client/ClientMain.java`:

- Novo parse de args: `--gui`, `--modo manual|auto`.
- Se `--gui`: instancia `GraphicUI`; caso contrário, mantém `ConsoleUI` (comportamento atual).
- O loop de bot continua como hoje, usando `ui.getEstadoAtual()` — funciona para `ConsoleUI` e `GraphicUI` igualmente.

`UI`, `ConsoleUI`, `BotJogador`, `ClientCallbackImpl` ficam intactos.

---

## 4. Escolha de modo

1. Se `--modo` foi informado, **prevalece** (sem diálogo). Valores: `manual` ou `auto`.
2. Se não, ao abrir a `GraphicUI`, o `DialogoModo` pergunta: **Modo Manual** ou **Modo Automático (bots jogam sozinhos)**.
3. Modo **manual**: a janela espera cliques do humano quando for a vez dele.
4. Modo **auto**: `ClientMain` inicia um `BotJogador` que decide jogadas pelo `GameServer`; a janela apenas exibe. Suporta todos os 4 processos cliente em auto, permitindo a demo "4 bots jogando sozinhos".

---

## 5. Layout da janela (mockup aprovado)

```
┌───────────────────────────────────────────────────────────┐
│ QUORIDOR · Modo: Manual        Você é o Jogador 2         │  <- barra superior (título, modo, identidade)
├───────────────────────────────────────────┬───────────────┤
│   coordenadas (0..8 col / 0..8 lin)       │  JOGADORES    │
│   ┌─────── tabuleiro 9x9 ─────────┐       │  ● Jogador 1  │
│   │ casas planas · peões coloridos│       │     (7,4) 6    │  <- badge VEZ dourado
│   │ cercas = barras               │       │  ● Jogador 2  │
│   │ destaque da vez               │       │     (1,4) 8    │  <- realce "você"
│   └───────────────────────────────┘       │  ● Jogador 3  │
│                                           │  ● Jogador 4  │
│                                           │  SUAS CERCAS  │
│                                           │  ▬▬▬ ▬▬▬ ▬▬  │
├───────────────────────────────────────────┴───────────────┤
│ Vez do Jogador 1 — aguardando...   Dica de interação       │  <- barra de status
└───────────────────────────────────────────────────────────┘
```

- Janela não-redimensionável ou com tamanho fixo calculado (para manter o visual estável).
- `JFrame` com `JPanel` raiz `BorderLayout`: `NORTH` = barra superior, `CENTER` = contêiner do tabuleiro, `EAST` = `PainelJogadores`, `SOUTH` = `BarraStatus`.

---

## 6. Interação manual (100% clique)

### 6.1 Mover peão
- Quando for a vez do jogador local, as **casas destino legais** são destacadas (visual sutil).
- Clique numa casa vizinha/destino válido → `GameServer.mover(meuId, destino)`.
- A validação final é do servidor; se `JogadaInvalidaException`, a `BarraStatus` mostra a mensagem.

### 6.2 Colocar cerca
- Botão **Cerca H / Cerca V** na barra de ferramentas alterna o modo cerca (orientação atual H ou V).
- Ao mover o mouse sobre o tabuleiro, um **preview** da cerca aparece na aresta candidata.
- Clique confirma → `GameServer.colocarCerca(meuId, cerca)`.
- **Clique direito** alterna a orientação H ↔ V rapidamente.
- Botão volta ao modo "Mover" após colocar (ou por clique explícito).

### 6.3 Regras de acessibilidade dos cliques
- Cliques em arestas/casas fora do padrão legal são ignorados com mensagem amigável na `BarraStatus`.
- Estados de habilitação: apenas quando `estado.getJogadorDaVez() == meuId` e status `EM_ANDAMENTO`.

---

## 7. Modo automático (ilustrativo)

- `ClientMain` com `--modo auto` cria `BotJogador(id)` e roda o mesmo loop do modo `--bot` atual (joga quando for a vez, dorme caso contrário, imprime `FIM vencedor=` no encerramento).
- A `GraphicUI` exibe cada `novoEstado` vindo do callback, com pequena **animação suave** (repaint em intervalos curtos) para tornar a demo agradável. A animação é opcional e não altera a lógica.
- O título da barra superior exibe "Modo: Automático".

---

## 8. Tema Moderno Plano (paleta centralizada em `EstiloUI`)

| Elemento | Cor / Valor |
|----------|-------------|
| Fundo da janela | `#eef1f6` |
| Fundo do tabuleiro | `#e9edf2` |
| Casa | branco `#ffffff`, borda interna `#dfe4ea` |
| Cerca | `#334155` (slate escuro) |
| Peão 1 | `#ef4444` (vermelho) |
| Peão 2 | `#3b82f6` (azul) |
| Peão 3 | `#22c55e` (verde) |
| Peão 4 | `#f59e0b` (âmbar) |
| Badge VEZ | fundo `#fff7e0`, borda `#f3d16b`, texto `#6b5200` |
| Realce "você" | fundo `#eef6ff`, borda `#bcd9f7` |
| Topo | `#2b3442`, título `#f7c948` |
| Texto secundário | `#8492a6` / `#7b8794` |
| Erro na barra de status | `#dc2626` |

- Fontes: sans-serif do sistema (`Segoe UI` / `Dialog`). Títulos em negrito, legendas em caixa alta pequena.
- Peões: círculos com número do jogador, cor sólida.
- Cantos arredondados (casas/peças/painéis) para o clima flat moderno.

---

## 9. Testes

1. **Unitário novo — `TabuleiroPanelTest` (lógica de clique):**
   - A lógica "coordenada de pixel → casa/aresta" e "clique → `Posicao`/`Cerca`" é extraída em métodos puros e testada **sem abrir janela**.
   - Casos: clique em casa vizinha válida; clique em casa inválida; clique em aresta para cerca H/V; conversão de coordenadas de pixel.
2. **Existente:** os 19 testes atuais devem continuar verdes (incluindo `SemSocketTest`, que continua garantindo a ausência de Socket).
3. **E2E:** `E2ETest` atual (4 bots sem GUI) permanece. Demonstração manual via `--gui --modo auto` é validada executando localmente (não no JUnit, para evitar exigir display em CI).

> Nota de ambiente: a máquina tem pouca RAM; os mesmos flags de JVM leve usados hoje devem ser mantidos para rodar as JVMs filhas.

---

## 10. Documentação

- **README.md**: seção da interface gráfica, flags `--gui` / `--modo`, como abrir o modo manual e o modo automático, screenshot da janela.
- **docs/relatorio.md**: nova seção descrevendo a GUI (tecnologia Swing, arquitetura, decisões visuais), evidência de execução da demo automática.
- **docs/superpowers/specs/**: este documento.

---

## 11. Fora de escopo (YAGNI)

- Histórico de jogadas / placar agregado / chat.
- Animação elaborada (apenas repaint suave opcional).
- Som.
- Interface web.
- Persistência de partidas.

---

## 12. Critérios de conclusão

- `mvn clean package` com exit 0 e um único jar (`target/quoridor-rmi-1.0.0.jar`).
- Testes existentes + novos verdes.
- `SemSocketTest` verde (garantia de "sem Socket").
- Demo automática executada com sucesso: 1 servidor + 4 clientes `--gui --modo auto` até o `[FIM]`, com a janela exibindo o tabuleiro evoluindo.
- Um humano consegue jogar em modo manual por cliques (mover peão e colocar cerca) contra bots.
- README e relatório atualizados.
