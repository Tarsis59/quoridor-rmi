# Relatório — Quoridor Distribuído (RMI)

**Disciplina:** Sistemas Distribuídos
**Data:** 2026-08-28
**Tecnologia:** Java 21 (Temurin) · Maven · **Java RMI puro** (`java.rmi`) · JUnit 5

---

## 1. Objetivo

Implementar o jogo de tabuleiro **Quoridor** para **4 jogadores** de forma **distribuída**: um processo servidor central mantém o estado e as regras, e quatro processos cliente (um por jogador) interagem remotamente. O requisito central da disciplina — **não usar sockets diretamente** — é atendido usando **Java RMI puro**: o RMI utiliza sockets internamente de forma transparente, mas o código-fonte nunca instancia `java.net.Socket` (fato verificado por teste automatizado).

## 2. Por que Java RMI puro

1. **Implementação canônica de RMI** da plataforma Java, sem bibliotecas externas e sem geração manual de stubs (automática desde o Java 5).
2. **Suporte nativo a callback**: em RMI o cliente expõe um objeto remoto que o servidor pode invocar de volta. Isso é essencial aqui — sem callback, os outros três clientes nunca saberiam que um movimento ocorreu e que é a vez deles.
3. **Serialização padrão do Java**: todo o tráfego de dados usa objetos `Serializable`, sem dependência externa.
4. É exatamente o que a disciplina espera quando o enunciado pede "RMI" explicitamente.

## 3. Arquitetura

