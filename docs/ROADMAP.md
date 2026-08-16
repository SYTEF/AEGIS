# Roadmap do AEGIS

## Como interpretar este roadmap

Este roadmap comunica a evolução pretendida do produto, não uma promessa imutável de entrega. Rótulos de versão descrevem marcos de capacidades e podem ser divididos, reordenados ou ter o escopo alterado após evidências e revisão humana. Uma capacidade só está concluída quando seus critérios de aceite aplicáveis e sua Definição de Pronto forem atendidos; o nome de uma versão isoladamente não indica implementação.

As fases detalhadas e prontas para aprovação estão em [PLANS.md](../PLANS.md). Alterações arquiteturais difíceis de reverter pertencem aos [ADRs](ADR/README.md).

## Sequência orientadora

- Estabelecer uma fatia vertical confiável antes de ampliar a quantidade de funcionalidades.
- Construir valor síncrono do catálogo antes da complexidade de integração assíncrona.
- Adicionar segurança e observabilidade junto às capacidades que elas protegem, não apenas em marcos tardios.
- Testar cada camada quando seu limite se tornar real.
- Construir o Centro de Controle de Qualidade a partir de evidências reais do AEGIS, não de dashboards especulativos.
- Calibrar pontuação/gates com dados antes de apresentá-los como inteligência.

## Marcos

### v0.1 — Fundação

**Resultado:** baseline oficial de produto e uma fundação backend mínima, executável e validada em CI Linux/Windows.

Inclui:

- visão, escopo, personas e critérios de sucesso do projeto;
- requisitos numerados e contratos conceituais;
- limites do Monólito Modular e modelos iniciais de ameaça/falha;
- estratégia de Engenharia de Qualidade em camadas e gates progressivos;
- modelo conceitual de dados, roadmap, planos e processo de ADR;
- regras operacionais para contribuidores e agentes.
- aplicação Spring Boot executável com health/info seguros, Problem Details, correlação, logging estruturado, testes e CI.

Não inclui frontend, banco de dados, infraestrutura externa, autenticação nem domínio de negócio. Questões documentais e limites preliminares permanecem explícitos, em vez de serem apresentados falsamente como resolvidos.

### v0.2 — Catálogo

**Resultado:** primeiro Catalog Experience vertical, com Product persistido e uma experiência web profissional como **PRÉVIA DE DESENVOLVIMENTO LOCAL**.

Escopo aprovado para implementação mediante autorização humana separada:

- Product com exatamente uma Category obrigatória; backend mínimo de Category com criação/leitura/listagem/desativação, filtro ACTIVE e regras fechadas de normalização, unicidade, lifecycle e uso;
- PostgreSQL 18.4 no schema `catalog`, Flyway, Docker Compose local e Testcontainers, sem H2; imagem oficial com digest fixado quando o Compose for criado;
- criação/leitura/listagem/atualização/desativação de Product, com SKU imutável, BRL, estoque, ciclo de vida e ETag/If-Match;
- histórico e intenção de outbox atômicos, sem worker, RabbitMQ ou publicação externa;
- contrato OpenAPI versionado entre backend e frontend;
- SPA React/TypeScript com Application Shell, Products List, Create, Details e Edit Product;
- experiência light-first, responsiva e acessível, sujeita ao Design Quality Gate;
- testes backend/frontend em camadas e smoke full-stack manual, sem Playwright;
- structured logging, correlation ID, Problem Details e health proporcionais, sem antecipar Prometheus/Grafana/OpenTelemetry.

Mídia, gestão visual completa de Category, mensageria/publicação externa, Auth/RBAC, dark mode completo e UI do Centro de Controle de Qualidade permanecem fora deste marco.

Até que a autenticação/RBAC da v0.3 esteja concluída:

- mutação do catálogo não pode ser descrita como segura ou completa quanto à autorização;
- a aplicação deve restringir/localizar o acesso ao desenvolvimento e não deve ser exposta publicamente;
- qualquer identidade de desenvolvimento é scaffolding de teste, não evidência de autenticação;
- critérios de aceite de autorização permanecem pendentes;
- um Gate de Exposição Externa bloqueia deploy/hospedagem além do ambiente local/isolado aprovado.

### v0.3 — Autenticação

**Resultado:** identidades e RBAC aplicados no servidor e no navegador protegem o comportamento e a experiência existentes do catálogo.

Escopo candidato:

- mecanismo escolhido de autenticação/sessão por ADR;
- usuários, roles/permissões e desativação/revogação;
- matriz de autorização do catálogo e administração;
- controles de abuso de autenticação e auditoria de segurança;
- testes negativos de API/segurança e bootstrap local documentado.
- integração de sessão/autorização ao Commerce existente e jornadas críticas Playwright limitadas.

