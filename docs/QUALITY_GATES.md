# Gates de Qualidade e Inteligência de Release do AEGIS

## Finalidade

Gates de Qualidade transformam evidências e política de risco em decisões no momento adequado. Eles são progressivos: feedback rápido executa cedo e validações mais amplas executam conforme uma alteração se aproxima da release. Gates protegem o produto; não são alvos a manipular.

Esta é uma proposta inicial de política. Limites e a Pontuação de Qualidade exigem calibração com baselines reais antes da aplicação. Definições de gates, fórmulas e regras de exceção devem ser versionadas para que uma decisão histórica de release permaneça explicável.

## Regra fundamental

**A Pontuação de Qualidade é uma entrada derivada da política. Regras de bloqueio crítico têm autoridade.**

Uma política calibrada pode usar pontuação baixa para produzir recomendação BLOCK, mas nenhuma pontuação é evidência por si só e nenhuma pontuação alta cancela um bloqueio crítico. Um candidato com 99/100 ainda é bloqueado por um achado crítico de segurança ativo, falha crítica de teste ou outra condição não substituível. Evidência obrigatória desconhecida, desatualizada ou ausente nunca é tratada como aprovada.

## Modelo de resultado dos gates

Todo gate produz um dos seguintes resultados:

- PASS: as condições exigidas foram avaliadas e atendidas;
- FAIL: as condições avaliadas não atenderam à política;
- INSUFFICIENT_EVIDENCE: entrada obrigatória está ausente, desatualizada, incompatível ou não pode ser atribuída à release;
- NOT_APPLICABLE: a política exclui explicitamente o gate/capacidade para este candidato e registra justificativa, ator autorizado, versão exata da política e referência de auditoria;
- ERROR: o gate não pôde ser avaliado por falha do sistema/ferramenta.

NOT_APPLICABLE não é escolhido por omissão nem por um cliente que produz evidências. Só está disponível por meio de avaliação de política autorizada e auditada. ERROR e INSUFFICIENT_EVIDENCE são distintos de falha do produto, mas bloqueiam quando a política exige a evidência.

## Maturidade progressiva

Capacidades de qualidade tornam-se aplicáveis somente quando seus pré-requisitos existem. Inaplicabilidade nunca significa aprovação.

| Maturidade | Resumo de evidências | Avaliação de gates críticos | Pontuação de Qualidade | Classificação de Risco | Recomendação do Motor |
| --- | --- | --- | --- | --- | --- |
| Fundação / Fase 01 | Somente evidência de documentação/build | Gates aplicáveis executam | NOT_APPLICABLE | NOT_APPLICABLE | NOT_APPLICABLE |
| Catálogo até fundação de evidências (v0.2–v0.6) | Exigido para candidato/build exato conforme cada fonte existir | Aplicável e independente da pontuação | NOT_APPLICABLE | NOT_APPLICABLE | NOT_APPLICABLE |
| Maturidade do Motor de Qualidade (v0.7+) | Exigido e atual | Exigida primeiro | Calculada somente sob versão de fórmula validada | Calculada somente sob política de risco validada | APPROVE, REVIEW ou BLOCK sob política versionada |

Antes da v0.7, a revisão de release usa evidências atuais, gates críticos, risco residual explícito e decisão humana. Não deve sintetizar pontuação/risco nem inferir aprovação a partir de valores NOT_APPLICABLE.

Antes de o Centro de Controle de Qualidade existir, NOT_APPLICABLE nesta tabela é uma regra documental de maturidade, não um registro de runtime fabricado. Quando uma API de resumo persistir aplicabilidade, deverá conter ator/versão autorizados da política e referência de auditoria.

### Gate aplicável à Fase 01

Para a fundação executável, a evidência exigida é: Maven Wrapper fixado e íntegro, Maven Enforcer para Java/Maven aprovados, `mvnw verify` verde, testes de contexto/unidade/componente/HTTP, regra ArchUnit, Jar executável, smoke manual dos endpoints operacionais, revisão da árvore de dependências, `git diff --check` e revisão de segurança/documentação. O workflow de CI deve repetir `verify` em Linux e Windows com privilégio mínimo; configuração sem execução remota bem-sucedida não satisfaz o gate.

