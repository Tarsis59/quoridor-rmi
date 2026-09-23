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
- Cada cliente, ao iniciar, faz `lookup` do serviço `QuoridorServer` e chama `registrar(callback, nome)`, entregando seu próprio objeto remoto de callback e recebendo uma `Sessao` (id 1–4 + token secreto).
- Toda ação de jogo (`mover(sessao, destino)`, `colocarCerca(sessao, cerca)`) é uma chamada **RMI síncrona** cliente → servidor. O servidor valida contra a engine, atualiza o estado e, se válido, **notifica os 4 clientes** via callback (`aoAtualizarEstado`).

## 4. Componentes

### Pacote `common` (compartilhado entre servidor e clientes)

| Classe | Papel |
|--------|-------|
| `Posicao` | `record` serializável com `linha`/`coluna` (0–8). |
| `Orientacao` | `enum` `HORIZONTAL`/`VERTICAL`. |
| `Cerca` | `Posicao` base + `Orientacao` (segmento de 2 casas). |
| `EstadoJogo` | Snapshot serializável: posições dos 4 peões, cercas colocadas (e o dono de cada uma), cercas restantes, jogador da vez, status (`AGUARDANDO`/`EM_ANDAMENTO`/`FINALIZADO`), vencedor, quem ainda está conectado e quantos já entraram na sala. |
| `Sessao` | `record` (id do jogador + token aleatório de 192 bits) devolvido no registro; o `toString` não expõe o token. |
| `JogadaInvalidaException` | Exception serializável (RMI exige exceções remotas serializáveis). |
| `GameServer` | Interface remota: `registrar`, `mover`, `colocarCerca`, `obterEstado`. |
| `ClientCallback` | Interface remota: `aoIniciarJogo`, `aoAtualizarEstado`, `aoFinalizarJogo`, `ping` (batimento). |

### Pacote `engine` (regras puras, sem RMI — 100% testável)

- `Tabuleiro` — tabuleiro 9×9, posições dos peões, matrizes de arestas bloqueadas (horizontais e verticais), matriz 8×8 dos **pontos centrais** ocupados por cercas, e toda a lógica de regras.
- `Partida` — orquestração: nomes, cercas restantes, jogadores ativos, ordem de turnos (pulando desconectados/travados), condição de vitória (inclusive W.O.) e geração de `EstadoJogo`.

As regras implementadas:
1. **Movimento ortogonal** de 1 casa, se a casa estiver livre e não houver parede entre as casas.
2. **Pulo sobre peão adjacente**: se a casa de destino está ocupada, verifica a casa **atrás** do adversário na mesma direção (pulo reto); se ali houver cerca, borda **ou outro peão** (com 4 jogadores não se pulam 2 peões), tenta as **duas casas diagonais**; se também bloqueadas, o movimento nessa direção é inválido. A lista de destinos não tem repetições.
3. **Colocação de cerca**: base de 0 a 7, sem sobreposição (aresta já ocupada) e sem **cruzamento** — cada cerca ocupa o ponto central entre suas duas metades, e duas cercas não podem usar o mesmo ponto central. Assim, uma vertical pode passar entre duas horizontais colocadas em linha (que não se cruzam), exatamente como no tabuleiro físico.
4. **Caminho garantido (BFS)**: antes de confirmar uma cerca, o servidor **simula** a colocação e roda uma busca em largura a partir da posição de cada um dos 4 peões até a respectiva borda de destino. Peões **não** contam como obstáculo nessa busca (eles se movem e podem ser pulados), conforme a regra oficial. Se qualquer jogador ficar sem caminho, a cerca é **rejeitada** (`JogadaInvalidaException`).
5. **Condição de vitória por borda** (variante 4 jogadores):

   | Jogador | Início | Destino |
   |---------|--------|---------|
   | 1 | `(8,4)` | linha 0 (topo) |
   | 2 | `(0,4)` | linha 8 (base) |
   | 3 | `(4,8)` | coluna 0 (esquerda) |
   | 4 | `(4,0)` | coluna 8 (direita) |

   Cada jogador começa com **5 cercas** — regra oficial do Quoridor para 4 jogadores (as 20 cercas do jogo divididas entre os 4).
