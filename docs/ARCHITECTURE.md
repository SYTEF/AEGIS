# Arquitetura do AEGIS

## Estado e contexto

Este documento define a arquitetura inicial pretendida. Ele não afirma que os componentes já existem. A direção de Monólito Modular (Modular Monolith) está aceita no [ADR-001](ADR/ADR-001-modular-monolith.md). Alterações materiais exigem análise de impacto e, quando difíceis de reverter ou amplamente consequentes, um [Registro de Decisão Arquitetural](ADR/README.md).

O AEGIS deve oferecer suporte a um fluxo real de catálogo e a um fluxo de qualidade/release, mantendo-se compreensível e reproduzível localmente. O principal risco arquitetural é adicionar complexidade de sistemas distribuídos pela aparência no portfólio, em vez de atender a uma necessidade do produto. O estilo inicial é, portanto, um **Monólito Modular**, com mensageria assíncrona apenas em limites que se beneficiem de desacoplamento e recuperação.

## Princípios e restrições

1. Simplicidade antes de complexidade.
2. Monólito Modular primeiro; uma extração exige pressão mensurada e um ADR.
3. Engenharia de Qualidade faz parte da arquitetura.
4. Segurança, observabilidade e testabilidade são projetadas desde o início.
5. Limites internos têm responsabilidade e direção de dependência explícitas.
6. Consistência transacional é preferida dentro de um módulo; fluxos entre limites expõem estado recuperável.
7. Presumem-se entrega at-least-once de mensagens e consumidores idempotentes.
8. Falhas são explícitas, observáveis e limitadas.
9. Automação apoia, mas não substitui, análise de risco e testes exploratórios.
10. Tecnologia é selecionada para resolver um problema concreto.

Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch e microservices são proibidos até que um ADR aprovado responda: **Qual problema concreto e mensurado estamos resolvendo, por que o design atual é insuficiente e qual custo operacional estamos aceitando?**

## Contexto do sistema

~~~mermaid
flowchart LR
    Actor["Usuários de catálogo e qualidade"]
    UI["Aplicação Web AEGIS"]
    API["Monólito Modular AEGIS"]
    DB[("PostgreSQL")]
    MQ[("RabbitMQ")]
    OBJ[("MinIO")]
    Worker["Processo worker de integração"]
    External["Mock do Centro de Vendas Externo"]
    Sources["CI, testes e fontes de segurança e performance"]
    Obs["Stack de observabilidade"]

    Actor --> UI --> API
    API --> DB
    API --> OBJ
    API --> MQ
    MQ --> Worker --> External
    Sources --> API
    API -. telemetria .-> Obs
    Worker -. telemetria .-> Obs
~~~

O worker de integração pode inicialmente compartilhar a mesma base de código e artefato de deploy, executando como um perfil de processo distinto. Isso preserva a reutilização entre módulos sem fingir que existem serviços independentes.

## Contêineres lógicos

| Contêiner | Responsabilidade | Direção inicial |
| --- | --- | --- |
| Aplicação Web | Fluxos acessíveis para Commerce e Centro de Controle de Qualidade | React + TypeScript, após aprovação da fundação |
| Aplicação/API | Contratos HTTP, composição de consultas, lógica de negócio modular e ingestão | Java + Spring Boot, Monólito Modular |
| Worker de integração | Consumir trabalho de saída, chamar o mock downstream, repetir e informar status | Mesma base de código do backend, com perfil de runtime separado se útil |
| PostgreSQL | Fonte da verdade transacional, constraints, histórico, metadados de auditoria e outbox | Um banco lógico com schemas/tabelas pertencentes aos módulos |
| RabbitMQ | Entrega assíncrona durável nos limites de integração/mídia | Adicionado somente em sua fase do roadmap |
| MinIO | Imagens de produtos e futuras evidências em objetos quando apropriado | Referências e checksums mantidos no PostgreSQL |
| Mock do Centro de Vendas Externo | Comportamentos controlados de contrato, latência e falha | Nunca tratado como confiável/interno |
| Stack de observabilidade | Coleta e visualização de telemetria | OpenTelemetry, Prometheus e Grafana em fases posteriores |

## Limites dos módulos

### auth

É responsável por usuários, roles, permissões, credenciais/sessões e política de autorização. Expõe identidade do ator autenticado e verificações de permissão; outros módulos não devem ler diretamente tabelas de credenciais.

### catalog

