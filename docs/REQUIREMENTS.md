# Requisitos do AEGIS

## Finalidade e convenções

Este documento é a baseline inicial de requisitos do produto. Ele descreve capacidades pretendidas; a atribuição de uma versão no roadmap não significa que a capacidade já foi implementada.

Os IDs de requisitos são estáveis:

- REQ-<DOMAIN>-NNN: requisito funcional;
- NFR-<QUALITY>-NNN: requisito não funcional;
- BR-<DOMAIN>-NNN: regra de negócio.

A prioridade usa **Must**, **Conditional Must**, **Should** e **Could**. Um Conditional Must torna-se obrigatório quando sua capacidade/maturidade nomeada se aplica e permanece explicitamente NOT_APPLICABLE antes disso. Os critérios de aceite usam uma linguagem concisa no formato Dado/Quando/Então e devem ser refinados com exemplos antes da implementação. Um requisito não está concluído apenas porque um teste automatizado de caminho feliz passou.

## Requisitos funcionais

### Identidade e acesso (AUTH)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-AUTH-001 | Must | O sistema deve autenticar um usuário ativo com credenciais suportadas. | Dadas credenciais válidas de um usuário ativo, quando a autenticação for bem-sucedida, então uma sessão/token com prazo limitado será emitida sem expor secrets; credenciais inválidas retornarão uma resposta genérica de não autorizado. |
| REQ-AUTH-002 | Must | O sistema deve encerrar ou invalidar uma sessão autenticada. | Dado um usuário autenticado, quando logout ou revogação forem concluídos, então a sessão afetada não poderá acessar endpoints protegidos. |
| REQ-AUTH-003 | Must | Capacidades protegidas devem exigir autenticação. | Dada uma sessão anônima ou expirada, quando uma operação protegida for tentada, então ela será rejeitada como não autenticada e nenhum estado será alterado. |
| REQ-AUTH-004 | Must | A autorização deve ser baseada em roles e permissões. | Dados usuários com permissões diferentes, quando invocarem a mesma operação, então somente os atores autorizados terão sucesso e as negações serão auditáveis. |
| REQ-AUTH-005 | Must | Administradores autorizados devem criar/desativar usuários e adicionar/remover atribuições de roles. | Toda alteração valida ator/alvo, protege o último administrador ativo, revoga as sessões afetadas após uma mudança crítica de permissão e emite o evento de auditoria fail-closed exigido. |
| REQ-AUTH-006 | Should | Eventos de sessão sensíveis para segurança devem ser visíveis ao usuário afetado ou a um administrador. | Indicadores de login bem-sucedido e malsucedido contêm momento e contexto seguro do cliente sem registrar credenciais ou tokens brutos. |

#### Matriz conceitual mínima de roles

Esta é uma baseline de planejamento, não uma política de autorização implementada ou exaustiva. As permissões exatas serão revisadas antes da Fase 03.

| Role | Responsabilidades pretendidas | Permissões conceituais mínimas |
| --- | --- | --- |
| ADMIN | Criar/desativar usuários, atribuir/remover roles, inspecionar auditoria e administrar a plataforma | admin:users, admin:roles, audit:read; permissões de negócio são concedidas explicitamente, não implicitamente |
| QUALITY_MANAGER | Gerenciar releases/evidências, avaliar gates e registrar decisões autorizadas de release | quality:read, quality:write, release:decide; quality:policy:admin é separada e deve ser concedida explicitamente |
| OPERATOR | Operar fluxos de catálogo e mídia | catalog:read, catalog:write, catalog:history:read, media:read, media:write |
| VIEWER | Ler resumos autorizados de Commerce e qualidade sem mutação | catalog:read, media:read, quality:read |

quality:policy:admin controla alterações em fórmulas de pontuação, políticas de risco e definições de gates. Nunca é implícita em quality:write genérica. Identidades de CI/ingestão de evidência são identidades de serviço com escopo, não roles humanas, e não recebem permissão para decisão de release ou administração de políticas.

