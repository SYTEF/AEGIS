# Estratégia de Segurança do AEGIS

## Finalidade e estado atual

Segurança é uma restrição de design no AEGIS Commerce, no Centro de Controle de Qualidade e no Laboratório de Falhas. Este documento define a política inicial e o modelo de ameaças. A Fase 01 implementa somente os controles da fundação HTTP descritos abaixo. A Fase 02 está aprovada como Local Development Preview, mas seus controles ainda não estão implementados; não existe Auth/RBAC nem certificação para produção.

### Controles implementados na Fase 01

- bind local padrão em `127.0.0.1`, JMX desabilitado e ausência de credenciais ou secrets reais no repositório;
- exposição do Actuator limitada a `health` e `info`, com respostas de health sem componentes ou detalhes internos;
- `info` construído por allowlist e commit SHA opcional validado, sem dump de environment, host, PID, caminhos, propriedades ou argumentos;
- correlação limitada por allowlist/tamanho, substituição segura de valores inválidos ou múltiplos e limpeza/restauração do MDC em `finally`;
- Problem Details com detalhe seguro, path limitado sem query string/matrix parameters, timestamp UTC e sem stack trace ou classe de exceção no cliente;
- logs JSON com evento HTTP mínimo, template de rota quando disponível ou path sanitizado, startup info desabilitado e sem usuário/path local, headers, cookies, corpos ou query string;
- CI sem secrets, com `contents: read`, actions fixadas por SHA completo e somente build/testes.

### Controles obrigatórios para a Local Development Preview da Fase 02

Quando a Fase 02 for implementada:

- backend e dev server frontend devem permanecer ligados a loopback por padrão;
- uma tentativa de iniciar a prévia mutável em interface pública deve falhar com segurança, não apenas emitir warning;
- a porta PostgreSQL publicada no host deve ser limitada a loopback;
- a UI deve exibir aviso persistente de que autenticação/RBAC não existem;
- não devem existir login, token, role ou usuário autenticado fictícios;
- o dev server deve usar proxy local para `/api`; CORS permissivo não é um atalho aceitável;
- DTOs explícitos, rejeição de campos desconhecidos e allowlists de ordenação/filtro devem impedir mass assignment;
- normalização Unicode/whitespace aprovada para Category, Product Name e Product Description deve ocorrer no servidor antes da validação, unicidade e persistência, sem confiar na normalização do navegador;
- Name e Description são conteúdo não confiável e devem ser renderizados como texto, sem HTML arbitrário;
- ETag/If-Match e optimistic locking devem impedir lost update, mas não são autorização;
- criação/mudança de Product concorrente com desativação de Category deve falhar de forma consistente antes de confirmar Product ACTIVE associado a Category INACTIVE;
- structured logging, Problem Details e X-Correlation-ID da Fase 01 devem ser reutilizados sem registrar payloads, Description, SKU, preço ou configuração interna.

Esses controles permitem apenas execução local/isolada. A Fase 02 não satisfaz REQ-AUTH-*, não passa o Gate de Exposição Externa e não pode ser descrita como production-ready.

Achados de segurança são evidências para decisões de release. Varredura automatizada apoia, mas não substitui, modelagem de ameaças, revisão de código, testes de casos de abuso ou triagem humana.

## Princípios de segurança

- Negar por padrão e conceder a capacidade mínima necessária.
- Autenticar identidades e autorizar toda ação protegida no servidor.
- Tratar navegadores, uploads, mensagens, fontes de CI e serviços externos como não confiáveis.
- Validar em todo limite de confiança e codificar/parametrizar para o contexto de destino.
- Minimizar dados sensíveis, exposição, privilégios, retenção e blast radius.
- Manter secrets fora do código, logs, traces, erros e evidências.
- Usar padrões seguros; capacidades perigosas como o Laboratório de Falhas ficam desligadas por padrão.
- Registrar ações atribuíveis e relevantes para segurança em histórico de auditoria protegido contra alteração.
- Operar em fail-closed quando continuar contornaria autenticação, autorização, auditoria crítica ou controles de integridade.
- Tornar falhas de controle de segurança observáveis sem vazar detalhes do ataque.
- Corrigir e reavaliar continuamente; uma varredura isolada não é garantia.