É responsável por produtos, categorias, SKU, preço, estoque, estado do produto, histórico do produto e intenção transacional durável de eventos de domínio do catálogo. Uma transação do catálogo pode persistir atomicamente o agregado, histórico e sua intenção de outbox. Catálogo não depende de Mídia nem realiza chamadas HTTP downstream dentro de transações do catálogo.

### media

É responsável por objetos de mídia, metadados, validação, estado de processamento, interação segura com armazenamento e associações de imagens do produto. Pode usar a interface pública de Produto do Catálogo para validar a referência de um produto; Catálogo nunca chama Mídia. Mídia trata arquivos recebidos como hostis.

### integration

É responsável por orquestração da publicação, mapeamento de payload externo, tentativas de entrega, retry, idempotência, tratamento downstream e replay. **Não** é responsável nem escreve a intenção de evento do catálogo. Reivindica/marca intenções pendentes somente por uma porta de publicação pertencente ao Catálogo e nunca acessa diretamente a persistência do Catálogo.

### quality

É responsável por releases, candidatos, catálogo/resultados de testes, metadados de evidência, defeitos, achados, métricas, políticas versionadas, avaliações de gates, pontuações, risco e recomendações. Não autoriza deploy automaticamente.

Para evitar um único “objeto deus” interno, quality mantém sublimites conceituais internos e continua sendo um módulo:

- **catálogo e ingestão de evidências:** suítes, casos, execuções, resultados, proveniência e metadados de evidência;
- **evidências da release:** releases, candidatos, defeitos, achados, resultados de performance e rastreabilidade;
- **política e avaliação:** gates versionados, fórmulas de pontuação, políticas de risco e avaliações determinísticas;
- **decisão:** apresentação da recomendação, decisão final autorizada e exceções.

Esses são limites internos de pacote/responsabilidade, não serviços separados nem bancos de propriedade independente.

### audit

É responsável por registros de auditoria append-only, política de retenção/redação e consultas autorizadas. Módulos enviam fatos seguros de auditoria por um limite explícito de append que carrega snapshots de ator e correlação; Auditoria não chama Auth ou o módulo de origem para reconstruí-los e, portanto, não introduz ciclo de dependência.

### Limite de composição de aplicação/consulta

Esse limite fica acima das APIs de aplicação dos módulos e constrói representações de leitura voltadas ao cliente que precisam de dados de mais de um módulo. Por exemplo, uma visão combinada de produto pode ler independentemente dados de Produto do Catálogo e metadados de Mídia. Ele não contém regras de domínio, não é responsável por estado transacional e não deve permitir que Catálogo dependa de Mídia.

## Regras de dependência

~~~mermaid
flowchart TD
    Adapters["Adaptadores HTTP, mensageria e armazenamento"] --> App["Serviços de aplicação do módulo"]
    App --> Domain["Modelo de domínio e políticas do módulo"]
    Adapters --> Ports["Portas pertencentes ao módulo"]
    Ports --> App

    Catalog["catalog"] --> AuthAPI["API pública de autorização de auth"]
    Composition["composição de aplicação/consulta"] --> CatalogQuery["API de consulta de catalog"]
    Composition --> MediaQuery["API de consulta de media"]
    Media["media"] --> CatalogAPI["API pública de produto de catalog"]
    Integration["integration"] --> CatalogEvents["porta de publicação de catalog"]
    Quality["quality"] --> AuthAPI
    Modules["todos os módulos de negócio"] --> AuditAPI["API de append de audit"]
~~~

- A lógica de domínio não deve depender de detalhes de framework, HTTP, broker ou object storage.
- Acesso entre módulos usa uma API pública explícita de aplicação ou evento publicado; acesso direto ao repositório/tabela de outro módulo é proibido.
- Dependências cíclicas entre módulos são proibidas. Utilitários técnicos compartilhados permanecem pequenos e não contêm regras de negócio.
- Catálogo nunca depende de Mídia. Representações para clientes que combinem seus dados são montadas somente pelo limite de composição de aplicação/consulta.
- A porta de publicação pertencente ao Catálogo é a única interface pela qual a Integração pode reivindicar ou atualizar o estado das intenções de outbox do catálogo.
- Chaves estrangeiras podem existir entre limites de módulos somente quando responsabilidade, ciclo de vida e acoplamento forem deliberados e documentados. IDs estáveis mais APIs de módulo são preferidos quando a independência de ciclo de vida importa.
- Chamadas internas de módulos são síncronas e em processo por padrão. Mensageria dentro do monólito exige necessidade concreta de recuperação ou desacoplamento.

