# Estratégia de Observabilidade do AEGIS

## Finalidade

Observabilidade permite que profissionais de engenharia expliquem o comportamento do sistema a partir dos sinais emitidos. Para o AEGIS, também é uma capacidade de Engenharia de Qualidade: testes e evidências de release devem ajudar uma investigação a atravessar limites de HTTP, banco de dados, mensagem, worker e downstream sem adivinhação.

Este documento define a baseline atual e requisitos futuros de instrumentação. A Fase 01 implementa logs JSON, correlação HTTP/MDC, liveness, readiness e metadados seguros de build. OpenTelemetry, Prometheus e Grafana permanecem não implementados e serão introduzidos incrementalmente apenas quando autorizados e úteis.

## Princípios

- Instrumentar fluxos críticos e limites de falha durante o design, não depois de incidentes.
- Preferir sinais estruturados, consistentes e consultáveis a logs excessivamente narrativos.
- Correlacionar logs, métricas, traces, registros de auditoria, evidências de teste e releases.
- Registrar resultados e contexto seguro, nunca secrets ou payloads arbitrários.
- Controlar cardinalidade, volume, retenção e custo.
- Tornar estados desconhecidos, desatualizados e falhas de telemetria explícitos.
- Alertas representam risco acionável ao usuário/sistema, não toda linha de erro.
- Telemetria valida hipóteses, mas não é a única fonte da verdade do produto.
- Falhas de instrumentação não devem quebrar silenciosamente a lógica de negócio; auditoria de segurança obrigatória segue sua política mais rigorosa.

## Modelo de sinais

~~~mermaid
flowchart LR
    Request["Solicitação / teste / evento"] --> Context["Contexto de correlação e trace"]
    Context --> Logs["Logs estruturados"]
    Context --> Metrics["Métricas"]
    Context --> Traces["Traces distribuídos"]
    Context --> Audit["Eventos de auditoria"]
    Context --> Evidence["Evidências de teste/release"]
    Logs --> Investigate["Investigação de falha"]
    Metrics --> Investigate
    Traces --> Investigate
    Audit --> Investigate
    Evidence --> Investigate
~~~

## Identificadores de correlação e trace

### ID de correlação

Um ID de correlação representa um fluxo lógico de negócio/diagnóstico e pode sobreviver além de um único trace síncrono. Regras:

- aceitar X-Correlation-ID somente se corresponder às restrições de tamanho/caracteres; caso contrário, gerar novo ID opaco;
- retornar o ID efetivo nas respostas da API, incluindo respostas de erro seguras;
- propagá-lo por chamadas entre módulos, eventos de outbox, mensagens do broker, solicitações de integração, auditoria e evidências de qualidade;
- preservar vínculos de causalidade quando um evento causar outro;
- não codificar usuário, ambiente ou secret no ID;
- registrá-lo como campo estruturado, não apenas no texto da mensagem.

Na Fase 01, exatamente um valor que corresponda a `^[A-Za-z0-9._-]{1,128}$` é preservado sem `trim`; ausência, invalidade ou multiplicidade gera um UUID v4. Para requisições aceitas pelo conector e encaminhadas à cadeia Servlet/Spring, o valor efetivo é devolvido no header, mantido no MDC durante o dispatch e incluído em Problem Details. O contexto é restaurado ou removido em `finally`. Requisições malformadas rejeitadas antes do filtro ficam fora dessa garantia. Propagação assíncrona não existe nesta fase.

### IDs de trace e span

O contexto de trace do OpenTelemetry representa um caminho causal de execução. Contexto recebido, compatível e confiável, pode continuar após validação; caso contrário, novo contexto é criado. Publicação e consumo assíncronos usam semântica de span/link de mensageria para que atrasos e retries sejam visíveis. IDs de trace são retornados ou vinculados a evidências diagnósticas quando seguro.

ID de correlação e ID de trace são relacionados, mas não intercambiáveis: retries/replays podem compartilhar uma correlação de negócio e produzir novos traces.

