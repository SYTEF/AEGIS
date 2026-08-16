# AEGIS — Commerce Quality Engineering Platform

> Quality isn't the final step. It's part of the architecture.
>
> Qualidade não é a etapa final. Ela faz parte da arquitetura.

AEGIS é uma plataforma de nível profissional para portfólio, criada para demonstrar Engenharia de Qualidade em toda a arquitetura e no ciclo de entrega de uma aplicação real. Não é um framework de automação de testes: combina um produto de comércio com um produto de qualidade e inteligência de releases.

## Domínios do produto

### AEGIS Commerce

Um sistema de gerenciamento de catálogo de produtos projetado para evoluir com autenticação, autorização, produtos, categorias, SKU, preços, estoque, mídia, histórico, auditoria, busca, integrações, eventos, retry e idempotência.

### Centro de Controle de Qualidade do AEGIS (AEGIS Quality Control Center)

Um centro de controle para releases, execuções de testes, defeitos, achados de segurança, resultados de performance, métricas de qualidade, risco e decisões de release. Um futuro Motor de Qualidade (Quality Engine) combinará uma Pontuação de Qualidade transparente com gates críticos inegociáveis.

## Por que o AEGIS existe

O AEGIS demonstra que qualidade é uma propriedade do sistema. Testabilidade, segurança, observabilidade, resiliência e rastreabilidade são preocupações arquiteturais, não atividades adiadas até a entrega. O público profissional pretendido inclui profissionais nas funções de QA Engineer, Quality Engineer, SDET, Test Automation Engineer e Software Engineer in Test.

## Direção arquitetural

O AEGIS começa como um **Monólito Modular (Modular Monolith)**, formalmente aceito no [ADR-001](docs/ADR/ADR-001-modular-monolith.md), com limites claros:

- `auth`: identidade, autenticação e RBAC;
- `catalog`: produtos, categorias, SKU, preço e estoque;
- `media`: recebimento e processamento seguro de imagens;
- `integration`: comunicação confiável com o Mock do Centro de Vendas Externo;
- `quality`: releases, evidências de teste, métricas, gates, risco e recomendações;
- `audit`: registros imutáveis de ações relevantes para segurança e negócio.

As tecnologias futuras preferenciais são React e TypeScript, Java e Spring Boot, PostgreSQL, RabbitMQ, MinIO, Playwright, k6, Docker Compose, GitHub Actions, OpenTelemetry, Prometheus e Grafana. Elas não são implementadas por esta fundação e continuam sujeitas a Registros de Decisão Arquitetural (Architecture Decision Records — ADRs).

A **baseline de planejamento** aprovada para a futura Fase 01 é JDK 25 LTS, Spring Boot 4.1.x, Maven, Jar executável, coordenadas Maven `io.github.sytef:aegis` e pacote-base `io.github.sytef.aegis`. O GitHub Actions começará com `permissions: contents: read`. Essas decisões não significam que a Fase 01 foi implementada nem autorizada a começar.

Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch e microservices estão explicitamente excluídos, a menos que um problema mensurável os justifique.

## Estado do repositório

Este repositório está atualmente na **v0.1 — Fundação**. Ele contém apenas a documentação oficial de produto e engenharia. Ainda não há frontend, backend, banco de dados, suíte de testes executável ou infraestrutura.

## Mapa da documentação

| Documento | Finalidade |
| --- | --- |
| [Projeto](docs/PROJECT.md) | Visão, usuários, escopo e critérios de sucesso |
| [Requisitos](docs/REQUIREMENTS.md) | Requisitos funcionais e não funcionais, regras e critérios de aceite |
| [Arquitetura](docs/ARCHITECTURE.md) | Limites do sistema, módulos, fluxos e cenários de falha |
| [Modelo de dados](docs/DATA_MODEL.md) | Entidades conceituais, responsabilidades e relações |
| [Especificação da API](docs/API_SPEC.md) | Contratos HTTP conceituais iniciais |
| [Estratégia de testes](docs/TEST_STRATEGY.md) | Estratégia de Engenharia de Qualidade e níveis de teste |
| [Segurança](docs/SECURITY.md) | Princípios de segurança e modelo de ameaças inicial |
| [Gates de Qualidade](docs/QUALITY_GATES.md) | Gates progressivos, pontuação, risco e bloqueios críticos |
| [Observabilidade](docs/OBSERVABILITY.md) | Logs, métricas, traces e investigação de falhas |
| [Roadmap](docs/ROADMAP.md) | Evolução incremental e revisável do produto |
| [Planos de execução](PLANS.md) | Objetivos, escopo e definições de pronto das fases |
| [Constituição dos agentes](AGENTS.md) | Regras operacionais para contribuidores e agentes |
| [ADRs](docs/ADR/README.md) | Processo e índice de decisões arquiteturais |
| [Glossário](docs/GLOSSARY.md) | Significado compartilhado de release, candidato, build, evidência, gate, risco e decisão |

## Políticas centrais de qualidade

- Todo requisito material deve ser rastreável a testes, execuções, evidências, defeitos e releases quando aplicável.
- Uma Pontuação de Qualidade informa decisões, mas nunca sobrepõe um gate crítico.
- Testes não devem ser excluídos, ignorados ou enfraquecidos apenas para obter um pipeline verde.
- Um teste nunca deve ser alterado somente para passar; primeiro deve-se determinar se o defeito está no produto, teste, dado, ambiente ou requisito.
- Falhas devem ser diagnosticáveis por meio de evidências úteis e observabilidade.
- Decisões importantes e difíceis de reverter devem ser registradas em ADRs.

## Rastreabilidade conceitual

```text
Requisito -> Caso de Teste -> Execução de Teste -> Evidência -> Defeito -> Release
```

Os identificadores são estáveis e legíveis por humanos, por exemplo `REQ-CAT-001`, `TC-API-CAT-001`, `DEF-001` e a release `v1.0.0`.

## Restrições atuais

- A documentação é a autoridade para a fundação, mas detalhes de implementação podem evoluir por meio de ADRs.
- Os valores-limite nas políticas de qualidade, segurança e performance são propostas iniciais e devem ser calibrados com evidências.
- As instruções de execução local serão adicionadas quando os primeiros componentes executáveis existirem.

## Metas de entrega

- **MPR — Release Mínima de Portfólio (Minimum Portfolio Release):** comprova o catálogo seguro, o caminho de engenharia reproduzível, evidências de qualidade em camadas, rastreabilidade e gates críticos sem exigir todos os diferenciais avançados.
- **v1.0 — Visão Completa:** preserva a Inteligência de Release, a Pontuação de Qualidade/risco/recomendação validados, o Laboratório de Falhas e a demonstração completa de observabilidade planejada.

A MPR impede que funcionalidades avançadas tornem o portfólio impossível de concluir; ela não reduz os gates aplicáveis de qualidade ou segurança. Consulte o [roadmap](docs/ROADMAP.md#mpr--release-mínima-de-portfólio).

## Contribuição

Antes de alterar o projeto, leia [AGENTS.md](AGENTS.md), os requisitos e as seções de arquitetura relevantes, além da fase aplicável em [PLANS.md](PLANS.md). Não implemente uma fase do roadmap sem aprovação humana explícita.

## Licença

Nenhuma licença foi selecionada. Até que uma seja adicionada, nenhum direito de reutilização é concedido por padrão.
