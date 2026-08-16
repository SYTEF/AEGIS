# Registros de Decisão Arquitetural

## Finalidade

Um Registro de Decisão Arquitetural (Architecture Decision Record — ADR) registra uma decisão importante, seu contexto, alternativas e consequências. ADRs preservam por que o AEGIS escolheu uma abordagem para que futuros contribuidores possam avaliá-la sem reconstruir uma discussão perdida.

ADRs são apropriados quando uma decisão é difícil de reverter, afeta vários módulos ou atributos de qualidade, introduz custo operacional, define contrato durável ou rejeita uma alternativa plausível por um motivo não óbvio. Detalhes locais e rotineiros de implementação não precisam de ADR.

## Processo de decisão

1. Identificar o problema concreto, restrições, requisitos e atributos de qualidade afetados.
2. Descrever opções viáveis, incluindo “não fazer nada/usar o design atual”.
3. Comparar benefícios, custos, riscos, testabilidade, segurança, observabilidade, operações e migração/rollback.
4. Propor uma decisão com consequências mensuráveis.
5. Obter a revisão humana/técnica exigida antes da implementação.
6. Marcar o status e vincular o ADR à documentação/plano afetado.
7. Se a decisão mudar, criar um ADR substituto; não reescrever o histórico aceito, exceto por pequenas correções.

## Nomenclatura e status

Arquivos usam ADR-NNN-short-kebab-title.md, por exemplo ADR-001-modular-monolith.md.

Status permitidos:

- Proposed: em revisão; não autoriza implementação;
- Accepted: aprovado e esperado como orientação para a implementação;
- Rejected: considerado, mas não selecionado, com justificativa;
- Deprecated: deixou de ser recomendado, mas ainda pode existir;
- Superseded by ADR-NNN: substituído por decisão posterior.

Números nunca são reutilizados. O índice abaixo é atualizado sempre que um ADR é adicionado ou muda de status.

## Template de ADR

~~~markdown
# ADR-NNN — Título da decisão

- Status: Proposed
- Data: YYYY-MM-DD
- Responsáveis: <roles/nomes>
- Requisitos relacionados: REQ-...
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

Qual problema concreto estamos resolvendo? Quais restrições e evidências existem?

## Direcionadores da decisão

- Necessidade do produto
- Simplicidade e manutenibilidade
- Segurança, testabilidade e observabilidade
- Confiabilidade/performance
- Operação local e custo

## Opções consideradas

### Opção A

Benefícios, custos e riscos.

### Opção B

Benefícios, custos e riscos.

## Decisão

A opção selecionada e seu escopo exato.

## Consequências

Consequências positivas, negativas e neutras, incluindo migração/rollback.

## Validação

Como a decisão será testada ou medida e quando deverá ser revisitada.
~~~

## ADRs futuros propostos

Os títulos a seguir são candidatos, não decisões aceitas:

| Candidato | Problema a decidir | Momento esperado |
| --- | --- | --- |
| ADR-003 RabbitMQ | Por que mensageria assíncrona é necessária, topologia, retry/DLQ e alternativas | Antes da fase de integração |
| ADR-004 MinIO | Necessidade de object storage, ciclo de vida, configuração local e alternativas | Antes da fase de mídia |
| ADR-005 Playwright | Papel dos testes no navegador, estrutura do projeto, matriz de navegadores e alternativas | Antes da primeira suíte E2E do frontend |
| ADR-006 Autenticação | Modelo de cookie/token/provedor, ciclo de sessão e consequências de segurança | Antes da fase de autenticação |
| ADR-008 Fórmula de qualidade e versionamento de política | Dimensões da pontuação, evidência ausente, gates e governança | Antes da implementação do Motor de Qualidade |
| ADR-009 Backends de telemetria de observabilidade | Coleta/armazenamento, perfil local, retenção e custo | Antes da fase completa de observabilidade |

A numeração dos candidatos pode mudar até que um arquivo seja criado. Não crie todos os ADRs antecipadamente; crie um quando a decisão estiver pronta e for necessária.

## Critério mínimo para tecnologias

Um ADR que proponha Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch ou microservices deve responder:

1. Qual problema concreto e mensurado existe?
2. Por que o Monólito Modular/stack atual não consegue resolvê-lo de forma mais simples?
3. Quais novos modos de falha, superfície de segurança e custo operacional surgem?
4. Como reprodutibilidade local, testes e observabilidade permanecerão confiáveis?
5. Qual é o caminho de migração e rollback?

Sem resposta convincente e aprovação explícita, a decisão é **Rejected**.

## Índice de ADRs

| ADR | Status | Decisão |
| --- | --- | --- |
| [ADR-001 — Monólito Modular](ADR-001-modular-monolith.md) | Accepted | Começar com módulos explícitos no mesmo processo e exigir evidência mensurada antes da distribuição |
| [ADR-002 — PostgreSQL para a persistência do Catálogo](ADR-002-postgresql.md) | Accepted | Usar PostgreSQL 18.4, schema `catalog`, Flyway, Docker Compose local e Testcontainers, sem H2 |
| [ADR-007 — ETag/If-Match e concorrência otimista do Catálogo](ADR-007-etag-if-match-optimistic-concurrency.md) | Accepted | Exigir `If-Match` em PUT/desativação, retornar 428 quando ausente e 412 quando stale |
| [ADR-010 — Arquitetura do frontend do AEGIS Commerce](ADR-010-frontend-architecture.md) | Accepted | Construir uma SPA React/TypeScript com Vite, boundaries por feature e Design Quality Gate |
| [ADR-011 — OpenAPI como contrato entre backend e frontend](ADR-011-openapi-contract.md) | Accepted | Versionar OpenAPI, validar drift e gerar somente tipos TypeScript, sem SDK runtime |

ADRs futuros permanecem candidatos até que sua decisão seja necessária e revisada.
