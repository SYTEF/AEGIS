# Planos de Execução do AEGIS

## Governança do plano

Este arquivo decompõe o [roadmap](docs/ROADMAP.md) em fases candidatas e revisáveis. Ele não concede autorização geral para implementá-las. **A Fase 01 está implementada localmente na branch `feat/phase-01-foundation` e aguarda o gate da CI remota e revisão humana; nenhuma fase posterior foi iniciada ou autorizada.** Cada nova fase exige aprovação humana explícita, confirmação de escopo e refinamento dos requisitos afetados antes do início do trabalho.

Regras:

- Concluir ou replanejar deliberadamente a fase atual antes de iniciar a próxima.
- Manter as fases independentemente revisáveis e demonstráveis.
- Atualizar IDs de requisitos, riscos e critérios de aceite conforme o aprendizado.
- Não antecipar infraestrutura de fases posteriores “para uso futuro”.
- Aplicar [AGENTS.md](AGENTS.md) e as políticas aplicáveis de qualidade, segurança e observabilidade a cada fase.
- Criar ADRs imediatamente antes de decisões importantes, não como documentação especulativa.

## Fase 01 — Fundação backend executável mínima

### Estado

Implementada localmente e aguardando o gate da CI remota e revisão humana. A fundação produz um Jar executável sem serviços externos, expõe somente health/info operacionais, usa Problem Details e correlação segura, possui testes de unidade/componente/arquitetura e um workflow de CI Linux/Windows. Isso não autoriza nem inicia a Fase 02.

### Objetivo

Comprovar o menor caminho de desenvolvimento reproduzível com JDK 25 LTS / Spring Boot 4.1.x e a convenção modular, sem implementar comportamento de catálogo nem adicionar infraestrutura externa.

### Escopo

- Aplicar a baseline técnica aprovada: JDK 25 LTS, Spring Boot 4.1.x, Maven e empacotamento em Jar executável.
- Usar as coordenadas Maven io.github.sytef:aegis e o pacote-base io.github.sytef.aegis.
- Seguir o [ADR-001](docs/ADR/ADR-001-modular-monolith.md) aceito para a direção de Monólito Modular e as regras de dependência.
- Criar um projeto backend com um ponto de entrada mínimo da aplicação.
- Documentar e expor os comportamentos futuros de liveness, readiness e info não sensível usando mecanismos oferecidos pelo framework.
- Adotar Problem Details com application/problem+json para erros HTTP.
- Estabelecer o contrato limitado de X-Correlation-ID para respostas, logs estruturados e respostas de erro.
- Adicionar o menor workflow do GitHub Actions para build Maven, testes unitários e validação estática/básica de qualidade, com permissions: contents: read e sem secrets.
- Documentar pré-requisitos locais e comandos exatos de execução/validação.

### Fora de Escopo

- Frontend, PostgreSQL, RabbitMQ, MinIO, Grafana ou Docker Compose.
- Produtos, categorias, usuários, autenticação real, lógica de negócio do catálogo ou qualquer CRUD.
- Centro de Controle de Qualidade, Motor de Qualidade, mensageria ou mock externo.
- Stack completa de observabilidade; apenas diagnósticos estruturados no nível da aplicação justificados pelo esqueleto.
- Deploy ou hospedagem pública.

### Dependências

- Autorização humana explícita para implementar a Fase 01; as decisões deste plano autorizam apenas o planejamento.
- ADR-001 — Monólito Modular aceito.
- Disponibilidade de JDK 25 LTS e Maven nos ambientes de desenvolvimento e CI.
- Seleção de uma abordagem leve para imposição de modularidade consistente com o ADR-001; ela não deve criar módulos especulativos vazios.

### Entregáveis

- Estrutura mínima de código/build do backend Maven que produza aegis como Jar executável sob o group io.github.sytef e o pacote io.github.sytef.aegis.
- Contratos de versão/info, liveness/readiness e Problem Details documentados em OpenAPI executável ou contrato gerado equivalente quando houver suporte a endpoints operacionais.
- Testes smoke de unidade/componente e uma semente de teste de limites arquiteturais quando trouxer valor.
- Validação básica no GitHub Actions com contents: read, sem secrets e sem permissão adicional sem justificativa explícita.
- Instruções de desenvolvimento local e fixação de versões.
- Atualizações documentais que distingam o que passou a existir do que continua conceitual.

### Critérios de Aceite

1. Um novo contribuidor consegue clonar, executar um comando documentado e obter build/teste limpo e bem-sucedido sem serviços externos.
2. O Jar executável inicia; liveness indica somente a vida do processo, readiness indica aceitação de tráfego e info expõe apenas nome do serviço, versão da aplicação/build e commit SHA quando disponível.
3. Erros HTTP inesperados usam application/problem+json sem vazamento de stack trace ou configuração.
4. Um X-Correlation-ID válido, com 1–128 caracteres da lista permitida, é retornado e registrado; entrada ausente/inválida é substituída sem repetir conteúdo inseguro.
5. A convenção de pacote/módulo impede ou testa ao menos uma direção de dependência proibida sem criar módulos especulativos vazios.
6. A CI executa build Maven, testes unitários e validação estática/básica em ambiente limpo usando permissions: contents: read e sem secrets.
7. Nenhum frontend, banco de dados, broker, object storage, Grafana, autenticação, CRUD ou comportamento de negócio do catálogo foi adicionado.

### Testes Esperados

