# Modelo Conceitual de Dados do AEGIS

## Finalidade

Este modelo descreve conceitos de negócio, responsabilidades, identidade e relações. Intencionalmente, não define SQL DDL, índices nem mapeamentos de framework. Tabelas devem ser introduzidas somente quando um requisito precisar de estado durável; o design físico será validado contra padrões de acesso e documentado por migrações.

## Princípios de modelagem

- PostgreSQL 18.4 é a fonte da verdade transacional conforme o [ADR-002](ADR/ADR-002-postgresql.md); o Catálogo é responsável pelo schema `catalog` e suas migrations Flyway.
- Toda entidade tem um ID estável e opaco; identificadores públicos/de negócio, como SKU e versão da release, são separados.
- Timestamps são instantes UTC. A localização de exibição é responsabilidade do cliente.
- Dinheiro usa semântica decimal fixa e moeda ISO 4217. A Fase 02 é BRL-only, com escala 2 e persistência `NUMERIC(19,2)`.
- Agregados mutáveis usam uma versão para concorrência otimista.
- Desativação/retenção é preferida à exclusão destrutiva quando histórico, auditoria ou rastreabilidade precisam sobreviver.
- Campos sensíveis são minimizados, protegidos e nunca copiados para histórico/evidências sem necessidade.
- Dados binários de objetos pertencem ao object storage; PostgreSQL armazena metadados e referências opacas.
- Avaliações de qualidade retêm a versão da política/fórmula e o snapshot de origem necessários para explicar resultados históricos.

## Resumo de responsabilidades por domínio

| Módulo | Conceitos sob sua responsabilidade |
| --- | --- |
| auth | usuários, roles, permissões, atribuições de roles, sessões/credenciais conforme design futuro |
| catalog | produtos, categorias, associações entre produto e categoria, histórico do produto, intenções de outbox do catálogo |
| media | imagens do produto, tentativas/metadados de processamento de mídia |
| integration | tentativas de publicação/entrega, mapeamentos downstream, registros de idempotência do consumidor |
| quality | releases/candidatos, suítes, casos, execuções, resultados, metadados de evidência, métricas, gates/avaliações, defeitos, achados, decisões |
| audit | eventos de auditoria |

## Entidades de comércio e identidade

### users

Representa uma pessoa ou identidade de serviço aprovada.

Conceitos principais: ID estável, identificador de login/e-mail normalizado e único, nome de exibição, estado ativo, referência de credencial/sessão (não o secret bruto), timestamps de criação/atualização e versão de concorrência. Secrets de autenticação devem ser separados e protegidos conforme o mecanismo de identidade escolhido.

Relações:

- muitos-para-muitos com roles por meio do conceito de atribuição de role;
- referência de ator no histórico do produto, eventos de auditoria e decisões de release;
- pode ser desativado, mas não apagado silenciosamente da atribuição histórica.

### roles

Agrupa permissões nomeadas para RBAC. Conceitos principais: ID estável, nome único, descrição, estado ativo e versão da política. Permissões podem ser modeladas inicialmente como capacidades definidas em código; uma tabela de permissões no banco só será adicionada se gerenciamento dinâmico for necessário.

### categories

Classifica Products. Conceitos mínimos aprovados para a Fase 02: ID estável, nome normalizado de exibição, chave canônica de unicidade separada, estado ACTIVE/INACTIVE, timestamps e versão de concorrência. O nome obrigatório passa por Unicode NFC, remoção de whitespace externo e redução de sequências internas de whitespace a um espaço; a forma resultante, validada com 2–80 caracteres, é preservada para exibição. A chave canônica é essa forma convertida para uppercase com `Locale.ROOT` e é única sem diferenciar maiúsculas/minúsculas.

Category nasce ACTIVE e não possui reativação na Fase 02. Uma Category INACTIVE não recebe novas associações. A desativação é bloqueada quando qualquer Product ACTIVE a referencia e é permitida quando todas as referências existentes pertencem a Products INACTIVE; uma segunda desativação não altera estado nem versão e não cria novo efeito durável. Hierarquia, subcategories e associação many-to-many não são presumidas.

### products

Raiz do agregado de Catálogo. Conceitos da Fase 02: ID estável; SKU obrigatório, normalizado, imutável e único; Name obrigatório; Description obrigatória; preço BRL decimal fixo; estoque inteiro não negativo; exatamente uma Category obrigatória; estado ACTIVE/INACTIVE; timestamps e versão de concorrência.

