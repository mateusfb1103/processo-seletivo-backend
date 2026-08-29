# Desafio Técnico: API de Pedidos

API REST em Spring Boot para criação de pedidos com persistência, publicação e consumo assíncrono via RabbitMQ, indexação em Elasticsearch para busca, e containerização via Docker.

---

## Sumário

- [1. Mapeamento de entidades e regras de negócio](#1-mapeamento-de-entidades-e-regras-de-negócio)
- [2. Explicação da solução](#2-explicação-da-solução)
- [3. Decisões técnicas tomadas](#3-decisões-técnicas-tomadas)
- [4. Instruções de execução](#4-instruções-de-execução)
- [5. Resposta aos pontos de atenção](#5-resposta-aos-pontos-de-atenção)
- [6. Pontos que melhoraria com mais tempo](#6-pontos-que-melhoraria-com-mais-tempo)

---

## 1. Mapeamento de entidades e regras de negócio

O primeiro passo foi mapear as entidades e as regras de negócio que cada uma carrega.

### Pedido

| Campo | Tipo | Regra |
|---|---|---|
| `idPedido` | UUID (PK) | Gerado pela aplicação na criação |
| `idCliente` | UUID | Informado pelo cliente na requisição, sem entidade `Cliente` própria (fora do escopo do desafio) |
| `valorTotal` | BigDecimal | **Sempre calculado pelo backend:** soma dos subtotais dos itens. Nunca aceito diretamente do request |
| `status` | Enum (`RECEBIDO`, `EM_PROCESSAMENTO`) | Definido como `RECEBIDO` na criação |
| `dataCriacao` | LocalDateTime | Preenchida automaticamente no momento da criação |
| `endereco` | Endereco (embutido) | Endereço de entrega do pedido |
| `itens` | Lista de ItemPedido | Relação um-para-muitos, persistida em cascata |

### ItemPedido

| Campo | Tipo | Regra |
|---|---|---|
| `idItemPedido` | UUID (PK) | Gerado pela aplicação |
| `pedido` | FK → Pedido | Item pertence a exatamente um pedido |
| `produto` | FK → Produto | Referência ao catálogo |
| `qtdProduto` | int | Quantidade solicitada, mínimo 1 |
| `valorUnitario` | BigDecimal | **Snapshot** do preço do produto no momento da compra: não muda se o preço do produto mudar depois |
| `subtotal` | BigDecimal | `valorUnitario × qtdProduto`, calculado pelo backend |

### Produto

| Campo | Tipo | Regra |
|---|---|---|
| `idProduto` | UUID (PK) | Pré-cadastrado via `data.sql` (catálogo estático, ver seção 3) |
| `nomeProduto` | String | — |
| `valorProduto` | BigDecimal | Fonte de verdade do preço; nunca aceito do cliente |

### Entrega

| Campo | Tipo | Regra |
|---|---|---|
| `idEntrega` | UUID (PK) | Gerado pelo consumidor da fila |
| `idPedido` | UUID (FK lógica, único) | Referência ao pedido de origem: **não** é um relacionamento JPA, apenas o ID, já que a Entrega é criada de forma assíncrona e desacoplada |
| `enderecoEntrega` | Endereco (embutido) | Cópia do endereço enviado na mensagem |
| `dataCriacao` | LocalDateTime | Preenchida no momento do consumo da mensagem |

Regra de negócio: **uma Entrega só pode ser criada se o Pedido referenciado existir**, e **nunca é duplicada** para o mesmo pedido (idempotência).

### Endereco (`@Embeddable`, reutilizado em Pedido e Entrega)

| Campo | Tipo |
|---|---|
| `logradouro` | String |
| `numero` | int |
| `bairro` | String |
| `cep` | String |
| `cidade` | String |
| `estado` | String |
| `complemento` | String (opcional) |

### DocumentoPedido (documento Elasticsearch, índice `pedidos`)

Desnormalização do `Pedido` para leitura/busca, alimentada de forma assíncrona no mesmo fluxo de criação:

| Campo | Tipo |
|---|---|
| `idPedido` | UUID (`@Id` do documento) |
| `idCliente` | UUID |
| `itens` | Lista de itens (nome, quantidade, valores) |
| `valorTotal` | BigDecimal |
| `status` | Enum |
| `dataCriacao` | LocalDateTime |
| `enderecoEntrega` | Endereço |

---

## 2. Explicação da solução

### Fluxo principal

```
1. POST /pedidos
   └─ PedidoController → PedidoService
        ├─ Busca cada Produto no catálogo (erro 404 se algum idProduto não existir)
        ├─ Calcula subtotal de cada item e valorTotal do pedido
        ├─ Persiste Pedido + ItemPedido no H2 (JPA, cascata)
        ├─ Indexa o pedido no Elasticsearch (índice "pedidos", para busca)
        └─ Publica PedidoMensagem na fila RabbitMQ (pedidos.criados.queue)

2. EntregaListener (consumidor RabbitMQ, assíncrono)
   ├─ Valida que o Pedido referenciado existe
   ├─ Verifica se já existe Entrega para esse pedido (evita duplicidade)
   └─ Persiste a Entrega no H2
```

A criação do pedido e a criação da entrega são propositalmente desacopladas: o `PedidoService` **nunca** chama `EntregaRepository` diretamente, a única forma de uma `Entrega` existir é através do `EntregaListener`, o que comprova estruturalmente que o fluxo passa pelo RabbitMQ.

### Endpoints disponíveis

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/pedidos` | Cria um pedido (dispara o fluxo acima) |
| `GET` | `/pedidos` | Lista todos os pedidos (fonte: H2) |
| `GET` | `/pedidos/{id}` | Busca um pedido por ID (fonte: H2) |
| `GET` | `/pedidos/busca` | Lista todos os pedidos indexados (fonte: Elasticsearch) |
| `GET` | `/pedidos/busca/{id}` | Busca um pedido por ID (fonte: Elasticsearch) |
| `GET` | `/produtos` | Lista o catálogo de produtos disponíveis |
| `GET` | `/entregas` | Lista todas as entregas já processadas |

### Tratamento de erros

Centralizado em `GlobalExceptionHandler`:
- `ProdutoNaoEncontradoException` → `404`
- `PedidoNaoEncontradoException` → `404`
- Falha de validação de campos (`@Valid`) → `400`, com detalhamento por campo
- Qualquer outra exceção não mapeada → `500` genérico, sem vazar detalhe interno

### Resiliência de mensageria

A fila `pedidos.criados.queue` está configurada com:
- **Retry com backoff exponencial** (3 tentativas: 1s, 2s, 4s) antes de desistir de uma mensagem
- **Dead Letter Queue** (`pedidos.criados.dlq`): mensagens que falham em todas as tentativas são movidas para essa fila, em vez de ficarem em loop infinito de redelivery. Pode ser inspecionada manualmente no painel do RabbitMQ

---

## 3. Decisões técnicas tomadas

- **`BigDecimal` para todo valor monetário** (nunca `double`/`float`), evitando erros de arredondamento de ponto flutuante.
- **`valorTotal` e `valorUnitario` sempre calculados no backend.** O `PedidoRequest` não possui nenhum campo de preço, o cliente só informa `idProduto` e `qtdProduto`. O preço é sempre buscado no catálogo (`Produto`).
- **Catálogo de produtos estático via `data.sql`**, sem entidade `Cliente`/CRUD de `Produto` completo. O enunciado pede "código do cliente/produto", não gestão de cadastro, por isso optei por manter o escopo fiel ao pedido, evitando overengineering. Ver lista de produtos e IDs pré-cadastrados na seção 4.
- **DTOs separados das entidades JPA** (`PedidoRequest`/`PedidoResponse` ≠ `Pedido`), para que mudanças de schema de persistência não vazem para o contrato da API.
- **`Entrega` referencia `Pedido` apenas por UUID cru**, não por relacionamento JPA: é criada de forma assíncrona e desacoplada, não haveria necessidade de carregar o agregado `Pedido` inteiro só para popular essa FK.
- **Idempotência no consumidor da fila**: o `EntregaListener` verifica se já existe uma `Entrega` para o `idPedido` recebido antes de criar uma nova, evitando duplicidade em caso de reentrega de mensagem pelo RabbitMQ.
- **Retry + Dead Letter Queue**: mensagens malformadas ou que falham persistentemente não ficam em loop infinito consumindo recursos, são desviadas após 3 tentativas para investigação manual.
- **Elasticsearch como camada de leitura/busca, não fonte de verdade**: o H2 é a fonte transacional, o índice `pedidos` é alimentado de forma best-effort na criação do pedido, para consultas mais flexíveis (`GET /pedidos/busca`).
- **`spring.jpa.defer-datasource-initialization: true`**: garante que o Hibernate crie o schema antes do `data.sql` popular os dados (ordem que não é padrão no Spring Boot).
- **H2 em memória com nome fixo (`testdb`)**: evita URL aleatória a cada restart, permitindo acesso consistente via H2 Console.

---

## 4. Instruções de execução

### Subindo a aplicação (um único comando)

Na raiz do projeto (onde estão `docker-compose.yml` e `Dockerfile`):

```bash
docker-compose up --build -d
```

Isso sobe quatro containers: a aplicação Spring Boot, RabbitMQ, Elasticsearch e Kibana. Aguarde cerca de 30-40 segundos até todos os healthchecks ficarem `healthy`, pode confirmar com:

```bash
docker ps
```

### Parando a stack

```bash
docker-compose down
```

### Acessando a aplicação

| Serviço | URL | Observações |
|---|---|---|
| **API REST** | http://localhost:8080 | Ver tabela de endpoints na seção 2 |
| **RabbitMQ Management** | http://localhost:15672/#/ | Login: `guest` / `guest` |
| **H2 Console** | http://localhost:8080/h2-console/ | Ver dados de conexão abaixo |
| **Elasticsearch** | http://localhost:9200 | Acesso direto à API do Elasticsearch (sem autenticação, ambiente de desenvolvimento) |
| **Kibana** | http://localhost:5601/app/home | Ver instruções de Data View abaixo |

#### Conectando ao H2 Console

Acesse http://localhost:8080/h2-console/ e preencha exatamente:

| Campo | Valor |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:testdb` |
| User Name | `sa` |
| Password | *(deixe em branco)* |

Tabelas disponíveis: `PEDIDOS`, `ITEM_PEDIDO`, `PRODUTOS`, `ENTREGAS`.

#### Visualizando os pedidos indexados no Kibana

1. Acesse http://localhost:5601/app/home
2. Vá em **Discover** (menu lateral)
3. Clique em **Create a data view**
4. Preencha:
   - **Name**: `Pedidos`
   - **Index pattern**: `pedidos*`
   - **Timestamp field**: `dataCriacao`
5. Salve e volte ao Discover — os pedidos criados via `POST /pedidos` aparecem automaticamente ali, já que são indexados no Elasticsearch no momento da criação.

### Catálogo de produtos pré-cadastrado (via `data.sql`)

Como não há endpoint de cadastro de produtos (fora do escopo do desafio — ver seção 3), os produtos abaixo já vêm populados ao subir a aplicação, e podem ser usados diretamente nos testes de criação de pedido:

| ID | Nome | Preço |
|---|---|---|
| `11111111-1111-1111-1111-111111111111` | Notebook Dell Inspiron | R$ 3.500,00 |
| `22222222-2222-2222-2222-222222222222` | Mouse Logitech MX | R$ 250,00 |
| `33333333-3333-3333-3333-333333333333` | Teclado Mecânico Redragon | R$ 180,00 |
| `44444444-4444-4444-4444-444444444444` | Monitor LG UltraWide | R$ 1.800,00 |
| `55555555-5555-5555-5555-555555555555` | Headset HyperX Cloud | R$ 350,00 |

Também disponível via `GET /produtos`.

### Exemplo de requisição para criar um pedido

```bash
POST http://localhost:8080/pedidos 
body: JSON
  {
    "idCliente": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "itens": [
      { "idProduto": "11111111-1111-1111-1111-111111111111", "qtdProduto": 2 }
    ],
    "enderecoEntrega": {
      "logradouro": "Rua das Flores",
      "numero": 123,
      "bairro": "Centro",
      "cep": "86000-000",
      "cidade": "Londrina",
      "estado": "PR",
      "complemento": "Apto 45"
    }
}
```

Resposta esperada: `201 Created`, com `valorTotal` calculado automaticamente (R$ 7.000,00, no exemplo acima).

Um script PowerShell (`testar-fluxo.ps1`) com uma sequência completa de testes (produtos, criação de pedido, casos de erro, verificação de entrega) está disponível na raiz do projeto, para rodar:
```bash
.\testar-fluxo.ps1
```

### Rodando os testes

```bash
./mvnw test
```

Cobre as regras de negócio principais de `PedidoService`, `BuscaPedidoService` e `EntregaListener` (cálculo de valores, produto/pedido inexistente, idempotência do consumidor).

---

## 5. Resposta aos pontos de atenção

| Item | Status |
|---|---|
| Criação de pedido com persistência | Implementado |
| Publicação em fila após criação | Implementado, com retry e Dead Letter Queue |
| Consumidor da fila criando `Entrega` | Implementado com validação de pedido existente e idempotência |
| Containerização via `docker-compose up` (comando único) | Implementado: sobe app, RabbitMQ, Elasticsearch e Kibana |
| Testes unitários das regras de negócio | Implementado (testes unitários com JUnit 5 + Mockito) |
| **Diferencial:** busca de pedidos com Elasticsearch | Implementado: indexação assíncrona na criação, endpoints `GET /pedidos/busca` e `GET /pedidos/busca/{id}`, visualização via Kibana |
---

## 6. Pontos que melhoraria com mais tempo

- **Pipeline de CI (GitHub Actions):** rodar `./mvnw test` automaticamente a cada push/PR, e opcionalmente buildar a imagem Docker como validação adicional.
- **Testes de integração** (`@SpringBootTest` + `MockMvc`, possivelmente com Testcontainers para RabbitMQ/Elasticsearch reais), cobrindo o fluxo ponta a ponta (`POST /pedidos` → banco → fila → `Entrega`), além dos testes unitários já existentes.
- **Padrão Outbox** para a publicação no RabbitMQ: hoje, `criarPedido` é uma única transação cobrindo `save` + indexação Elasticsearch + publish RabbitMQ — se o RabbitMQ estiver indisponível no momento da publicação, a transação inteira reverte (o pedido nem é salvo). O padrão *Transactional Outbox* desacoplaria essas etapas, entregando maior tolerância a falhas.
- **Consumidor dedicado para a DLQ**, com alerta/log estruturado quando uma mensagem cai ali definitivamente, em vez de exigir inspeção manual do painel do RabbitMQ.
- **`MapStruct`** para as conversões DTO↔entidade (hoje feitas manualmente em `PedidoService`/`EntregaListener`), reduzindo boilerplate e a duplicação atual entre os métodos `toEndereco`/`toEnderecoDTO` presentes em mais de uma classe.
- **Cliente e Produto como entidades completas** (com endpoints de cadastro), caso o escopo real do sistema crescesse.
- **Paginação** nos endpoints de listagem (`GET /pedidos`, `GET /entregas`, `GET /pedidos/busca`), que hoje retornam a coleção completa.
- **Autenticação/autorização:** seria o próximo passo natural antes de qualquer uso real.
- **Observabilidade:** correlação de logs entre a criação do pedido e o consumo assíncrono (ex: um `correlationId` propagado na mensagem RabbitMQ), facilitando rastrear um pedido específico através do fluxo assíncrono em ambiente com múltiplas mensagens concorrentes.
