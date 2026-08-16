# Especificação Conceitual da API do AEGIS

## Estado

Os contratos de negócio deste documento permanecem em nível de design e ainda não possuem OpenAPI executável. Somente os contratos operacionais da Fase 01 estão implementados. Detalhes dos demais endpoints podem ser refinados durante sua fase do roadmap, mas alterações devem permanecer alinhadas aos [requisitos](REQUIREMENTS.md), à política de segurança e às regras de compatibilidade. Um documento OpenAPI executável deve tornar-se a fonte do contrato quando a primeira API pública de negócio for implementada.

## Convenções

- Caminho-base: /api/v1.
- Formato: JSON, salvo quando um endpoint tratar explicitamente conteúdo binário/multipart.
- Transporte: HTTPS fora do desenvolvimento local.
- Autenticação: mecanismo bearer/sessão a ser selecionado por ADR; endpoints protegidos sempre aplicam autorização no servidor.
- Content-type: respostas JSON bem-sucedidas usam application/json; respostas de erro usam application/problem+json; upload usa multipart/form-data controlado ou um design de upload assinado selecionado posteriormente.
- Timestamps: instantes UTC no padrão ISO 8601, por exemplo 2026-08-16T15:00:00Z.
- IDs: strings opacas; clientes não devem inferir ordenação ou tipo a partir delas.
- Dinheiro: { "amount": "19.90", "currency": "BRL" }; valores decimais são serializados como strings para preservar a precisão.
- Campos desconhecidos da solicitação: rejeitados em comandos sensíveis para segurança, salvo se uma política explícita de compatibilidade os permitir.
- Correlação: para solicitações aceitas pelo conector e que entram na cadeia Servlet/Spring, aceitar um X-Correlation-ID válido ou gerar um e retornar o ID efetivo no header da resposta e no contexto diagnóstico.
- Versionamento: alterações incompatíveis da API exigem novo caminho/media type principal e aviso de migração; campos opcionais aditivos normalmente são compatíveis.

### Contratos operacionais da Fase 01

Esses contratos estão implementados e cobertos por testes na Fase 01. Endpoints operacionais ficam fora do caminho de negócio /api/v1.

| Endpoint | Finalidade | Contrato |
| --- | --- | --- |
| GET /actuator/health/liveness | Indicar apenas se o processo está vivo | 200 com UP mínimo; não deve consultar dependências externas nem expor configuração |
| GET /actuator/health/readiness | Indicar se a aplicação pode receber tráfego com segurança | 200 quando pronta, 503 quando não pronta; regras de dependência evoluem somente quando dependências existirem |
| GET /actuator/info | Retornar metadados de build não sensíveis | Expõe somente service.name, service.applicationVersion, build.version e build.commitSha quando AEGIS_COMMIT_SHA contiver 7..64 caracteres hexadecimais |

Respostas de info/health nunca devem expor secrets, dumps de variáveis de ambiente, credenciais, caminhos internos, configuração bruta, URLs de dependências ou stack traces. A Fase 01 não tem banco de dados, broker, object store nem serviço downstream, portanto readiness não deve inventar verificações de dependência.

### Contrato de ID de correlação

- Nome do header: X-Correlation-ID.
- Um valor recebido só é aceito quando tem comprimento de 1..128 caracteres e cada caractere corresponde à allowlist [A-Za-z0-9._-].
- Exatamente um valor recebido é avaliado sem `trim`; valores múltiplos são inválidos.
- Um valor ausente, vazio, grande demais ou inválido é substituído por um UUID v4 gerado pelo servidor; o valor rejeitado nunca é devolvido nem registrado literalmente.
- O ID efetivo aparece no header da resposta, nos logs estruturados e no corpo de erro Problem Details.
- Um ID de correlação é apenas diagnóstico: não é autenticação, autorização, idempotência nem prova de unicidade, e clientes podem legitimamente reutilizá-lo em um fluxo lógico.
- A propagação downstream deve preservar o valor canônico limitado e impedir injeção em header/log.

Essas garantias começam depois que o servidor HTTP aceita a requisição e a encaminha à cadeia Servlet/Spring. HTTP malformado, percent encoding rejeitado pelo connector ou headers recusados antes do filtro podem receber a resposta mínima do container sem Problem Details nem `X-Correlation-ID`. A Fase 01 documenta esse limite e não customiza o Tomcat para substituí-lo.