### Catálogo (CAT)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-CAT-001 | Must | Usuários autorizados devem criar um produto com SKU, nome, descrição, preço, estoque e atribuição de categoria. | Entrada válida cria um produto identificável; campos inválidos geram erros no nível do campo e nenhum produto parcial. |
| REQ-CAT-002 | Must | Usuários autorizados devem recuperar um produto por seu identificador estável. | Um produto visível existente retorna sua representação atual; um identificador desconhecido retorna não encontrado sem vazar dados restritos. |
| REQ-CAT-003 | Must | Usuários autorizados devem atualizar atributos permitidos do produto com proteção de concorrência. | Uma versão atual é atualizada atomicamente e registra histórico; uma versão obsoleta retorna conflito sem sobrescrever dados mais novos. |
| REQ-CAT-004 | Must | Usuários autorizados devem desativar um produto sem apagar o histórico necessário. | A desativação remove o produto dos resultados ativos padrão, preserva o histórico e emite eventos de auditoria/integração. |
| REQ-CAT-005 | Must | Usuários autorizados devem gerenciar categorias e seu estado ativo. | Alterações válidas de categoria são persistidas; uma categoria em uso proibitivo não pode ser removida ou desativada sem um resultado explícito em conformidade com a política. |
| REQ-CAT-006 | Must | O SKU de produto deve ser normalizado e único. | SKUs normalizados equivalentes não podem coexistir; uma duplicidade retorna conflito e não altera dados. |
| REQ-CAT-007 | Must | Preço e estoque do produto devem seguir regras numéricas definidas. | Preço ou estoque negativo é rejeitado; precisão decimal e moeda são determinísticas; valores válidos de limite persistem exatamente. |
| REQ-CAT-008 | Must | Usuários devem pesquisar e filtrar o catálogo. | Combinações suportadas de consulta, categoria e estado ativo retornam apenas registros autorizados correspondentes, com os critérios aplicados representados na resposta. |
| REQ-CAT-009 | Must | Coleções de produtos devem ser paginadas e ordenadas deterministicamente. | O tamanho da página é limitado, parâmetros inválidos são rejeitados e solicitações repetidas sobre dados inalterados retornam ordenação estável. |
| REQ-CAT-010 | Must | Alterações materiais do produto devem criar um registro de histórico. | Toda criação/atualização/desativação bem-sucedida registra ator, momento, produto, ação e uma representação segura da alteração antes/depois. |
| REQ-CAT-011 | Should | Usuários devem visualizar o histórico do produto se autorizados. | Os resultados são cronológicos, paginados e não expõem valores sensíveis para segurança que tenham sido redigidos. |
| REQ-CAT-012 | Must | Escritas do catálogo devem publicar um evento de domínio interno após commit bem-sucedido. | Toda alteração material confirmada produz um evento logicamente identificável para processamento downstream; alterações revertidas não publicam evento confirmado. |

### Mídia (MED)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-MED-001 | Must | Usuários autorizados devem fazer upload de imagens de produto suportadas. | Tipo, assinatura, tamanho e autorização são validados antes da aceitação durável; arquivos rejeitados não se tornam recuperáveis publicamente. |
| REQ-MED-002 | Must | Mídias recebidas devem ter um ciclo de processamento explícito. | Um item de mídia transita por estados documentados como pendente, processando, pronto ou falhou, com códigos de motivo para falha. |
| REQ-MED-003 | Must | O processamento de imagens deve criar somente variantes e metadados aprovados. | O processamento bem-sucedido registra dimensões, tipo, checksum e referências de armazenamento; conteúdo binário não entra em tabelas transacionais. |
| REQ-MED-004 | Must | A recuperação de mídia deve aplicar visibilidade do produto e entrega segura do conteúdo. | Acesso não autorizado é rejeitado; respostas usam content-types seguros e não expõem credenciais nem caminhos internos de armazenamento. |
| REQ-MED-005 | Must | Usuários devem associar, ordenar e remover referências de imagens do produto. | Alterações de associação são atômicas, preservam necessidades de auditoria/histórico e não deixam objetos públicos órfãos não pretendidos. |
| REQ-MED-006 | Should | Falhas no processamento de mídia devem permitir retry limitado ou reprocessamento autorizado. | O retry é idempotente, o número de tentativas é visível e a falha terminal não bloqueia leituras não relacionadas do produto. |

