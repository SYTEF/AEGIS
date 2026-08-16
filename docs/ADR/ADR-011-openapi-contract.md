# ADR-011 — OpenAPI como contrato entre backend e frontend

- Status: Accepted
- Data: 2026-08-16
- Responsáveis pela decisão: NEXUS / responsável pelo repositório
- Requisitos relacionados: REQ-CAT-001 a REQ-CAT-009, NFR-COMP-001, NFR-TEST-001, NFR-TEST-002
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

A Fase 02 possui duas trilhas implementadas em toolchains distintas: uma API Java/Spring Boot e uma SPA React/TypeScript. Tipos manuais duplicados podem divergir em campos, enums, headers, paginação e Problem Details. Ao mesmo tempo, gerar um SDK completo ou adotar consumer-driven contracts antes de haver consumidores independentes adicionaria complexidade sem benefício proporcional.

O contrato precisa ser revisável antes do código cliente, verificável na CI e útil para gerar tipos sem esconder o comportamento HTTP importante, especialmente ETag/If-Match e respostas de erro.

## Direcionadores da decisão

- Uma fonte de contrato versionada e legível por ferramentas.
- Detecção de drift entre API e frontend antes do merge.
- Tipos TypeScript sem client runtime ou abstração excessiva.
- Compatibilidade explícita de respostas, headers e erros.
- Evolução incremental dentro de `/api/v1`.

## Opções consideradas

### DTOs e tipos TypeScript manuais

Rejeitada como proteção única. A duplicação não oferece um gate automático para alterações incompatíveis.

### Gerar um SDK completo

Adiada. Um SDK adicionaria templates, convenções e código gerado que não são necessários para a pequena API da Fase 02.

### Consumer-driven contracts

Adiada até existir um consumidor ou uma cadência de entrega independente que justifique contratos por consumidor.

### OpenAPI versionado com tipos gerados

Aceita. Mantém um artefato explícito, permite validação do provedor e gera somente a informação estática necessária ao frontend.

## Decisão

- Um documento OpenAPI versionado sob `contracts/openapi/` será a fonte executável do contrato de negócio `/api/v1` quando a implementação da Fase 02 começar.
- [API_SPEC.md](../API_SPEC.md) continua documentando contexto, políticas e endpoints futuros; em caso de divergência sobre um endpoint implementado, o OpenAPI revisado e seus testes são a autoridade executável.
- O backend produzirá sua descrição OpenAPI por springdoc e a CI detectará divergência em relação ao artefato versionado.
- O frontend gerará tipos TypeScript por `openapi-typescript` a partir do mesmo artefato.
- Não será gerado um client runtime. Um adaptador `fetch` pequeno continuará explícito quanto a URL, método, headers, ETag, correlation ID e Problem Details.
- O contrato inclui schemas de sucesso/erro, parâmetros, paginação, enums, status HTTP, `Location`, `ETag`, `If-Match`, `X-Correlation-ID` e media types aplicáveis.
- Exemplos só se tornam fixtures após validação; não conterão secrets nem dados pessoais.
- A CI executará uma verificação determinística de drift e geração de tipos. Saída gerada não poderá ser editada manualmente para esconder divergência.
- Alterações incompatíveis exigem estratégia explícita de versão/migração. Campos opcionais aditivos continuam sujeitos a compatibilidade do consumidor.

O artefato OpenAPI ainda não é criado por este ADR porque a tarefa atual formaliza decisões e não implementa contratos executáveis. Sua criação pertence ao primeiro bloco de contrato da Fase 02, antes dos endpoints e do client frontend.

## Consequências

### Positivas

- Backend e frontend compartilham schemas e enums verificáveis.
- Headers de concorrência e Problem Details deixam de depender de conhecimento informal.
- Reviews conseguem avaliar mudanças do contrato em um diff dedicado.
- O frontend recebe tipagem sem acoplamento a um SDK gerado.

### Negativas

- O artefato precisa ser atualizado deliberadamente junto das mudanças da API.
- A CI precisa normalizar/validar a saída para evitar diffs não determinísticos.
- OpenAPI não comprova sozinho semântica de negócio; testes continuam obrigatórios.

### Neutras

- Schemas de eventos e contratos do mock externo permanecem separados e entram em fases posteriores.
- O uso futuro de contract testing adicional não é proibido, mas exige um problema concreto.

## Compatibilidade e governança

- A revisão deve distinguir mudança aditiva, alteração incompatível e correção de documentação.
- Remoção, mudança de tipo, novo campo obrigatório ou alteração semântica de status/header é incompatível salvo migração aprovada.
- O texto de mensagens de erro não é contrato estável; `status`, `code`, campos estruturados e media type são.
- Nenhum teste ou tipo gerado será enfraquecido apenas para aceitar drift.

## Validação

1. OpenAPI valida estruturalmente e contém somente os endpoints implementados/autorizados.
2. A descrição produzida pelo backend não diverge do contrato versionado.
3. Tipos frontend são gerados de forma reproduzível.
4. Testes HTTP cobrem status, headers e schemas críticos.
5. Mudança incompatível falha antes do merge ou possui migração/versionamento aprovado.
6. O client frontend não redefine manualmente DTOs cobertos pelo contrato.
