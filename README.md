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

A fundação executável da Fase 01 usa JDK 25 LTS, Spring Boot 4.1.0, Maven 3.9.16 por meio do Maven Wrapper, Jar executável, coordenadas Maven `io.github.sytef:aegis` e pacote-base `io.github.sytef.aegis`. O workflow de CI usa `permissions: contents: read` e está configurado para validar o build em Linux e Windows; a Fase 01 permanece aguardando o gate da execução remota.

Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch e microservices estão explicitamente excluídos, a menos que um problema mensurável os justifique.

## Estado do repositório

Este repositório está atualmente na **v0.1 — Fundação**. Ele contém a documentação oficial e um backend mínimo executável com health, info seguro, Problem Details, correlação, logging estruturado, testes e CI. Ainda não há frontend, banco de dados, mensageria, object storage, autenticação nem comportamento de negócio do catálogo.

## Execução local

### Pré-requisitos

- JDK 25 LTS disponível em `JAVA_HOME` e no `PATH`.
- Acesso à internet na primeira execução para que o Maven Wrapper obtenha a distribuição e as dependências verificadas.
- Nenhum banco de dados, container ou serviço externo é necessário na Fase 01.

O Maven Wrapper fixa Maven 3.9.16 e valida o checksum SHA-256 da distribuição. Não é necessário instalar Maven separadamente.

### Build e testes

Windows:

```powershell
.\mvnw.cmd verify
```

Linux/macOS:

```bash
./mvnw verify
```

Se um checkout em sistema POSIX não preservar a permissão de execução do script, execute uma única vez `chmod +x mvnw` antes dos comandos acima.

### Executar durante o desenvolvimento

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

### Executar o Jar

Depois de `verify`:

```bash
java -jar target/aegis-0.1.0-SNAPSHOT.jar
```

A aplicação escuta por padrão somente em `127.0.0.1:8080`.

| Endpoint | Finalidade |
| --- | --- |
| `http://127.0.0.1:8080/actuator/health/liveness` | Vida do processo |
| `http://127.0.0.1:8080/actuator/health/readiness` | Prontidão para receber tráfego |
| `http://127.0.0.1:8080/actuator/info` | Nome e versão do serviço/build e SHA opcional |

Para expor o commit em `/actuator/info`, defina `AEGIS_COMMIT_SHA` com 7–64 caracteres hexadecimais. Um valor ausente ou inválido é omitido. Somente os endpoints `health` e `info` são expostos.

Windows/PowerShell:

```powershell
$env:AEGIS_COMMIT_SHA = git rev-parse HEAD
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
AEGIS_COMMIT_SHA="$(git rev-parse HEAD)" ./mvnw spring-boot:run
```

Encerre a aplicação no terminal com `Ctrl+C`. Se a inicialização informar que a porta 8080 já está em uso, encerre o processo anterior antes de tentar novamente. No Windows, `Get-NetTCPConnection -LocalPort 8080 -State Listen` ajuda a identificar a porta ocupada; em Linux/macOS, use a ferramenta disponível no sistema, como `lsof -i :8080` ou `ss -ltnp`.

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

- A documentação e os contratos executáveis da Fase 01 são a autoridade para a fundação; detalhes posteriores podem evoluir por meio de requisitos, testes e ADRs.
- Os valores-limite nas políticas de qualidade, segurança e performance são propostas iniciais e devem ser calibrados com evidências.
- A Fase 01 não implementa API pública de negócio sob `/api/v1`; seus endpoints são exclusivamente operacionais.

## Metas de entrega

- **MPR — Release Mínima de Portfólio (Minimum Portfolio Release):** comprova o catálogo seguro, o caminho de engenharia reproduzível, evidências de qualidade em camadas, rastreabilidade e gates críticos sem exigir todos os diferenciais avançados.
- **v1.0 — Visão Completa:** preserva a Inteligência de Release, a Pontuação de Qualidade/risco/recomendação validados, o Laboratório de Falhas e a demonstração completa de observabilidade planejada.

A MPR impede que funcionalidades avançadas tornem o portfólio impossível de concluir; ela não reduz os gates aplicáveis de qualidade ou segurança. Consulte o [roadmap](docs/ROADMAP.md#mpr--release-mínima-de-portfólio).

## Contribuição

Antes de alterar o projeto, leia [AGENTS.md](AGENTS.md), os requisitos e as seções de arquitetura relevantes, além da fase aplicável em [PLANS.md](PLANS.md). Não implemente uma fase do roadmap sem aprovação humana explícita.

## Licença

Nenhuma licença foi selecionada. Até que uma seja adicionada, nenhum direito de reutilização é concedido por padrão.