Não há Pontuação de Qualidade, Classificação de Risco nem recomendação automática nesta fase. Varredura automatizada de secrets/dependências, SBOM, assinatura e proveniência de artefato ainda não são gates implementados; a revisão de dependências e secrets é manual até que ferramenta e política sejam aprovadas.

### Gates aplicáveis à Fase 02

A Fase 02 acrescenta, sem remover os gates da fundação:

- backend build/testes/arquitetura em Linux e Windows;
- integração PostgreSQL 18.4 real para migrations, constraints, atomicidade, unicidade, optimistic locking e invariável concorrente Product/Category;
- conformidade de API, Problem Details, ETag/If-Match e contrato OpenAPI;
- frontend install reproduzível, lint, typecheck, testes de unidade/componente/integração/acessibilidade e build;
- smoke full-stack manual com frontend, backend e PostgreSQL reais;
- Design Quality Gate;
- Gate de Exposição Externa, que permanece bloqueante enquanto Auth/RBAC não existirem.

Playwright, Pontuação de Qualidade, Classificação de Risco, recomendação automática, Prometheus, Grafana e OpenTelemetry não são evidências exigidas da Fase 02.

## Gates progressivos

### 1. Gate de Pull Request

Objetivo: impedir que defeitos conhecidos e alterações inseguras entrem na branch padrão, preservando feedback rápido.

Exigido quando aplicável:

- alteração revisada com requisito/item e contexto de risco;
- compilação/verificação de tipos;
- formatação e lint/análise estática;
- testes de unidade e componente do escopo alterado;
- verificações de compatibilidade de contrato/schema;
- varredura de secrets e verificações de código-fonte/dependência com alto sinal;
- validação de migração quando houver alteração de schema;
- revisão de impacto em documentação, observabilidade e segurança;
- nenhum teste ignorado/enfraquecido nem asserção crítica reduzida sem explicação.

Meta de feedback: verificações rápidas normalmente devem terminar em até 10 minutos quando existir uma baseline; suítes justificadamente mais lentas executam em paralelo ou em gates posteriores sem remover proteção essencial do PR.

Falhas críticas: falha de build/tipos, falha determinística de teste relevante, secret versionado detectado, contrato incompatível sem migração aprovada ou enfraquecimento não autorizado de controles/testes.

### 2. Gate de Build

Objetivo: comprovar que um candidato reproduzível e identificável foi construído a partir de código revisado.

Exigido:

- build limpo a partir do commit selecionado;
- identidade do artefato e commit de origem registrados;
- resolução/lock de dependências reproduzível;
- varredura de artefato/container quando esses artefatos existirem;
- verificações exigidas de configuração/schema aprovadas;
- inventário de software/proveniência adicionado conforme a maturidade da fase de release.

Um artefato reconstruído a partir de código ou resolução de dependências diferente é evidência diferente, mesmo que use a mesma versão de exibição.

### 3. Gate de Testes

Objetivo: avaliar correção funcional e confiança em dados/integração do candidato.

Exigido conforme o escopo da alteração e release:

- suítes de unidade, componente e integração passam;
- suítes de API e contrato passam;
- jornadas E2E críticas selecionadas passam quando a política da fase as exigir; na Fase 02, a evidência de navegador é um smoke full-stack manual e Playwright permanece adiado;
- verificações de qualidade de dados/migração/reconciliação passam;
- a rastreabilidade de requisitos não tem lacunas críticas sem explicação;
- testes instáveis/em quarentena são relatados separadamente com responsável e expiração;
- resultados de teste correspondem ao build exato e ao ambiente/perfil aprovado.

Falhas críticas: falha de caso de teste crítico; comportamento de corrupção/atualização perdida/mensagem perdida; suíte obrigatória indisponível ou atribuída a outro build; teste crítico em quarentena sem evidência equivalente.

### Gate de Qualidade de Design

Objetivo: tornar a qualidade visual e de interação uma propriedade verificável, não uma opinião estética.

O gate é obrigatório para a experiência Product da Fase 02 e falha quando ocorrer qualquer uma destas condições:

