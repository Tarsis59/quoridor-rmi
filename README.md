# Quoridor Distribuído (RMI)

Jogo **Quoridor para 4 jogadores** implementado de forma **distribuída** com **Java RMI puro** (`java.rmi`), para a disciplina de **Sistemas Distribuídos**.

- Servidor central com **RMI Registry embutido** (porta 1099) — sem depender do utilitário externo `rmiregistry`.
- 4 clientes (um por jogador) com **callback RMI** para sincronização em tempo real entre todos.
- **Nenhum uso de `java.net.Socket`** — apenas RMI (verificado por teste automatizado `SemSocketTest`).
- **Regras oficiais do Quoridor para 4 jogadores**: tabuleiro 9×9, **5 cercas por jogador** (20 no total), pulo sobre peão, pulo diagonal quando há cerca/borda/outro peão atrás (nunca se pulam 2 peões), cercas não podem se sobrepor nem se cruzar e nunca podem fechar todo o caminho de alguém.
- **Autorização por sessão**: no registro cada jogador recebe um token secreto; o servidor recusa jogadas de quem tenta jogar no lugar de outro.
- **Tolerância a falhas**: o servidor faz *ping* nos clientes; quem cai ou fecha a janela tem a vez pulada, e o último jogador conectado vence por W.O. — a partida nunca trava.
- Engine de regras testada com JUnit (movimento, pulos, cercas, cruzamento, caminho garantido, vitória, turnos, desconexão).
- Validação **E2E**: sobe 1 servidor + 4 clientes bot em processos separados e joga uma partida completa até alguém vencer; outro E2E derruba um cliente no meio e confirma que a partida continua.

---

## Requisitos

- **JDK 17+** (testado com JDK 21 Temurin)
- **Maven 3.9+**

## Como compilar

```bash
MAVEN_OPTS="-Xmx512m" mvn clean package
```

> No PowerShell: `$env:MAVEN_OPTS="-Xmx512m"; mvn clean package`
> Para compilar **sem rodar os testes** (mais rápido, ideal só para jogar): adicione `"-Dmaven.test.skip=true"` no fim — **com aspas**: sem elas o PowerShell quebra o argumento no ponto e o Maven responde `Unknown lifecycle phase ".test.skip=true"`.

O artefato compilado fica em `target/classes` (sem dependências externas além da JDK).

## Como rodar

> 📖 **Guia completo, passo a passo e sem erros, com as duas formas de jogo e a solução de problemas: [COMO_EXECUTAR.md](COMO_EXECUTAR.md).**

O jogo usa **1 servidor + 4 clientes**, cada um em um **terminal próprio**. A partida **começa automaticamente** quando o 4º jogador entra.

### 1. Servidor (Terminal 1)

```bash
java -cp target/classes server.ServerMain
```

### 2a. Jogar no terminal (Terminais 2 a 5)

```bash
java -cp target/classes client.ClientMain --nome Jogador1
java -cp target/classes client.ClientMain --nome Jogador2
java -cp target/classes client.ClientMain --nome Jogador3
java -cp target/classes client.ClientMain --nome Jogador4
```

Quando for a sua vez, o cliente lista os **movimentos válidos**; digite `mover cima`, `mover 3 4`, `cerca 4 4 h`, `tabuleiro`, `ajuda` ou `sair` (veja a tabela completa abaixo).

