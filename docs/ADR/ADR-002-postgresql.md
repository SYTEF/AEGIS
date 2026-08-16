# ADR-002 — PostgreSQL para a persistência do Catálogo

- Status: Accepted
- Data: 2026-08-16
- Responsáveis pela decisão: NEXUS / responsável pelo repositório
- Requisitos relacionados: REQ-CAT-001 a REQ-CAT-012, REQ-INT-006, NFR-DATA-001, NFR-PORT-001
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

A Fase 02 introduz o primeiro domínio real e durável do AEGIS. Product exige unicidade de SKU inclusive entre registros inativos, dinheiro com precisão decimal, estoque não negativo, concorrência otimista e persistência atômica do agregado, histórico e intenção de outbox. Esses comportamentos dependem de constraints, transações e semântica de concorrência do banco que uma implementação apenas em memória não representa com fidelidade.

O AEGIS também precisa permanecer reproduzível para contribuidores, CI e demonstrações locais. A decisão de persistência deve equilibrar fidelidade técnica com um caminho de inicialização simples, sem antecipar mensageria ou uma stack completa de infraestrutura.

## Direcionadores da decisão

- Integridade de SKU, dinheiro, estoque e relações do Catálogo.
- Atomicidade de Product, histórico e intenção de outbox.
- Testes reais de constraints, migrações e concorrência.
- Reprodutibilidade local e na CI.
- Compatibilidade com o Monólito Modular do [ADR-001](ADR-001-modular-monolith.md).
- Simplicidade operacional proporcional à Fase 02.

## Opções consideradas

### Persistência em memória primeiro

Reduziria o setup inicial, mas adiaria constraints, migrações, transações e concorrência reais. Criaria uma implementação descartável e permitiria falsa confiança em invariantes centrais.

### H2 como substituto local/de teste

Ofereceria inicialização rápida, mas suas diferenças de dialeto, tipos, constraints e concorrência não comprovariam o comportamento PostgreSQL. Manter dois bancos aumentaria a matriz e poderia mascarar defeitos.

### PostgreSQL desde o primeiro domínio real

Introduz custo moderado de container e configuração, mas valida desde o início o sistema de persistência que sustenta as garantias do produto.

## Decisão

A Fase 02 usará **PostgreSQL 18.4** como fonte da verdade transacional do Catálogo.

- O Catálogo será responsável pelo schema `catalog`, suas tabelas e suas migrations.
- Migrations serão SQL versionadas e aplicadas pelo Flyway; alterações corretivas serão forward-only, sem reescrever uma migration já aplicada.
- A versão do PostgreSQL é fixada em `18.4` para desenvolvimento local, integração e CI.
- O futuro Docker Compose usará a imagem oficial do PostgreSQL 18.4. Seu digest exato será resolvido e fixado quando o artefato Compose for criado; este ADR não inventa um digest antes de existir o artefato verificável.
- Docker Compose fornecerá somente a dependência PostgreSQL necessária ao desenvolvimento local nesta fase.
- Um PostgreSQL instalado separadamente continuará sendo uma alternativa documentada; Docker não será exigido como única forma de executar o backend.
- Testes de integração usarão PostgreSQL real por Testcontainers.
- H2 não será introduzido.
- Product, product history e catalog outbox intent serão persistidos atomicamente em uma transação pertencente ao Catálogo.
- Dados seed serão opcionais, habilitados explicitamente apenas no perfil local e idempotentes. Seed não fará parte das migrations de produção nem será necessário para que testes passem.
- RabbitMQ, worker e publicação externa permanecem ausentes. A intenção de outbox é somente estado durável pertencente ao Catálogo na Fase 02.

## Consequências

### Positivas

- Constraints e tipos reais protegem invariantes críticos.
- Migrações passam a fazer parte da evolução revisável do produto.
- Concorrência e rollback podem ser validados no mesmo banco usado pela aplicação.
- A demonstração de portfólio inclui persistência durável e reproduzível.
- Não existe divergência H2/PostgreSQL para diagnosticar.

### Negativas

- Desenvolvimento local e integração exigem uma instância PostgreSQL disponível.
- Testes de integração serão mais lentos que testes puramente em memória.
- Docker/Testcontainers acrescentam requisitos de recursos e disponibilidade no job de integração.
- O schema e as migrations exigem disciplina de compatibilidade e recuperação.

### Neutras

- Testes unitários de domínio continuam sem banco.
- Docker Compose não containeriza automaticamente backend ou frontend.
- Um único banco lógico não permite acesso direto entre módulos; responsabilidade continua definida pelo ADR-001.

## Segurança e operação

- A porta local do PostgreSQL deve ficar limitada a loopback quando precisar ser publicada no host.
- Credenciais de desenvolvimento são inertes e separadas de qualquer secret real; arquivos locais com secrets não são versionados.
- Liveness não depende do banco. Readiness considera PostgreSQL e conclusão das migrations porque o Catálogo não opera com segurança sem sua fonte de verdade.
- Logs e erros não expõem SQL com valores, credenciais, hostnames internos nem payloads de Product.
- Reset de volume deve ser documentado como operação destrutiva e nunca executado implicitamente.

## Validação

A decisão será validada por:

1. migration de um banco vazio até a versão atual;
2. constraints reais para SKU, dinheiro, estoque, estado e relações aprovadas;
3. testes concorrentes de unicidade e optimistic locking;
4. rollback integral quando Product, histórico ou outbox intent falhar;
5. execução local documentada com Docker Compose e alternativa PostgreSQL externa;
6. uso consistente do PostgreSQL 18.4 no Compose, Testcontainers e CI;
7. digest da imagem oficial resolvido e fixado no momento em que o Compose for criado;
8. integração em CI Linux com Testcontainers;
9. ausência de H2 no grafo de dependências.

## Condições para revisitar

Revisitar somente se PostgreSQL impedir um requisito mensurado, se o custo local deixar de ser proporcional ou se um módulo exigir isolamento de dados que o schema atual não consiga preservar. Preferência por outra tecnologia, sem problema concreto, não é evidência suficiente.