### Integração externa (INT)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-INT-001 | Must | Alterações materiais do catálogo devem ser sincronizadas de forma assíncrona com o Mock do Centro de Vendas Externo. | Uma alteração elegível confirmada cria uma tarefa durável de integração e a transação de catálogo visível ao usuário não depende da disponibilidade downstream. |
| REQ-INT-002 | Must | Mensagens de saída devem conter ID de evento único, versão de schema, momento de ocorrência e contexto de correlação. | Consumidores conseguem distinguir identidade/versão do evento e rastreá-lo até a operação originadora. |
| REQ-INT-003 | Must | Consumidores devem processar entregas duplicadas de forma idempotente. | Uma nova entrega do mesmo evento não duplica o efeito de negócio externo e produz um resultado de duplicidade observável. |
| REQ-INT-004 | Must | Falhas transitórias devem usar retry limitado com backoff. | Falhas elegíveis repetem conforme a política; tentativas e próximo retry são observáveis; falhas permanentes não repetem indefinidamente. |
| REQ-INT-005 | Must | Mensagens esgotadas ou não repetíveis devem entrar em um estado de falha recuperável. | A mensagem e o contexto sanitizado da falha são retidos para inspeção e replay autorizados após a correção. |
| REQ-INT-006 | Must | A publicação de eventos não deve perder alterações confirmadas do catálogo. | O Catálogo armazena atomicamente estado do produto, histórico do produto e sua intenção de outbox; a Integração reivindica/publica essa intenção somente pela porta de publicação do Catálogo, com reconciliação de registros travados. |
| REQ-INT-007 | Should | Operadores autorizados devem inspecionar o status de sincronização por produto/evento. | O status expõe estados pendente, processando, entregue ou falhou, tentativas e timestamps seguros, sem secrets. |
| REQ-INT-008 | Should | Operadores autorizados devem repetir trabalho de integração elegível que falhou. | O replay exige motivo/ator, preserva a relação de identidade original e não pode contornar validação ou idempotência. |

### Auditoria (AUD)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-AUD-001 | Must | Ações relevantes para segurança e negócio devem criar eventos de auditoria imutáveis. | Ator, ação, alvo, resultado, momento e ID de correlação são registrados; roles normais da aplicação não podem editar um evento de auditoria. |
| REQ-AUD-002 | Must | Revisores autorizados devem consultar eventos de auditoria usando filtros limitados e paginação. | Os resultados respeitam privilégio mínimo, ordenação determinística e política de retenção/redação. |
| REQ-AUD-003 | Must | Falhas de auditoria devem ser visíveis e devem operar em fail-closed para ações críticas designadas. | Uma ação crítica sem aceitação durável de auditoria disponível é rejeitada; projeções não críticas repetem e alertam. Contenções emergenciais, como parar o Laboratório de Falhas, prosseguem mesmo com auditoria degradada e geram alerta crítico de auditoria. |

