# Estratégia de Engenharia de Qualidade e Testes do AEGIS

## Finalidade

Esta estratégia define como o AEGIS constrói confiança no comportamento do produto e nas decisões de release. Ela se aplica ao design, à implementação, à entrega e à operação. Não é uma instrução para automatizar todo teste nem maximizar uma contagem de testes por vaidade.

A estratégia é baseada em risco e orientada por evidências. A responsabilidade pela qualidade é compartilhada: Produto esclarece valor e aceite; desenvolvimento previne e detecta defeitos perto do código; profissionais de Engenharia de Qualidade moldam cobertura e investigação; especialistas em segurança desafiam ameaças; e responsáveis por releases tomam decisões explícitas.

## Política inegociável

> Nunca modifique um teste apenas para fazê-lo passar.

Um teste com falha é evidência a investigar. Determine se a causa está no comportamento do produto, design/implementação do teste, ambiguidade do requisito, dados, ambiente, ferramenta ou em uma alteração aceita. Um teste só pode mudar quando o comportamento esperado ou o próprio teste estiver demonstravelmente errado, com revisão e rastreabilidade.

Testes não devem ser excluídos, pulados, colocados em quarentena, escondidos por retry nem enfraquecidos somente para obter um pipeline verde. Exceções seguem a política de testes instáveis e permanecem visíveis.

## Princípios de Engenharia de Qualidade

1. Prevenir defeitos com requisitos claros, exemplos, revisão de design e limites simples.
2. Testar na camada eficaz mais baixa; adicionar cobertura em camadas superiores para confiança de integração e do usuário.
3. Priorizar por impacto de negócio, probabilidade, detectabilidade e exposição da mudança.
4. Tornar falhas relevantes para produção observáveis e reproduzíveis em ambientes seguros.
5. Tratar código, dados e ferramentas de teste como ativos com qualidade de produção.
6. Separar falhas do produto de falhas de teste e infraestrutura nos relatórios.
7. Preservar evidência, identidade do candidato/build da release, ambiente e versão da política.
8. Usar automação para verificações repetíveis; usar exploração humana para descoberta, ambiguidade e experiência.
9. Projetar acessibilidade, segurança, performance, resiliência e integridade de dados desde o início.
10. Preferir testes determinísticos e limites controlados a sleeps e retries amplos.
11. Não usar pontuação agregada para ocultar falha crítica.
12. Refinar continuamente a cobertura usando defeitos, telemetria, padrões de alteração e riscos que escaparam.

## Baseline executável da Fase 01

A fundação atual é validada por `./mvnw verify` ou `.\mvnw.cmd verify`. A suíte cobre bootstrap do contexto, contratos HTTP reais em porta aleatória, liveness/readiness, allowlist e matriz negativa do Actuator, `info`, Problem Details para 400/404/405/406/415/500, limites e entradas adversariais do `X-Correlation-ID`, presença do header, MDC, isolamento entre solicitações, concorrência determinística, `Error`, redispatch, resposta committed, sanitização de path e campos seguros/diagnosticáveis dos logs estruturados. Controllers que exercitam erros existem somente no escopo de testes.

Testes unitários exercitam a política pura de correlação e a contribuição segura de build. Testes de componente/integração exercitam o filtro e o servidor HTTP real. A regra ArchUnit atual protege apenas uma direção útil e existente: `foundation.correlation` não depende de Spring, Jakarta nem `foundation.web`. Não existe meta percentual artificial de cobertura, retry automático ou `sleep` arbitrário.

## Atividades de qualidade no ciclo de vida

| Etapa | Atividades | Evidências |
| --- | --- | --- |
| Descoberta | Análise de personas/riscos, exemplos, fora de escopo, casos de abuso | requisitos revisados, premissas, questões em aberto |
| Design | Revisão de arquitetura/testabilidade, modelo de ameaças, modos de falha, design de contratos/dados | achados de design, ADRs, abordagem de teste |
| Implementação | Análise estática, testes de unidade/componente, revisão segura, verificações exploratórias locais | relatórios de build/testes e diff revisado |
| Integração | Verificações de API, banco de dados, contrato, mensageria e migração | relatórios versionados de execução e traces |
| Sistema | E2E críticos, acessibilidade, exploração, performance e resiliência | evidências vinculadas ao build/release |
| Release | Avaliação de gates, rastreabilidade, revisão de risco residual e decisão humana | snapshot imutável de evidências e justificativa |
| Operação | Revisão de telemetria, aprendizado com incidentes, verificações sintéticas quando justificadas | alertas, traces, links de defeito e regressão |

