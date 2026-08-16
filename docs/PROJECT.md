# Definição do Projeto AEGIS

## Identidade

- **Nome:** AEGIS — Commerce Quality Engineering Platform
- **Tagline:** Quality isn't the final step. It's part of the architecture.
- **Tradução:** Qualidade não é a etapa final. Ela faz parte da arquitetura.

## Visão

Construir um produto confiável e inspecionável no qual a qualidade seja projetada nos requisitos, na arquitetura, na implementação, na entrega e nas operações. O AEGIS deve demonstrar como um fluxo real de comércio e um Centro de Controle de Qualidade (Quality Control Center) podem produzir decisões de release baseadas em evidências sem reduzir a Engenharia de Qualidade à automação de UI.

## Problema

Muitos projetos de demonstração apresentam testes como uma coleção de scripts adicionados depois do desenvolvimento da aplicação. Eles omitem análise de risco, rastreabilidade, qualidade de dados, contratos, segurança, performance, resiliência, observabilidade e governança de release. Como resultado, não demonstram como decisões de qualidade são tomadas em um sistema de software com mentalidade de produção.

Equipes também consolidam frequentemente evidências de release de forma manual entre ferramentas desconectadas. Taxas de aprovação podem ser relatadas sem o risco de negócio, achados críticos podem ser ocultados por pontuações agregadas e falhas podem ser difíceis de investigar porque telemetria e evidências não foram projetadas com testabilidade em mente.

## Proposta

O AEGIS consiste em dois domínios de produto conectados:

1. **AEGIS Commerce** — um sistema real de gerenciamento de catálogo de produtos com capacidades de identidade, catálogo, mídia, auditoria e integração externa confiável.
2. **Centro de Controle de Qualidade do AEGIS (AEGIS Quality Control Center)** — um sistema que consolida requisitos, resultados de testes, evidências, defeitos, achados de segurança, métricas de performance e sinais de observabilidade por release.

Um futuro **Motor de Qualidade (Quality Engine)** calculará uma Pontuação de Qualidade transparente, classificará o risco e recomendará uma decisão de release. Regras críticas permanecem bloqueios críticos independentemente da pontuação.

## Objetivos

- Entregar um produto coerente em vez de uma vitrine exclusivamente de testes.
- Incorporar testabilidade, segurança e observabilidade à arquitetura e aos critérios de aceite.
- Demonstrar testes em camadas: unidade, componente, integração, API, contrato, ponta a ponta, segurança, performance, resiliência, acessibilidade e qualidade de dados.
- Oferecer rastreabilidade ponta a ponta do requisito à decisão de release.
- Tornar falhas reproduzíveis e diagnosticáveis com evidências, logs, métricas e traces.
- Evoluir incrementalmente a partir de um Monólito Modular e adicionar complexidade operacional somente quando justificada.
- Documentar trade-offs importantes por meio de ADRs.

## Usuários e partes interessadas

| Persona | Necessidade |
| --- | --- |
| Administrador do Catálogo | Gerenciar categorias e informações completas de produtos com segurança |
| Operador de Estoque | Manter o estoque com validação e histórico |
| Operador de Mídia | Fazer upload e acompanhar o processamento de imagens de produtos |
| Gerente de Release | Compreender prontidão da release, risco e condições de bloqueio |
| Profissional de QA / Engenharia de Qualidade | Projetar cobertura, executar testes, anexar evidências e investigar falhas |
| Pessoa Desenvolvedora | Receber feedback rápido e acionável e reproduzir defeitos |
| Revisor de Segurança | Inspecionar achados, trilhas de auditoria e riscos de release não resolvidos |
| Gestor de Engenharia / Entrevistador | Avaliar decisões de engenharia e a credibilidade das práticas de qualidade |
| Centro de Vendas Externo | Consumir alterações do catálogo por meio de um limite downstream simulado |

As personas podem ser desempenhadas pela mesma pessoa nas primeiras versões. A autorização ainda deve modelar suas responsabilidades explicitamente.

## Escopo do produto

### AEGIS Commerce

- Autenticação, usuários, roles e autorização.
- Produtos, categorias, SKU, preço, estoque, descrições, imagens e busca.
- Paginação, filtros e ordenação determinística.
- Histórico de produtos e eventos de auditoria de segurança/negócio.
- Validação, armazenamento e ciclo de processamento de imagens.
- Integração assíncrona confiável com um Mock do Centro de Vendas Externo.
- Eventos, retries, idempotência e estados de falha visíveis.

### Centro de Controle de Qualidade

- Releases, suítes, casos, execuções, resultados e referências de evidência.
- Defeitos, achados de segurança, métricas de performance e métricas de qualidade.
- Rastreabilidade de requisitos até releases.
- Pontuação de Qualidade transparente, nível de risco, avaliação de gates e recomendação de release.
- Decisão final de release de responsabilidade humana, com ator, momento e justificativa.
- Cenários do Laboratório de Falhas para testes controlados de resiliência em ambientes não produtivos.