### Protocolo de vínculo da execução de teste

Um único resultado de teste pode disparar zero, um ou vários fluxos HTTP/mensagens. A evidência, portanto, armazena uma coleção limitada de links de solicitações em vez de um identificador ambíguo:

1. O runner identifica execução/resultado exato do teste, candidato da release e build imutável sob teste.
2. Para cada solicitação relevante, pode enviar X-Correlation-ID válido ou capturar o valor gerado pelo servidor e retornado na resposta.
3. O runner registra um link ordenado da solicitação com sequência, ID efetivo de correlação, rota/operação segura, momento e resultado.
4. Quando o acesso a traces é autorizado, o link acrescenta o(s) ID(s) de trace associado(s); uma correlação pode mapear vários traces após retry/replay.
5. A evidência do resultado armazena a coleção limitada de links ou referência/checksum de artefato imutável quando a coleção exceder o limite do schema de ingestão.
6. A ingestão no Centro de Controle de Qualidade valida candidato/build, fonte efetiva e schema antes de aceitar esses links.

IDs de correlação e trace são ponteiros diagnósticos, não prova de que uma asserção passou. Nunca são labels do Prometheus. URLs brutas, headers, corpos e credenciais são excluídos dos links de solicitações. O limite exato da coleção será definido com o schema de ingestão de evidências antes da Fase 06.

## Logs estruturados

O runtime da Fase 01 usa o formato JSON estruturado nativo do Spring Boot em todos os ambientes para manter o contrato observável simples. A evolução poderá adotar uma saída local alternativa somente se preservar campos e testes. Campos comuns:

- timestamp UTC, severidade e nome/código estável do evento;
- serviço/processo, módulo, ambiente e versão/build;
- ID de correlação, ID de trace e ID de span;
- template de rota e método/status HTTP, não URL bruta sensível;
- ID do ator/serviço quando a política permitir, nunca credenciais;
- ID de entidade/evento/candidato da release/execução de teste quando relevante ao diagnóstico;
- resultado, duração e classificação segura do erro;
- tentativa de retry, dependência/operação e schema/ID do evento para trabalho assíncrono.

Níveis de log:

- DEBUG: detalhe diagnóstico local/direcionado, desabilitado ou amostrado em operação compartilhada normal;
- INFO: ciclo de vida e resultado material bem-sucedido, evitando ruído por item;
- WARN: degradação recuperada, retry, entrada suspeita rejeitada ou limite próximo;
- ERROR: operação com falha que exige investigação ou falha terminal de processamento.

Nomes estáveis de evento são preferíveis à análise de texto, por exemplo catalog.product.updated, integration.delivery.retry_scheduled e quality.gate.failed.

O evento implementado `http.request.completed` inclui método, template de rota resolvido quando disponível — ou fallback de path limitado e sanitizado —, status, duração em milissegundos e `correlationId` pelo MDC. Query string, matrix parameters e caracteres de controle são removidos pela mesma política usada em Problem Details. O startup info que revelaria usuário e diretório local está desabilitado. Erros inesperados registram classificação, tipo e primeiro frame estrutural pertencente ao AEGIS quando disponível, sem mensagem, causa, payload ou stack trace bruto. Os testes verificam que path local, query string, matrix parameters, valor de correlação rejeitado e secrets sintéticos não aparecem nos logs capturados, e que origens distintas permanecem diagnosticáveis.

Nunca registrar senhas, tokens/cookies, headers de autorização, URLs assinadas, secrets, corpos completos de solicitação/resposta, conteúdo binário recebido, parâmetros SQL brutos com dados ou dados pessoais desnecessários. A redação é baseada em allowlist e testada. Caracteres de controle são sanitizados para impedir injeção em log.

## Métricas

Métricas usam labels estáveis e de baixa cardinalidade. Dimensões de produto, dependência e qualidade incluem:

### HTTP/aplicação