```
                          ┌─────────────────────────────┐
                          │   RMI Registry (porta 1099) │
                          └─────────────┬───────────────┘
                                        │ bind / lookup
                          ┌─────────────▼───────────────┐
                          │        GameServerImpl       │
                          │  Estado · regras · turnos   │
                          └─────────────┬───────────────┘
                 chamada RMI (move/wall) │ ▲ callback (broadcast de estado)
          ┌───────────┬──────────┬───────┴───────┬───────────┐
          ▼           ▼          ▼               ▼           ▼
  ┌────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
  │ Cliente 1  │ │ Cliente 2│ │ Cliente 3│ │ Cliente 4│ │   ...    │
  │ Jogador 1  │ │ Jogador 2│ │ Jogador 3│ │ Jogador 4│ │          │
  │ + callback │ │ +callback│ │ +callback│ │ +callback│ │          │
  └────────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

- O servidor cria o **RMI Registry embutido** no próprio processo (`LocateRegistry.createRegistry(1099)`), evitando a dependência do utilitário externo `rmiregistry`, que é uma fonte comum de problemas em demonstrações.
- Cada cliente, ao iniciar, faz `lookup` do serviço `QuoridorServer` e chama `registrar(callback, nome)`, entregando seu próprio objeto remoto de callback.
- Toda ação de jogo (`mover`, `colocarCerca`) é uma chamada **RMI síncrona** cliente → servidor. O servidor valida contra a engine, atualiza o estado e, se válido, **notifica os 4 clientes** via callback (`aoAtualizarEstado`).

## 4. Componentes

### Pacote `common` (compartilhado entre servidor e clientes)

| Classe | Papel |
|--------|-------|
| `Posicao` | `record` serializável com `linha`/`coluna` (0–8). |
| `Orientacao` | `enum` `HORIZONTAL`/`VERTICAL`. |
| `Cerca` | `Posicao` base + `Orientacao` (segmento de 2 casas). |
| `EstadoJogo` | Snapshot serializável: posições dos 4 peões, cercas colocadas, cercas restantes, jogador da vez, status (`AGUARDANDO`/`EM_ANDAMENTO`/`FINALIZADO`) e vencedor. |
| `JogadaInvalidaException` | Exception serializável (RMI exige exceções remotas serializáveis). |
| `GameServer` | Interface remota: `registrar`, `mover`, `colocarCerca`, `obterEstado`. |
| `ClientCallback` | Interface remota: `aoIniciarJogo`, `aoAtualizarEstado`, `aoFinalizarJogo`. |

### Pacote `engine` (regras puras, sem RMI — 100% testável)

- `Tabuleiro` — tabuleiro 9×9, posições dos peões, matrizes de paredes horizontais e verticais, e toda a lógica de regras.
- `Partida` — orquestração: nomes, cercas restantes, ordem de turnos, condição de vitória e geração de `EstadoJogo`.

As regras implementadas:
1. **Movimento ortogonal** de 1 casa, se a casa estiver livre e não houver parede entre as casas.
2. **Pulo sobre peão adjacente**: se a casa de destino está ocupada, verifica a casa **atrás** do adversário na mesma direção (pulo reto); se bloqueada, tenta as **duas casas laterais**; se também bloqueadas, o movimento nessa direção é inválido.
3. **Colocação de cerca**: dentro do tabuleiro, sem sobreposição e sem cruzamento sobre uma cerca já existente.
4. **Caminho garantido (BFS)**: antes de confirmar uma cerca, o servidor **simula** a colocação e roda uma busca em largura a partir da posição de cada um dos 4 peões até a respectiva borda de destino. Se qualquer jogador ficar sem caminho, a cerca é **rejeitada** (`JogadaInvalidaException`) — regra que impede "prender" um adversário sem vitória.
5. **Condição de vitória por borda** (variante 4 jogadores):

   | Jogador | Início | Destino |
   |---------|--------|---------|
   | 1 | `(8,4)` | linha 0 (topo) |
   | 2 | `(0,4)` | linha 8 (base) |
   | 3 | `(4,8)` | coluna 0 (esquerda) |
   | 4 | `(4,0)` | coluna 8 (direita) |

   Cada jogador começa com **10 cercas**.

### Pacote `server`

- `GameServerImpl extends UnicastRemoteObject implements GameServer` — encapsula a `Partida`, controla a fila de turnos (aceita apenas jogada do jogador da vez), mantém os callbacks registrados em `ConcurrentHashMap` e faz **broadcast** via callback após cada jogada válida. Um thread *daemon* de vigilância (`vigiarDesconexoes`) avança o turno automaticamente quando o jogador da vez desconectou.
- `ServerMain` — cria o registry embutido, faz `rebind` do serviço e aguarda os jogadores.

### Pacote `client`

- `ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback` — recebe os snapshots e delega para a `UI` local (com `synchronized` para garantir thread-safety entre a thread de callback e a thread de leitura de comandos).
- `ConsoleUI` — desenha o tabuleiro 9×9 em caracteres, lê comandos (`mover`, `cerca`, `ajuda`, `sair`) e exibe mensagens claras para `JogadaInvalidaException`.
- `BotJogador` — IA simples usada na validação E2E e no modo demo (`--bot`): avança pela menor distância até a meta e, periodicamente, tenta cercas para atrasar o oponente mais próximo.
- `ClientMain` — faz `lookup`, registra o callback e entra no loop de UI (ou no loop do bot).

## 5. Protocolo RMI e fluxo de dados

1. Cliente → `GameServer.registrar(callback, nome)` → servidor guarda o callback e atribui o `idJogador` (1–4).
2. No **4º registro**, o servidor inicia a partida, monta o `EstadoJogo` inicial e chama `aoIniciarJogo` nos 4 callbacks.
3. O jogador da vez chama `mover`/`colocarCerca` → o servidor valida contra a engine → se válido, atualiza o estado, avança o turno e chama `aoAtualizarEstado` nos 4 callbacks (inclusive no ativo, para refletir o novo turno).
4. Jogada inválida → o servidor lança `JogadaInvalidaException` de volta **apenas ao próprio cliente** (padrão de erro imediato, sem broadcast).
5. Vitória → o servidor marca `FINALIZADO` + `idVencedor`, chama `aoFinalizarJogo(idVencedor)` e depois `aoAtualizarEstado` nos 4 callbacks.

## 6. Decisões de design

- **Registry embutido no servidor**: reduz passos de demonstração e evita conflito com instâncias externas do `rmiregistry`.
- **Broadcast para os 4 clientes, inclusive o ativo**: simplifica o cliente — ele não precisa "adivinhar" o resultado da própria jogada; o estado chega pelo mesmo canal em todos os casos.
- **Erros de jogada como exceção direta, não callback**: o erro pertence a quem jogou; é mais simples e imediato do que um canal assíncrono.
- **Engine desacoplada do RMI**: as regras vivem em `engine` sem nenhuma referência a RMI, o que permite testá-las isoladamente com JUnit — a parte que mais impacta a nota.
- **Desconexão tolerada**: `RemoteException` no broadcast marca o cliente como desconectado sem derrubar o servidor; se o jogador da vez desconectou, o vigia avança o turno para a partida não travar.
- **Baixo acoplamento por `EstadoJogo`**: o estado completo é transportado em um único snapshot serializável, o que facilita reconexão e sincronização.

## 7. Interface gráfica (Swing)

O cliente ganhou uma **interface gráfica em Java Swing** (pacote `client.ui`) que mantém o requisito de **RMI puro e ausência de `java.net.Socket`**: a GUI é apenas mais uma implementação da interface local `JogadorUI` (`novoEstado`, `finalizar`, `getEstadoAtual`), alimentada pelo mesmo `ClientCallbackImpl` do modo texto. Nenhuma linha de RMI, engine ou classes `common` foi alterada — o `SemSocketTest` continua verificando a ausência de sockets.

### 7.1 Componentes

| Classe | Responsabilidade |
|--------|------------------|
| `GraphicUI` | Implementa `JogadorUI`. Janela (`JFrame`), agrega os painéis, orquestra modo/estado e converte callbacks para a EDT via `SwingUtilities.invokeLater`. |
| `TabuleiroPanel` | `JPanel` com `paintComponent`: desenha o tabuleiro 9×9 (casas, peões, cercas, coordenadas, destaque de vez) e trata cliques + preview de cerca. |
| `PainelJogadores` | Lista os 4 jogadores (cor, posição, cercas restantes, badge `VEZ`, realce "você"). |
| `BarraStatus` | Mensagens de status, dicas e erros. |
| `DialogoModo` | Diálogo inicial: escolha entre **Manual** e **Automático**. |
| `Geometria` | Conversão pura pixel ↔ casa/aresta, testável sem abrir janela. |
| `EstiloUI` | Tema "Moderno Plano" centralizado (cores, fontes, medidas). |

### 7.2 Modos de uso

- **Modo Manual** — o jogador humano age **100% por cliques**: casas destino legais destacadas, barra de ferramentas `Mover`/`Cerca H`/`Cerca V`, preview da cerca em tempo real (verde = válida, vermelho = inválida) e clique direito para alternar a orientação H ↔ V.
- **Modo Automático (ilustrativo)** — os 4 processos usam o `BotJogador` existente e a janela exibe a partida evoluindo sozinha; demonstra o jogo completo sem intervenção.

O modo é escolhido por `--modo manual|auto` ou, na ausência da flag, por um diálogo na abertura.

### 7.3 Decisões visuais (tema "Moderno Plano")

Paleta *flat* centralizada em `EstiloUI`: fundo `#eef1f6`, topo escuro `#2b3442` com título dourado `#f7c948`, peões vermelho/azul/verde/âmbar, badge de vez dourado e realce azul para o jogador local. Cantos arredondados em casas, peças e painéis — sem dependências externas (apenas `java.desktop` da JDK, mantendo o jar único).