## Autenticação e autorização

| Acesso | Significado |
| --- | --- |
| Público | Nenhuma sessão de usuário exigida, mas limites de taxa e controles de entrada ainda se aplicam |
| Autenticado | Identidade ativa válida exigida |
| Permissão | Identidade ativa mais capacidade nomeada no servidor, como catalog:write |

Falhas de autorização usam 401 quando a identidade está ausente/inválida e 403 quando uma identidade conhecida não possui permissão. As respostas não devem revelar se recursos protegidos e inacessíveis existem.

Vocabulário inicial de permissões (sujeito ao ADR de autenticação):

- catalog:read, catalog:write, catalog:history:read;
- media:read, media:write;
- quality:read, quality:write, quality:ingest, quality:policy:admin, release:decide;
- audit:read, faultlab:operate, admin:users, admin:roles.

As roles mínimas ADMIN, QUALITY_MANAGER, OPERATOR e VIEWER são definidas em [REQUIREMENTS.md](REQUIREMENTS.md#matriz-conceitual-mínima-de-roles). quality:policy:admin é uma permissão distinta e nunca é implícita em quality:write.

## Contratos comuns de resposta

### Metadados de recurso

Recursos mutáveis expõem id, createdAt, updatedAt e version quando relevante. Comandos de atualização fornecem a versão esperada por If-Match ou um campo da solicitação; a fase de implementação deve escolher um padrão consistente.

### Resposta de coleção

~~~json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "sort": ["name,asc"]
}
~~~

Índices de página começam em 0. O tamanho padrão é 20, o máximo é 100 e a ordenação deve incluir um critério estável de desempate. Parâmetros inválidos ou sem limite retornam 400.

### Resposta de erro

~~~json
{
  "type": "urn:aegis:problem:validation-error",
  "title": "Falha na validação da solicitação",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/api/v1/products",
  "correlationId": "01J...",
  "timestamp": "2026-08-16T15:00:00Z",
  "errors": [
    { "field": "sku", "code": "REQUIRED", "message": "SKU é obrigatório." }
  ]
}
~~~

O media type da resposta é application/problem+json. O formato segue a semântica Problem Details e acrescenta um code estável da aplicação. detail e mensagens de campo são seguros para clientes; stack traces, SQL, caminhos, hostnames internos e secrets são excluídos.

`instance` usa somente uma representação limitada e sanitizada do path: query string, matrix parameters e caracteres de controle são removidos; uma representação inválida recua para `/` sem refletir o URI bruto.

### Códigos de status comuns

| Status | Significado |
| --- | --- |
| 200 | Leitura/atualização/comando bem-sucedido com corpo na resposta |
| 201 | Recurso criado; Location o identifica |
| 202 | Aceito para processamento assíncrono; o recurso de status é retornado |
| 204 | Operação bem-sucedida sem corpo |
| 400 | Sintaxe malformada, parâmetros não suportados ou falha de validação |
| 401 | Autenticação ausente, expirada ou inválida |
| 403 | Identidade autenticada, mas sem permissão |
| 404 | Recurso ausente ou ocultado intencionalmente |
| 409 | Conflito de unicidade, transição de estado, idempotência ou concorrência |
| 412 | Precondição If-Match falhou, se a concorrência por ETag for selecionada |
| 413 | Upload/corpo excede o tamanho permitido |
| 415 | Media type não suportado ou divergente do conteúdo |
| 422 | Comando sintaticamente válido, mas semanticamente inaceitável, quando diferenciado de 400 |
| 429 | Limite de taxa excedido; incluir orientação segura de retry |
| 500 | Erro inesperado do servidor com ID de correlação |
| 502/503/504 | Falha/indisponibilidade/timeout de dependência quando o contrato síncrono depender dela |

Clientes devem depender de status e code estável, não do texto do erro.