- Teste unitário para qualquer política customizada de correlação/resposta.
- Teste de componente para info/health e comportamento seguro de erro.
- Testes de contrato para limites de exposição de liveness/readiness/info e application/problem+json.
- Testes de limite para valores aceitos, ausentes, grandes demais e inválidos de X-Correlation-ID.
- Teste de regra arquitetural caso seja selecionado um mecanismo de imposição confiável e leve.
- Validação smoke de build limpo e inicialização.
- Varredura baseline de secrets/dependências apropriada ao build criado.

### Riscos

- Gastar uma fase com scaffolding sem valor de produto.
- Configurar arquitetura, telemetria ou CI em excesso antes de os requisitos exigirem.
- Escolher versões de ferramentas ou convenções de build que aumentem o atrito para contribuidores.
- Endpoints de health/info do framework vazarem configuração.
- Disponibilidade do toolchain ou uma mudança de patch no Spring Boot 4.1.x afetar o build fixado.

Mitigação: limitar esta fase a uma prova técnica vertical curta e iniciar a Fase 02 somente após revisão. Rejeitar módulos/dependências criados para “preparar o futuro”.

### Definição de Pronto (Definition of Done)

- Os critérios de aceite e as verificações aplicáveis de [AGENTS.md](AGENTS.md#definition-of-done) passam.
- Build, verificações estáticas e testes esperados passam localmente e na CI.
- A revisão de dependências/segurança não possui bloqueio crítico não resolvido.
- README/API/arquitetura/documentação de execução local correspondem ao esqueleto executável.
- O diff completo, as permissões da CI e os artefatos gerados foram revisados.
- Nenhum banco de dados, frontend ou funcionalidade não relacionada está presente.
- As permissões do workflow são exatamente contents: read, salvo se um requisito revisado justificar mais.

## Fase 02 — Núcleo do catálogo e prévia local de persistência (v0.2)

### Objetivo

Entregar a primeira fatia de valor real do produto por meio de uma API apoiada por PostgreSQL como **PRÉVIA DE DESENVOLVIMENTO LOCAL**, sem alegar mutação segura até que a Fase 03 conclua autenticação/RBAC.

### Escopo

- Refinar REQ-CAT-001 a REQ-CAT-012 em exemplos prontos para implementação, selecionando uma pequena sequência de fatias verticais.
- Decidir a versão do PostgreSQL, a ferramenta de migração e a responsabilidade pelo schema por meio do ADR-002.
- Decidir o contrato de concorrência (ETag ou versão explícita) por meio do ADR-007.
- Implementar criação/leitura/atualização/desativação de categorias e produtos, invariantes de SKU/dinheiro/estoque, paginação/filtro/ordenação e histórico de produto.
- Persistir a intenção de evento de domínio pertencente ao Catálogo atomicamente com estado/histórico do produto; nenhum broker ou publicador de Integração será introduzido ainda.
- Introduzir o comportamento mínimo de auditoria append-only para alterações do catálogo sem construir a UI completa de auditoria.
- Adicionar migrações de banco, dados locais determinísticos e uma dependência PostgreSQL local reproduzível.
- Publicar OpenAPI executável para os endpoints implementados.
- Adicionar logs estruturados, métricas/detalhes de health e correlação proporcionais aos caminhos de catálogo/banco de dados.

### Fora de Escopo

- Autenticação/RBAC reais. Uma identidade de desenvolvimento claramente identificada pode apoiar testes locais, mas não é evidência de autenticação nem satisfaz critérios de aceite de autorização.
- Imagens de produtos, mensageria, integração externa e Centro de Controle de Qualidade.
- Frontend no navegador.
- Categorias hierárquicas, expansão para múltiplas moedas, carrinho/pedido/pagamento.
- Deploy em produção/alta disponibilidade.
- Exposição pública/externa, hospedagem de demonstração compartilhada ou qualquer ambiente alcançável além do limite de desenvolvimento local/isolado aprovado.

### Dependências

- Fase 01 concluída.
- ADR-002 e ADR-007 aprovados.
- Questões de cardinalidade de categoria, reutilização de SKU e moeda inicial resolvidas.
- Docker ou alternativa PostgreSQL local reproduzível aprovada por ADR.
- Um Gate de Exposição Externa aplicável que falhe quando este perfil anterior à autenticação estiver configurado para hospedagem pública/compartilhada.

### Entregáveis

- Domínio/módulo de Catálogo, limite público de aplicação/API e adaptador PostgreSQL.
- Migrações versionadas e mecanismo local de seed/factory.
- Contratos implementados de OpenAPI, erros, paginação e concorrência.
- Histórico do produto e fatos seguros de auditoria.
- Suítes de unidade, componente, integração com banco de dados e API do Catálogo.
- Documentação de execução/reset local e perfil de teste de banco de dados na CI.
- Aviso persistente de prévia local e proteção de configuração indicando que a mutação do catálogo não está completa quanto à autorização.

### Critérios de Aceite

1. Fluxos válidos de produto/categoria satisfazem o subconjunto aprovado de REQ-CAT-* e escritas inválidas são atômicas.
2. SKU normalizado duplicado, preço/estoque negativos, categoria inválida e atualização obsoleta são rejeitados de forma determinística.
3. Paginação repetida sobre dados inalterados é estável e o tamanho máximo de página é aplicado.
4. Alterações materiais criam histórico preciso com ator, tempo e correlação.
5. Restrições do banco protegem invariantes críticos e migrações funcionam a partir de um banco vazio.
6. Erros de API são seguros e compatíveis com o contrato; nenhuma entidade de persistência é exposta diretamente.
7. A configuração local e de CI é reproduzível e documentada.
8. O Gate de Exposição Externa comprova que o catálogo anterior à autenticação não pode ser configurado/implantado como ambiente externamente acessível.
9. As partes de autorização de REQ-CAT-* permanecem explicitamente pendentes; nenhum teste com identidade de desenvolvimento é relatado como evidência de RBAC.

### Testes Esperados

- Testes unitários/de propriedade/parametrizados para SKU, dinheiro, estoque, transições e mapeamentos.
- Testes de componente/API de caminho feliz, limites, validação, paginação, concorrência e erro.
- Testes de integração PostgreSQL para unicidade, transações, constraints e migrações.
- Verificações de qualidade de dados para histórico e rollback/ausência de estado parcial.
- Smoke básico de performance para regressões de paginação/consulta, ainda não como gate rígido de carga.
- Testes de segurança para mass assignment, entrada de injeção e exposição de erros mesmo antes da autenticação completa.
- Teste de configuração/arquitetura para o Gate de Exposição Externa; testes de autorização permanecem pendentes até a Fase 03.

### Riscos

- Uma API temporariamente não autenticada ser confundida com comportamento pronto para release.
- Inflação do modelo de dados além dos padrões de acesso atuais.
- Ambiguidade de moeda/categoria causar decisões incompatíveis de schema/API.
- Atrito com containers de teste/Docker local.

### Definição de Pronto

- Requisitos de catálogo aprovados e exemplos de contrato são rastreáveis a testes aprovados.
- Build, lint/análise estática, verificações de unidade/componente/integração/API/migração passam.
- Não há defeito de atualização perdida ou escrita parcial nos cenários cobertos.
- Revisões de ameaças e telemetria estão atualizadas para o comportamento de banco/API.
- OpenAPI, modelo de dados, arquitetura e documentação de execução local correspondem à implementação.
- Evidências e limitações da release v0.2 estão registradas; não há alegação de uso multiusuário seguro.
- Documentação e diagnósticos de UI/API identificam v0.2 como prévia local; a Fase 03 é um fechamento de segurança obrigatório antes da exposição.

## Fase 03 — Autenticação, RBAC e UI do Commerce (v0.3)

### Objetivo

Proteger operações de catálogo com identidade/RBAC reais e expor um fluxo mínimo e acessível do Commerce em React/TypeScript.

### Escopo

- Aceitar o ADR-006 para a arquitetura de autenticação/sessão.
- Implementar REQ-AUTH-* aprovados, bootstrap de usuários/roles e matriz de permissões do catálogo.
- Concluir todos os critérios de aceite de autorização deixados pendentes pela prévia local da Fase 02 e substituir o limite da identidade de desenvolvimento.
- Aplicar autorização de endpoint, objeto e estado; auditar ações relevantes para segurança.
- Criar um shell mínimo de frontend com login/gerenciamento de sessão e fluxos centrais de listagem/edição de produtos/categorias.
- Aplicar configuração segura do navegador, política de CSRF/CORS/armazenamento de token e fundamentos de acessibilidade.
- Adicionar estratégia de build/lint/unidade/componente do frontend e jornadas críticas limitadas em Playwright.

### Fora de Escopo

- Cadastro público, login social, federação complexa de identidade ou MFA, salvo inclusão explícita por ADR.
- Mídia, mensageria e Centro de Controle de Qualidade.
- Design system pixel-perfect ou ampla cobertura de permutações E2E.
- Alegações de recuperação de identidade/conformidade de produção.

### Dependências

- Fase 02 concluída e contratos de API estáveis.
- Matriz de roles/permissões, bootstrap de administrador e requisitos de desativação/sessão aprovados.
- ADR de autenticação e decisão de pacote/tooling do frontend.
- Baseline de navegadores suportados definida.

### Entregáveis

- Módulo de Auth, APIs de catálogo protegidas e alterações de migração.
- Experiência mínima do Commerce em React/TypeScript com formulários/navegação acessíveis.
- Eventos de auditoria de segurança e telemetria de autenticação/autorização.
- OpenAPI/modelo de ameaças atualizados e identidades de demonstração local com dados sintéticos inertes.
- Cobertura de unidade/componente/API/segurança/acessibilidade/Playwright.

### Critérios de Aceite

1. Usuários ativos autenticam e encerram a sessão conforme o modelo selecionado; sessões inválidas, desativadas ou revogadas falham com segurança.
2. Negações de permissão e de objeto/estado são aplicadas no servidor e auditadas sem vazar recursos.
3. Fluxos críticos da UI do catálogo são operáveis por teclado, rotulados e expõem estados de validação/erro de forma acessível.
4. O gerenciamento de sessão no navegador atende à política de cookie/token, CSRF, CORS e headers de segurança do ADR.
5. O bootstrap de administrador não distribui credencial padrão conhecida e é reproduzível localmente.
6. A ausência/ocultação de um controle na UI nunca é a única proteção de autorização.
7. O Gate de Exposição Externa permanece bloqueante até que autenticação/RBAC reais, bootstrap seguro e todas as verificações pendentes de autorização do Catálogo passem; somente então um ambiente hospedado poderá ser avaliado mediante aprovação separada.

### Testes Esperados

- Testes unitários para políticas de permissão/sessão.
- Matriz de API para casos anônimo, permitido, negado, desativado e sessão obsoleta.
- Testes administrativos de roles para criação/desativação, atribuição/remoção, proteção do último administrador e revogação de sessão após mudanças críticas de permissão.
- Testes de abuso/rate/generic-error de autenticação e de auditoria/redação.
- Testes de unidade/componente do frontend para formulários/estado/erros.
- Regras automatizadas de acessibilidade mais smoke manual por teclado/leitor de tela.
- Poucas jornadas Playwright para login, alteração permitida no catálogo e ação negada.

### Riscos

- Erros de armazenamento de token/CSRF e lacunas de autorização.
- Sobreposição de roles em uma equipe de uma pessoa ocultar limitações de segregação.
- Uso excessivo de testes de UI e locators frágeis.
- Divergência de contrato entre frontend e backend.

### Definição de Pronto

- Requisitos de Auth/RBAC e mitigações de ameaças passam com evidência de permissão/negação.
- Build/lint/testes são executados para backend e frontend; jornadas críticas Playwright são estáveis sem sleeps arbitrários.
- Nenhuma credencial/token vaza em logs, URL, código-fonte ou artefatos.
- Achados manuais/automatizados de acessibilidade são triados.
- Documentação de API/segurança/arquitetura/demonstração local está atualizada.
- O aviso de prévia insegura da Fase 02 só é retirado depois que o Gate de Exposição Externa comprovar os critérios de fechamento.

## Fase 04 — Ciclo de vida seguro de mídia (v0.4)

### Objetivo

Dar suporte a imagens de produtos por meio de um ciclo de processamento seguro, privado e observável.

### Escopo

- Aceitar o ADR-004 para MinIO/object storage e decidir o fluxo de upload.
- Implementar REQ-MED-* aprovados, metadados, estados, associações/ordenação e limpeza.
- Validar assinatura do conteúdo, tipo, limites de bytes/pixels/descompressão e autorização.
- Recodificar formatos aprovados em um processador restrito e remover metadados desnecessários.
- Implementar retry/reprocessamento e estado de falha visível.
- Adicionar configuração MinIO/local apenas nesta fase.

### Fora de Escopo

- Vídeos/documentos/SVG, CDN pública e serviço de análise de malware em produção.
- Transformações arbitrárias ou definidas pelo usuário.
- Busca/gerenciamento de ativos de mídia além das necessidades de produto.
- RabbitMQ, salvo se evidências de processamento mostrarem a necessidade atual de um broker e um ADR o aprovar; primeiro deve-se preferir um modelo de execução simples e recuperável.

### Dependências

- Autenticação/RBAC da Fase 03 e limites de Produto.
- Regras aprovadas de formato/tamanho/dimensão/retenção.
- ADR de MinIO e abordagem selecionada de biblioteca segura/isolamento de processo para imagens.

### Entregáveis

- Módulo de Mídia e fluxo seguro de API/UI.
- Perfil privado de object storage, migração de metadados e reconciliação/limpeza.
- Telemetria de status/retry do processamento e resultados visíveis ao usuário.
- Cobertura de arquivo hostil, integração com armazenamento, resiliência e acessibilidade.
- Modelo de ameaças e limites operacionais atualizados.

### Critérios de Aceite

1. Imagens válidas e suportadas tornam-se prontas com variantes/checksum/metadados seguros corretos.
2. Divergência entre extensão/MIME, excesso de tamanho/risco de descompressão e upload/recuperação não autorizados são rejeitados com segurança.
3. Chaves/credenciais internas e objetos pendentes/com falha não são expostos publicamente.
4. Falha do processador/armazenamento produz estado recuperável/terminal visível sem corromper dados do produto.
5. Exclusão/reordenação/retry são autorizados, seguros para concorrência e auditados.
6. A reconciliação detecta os casos previstos de itens órfãos/travados sem ambiguidade destrutiva.

### Testes Esperados

- Testes unitários para políticas de tipo/estado/limite.
- Testes de API para autorização, validação, ordenação e status.
- Integração com MinIO para armazenamento/recuperação/limpeza/indisponibilidade.
- Fixtures hostis: assinatura divergente, arquivo truncado, candidatos polyglot e limites de descompressão/dimensão usando amostras inertes e seguras.
- Testes de timeout/falha/retry do processador e reconciliação de órfãos.
- Acessibilidade da UI e jornada Playwright focada em mídia.

### Riscos

- Vulnerabilidades do decoder/exaustão de recursos.
- Objetos órfãos e erros irreversíveis de limpeza.
- Exposição de URL assinada ou bucket.
- Introdução prematura de mensageria.

### Definição de Pronto

- Requisitos de mídia, controles de ameaça e cenários de recuperação passam.
- Não existe formato ativo não suportado nem objeto público por padrão.
- Verificações de build/estática/segurança/dependência e testes em camadas passam.
- Limites, ciclo de vida, limpeza, armazenamento e documentação de execução local estão atualizados.
- Evidência diagnóstica identifica estágio/falha da imagem sem vazar dados do arquivo.

## Fase 05 — Integração externa confiável (v0.5)

### Objetivo

Sincronizar alterações confirmadas do catálogo com um Mock do Centro de Vendas Externo controlado sem perder alterações nem duplicar efeitos de negócio.

### Escopo

- Finalizar envelope/schema de eventos e aceitar o ADR-003 para topologia/retry/DLQ do RabbitMQ.
- Usar a intenção transacional de outbox e a porta de publicação pertencentes ao Catálogo; implementar publicador, tratamento de entrega, worker e contrato do mock pertencentes à Integração sem acesso direto às tabelas do Catálogo.
- Implementar idempotência, retry/backoff limitados, timeouts, falha terminal, status, reconciliação e replay autorizado.
- Propagar contexto de correlação/trace e expor health de fila/outbox/entrega.
- Adicionar dependências RabbitMQ/mock local apenas agora.

### Fora de Escopo

- Provedor real de vendas, Kafka, alegações de exactly once ou extração em microservices.
- Plataforma genérica de integração, workflow engine ou roteamento arbitrário de mensagens.
- Replay automático de falhas permanentes/desconhecidas.

### Dependências

- Modelo estável de commit/histórico do catálogo e permissões de Auth.
- Revisão de minimização/compatibilidade do payload de evento.
- ADR do RabbitMQ, contrato do mock e taxonomia de retry/erros.
- Permissão de operador e comportamento de auditoria de replay definidos.

### Entregáveis

- Contratos versionados de evento do catálogo e HTTP downstream.
- Integração pela porta de publicação do Catálogo, mais implementação do publicador/worker/mock; o Catálogo permanece proprietário e escritor da intenção e a Integração é responsável por publicação/entrega.
- Capacidade de status/replay da integração e telemetria segura.
- Adições de broker/mock ao Docker Compose e runbook.
- Suítes de contrato/integração/reinício/idempotência/resiliência.

### Critérios de Aceite

1. Uma alteração elegível e confirmada do catálogo chega eventualmente a um mock saudável e é rastreável ponta a ponta pela porta de publicação do Catálogo.
2. Alterações revertidas não produzem eventos confirmados; alterações confirmadas sobrevivem à indisponibilidade de processo/broker.
3. Publicação/entrega/replay duplicados não duplicam o efeito de negócio downstream.
4. Falha transitória faz retry conforme a política; falha permanente/esgotada é retida e visível.
5. Escritas no catálogo continuam disponíveis durante indisponibilidade downstream quando a capacidade da outbox é segura.
6. Replay é autorizado/auditado e preserva identidade/idempotência.

### Testes Esperados

- Testes transacionais de banco para atomicidade de catálogo/histórico/outbox.
- Testes de arquitetura que comprovem que Integração não acessa diretamente repositórios/tabelas do Catálogo.
- Testes de janela de falha do publicador e de claim/reinício.
- Testes de integração para duplicação/redelivery/backlog/indisponibilidade do broker.
- Testes de contrato de provedor/consumidor incluindo erros e incompatibilidade de versão.
- Testes de timeout/retry/backoff/idempotência/DLQ/replay do worker.
- Experimentos de falha para downstream indisponível/lento e recuperação.

### Riscos

- Perda por escrita dupla, efeito duplicado e loops de mensagens problemáticas.
- Tempestades de retry ou crescimento ilimitado de fila/recursos.
- Mock e adaptador concordarem com contrato incorreto e não documentado.
- Alegações excessivas de independência de serviço/semântica exactly once.

### Definição de Pronto

- Os critérios de REQ-INT-* e cenários de falha passam com evidência retida.
- Não há perda silenciosa nem efeito de negócio duplicado nas janelas de falha/retry testadas.
- Contrato/versão, topologia, runbook e documentação de recuperação estão atualizados.
- Revisão de segurança/privilégio mínimo para broker/replay e dashboards de telemetria passam.
- A fase permanece uma base de código de Monólito Modular mesmo que o worker execute separadamente.

## Fase 06 — Evidências de qualidade e Centro de Controle (v0.6)

### Objetivo

Construir rastreabilidade confiável de release/teste/evidência e uma UI acessível usando fontes reais do AEGIS antes de calcular uma Pontuação de Qualidade.

### Escopo

- Implementar releases/candidatos, suítes/casos, execuções/resultados, referências de evidência, defeitos, achados de segurança e resultados de performance para REQ-QLT-001 a REQ-QLT-008.
- Implementar avaliação independente e versionada de gates críticos (REQ-QLT-011) e o resumo completo de evidências do candidato (REQ-QLT-012).
- Adicionar APIs de ingestão com escopo, autenticação, versionamento de schema e idempotência.
- Atribuir toda evidência ao commit/build/release/ambiente exatos e mostrar atualização/lacunas.
- Implementar navegação bidirecional entre requisito, caso, resultado, evidência, defeito e release.
- Criar visões iniciais do Centro de Controle de Qualidade para resumo de release e status das fontes.
- Integrar saídas reais das ferramentas existentes de teste/segurança/performance do AEGIS conforme existirem.

### Fora de Escopo

- Pontuação de Qualidade, classificação de risco, recomendação do Motor e fluxo de decisão final. Esses itens são explicitamente NOT_APPLICABLE, não zero/aprovado.
- Gerenciamento genérico de testes ou integrações com todos os trackers externos.
- Copiar todos os logs/artefatos brutos da CI para o PostgreSQL.
- Dados fabricados somente para preencher dashboards (fixtures de demonstração devem ser identificadas).

### Dependências

- Identificadores confiáveis existentes de build/teste e seus relatórios.
- Decisões aprovadas de schema de ingestão, atomicidade/limites de lote, identidade da fonte e retenção de evidência.
- Convenções de IDs de requisito/teste e proveniência de artefatos de build.
- Fundação de UI acessível da Fase 03.

### Entregáveis

- Dados/API/UI do módulo de Qualidade para evidências e rastreabilidade.
- Adaptadores/contratos de ingestão para fontes aprovadas.
- Implementação de acesso/redação/retenção de evidências.
- Telemetria de atualização, rejeição e duplicação de fontes.
- Suítes de qualidade de dados, API, segurança, acessibilidade e E2E.

### Critérios de Aceite

1. A ingestão duplicada de execução de uma fonte é idempotente; entrada conflitante/malformada/com schema não suportado é rejeitada de forma visível.
2. Uma visão de release distingue evidência aprovada, reprovada, ignorada, bloqueada, com erro, com retry e ausente/desatualizada.
3. Um revisor consegue percorrer a cadeia de rastreabilidade exigida e ver lacunas em vez de links inferidos.
4. Todo resultado pode ser atribuído ao build/ambiente/fonte exatos e a evidência segura.
5. Artefatos sensíveis brutos exigem acesso mais restrito que o resumo agregado.
6. Agregações de dashboard correspondem a fixtures de fonte verificadas independentemente e nunca convertem desconhecido em aprovação.
7. O resumo de evidências e os gates críticos funcionam para um candidato/build exato enquanto pontuação, risco e recomendação do Motor permanecem NOT_APPLICABLE auditados sem sugerir aprovação.

### Testes Esperados

- Testes unitários para políticas de normalização, agregação, status e atualização.
- Testes de API para schema/versão/autorização/tamanho/idempotência/comportamento em lote.
- Testes de integração com banco de dados e reconciliação de qualidade de dados.
- Testes de segurança para fonte forjada, atribuição entre releases, URL/acesso de evidência e abuso de payload.
- Exploração manual/acessibilidade das visões de release/rastreabilidade.
- Jornada crítica Playwright de rastreabilidade.

### Riscos

- Proveniência não confiável gerar falsa confiança.
- Armazenar evidência demais, sensível ou de alto volume.
- Bugs de agregação do dashboard ou status ambíguos.
- Construir um gerenciador genérico de testes em vez de atender às necessidades de release do AEGIS.

### Definição de Pronto

- Requisitos aprovados de evidência de qualidade passam com fidelidade conhecida da fonte.
- Agregações são verificadas independentemente contra fixtures representativas.
- Controles de segurança/acesso/retenção e observabilidade passam.
- Caminhos críticos da UI atendem às verificações de acessibilidade.
- A saída do produto retorna pontuação, risco e recomendação do Motor apenas como NOT_APPLICABLE governado; não exibe pontuação numérica sintética.
- O conjunto de capacidades resultante pode ser avaliado no checkpoint da MPR sem alegar a Visão Completa v1.0.

## Fase 07 — Motor de Qualidade e decisões de release (v0.7)

### Objetivo

Produzir avaliações transparentes de gates críticos, pontuação experimental, classificação de risco e recomendação, preservando a autoridade humana sobre a decisão.

### Escopo

- Implementar política/avaliações versionadas de gates e regras de bloqueio crítico.
- Implementar REQ-QLT-009, REQ-QLT-010 e REQ-QLT-015: calibrar dimensões da pontuação, política de risco, limite de recomendação e comportamento para dados ausentes.
- Implementar classificação de risco versionada e recomendação explicável.
- Registrar decisões finais humanas autorizadas e exceções permitidas com prazo.
- Exibir versão de fórmula/política, contribuições, bloqueios, corte de evidência e justificativa.
- Adicionar cenários adversariais projetados para revelar agregações enganosas.

### Fora de Escopo

- Decisões opacas de release por ML/IA, deploy automático ou bypass de regras críticas baseado em pontuação.
- Reescrita retroativa de avaliações/decisões históricas.
- Alegar precisão preditiva sem validação representativa.

### Dependências

- Evidência confiável/atual da Fase 06.
- ADR-008 aprovado para fórmula/gates, mapa de requisitos críticos, limites e autoridade sobre exceções.
- Cenários representativos de release para calibração.

### Entregáveis

- Motor de Qualidade determinístico e versionado e registros de avaliação.
- UI/API de gates críticos, risco, recomendação e decisão final.
- Conjunto de dados de calibração/adversarial e relatório de avaliação.
- Auditoria/telemetria para alterações de política, erros de avaliação e decisões.
- Governança de Gates de Qualidade e documentação de operação atualizadas.

### Critérios de Aceite

1. As mesmas entradas imutáveis e versão de política produzem a mesma pontuação/risco/recomendação.
2. Qualquer cenário de bloqueio crítico recomenda BLOCK independentemente da pontuação.
3. Pontuação abaixo de versioned_block_threshold recomenda BLOCK; nenhuma pontuação alta altera falha de gate crítico.
4. Entrada ausente/desatualizada/com erro produz comportamento explícito de insuficiência/desconhecido, nunca aprovação.
5. Toda pontuação expõe contribuições/pesos/fórmula e todo risco/recomendação expõe motivos.
6. Somente humanos autorizados registram decisões finais/exceções com justificativa imutável e expiração.
7. Atualizações de política não alteram resultados históricos de avaliação.

### Testes Esperados

- Testes unitários/de propriedade/tabela de decisão para fórmulas, limites, arredondamento e bloqueios críticos.
- Testes de mutação/adversariais que tentam compensar falha crítica com grande volume de aprovações de baixo risco.
- Testes de integração de dados para corte/versão/histórico de evidências.
- Testes de API/RBAC/auditoria para políticas e decisões.
- Acessibilidade da UI e jornada Playwright de revisão da decisão.
- Revisão de calibração contra releases representativas e resultados conhecidos.

### Riscos

- Falsa precisão, incentivos perversos e dimensões contadas em duplicidade.
- Adulteração de política ou exceção não autorizada.
- Evidência desatualizada e apresentação de pontuação enganarem usuários.
- Dados de calibração insuficientes para justificar pesos.

### Definição de Pronto

- Regras críticas e explicabilidade passam por revisão adversarial.
- A fórmula permanece identificada como experimental se a validação for insuficiente.
- Verificações de segurança/auditoria/acesso e histórico imutável passam.
- A documentação declara claramente limitações/premissas residuais.
- A aprovação humana permanece separada da recomendação.

## Fase 08 — Endurecimento de segurança e performance (v0.8)

### Objetivo

Estabelecer garantia reproduzível e orientada a risco de segurança e performance sobre o produto implementado.

### Escopo

- Atualizar o modelo de ameaças e executar testes manuais direcionados às superfícies de maior risco.
- Amadurecer varreduras de código-fonte/dependências/secrets/artefatos e o fluxo de triagem.
- Criar workload/dataset/ambiente de referência no k6 e baselines.
- Ajustar/indexar/corrigir gargalos medidos sem alterar a semântica.
- Calibrar gates de performance/segurança e fornecer seus dados ao Centro de Controle de Qualidade.
- Exercitar limites de abuso/recursos para login, busca, upload, ingestão e replay.

### Fora de Escopo

- Certificação formal de conformidade, alegações públicas de pentest ou promessas universais de escala.
- Ajustes sem medição, testes de carga em produção sem autorização ou ocultação de erros funcionais como ruído de performance.
- Kubernetes/auto-scaling como resposta padrão a performance.

### Dependências

- Jornadas críticas estáveis do produto e dataset representativo.
- Perfil definido de ambiente/hardware suportado e modelo de workload.
- Política de triagem de severidade/explorabilidade dos achados e responsáveis definidos.

### Entregáveis

- Modelo de ameaças/relatório de testes de segurança atualizados e evidência de correção.
- Cenários k6 versionados, gerador de dataset e relatório baseline.
- Política calibrada de limites/gates com critérios de validade do ambiente.
- Dashboards de segurança/performance e ingestão no Centro de Controle de Qualidade.
- Runbooks para investigação de regressão.

### Critérios de Aceite

1. Workloads de referência são reproduzíveis e identificam build/ambiente/recursos.
2. Endpoints críticos atendem às metas aprovadas de percentil/erro/recuperação ou os riscos são bloqueados.
3. Não existe achado crítico explorável confirmado e não resolvido.
4. Controles de recursos/abuso de autenticação/upload/busca/ingestão falham com segurança nos cenários aprovados.
5. Alterações de performance preservam correção/integridade de dados e demonstram melhoria medida.
6. Relatórios distinguem invalidade de ferramenta/ambiente de resultado do produto.

### Testes Esperados

- Smoke/baseline/load no k6 e stress/soak selecionados somente quando justificados.
- Testes manuais e automatizados de segurança para autenticação/RBAC/upload/API/ingestão.
- Varreduras de código estático/dependências/secrets/artefatos com triagem e novo teste.
- Testes de regressão/integração em torno das otimizações de performance.
- Verificações de resiliência/recuperação durante carga quando seguras.

### Riscos

- Ambiente de performance ruidoso ou não representativo.
- Falsos positivos/negativos de scanners e segurança tratada como checklist.
- Otimizações que enfraqueçam validação, auditoria ou consistência.
- Métricas usadas como alegações de marketing além das evidências.

### Definição de Pronto

- Baselines reproduzíveis e limitações exatas estão documentadas.
- Gates críticos aplicáveis de segurança/performance passam com evidências atuais.
- Otimizações mantêm cobertura de regressão funcional/de dados/segurança.
- Achados e limites remanescentes têm justificativa/responsável explícitos.
- O Centro de Controle de Qualidade representa corretamente resultados e atualização das fontes.

## Fase 09 — Observabilidade completa e Laboratório de Falhas (v0.9)

### Objetivo

Demonstrar que falhas críticas podem ser injetadas, observadas, investigadas e recuperadas com segurança em ambientes não produtivos.

### Escopo

- Aceitar a decisão de backend/topologia de observabilidade e adicionar os componentes OpenTelemetry, Prometheus e Grafana necessários localmente.
- Implementar logs/métricas/traces críticos, dashboards, alertas e runbooks de [OBSERVABILITY.md](docs/OBSERVABILITY.md).
- Implementar autorização do Laboratório de Falhas, negação por ambiente, cenários permitidos, TTL e parada de emergência.
- Cobrir incrementalmente downstream indisponível/timeout, latência/500 da API, latência de banco, falha de imagem e interrupção da fila.
- Vincular hipótese do experimento, execução de teste, trace/evidência e conclusão no Centro de Controle de Qualidade.

### Fora de Escopo

- Injeção de falhas em produção, scripts/URLs/SQL arbitrários, plataforma de chaos engineering ou alegações de SRE 24/7.
- Instrumentar cada método ou reter telemetria sem limite.
- Alertar sobre todo erro sem possibilidade de ação.

### Dependências

- Fluxos críticos maduros, correlação e fundamentos de telemetria existentes.
- Controles seguros de falha, durações/intensidades máximas e identidade do ambiente aprovados.
- Decisão de armazenamento/retenção/custo de observabilidade e orçamento de recursos locais.

### Entregáveis

- Perfil/stack local de telemetria, instrumentação crítica e dashboards/alertas versionados.
- Controles/UI/API do Laboratório de Falhas com estado ativo inequívoco.
- Catálogo de experimentos, procedimentos de abortar/recuperar e runbooks de investigação.
- Evidências de resiliência, segurança, telemetria e recuperação no Centro de Controle de Qualidade.

### Critérios de Aceite

1. A configuração de produção impede independentemente a ativação do Laboratório de Falhas; o padrão é desligado em todos os ambientes.
2. Somente cenários permitidos e autorizados executam dentro de escopo/TTL, e a parada de emergência é idempotente/auditada.
3. Cada cenário demonstra comportamento degradado esperado, blast radius limitado e recuperação sem perda silenciosa de dados/mensagens.
4. A equipe de QA consegue navegar da execução/correlação com falha ao limite responsável usando telemetria segura.
5. Dashboards evitam labels sensíveis/de alta cardinalidade e alertas possuem runbooks acionáveis.
6. Indisponibilidade da telemetria não bloqueia o trabalho da aplicação nem fabrica evidência de sucesso.

### Testes Esperados

- Testes de segurança de permissão/ambiente/parâmetro/TTL/parada de emergência.
- Propagação de trace entre HTTP/banco/outbox/broker/worker/downstream.
- Classificação/cardinalidade de métricas e testes de redação de secrets em logs/traces.
- Health checks durante falha/recuperação de dependências.
- Experimentos de falha com hipótese, condição de aborto, sinais observados e reconciliação de integridade de dados.
- Exercício de regra de alerta/runbook usando cenários controlados.

### Riscos

- Escape/persistência da falha e blast radius inseguro.
- Complexidade de recursos da stack local reduzir a reprodutibilidade.
- Vazamento de telemetria, alta cardinalidade ou lacunas de amostragem.
- Testes acoplados ao texto exato de logs em vez do comportamento do produto.

### Definição de Pronto

- Controles de segurança e negação em produção passam por revisão independente.
- Cada cenário implementado tem evidência repetível de resultado/recuperação.
- O walkthrough de investigação da equipe de QA alcança o diagnóstico esperado sem adivinhação privilegiada no banco de dados.
- Segurança/cardinalidade/retenção da telemetria e necessidades de recursos locais estão documentadas.
- Não existe risco crítico não resolvido no Laboratório de Falhas ou na observabilidade.

## Fase 10 — Release de portfólio v1.0 — Visão Completa

### Objetivo

Consolidar o AEGIS em uma release de portfólio coerente, reproduzível e honestamente delimitada da Visão Completa, preservando o núcleo MPR demonstrável separadamente.

### Escopo

- Concluir ou redefinir explicitamente o escopo dos requisitos Must da v1.0 e das lacunas de rastreabilidade.
- Refinar as jornadas críticas acessíveis do Commerce e do Centro de Controle de Qualidade.
- Estabilizar bootstrap local, demonstração determinística, gates de CI e identidade de artefatos.
- Executar o plano final de evidências funcionais, de integração, segurança, performance, resiliência, acessibilidade e qualidade de dados.
- Revisar toda a documentação/ADRs, pontuação/gates/risco e limitações residuais.
- Registrar uma decisão humana para a release v1.0.

### Fora de Escopo

- Fingir que a operação local de portfólio constitui SLA público/certificação de conformidade.
- Nova capacidade importante, migração de infraestrutura ou reescrita arquitetural.
- Ocultar defeitos, testes instáveis, lacunas ou risco aberto para apresentação.
- Commit/push/deploy sem autorização explícita e separada.

### Dependências

- Marcos aprovados anteriores concluídos ou retirados do escopo explicitamente com documentação.
- Checkpoint de capacidade MPR demonstrado ou lacuna remanescente resolvida explicitamente antes do refinamento da Visão Completa.
- Escopo/build do release candidate congelado e política de evidências pronta.
- Decisão do alvo de demonstração: somente local e reproduzível ou demonstração hospedada avaliada separadamente.

### Entregáveis

- Candidato v1.0 pronto para tag (sem criar tag automaticamente) e inventário imutável de evidências.
- Demonstração roteirizada e acessível e walkthrough de investigação de falhas.
- Configuração/remoção local reproduzível e documentação de troubleshooting.
- Relatórios finais de rastreabilidade, segurança, performance, resiliência e observabilidade.
- Avaliação de qualidade, risco/recomendação e decisão final autorizada.
- Narrativa de portfólio vinculando decisões de engenharia a evidências e limitações.

### Critérios de Aceite

1. Um novo revisor consegue seguir os pré-requisitos documentados para executar o produto e a validação central no ambiente declarado.
2. Jornadas críticas de Commerce, Centro de Controle de Qualidade, integração e investigação de falhas comportam-se como documentado.
3. Gates exigidos usam evidências atuais do candidato exato; nenhum bloqueio crítico permanece.
4. Requisitos Must estão implementados e rastreados ou removidos explicitamente do escopo v1.0 com aprovação.
5. Alegações de segurança/performance/acessibilidade declaram o escopo/ambiente testado e não generalizam além dele.
6. A arquitetura não contém tecnologia proibida injustificada nem decisão crítica não documentada.
7. A decisão final distingue pontuação, risco, recomendação, exceções e aprovação humana.

### Testes Esperados

- Suíte completa aprovada da release em unidade/componente/integração/API/contrato/E2E.
- Varredura de segurança mais novo teste manual direcionado aos riscos altos.
- Cenários k6 de referência e comparação de regressão.
- Experimentos selecionados de resiliência/recuperação no Laboratório de Falhas.
- Revisão automatizada/manual de acessibilidade dos fluxos críticos.
- Verificações de reconciliação/migração/rastreabilidade/atualização das evidências.
- Ensaio de reprodutibilidade local/máquina limpa.

### Riscos

- Refinamento da demonstração deslocar a correção de riscos.
- Divergência de ambiente e evidências desatualizadas.
- Falhas instáveis/de infraestrutura serem ocultadas perto da release.
- Alegações excederem o escopo real de implantação/teste.
- Inconsistência documental após a evolução acumulada.

### Definição de Pronto

- O Gate de Release passa sob a política versionada atual sem bloqueio crítico não substituível.
- Toda evidência é atribuível, atual e revisada; exceções são explícitas e válidas.
- A reprodutibilidade limpa e o walkthrough de demonstração/investigação são bem-sucedidos.
- Limitações de segurança, performance, acessibilidade, dados e operação estão documentadas.
- O diff completo/estado do repositório, links da documentação e índice de ADRs foram revisados.
- Uma pessoa autorizada registra a decisão de release; commit, tag, push ou deploy ocorre somente mediante solicitação explícita separada.

## Próxima aprovação recomendada

Revisar o diff, as evidências locais e o resultado da CI da **Fase 01** antes de autorizar commit, push, merge ou qualquer planejamento de implementação da Fase 02. Catálogo, banco de dados e frontend permanecem fora do escopo e exigem aprovação humana separada.