## Ativos e objetivos de segurança

| Ativo | Objetivos principais | Exemplos de dano |
| --- | --- | --- |
| Credenciais e sessões | confidencialidade, integridade, capacidade de revogação | tomada de conta, personificação |
| Roles e permissões | integridade, auditabilidade | elevação de privilégio, decisão de release não autorizada |
| Dados de catálogo/produto | integridade, disponibilidade | preço/estoque incorreto, conteúdo oculto ou malicioso |
| Imagens de produtos | integridade, processamento seguro, controle de acesso | malware/polyglot, XSS armazenado, exaustão de recursos |
| Auditoria/histórico | integridade, retenção, acesso restrito | repúdio, adulteração de evidência, vazamento de privacidade |
| Mensagens/outbox | autenticidade, integridade, proteção contra replay | alterações downstream duplicadas/perdidas |
| Evidências de teste e qualidade | proveniência, integridade, confidencialidade | aprovação forjada, atribuição ao build errado, vazamento de secret |
| Política/pontuação/gates de qualidade | integridade, explicabilidade | risco crítico diluído ou política adulterada |
| Decisões de release | autorização, não repúdio | aprovação não autorizada, exceção ocultada |
| Controles do Laboratório de Falhas | autorização rigorosa, contenção, disponibilidade | interrupção persistente ou ativação em produção |
| Secrets e cadeia de suprimentos | confidencialidade, integridade | comprometimento de dependência, acesso à infraestrutura |

## Limites de confiança

~~~mermaid
flowchart LR
    Browser["Navegador/cliente não confiável"] -->|"HTTPS + autenticação"| API["Limite de confiança da API AEGIS"]
    CI["CI e fontes de teste"] -->|"identidade com escopo + schema"| API
    API -->|"conta com privilégio mínimo"| DB[("PostgreSQL")]
    API -->|"objetos privados"| Store[("MinIO")]
    API -->|"mensagens versionadas"| Broker[("RabbitMQ")]
    Broker --> Worker["Worker de integração"]
    Worker -->|"timeout + idempotência"| External["Mock não confiável do Centro de Vendas"]
    API -."telemetria sanitizada".-> Telemetry["Sistemas de observabilidade"]
~~~

Cruzar um limite exige autenticação explícita quando aplicável, autorização, validação, timeout/limites de recursos e telemetria segura. Estar em uma rede interna não é controle de identidade.

## Autenticação

O mecanismo exato de autenticação/sessão no navegador exige um ADR. Todo design aceito deve:

- usar framework/provedor maduro em vez de criptografia customizada;
- armazenar senhas somente como hash adaptativo com salt se as senhas forem gerenciadas localmente;
- aplicar TLS fora do desenvolvimento exclusivamente local;
- usar credenciais/sessões com prazo, rotação e revogação adequadas ao modelo de ameaças;
- impedir session fixation e reutilização de token após logout/revogação quando prometido;
- proteger tokens no transporte/armazenamento e nunca colocar bearer tokens em URLs;
- limitar taxa e monitorar tentativas de autenticação sem expor existência da conta;
- retornar mensagens genéricas de falha de autenticação;
- definir recuperação/bootstrap administrativo com segurança antes do uso em produção;
- considerar MFA para aprovadores de release/administradores antes de um deploy real.

Se cookies do navegador forem selecionados, devem ser HttpOnly, Secure fora de HTTP local, ter SameSite e escopo adequados e ser combinados com proteção CSRF para alterações de estado. Se bearer tokens forem selecionados, armazenamento no navegador e consequências de XSS exigem revisão explícita; tokens de longa duração em localStorage não são padrão sem análise.

Identidades de ingestão serviço-a-serviço/CI são distintas de sessões humanas, estritamente delimitadas, rotacionáveis e atribuíveis.

## Autorização e RBAC

