# Constituição Operacional do AEGIS

## Autoridade e escopo

Este arquivo rege agentes humanos e automatizados que trabalhem em qualquer parte do repositório AEGIS. Instruções mais específicas podem acrescentar restrições, mas não podem enfraquecer esta constituição, a política de segurança, os requisitos ou os Gates de Qualidade. Quando houver conflito entre instruções ou o escopo não estiver claro, pare e solicite orientação humana.

O repositório é intencionalmente incremental. Uma entrada no roadmap não autoriza sua implementação. Trabalhe apenas na fase ou tarefa explicitamente aprovada.

## Política de Idioma

- O idioma humano oficial do projeto é português do Brasil (PT-BR).
- Documentação, explicações, critérios, riscos, recomendações e relatórios de agentes devem ser escritos em PT-BR.
- Agentes que trabalhem no AEGIS devem apresentar seus relatórios do projeto em PT-BR.
- Código-fonte deve usar preferencialmente identificadores técnicos em inglês quando isso seguir as convenções profissionais e do ecossistema, por exemplo `ProductService`, `ProductRepository`, `CorrelationIdFilter` e `QualityGate`.
- Nomes oficiais de tecnologias, bibliotecas e ferramentas devem ser preservados.
- Protocolos, formatos, content-types, headers HTTP, comandos, caminhos e identificadores técnicos não devem ser traduzidos.
- Comentários e documentação explicativa do código podem ser escritos em português quando forem realmente necessários; comentários óbvios devem ser evitados independentemente do idioma.
- Toda nova documentação deve obedecer a esta política.

Essa combinação preserva as convenções do ecossistema, a legibilidade internacional, o alinhamento com a documentação das bibliotecas, a manutenção e o valor profissional do portfólio.

## Obrigações centrais

- Preservar o AEGIS como um produto com Engenharia de Qualidade incorporada, não como uma vitrine de scripts de teste.
- Preferir o design mais simples que satisfaça os requisitos aprovados.
- Manter os limites do Monólito Modular; complexidade distribuída exige um problema concreto e um ADR aprovado.
- Nunca mascarar erros, fabricar evidências nem afirmar que uma capacidade não implementada existe.
- Tratar segurança, testabilidade, observabilidade, integridade de dados e acessibilidade como preocupações de design.
- Preservar o trabalho do usuário e alterações não relacionadas; não realizar operações destrutivas no Git ou no sistema de arquivos sem autorização explícita.
- Manter secrets, credenciais e dados pessoais/sensíveis fora do código-fonte, logs, exemplos e artefatos.

## Antes de alterar qualquer coisa

Um agente deve:

1. Confirmar a raiz do repositório, o estado atual do working tree e o escopo da tarefa.
2. Ler este arquivo, a fase relevante em [PLANS.md](PLANS.md) e os documentos aplicáveis em `docs/`.
3. Identificar os IDs de requisitos e critérios de aceite afetados; não inventar comportamentos que os contradigam.
4. Consultar [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) e os [ADRs](docs/ADR/README.md) relevantes para conhecer limites e decisões.
5. Avaliar o impacto em Engenharia de Qualidade: camadas de teste, dados, evidências, rastreabilidade, regressões e diagnóstico de falhas.
6. Avaliar o impacto em segurança: autenticação/autorização, validação, secrets, dados sensíveis, abuso e cadeia de suprimentos.
7. Avaliar, quando aplicável, impactos em dados/migração, compatibilidade de API/eventos, observabilidade e execução local.
8. Inspecionar código, testes e configurações existentes antes de propor um padrão. Não presumir que uma arquitetura representada apenas por placeholders esteja implementada.
9. Consultar uma pessoa antes de expandir materialmente o escopo, executar uma ação destrutiva, causar efeito público/externo ou tomar uma decisão difícil de reverter.

Para trabalhos exclusivamente documentais, as verificações “aplicáveis” ainda incluem consistência entre documentos e links relativos; verificações executáveis não devem ser fabricadas.

## Durante uma alteração

- Fazer alterações pequenas, coesas, revisáveis e vinculadas ao escopo aprovado.
- Seguir as convenções e responsabilidades existentes dos módulos; evitar acesso direto aos detalhes internos de persistência de outro módulo.
- Manter políticas de negócio separadas de detalhes de framework/E/S e expor limites controláveis para testes.
- Validar entradas nos limites de confiança e autorizar ações protegidas no servidor.
- Usar contratos de erro seguros; nunca expor stack traces, secrets ou payloads sensíveis.
- Preservar compatibilidade retroativa ou documentar/versionar uma quebra intencional.
- Tornar o comportamento assíncrono idempotente, limitar retry e deixar o estado de falha visível.
- Adicionar logs/métricas/traces de forma intencional, com correlação e redação; evitar métricas de alta cardinalidade.
- Atualizar testes junto com mudanças de comportamento na camada eficaz mais baixa, além da cobertura necessária de integração/usuário.
- Manter os resultados dos testes determinísticos. Não adicionar sleeps, captura com descarte de erros nem retry global sem controle.
- Não adicionar tecnologia, abstração, tabela, módulo ou serviço sem requisito/caso de uso.
- Registrar decisões difíceis de reverter em ADRs; não decidir silenciosamente apenas por meio do código.
- Nunca editar dependências geradas nem saída de build como se fossem código-fonte.

## Regra crítica de integridade dos testes

Testes não devem ser excluídos, ignorados, pulados, colocados em quarentena, submetidos a retry amplo, enfraquecidos nem ter asserções removidas apenas para obter um pipeline verde.

Nunca modifique um teste somente para fazê-lo passar. Investigue se a causa é:

- um defeito do produto;
- uma alteração legítima de requisito/contrato;
- uma expectativa de teste incorreta ou obsoleta;
- não determinismo na implementação ou nos dados do teste;
- falha de ambiente/ferramenta.