## Testes Baseados em Risco (Risk Based Testing)

### Modelo de risco

Cada capacidade é avaliada por:

- **Impacto:** perda de negócio, dano de segurança/privacidade, corrupção de dados, credibilidade da release ou custo de recuperação.
- **Probabilidade:** complexidade, novidade, frequência de mudança, quantidade de integrações e defeitos anteriores.
- **Detectabilidade:** probabilidade de controles existentes revelarem o problema antes dos usuários.
- **Exposição:** frequência de uso e quantidade de usuários/dados afetados.

Uma classificação qualitativa inicial (Crítico, Alto, Médio, Baixo) é registrada no planejamento. Um modelo numérico pode apoiar a priorização no futuro, mas deve permanecer explicável.

### Áreas iniciais de alto risco

| Área | Principais riscos | Ênfase exigida |
| --- | --- | --- |
| Autenticação/RBAC | comprometimento de conta, elevação de privilégio, enumeração | política em unidade, API negativa/abuso, verificações de segurança e auditoria |
| Preço/estoque/SKU | corrupção silenciosa, duplicidades, atualizações perdidas | testes de limite/propriedade, constraints do banco, concorrência e API |
| Upload de mídia | arquivo malicioso, exaustão de recursos, acesso não autorizado | validação de conteúdo, autorização, testes de segurança e resiliência |
| Outbox/mensageria | alterações perdidas/duplicadas, tempestade de retry, trabalho travado | integração transacional, idempotência, reinício e testes de falha |
| Ingestão de qualidade | atribuição ao build errado, evidência duplicada/desatualizada/malformada | schema, idempotência, qualidade de dados e autorização |
| Decisão de qualidade/release | risco crítico ocultado pela pontuação, resultado não reproduzível | testes determinísticos da política, gates críticos, auditoria/rastreabilidade |
| Laboratório de Falhas | blast radius não pretendido ou falha persistente | autorização, negação em produção, TTL e parada de emergência |

O risco determina profundidade, independência e posicionamento no gate. Baixo risco não significa ausência de teste; pode significar cobertura focada de unidade/API e amostragem exploratória em vez de ampla automação E2E.

## Arquitetura de testes: uma pirâmide prática

~~~mermaid
flowchart TB
    E2E["Poucos: jornadas críticas ponta a ponta"]
    API["Focados: cenários de API, contrato, acessibilidade, resiliência e performance"]
    INT["Fortes: testes de componente e integração nos limites de módulo/dados/mensagens"]
    UNIT["Amplos: testes rápidos de unidade e políticas"]
    UNIT --> INT --> API --> E2E
~~~

Este é um modelo de feedback e isolamento, não uma meta de porcentagem fixa. A maioria das permutações de negócio pertence abaixo da UI. Testes E2E comprovam um pequeno conjunto de jornadas críticas e integrações entre limites. Testes não funcionais atravessam os níveis em vez de ficar no topo da pirâmide.

## Níveis e tipos de teste

### Testes unitários

Escopo: regras puras de domínio, value objects, validadores, políticas, mapeadores e transições de estado sem rede/banco real.

Exemplos principais:

- normalização de SKU e comportamento da decisão de unicidade;
- limites de dinheiro/estoque e regras de transição do produto;
- políticas de permissão e avaliação de gates;
- fórmula de Pontuação de Qualidade/risco, tratamento de dados ausentes e prevalência de bloqueio crítico;
- classificação de retry/cálculo de backoff;
- mapeamento de schema de evento e redação.

Características: milissegundos, determinísticos, isolados, legíveis e com ampla cobertura de limites. Testes de propriedade/parametrizados são preferidos para invariantes com muitas dimensões. Faça mock apenas de portas sob nossa responsabilidade, não de todo método interno.

### Testes de componente

Escopo: um módulo por seu limite público de aplicação/API, com dependências externas substituídas por fakes realistas ou dependências efêmeras conforme apropriado.