## Contratos de autenticação

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /auth/login | Autenticar usando o mecanismo de credencial selecionado | Público | 200 com resumo de sessão/token; 400, 401, 429 |
| POST /auth/logout | Invalidar a sessão/token atual quando suportado | Autenticado | 204; 401 |
| POST /auth/refresh | Rotacionar/renovar uma sessão elegível | Contexto válido de refresh/sessão | 200; 401, 409, 429 |
| GET /auth/me | Retornar identidade, roles e permissões efetivas atuais | Autenticado | 200; 401 |
| POST /admin/users | Criar usuário inativo ou ativo conforme política | admin:users | 201; 400, 401, 403, 409 |
| POST /admin/users/{userId}/disable | Desativar usuário e revogar todas as sessões ativas | admin:users | 200; 400, 401, 403, 404, 409 para proteção do último administrador |
| PUT /admin/users/{userId}/roles/{role} | Atribuir uma role permitida idempotentemente | admin:roles | 204; 400, 401, 403, 404, 409 |
| DELETE /admin/users/{userId}/roles/{role} | Remover uma role sob proteções do último administrador | admin:roles | 204; 400, 401, 403, 404, 409 |

Solicitação conceitual de login:

~~~json
{ "username": "catalog.admin@example.test", "password": "<redacted>" }
~~~

Corpo conceitual de sucesso:

~~~json
{
  "user": { "id": "usr_...", "displayName": "Administrador do Catálogo" },
  "roles": ["CATALOG_ADMIN"],
  "permissions": ["catalog:read", "catalog:write"],
  "expiresAt": "2026-08-16T16:00:00Z"
}
~~~

A resposta final de token/cookie, a proteção CSRF e o design de refresh dependem do ADR de autenticação. Falhas de login usam texto genérico e não podem revelar existência ou estado da conta.

Desativação de usuário e qualquer alteração crítica de role/permissão revogam as sessões ativas afetadas depois que a mudança for aceita de forma durável. Criação/desativação de usuário, atribuição/remoção de role e rejeição do último administrador exigem aceitação de auditoria fail-closed. O mecanismo exato de sessão permanece uma decisão do ADR-006.

## Contratos de produto

A coluna de acesso abaixo representa o contrato seguro de destino. Durante a **PRÉVIA DE DESENVOLVIMENTO LOCAL** da Fase 02, autenticação/RBAC reais e as evidências relacionadas de 401/403 permanecem pendentes; endpoints de mutação não devem ser expostos externamente. A Fase 03 deve fechar essa lacuna antes que o Gate de Exposição Externa possa passar.

### Representação de produto

~~~json
{
  "id": "prd_...",
  "sku": "AEG-001",
  "name": "Produto de Referência",
  "description": "Um produto do catálogo.",
  "price": { "amount": "129.90", "currency": "BRL" },
  "stock": 12,
  "status": "ACTIVE",
  "categories": [{ "id": "cat_...", "name": "Referência" }],
  "links": { "images": "/api/v1/products/prd_.../images" },
  "version": 3,
  "createdAt": "2026-08-16T14:00:00Z",
  "updatedAt": "2026-08-16T15:00:00Z"
}
~~~

Esta é a representação de produto pertencente ao Catálogo e não contém metadados pertencentes à Mídia. Um cliente pode seguir o link de imagens, ou uma futura resposta de composição de aplicação/consulta pode combinar modelos de leitura de Catálogo e Mídia acima dos dois módulos. Catálogo nunca chama Mídia para construir esse recurso.

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /products | Criar um produto | catalog:write | 201; 400, 401, 403, 409 por SKU duplicado |
| GET /products/{productId} | Obter visão autorizada do produto | catalog:read | 200; 401, 403, 404 |
| GET /products | Pesquisar/filtrar/paginar produtos | catalog:read | 200; 400, 401, 403 |
| PUT /products/{productId} | Substituir campos permitidos do produto com verificação de concorrência | catalog:write | 200; 400, 401, 403, 404, DECISÃO EM ABERTO: 409 ou 412 |
| PATCH /products/{productId} | Atualizar parcialmente campos explicitamente suportados | catalog:write | 200; 400, 401, 403, 404, DECISÃO EM ABERTO: 409 ou 412 |
| POST /products/{productId}/deactivation | Desativar com motivo explícito | catalog:write | 200; 400, 401, 403, 404, 409 por estado inválido |
| GET /products/{productId}/history | Paginar histórico de alterações do produto | catalog:history:read | 200; 400, 401, 403, 404 |
| GET /products/{productId}/integration-status | Inspecionar sincronização downstream | catalog:read | 200; 401, 403, 404 |

GET /products aceita inicialmente:

- query: texto limitado de busca por nome/SKU;
- categoryId;
- status (ACTIVE, INACTIVE quando autorizado);
- page, size, sort usando uma allowlist.

A criação aceita Idempotency-Key opcional se a implementação oferecer cache seguro dos resultados do comando. SKU duplicado sempre é conflito, independentemente da chave de idempotência.

## Contratos de categoria

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /categories | Criar categoria | catalog:write | 201; 400, 401, 403, 409 por nome normalizado/slug duplicado |
| GET /categories/{categoryId} | Obter categoria | catalog:read | 200; 401, 403, 404 |
| GET /categories | Pesquisar/paginar categorias | catalog:read | 200; 400, 401, 403 |
| PUT /categories/{categoryId} | Atualizar categoria com verificação de concorrência | catalog:write | 200; 400, 401, 403, 404, DECISÃO EM ABERTO: 409 ou 412 |
| POST /categories/{categoryId}/deactivation | Desativar conforme política do catálogo | catalog:write | 200; 400, 401, 403, 404, 409 quando o uso impedir |

Endpoints de hierarquia de categorias são intencionalmente ausentes até existir um requisito de hierarquia.

## Contratos de mídia

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /products/{productId}/images | Validar e aceitar imagem para processamento assíncrono | media:write | 202; 400, 401, 403, 404, 413, 415, 422 |
| GET /products/{productId}/images | Listar metadados de imagens do produto | media:read | 200; 401, 403, 404 |
| GET /products/{productId}/images/{imageId} | Obter metadados e referência autorizada de entrega | media:read | 200; 401, 403, 404, 409 quando não pronta |
| DELETE /products/{productId}/images/{imageId} | Remover associação e agendar limpeza segura do objeto | media:write | DECISÃO EM ABERTO: 202 ou 204; 401, 403, 404, 409 |
| POST /products/{productId}/images/{imageId}/reprocessing | Repetir processamento elegível que falhou | media:write | 202; 400, 401, 403, 404, 409 por estado inválido |
| PUT /products/{productId}/images/order | Reordenar imagens atomicamente | media:write | 200; 400, 401, 403, 404, DECISÃO EM ABERTO: 409 ou 412 |

Resposta de upload:

~~~json
{
  "id": "img_...",
  "productId": "prd_...",
  "status": "PENDING",
  "statusUrl": "/api/v1/products/prd_.../images/img_...",
  "correlationId": "01J..."
}
~~~

O servidor gera o nome do objeto no armazenamento. As verificações incluem permissão autenticada, tamanho, assinatura/tipo do conteúdo decodificado, dimensões seguras, limites de descompressão e política de processamento. Credenciais/caminhos públicos de object storage nunca são retornados.

## Contratos de ingestão e consulta de qualidade

Endpoints de ingestão destinam-se a ferramentas/CI aprovadas. Exigem identidade dedicada com escopo, controles de taxa/tamanho, versão de schema e identidade de idempotência baseada na fonte **efetiva** mais a identidade de execução na fonte.

O campo source enviado pelo cliente é uma alegação, não uma autoridade. O servidor deriva a fonte efetiva da identidade de serviço autenticada ou valida a alegação contra a allowlist dessa identidade. Evidência aceita vincula, quando disponível, a fonte efetiva ao repositório, workflow, commit SHA, identidade imutável do build e versão do schema. Um payload não ganha confiança ao nomear uma fonte privilegiada.

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /quality/test-runs | Ingerir execução e lote de resultados | quality:ingest | 202 aceito/seguro para duplicidade; 400, 401, 403, 409, 413, 422 |
| GET /quality/test-runs/{runId} | Obter execução normalizada e status da ingestão | quality:read | 200; 401, 403, 404 |
| GET /quality/test-runs | Filtrar execuções por release, camada, status ou fonte | quality:read | 200; 400, 401, 403 |
| POST /quality/security-findings | Ingerir relatório de segurança normalizado/bruto suportado | quality:ingest | 202; 400, 401, 403, 409, 413, 422 |
| POST /quality/performance-results | Ingerir métricas de cenário e referência de relatório | quality:ingest | 202; 400, 401, 403, 409, 413, 422 |
| POST /quality/defects | Registrar/sincronizar referência de defeito | quality:write | DECISÃO EM ABERTO: 200 ou 201; 400, 401, 403, 409 |
| GET /quality/traceability/requirements/{requirementId} | Retornar casos, execuções, evidências, defeitos e releases vinculados | quality:read | 200; 401, 403, 404 |