### Centro de Controle de Qualidade (QLT)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-QLT-001 | Must | Usuários autorizados devem criar uma release e associar candidatos/identidades de build imutáveis. | A versão da release é única; cada candidato referencia uma identidade imutável de build e corte de evidência, e uma decisão final identifica o candidato exato sem sobrescrever candidatos anteriores. |
| REQ-QLT-002 | Must | O sistema deve registrar suítes e casos de teste com referências externas estáveis. | Casos incluem camada, responsável, status, estado de automação e links de requisitos; IDs externos duplicados são rejeitados. |
| REQ-QLT-003 | Must | O sistema deve ingerir execuções de teste e resultados individuais de fontes aprovadas. | Payloads válidos e autenticados são aceitos de forma idempotente; payloads malformados ou com schema desconhecido são rejeitados com erros acionáveis. |
| REQ-QLT-004 | Must | Resultados de testes devem preservar contexto de execução e referências de evidência. | Status do resultado, duração, ambiente, identidade de build/commit e metadados da evidência são retidos sem armazenar secrets. |
| REQ-QLT-005 | Must | Requisitos devem ser rastreáveis a casos, resultados, evidências, defeitos e releases. | Um revisor consegue navegar por todos os links disponíveis nas duas direções e identificar links ausentes. |
| REQ-QLT-006 | Must | O sistema deve registrar defeitos e associá-los aos requisitos, resultados e releases afetados. | Severidade, prioridade, status, responsável/referência e histórico são capturados; o fechamento não apaga associações anteriores. |
| REQ-QLT-007 | Must | O sistema deve ingerir achados de segurança normalizados. | Identidade da fonte, regra, severidade, componente afetado, estado e referência de evidência são retidos; duplicidades seguem uma política de fingerprint documentada. |
| REQ-QLT-008 | Must | O sistema deve ingerir resultados de performance e limites normalizados. | Cenário, workload, latência de percentil, throughput, taxa de erro e resultado do limite são atribuíveis a um build/release. |
| REQ-QLT-009 | Conditional Must | O Motor de Qualidade deve calcular uma Pontuação de Qualidade explicável a partir de entradas e pesos versionados. | Toda pontuação aplicável expõe versão da fórmula, métricas contribuintes, tratamento de dados ausentes e momento do cálculo; antes da maturidade do Motor de Qualidade, fica explicitamente NOT_APPLICABLE. |
| REQ-QLT-010 | Conditional Must | O Motor de Qualidade deve classificar o risco do candidato usando política versionada. | O nível aplicável e as condições contribuintes são exibidos; entradas/política idênticas produzem o mesmo resultado; antes da maturidade do Motor de Qualidade, fica explicitamente NOT_APPLICABLE. |
| REQ-QLT-011 | Must | O sistema deve avaliar Gates de Qualidade versionados independentemente da pontuação numérica. | Todo gate exigido tem resultado e motivos atribuíveis; qualquer regra ativa de bloqueio crítico produz resultado bloqueante e impede aprovação; quando a recomendação se aplicar, força BLOCK mesmo se a pontuação calculada for alta. |
| REQ-QLT-012 | Must | O sistema deve apresentar um resumo de evidências de um candidato/build exato de release. | Usuários autorizados veem corte/atualização de evidência, status de testes, falhas, defeitos, achados de segurança, resultados de performance, métricas e evidência ausente sem exigir pontuação ou classificação de risco. |
| REQ-QLT-013 | Must | Uma pessoa autorizada deve registrar a decisão final de release e a justificativa. | Aprovação/bloqueio/exceção registra ator, momento, justificativa, snapshot de evidências e qualquer exceção com prazo; uma recomendação isolada nunca faz deploy. |
| REQ-QLT-014 | Should | Políticas e Gates de Qualidade devem ser versionados e suas alterações auditadas. | Uma avaliação de release referencia a versão exata da política; atualizações de política não reescrevem silenciosamente decisões históricas. |
| REQ-QLT-015 | Conditional Must | O Motor de Qualidade deve produzir uma recomendação de release explicável a partir dos resultados dos gates, pontuação/risco quando aplicáveis e política versionada. | A recomendação é APPROVE, REVIEW ou BLOCK, identifica motivos e versão da política, nunca sobrepõe bloqueios críticos e é NOT_APPLICABLE antes de o Motor de Qualidade existir. |

### Laboratório de Falhas (FLT)

| ID | Prioridade | Requisito | Critérios de aceite |
| --- | --- | --- | --- |
| REQ-FLT-001 | Must | Usuários autorizados devem ativar somente cenários de falha predefinidos em ambientes não produtivos permitidos. | Produção é negada por design; a ativação registra ator, cenário, escopo, expiração e marcador de correlação. |
| REQ-FLT-002 | Must | Os cenários iniciais devem cobrir indisponibilidade/timeout downstream, latência/HTTP 500 da API, latência do banco de dados, falha de imagem e interrupção de fila. | Todo cenário implementado tem controles determinísticos, limites de blast radius e estado de ativação observável. |
| REQ-FLT-003 | Must | Falhas devem expirar automaticamente e permitir parada de emergência. | Uma falha não pode persistir além do TTL máximo; a parada é idempotente e seu resultado é observável/auditado. |
| REQ-FLT-004 | Should | Experimentos de falha devem vincular hipótese, telemetria, execução de teste e conclusão. | Um experimento concluído identifica comportamento esperado, sinais observados e achados não resolvidos. |

## Requisitos não funcionais

As metas abaixo são objetivos iniciais de design. Limites exatos devem ter baseline e versão definidos antes de se tornarem gates de release.