MFA ou provedor externo de identidade só é incluído se o modelo de ameaças/deploy justificar.

Este marco deve concluir todos os critérios de autorização intencionalmente pendentes da prévia local v0.2 antes que o catálogo possa ser exposto externamente ou descrito como controlado por acesso.

### v0.4 — Mídia

**Resultado:** ciclo seguro de imagens do produto.

Escopo candidato:

- decisão sobre MinIO e dependência local;
- upload restrito, validação, armazenamento privado e metadados;
- ciclo de processamento, variantes, falha/retry e limpeza;
- associação/ordenação no produto e recuperação autorizada;
- testes de upload hostil, integração, recursos e observabilidade.

Não aceitar SVG nem arquivos genéricos sem requisito concreto e modelo de ameaças atualizado.

### v0.5 — Integração

**Resultado:** alterações do catálogo chegam a um Mock do Centro de Vendas Externo de forma confiável e observável.

Escopo candidato:

- contrato versionado de eventos e outbox transacional;
- topologia RabbitMQ baseada em ADR;
- worker de integração e mock downstream controlado;
- retry limitado, idempotência, falha terminal, reconciliação e replay;
- testes de contrato, reinício, duplicidade e caminhos de falha;
- dashboards e alertas de fila/outbox/entrega.

Este marco introduz deliberadamente falhas distribuídas somente depois que o comportamento central do catálogo estiver estável.

### v0.6 — Engenharia de Qualidade

**Resultado:** o Centro de Controle de Qualidade ingere e apresenta evidências rastreáveis de fontes reais de teste do AEGIS.

Escopo candidato:

- releases, suítes/casos, execuções/resultados e metadados de evidência;
- atribuição de fonte/build e ingestão idempotente;
- normalização de defeitos, achados de segurança e resultados de performance;
- rastreabilidade requisito-release e visões de links ausentes;
- interface inicial acessível do Centro de Controle de Qualidade;
- jornadas críticas Playwright e charters exploratórios documentados.

Este marco não alega pontuação inteligente; primeiro estabelece evidências confiáveis.

Pontuação de Qualidade, Classificação de Risco e recomendação do Motor ficam NOT_APPLICABLE durante todo este marco. Gates críticos aplicáveis e atualização das evidências ainda governam a revisão do candidato e podem bloquear a release.

### v0.7 — Motor de Qualidade

**Resultado:** Inteligência de Release explicável e versionada sobre evidências confiáveis.

Escopo candidato:

- política versionada de gates críticos e avaliações imutáveis;
- implementação preliminar da Pontuação de Qualidade com dimensões contribuintes;
- Classificação de Risco e motivos da recomendação;
- comportamento para evidência desatualizada/ausente/com erro;
- decisão humana autorizada e fluxo de exceção com prazo;
- testes de calibração e casos adversariais que tentem ocultar risco crítico.

A pontuação permanece experimental até que seus resultados sejam avaliados contra cenários representativos.

### v0.8 — Segurança e Performance

**Resultado:** garantia não funcional mais profunda, reproduzível e aplicada aos limites implementados.

Escopo candidato:

- modelo de ameaças atualizado e revisão manual de segurança direcionada;
- maturidade de varredura da cadeia de suprimentos, código-fonte, dependências e artefatos;
- workload, dataset, baseline e limites calibrados de referência no k6;
- endurecimento de abuso de API, upload, ingestão e autorização;
- evidências em dashboard e integração ao Centro de Controle de Qualidade;
- gates de release de performance/segurança com política justificada de exceções.

Limites só são promovidos a política rígida quando ambientes e medições forem válidos.

### v0.9 — Observabilidade e Laboratório de Falhas

**Resultado:** experimentos controlados de falha comprovam diagnóstico, contenção e recuperação.

Escopo candidato:

- correlação/tracing com OpenTelemetry para fluxos críticos;
- métricas Prometheus e dashboards operacionais/de qualidade no Grafana;
- alertas acionáveis e runbooks de investigação exercitados;
- Laboratório de Falhas protegido por permissão, não produtivo, com TTL/parada de emergência;
- cenários de downstream, latência, HTTP 500, banco de dados, mídia e fila;
- hipóteses, traces, resultados e evidências de recuperação vinculados às releases.

A injeção de falhas permanece desligada e indisponível em produção por design.

## MPR — Release Mínima de Portfólio