Product é responsável pelos invariantes das alterações permitidas. Na Fase 02, muitos Products podem referenciar uma Category, e cada Product referencia exatamente uma Category por uma associação obrigatória; não existe tabela many-to-many. Product nasce ACTIVE, pode transitar uma vez para INACTIVE e não pode ser reativado. Uma segunda desativação não altera estado, versão, histórico nem outbox intent. Nenhum estado draft é presumido.

Regras de valor aprovadas:

- SKU remove whitespace externo, converte para uppercase, aceita somente `A-Z`, `0-9`, `.`, `_`, `-` e possui 3–64 caracteres; permanece reservado entre ativos e inativos e nunca é reutilizado na Fase 02;
- Name passa por Unicode NFC, remoção de whitespace externo e redução de sequências internas de whitespace a um espaço; é obrigatório e possui 3–120 caracteres depois dessa normalização;
- Description passa por Unicode NFC, normalização de line endings para LF e remoção somente de whitespace externo; preserva whitespace interno significativo, parágrafos e quebras de linha, não sofre transformação de caixa e possui 1–2000 caracteres depois dessa normalização;
- preço é maior ou igual a zero, usa BRL e escala 2; `float`/`double` são proibidos;
- estoque é inteiro maior ou igual a zero.

### Baseline física aprovada para o Catálogo na Fase 02

O desenho exato será materializado por migrations, mas os seguintes limites já estão decididos:

- schema PostgreSQL `catalog` pertencente ao módulo;
- relação durável de Categories com estado ACTIVE/INACTIVE, nome normalizado para display e chave canônica case-insensitive única;
- relação durável de Products com FK obrigatória para uma única Category;
- SKU canônico com constraint única abrangendo Products ativos e inativos;
- preço `NUMERIC(19,2)`, moeda BRL e checks para preço/estoque não negativos;
- versão para optimistic locking e ETag/If-Match conforme o [ADR-007](ADR/ADR-007-etag-if-match-optimistic-concurrency.md);
- relações append-only de product history e catalog outbox intent escritas na mesma transação de Product.

### product_images

Metadados de mídia associados a um produto. Conceitos principais: ID estável, ID do produto, chave opaca de armazenamento, metadado seguro do nome original do arquivo, media type detectado, tamanho em bytes, checksum, largura/altura, estado de processamento, ordem de exibição, indicador de imagem principal, código do motivo da falha, timestamps e versão.

O conteúdo binário não é armazenado aqui. Um estado pronto significa que a validação/processamento exigido teve sucesso; a associação não torna um objeto publicamente acessível por padrão.

### product_history

Histórico de negócio append-only de alterações materiais do Product. Conceitos principais: ID do histórico, ID do Product, tipo/ID do ator, ação, momento de ocorrência, ID de correlação, fonte e conjunto estruturado seguro de alterações ou snapshot. Antes de Auth, o ator é identificado explicitamente como `LOCAL_PREVIEW`, nunca como usuário autenticado. Ele apoia a rastreabilidade de negócio e é distinto do logging geral de auditoria de segurança.

### audit_events

Registro append-only de ações relevantes para segurança e negócio. Conceitos principais: ID do evento, momento de ocorrência, tipo/ID do ator, ação, tipo/ID do alvo, resultado, código do motivo, ID de correlação/trace, contexto da fonte e metadados sanitizados.

Registros de auditoria não devem armazenar credenciais, tokens, conteúdo completo de arquivos nem corpos arbitrários de solicitações. Retenção e acesso são controlados por política.

A relação com o ator é uma atribuição lógica, não uma permissão para Auditoria consultar Auth durante o append. O evento carrega um snapshot imutável do tipo/ID do ator pelo limite de auditoria; a segurança de uma chave estrangeira entre módulos continua sendo uma decisão de design físico.

## Conceitos de apoio à confiabilidade