### 2b. Ou jogar na interface gráfica (Terminais 2 a 5)

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot2
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot3
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot4
```

> Na GUI, use `--modo manual` (ou só `--gui`, que pergunta na abertura) para **jogar por cliques**: clique na casa legal para mover e use a barra de ferramentas para cercas (clique direito alterna H/V).

### Argumentos opcionais

| Flag | Aplicável a | Padrão | Descrição |
|------|-------------|--------|-----------|
| `--porta <n>` | servidor e cliente | `1099` | Porta do RMI Registry |
| `--host <host>` | cliente | `localhost` | Host (IP) do servidor |
| `--hostname <ip>` | servidor e cliente | `localhost` | IP desta máquina anunciado ao RMI (só para jogar em **máquinas diferentes**) |
| `--nome <nome>` | cliente | `Jogador` | Nome exibido na partida |
| `--bot` | cliente | — | Modo automático (IA simples) |
| `--gui` | cliente | — | Abre a interface gráfica (Swing) |
| `--modo <manual\|auto>` | cliente | diálogo na abertura | Modo da interface gráfica |

## Comandos do jogador (modo texto)

| Comando | Exemplo | Descrição |
|---------|---------|-----------|
| `mover cima/baixo/esquerda/direita` | `mover cima` | Move o peão 1 casa (ou pula reto sobre um adversário) |
| `mover <linha> <coluna>` | `mover 3 4` | Move para uma casa específica (use para **pulos diagonais**) |
| `cerca <linha> <coluna> <h/v>` | `cerca 4 4 h` | Coloca uma cerca horizontal/vertical (5 por jogador) |
| `tabuleiro` | `tabuleiro` | Redesenha o tabuleiro |
| `ajuda` | `ajuda` | Mostra os comandos |
| `sair` | `sair` | Sai do cliente |

> Cerca: `linha` e `coluna` vão de **0 a 7** (base da cerca no canto superior-esquerdo do segmento de 2 casas).
> `h` = cerca **abaixo** das casas (l,c) e (l,c+1); `v` = cerca **à direita** das casas (l,c) e (l+1,c).

## Regras implementadas (oficiais, 4 jogadores)

| Regra | Como funciona |
|-------|---------------|
| Início | J1 embaixo (8,4) → meta linha 0; J2 em cima (0,4) → meta linha 8; J3 à direita (4,8) → meta coluna 0; J4 à esquerda (4,0) → meta coluna 8 |
| Cercas | **5 por jogador** (as 20 cercas do jogo divididas entre 4). Cada cerca tem 2 casas de comprimento |
| Jogada | Na sua vez: **mover** o peão **ou** colocar **uma** cerca |
| Movimento | 1 casa na horizontal/vertical, sem atravessar cercas |
| Pulo | Peão adjacente pode ser pulado em linha reta; se atrás dele houver cerca, borda ou **outro peão**, o pulo é **diagonal** (nunca se pulam 2 peões) |
| Cerca válida | Não sobrepõe outra, **não cruza** outra no mesmo ponto central e **não fecha todo o caminho** de nenhum jogador (peões não contam como bloqueio) |
| Vitória | O primeiro a alcançar qualquer casa da borda oposta vence |
| Desconexão | Quem cair tem a vez pulada; se sobrar só 1 conectado, ele vence por W.O. |

## Modo bot (demonstração / validação E2E)

```bash
java -cp target/classes client.ClientMain --nome Bot1 --bot
```

Cada bot decide sua jogada automaticamente com o mesmo motor de regras do servidor: anda pelo menor caminho até a meta e, quando um adversário está mais perto de vencer, coloca a cerca que mais atrasa esse adversário sem atrasar a si mesmo.

## Interface gráfica (Swing)

O cliente também possui uma **interface gráfica em Java Swing** com visual "Moderno Plano" — tabuleiro 9×9 com casas, peões coloridos, cercas e destaque da vez, painel lateral de jogadores e barra de status. As **cercas são coloridas pela cor do jogador que as colocou** e cada linha do painel lateral tem uma **barra na cor do jogador**, para identificar rapidamente quem montou cada barreira e quem é quem. Para abri-la, use a flag `--gui`:

```bash
java -cp target/classes client.ClientMain --gui --nome Jogador1
```

### Modo Manual (por cliques)

Quando for a sua vez (e só nela), as **casas destino legais** são destacadas em verde no tabuleiro; basta clicar para mover o peão. A barra de ferramentas alterna entre **Mover**, **Cerca H** e **Cerca V** — ao mover o mouse sobre o tabuleiro, um **preview** da cerca mostra a aresta candidata (verde = válida, vermelho = inválida), e o **clique direito** alterna a orientação H ↔ V rapidamente. A validação final é sempre do servidor; erros aparecem na barra de status.

### Modo Automático (demonstração)

A janela apenas **exibe** a partida evoluindo sozinha em tempo real, com os 4 processos jogando como bots em um ritmo lento (~1 jogada a cada 1,5 s) — ideal para demonstrar o jogo completo sem intervenção. Ao final a janela continua aberta mostrando o resultado:

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot2
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot3
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot4
```