- uma tela/estado aprovado está ausente: loading, empty, no-results, erro, validação, conflito ou sucesso;
- a jornada listagem → criação → detalhe → edição → desativação não oferece ações/feedback claros;
- existe overflow horizontal, conteúdo essencial truncado ou layout quebrado nos viewports aprovados entre 320 e 1920 px;
- um fluxo crítico não é operável por teclado, perde foco ou remove o focus visível;
- contraste, nomes/roles, labels ou mensagens de erro não atendem à baseline WCAG 2.2 AA aprovada;
- submit duplicado é possível ou um HTTP 412 descarta silenciosamente a entrada do usuário;
- estado depende somente de cor, loading não possui conclusão/erro recuperável ou sucesso não é anunciado de forma perceptível;
- há erro de console não justificado;
- dados fictícios são usados somente para preencher a interface;
- componentes ignoram tokens/foundations sem justificativa e produzem inconsistência visível.

Evidência mínima: checklist versionado, resultados dos testes frontend aplicáveis e registro do smoke/revisão manual nos viewports e navegadores aprovados. “Estar bonito” não substitui esses critérios.

### 4. Gate de Segurança

Objetivo: impedir release com risco explorável conhecido e inaceitável.

Exigido conforme a exposição:

- varreduras de secrets, código-fonte, dependências e artefatos concluídas;
- testes de RBAC/autorização de objeto e regressão de segurança passam;
- alterações do modelo de ameaças revisadas para novos limites ou capacidades sensíveis;
- achados triados por severidade, explorabilidade, alcance e release afetada;
- verificações de upload/API/CI seguros aplicadas quando relevantes.

Falhas críticas:

- qualquer achado crítico explorável confirmado e não resolvido que afete o candidato;
- secret ou credencial válida exposta;
- bypass demonstrado de autenticação/autorização;
- bypass de integridade que permita forjar evidência de qualidade ou decisão de release;
- Laboratório de Falhas acessível em produção ou por ator não autorizado.

Um rótulo isolado do scanner exige triagem rápida; um problema crítico confirmado não pode ser diluído pela média.

### 5. Gate de Performance

Objetivo: garantir que workloads críticos atendam às expectativas versionadas de serviço e não apresentem regressão inaceitável.

Exigido quando o comportamento relevante para performance mudar ou para candidatos de release:

- workload, dataset, ambiente e identidade do build correspondem ao perfil aprovado;
- percentis de latência, throughput e taxa de erro atendem aos limites do cenário;
- saturação de recursos, idade da fila e recuperação são revisadas;
- regressão contra baseline válida é explicada.

As metas provisórias iniciais da API são p95 <= 300 ms para leituras do catálogo e p95 <= 500 ms para escritas no perfil de referência acordado. Não são gates rígidos até que uma baseline reproduzível defina workload e capacidade.

Falha crítica após calibração: a taxa de erro de performance excede o limite crítico do cenário, um limite crítico de SLO é violado ou o sistema não se recupera do workload aprovado. Invalidade do ambiente/gerador de carga produz ERROR, não PASS.

### Gate de Exposição Externa

Objetivo: impedir que uma prévia de desenvolvimento seja confundida com um sistema compartilhado seguro ou implantada como tal.

Para v0.2 e qualquer build posterior em que autenticação/RBAC reais estejam incompletas, este gate permite somente o perfil local/isolado documentado de desenvolvimento. Ele **falha** em qualquer solicitação para expor externamente mutações do catálogo.

O gate pode passar para exposição externa somente depois que a Fase 03 comprovar:

- comportamento real de autenticação/sessão e bootstrap seguro de administrador;
- autorização no servidor por role, objeto e estado;
- testes negativos para anônimo/negado/desativado/revogação de sessão;
- fechamento de todos os critérios de aceite de autorização pendentes da prévia do Catálogo;
- remoção da identidade de desenvolvimento como limite confiável de segurança;
- configuração de ambiente, modelo de ameaças e tratamento seguro de secrets revisados.

Este gate independe da Pontuação de Qualidade e é um bloqueio crítico para exposição hospedada/pública.

### 6. Gate de Release

Objetivo: combinar toda evidência atual em uma decisão explícita e auditável de release.

Exigido:

- escopo exato da release, commit/build e corte de evidência fixados;
- gates anteriores exigidos em PASS ou com exceção permitida e autorizada;
- regras de bloqueio crítico avaliadas com evidências atuais;
- Pontuação de Qualidade, Classificação de Risco e recomendação do Motor calculadas somente quando sua política validada se aplicar; caso contrário, cada uma fica explicitamente NOT_APPLICABLE com o registro autorizado da política;
- defeitos/achados não resolvidos e prontidão operacional revisados;
- rollback/recuperação e prontidão de observabilidade existentes para o formato de entrega implementado;
- decisão final registrada por pessoa autorizada com justificativa.

A partir da maturidade do Motor de Qualidade, a automação produz recomendação APPROVE, REVIEW ou BLOCK. Antes disso, recomendação é NOT_APPLICABLE; gates críticos aplicáveis ainda bloqueiam e uma pessoa autorizada ainda registra a decisão final. A automação nunca faz deploy nem registra aprovação humana por implicação.

## Seleção de gates baseada na alteração

Todo PR recebe gates baseline. Suítes adicionais são selecionadas pelos módulos, contratos, migrações de dados, tags de risco e defeitos históricos afetados. Exemplos:

| Alteração | Foco adicional obrigatório |
| --- | --- |
| Permissão/autenticação | revisão de segurança, matriz de permissão/negação, testes de sessão/auditoria |
| Dinheiro/estoque/SKU de produto | limites/propriedade, API, persistência/concorrência e qualidade de dados |
| Category ou associação Product/Category | normalização/unicidade canônica, lifecycle, conflitos de uso e corrida de associação versus desativação em PostgreSQL real |
| Schema de API/evento | compatibilidade e contrato de consumidor/provedor |
| Retry/outbox/worker | integração com banco/broker, idempotência, reinício e resiliência |
| Upload/processamento | segurança de arquivo hostil, integração com armazenamento, verificações de recursos/resiliência |
| Fórmula de pontuação/gate | política determinística, evidência ausente/desatualizada, prevalência de bloqueio crítico, auditoria |
| Consulta sensível a performance | revisão do plano/padrão de acesso e cenário de performance aprovado |
| Telemetria/redação | asserções de observabilidade e testes de vazamento de dados sensíveis |

A seleção baseada na alteração não pode omitir uma suíte exigida pela política de release; ela controla feedback antecipado e regressão direcionada.

## Conceito inicial da Pontuação de Qualidade

### Objetivo e limites

A pontuação resume várias dimensões de qualidade para comparação e discussão. Não é probabilidade de ausência de defeitos, avaliação de performance de pessoas nem substituta dos detalhes dos gates.

Formato de fórmula proposto para o futuro ADR-008, ainda sem versão de política:

~~~text
Pontuação de Qualidade = soma(pontuação da dimensão x peso da dimensão) - penalidades explícitas limitadas
~~~

Cada dimensão é normalizada para 0..100. Pesos candidatos iniciais:

| Dimensão | Peso | Entradas candidatas |
| --- | ---: | --- |
| Confiança funcional | 25% | resultados exigidos de teste ponderados por risco, resultados de jornadas críticas |
| Confiança de segurança | 20% | achados triados, resultados de verificações de autorização/segurança |
| Confiabilidade e resiliência | 15% | integração, retry/idempotência e experimentos de falha aprovados |
| Performance | 15% | resultados de limites, regressão, taxa de erro e recuperação |
| Confiança de requisitos/rastreabilidade | 10% | cobertura exigida, links ausentes, atribuição/atualização de evidências |
| Qualidade de dados | 10% | constraints, migrações, reconciliação e validade de ingestão |
| Acessibilidade | 5% | resultados automatizados e manuais dos fluxos críticos quando a UI existir |

A fórmula deve definir:

- ponderação por risco em vez de domínio da taxa de aprovação bruta;
- tratamento de skipped, blocked, error, retry e quarentena;
- dimensões exigidas versus opcionais/não aplicáveis por maturidade da release;
- janela de atualização e comportamento para evidência ausente;
- limites de penalidade e prevenção de contagem duplicada do mesmo problema;
- precisão/arredondamento;
- validade mínima de amostra/perfil para performance;
- snapshot imutável de entradas e versão da fórmula;
- limites/clamping da saída e significado/limite de qualquer penalidade explícita;
- linhagem das evidências para que o mesmo resultado/achado não seja recompensado ou penalizado em duplicidade entre dimensões;
- tratamento de dimensões não aplicáveis sem reponderação silenciosa que infle o resultado;
- limite de bloqueio versionado após calibração empírica, sem valor numérico fixado por esta fundação.

