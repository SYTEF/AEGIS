# ADR-007 — ETag/If-Match e concorrência otimista do Catálogo

- Status: Accepted
- Data: 2026-08-16
- Responsáveis pela decisão: NEXUS / responsável pelo repositório
- Requisitos relacionados: REQ-CAT-002, REQ-CAT-003, REQ-CAT-004, REQ-CAT-005, NFR-DATA-001, NFR-COMP-001
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

Recursos mutáveis do Catálogo serão editáveis por uma interface web e por clientes HTTP. Duas leituras podem observar a mesma versão e tentar gravar alterações diferentes. Sem uma precondição explícita, a última escrita poderia sobrescrever silenciosamente dados confirmados pela primeira.

O modelo de persistência possui uma versão otimista, mas o contrato HTTP também precisa expressar qual representação o cliente editou. A decisão deve distinguir ausência de precondição de uma precondição stale e oferecer ao frontend uma experiência recuperável de conflito.

## Direcionadores da decisão

- Impedir lost update sem lock pessimista de longa duração.
- Usar semântica HTTP explícita e interoperável.
- Manter detalhes de persistência fora do comando HTTP.
- Permitir uma experiência de conflito clara no frontend.
- Produzir respostas Problem Details estáveis e testáveis.

## Opções consideradas

### Versão somente no corpo

É simples de serializar, mas mistura representação de domínio com precondição HTTP e torna menos evidente para intermediários/clientes que a escrita é condicional.

### Last-write-wins

Rejeitada porque permite perda silenciosa de alterações e viola NFR-DATA-001.

### Lock pessimista

Rejeitado como padrão de interação HTTP porque manteria contenção sem uma sessão transacional contínua entre leitura humana e atualização.

### ETag e If-Match apoiados por optimistic locking

Selecionada por tornar a versão observada uma precondição HTTP e ainda usar a proteção transacional do banco.

## Decisão

- GET individual de Product ou Category mutável retorna um ETag forte e opaco que representa a versão atual do recurso.
- `POST /api/v1/products` retorna `Location`, a representação criada e seu ETag.
- `POST /api/v1/categories` retorna `Location`, a representação criada e seu ETag.
- PUT de Product exige `If-Match` com o ETag obtido pelo cliente. A Fase 02 não oferece PUT de Category.
- Operações de desativação de Product ou Category também exigem `If-Match`.
- Ausência de `If-Match` retorna HTTP `428 Precondition Required` com Problem Details e código estável.
- Um ETag que não corresponde à versão atual retorna HTTP `412 Precondition Failed`; não retorna `409` para esse caso.
- O banco usa optimistic locking para impedir a gravação concorrente mesmo se houver corrida entre validação e commit.
- A representação pode expor `version` para diagnóstico, mas `If-Match` é a autoridade do contrato de escrita da Fase 02.
- `PUT` é o único contrato de atualização de Product na Fase 02. `PATCH` permanece fora até existir necessidade concreta de atualização parcial.
- SKU é imutável e não faz parte dos campos alteráveis pelo `PUT`.
- Uma desativação de Product já inativo não altera estado, versão, histórico nem outbox intent.
- Uma desativação de Category já inativa não altera estado nem versão e não produz novo efeito durável.
- Criação ou mudança de Product ACTIVE e desativação de Category devem preservar a invariável concorrente: nenhuma transação pode confirmar um Product ACTIVE associado a uma Category INACTIVE. A estratégia física de locking será escolhida e comprovada na implementação; este ADR não a antecipa.

As precondições são avaliadas antes do comando. Portanto, um retry de desativação com um ETag consumido pela primeira execução pode receber `412`; isso não viola a idempotência dos efeitos, pois nenhuma segunda mudança, histórico ou intenção de outbox é produzida. O cliente deve recarregar a representação atual.

## Contrato de erro

| Situação | HTTP | Código estável inicial |
| --- | ---: | --- |
| `If-Match` ausente | 428 | `CATALOG_PRECONDITION_REQUIRED` |
| ETag stale ou optimistic lock perdido | 412 | `CATALOG_PRODUCT_VERSION_CONFLICT` ou `CATALOG_CATEGORY_VERSION_CONFLICT` |
| Category inexistente | 404 | `CATALOG_CATEGORY_NOT_FOUND` |
| Product inexistente | 404 | Código específico de Product definido no contrato OpenAPI |

Problem Details inclui o correlation ID, mas não revela a versão atual, dados concorrentes ou detalhes de persistência. A representação mais recente é obtida por novo GET explícito.

## Consequências

### Positivas

- Lost update é rejeitado de forma determinística.
- O contrato separa ausência de precondição de conflito stale.
- O frontend pode preservar os dados digitados e oferecer recarregamento deliberado.
- A versão interna pode evoluir sem se tornar uma convenção numérica obrigatória do cliente.

### Negativas

- Clientes precisam preservar e enviar o ETag.
- Cache, mocks e testes devem modelar headers, não apenas corpos JSON.
- Um retry mutável não pode ser cego; o cliente precisa interpretar `412`.

### Neutras

- Coleções não precisam fornecer um ETag individual confiável para edição; o cliente obtém a representação atual por GET individual antes de alterar.
- ETag não substitui idempotência de criação nem controle de autorização.

## Experiência obrigatória do frontend

- O formulário preserva a entrada local quando recebe `412`.
- A mensagem explica que o Product mudou desde a leitura.
- O usuário pode recarregar a versão mais recente; o frontend não sobrescreve automaticamente a alteração concorrente.
- Mutations não usam retry automático.
- Sucesso atualiza o ETag armazenado antes de uma nova ação.

## Validação

1. GET individual e criação retornam ETag.
2. PUT/deactivation sem `If-Match` retornam `428` sem efeito.
3. ETag stale retorna `412` sem sobrescrever dados.
4. Duas transações concorrentes não confirmam versões conflitantes.
5. Corridas entre criação/mudança de Product e desativação de Category nunca confirmam Product ACTIVE associado a Category INACTIVE.
6. Segunda desativação de Product não gera histórico/outbox adicional; segunda desativação de Category não gera novo efeito durável.
7. Testes frontend cobrem preservação do formulário e recuperação explícita.
8. OpenAPI, testes de API e tipos do frontend concordam sobre headers e respostas.