- contagem de solicitações, histograma de duração e contagem de erros por template de rota/método/classe de status;
- solicitações ativas e solicitações rejeitadas/limitadas;
- contagens de negação de validação, autenticação e autorização, apenas com labels seguros;
- saturação de JVM/processo/runtime do backend.

### Catálogo/dados

- resultados de comandos do produto e conflitos de concorrência;
- duração de operações/transações do banco, utilização do pool e timeouts;
- falhas de append de histórico/auditoria;
- discrepâncias de reconciliação, como dados travados/órfãos, sem IDs brutos de entidade como labels.

### Mídia

- uploads aceitos/rejeitados por classe segura de motivo;
- duração do processamento e contagens de sucesso/falha/retry;
- idade e quantidade de itens pendentes/com falha;
- erros de dependência do object storage e backlog de limpeza.

### Mensageria/integração

- quantidade não publicada na outbox e idade do item mais antigo;
- resultado/duração da publicação;
- profundidade da fila/lag do consumidor e idade da mensagem mais antiga quando disponíveis;
- contagens de sucesso/retry/falha terminal/duplicidade na entrega;
- latência/erro/timeout downstream por operação, sem URL completa/labels de cliente;
- tentativas de retry e backlog de dead-letter/falhas recuperáveis.

### Centro de Controle de Qualidade

- contagens de ingestão aceita/rejeitada/duplicada por fonte/tipo aprovado;
- atraso da ingestão e atualização das evidências;
- contagens de resultados por camada/status e distinção da primeira tentativa/retry;
- resultados de gates, contagem de bloqueios críticos e erros de avaliação;
- distribuição de pontuação/risco por classe de release/versão da fórmula, evitando IDs de release como labels;
- contagem de defeitos/achados abertos por severidade e faixas de idade;
- divergência entre decisão final/recomendação e idade da exceção.

### Laboratório de Falhas

- quantidade de falhas ativas por cenário/ambiente permitido;
- resultados de ativação/expiração/parada de emergência;
- duração da recuperação do experimento;
- banner/estado visível permanente enquanto qualquer falha estiver ativa.

Valores de labels do Prometheus não devem conter SKU, ID do produto, ID do usuário, ID de correlação, ID de trace, exceção bruta ou versão da release se não forem limitados. Esses dados pertencem a logs/traces com controles adequados.

## Traces

Rastrear caminhos críticos:

- validação de login/sessão e decisão de autorização, sem credenciais/secrets da política;
- criação/atualização no catálogo até commit do banco e criação da outbox;
- reivindicação/publicação da outbox, entrega do broker, processamento no worker e solicitação downstream;
- metadados de upload de mídia, interação com objeto e etapas de processamento;
- validação/normalização/persistência da ingestão de qualidade;
- avaliação de gate/pontuação/risco e registro da decisão de release.

Spans identificam operação do módulo, resultado, duração e atributos seguros de dependência. Instrumentação de banco registra operação/tabela ou formato sanitizado da instrução, não valores sensíveis vinculados. Spans HTTP usam templates de rota. Spans de mensageria carregam tipo/versão e ID do evento em atributos controlados ou logs, sem labels de métricas de alta cardinalidade.

A política de amostragem deve preservar erros e experimentos críticos de release/falha enquanto controla o volume do tráfego normal. A escolha de head/tail sampling e disponibilidade do collector são decisões operacionais posteriores. Trace ausente não é interpretado como operação aprovada.

## Health checks

| Verificação | Significado | Comportamento |
| --- | --- | --- |
| Liveness | Processo está executando e não está irrecuperavelmente travado | Não falha apenas porque uma dependência externa recuperável está indisponível |
| Readiness | Instância pode aceitar seu trabalho pretendido com segurança | Reflete prontidão de dependência crítica/migração/inicialização; impede tráfego prematuro |
| Startup | Inicialização/migração lenta foi concluída | Separa tolerância de startup de liveness em runtime quando a plataforma oferece suporte |
| Detalhe de dependência | Estado diagnóstico autorizado de banco/broker/storage/downstream | Não é exposto publicamente com hostnames, credenciais ou detalhes internos |