- Permissões representam capacidades como catalog:write e release:decide; roles agrupam capacidades.
- Autorização de endpoint, objeto e transição de estado é sempre aplicada no servidor.
- Um usuário não deve inferir nem acessar objeto protegido apenas alterando um ID (defesa contra BOLA/IDOR).
- Alterações de role, desativação, mudanças de política, replays, operações do Laboratório de Falhas e decisões de release são auditadas.
- Operadores normais de catálogo não podem alterar política de qualidade nem registros de auditoria.
- Ingestão de evidências de qualidade não concede permissão de decisão de release.
- quality:policy:admin é separada de quality:write genérica e é exigida para fórmulas, políticas de risco e definições de gates.
- Uma demonstração individual pode acumular várias roles, mas o modelo deve preservar a separação e mostrar quando a segregação de funções não é alcançada.
- Bootstrap administrativo e prevenção da remoção acidental do último administrador necessário precisam de critérios de aceite explícitos antes da implementação.

A política de autorização deve ser testada com exemplos permitidos e negados. Ocultar botões é usabilidade, não controle de acesso.

A matriz inicial de responsabilidades ADMIN, QUALITY_MANAGER, OPERATOR e VIEWER está definida em [REQUIREMENTS.md](REQUIREMENTS.md#matriz-conceitual-mínima-de-roles). Antes da autenticação real, a mutação do catálogo é apenas uma prévia de desenvolvimento local; exposição externa ou falha do bloqueio de bind são achados P0 e bloqueios críticos.

## Validação de entrada e segurança da saída

- Usar modelos explícitos de solicitação/comando e campos permitidos para impedir mass assignment.
- Aplicar validação de tamanho, tipo, intervalo, formato, estado, referência e invariantes de negócio.
- Normalizar somente onde regras exigirem explicitamente; validar formas canônicas consistentemente, como SKU.
- Usar consultas parametrizadas/binding do ORM; nunca concatenar SQL, comandos ou caminhos de objeto não confiáveis.
- Permitir somente campos de ordenação/filtro aprovados e limitar complexidade de consulta, tamanho de página e volume de resultados.
- Codificar saída para seu contexto real de HTML/URL/JSON e aplicar Content Security Policy restritiva ao frontend.
- Tratar descrições/nomes de produto como conteúdo não confiável mesmo quando inseridos por usuários autenticados.
- Evitar desserialização insegura e ativação de tipos polimórficos vindos da entrada.
- Contratos de erro expõem códigos seguros e estáveis e IDs de correlação, nunca stack traces, SQL, hostnames ou secrets.
- Requisições rejeitadas pelo servidor HTTP antes da cadeia Servlet/Spring ficam fora das garantias de Problem Details e correlação da aplicação; a resposta mínima do container não deve revelar versão, stack trace ou detalhe interno.
- Validar schema de mensagem/evento e ingestão de qualidade, além da proveniência da fonte/build; não confiar em alegações do payload da CI somente porque estão bem formadas.

## Upload seguro e processamento de mídia

Imagens de produtos são hostis até serem validadas e processadas.

Controles exigidos:

- autorização e permissão de produto no nível do objeto antes do upload;
- limites conservadores de tamanho total/solicitação/arquivo, dimensões, pixels/descompressão e tempo de processamento;
- validação de assinatura/magic bytes do conteúdo e decoder seguro, não apenas extensão/MIME;
- allowlist de formatos raster realmente necessários; SVG é inicialmente rejeitado pelo risco de conteúdo ativo;
- nomes de objeto opacos gerados pelo servidor; nome original retido apenas como metadado sanitizado quando necessário;
- armazenamento privado por padrão, com prefixos/buckets separados para pendente/processado quando útil;
- processamento em contexto restrito sem privilégios desnecessários de rede/sistema de arquivos;
- recodificação em variantes de saída aprovadas para reduzir risco de conteúdo incorporado/ativo;
- checksum, metadados de mídia, status e trilha de auditoria;
- Content-Type, Content-Disposition, nosniff e autorização de entrega seguros;
- limpeza/reconciliação de objetos com falha, órfãos e excluídos;
- varredura antivírus apenas como defesa em profundidade, não substituto da validação e do isolamento.

URLs assinadas, se usadas, são de curta duração, restritas a um objeto/ação e nunca registradas com credenciais. Metadados da imagem, incluindo localização/EXIF, são removidos salvo requisito que justifique retenção.

## Segurança da API

O design da API segue [API_SPEC.md](API_SPEC.md) e aborda riscos OWASP de API:

- autorização de objeto/função/propriedade em toda operação protegida;
- limites de payload, paginação, taxa, tempo de execução e busca custosa;
- modelos explícitos de transição de estado e controles de idempotência/conflito;
- allowlist segura de CORS e headers de segurança;
- defesa CSRF se credenciais ambientes do navegador forem usadas;
- validação de schema/versão para APIs e eventos;
- timeouts de dependência e mapeamento controlado de erro;
- inventário de endpoints, versões e depreciações;
- monitoramento de abuso para login, uploads, ingestão, replay e Laboratório de Falhas;
- nenhum endpoint debug/admin do framework exposto ou não documentado.

Limite de taxa é baseado em risco e aplicado em camadas. Não deve ser a única defesa contra operações custosas sem limite.

## Secrets e configuração

- Nenhum secret real no código, exemplos, dados de teste, camadas de imagem Docker ou arquivos de ambiente versionados.
- .env.example pode documentar nomes com placeholders inertes; .env permanece ignorado.
- Desenvolvimento local usa valores gerados ou pertencentes ao desenvolvedor; CI usa armazenamento protegido de secrets com privilégio mínimo.
- Preferir credenciais de curta duração/workload quando suportadas; rotacionar credenciais de longa duração e documentar responsabilidade.
- Separar credenciais por ambiente e componente; contas de banco, broker e object store recebem somente as permissões necessárias.
- Falhar a inicialização quando secret exigido estiver ausente ou inseguro para o ambiente selecionado; nunca usar silenciosamente um padrão inseguro para produção.
- Redigir campos comuns e estruturados de secrets nos limites de logging/telemetria e testar a redação.
- Varredura de secrets executa antes da release e, idealmente, antes do commit/PR.

## Segurança de logs, telemetria e evidências

Logging de segurança registra resultado e atribuição, não conteúdo sensível. Nunca registrar:

- senhas, hashes, bearer/refresh tokens, cookies de sessão ou chaves secretas;
- headers completos de autorização ou URLs assinadas;
- corpos arbitrários de solicitação/resposta;
- arquivos brutos recebidos;
- dados pessoais desnecessários ou artefatos completos de qualidade.

Usar campos estruturados permitidos, IDs estáveis pseudônimos quando apropriado, IDs de correlação/trace e controles centralizados de acesso. Sanitizar quebras de linha/caracteres de controle para impedir injeção em log. Falha na exportação de telemetria não deve contornar controle de segurança; persistência crítica de auditoria segue a política fail-closed definida nos requisitos.

Arquivos de evidência podem conter secrets, dados pessoais ou caminhos internos. A ingestão exige política de conteúdo/tamanho, controle de acesso, retenção e redação. Acesso amplo de leitura ao Centro de Controle de Qualidade não concede automaticamente acesso a todo artefato bruto.

A source declarada em um payload de evidência nunca é autoridade. A fonte efetiva é derivada ou obrigatoriamente validada contra a identidade autenticada e delimitada e, quando disponível, vinculada a repositório, workflow, commit SHA, identidade imutável do build e versão do schema. ID de execução da fonte mais fingerprint canônico do payload oferecem detecção de replay/conflito. Assinaturas/atestações de artefatos continuam sendo opção futura de fortalecimento.

## Estratégia de auditoria

Eventos de auditoria são append-only durante operações normais e incluem horário UTC, ator/serviço autenticado, ação, alvo, resultado, código de motivo, fonte e ID de correlação. Eventos prioritários incluem:

- indicadores de sucesso/falha/revogação de autenticação;
- alterações de role, permissão e status de usuário;
- mutações de produto/catálogo/mídia;
- autorização negada e padrões suspeitos de abuso;
- replay de integração e tratamento de falha terminal;
- alterações de política/gate de qualidade e rejeição de ingestão de evidência;
- ativação/parada/expiração do Laboratório de Falhas;
- snapshot da recomendação de release, decisão final e exceção.

O acesso à auditoria é restrito, consultas também são auditáveis quando o risco justificar, relógios são sincronizados e política de retenção/detecção de adulteração é definida antes do uso em produção. Dados de auditoria não são editáveis silenciosamente pela aplicação.

Os futuros comandos a seguir exigem aceitação durável de auditoria antes de informar sucesso:

- criação/desativação privilegiada de usuário e atribuição/remoção de role;
- alterações de fórmula de qualidade, política de risco e definição de gates;
- decisões finais de release e exceções aprovadas;
- replay de falhas terminais da integração;
- ativação ou alteração de parâmetro/escopo do Laboratório de Falhas;
- recuperação de credencial ou rotação privilegiada de secret/identidade quando implementada.

A auditoria de alterações de negócio do catálogo pode ser uma projeção eventualmente consistente da intenção confirmada da outbox, com retry e alerta. Contenção nunca deve ser impedida por falha de auditoria: parada emergencial do Laboratório de Falhas/revogação de sessão prossegue e gera alerta crítico quando o append de auditoria não puder ser concluído. O limite de append recebe snapshots de ator/correlação e não chama Auth, evitando ciclo de dependência.

## Segurança da integração externa e mensageria

- Tratar downstream e mensagens como não confiáveis mesmo dentro de redes Docker.
- Usar contas/vhosts/permissões separados e com privilégio mínimo no broker por componente quando configurado.
- Validar schema, tipo/versão de evento, tamanho e identidades exigidas antes do processamento.
- Aplicar idempotência e proteger contra replay; não tratar contagem de entregas como prova isolada de malícia.
- Autenticar e criptografar comunicação downstream fora de ambientes controlados exclusivamente locais.
- Definir timeout, limitar retries e evitar vazamento de credenciais em corpos de erro/telemetria.
- Acesso a dead-letter/replay é privilegiado e auditado; payloads permanecem sujeitos à classificação de dados.
- Não desserializar classes arbitrárias nem executar expressões fornecidas por mensagens.

## Integridade do Motor de Qualidade e da release

- Fontes de evidência usam identidades autenticadas, delimitadas e ingestão idempotente.
- Identidade de release/build, fonte, schema, timestamp e atualização são validados.
- Fórmula, pesos, gates e política de risco são versionados, revisados e auditados.
- Entradas e versões das avaliações históricas são retidas ou referenciadas de modo imutável.
- Evidência ausente/com erro/desatualizada é explícita, nunca convertida em aprovação.
- Resultados de gates críticos não podem ser sobrepostos pelo cálculo da pontuação.
- Decisões humanas finais e qualquer exceção permitida exigem permissão, justificativa, snapshot de evidência e expiração.
- A apresentação no dashboard deve distinguir recomendação, decisão final e dados desatualizados para impedir engenharia social pela UI.

## Segurança do Laboratório de Falhas

- Compilar/configurar o Laboratório de Falhas fora da exposição de produção ou negar por múltiplos controles independentes.
- Desligado por padrão em todo ambiente; somente cenários e escopos explicitamente permitidos.
- Permissão forte, distinta de permissões comuns de QA/catálogo.
- Intensidade/duração máximas, TTL automático, parada de emergência e verificação da identidade do ambiente.
- Nenhum parâmetro de falha aceita injeção arbitrária de URL, SQL, comando, script ou expressão.
- Toda ativação, alteração, expiração e parada é auditada e visivelmente indicada.
- Experimentos definem critérios de aborto e não podem atingir sistemas fora do ambiente controlado do AEGIS.

## Segurança da cadeia de suprimentos e CI/CD

Na implementação corrente e em sua evolução:

- fixar versões de ferramentas/actions em referências imutáveis ou controladas quando viável;
- revisar necessidade, proveniência, manutenção e licença de dependências;
- gerar inventário de dependências/SBOM para candidatos de release conforme a maturidade crescer;
- verificar dependências, código-fonte, secrets e imagens de container sob política versionada;
- proteger a branch padrão e gates exigidos;
- restringir permissões do token do workflow e acesso a secrets por pull requests não confiáveis;
- produzir artefatos rastreáveis a partir de código revisado e registrar identidade de commit/build;
- separar autoridade de build da de deploy; credenciais de ingestão de relatórios de teste não podem fazer deploy;
- corrigir segundo explorabilidade e exposição, preservando bloqueios críticos de severidade.

## Modelo de ameaças inicial

A tabela usa STRIDE como estímulo, não como alegação de completude.

| Ameaça | Limite/ativo | Exemplo | Mitigações iniciais | Risco residual/em aberto |
| --- | --- | --- | --- | --- |
| Spoofing | Autenticação | credential stuffing ou token roubado | hash adaptativo, erros genéricos, limite de taxa, expiração/revogação, TLS, auditoria | MFA e mecanismo de identidade não decididos |
| Spoofing | Ingestão de qualidade | fonte de CI forjada informa testes aprovados | identidade de serviço delimitada, idempotência de fonte/execução, proveniência do build, auditoria | maturidade de assinatura/atestação de artefato em aberto |
| Tampering | Catálogo | mutação não autorizada de preço/estoque | Fase 02 somente em loopback, validação, concorrência e histórico; RBAC/verificações de objeto entram na Fase 03 | até Auth/RBAC, qualquer exposição externa é P0 e bloqueante |
| Tampering | Relação Product/Category | corrida associa Product ACTIVE enquanto Category é desativada | invariável transacional, precondições ETag/If-Match, constraints/locking proporcionais e teste concorrente PostgreSQL | mecanismo físico de locking deve ser escolhido e comprovado na implementação |
| Tampering | Política de qualidade | pesos/gates alterados para aprovar release | permissão separada, versionamento, revisão, auditoria, avaliação histórica imutável | segregação em portfólio de uma pessoa |
| Repúdio | Release | aprovador nega exceção/decisão | decisão atribuível e imutável e snapshot de evidência | não repúdio/assinatura mais forte no futuro |
| Divulgação de informação | Erros/telemetria | token, SQL ou dados pessoais em logs/evidências | campos estruturados permitidos, testes de redação, erros seguros, acesso/retenção | padrões de ferramentas de terceiros exigem revisão |
| Divulgação de informação | Object storage | URL de imagem/evidência adivinhada ou pública | buckets privados, chaves opacas, autorização de objeto, URLs assinadas curtas | design de CDN/cache em aberto |
| Negação de serviço | API/busca | página/consulta/upload/relatório enorme | limites de tamanho/página/tempo/taxa, streaming, quotas, backpressure | limites exatos exigem baseline |
| Negação de serviço | Processamento de imagem | bomba de descompressão ou entrada maliciosa no decoder | limites de pixels/tamanho/tempo, recodificação, privilégio mínimo isolado | escolha de malware/decoder em aberto |
| Negação de serviço | Mensageria | tempestade de retry ou mensagem problemática | classificação, backoff/jitter limitado, DLQ, política de circuito, alerta | topologia/capacidade não decididas |
| Elevação de privilégio | API | BOLA/IDOR ou mass assignment | autorização de objeto/função/propriedade, modelos explícitos de comando, testes negativos | biblioteca de autorização/política central não decidida |
| Elevação de privilégio | Laboratório de Falhas | usuário comum ativa atraso no banco | permissão distinta, negação por ambiente, allowlist, TTL, auditoria | design de proteção independente em produção pendente |
| Integridade/replay | Integração | mensagem duplicada repete alteração downstream | identidade de evento, consumidor/chave downstream idempotente, resultado atômico | comportamento de idempotência downstream deve ser contratado |
| Integridade | Pontuação de qualidade | evidência ausente/desatualizada tratada como aprovada | atualização, estado de evidência insuficiente, gates críticos, versão da fórmula | pesos/limites precisam de calibração |
| Cadeia de suprimentos | Build | dependência/action maliciosa | fixação de versões, workflow com privilégio mínimo, varredura, revisão, proveniência | meta de assinatura/SLSA não selecionada |

## Mapa de verificação orientado pela OWASP

| Preocupação | Foco do AEGIS |
| --- | --- |
| Controle de acesso quebrado / API BOLA | RBAC mais testes negativos no nível de recurso e transição de estado |
| Falhas criptográficas | bibliotecas maduras, TLS, secrets protegidos, sem criptografia customizada |
| Injeção | modelos explícitos, consultas parametrizadas, codificação segura de saída, sem expressões arbitrárias no Laboratório de Falhas |
| Design inseguro | casos de abuso, gates críticos, idempotência, retry limitado, revisão de ameaças por fase |
| Configuração incorreta de segurança | padrões seguros por ambiente, sem endpoints debug, headers/CORS, dependências de privilégio mínimo |
| Componentes vulneráveis/desatualizados | inventário, varredura, triagem, bloqueio crítico e responsável pela correção |
| Falhas de autenticação | respostas genéricas, ciclo de sessão, proteção contra força bruta e auditoria |
| Falhas de integridade | contratos versionados, proveniência, idempotência, dependências controladas da CI |
| Falhas de logging/monitoramento | eventos seguros de segurança, alertas, correlação e caminhos de investigação testados |
| SSRF | não aceitar URLs arbitrárias de downstream/objeto; permitir destinos explicitamente e restringir egress do worker |

## Cadência de revisão e testes de segurança

- Requisitos/design: identificar ativos, casos de abuso, autorização, classificação de dados e modo de falha.
- Pull request: revisão segura mais verificações aplicáveis de código estático, secrets e dependências.
- Funcionalidade/integração: testes de autorização, API negativa, upload, mensageria e integridade de dados.
- Candidato a release: achados triados, avaliação de gate crítico, risco residual e revisão de exceção.
- Mudança periódica/importante: atualizar modelo de ameaças e executar testes manuais direcionados.
- Depois de incidente/defeito: preservar evidência, avaliar caminhos semelhantes e adicionar cobertura de prevenção/detecção.

## Tratamento de vulnerabilidades

Relate vulnerabilidades de forma privada ao responsável pelo repositório até existir um canal formal. Registre severidade, explorabilidade, versões afetadas, evidência, contenção, responsável e alvo. Não publique detalhes exploráveis nem credenciais reais em itens. A correção exige teste de regressão/segurança e revisão de controles relacionados. Achados críticos exploráveis são bloqueios críticos conforme [QUALITY_GATES.md](QUALITY_GATES.md).

## Checklist de segurança da release

- Comportamentos de autenticação/sessão e autorização correspondem ao design revisado.
- Entradas, arquivos, APIs, eventos e fontes de evidência novos/alterados foram modelados quanto a ameaças.
- Nenhum secret ou artefato sensível foi introduzido em código, logs ou relatórios.
- Achados de dependência/código/imagem foram triados; bloqueios críticos resolvidos.
- Testes de segurança e verificações de auditoria/redação passam para o risco alterado.
- Configuração de privilégio mínimo e diferenças entre ambientes foram revisadas.
- Ameaças relevantes, riscos residuais, exceções e documentação estão atualizados.

## Decisões e riscos de segurança em aberto

- Arquitetura de autenticação, escopo de MFA e recuperação de administrador.
- Matriz detalhada de roles/permissões e separação do aprovador de exceções.
- Gerenciamento de chaves/secrets para futuro ambiente hospedado.
- Limites de formato/tamanho de upload, sandbox de processamento e papel da varredura de malware.
- Acesso, retenção e fluxo de redação do armazenamento de evidências.
- Meta de proveniência/assinatura de artefatos para v1.0.
- Retenção, evidência de adulteração e conciliação de privacidade/exclusão da auditoria.
- Limites concretos de taxa e alertas baseados em uso medido.