Para facilitar a identificação visual, cada **cerca é pintada com a cor do jogador que a colocou** (a autoria de cada cerca é carregada no próprio `EstadoJogo`, numa lista paralela de donos preenchida pelo `Partida`), e cada linha do painel lateral de jogadores tem uma **barra vertical na cor do respectivo jogador**.

### 7.4 Validação

- **13 novos testes JUnit** (pacote `client.ui`): `GeometriaTest` (pixel ↔ casa/aresta), `TabuleiroPanelTest` (clique → `Posicao`/`Cerca` via eventos sintéticos), `PainelJogadoresTest`, `BarraStatusTest` e `GraphicUITest` (construção e troca de estado sem abrir janela).
- **Demo automática executada (2026-08-28):** 1 servidor + 4 clientes `--gui --modo auto` jogaram uma partida completa até `[FIM] vencedor=4 (Bot4)`, com a janela exibindo o tabuleiro evoluindo em ~1 jogada/2s (partida de ~82s), tempo suficiente para acompanhar cada jogada e cada cerca colorida. A captura está em `docs/img/quoridor-gui.png`.

## 8. Testes e validação

### Unitários (JUnit 5)

- `TabuleiroTest` (12 casos): movimento nas 4 direções, borda do tabuleiro, parede bloqueando movimento, pulo reto, pulo lateral, pulo bloqueado, cerca sobreposta/cruzada/fora do tabuleiro, **cerca que isolaria um jogador (rejeitada)**, cerca válida preservando caminho de todos, vitória pelos 4 lados.
- `PartidaTest` (5 casos): jogada fora da vez rejeitada, turnos em ciclo, vitória ao alcançar a meta, movimento inválido, estado inicial correto.

### SemSocketTest

Varre todo `src/main/java` e falha se qualquer arquivo contiver `java.net.Socket`, `new Socket` ou `ServerSocket` — garante o requisito "RMI sem socket explícito".

### E2ETest (partida completa)

Sobe **1 servidor + 4 clientes em processos JVM separados** (`ProcessBuilder`) com jogadores automáticos (`--bot`) e joga a partida até o fim:

- Verifica que os 4 jogadores se registraram (`Jogador 4 (...)` no log do servidor).
- Verifica que a partida terminou (`[FIM] vencedor=<1–4>`).
- Verifica que **os 4 clientes receberam o mesmo vencedor** via callback (`FIM vencedor=` no log de cada cliente).

**Resultado da validação (execução real em 2026-08-28):** a partida completa terminou com **vencedor = 4 (Bot4)**, que alcançou a coluna 8, e os 4 clientes confirmaram o estado final via callback.

## 9. Como validar

```bash
mvn clean test          # roda toda a suíte (unitários + sem-socket + e2e)
mvn clean package       # compila o jar
```

Demo manual (5 terminais):

```bash
java -cp target/classes server.ServerMain
java -cp target/classes client.ClientMain --nome Jogador1
java -cp target/classes client.ClientMain --nome Jogador2
java -cp target/classes client.ClientMain --nome Jogador3
java -cp target/classes client.ClientMain --nome Jogador4
```

Demo da interface gráfica em modo automático (1 servidor + 4 janelas):

```bash
java -cp target/classes server.ServerMain
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot2
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot3
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot4
```

## 10. Conclusão

O projeto entrega um Quoridor distribuído completo e funcional em Java RMI puro: engine com regras robustas (incluindo pulo e caminho garantido por BFS), servidor com registry embutido e broadcast via callback, clientes com interface de console, modo bot e **interface gráfica Swing** (manual por cliques e automática ilustrativa), e uma suíte de testes que cobre unitário, a GUI, ausência de sockets e uma partida E2E completa — atendendo integralmente os requisitos da disciplina.