## Fluxos principais

### Alteração do catálogo e sincronização externa

~~~mermaid
sequenceDiagram
    actor User as Usuário
    participant UI as Frontend
    participant CAT as API do Catálogo
    participant DB as PostgreSQL
    participant OUT as Porta de Outbox do Catálogo
    participant PUB as Publicador da Integração
    participant MQ as RabbitMQ
    participant W as Worker de Integração
    participant EXT as Mock do Centro de Vendas

    User->>UI: Edita produto
    UI->>CAT: Solicitação autorizada + versão
    CAT->>CAT: Valida permissão e invariantes
    CAT->>DB: Confirma atomicamente produto, histórico e intenção de outbox
    DB-->>CAT: Confirmado
    CAT-->>UI: Resposta do produto
    PUB->>OUT: Reivindica intenção pendente do catálogo
    OUT->>DB: Reivindica pela persistência pertencente ao Catálogo
    PUB->>MQ: Publica evento versionado
    PUB->>OUT: Marca resultado da publicação
    MQ-->>W: Entrega at-least-once
    W->>W: Verificação de idempotência e mapeamento
    W->>EXT: Solicitação correlacionada
    alt sucesso
        EXT-->>W: Aceito
        W->>DB: Registra resultado da entrega
        W-->>MQ: Confirma
    else falha transitória
        W->>DB: Registra tentativa e erro seguro
        W-->>MQ: Retry com backoff limitado
    else falha terminal
        W->>DB: Marca falha recuperável
        W-->>MQ: Dead-letter / rejeita conforme política
    end
~~~

A resposta do catálogo não afirma que a sincronização externa ou sua projeção de auditoria foi concluída. Catálogo é responsável pela intenção e sua persistência; Integração é responsável por publicação e entrega. A outbox fecha a lacuna de escrita dupla entre banco e broker. A reivindicação pelo publicador e a idempotência do consumidor devem tolerar reinício e entrega duplicada.

## Garantias de atomicidade e consistência

| Preocupação | Garantia | Limite |
| --- | --- | --- |
| Estado do produto + histórico do produto + intenção de outbox do catálogo | **Atômica** em uma transação de banco pertencente ao Catálogo | catalog |
| Publicação da intenção de outbox no RabbitMQ | **Eventualmente consistente**, recuperável e at-least-once | porta de publicação de catalog -> publicador de integration |
| Entrega ao Centro de Vendas Externo | **Eventualmente consistente**, idempotente e com retry limitado | integration -> limite externo |
| Projeção de auditoria do negócio do Catálogo | **Eventualmente consistente** a partir da intenção confirmada do catálogo, salvo ação crítica classificada como fail-closed | catalog -> limite de append de audit |
| Eventos de auditoria críticos para segurança designados | Aceitação durável de auditoria é exigida antes de o comando informar sucesso; o protocolo transacional exato será escolhido antes da implementação | módulo de origem -> limite de append de audit |
| Visão combinada de Produto/Mídia para o cliente | Composição em tempo de leitura; sem transação entre módulos nem dependência de domínio | limite de composição de aplicação/consulta |

Fatos de auditoria carregam snapshots imutáveis de ID/tipo do ator, ação, alvo, resultado, momento e contexto de correlação. Auditoria nunca consulta Auth durante o append. Se uma projeção eventual de auditoria falhar, ela faz retry e alerta; não pode alterar a transação original do produto.

### Evidências de qualidade e decisão de release

~~~mermaid
flowchart LR
    Evidence["Testes, cobertura, segurança, performance, defeitos e observabilidade"] --> Normalize["Validação autenticada e normalização"]
    Normalize --> Store["Snapshot de evidências do candidato/build"]
    Store --> Gates["Avaliação de gates críticos"]
    Store --> Score["Cálculo versionado da pontuação"]
    Gates --> Risk["Classificação de risco"]
    Score --> Risk
    Risk --> Rec["Recomendação explicável"]
    Rec --> Human["Decisão humana autorizada"]
    Gates --> Human
~~~

A ingestão é idempotente por fonte e identidade de execução. Evidências ausentes, desatualizadas ou incompatíveis são visíveis e não podem ser interpretadas silenciosamente como aprovadas. Avaliações históricas retêm a versão de sua política/fórmula.

### Processamento de mídia