| ID | Qualidade | Requisito / aceite mensurável |
| --- | --- | --- |
| NFR-PERF-001 | Performance | Para um dataset de referência e perfil local/CI acordados, endpoints de leitura do catálogo devem atender p95 <= 300 ms e endpoints de escrita p95 <= 500 ms, excluindo a conclusão assíncrona downstream. |
| NFR-PERF-002 | Performance | O tamanho padrão de coleções deve ser 20 e o máximo aplicado 100, salvo se um endpoint documentar valor mais restrito. |
| NFR-REL-001 | Confiabilidade | Trabalho de integração confirmado deve ser recuperável após reinício de processo; nenhuma mensagem confirmada pode ser descartada silenciosamente. |
| NFR-REL-002 | Confiabilidade | Políticas de retry devem ser limitadas, usar backoff com jitter quando apropriado e expor falha terminal. |
| NFR-SEC-001 | Segurança | Todas as operações protegidas devem aplicar autenticação e autorização no servidor; visibilidade na UI nunca é controle de autorização. |
| NFR-SEC-002 | Segurança | Secrets, credenciais brutas, tokens de sessão e dados pessoais sensíveis não devem aparecer em logs, traces, evidências ou respostas de erro. |
| NFR-SEC-003 | Segurança | Dependências e imagens devem ser verificadas sob política versionada; achados críticos exploráveis não resolvidos bloqueiam a release. |
| NFR-OBS-001 | Observabilidade | Solicitações recebidas, mensagens assíncronas e tentativas de integração devem propagar ou criar contexto de correlação e trace. |
| NFR-OBS-002 | Observabilidade | Fluxos críticos devem expor logs estruturados, métricas e traces suficientes para localizar o limite da falha sem permitir vazamento de dados sensíveis. |
| NFR-OBS-003 | Observabilidade | Uma execução de teste que dispare várias solicitações deve preservar um conjunto limitado de IDs de correlação e seus IDs de trace nas evidências, para que cada falha observada seja ligada ao build e à execução exatos. |
| NFR-TEST-001 | Testabilidade | Limites externos, tempo, retry e comportamento de falha devem ser controláveis por interfaces/fakes seguros em testes automatizados. |
| NFR-TEST-002 | Testabilidade | Todo requisito Must ou Conditional Must aplicável deve ter cobertura de verificação documentada antes da aprovação de sua release-alvo. |
| NFR-DATA-001 | Integridade de dados | Invariantes transacionais devem ser aplicados nas camadas apropriadas da aplicação e do banco; atualizações concorrentes não podem perder dados silenciosamente. |
| NFR-DATA-002 | Qualidade de dados | A ingestão de evidências deve validar schema, fonte, timestamps e identidade de release/build e deve tornar evidências ausentes ou desatualizadas visíveis. |
| NFR-ACC-001 | Acessibilidade | Interfaces de usuário devem buscar WCAG 2.2 AA nos fluxos críticos suportados, incluindo acesso por teclado, foco visível, nomes/roles e contraste. |
| NFR-COMP-001 | Compatibilidade | APIs e eventos devem usar versões explícitas e regras de compatibilidade documentadas; mudanças incompatíveis exigem planejamento de migração. |
| NFR-MAINT-001 | Manutenibilidade | Dependências entre módulos devem seguir [ARCHITECTURE.md](ARCHITECTURE.md); novo acoplamento entre módulos ou infraestrutura exige revisão e, quando significativo, um ADR. |
| NFR-PORT-001 | Portabilidade | Quando componentes executáveis existirem, um contribuidor deve conseguir iniciar dependências locais exigidas por meio de um fluxo documentado e reproduzível. |
| NFR-PRIV-001 | Privacidade | A coleta de dados pessoais e diagnósticos deve ser minimizada e governada por políticas documentadas de retenção e exclusão antes do uso em produção. |

## Regras de negócio