Envelope conceitual de execução de testes:

~~~json
{
  "schemaVersion": "1.0",
  "source": "github-actions/playwright",
  "sourceRunId": "123456789",
  "releaseVersion": "v0.6.0",
  "candidateId": "cand_...",
  "repository": "SYTEF/AEGIS",
  "workflow": "quality-validation",
  "build": { "commitSha": "<full-sha>", "artifactId": "backend-..." },
  "environment": "ci",
  "startedAt": "2026-08-16T14:00:00Z",
  "finishedAt": "2026-08-16T14:04:00Z",
  "results": [
    {
      "testCaseId": "TC-API-CAT-001",
      "status": "PASSED",
      "durationMs": 245,
      "attempt": 1,
      "evidence": []
    }
  ]
}
~~~

A resposta de ingestão retorna effectiveSource resolvida no servidor e identifica registros aceitos, rejeitados e duplicados sem converter falsamente ingestão parcial em sucesso completo. Tamanho máximo do lote e semântica de atomicidade devem ser fixados no contrato de implementação.

A proteção contra replay usa a fonte efetiva autenticada, o ID de execução da fonte, a versão do schema e um fingerprint canônico do payload dentro de uma janela versionada de retenção. Um replay exato retorna o resultado idempotente registrado; reutilizar a mesma identidade com build/schema/payload conflitante retorna 409. Envios antigos ou fora da janela exigem um caminho explicitamente autorizado de importação/reconciliação e são auditados. Assinatura/atestação de artefatos pode fortalecer a proveniência futuramente, mas não é exigida pela fundação.

## Contratos de release

| Método e endpoint | Finalidade | Acesso | Respostas esperadas |
| --- | --- | --- | --- |
| POST /releases | Criar release/versão lógica e seu escopo | quality:write | 201; 400, 401, 403, 409 por versão duplicada |
| GET /releases/{releaseId} | Obter metadados da release | quality:read | 200; 401, 403, 404 |
| GET /releases | Paginar/filtrar releases | quality:read | 200; 400, 401, 403 |
| PATCH /releases/{releaseId} | Atualizar campos permitidos antes da finalização com verificação de concorrência | quality:write | 200; 400, 401, 403, 404, DECISÃO EM ABERTO: 409 ou 412 |
| POST /releases/{releaseId}/candidates | Registrar build imutável como candidato da release | quality:write | 201; 400, 401, 403, 404, 409 por identidade de build duplicada/conflitante |
| GET /releases/{releaseId}/candidates/{candidateId} | Obter candidato/build e metadados de corte de evidência | quality:read | 200; 401, 403, 404 |
| GET /releases/{releaseId}/candidates/{candidateId}/quality-summary | Obter atualização das evidências e gates aplicáveis; pontuação/risco/recomendação podem ser NOT_APPLICABLE | quality:read | 200; 401, 403, 404, 409 quando avaliação indisponível |
| POST /releases/{releaseId}/candidates/{candidateId}/evaluations | Avaliar corte fixo de evidências usando política versionada ativa | quality:write | DECISÃO EM ABERTO: 201 ou 202; 400, 401, 403, 404, 409, 422 |
| GET /releases/{releaseId}/candidates/{candidateId}/traceability | Obter resumo de cobertura e links ausentes | quality:read | 200; 401, 403, 404 |
| POST /releases/{releaseId}/candidates/{candidateId}/decisions | Registrar decisão final autorizada e justificativa para o build exato | release:decide | 201; 400, 401, 403, 404, 409, 422 |

Resumo conceitual de qualidade:

~~~json
{
  "release": { "id": "rel_...", "version": "v1.0.0" },
  "candidate": { "id": "cand_...", "label": "rc.2", "buildId": "..." },
  "evidenceCutoff": "2026-08-16T15:00:00Z",
  "freshness": "CURRENT",
  "dimensions": {
    "functional": "PASS",
    "api": "PASS",
    "integration": "PASS",
    "e2e": "PASS",
    "security": "PASS",
    "performance": "PASS"
  },
  "qualityScore": { "value": 94, "maximum": 100, "formulaVersion": "1.0" },
  "risk": { "level": "LOW", "policyVersion": "1.0", "reasons": [] },
  "hardGates": [],
  "recommendation": "APPROVE",
  "finalDecision": null,
  "missingEvidence": []
}
~~~