A MPR é um checkpoint de capacidades projetado para comprovar o núcleo profissional mesmo que funcionalidades avançadas precisem de mais tempo. Não é uma segunda arquitetura nem permissão para reduzir a qualidade. Só pode ser declarada quando o escopo exato incluído estiver documentado e seus gates aplicáveis passarem.

Evidência mínima do núcleo:

- caminho reproduzível de inicialização/teste local a partir de clone limpo;
- gerenciamento autenticado e autorizado de produto/categoria com PostgreSQL;
- validação, histórico/auditoria, concorrência e comportamento seguro de erros;
- um fluxo de integração externa confiável, idempotente e observável, se a integração estiver incluída no escopo declarado da MPR;
- cobertura em camadas de unidade/componente/integração/API/contrato e uma pequena jornada crítica de UI quando a UI estiver incluída;
- baseline de segurança, UI crítica acessível, correlação/diagnósticos e gates progressivos na CI;
- atribuição de candidato/build da release, resumo de evidências, rastreabilidade e resultados de gates críticos sem exigir pontuação numérica.

A MPR deliberadamente **não** exige a futura Pontuação de Qualidade, a UI completa de Inteligência de Release, o catálogo completo de cenários do Laboratório de Falhas nem a stack completa de observabilidade. Esses itens permanecem na Visão Completa e podem ser objetivos adicionais se ameaçarem a conclusão. Funcionalidades já concluídas antes da MPR permanecem incluídas e devem atender aos seus gates.

O checkpoint preferencial ocorre depois que a capacidade confiável de resumo de evidências existir, mas aprendizados do roadmap podem mudar a ordem ou reduzir amplitude não essencial com aprovação humana.

### v1.0 — Release de Portfólio da Visão Completa

**Resultado:** uma release coerente e reproduzível da Visão Completa para portfólio, demonstrando um produto real e Engenharia de Qualidade profissional, incluindo os diferenciais avançados preservados abaixo.

Escopo candidato:

- jornadas críticas acessíveis e refinadas do Commerce e do Centro de Controle de Qualidade;
- bootstrap local, demonstração, testes e caminhos de investigação de falhas documentados;
- artefatos reproduzíveis e gates progressivos no GitHub Actions;
- requisitos Must satisfeitos ou decisões explícitas de escopo;
- evidências atuais de segurança, performance, resiliência e observabilidade;
- ADRs, arquitetura, limitações operacionais e riscos residuais revisados;
- pontuação/risco/recomendação da release e decisão humana final;
- experimentos controlados do Laboratório de Falhas e Inteligência de Release sustentada por evidências validadas.

“Produção” aqui significa qualidade de portfólio com mentalidade de produção. Hospedagem pública real, SLA, conformidade jurídica/de privacidade e operação 24/7 exigem avaliação separada de deploy.

## Trilhas transversais

Todo marco avalia:

- requisitos e rastreabilidade;
- design seguro/alterações de ameaças;
- testabilidade e cobertura automatizada/manual apropriada;
- integridade de dados/migração;
- diagnósticos estruturados e comportamento operacional de falha;
- consistência da documentação/ADRs;
- reprodutibilidade local e experiência do contribuidor;
- estado do Gate de Exposição Externa enquanto autenticação real estiver ausente.

Acessibilidade começa junto com qualquer UI, não somente na v0.8. Segurança, testes e observabilidade amadurecem continuamente, mesmo quando um marco dá ênfase especial a uma área.

## Pontos de decisão

Aprovação humana é exigida antes de iniciar cada fase de [PLANS.md](../PLANS.md). Em cada limite de marco, revisar:

1. A próxima capacidade resolve um problema de produto/qualidade?
2. Os limites atuais estão saudáveis o suficiente para serem ampliados?
3. Quais novos riscos de segurança, dados e operação aparecem?
4. Quais evidências comprovam o marco e o que permanece desconhecido?
5. A ordem ou o escopo do roadmap deve mudar com base no aprendizado?

## Complexidade explicitamente adiada

Nenhum marco do roadmap autoriza implicitamente Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch ou microservices. Se uma futura medição sugerir alguma dessas tecnologias, compare alternativas mais simples e documente a decisão por ADR antes de planejar a implementação.

## Questões atuais em aberto

- A v1.0 precisa de demonstração pública hospedada ou uma demonstração local reproduzível é o alvo de aceite?
- Qual modelo de navegador/autenticação e profundidade de separação de roles atendem ao objetivo do portfólio?
- Qual perfil de hardware/dados torna alegações de performance reproduzíveis para revisores?
- Qual retenção de evidências é viável sem tornar o repositório ou a stack local pesados?
- Quais integrações com sistemas externos de issues/testes são desnecessárias para v1.0 e devem permanecer simuladas?