### Escopo de engenharia

- Estratégia de testes automatizados e exploratórios.
- Segurança by Design, Observabilidade by Design e Testabilidade by Design.
- Reprodutibilidade local e Gates de Qualidade de CI/CD quando a implementação começar.
- Documentação que evolui com o comportamento.

## Fora de escopo

Os itens a seguir estão fora do escopo inicial do produto:

- Carrinho, checkout, pagamento, atendimento de pedidos e storefront para consumidores.
- Plataformas reais de vendas de terceiros; a primeira integração é um mock controlado.
- Cobrança SaaS multi-tenant ou funcionalidades de marketplace global.
- Aplicações móveis nativas.
- Compromissos de deploy em produção, alta disponibilidade ou recuperação de desastre durante as fases de fundação.
- Substituição genérica de gerenciamento de testes para todas as equipes.
- Decisões de release baseadas em IA sem política determinística e explicável.
- Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch ou microservices sem ADR aprovado e sustentado por evidências.

## Diferenciais

- Um domínio de produto real e um domínio de qualidade evoluem juntos.
- Evidências de qualidade são modeladas como dados do produto, não ficam ocultas apenas em logs da CI.
- Risco crítico não pode ser diluído por uma pontuação numérica.
- O Laboratório de Falhas conecta falhas controladas a testes de resiliência e observabilidade.
- A rastreabilidade apoia auditoria e seleção direcionada de regressão.
- A arquitetura permanece intencionalmente simples até que escala ou isolamento exijam mudança.

## Critérios de sucesso