Um finalDecision ausente nunca equivale a aprovação. Um gate crítico com falha força recomendação BLOCK mesmo que qualityScore.value seja alta.

Antes da maturidade do Motor de Qualidade, o resumo retorna qualityScore, risk e recommendation como NOT_APPLICABLE com a referência da política de aplicabilidade; não retorna zeros sintéticos nem infere aprovação. As definições de release, candidato e build estão no [glossário](GLOSSARY.md).

## Idempotência e retries

- GET, PUT com a mesma representação completa e semântica de DELETE devem ser idempotentes quando o domínio permitir.
- Comandos POST elegíveis aceitam Idempotency-Key; escopo, retenção e fingerprint do corpo da chave são documentados por endpoint.
- Reutilizar uma chave com corpo diferente retorna 409 IDEMPOTENCY_CONFLICT.
- Um cliente que recebeu timeout pode repetir somente conforme a política do endpoint; o processamento no servidor deve impedir efeitos duplicados.
- O consumo de mensagens é at-least-once e usa a identidade do evento para idempotência.

## Controles de taxa, tamanho e abuso

Limites exatos exigem baseline, mas todo endpoint deve ter limites de tamanho de solicitação/corpo, paginação, timeout e concorrência/recursos. Login, upload, busca custosa, ingestão e controles do Laboratório de Falhas recebem proteção dedicada. Respostas 429 podem incluir Retry-After sem revelar capacidade interna.

## Requisitos de segurança da API

- Validar nas camadas de transporte, sintaxe, semântica e domínio.
- Vincular autorização do recurso ao objeto solicitado, não somente ao endpoint.
- Usar allowlists para campos de ordenação, content-types e transições de estado.
- Impedir mass assignment por modelos explícitos de comando.
- Não expor entidades JPA/domínio diretamente como modelos da API.
- Registrar resultados seguros de segurança com correlação; nunca registrar credenciais, tokens bearer brutos, conteúdo completo de upload nem corpos arbitrários de evidências.
- Proteger alterações de estado com cookies do navegador contra CSRF se cookies forem selecionados.
- Publicar headers de segurança e política de CORS a partir de allowlist explícita.

## Evolução e testes do contrato

- OpenAPI torna-se executável e revisada com a primeira implementação da API pública de negócio em `/api/v1`; os endpoints operacionais da Fase 01 não antecipam esse artefato.
- Testes de contrato de consumidor/provedor protegem o limite do Mock do Centro de Vendas e schemas de eventos.
- Verificações de compatibilidade retroativa executam antes do merge quando contratos mudarem.
- Exemplos tornam-se fixtures de teste somente após validação; exemplos documentais não devem conter secrets reais nem dados pessoais.
- Depreciação registra substituição, impacto, caminho de migração e marco de remoção.

## Decisões em aberto sobre semântica das respostas

Status codes alternativos nas tabelas de endpoints estão explicitamente não resolvidos e devem ser definidos antes da aceitação da OpenAPI executável:

- concorrência otimista: 409 Conflict versus 412 Precondition Failed, em coordenação com o ADR-007;
- exclusão/limpeza assíncrona: 202 Accepted versus 204 No Content concluído;
- comportamento de criar-ou-sincronizar: 200 OK existente versus 201 Created novo;
- criação síncrona de avaliação com 201 Created versus assíncrona com 202 Accepted;
- limite consistente entre 400 Bad Request e 422 Unprocessable Content;
- se Product precisa de PUT e PATCH ou se um único contrato de atualização é suficiente.

Nenhuma implementação pode selecionar o status que apenas faça um teste passar. A decisão deve atualizar exemplos, testes e OpenAPI consistentemente.

## Questões de contrato em aberto

- Sessão por cookie versus access/refresh tokens para a arquitetura do navegador.
- ETag/If-Match versus versão explícita no corpo para concorrência otimista.
- Uma categoria versus várias categorias no comando inicial de produto.
- Upload multipart direto versus upload controlado e assinado de objeto.
- Upload binário de evidência versus referências a artefatos externos.
- Atomicidade dos lotes de ingestão e tamanhos máximos de relatório.
- Semântica de exceção para uma release com bloqueio crítico.