6. **Jogador travado**: se um jogador não tiver nenhum movimento legal nem cerca possível (raro, mas possível com 4 peões), sua vez é pulada em vez de travar a partida.

### Pacote `server`

- `GameServerImpl extends UnicastRemoteObject implements GameServer` — encapsula a `Partida`, **autentica cada jogada pela sessão** (token comparado em tempo constante; um cliente não consegue jogar no lugar de outro), mantém os callbacks registrados e faz **broadcast** via callback após cada mudança de estado (inclusive durante a espera, mostrando "2/4 jogadores"). Um thread *daemon* de vigilância faz **ping** em todos os clientes a cada 1,5 s — fora do lock, para um cliente lento não travar os demais — e marca como desconectado quem não responde.
- `ServerMain` — cria o registry embutido, faz `rebind` do serviço, define um *timeout* de resposta RMI (5 s) para callbacks e aguarda os jogadores.

### Pacote `client`

- `ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback` — recebe os snapshots e delega para a `UI` local; responde ao `ping` do servidor.
- `CaixaEstado` — guarda o último estado recebido e acorda (wait/notify) quem espera por um novo, sem *polling*.
- `ConsoleUI` — desenha o tabuleiro 9×9 em caracteres, mostra os movimentos válidos na sua vez, lê comandos (`mover`, `cerca`, `tabuleiro`, `ajuda`, `sair`) e exibe mensagens claras para `JogadaInvalidaException`.
- `BotJogador` — IA usada na validação E2E e no modo demo (`--bot`): anda pelo menor caminho (BFS) e, quando um adversário está mais perto da vitória, coloca a cerca de maior ganho líquido (atraso do adversário − atraso próprio).
- `ClientMain` — faz `lookup`, registra o callback e entra no loop de UI (ou no loop do bot, que age no máximo uma vez por estado recebido).

## 5. Protocolo RMI e fluxo de dados

1. Cliente → `GameServer.registrar(callback, nome)` → servidor guarda o callback, atribui o `idJogador` (1–4), gera o token e devolve a `Sessao`. Nomes são sanitizados (sem caracteres de controle, até 20 caracteres). Um 5º cliente recebe "Sala cheia".
2. No **4º registro**, o servidor inicia a partida e chama `aoIniciarJogo` nos 4 callbacks (exatamente uma vez por cliente).
3. O jogador da vez chama `mover`/`colocarCerca` com sua sessão → o servidor autentica, valida contra a engine → se válido, atualiza o estado, avança o turno e chama `aoAtualizarEstado` nos callbacks conectados (inclusive no ativo, para refletir o novo turno).
4. Jogada inválida ou sessão inválida → o servidor lança `JogadaInvalidaException` de volta **apenas ao próprio cliente** (sem broadcast).
5. Vitória → o servidor marca `FINALIZADO` + `idVencedor`, envia primeiro `aoAtualizarEstado` (tabuleiro final) e depois `aoFinalizarJogo(idVencedor)`, uma única vez para cada cliente.
6. Queda de cliente (falha num callback ou no `ping`) → o jogador é marcado como inativo, sua vez passa a ser pulada e todos recebem o novo estado; se sobrar 1 jogador conectado, ele vence por W.O.

## 6. Decisões de design

- **Registry embutido no servidor**: reduz passos de demonstração e evita conflito com instâncias externas do `rmiregistry`.
- **Broadcast para os 4 clientes, inclusive o ativo**: simplifica o cliente — ele não precisa "adivinhar" o resultado da própria jogada; o estado chega pelo mesmo canal em todos os casos.
- **Erros de jogada como exceção direta, não callback**: o erro pertence a quem jogou; é mais simples e imediato do que um canal assíncrono.
- **Engine desacoplada do RMI**: as regras vivem em `engine` sem nenhuma referência a RMI, o que permite testá-las isoladamente com JUnit — a parte que mais impacta a nota.
- **Desconexão tolerada**: `RemoteException` no broadcast ou no `ping` marca o cliente como desconectado sem derrubar o servidor. Antes da revisão, a queda do jogador **da vez** só era percebida no próximo broadcast — que nunca acontecia, travando a partida; o *ping* periódico resolve isso.
- **Autorização por recurso**: não basta "estar conectado" — cada jogada precisa da sessão do próprio jogador, evitando que um cliente mova o peão de outro.
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