A resposta pública de health é mínima. A política de readiness da API de Catálogo deve distinguir a necessidade essencial do banco da degradação assíncrona de broker/downstream: falha downstream não deve desabilitar desnecessariamente leituras/escritas do catálogo quando a outbox puder acumular com segurança. A readiness do worker depende dos limites de broker/banco necessários.

Na Fase 01, liveness e readiness retornam somente `UP` porque não existem dependências externas nem migrações. Ambas usam a porta HTTP da aplicação. Outros endpoints do Actuator não são expostos.

Atualização da fonte de evidência de qualidade é status de qualidade, não liveness do processo.

## Dashboards

Dashboards são visões de sinais versionados/sob responsabilidade definida, não o único lugar em que as definições existem.

### Visão geral do sistema

- taxa de solicitações, erros e latência por módulo;
- saturação de runtime/banco;
- health das dependências;
- deploy/build atual e estado ativo do Laboratório de Falhas.

### Catálogo e mídia

- resultados de comandos/leituras do catálogo e conflitos de concorrência;
- latência/timeouts do banco;
- idade de itens pendentes/com falha de mídia, duração do processamento e health do object storage.

### Confiabilidade da integração

- backlog da outbox e idade do item mais antigo;
- taxa de publicação/consumo/entrega;
- retries, resultados duplicados, falhas terminais e atividade de replay;
- latência/taxa de erro downstream e recuperação.

### Qualidade da release

- atualização/cobertura das evidências por release;
- suítes/resultados e estabilidade na primeira tentativa;
- defeitos/achados, limites de performance e resultados dos gates;
- pontuação/risco/recomendação com versão da fórmula/política;
- bloqueios críticos, exceções e decisão humana final.

### Experimento de falha

- hipótese/janela do experimento e falha injetada ativa;
- indicador de estado estável, fluxo afetado e sinal da dependência;
- traces exemplares, tempo de recuperação e condição de aborto;
- execução/evidência/conclusão do teste vinculado.

## Alertas

Alertas exigem responsável, severidade, descrição acionável, link de investigação e runbook testado. Candidatos iniciais, após definição de baseline:

| Condição | Justificativa | Resposta inicial |
| --- | --- | --- |
| Taxa de erro/latência da API excede limite sustentado | degradação visível ao usuário | inspecionar deploy, rota e traces de dependência |
| Saturação/timeouts do pool do banco | risco de disponibilidade em cascata | inspecionar operações lentas, uso de conexões e alterações recentes |
| Idade/backlog da outbox cresce | alterações confirmadas não chegam ao broker | inspecionar publicador/broker e claims travados |
| Falhas terminais da integração ou pico de retry | divergência downstream/tempestade de retry | classificar resposta downstream, pausar/repetir com segurança |
| Idade de falha/backlog de mídia cresce | mídia do produto indisponível/travada | inspecionar processador/armazenamento e limites de recursos |
| Falha em append de auditoria exigida | risco de responsabilização/segurança | invocar política fail-closed e investigar armazenamento/caminho |
| Fonte de evidência desatualizada ou pico de rejeição de ingestão | decisão de release pode ser inválida | bloquear/marcar desconhecido e inspecionar fonte/schema/autenticação |
| Erro de avaliação de Gate de Qualidade | recomendação não confiável | marcar avaliação indisponível e investigar fórmula/entrada |
| Achado crítico de segurança ingerido | risco imediato de release | validar, notificar responsável e bloquear criticamente a release afetada |
| Falha próxima do TTL ou parada falha | risco de contenção | parada de emergência e escalação ao responsável pelo ambiente |

Evitar alertar a cada 4xx, retry ou teste com falha. Agregar e direcionar conforme o impacto. Limites começam como observações em dashboard, tornam-se alertas após baseline e tornam-se gates críticos somente sob política de qualidade versionada.

## Investigação de falhas assistida por QA