1. A API autoriza o ator e valida metadados, limites declarados e contexto do produto.
2. O conteúdo do arquivo é transmitido com limites de tamanho, validação de assinatura/tipo e um nome de objeto gerado.
3. O objeto permanece não público e pendente até que o processamento/inspeção aprovado seja concluído.
4. O processamento cria variantes aprovadas e registra checksums, dimensões e status.
5. A falha torna-se um estado terminal ou repetível visível; leituras de produto continuam disponíveis.
6. Exclusão/retenção tratam metadados e objetos sem violar requisitos de auditoria.

## Contratos de comunicação

### HTTP

- JSON sobre HTTPS fora do desenvolvimento local.
- Prefixo de caminho versionado (/api/v1) para contratos públicos conceituais.
- Formato de erro estável com código de máquina, mensagem segura, detalhes de campos, ID de correlação e timestamp.
- Concorrência otimista para atualizações materiais, inicialmente por uma decisão futura entre campo de versão ou ETag/If-Match.
- Paginação limitada e ordenação determinística.
- Chaves de idempotência para operações de criação/comando elegíveis a retry.

### Eventos e mensagens

Cada envelope contém:

- eventId único;
- eventType e schemaVersion;
- occurredAt e identidade do produtor;
- ID do agregado/entidade e versão do agregado quando aplicável;
- IDs de correlação e causalidade;
- payload com o mínimo de dados necessário.

Schemas incompatíveis usam uma nova versão com janela de compatibilidade/migração. Consumidores ignoram campos aditivos desconhecidos e rejeitam versões obrigatórias não suportadas de forma visível. Payloads de mensagens não devem conter credenciais, tokens brutos nem dados pessoais desnecessários.

## Limites de persistência e consistência

- PostgreSQL é a fonte da verdade para estado transacional.
- Cada módulo é responsável por suas tabelas e migrações, mesmo em um único banco.
- Uma transação do Catálogo atualiza atomicamente agregado, histórico de produto e intenção de outbox pertencente ao Catálogo. Auditoria é acessada somente pelo limite explícito sob a garantia descrita acima.
- Fluxos entre módulos ou externos usam consistência eventual com status explícito.
- Dinheiro usa precisão fixa e moeda; timestamps são armazenados como instantes UTC; IDs são opacos e estáveis.
- Concorrência otimista evita perda silenciosa de atualizações em registros mutáveis de catálogo e política.
- Objetos binários ficam no MinIO; o banco armazena metadados, checksum e chave opaca do objeto.

Consulte [DATA_MODEL.md](DATA_MODEL.md) para entidades conceituais. Schema físico e ferramentas de migração exigem um ADR ou decisão no plano de implementação.

## Limites de integração

O Mock do Centro de Vendas Externo está fora dos limites de confiança e transacionais. As chamadas exigem:

- timeout explícito menor que o lease de processamento do worker;
- classificação de retry e número limitado de tentativas;
- chave de idempotência/identidade do evento;
- testes de contrato pertencentes às expectativas do adaptador e ao comportamento do mock;
- telemetria sanitizada;
- circuit breaker somente se medições mostrarem tempestades de retry ou falha downstream contínua;
- reconciliação/replay autorizado e auditado.

O mock deve simular resultados realistas, mas não pode sustentar alegações de produção sobre um provedor real não especificado.

## Cenários de falha

| Cenário | Comportamento esperado | Evidência/telemetria exigida |
| --- | --- | --- |
| Serviço externo indisponível/timeout | Commit do catálogo tem sucesso; integração repete e depois entra em falha recuperável | ID do evento, tentativas, latência, erro classificado, profundidade da fila, trace |
| Mensagem duplicada | Consumidor detecta processamento anterior e evita efeito duplicado | contador de duplicidade, registro de idempotência e log correlacionado |
| Falha do publicador após publicar no broker | Duplicidade pode ocorrer; nenhuma alteração confirmada é perdida | estado da outbox, tentativa de publicação e resultado de idempotência do consumidor |
| RabbitMQ indisponível | Outbox acumula com segurança; health da API mostra dependência degradada sem perder escritas do catálogo | idade/quantidade não publicada, estado da conexão e alerta |
| Falha do processador de imagem | Mídia torna-se com falha/repetível; catálogo existente permanece legível | ID da mídia, estágio, código do motivo, tentativa e trace |
| MinIO indisponível | Upload/processamento falha com segurança sem metadado pronto órfão | classe do erro de armazenamento, sinal de limpeza/reconciliação |
| Latência/indisponibilidade do banco | Solicitações atingem timeout dentro do orçamento, falham sem estado parcial e expõem degradação da dependência | pool, latência de consulta, contagem de timeouts e traces sem dados SQL brutos |
| HTTP 500 aleatório da API | Resposta de erro estável e segura; correlação permite investigação | código do erro, classificação da exceção, trace e métrica da solicitação |
| Atualização concorrente obsoleta | Atualização rejeitada como conflito, estado mais novo preservado | ID/versão do produto e métrica de conflito |
| Evidência de qualidade inválida | Payload rejeitado/colocado em quarentena; avaliação existente da release permanece inalterada | fonte, erro de schema, ID de ingestão e métrica de rejeição |
| Fonte de qualidade ausente/desatualizada | Resumo da qualidade mostra evidência insuficiente/desatualizada; política pode bloquear | atualização da fonte, timestamps esperado/recebido e motivo do gate |
| Erro do Motor de Qualidade | Nenhuma pontuação fabricada; última avaliação claramente marcada como desatualizada ou indisponível | versão da fórmula, ID do conjunto de entradas, erro de cálculo e alerta |