Uma alteração legítima de teste deve explicar qual causa se aplica e preservar ou melhorar a cobertura de risco. A quarentena segue [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md#política-de-testes-instáveis), exige responsável e prazo de expiração e nunca conta como aprovação.

## Atalhos proibidos

Sem requisito explícito, evidência e ADR aprovado, não introduza Kubernetes, Kafka, service mesh, event sourcing, CQRS, blockchain, Elasticsearch ou microservices.

Também não:

- desabilite validação, autorização, TLS/verificações de segurança ou Gates de Qualidade para simplificar o desenvolvimento;
- use secrets reais ou dados pessoais/de produção em testes/demonstrações;
- torne o Laboratório de Falhas acessível em produção;
- trate testes apenas com mocks como prova de que o comportamento real de persistência, mensageria ou armazenamento funciona;
- interprete evidências de qualidade ausentes ou desatualizadas como aprovação;
- permita que a Pontuação de Qualidade sobreponha uma regra de bloqueio crítico;
- afirme semântica “exactly once” entre limites distribuídos;
- realize refatorações oportunistas e não relacionadas durante uma tarefa focada;
- faça commit, push, deploy, publicação ou contato com sistemas externos, salvo solicitação explícita.

## Depois de uma alteração

Um agente deve:

1. Executar o menor conjunto completo de validações aplicáveis e, em seguida, verificações mais amplas proporcionais ao risco.
2. Revisar o diff completo em busca de edições não intencionais, arquivos gerados, secrets e expansão de escopo.
3. Confirmar que erros/falhas foram resolvidos, não ocultados.
4. Verificar a rastreabilidade entre requisitos, testes e evidências onde implementada.
5. Reavaliar autorização, validação, integridade de dados, compatibilidade retroativa e modos de falha.
6. Reavaliar telemetria, correlação, redação, health e comportamento operacional quando relevantes.
7. Atualizar documentação, exemplos, ADRs e planos quando o comportamento ou as decisões mudarem.
8. Relatar as validações realmente executadas, resultados, verificações não executadas, premissas, riscos e trabalhos posteriores.
9. Não fazer commit nem push, salvo solicitação humana explícita.

<a id="definition-of-done"></a>

## Definição de Pronto (Definition of Done)

Uma alteração está pronta somente quando todas as condições aplicáveis forem atendidas:

### Escopo e correção

- O objetivo aprovado e os critérios de aceite são satisfeitos sem expansão não relacionada.
- Os IDs de requisitos e regras de domínio estão refletidos na implementação e nos testes.
- Os comportamentos de limite, erro, concorrência e falha são considerados, não apenas o caminho feliz.

### Build e qualidade estática

- O build e a verificação de tipos são concluídos com sucesso.
- A formatação e o lint/análise estática são concluídos com sucesso.
- Nenhuma dependência injustificada, supressão de aviso ou ruído gerado é introduzido.

### Testes e evidências

- As verificações apropriadas de unidade, componente, integração, API, contrato, E2E e não funcionais passam, quando aplicáveis.
- Riscos novos ou alterados têm cobertura na camada eficaz mais baixa.
- Os dados de teste são isolados e nenhuma falha é mascarada por retry, skip ou asserção enfraquecida.
- As evidências relevantes de execução identificam build/ambiente e permitem diagnosticar falhas.

### Segurança

- As considerações sobre autenticação, autorização, validação, secrets, abuso e dependências foram revisadas.
- As verificações de segurança aplicáveis passam e não existe bloqueio crítico não resolvido.
- Dados sensíveis estão ausentes de logs, erros, evidências e código-fonte.

### Documentação e decisões

- A documentação de API, eventos, dados e comportamento está atualizada.
- Trade-offs importantes estão registrados em um ADR.
- Links relativos e terminologia entre documentos permanecem consistentes.

### Observabilidade e operações

- Novos resultados e falhas críticos têm telemetria estruturada e correlação proporcionais.
- Health, retry/recuperação, alertas/runbooks e execução local estão atualizados quando aplicáveis.
- Nenhuma telemetria sensível ou de alta cardinalidade é introduzida.

### Revisão

- O diff completo e o estado do repositório foram revisados.
- Todas as validações executadas e não executadas, riscos abertos e limitações são relatados com honestidade.

Para alterações exclusivamente documentais, itens executáveis de build/lint/testes são `não aplicáveis`, mas a revisão de links, consistência, escopo e diff é obrigatória.

## Modelo de relatório de alteração

Use uma entrega concisa:

```text
Escopo:
Requisitos:
Arquivos alterados:
Validações executadas:
Validações não executadas e motivo:
Impacto em segurança/observabilidade:
Riscos e questões em aberto:
```

Nunca informe que uma verificação passou se ela não foi executada.

## Autoridade da documentação

- [docs/PROJECT.md](docs/PROJECT.md): intenção e escopo do produto.
- [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md): baseline de comportamento e aceite.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): limites e regras de dependência.
- [docs/API_SPEC.md](docs/API_SPEC.md) e [docs/DATA_MODEL.md](docs/DATA_MODEL.md): contratos/modelos conceituais.
- [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/QUALITY_GATES.md](docs/QUALITY_GATES.md) e [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md): políticas transversais.
- [docs/ROADMAP.md](docs/ROADMAP.md): direção revisável.
- [PLANS.md](PLANS.md): fases candidatas a trabalho aprovado.
- ADRs aceitos: razões e consequências das decisões arquiteturais. Um ADR substituto deve declarar o que substitui.

Quando documentos divergirem, não selecione a regra mais conveniente. Identifique o conflito, avalie o risco e solicite/registre uma resolução deliberada.