Finalidade: verificar wiring, serialização, filtros de autorização, mapeamento de erros e comportamento do módulo sem iniciar o produto completo. Testes de componente não devem contornar os mesmos caminhos de validação/autorização usados em produção apenas por conveniência.

### Testes de integração

Escopo: integração real com PostgreSQL, RabbitMQ, MinIO e o adaptador do mock externo, introduzidos somente em suas fases.

Cenários exigidos incluem:

- migrações e constraints do banco;
- rollback de transação e concorrência otimista;
- atomicidade da outbox, reinício do publicador e reconciliação de item travado;
- entrega duplicada/idempotência, retry e comportamento de dead-letter;
- upload/processamento/limpeza de objeto e armazenamento indisponível;
- timeout do worker e classificação de erro downstream.

Use versões de dependências compatíveis com produção em containers isolados quando viável. Um repositório com mock não é evidência de que constraints SQL ou transações funcionam.

### Testes de API

Escopo: comportamento HTTP independente da UI.

Cobrir:

- caminhos feliz, negativo, de limite e transição de estado;
- diferenças entre autenticação e autorização;
- permissão no nível do objeto, mass assignment e entradas de injeção;
- schema de validação/erro e ID de correlação;
- máximos de paginação, ordenação determinística e combinações de filtros;
- concorrência e idempotência;
- negociação de conteúdo, controles de tamanho/taxa e compatibilidade.

Testes de API carregam a maior parte da cobertura funcional entre funcionalidades porque são mais rápidos e diagnosticáveis que E2E de UI.

### Testes de contrato

Contratos incluem OpenAPI HTTP, schemas de eventos, Mock do Centro de Vendas Externo e formatos de ingestão das fontes de qualidade.

- Verificações de schema/conformidade do provedor protegem os contratos do AEGIS.
- Exemplos orientados pelo consumidor protegem premissas sobre o mock downstream quando úteis.
- Testes de compatibilidade detectam mudanças incompatíveis antes do merge.
- Testes de contrato validam comportamento de falha/erro, timeouts e rejeição de versão, não apenas campos de payload bem-sucedidos.
- Um mock não é considerado correto apenas porque corresponde à implementação; ambos são verificados contra o contrato revisado.

### Testes ponta a ponta

Use Playwright + TypeScript para um conjunto deliberadamente pequeno de jornadas no navegador depois que o frontend existir. Jornadas candidatas iniciais:

1. operador autorizado cria e atualiza um produto;
2. usuário não autorizado não consegue alterar estado do catálogo;
3. operador de mídia faz upload de imagem e observa o resultado do processamento;
4. gerente de release revisa evidência, gate bloqueante, pontuação/risco e registra decisão;
5. profissional de QA navega pela rastreabilidade requisito-resultado-defeito.

Testes E2E devem usar roles/labels acessíveis, hooks de teste estáveis voltados ao domínio somente quando necessários, dados controlados e esperas explícitas por condição. Sleeps arbitrários são proibidos. Testes de UI não duplicam cada combinação de campo coberta em camadas inferiores.

### Testes de segurança

A verificação de segurança segue [SECURITY.md](SECURITY.md) e inclui:

- análise estática, varredura de secrets e varredura de dependências/containers;
- testes de autenticação/sessão, RBAC e autorização no nível de objeto;
- verificações de validação de entrada, injeção e exposição insegura de erros;
- testes de polyglot/assinatura/tamanho/descompressão de upload e recuperação;
- testes de abuso/limite de taxa de API, CSRF/CORS/headers de segurança conforme a arquitetura;
- proveniência e replay/falsificação de mensagens/evidências;
- testes manuais orientados por ameaças para funcionalidades de alto risco.

Scanners automatizados produzem candidatos, não defeitos automaticamente aceitos. Achados exigem triagem, contexto de explorabilidade, correção e novo teste. Achados críticos exploráveis bloqueiam conforme [QUALITY_GATES.md](QUALITY_GATES.md).

### Testes de performance

Use k6 para modelos de workload de API versionados depois que os endpoints relevantes se estabilizarem. Categorias de teste:

- smoke: correção do script/ambiente;
- baseline: workload normal reproduzível;
- load: concorrência/volume esperados;
- stress: descoberta de limites, nunca requisito rotineiro de release sem necessidade;
- soak: vazamentos/acúmulo de fila ao longo do tempo quando justificado;
- spike: pico súbito de ingestão/catálogo quando o risco justificar.

Todo resultado registra candidato/commit/build da release, ambiente, dataset, versões das dependências, workload, warm-up, duração e contexto de recursos. Avalie percentis de latência, throughput, taxa de erro, saturação, idade da fila e recuperação — não somente latência média. Limites devem usar perfil controlado de referência e distinguir erros do produto de limites do gerador de carga/ambiente.

Metas provisórias iniciais estão em [REQUIREMENTS.md](REQUIREMENTS.md#requisitos-não-funcionais); baselines devem validá-las antes de se tornarem gates rígidos.

### Testes de resiliência

Verificações de resiliência validam o comportamento durante e depois de falhas controladas:

- downstream indisponível, timeout e respostas lentas;
- broker indisponível, backlog e entrega duplicada;
- latência do banco de dados ou esgotamento de conexões;
- falha do processador de imagem/object storage;
- HTTP 500 interno aleatório;
- reinício do worker durante processamento;
- Motor de Qualidade ou fonte de evidência indisponível.

Cada experimento declara hipótese, estado estável, falha injetada, blast radius, telemetria esperada, condição de aborto e critério de recuperação. O Laboratório de Falhas é não produtivo, protegido por permissão, desligado por padrão, tem TTL e parada de emergência. Passar significa que o sistema falha conforme projetado e se recupera sem perda silenciosa — não que nenhum erro ocorreu.

### Testes de acessibilidade

Buscar WCAG 2.2 AA nos fluxos críticos suportados. Combinar:

- revisão semântica de design/componentes;
- regras automatizadas em verificações de componente e Playwright;
- navegação somente por teclado e foco visível;
- smoke com leitor de tela nas jornadas críticas;
- zoom/reflow, contraste, identificação de erro e anúncio de status;
- movimento reduzido e indicadores que não dependam apenas de cor quando aplicável.

Ferramentas automatizadas de acessibilidade detectam apenas uma parte dos problemas; avaliação manual é exigida antes de chamar um fluxo crítico de UI de acessível.

### Testes de qualidade de dados

Validar dados de comércio e evidências de qualidade:

- constraints de banco, nulidade, precisão e unicidade normalizada;
- migrações contra dados representativos e plano de recuperação por rollback/avanço;
- precisão de histórico antes/depois e atribuição de auditoria;
- reconciliação entre evento/banco e detecção de duplicidade;
- schema de evidência, fonte, identidade de release/build, timestamp/atualização e checksum/referência;
- correção de agregações de taxa de aprovação, contagem de severidade, percentis e entradas da pontuação;
- entradas ausentes, tardias, duplicadas, conflitantes e fora de ordem;
- retenção/redação e detecção de órfãos.

Dashboards de qualidade nunca devem transformar entrada desconhecida ou desatualizada em zero/aprovação.

### Testes exploratórios

Charters com prazo focam ambiguidade, fluxos, recuperação de erros e interações entre domínios. Um charter registra missão, build/ambiente, dados, observações, evidências, defeitos e questões restantes. Tours iniciais sugeridos incluem limites/concorrência do catálogo, uso indevido de permissão, uploads hostis, retry/replay e explicação da decisão de release.

## Verificações estáticas e revisão

As verificações atuais incluem Maven Enforcer, compilação, testes de unidade/componente/integração e ArchUnit. O workflow de CI está configurado para executar `verify` em Linux e Windows; o gate só fica satisfeito depois da execução remota bem-sucedida. Formatação/lint dedicado, varredura automatizada de dependências/secrets e compatibilidade de contrato ainda não foram introduzidos; tornam-se gates quando uma ferramenta e política forem aprovadas. A revisão examina correção, clareza, testabilidade, segurança, telemetria, migração/recuperação e documentação — não apenas percentual de cobertura.

## Ambientes de teste

| Ambiente | Finalidade | Dados | Controles esperados |
| --- | --- | --- | --- |
| Local | desenvolvimento rápido e testes focados | dados sintéticos gerados/seed | dependências reproduzíveis, padrões seguros, sem secrets reais |
| CI efêmera | validação automatizada isolada por alteração | factories/seeds sintéticos determinísticos | versões fixadas, isolamento paralelo, relatórios preservados em falha |
| Integração | cenários entre componentes, contrato e migração | dataset sintético representativo | resets controlados, mock externo, broker/armazenamento conforme as fases os adicionarem |
| Performance | baseline reproduzível de workload | dataset sintético maior e versionado | perfil estável de recursos, consciência de exclusividade/noisy neighbor |
| Demonstração/staging | jornada de portfólio e validação exploratória | identidades/dados sintéticos de demonstração | configuração semelhante à produção quando prático, sem dados de produção |
| Produção (futuro) | operação real, não ambiente destrutivo de testes | dados reais governados | sem Laboratório de Falhas; apenas smoke/monitoramento sintético seguro se aprovado |

Paridade de ambiente é baseada em risco. Diferenças de versões, configuração, topologia e feature flags são documentadas junto aos resultados. Nenhum teste depende de estado não registrado da máquina de um desenvolvedor.

## Estratégia de dados de teste

- Usar dados sintéticos e determinísticos por padrão; nunca copiar dados pessoais de produção para ambientes inferiores.
- Fornecer builders/factories com padrões válidos de domínio e sobrescritas explícitas.
- Gerar chaves únicas de identidade/SKU de teste sem depender da ordem de execução.
- Manter um pequeno dataset de referência versionado para demonstrações/contratos e outro escalável para performance.
- Criar dados pela camada sob teste, salvo quando o custo de preparação obscurecer o alvo; configuração em nível inferior deve preservar os invariantes exigidos.
- Isolar execuções paralelas por namespaces/IDs únicos e limpar com segurança; testes devem tolerar retenção diagnóstica em caso de falha.
- Tratar relógio, timezone, localidade, moeda, Unicode e limites numéricos como dimensões explícitas.
- Redigir secrets/dados pessoais em relatórios, capturas de tela, traces e mensagens de falha.
- A limpeza de teste nunca mira ambientes amplos ou ambíguos e nunca oculta falha anterior do teste.

## Política de evidências

Proveniência mínima da execução:

- IDs de requisito/caso de teste quando aplicáveis;
- candidato da release, commit de origem e identidade imutável do build/artefato;
- versão do teste/ferramenta e comando/perfil;
- ambiente e versões relevantes de dependência/configuração;
- início/fim, resultado, duração e histórico de tentativas;
- classificação/mensagem sanitizada da falha;
- links/checksums de relatórios, capturas de tela, vídeos, logs e traces conforme apropriado;
- ID de correlação/trace para falhas entre limites.

A evidência é proporcional: não colete artefatos sensíveis ou enormes por padrão. Execuções aprovadas podem reter resumos; falhas e execuções de gate de release retêm artefatos diagnósticos sob política explícita de retenção/acesso. Capturas de tela isoladas são prova insuficiente de correção de backend/dados.

## Rastreabilidade

A cadeia-alvo é:

~~~text
Requisito -> Caso de Teste -> Execução de Teste -> Evidência -> Defeito -> Release
~~~

- IDs de requisitos se originam em [REQUIREMENTS.md](REQUIREMENTS.md).
- Testes automatizados incluem metadados estáveis de caso/requisito sem tornar os nomes ilegíveis.
- Execuções e resultados vinculam-se a um candidato/build exato da release; apenas a versão exibida da release é insuficiente.
- Defeitos vinculam expectativa que falhou, evidência e requisito/release afetado.
- Relatórios de rastreabilidade mostram links e lacunas; um link ausente não é sintetizado.
- A cobertura de regressão é escolhida por risco, módulos/contratos afetados e defeitos históricos, não apenas por rastreabilidade.

## Ciclo de vida do defeito

1. **Observado:** preservar ambiente, build, dados, passos, esperado/real e evidência.
2. **Triado:** confirmar reprodutibilidade, classificar problema de produto/teste/ambiente/requisito, severidade, prioridade e responsável.
3. **Aceito:** decidir corrigir, adiar, marcar duplicado ou não-defeito, com justificativa e requisitos/releases afetados.
4. **Em andamento:** implementar a menor alteração correta e adicionar/ajustar cobertura legítima de prevenção/detecção.
5. **Pronto para novo teste:** identificar build e escopo de regressão afetado.
6. **Verificado/fechado:** reproduzir o cenário original, verificar a correção e a regressão direcionada; reter evidência/histórico.
7. **Reaberto:** se o comportamento persistir/voltar, adicionar nova evidência sem sobrescrever a verificação anterior.

Severidade descreve impacto; prioridade descreve agendamento. Uma falha instável ou de ambiente ainda é rastreada e tem responsável; não é reclassificada como aprovação do produto.

## Princípios de automação

- Automatizar quando repetição, risco de regressão, permutações de dados ou feedback rápido justificarem o custo de manutenção.
- Manter asserções focadas em resultados e contratos de negócio, não em implementação incidental.
- Preferir interfaces públicas e locators acessíveis; evitar asserções no banco como única prova de comportamento visível ao usuário.
- Não usar ordem de teste com estado mutável compartilhado, sleep incondicional, retry infinito nem captura com descarte.
- Retry existe para caracterizar comportamento transitório, não escondê-lo; todas as tentativas permanecem visíveis.
- Utilitários de teste permanecem mais simples que o comportamento verificado e recebem revisão/testes proporcionais ao risco.
- Relatórios gerados são artefatos, não ruído versionado no código-fonte.
- Métricas de cobertura revelam código/requisitos não exercitados, mas não comprovam a qualidade das asserções.

## Política de testes instáveis

Um teste instável apresenta resultados inconsistentes para o mesmo produto/configuração/entrada relevante. Suspeita de instabilidade exige:

1. preservar todas as tentativas e a evidência da primeira falha;
2. criar item rastreado com responsável, severidade, suíte afetada e primeira/última ocorrência;
3. classificar a provável fonte: não determinismo do produto, teste, dados, ambiente ou ferramenta;
4. reproduzir sob repetição controlada e usar telemetria para localizar a condição de corrida/limite;
5. corrigir a causa raiz e comprovar estabilidade por execução repetida acordada;
6. adicionar o aprendizado a utilitários/padrões quando sistêmico.

Quarentena é ação de contenção excepcional, com prazo, aprovada por responsável. Um teste em quarentena:

- continua executado e relatado separadamente quando possível;
- nunca conta como aprovado;
- tem item, responsável e expiração;
- não pode remover cobertura de gate crítico sem evidência substituta e risco explícito de release;
- é restaurado ou substituído após a correção da causa raiz.

Retries globais que transformam sucesso eventual em aprovação sem qualificação são proibidos. Relate separadamente taxa de aprovação na primeira tentativa e resultado do retry.

## Expectativas de entrada e saída

Antes que uma funcionalidade entre em implementação, requisitos, exemplos, riscos, preocupações de segurança, observabilidade e abordagem de teste aplicáveis devem estar compreendidos. Antes de ser considerada pronta, build/lint/testes relevantes, revisão de segurança, documentação, telemetria e rastreabilidade passam conforme [AGENTS.md](../AGENTS.md) e [QUALITY_GATES.md](QUALITY_GATES.md).

## Métricas da estratégia

Sinais úteis incluem padrões de defeitos que escaparam, camada de detecção da falha, taxa de mudança com falha/reabertura, estabilidade na primeira tentativa, cobertura de requisitos críticos, atualização das evidências, tempo médio para diagnóstico e idade das exceções de gate. Essas métricas orientam melhoria e não devem ser usadas para classificar pessoas nem incentivar contagens superficiais de testes.

## Questões em aberto e necessidades de calibração

- Navegadores/dispositivos suportados e matriz manual de acessibilidade exatos.
- Hardware/dataset/workloads de referência para limites de performance.
- Retenção, armazenamento e política de acesso de evidências.
- Independência exigida para avaliação de release/segurança em uma equipe do tamanho do portfólio.
- Fonte da verdade para casos de teste manuais e defeitos antes de o Centro de Controle de Qualidade existir.
- Duração aceitável da quarentena por classe de risco.
- Pesos mínimos da pontuação e atualização exigida das evidências por classe de release.