## Mecanismos de testabilidade

- Políticas de negócio são determinísticas e separadas de E/S.
- Tempo, IDs, clientes externos e agendamento de retry são injetáveis por portas de responsabilidade definida.
- APIs e eventos têm schemas/exemplos adequados para verificações de contrato.
- Toda operação assíncrona expõe status e identificadores estáveis de correlação.
- Identidade de seed/build e dados de teste controlados são observáveis.
- Controles do Laboratório de Falhas são explícitos, delimitados, com prazo e indisponíveis em produção.
- A telemetria apoia asserções sobre resultados sem tornar logs específicos da implementação o único oráculo.

## Direção de deploy e execução local

Nenhum runtime é criado na fase de fundação. Quando a implementação começar, o formato pretendido para desenvolvimento é:

- frontend e backend executáveis diretamente para desenvolvimento rápido;
- PostgreSQL e, somente em suas fases, RabbitMQ e MinIO fornecidos por Docker Compose;
- um comando/caminho de bootstrap documentado, health checks e dados seed determinísticos;
- nenhuma exigência de Kubernetes ou conta em nuvem;
- worker de integração inicializável independentemente quando a integração assíncrona existir.

### Baseline de planejamento da Fase 01

A baseline futura aprovada é:

| Preocupação | Decisão |
| --- | --- |
| JDK | 25 LTS |
| Framework | Spring Boot 4.1.x |
| Build | Maven |
| Empacotamento | Jar executável |
| Coordenadas Maven | io.github.sytef:aegis |
| Pacote-base | io.github.sytef.aegis |
| CI | GitHub Actions com permissions: contents: read; permissões adicionais exigem justificativa explícita |

A Fase 01 permanece limitada a um esqueleto de aplicação executável, liveness/readiness/info, erros Problem Details, IDs de correlação limitados, validação de unidade/componente e CI de privilégio mínimo. Não contém frontend, PostgreSQL, RabbitMQ, MinIO, CRUD/lógica de negócio do catálogo, autenticação real, Motor de Qualidade ou Grafana. Os contratos detalhados estão em [API_SPEC.md](API_SPEC.md#contratos-operacionais-da-fase-01) e [PLANS.md](../PLANS.md#fase-01--fundação-backend-executável-mínima).

## Regras de evolução

Um módulo só pode ser considerado para extração se evidências mostrarem uma necessidade como escala independente, isolamento, cadência de release, responsabilidade ou limites de confiabilidade que o monólito não consiga atender de forma razoável. Antes da extração, deve-se verificar que responsabilidade, contratos, observabilidade e limites de dados do módulo já são saudáveis. Distribuição não corrige modularidade ruim.

## Decisões em aberto

- Mecanismo de autenticação/sessão e biblioteca de identidade.
- Estrutura de projetos backend/frontend e ferramenta de imposição da modularidade.
- Representação de concorrência otimista (campo version versus semântica HTTP ETag).
- Estratégia de schema físico do banco e ferramenta de migração.
- Política de object storage e retenção de evidências.
- Topologia RabbitMQ, mecanismo de retry e fluxo de dead-letter/replay.
- Fórmula de qualidade, pesos, janelas de atualização e autoridade de exceção.
- Protocolo físico para aceitação de auditoria fail-closed designada sem dependências cíclicas.

Essas decisões estão programadas para refinamento em [PLANS.md](../PLANS.md) e ADRs futuros.