Para uma falha automatizada ou exploratória, a equipe de QA deve conseguir:

1. Identificar requisito/caso de teste exato, candidato/build da release, ambiente, identidade dos dados e tentativa.
2. Copiar o ID de correlação da resposta ou ID de trace da evidência.
3. Localizar o span da solicitação e determinar se a falha ocorreu em UI, API/módulo, banco, broker, worker, storage ou downstream.
4. Comparar métricas na janela da execução para latência, saturação, fila/backlog e ativação de falhas.
5. Consultar logs estruturados por ID de correlação/evento/execução para resultados classificados e seguros.
6. Distinguir falha de asserção do produto de falha de infraestrutura, dados de teste, evidência desatualizada ou ferramenta.
7. Preservar a evidência mínima útil e vinculá-la a um defeito/release sem copiar secrets.
8. Reproduzir com dados/falha controlados quando seguro e então verificar recuperação e telemetria.

Exemplo:

~~~text
TC-INT-CAT-004 falhou no build abc...
  -> correlationId vincula commit do catálogo e evento de outbox
  -> eventId vincula publicação, consumidor e tentativas de entrega
  -> trace mostra timeout downstream em 2 s
  -> métrica mostra retries limitados e outbox saudável
  -> status da integração torna-se falha recuperável
  -> nenhum efeito downstream duplicado após replay
~~~

A equipe de QA não usa o texto privado de logs como único oráculo de correção. Contratos estáveis de estado/API comprovam comportamento; telemetria explica comportamento e recuperação.

## Correlação entre release e deploy

Todo runtime e recurso de telemetria identifica versão da aplicação, commit/build imutável e ambiente como atributos do recurso. Marcadores de deploy/alteração permitem comparar o comportamento antes/depois nos dashboards. Evidências de qualidade devem apontar ao candidato exato e à mesma identidade de build usada pelo runtime/deploy; a versão de exibição da release é insuficiente.

## Testes de observabilidade

- Testes de componente/integração verificam eventos/atributos estruturados exigidos para resultados críticos sem especificar demais o texto.
- Testes de redação injetam valores sintéticos semelhantes a secrets/dados pessoais e verificam sua ausência em logs/traces/erros.
- Testes de propagação de trace atravessam HTTP, outbox/mensagem e mock downstream.
- Testes de métricas verificam conjuntos limitados de labels e classificação correta do resultado.
- Health checks são testados sob falha e recuperação de dependências.
- Regras de alerta/runbooks são exercitados com cenários sintéticos/de falha seguros.
- Experimentos do Laboratório de Falhas exigem telemetria esperada e evidência de recuperação.

## Retenção, acesso e confiabilidade

A retenção varia por sinal/classificação e deve ser definida antes de produção hospedada. O acesso segue privilégio mínimo: acesso amplo a dashboards não implica acesso a dados brutos de segurança/auditoria/evidência. Sistemas de telemetria devem ter quotas, backpressure e comportamento para falha de disco/collector; threads da aplicação não devem bloquear indefinidamente na exportação.

Auditoria não equivale a logging da aplicação e pode exigir maior integridade/retenção. Referências de evidência de qualidade podem sobreviver a traces de alto volume, portanto trechos diagnósticos necessários ou links imutáveis seguem política explícita de evidências.

## Decisões e riscos em aberto

- Topologia/collector e perfil local de Docker Compose.
- Backend de logs/traces; Prometheus/Grafana isoladamente não armazenam logs/traces a longo prazo.
- Amostragem, retenção, residência de dados e restrições de custo.
- Objetivos concretos de nível de serviço e limites de alerta após baseline.
- Se snapshots de evidência retêm trechos selecionados de trace/log quando a telemetria expirar.
- Visibilidade segura de correlação na UI do cliente e fluxos de suporte com controle de acesso.
- Design de armazenamento/evidência de adulteração da auditoria, distinto de logs normais.
- Responsabilidade operacional/expectativas de plantão para um projeto de portfólio.