O sucesso é avaliado progressivamente. O [checkpoint da MPR](ROADMAP.md#mpr--release-mínima-de-portfólio) comprova o núcleo profissional mínimo; até a Visão Completa v1.0, o AEGIS deve demonstrar:

1. Um ambiente local reproduzível com caminhos documentados de inicialização, seed e encerramento.
2. Fluxos autenticados e autorizados de catálogo e mídia com auditoria e histórico.
3. Sincronização externa confiável, observável e idempotente.
4. Verificações automatizadas nas camadas apropriadas, com cobertura E2E baseada em risco em vez de uso excessivo de E2E.
5. Requisitos, evidências de testes e defeitos rastreáveis e associados a uma release.
6. Um Centro de Controle de Qualidade que explique entradas da pontuação, bloqueios críticos, risco e recomendação.
7. Cenários demonstráveis de segurança, performance e resiliência com evidências preservadas.
8. Gates de Qualidade na CI que sejam rápidos no nível de pull request e progressivamente mais profundos em direção à release.
9. Documentação e ADRs correspondentes ao comportamento implementado.
10. Nenhum achado crítico de segurança nem falha crítica de teste conhecida e não resolvida em uma release aprovada.

Metas quantitativas iniciais estão definidas em [QUALITY_GATES.md](QUALITY_GATES.md); são políticas a calibrar, não métricas de vaidade.

## Objetivos profissionais

O projeto foi projetado como evidência para as funções de QA Engineer, Quality Engineer, SDET, Test Automation Engineer e Software Engineer in Test. Ele deve demonstrar:

- pensamento de produto e risco;
- análise de requisitos e design de critérios de aceite;
- arquitetura de software e modularidade;
- estratégia de testes para qualidades funcionais e não funcionais;
- manutenibilidade da automação e seleção adequada de camadas;
- testes de API, dados, integração e UI;
- engenharia segura e consciência de ameaças;
- análise de performance e resiliência;
- investigação assistida por observabilidade;
- governança de CI/CD, comunicação de defeitos e raciocínio de release.

## Premissas do produto

- Uma única organização e um catálogo de tamanho moderado são suficientes para a primeira release completa.
- O PostgreSQL pode permanecer como fonte da verdade dos dados transacionais.
- A entrega assíncrona é útil para a integração externa, mas chamadas síncronas em processo são preferidas entre módulos internos.
- Object storage é justificado para imagens de produtos; dados binários de imagem não devem ser armazenados em tabelas transacionais.
- Uma recomendação de release é consultiva até que uma pessoa autorizada registre a decisão final.

Premissas devem ser validadas durante a implementação. Premissas frágeis ou refutadas tornam-se requisitos, riscos ou ADRs em vez de comportamento implícito.

## Revisão NEXUS da fundação — 2026-08-16

### Achados

| Perspectiva | Achado |
| --- | --- |
| Produto | O AEGIS contém dois produtos conectados com atores e fluxos confiáveis: operações de catálogo geram preocupações reais de estado/falha, enquanto o Centro de Controle transforma suas evidências de qualidade em decisões de release. Ele não é definido como um framework de automação. |
| Arquitetura | O limite de Monólito Modular é proporcional. RabbitMQ e MinIO são adiados até que os requisitos de integração/mídia os justifiquem; o worker pode permanecer na mesma base de código. Nenhuma tecnologia proibida é planejada por padrão. |
| QA | Testabilidade, testes em camadas, rastreabilidade, evidências, ciclo de defeito, testes não funcionais e integridade dos gates críticos estão presentes nos requisitos e na arquitetura, em vez de serem anexados apenas à CI. |
| Segurança | A fundação cobre RBAC, autorização de objeto, uploads hostis, secrets, proveniência de evidências, integridade da política de release e contenção do Laboratório de Falhas. Os designs concretos de identidade/sessão e retenção permanecem deliberadamente abertos. |
| DevOps | Um caminho local-first está planejado e evita dependência de Kubernetes/nuvem. Ainda não existe ambiente executável; por isso, a reprodutibilidade é um critério de aceite da Fase 01, não uma capacidade atual. |
| Observabilidade | Fluxos críticos definem logs, métricas, traces, IDs, semântica de health, dashboards, alertas e um caminho de investigação para QA. Backends/armazenamento e retenção de telemetria não estão resolvidos. |
| Red Team | As premissas mais frágeis são proveniência confiável das fontes de qualidade, calibração realista da pontuação, segregação suficiente de funções para uma pessoa, negação confiável do Laboratório de Falhas em produção, recursos locais modestos e representatividade do mock externo. |

### Decisões

- A fundação é exclusivamente documental; não alega implementação, certificação de segurança ou prontidão para produção.
- Começar como Monólito Modular conforme o [ADR-001](ADR/ADR-001-modular-monolith.md) aceito, com APIs explícitas dos módulos e PostgreSQL como fonte transacional da verdade pretendida.
- Introduzir MinIO para necessidades aprovadas de imagem e RabbitMQ para integração externa confiável apenas em suas respectivas fases aprovadas.
- Manter a política de qualidade versionada e explicável; regras de bloqueio crítico prevalecem sobre a Pontuação de Qualidade.
- A automação recomenda; uma pessoa autorizada registra a decisão final de release.
- A Fase 01 permanece uma prova mínima de backend/build/health executável, sem banco de dados, frontend ou CRUD de negócio.

### Riscos

- A amplitude da capacidade futura documentada pode ser confundida com escopo comprometido ou comportamento concluído.
- Limites preliminares de performance e pesos da pontuação podem criar falsa precisão antes da definição de uma baseline.
- O Centro de Controle de Qualidade pode se expandir para um produto genérico de gerenciamento de testes e desviar das necessidades de release do AEGIS.
- Um portfólio operado por uma pessoa não comprova totalmente a segregação de funções, mesmo quando o RBAC a modela.
- Retenção de evidências, telemetria e auditoria pode gerar custos ainda não quantificados de privacidade, armazenamento e acesso.
- PostgreSQL, RabbitMQ, MinIO e componentes de observabilidade locais juntos podem exceder o orçamento prático de recursos de um revisor.
- Não há licença selecionada, modelo de ameaças para ambiente hospedado nem compromisso de operação pública.

### Recomendações

- Obter autorização humana separada somente para a Fase 01, usando o ADR-001 aceito e a baseline aprovada de JDK 25 LTS / Spring Boot 4.1.x / Maven.
- Identificar com destaque métricas futuras/preliminares em toda UI implementada e saída de release até que sejam calibradas.
- Validar proveniência de evidências e identidade de build antes de implementar qualquer pontuação.
- Finalizar autenticação, matriz de roles, retenção e autoridade de exceções imediatamente antes das respectivas fases.
- Medir o uso local de recursos conforme cada dependência for introduzida e oferecer perfis leves em vez de executar toda a stack futura de uma só vez.
- Repetir esta revisão de múltiplas perspectivas a cada marco e converter premissas refutadas em requisitos, riscos ou ADRs.

## Governança

- [REQUIREMENTS.md](REQUIREMENTS.md) define o comportamento pretendido.
- [ARCHITECTURE.md](ARCHITECTURE.md) define limites e regras de dependência.
- [TEST_STRATEGY.md](TEST_STRATEGY.md), [SECURITY.md](SECURITY.md), [QUALITY_GATES.md](QUALITY_GATES.md) e [OBSERVABILITY.md](OBSERVABILITY.md) definem políticas transversais.
- [ROADMAP.md](ROADMAP.md) comunica a direção; [../PLANS.md](../PLANS.md) define fases prontas para aprovação.
- Os [registros ADR](ADR/README.md) explicam decisões importantes e substituem premissas anteriores quando aceitos.
- [GLOSSARY.md](GLOSSARY.md) define a terminologia de release e evidência entre documentos.