Antes da v0.7, a pontuação é NOT_APPLICABLE; exemplos documentais podem discutir fórmula experimental, mas a UI do produto não deve apresentar pontuação sintética como evidência de release. Ela só se torna entrada da política depois da validação com dados de cenários representativos e revisão de resultados enganosos.

## Regras da taxa de aprovação

A taxa bruta de aprovação é exibida, mas não usada isoladamente:

~~~text
taxa de aprovação = resultados aprovados / resultados executados elegíveis conforme a política
~~~

O denominador e o tratamento de skipped/blocked/error são divulgados. Retries preservam o status da primeira tentativa; aprovação eventual não é mesclada silenciosamente como aprovação limpa. Falhas críticas ponderadas por risco têm maior importância de decisão do que muitas aprovações de baixo risco.

## Classificação de Risco

O risco é classificado depois da avaliação dos gates críticos e da validade das evidências:

| Nível | Significado | Recomendação típica |
| --- | --- | --- |
| LOW | Evidência exigida está atual, sem bloqueios críticos, pontuação/sinais saudáveis e riscos residuais aceitos | APPROVE |
| MEDIUM | Sem bloqueio crítico, mas uma regressão material não crítica, exceção, lacuna limitada ou incerteza exige revisão explícita | REVIEW |
| HIGH | Falhas/achados significativos não resolvidos, incerteza ampla, recuperação fraca ou pontuação ruim indicam impacto material provável | BLOCK ou revisão sênior excepcional se a política permitir |
| CRITICAL | Ao menos um bloqueio crítico não substituível ou risco grave demonstrado de integridade/segurança | BLOCK |
| UNKNOWN | Evidência exigida está ausente, desatualizada, com erro ou não atribuível | BLOCK até a política de evidências ser atendida |

A política de classificação usa elevação pelo risco máximo: uma dimensão forte não reduz condição crítica em outra. O resultado inclui motivos e versão da política.

Quando o Motor de Qualidade se aplicar, sua versão de política pode definir:

~~~text
IF quality_score < versioned_block_threshold
  THEN recommendation = BLOCK
~~~

Nenhum limite numérico é aceito durante a fundação. Ele deve ser calibrado e versionado. Atingi-lo ou superá-lo nunca altera um gate com falha.

## Regras de bloqueio crítico

Regras obrigatórias iniciais:

~~~text
IF confirmed_critical_security_findings > 0
  THEN BLOCK RELEASE

IF critical_test_failures > 0
  THEN BLOCK RELEASE

IF authentication_or_authorization_bypass = true
  THEN BLOCK RELEASE

IF exposed_valid_secret = true
  THEN BLOCK RELEASE

IF data_integrity_loss_or_corruption = true
  THEN BLOCK RELEASE

IF required_evidence_is_missing_or_stale = true
  THEN BLOCK RELEASE AS UNKNOWN/INSUFFICIENT_EVIDENCE

IF performance_error_rate > versioned_critical_threshold
  THEN BLOCK RELEASE

IF quality_policy_or_evidence_integrity_is_compromised = true
  THEN BLOCK RELEASE

IF fault_lab_production_exposure = true
  THEN BLOCK RELEASE

IF external_exposure_requested = true
  AND real_authentication_rbac_gate != PASS
  THEN BLOCK EXTERNAL EXPOSURE

IF quality_engine_is_applicable = true
  AND quality_score < versioned_block_threshold
  THEN recommendation = BLOCK
~~~

A regra de pontuação baixa afeta a política versionada de recomendação; não substitui nem cria exceção para qualquer regra anterior de bloqueio crítico.

Regras adicionais por classe de release podem ser adicionadas por política versionada. Uma regra não pode ser removida apenas para fazer o candidato atual passar.

## Exceções

Algumas falhas de gates não críticos podem ser excepcionalmente aceitas se a política permitir. Uma exceção deve incluir:

- release, gate, requisito/componente e evidência afetados;
- motivo de negócio e por que a correção não pode preceder a release;
- severidade, probabilidade, exposição e impacto ao cliente/operação;
- controles compensatórios e monitoramento;
- responsável e aprovador(es) autorizado(s);
- expiração/meta de correção e plano de verificação;
- registro de auditoria e visibilidade no resumo da release.

Bloqueios críticos de segurança/autenticação/integridade são inicialmente não substituíveis. Qualquer proposta de torná-los substituíveis exige ADR de segurança/arquitetura e aprovação humana explícita; não pode ocorrer de modo ad hoc nos dados da release.

Exceções expiradas tornam-se automaticamente risco ativo e não podem aprovar releases posteriores. Reutilizar uma exceção exige nova decisão atribuível.

Marcar capacidade ou gate como NOT_APPLICABLE segue a mesma governança mínima: ator autorizado, justificativa explícita, candidato/build exato, versão da política e registro de auditoria. Não pode ser usado para dispensar gate obrigatório na política.

## Falhas instáveis e de infraestrutura

- Um teste instável não está aprovado; resultados da primeira tentativa e de retry permanecem visíveis.
- Quarentena é rastreada, tem responsável e expira conforme [TEST_STRATEGY.md](TEST_STRATEGY.md#política-de-testes-instáveis).
- Falha de infraestrutura/ferramenta produz ERROR; retry pode estabelecer validade do ambiente, mas não apagar a evidência de instabilidade.
- Se a evidência obrigatória não puder ser gerada até o corte, a release fica UNKNOWN/bloqueada em vez de aprovada com otimismo.
- Instabilidade repetida de infraestrutura é risco de entrega do produto e contribui para a Classificação de Risco.

## Atualização e proveniência das evidências

Toda entrada de gate deve identificar:

- fonte e identidade autenticada de ingestão;
- commit/build/artefato e release exatos;
- ambiente/perfil e versão da ferramenta/schema;
- momento da execução/medição e corte;
- resultado/checksum de evidência ou referência imutável quando apropriado;
- estado de triagem dos achados;
- atualização conforme a política.

Um build mais novo invalida evidências do escopo alterado, salvo se a política permitir explicitamente a reutilização e explicar o motivo. Evidência manual é atribuível e expira como evidência automatizada.

## Governança dos gates

- Gates e fórmulas são configuration-as-code ou equivalentemente controlados por versão quando implementados.
- Alterações recebem revisão de produto, QA, segurança e engenharia proporcional ao impacto.
- Avaliações históricas permanecem vinculadas à versão antiga.
- O dashboard expõe entradas, motivos, exceções e dados ausentes.
- Alterações de gate são auditadas e não podem reescrever retroativamente uma decisão de release.
- Decisões de aplicabilidade e substituições de política exigem autorização dedicada e referência imutável de auditoria.
- Métricas são testadas periodicamente contra incentivos perversos e falsa confiança.

## Exemplo inicial de resumo de release

~~~text
Release: v1.0.0
Candidato: rc.2
Build: <identidade imutável do build>
Corte de evidência: <timestamp UTC>

Funcional: PASS
API: PASS
Integração: PASS
E2E: PASS
Segurança: PASS
Performance: PASS

Pontuação de Qualidade: 94/100 (fórmula 1.0)
Risco: LOW (política 1.0)
Bloqueios Críticos: nenhum
Recomendação: APPROVE
Decisão Final: APPROVED por <ator autorizado> em <momento>
~~~

O resumo contém links para gates/evidências individuais e mostra desatualização ou exceções. Apenas o texto da apresentação não é a evidência.

## Calibração e questões em aberto

- Workload/dataset/ambiente de referência e limite crítico da taxa de erro de performance.
- Normalização e pesos da pontuação validados contra releases representativas.
- Quais requisitos e suítes são críticos para cada classe de release.
- Janelas de atualização de evidências por fonte.
- Evidência manual mínima de acessibilidade/exploração para releases com UI.
- Roles aprovadoras de exceção e se algum bloqueio de alto risco não relacionado à segurança é não substituível.
- Definição de métricas de cobertura resistentes à inflação superficial de testes/links.
- Caminho de alerta/escalação quando uma release anteriormente aprovada receber depois um achado crítico.