| ID | Regra |
| --- | --- |
| BR-AUTH-001 | Negar por padrão: a ausência de permissão explícita significa que a ação é proibida. |
| BR-AUTH-002 | Usuários desativados não podem iniciar novas sessões nem usar sessões revogadas. |
| BR-AUTH-003 | Antes que a Fase 03 forneça autenticação/RBAC reais, endpoints de mutação do catálogo são apenas uma prévia de desenvolvimento local, não devem ser expostos externamente e não podem satisfazer critérios de aceite de autorização. |
| BR-CAT-001 | A comparação de SKU usa uma regra de normalização documentada e é única entre produtos ativos e inativos, salvo se um ADR alterar a política de reutilização. |
| BR-CAT-002 | O preço é não negativo, usa moeda ISO 4217 explícita e semântica decimal fixa; aritmética de ponto flutuante é proibida para dinheiro persistido. |
| BR-CAT-003 | O estoque é um inteiro maior ou igual a zero; qualquer modelo futuro de reserva exige regras separadas. |
| BR-CAT-004 | A exclusão de produto é lógica no escopo inicial para que histórico, auditoria e evidências de release permaneçam referencialmente significativos. |
| BR-MED-001 | Extensão de arquivo ou MIME type informado pelo cliente nunca é validação suficiente isoladamente. |
| BR-INT-001 | Presume-se entrega at-least-once; consumidores devem ser idempotentes. Não se alega exactly once entre limites do sistema. |
| BR-INT-002 | Retry é permitido somente para falhas classificadas como transitórias; falhas de validação e autorização não se tornam bem-sucedidas por retry. |
| BR-QLT-001 | Uma Pontuação de Qualidade nunca cancela nem reduz uma regra ativa de bloqueio crítico. |
| BR-QLT-002 | Achados críticos de segurança, falhas críticas de teste e taxa de erro de performance acima do limite crítico aprovado bloqueiam a release. |
| BR-QLT-003 | Evidência obrigatória ausente não é aprovação; a política determina se gera evidência insuficiente ou bloqueio. |
| BR-QLT-004 | Somente uma pessoa autorizada registra a decisão final de release; a automação produz uma recomendação. |
| BR-QLT-005 | Exceções são explícitas, justificadas, com prazo, atribuíveis e não podem alterar silenciosamente evidências históricas. |
| BR-QLT-006 | Quando uma versão calibrada da política define um limite de bloqueio, Pontuação de Qualidade abaixo dele produz BLOCK; nenhum limite numérico é fixado durante a fundação e uma pontuação alta nunca cancela um bloqueio crítico. |
| BR-FLT-001 | A injeção de falhas é desabilitada em produção e fica desligada por padrão em todos os ambientes. |
| BR-TEST-001 | Um teste com falha é investigado; nunca é excluído, ignorado ou enfraquecido somente para obter um pipeline verde. |
| BR-AUD-001 | Aceitação durável de auditoria opera em fail-closed para alterações privilegiadas de usuário/role, alterações em políticas/gates de qualidade, decisões finais de release/exceções, replay de mensagens terminais e ativação/alteração do Laboratório de Falhas. Parada de emergência e outras ações de contenção devem prosseguir durante degradação da auditoria e gerar alerta crítico. |

## Critérios de aceite entre domínios

Uma capacidade é aceitável somente quando todas as condições aplicáveis forem atendidas:

1. O comportamento e os limites pretendidos são rastreáveis a um ou mais requisitos estáveis.
2. Autenticação, autorização, validação de entrada e casos de abuso foram considerados.
3. Alterações de estado são atômicas ou expõem um estado intermediário deliberado e recuperável.
4. Ações relevantes de negócio e segurança são auditáveis.
5. Falhas usam contratos de erro estáveis e seguros e não expõem secrets ou detalhes internos.
6. Existem testes nas camadas eficazes mais baixas, além de cobertura de cenário quando a confiança entre limites é necessária.
7. Logs, métricas, traces e contexto de correlação exigidos apoiam o diagnóstico.
8. Compatibilidade e efeitos de migração de APIs/eventos/dados foram avaliados.
9. Documentação e links de rastreabilidade refletem o comportamento implementado.
10. Gates de Qualidade aplicáveis passam ou uma exceção autorizada é registrada conforme a política. NOT_APPLICABLE exige motivo, ator autorizado, versão exata da política e registro de auditoria.

## Questões de requisitos em aberto

- Qual mecanismo de identidade e modelo de token/sessão melhor atendem à demonstração local e ao futuro deploy?
- Um SKU pode ser reutilizado após a desativação do produto e quais seriam as consequências downstream?
- Quais moedas e regras de localidade pertencem à v1.0?
- Que duração de armazenamento de evidências e retenção de dados pessoais são necessárias?
- Qual dataset de referência e perfil de hardware tornarão os limites de performance reproduzíveis?
- Quais sistemas de origem são autoridades para defeitos e requisitos na versão de portfólio?
- Quais roles podem aprovar uma exceção de release e quais bloqueios críticos nunca podem ser substituídos?