- **Testes JUnit** (pacote `client.ui`): `GeometriaTest` (pixel ↔ casa/aresta), `TabuleiroPanelTest` (clique → `Posicao`/`Cerca` via eventos sintéticos), `PainelJogadoresTest`, `BarraStatusTest` e `GraphicUITest` (construção e troca de estado sem abrir janela).
- No modo manual, as casas legais só são destacadas **na vez do próprio jogador**, cliques fora da vez geram aviso sem chamar o servidor, e as chamadas RMI saem da thread da interface (a janela nunca congela esperando a rede). No modo automático o tabuleiro fica só de exibição.
- Cada borda de chegada é pintada com o tom claro da cor do jogador que precisa alcançá-la; o peão de quem saiu fica esmaecido e o painel mostra "SAIU". A captura está em `docs/img/quoridor-gui.png`.

## 8. Testes e validação

### Unitários (JUnit 5)

- `TabuleiroTest` (20 casos): movimento nas 4 direções, borda, parede bloqueando movimento, pulo reto, pulo diagonal (cerca atrás e **outro peão atrás**), pulo bloqueado, sem destinos repetidos, cerca sobreposta/cruzada/fora do tabuleiro, **vertical entre duas horizontais em linha (permitida)**, cerca que isolaria um jogador (rejeitada), peões não bloqueiam caminho, distância BFS, vitória pelos 4 lados.
- `PartidaTest` (13 casos): jogada fora da vez, turnos em ciclo, vitória, movimento inválido, estado inicial com **5 cercas**, 6ª cerca recusada, vez pulada de desconectado, W.O., entradas nulas, nome sanitizado.
- `GameServerImplTest` (10 casos): tokens distintos, 5º jogador recusado, **jogar no lugar de outro é recusado**, broadcast a todos, estado de espera, queda detectada por ping, W.O., jogador que caiu não volta a jogar, fim avisado uma única vez e após o estado final.
- `BotJogadorTest` (5 casos) e `ConsoleUITest` (2 casos): estratégia do bot, 5 partidas completas simuladas terminando sempre, interpretação de comandos.

### SemSocketTest

Varre todo `src/main/java` e falha se qualquer arquivo contiver `java.net.Socket`, `new Socket` ou `ServerSocket` — garante o requisito "RMI sem socket explícito".

### E2ETest (partida completa)

Sobe **1 servidor + 4 clientes em processos JVM separados** (`ProcessBuilder`) com jogadores automáticos (`--bot`) e joga a partida até o fim:

- Verifica que os 4 jogadores se registraram (`Jogador 4 (...)` no log do servidor).
- Verifica que a partida terminou (`[FIM] vencedor=<1–4>`).
- Verifica que **os 4 clientes receberam o mesmo vencedor** via callback (`FIM vencedor=` no log de cada cliente).

Um segundo cenário (`partidaContinuaQuandoUmClienteCai`) mata à força o processo do Jogador 2 logo após o início e verifica que o servidor detecta a queda, pula a vez dele e a partida termina com outro vencedor.

**Resultado da validação (execução real em 2026-09-23):** os dois cenários E2E passaram; na partida completa foram colocadas exatamente 20 cercas (5 por jogador) e os 4 clientes confirmaram o mesmo vencedor via callback.

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

O projeto entrega um Quoridor distribuído completo e funcional em Java RMI puro: engine com as regras oficiais para 4 jogadores (5 cercas cada, pulos reto e diagonal, cruzamento correto de cercas e caminho garantido por BFS), servidor com registry embutido, autorização por sessão, detecção de queda por ping e broadcast via callback, clientes com interface de console, modo bot e **interface gráfica Swing** (manual por cliques e automática ilustrativa), e uma suíte de testes que cobre unitário, a GUI, ausência de sockets e uma partida E2E completa — atendendo integralmente os requisitos da disciplina.