Se `--modo` não for informado, um diálogo pergunta entre **Manual** e **Automático** na abertura.

![Interface gráfica do Quoridor](docs/img/quoridor-gui.png)

> Na imagem: cada borda de chegada tem o tom claro da cor do jogador que precisa alcançá-la; a vertical verde passa entre duas horizontais em linha (permitido, pois não se cruzam); o peão do jogador que saiu fica esmaecido.

## Testes

```bash
mvn test
```

| Suíte | Cobertura |
|-------|-----------|
| `TabuleiroTest` (20) | Movimento, borda, pulo reto/diagonal/bloqueado, pulo diagonal com 2º peão atrás, sem movimentos repetidos, sobreposição, cruzamento, vertical passando entre duas horizontais (permitido), cerca fora do tabuleiro, cerca que isolaria um jogador, peões não bloqueiam caminho, distância BFS, vitória pelos 4 lados |
| `PartidaTest` (13) | Turnos, jogada fora da vez, vitória, movimento inválido, **5 cercas por jogador** (6ª recusada), vez pulada de desconectado, vitória por W.O., entradas nulas, nome sanitizado |
| `GameServerImplTest` (10) | Registro de 4 com tokens distintos, 5º recusado, **jogar no lugar de outro é recusado**, broadcast para todos, queda detectada por ping, W.O., fim avisado uma vez só |
| `BotJogadorTest` (5) | Movimento pelo menor caminho, cerca contra quem está perto de vencer, 5 partidas completas simuladas sempre terminam |
| `ConsoleUITest` (2) | Interpretação de comandos (cerca só com `h`/`v`, direção que pula peão) |
| `SemSocketTest` (1) | Varre `src/main/java` e garante que **nenhum** arquivo usa `java.net.Socket`/`new Socket`/`ServerSocket` |
| `E2ETest` (2) | 1 servidor + 4 clientes bot em **processos separados**: partida completa até o vencedor (todos recebem o fim via callback) e partida em que um cliente é derrubado no meio e o jogo continua |
| `ui/*` (16) | Geometria, cliques do `TabuleiroPanel` (inclusive fora da vez), `PainelJogadores`, `BarraStatus` e `GraphicUI` |

## Estrutura

```
src/main/java/
├── common/   interfaces remotas + modelos serializáveis (EstadoJogo, Posicao, Cerca, Sessao, GameServer, ClientCallback)
├── engine/   regras do jogo, puras e sem RMI (Tabuleiro, Partida)
├── server/   GameServerImpl (lógica RMI + callbacks), ServerMain (registry embutido)
└── client/   ClientCallbackImpl, ConsoleUI, BotJogador, CaixaEstado, ClientMain
    └── ui/   interface gráfica Swing (GraphicUI, TabuleiroPanel, PainelJogadores, BarraStatus, DialogoModo, Geometria, EstiloUI)
src/test/java/
├── engine/   testes JUnit da engine
├── server/   testes do servidor (sessão, desconexão, W.O.)
├── client/   testes do bot e do console (+ ui/ com os testes da interface gráfica)
└── e2e/      SemSocketTest + E2ETest
```

Veja **`docs/relatorio.md`** para a explicação detalhada da arquitetura, do protocolo RMI e das decisões de design.