Esses conceitos são justificados pelos [requisitos de confiabilidade da integração](REQUIREMENTS.md#integração-externa-int), não pelo desejo de criar tabelas artificiais.

### catalog_outbox_intents

Intenção durável pertencente ao Catálogo para futura publicação de um evento de domínio confirmado. Conceitos principais: ID do evento, tipo, versão do schema, ID/versão do agregado, payload mínimo, momento de ocorrência e IDs de correlação/causalidade. Estado do Product, histórico e essa intenção são escritos atomicamente. A Fase 02 não possui worker, RabbitMQ, tentativa de publicação ou entrega externa; estado de reivindicação/publicação só será acrescentado quando a fase de Integração definir o publicador pela porta pertencente ao Catálogo. Aplicam-se minimização e retenção do payload.

### integration_deliveries

Acompanha o resultado do processamento downstream. Conceitos principais: ID da entrega, ID do evento, destino, estado, número de tentativas, última classificação segura de erro, próxima tentativa, momento da primeira/última tentativa e conclusão.

### idempotency_records

Registra a identidade de uma mensagem consumida ou comando HTTP elegível e o fingerprint do resultado dentro de uma janela de retenção definida. Evita efeitos de negócio duplicados; não promete exactly once global.

## Entidades do domínio de qualidade

### releases

Representa um escopo lógico e versionado de entrega, como v1.0.0. Conceitos principais: ID estável, versão única, título, descrição/escopo, estado do ciclo de vida, timestamps de criação/finalização e versão de concorrência.

Uma release agrupa candidatos e suas decisões; não é em si um build mutável. Uma release não deve alegar prontidão quando o candidato selecionado, a identidade do build ou a evidência exigida forem ambíguos.

### release_candidates

Representa uma tentativa de avaliação de uma release. Conceitos principais: ID do candidato, ID da release, rótulo/sequência do candidato, identidade imutável do build, ambiente-alvo, corte de evidência, estado do ciclo de vida e timestamps de criação/avaliação. Uma release pode ter vários candidatos, mas uma aprovação identifica exatamente um candidato. Um candidato rejeitado ou substituído permanece no histórico.

“Candidato a release” é um conceito de domínio justificado por builds/tentativas de avaliação repetidos; o schema físico pode incorporá-lo inicialmente somente se um candidato por release for imposto explicitamente. Ele nunca deve permitir que um build diferente substitua silenciosamente evidências já avaliadas.

### test_suites

Agrupamento lógico de casos por capacidade, camada ou finalidade da execução. Conceitos principais: ID, referência externa estável, nome, camada/tipo, responsável, estado ativo e versão. A associação a uma suíte não deve ser o único mecanismo de rastreabilidade de requisitos.

### test_cases

Registro de design de teste. Conceitos principais: ID, ID externo estável como TC-API-CAT-001, título, intenção, camada de teste, tags de risco, estado de automação, responsável, estado do ciclo de vida, precondições e referência opcional ao código-fonte. Passos detalhados podem permanecer no código executável ou em um sistema vinculado quando a duplicação se tornaria obsoleta.

Um caso pode cobrir vários requisitos e um requisito pode ter vários casos; isso exige uma associação explícita que contenha o tipo de cobertura quando útil.

### test_runs

Uma sessão de execução de uma fonte aprovada. Conceitos principais: ID da execução, par fonte efetiva/ID de execução da fonte para idempotência, identidade do candidato/build da release, escopo da suíte, ambiente, gatilho, início/fim, status geral, ferramenta/versão e estado da ingestão.

### test_results

Um resultado de caso/cenário dentro de uma execução. Conceitos principais: ID do resultado, ID da execução, ID do caso ou referência estável de teste, status (passed, failed, blocked, skipped, error), duração, tentativa, classificação da falha, resumo seguro e timestamps.

Resultados ignorados, bloqueados e com erro de infraestrutura permanecem distintos de aprovação/reprovação. Resultados de retry não apagam a primeira falha.

### evidence_items

Metadados que apontam para logs, capturas de tela, traces, relatórios ou artefatos. Conceitos principais: ID da evidência, associação a resultado/achado/experimento, tipo, fonte, URI/referência opaca de objeto, checksum, content-type, momento de criação, classe de retenção e classificação de sensibilidade. Se evidência binária usará MinIO ou armazenamento de artefatos da CI é uma decisão em aberto.

### quality_metrics

Medições normalizadas e com timestamp usadas na avaliação. Conceitos principais: ID da métrica, identidade do candidato/build da release, nome, valor, unidade, dimensão/fonte, momento da medição, estado de atualização e proveniência. Exemplos incluem taxa de aprovação, cobertura de requisitos, contagem de defeitos, percentis de latência e taxa de erro.

Séries temporais operacionais brutas de alta cardinalidade permanecem no sistema de observabilidade; somente snapshots/referências relevantes para avaliação devem ser copiados aqui.

### quality_gates

Definição de política versionada. Conceitos principais: ID do gate, código estável, nome, estágio, severidade, configuração da expressão/regra, entradas exigidas, comportamento de bloqueio, janela ativa e versão da política. A política do gate não é sobrescrita silenciosamente.

### gate_evaluations

Resultado imutável da aplicação de uma versão do gate a um snapshot de evidências do candidato. Conceitos principais: ID da avaliação, identidade do candidato/build da release, ID/versão do gate, status (passed, failed, insufficient_evidence, not_applicable, error), momento da avaliação, snapshot/referência de entrada e motivos legíveis por humanos. not_applicable também registra ator autorizado, justificativa, versão da política e referência de auditoria.

### quality_evaluations

Um resultado explicável do Motor de Qualidade. Conceitos principais: ID da avaliação, identidade do candidato/build da release, versão da fórmula, corte/snapshot de evidências, pontuação, nível de risco, recomendação, métricas contribuintes, tratamento de dados ausentes e momento do cálculo.

Esse registro complementa, mas nunca substitui, gate_evaluations individuais.

### defects

Referência normalizada de defeito. Conceitos principais: ID, sistema/referência externa, título, severidade, prioridade, estado do ciclo de vida, componente afetado, responsável, momento de criação/resolução e resumo seguro. Associações vinculam defeitos a requisitos, resultados de testes e releases sem duplicar um tracker externo completo.

### security_findings

Resultado de segurança normalizado. Conceitos principais: ID, fonte efetiva/fingerprint, identidade do candidato/build da release, regra/CWE quando conhecida, severidade, contexto de confiança/explorabilidade, componente/localização, status, primeira/última ocorrência, referência da correção e link de evidência. A política de deduplicação deve considerar a fonte.

### performance_results

Snapshot no nível do cenário. Conceitos principais: ID, identidade do candidato/build da release, cenário, perfil de workload, duração, latências em percentis, throughput, taxa de erro, contexto de recursos, versão da política de limite, resultado e link para evidência do relatório.

### release_decisions

Registro imutável da decisão. Conceitos principais: ID da decisão, ID da release, identidade exata do candidato/build, decisão (approved, blocked, approved_with_exception se a política permitir), ator, momento, justificativa, referência à evidência/avaliação de qualidade e expiração/condições da exceção.

Recomendações são produzidas por máquina; decisões pertencem a humanos. Uma reconsideração cria outro registro atribuído em vez de editar o histórico.

## Associações de rastreabilidade

Requisitos podem inicialmente permanecer na documentação em vez de uma tabela requirements. IDs estáveis ainda são referências de primeira classe. Uma entidade durável de requisitos só é justificada quando o Centro de Controle de Qualidade precisar de ciclo de vida/consultas além de metadados sincronizados.

~~~mermaid
erDiagram
    USERS }o--o{ ROLES : atribuídos
    PRODUCTS }o--o{ CATEGORIES : classificados_como
    PRODUCTS ||--o{ PRODUCT_IMAGES : possui
    PRODUCTS ||--o{ PRODUCT_HISTORY : alterações
    USERS ||--o{ PRODUCT_HISTORY : realiza
    USERS ||--o{ AUDIT_EVENTS : atua_em

    RELEASES ||--o{ RELEASE_CANDIDATES : possui
    RELEASE_CANDIDATES ||--o{ TEST_RUNS : avaliado_por
    TEST_SUITES }o--o{ TEST_CASES : agrupa
    TEST_RUNS ||--o{ TEST_RESULTS : contém
    TEST_CASES ||--o{ TEST_RESULTS : produz
    TEST_RESULTS ||--o{ EVIDENCE_ITEMS : sustenta
    RELEASE_CANDIDATES ||--o{ QUALITY_METRICS : mede
    RELEASE_CANDIDATES ||--o{ GATE_EVALUATIONS : recebe
    QUALITY_GATES ||--o{ GATE_EVALUATIONS : define
    RELEASE_CANDIDATES ||--o{ QUALITY_EVALUATIONS : recebe
    RELEASES }o--o{ DEFECTS : afetada_por
    RELEASE_CANDIDATES ||--o{ SECURITY_FINDINGS : inclui
    RELEASE_CANDIDATES ||--o{ PERFORMANCE_RESULTS : inclui
    RELEASE_CANDIDATES ||--o{ RELEASE_DECISIONS : decidida_por
~~~

Rastreabilidade conceitual de requisitos:

~~~mermaid
flowchart LR
    R["ID do Requisito"] --> C["Caso de Teste"]
    C --> X["Resultado / Execução de Teste"]
    X --> E["Evidência"]
    X --> D["Defeito"]
    R --> D
    X --> L["Candidato / build da release"]
    D --> L
    L --> Q["Avaliação de gate e qualidade"]
    Q --> Decision["Decisão de Release"]
~~~

## Restrições de ciclo de vida

- Product: nasce ACTIVE, pode transitar para INACTIVE, não pode ser reativado na Fase 02 e nunca é excluído fisicamente. Uma segunda desativação é idempotente quanto a estado/efeitos e um futuro estado draft exige novo requisito aprovado.
- Mídia: pending -> processing -> ready ou failed; retry devolve um item com falha elegível a processing sem inventar novo histórico bem-sucedido.
- Release: planned -> open -> finalized/closed. Cada candidato move-se separadamente por collecting evidence -> evaluated -> decided/superseded; reabertura ou substituição é auditada.
- Resultado de teste: imutável após ingestão aceita, exceto por enriquecimento seguro; um resultado corrigido na fonte cria uma nova relação de versão/tentativa.
- Defeito/achado: o fechamento preserva a severidade e o histórico no momento em que uma release foi avaliada.
- Registros de auditoria e avaliação são append-only nos fluxos normais do produto.

## Integridade de dados e concorrência

- Unicidade no banco sustenta SKU normalizado, chave canônica de Category, versão da release, ID do evento e chaves aprovadas de ingestão/idempotência.
- Chaves estrangeiras aplicam relações dentro de um limite de responsabilidade quando o ciclo de vida permite.
- Verificações da aplicação e do banco protegem invariantes críticos numéricos/de estado.
- Verificações de versão otimista no banco e ETag/If-Match no HTTP protegem Product contra atualizações perdidas.
- Criação/mudança de Product e desativação de Category preservam atomicamente a invariável de que nenhum Product ACTIVE referencia uma Category INACTIVE. A estratégia física de locking será selecionada e validada na implementação, sem ser presumida neste modelo conceitual.
- Uma transação do catálogo registra atomicamente estado do produto, histórico e intenção de outbox pertencente ao Catálogo quando exigido.
- Consumidores registram idempotência e resultado de negócio em uma transação quando possível.
- Jobs de reconciliação identificam objetos órfãos, trabalho de outbox travado, execuções incompletas e evidências desatualizadas; não corrigem silenciosamente dados ambíguos.

## Retenção, privacidade e classificação

Antes do uso em produção, toda classe de dados precisa de responsável e cronograma de retenção. Categorias iniciais:

- credenciais/tokens: secret, nunca registrados em log nem armazenados como evidência;
- identidade do usuário: dado pessoal restrito e minimizado;
- produto/catálogo: dado interno/de negócio;
- auditoria/achados de segurança: restritos e sensíveis à integridade;
- evidências de teste: classificadas por conteúdo e redigidas antes de acesso amplo;
- telemetria operacional: identificadores minimizados e retenção/alta cardinalidade limitadas;
- objetos de mídia: direitos e ciclo de vida validados, privados por padrão.

Solicitações de exclusão devem conciliar obrigações de privacidade com retenção legítima de auditoria por meio de política documentada, pseudonimização quando apropriada e análise jurídica antes do uso de dados pessoais reais.

## Questões de design físico

- Escolha de UUID/ULID e codificação de IDs externos.
- Estratégia de schemas dos módulos futuros; o Catálogo já usa `catalog`.
- Estratégia física de locking que comprovará, sob concorrência, a invariável entre Product ACTIVE e Category ACTIVE sem ampliar os limites conceituais aprovados.
- Escopo de moedas posterior à baseline BRL-only; SKU não é reutilizado na Fase 02.
- Representação do histórico: diff estruturado, snapshot selecionado ou híbrido.
- Armazenamento e retenção de evidências binárias.
- Restrições de identidade de release/candidato/build e se a identidade de build se torna uma tabela física separada.
- Normalização de métricas de qualidade versus payload JSON para fontes em evolução.
- Limites de particionamento/arquivamento para auditoria, histórico e dados de execução com base em volume medido.
