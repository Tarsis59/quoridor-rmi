# Design — Quoridor Distribuído (RMI) para Sistemas Distribuídos

- **Data:** 2026-08-28
- **Status:** Aprovado pelo usuário
- **Tecnologia:** Java 21 + Maven, RMI puro (`java.rmi`)

## Objetivo

Implementar o jogo **Quoridor para 4 jogadores** de forma distribuída usando **Java RMI puro**, onde um servidor central mantém o estado do jogo e 4 clientes (um por jogador) interagem via chamadas RMI síncronas, com **callback** para broadcast de estado. Satisfaz o requisito "não usar Socket" (RMI usa sockets internamente de forma transparente, mas o código nunca instancia `new Socket(...)`).

## Arquitetura

- **RMI Registry embutido** no processo do servidor, porta 1099 (`LocateRegistry.createRegistry(1099)`).
- Cada cliente faz `lookup` do servidor e registra seu **objeto remoto de callback** (`registrar(callback, nome)`).
- Chamadas de jogo (`mover`, `colocarCerca`) são **RMI síncronas cliente → servidor**.
- Após jogada válida, o servidor notifica todos os 4 clientes via callback (`aoAtualizarEstado`).

## Componentes e responsabilidades

### Pacote `common` (compartilhado server/client)
- `Posicao` — `record` com `linha` (0–8) e `coluna` (0–8).
- `Orientacao` — enum `HORIZONTAL`/`VERTICAL`.
- `Cerca` — `Posicao` base + `Orientacao`.
- `EstadoJogo` — snapshot `Serializable`: posições dos 4 peões, cercas colocadas, cercas restantes por jogador, jogador da vez, status (`AGUARDANDO`/`EM_ANDAMENTO`/`FINALIZADO`), vencedor.
- `JogadaInvalidaException` — exception customizada `Serializable` (RMI exige que exceptions remotas sejam serializáveis).
- `GameServer` — interface `Remote`:
  - `int registrar(ClientCallback cb, String nomeJogador)`
  - `void mover(int idJogador, Posicao destino)`
  - `void colocarCerca(int idJogador, Cerca cerca)`
  - `EstadoJogo obterEstado()`
- `ClientCallback` — interface `Remote`:
  - `void aoIniciarJogo(EstadoJogo estado)`
  - `void aoAtualizarEstado(EstadoJogo estado)`
  - `void aoFinalizarJogo(int idVencedor)`

### Pacote `engine` (puro, sem RMI — testável)
- `Orientacao`, `Cerca`, `Tabuleiro`, `Peao`, `Partida` (orquestração) e `RegrasQuoridor`.
- Regras:
  1. Movimento ortogonal de 1 casa (se livre e sem cerca entre as casas).
  2. Pulo sobre peão adjacente (reto se casa atrás livre; senão lateral nas 2 casas perpendiculares).
  3. Colocação de cerca válida (dentro do tabuleiro, sem sobreposição, sem cruzamento sobre uma cerca existente).
  4. **BFS de caminho garantido** para os 4 jogadores após simular a cerca (senão, jogada rejeitada).
  5. Condição de vitória por borda (J1→linha 0, J2→linha 8, J3→coluna 0, J4→coluna 8).
- Cada jogador começa com **10 cercas**.

### Pacote `server`
- `GameServerImpl` — encapsula a `Partida`, controla fila de turnos (só aceita jogada do jogador da vez), mantém lista de callbacks, faz broadcast via callback após jogada válida.
- `ServerMain` — cria registry embutido, faz `bind`, aguarda 4 registros, dispara `aoIniciarJogo`.

### Pacote `client`
- `ClientCallbackImpl` — recebe snapshots e atualiza a UI local (com `synchronized` para thread-safety com a thread de leitura de comandos).
- `ConsoleUI` — desenha tabuleiro 9x9 em caracteres, lê comandos (`mover`, `cerca`), exibe erros de `JogadaInvalidaException`.
- `ClientMain` — `lookup`, registro com callback, loop de UI.

## Fluxo de dados

1. Cliente → `GameServer.registrar` → servidor guarda callback e atribui `idJogador` (1–4).
2. 4º registro → servidor monta `EstadoJogo` inicial e chama `aoIniciarJogo` nos 4 callbacks.
3. Jogador da vez chama `mover`/`colocarCerca` → servidor valida contra a engine → se válido, atualiza estado, avança turno, chama `aoAtualizarEstado` nos 4 callbacks (inclusive o ativo, para refletir o novo turno).
4. Jogada inválida → servidor lança `JogadaInvalidaException` de volta ao próprio cliente (sem callback).
5. Vitória → servidor marca `FINALIZADO` + `idVencedor`, chama `aoFinalizarJogo` nos 4 callbacks.

## Tratamento de erros / bordas

- **Fora de turno:** servidor rejeita com `JogadaInvalidaException` (nunca confiar só no cliente).
- **Cliente desconecta:** exceções `RemoteException` durante broadcast são capturadas e o cliente é marcado como desconectado, sem derrubar o servidor.
- **Partida travada por desconexão:** se o jogador da vez desconectou, o servidor avança para o próximo jogador conectado após um tempo configurável.

## Testes

- **Unitários (JUnit 5):** movimento ortogonal (todas as direções + borda), pulo reto/lateral/bloqueado, cerca sobreposta/cruzada/fora, cerca que isola caminho (rejeitada), condição de vitória para os 4 lados.
- **Integração/E2E:** classe `E2ETest` que sobe o servidor e 4 clientes **em processos separados** via `ProcessBuilder`, com jogadas programadas determinísticas até um vencedor, verificando que todos os clientes recebem o estado final.

## Não-escopo (YAGNI)

- Sem persistência, sem autenticação, sem rede/multimáquina (só `localhost`), sem GUI gráfica (console primeiro; possível GUI opcional depois).

## Estrutura de pastas

```
quoridor-distribuido/
├── pom.xml
├── src/main/java/
│   ├── common/   (interfaces remotas + modelos serializáveis)
│   ├── engine/   (regras do jogo — puro, sem RMI)
│   ├── server/   (GameServerImpl, ServerMain)
│   └── client/   (ClientCallbackImpl, ConsoleUI, ClientMain)
├── src/test/java/
│   ├── engine/   (testes JUnit da engine)
│   └── e2e/      (teste de partida completa)
├── README.md
└── docs/relatorio.md
```
