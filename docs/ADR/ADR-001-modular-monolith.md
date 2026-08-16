# ADR-001 — Monólito Modular

- Status: Accepted
- Data: 2026-08-16
- Responsáveis pela decisão: NEXUS / responsável pelo repositório
- Requisitos relacionados: NFR-MAINT-001, NFR-PORT-001, NFR-TEST-001
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

O AEGIS deve demonstrar um produto real de Commerce e um Centro de Controle de Qualidade, mantendo-se compreensível, testável e reproduzível na máquina local de um contribuidor. Atualmente, o projeto não possui escala medida, equipes independentes, conflitos de frequência de deploy, requisitos de isolamento nem restrições operacionais que justifiquem serviços distribuídos.

Começar com microservices adicionaria falhas de rede, transações distribuídas, coordenação de contratos/versões, topologia de deploy, limites de segurança e custos de observabilidade antes de o produto validar essas necessidades. Esses custos contrariariam os objetivos do projeto de Engenharia de Qualidade profissional, baixa carga cognitiva e entrega incremental.

## Decisão

O AEGIS começará como um **Monólito Modular (Modular Monolith)**.

- Capacidades de negócio são organizadas em módulos explícitos, com modelos de domínio, APIs de aplicação e limites de persistência sob responsabilidade definida.
- Chamadas entre módulos usam interfaces públicas de aplicação documentadas ou eventos publicados. Acesso direto ao repositório ou tabela de outro módulo é proibido.
- Dependências entre módulos devem ser acíclicas e aplicáveis pela estrutura e por verificações arquiteturais automatizadas quando existir código.
- Chamadas internas são síncronas e no mesmo processo por padrão.
- Mensageria assíncrona é introduzida apenas por um requisito concreto de confiabilidade, recuperação ou integração externa.
- Uma base de código e um artefato de aplicação são preferidos inicialmente. Um worker pode executar como perfil de processo separado da mesma base de código quando a integração assíncrona exigir; isso não o torna um microservice.
- A execução local deve continuar possível sem Kubernetes, conta em nuvem ou service mesh.

Os módulos conceituais iniciais são auth, catalog, media, integration, quality e audit. Um limite de composição de aplicação/consulta pode combinar modelos de leitura pertencentes aos módulos para representações de clientes sem criar ciclos entre módulos.

## Alternativas consideradas

### Microservices desde o início

Rejeitada. Nenhum requisito medido de escala, responsabilidade ou deploy compensa a complexidade adicional de rede, consistência, segurança, testes e operação.

### Monólito não estruturado

Rejeitada. Um único deploy sem limites aplicáveis tornaria as responsabilidades pouco claras, incentivaria acoplamento direto por tabelas e dificultaria testar ou compreender a evolução posterior.

### Funções serverless por capacidade

Rejeitada para o sistema inicial. Distribuiria o comportamento e a execução local sem workload ou benefício operacional demonstrado.

### Monólito Modular

Aceita. Oferece limites explícitos e restrições realistas de engenharia, preservando execução, transações, refatoração e feedback de testes simples.

## Consequências

### Positivas

- Menor custo cognitivo e operacional.
- Inicialização local e depuração diretas.
- Forte consistência transacional dentro de operações pertencentes a um módulo.
- Feedback mais rápido de testes de unidade, componente e integração.
- Limites claros de capacidades podem ser validados antes de qualquer extração futura.
- Uma base de código sustenta políticas transversais coerentes de segurança, observabilidade e qualidade.

### Negativas

- Limites entre módulos exigem aplicação deliberada porque a proximidade de processo e banco facilita atalhos.
- Um único artefato de aplicação pode acoplar cadência de release e escala.
- Transações entre módulos podem parecer tentadoras e devem permanecer excepcionais e explícitas.
- Um worker de integração executado separadamente ainda compartilha a evolução do código/artefato com a aplicação.

### Neutras

- PostgreSQL pode ser um único banco lógico, mas os módulos são responsáveis por suas tabelas e migrações.
- RabbitMQ e MinIO permanecem dependências externas somente quando suas fases aprovadas os introduzirem.
- Monólito Modular não proíbe extração futura; exige evidência primeiro.

## Riscos

- A base de código pode degradar para um monólito fortemente acoplado se APIs públicas de módulos e direções de dependência não forem aplicadas.
- Uma capacidade central quality pode crescer demais se seus sublimites internos não permanecerem explícitos.
- Utilitários técnicos compartilhados podem acumular regras de negócio e criar acoplamento oculto.
- Chaves estrangeiras ou transações entre módulos podem enfraquecer a responsabilidade pelo ciclo de vida.
- Contribuidores podem confundir perfis de runtime separados com serviços independentes.

As mitigações incluem testes de arquitetura, migrações sob responsabilidade dos módulos, APIs públicas pequenas, revisão de dependências, ADRs para persistência entre módulos e auditorias regulares dos limites.

## Validação

A decisão é validada incrementalmente ao confirmar que:

1. A Fase 01 consegue construir, testar e executar localmente um artefato de aplicação com caminho de comandos documentado.
2. Regras de dependência entre módulos podem ser expressas e verificadas automaticamente sem módulos especulativos vazios.
3. O comportamento do Catálogo pode ser implementado sem acesso direto a repositórios de outros módulos.
4. A composição de Mídia e a publicação da Integração preservam responsabilidade acíclica.
5. O uso local de recursos permanece prático conforme dependências aprovadas forem introduzidas.
6. Testes de componente/integração conseguem isolar limites de módulos e exercitar adaptadores reais quando necessário.

Evidência de que o Monólito Modular é difícil de manter deve identificar o limite concreto e a falha mensurada; preferência geral por microservices não é evidência.

## Condições para revisitar

Revisitar esta decisão somente quando uma ou mais condições mensuradas existirem:

- um módulo exigir escala independente materialmente diferente;
- equipes independentes precisarem de cadências de release incompatíveis ou isolamento de responsabilidade;
- um limite de isolamento de falha ou segurança não puder ser obtido razoavelmente no mesmo processo;
- tamanho do deploy/inicialização/comportamento de recursos criar restrição operacional demonstrada;
- um módulo tiver contratos e responsabilidade de dados estáveis, mas o monólito impedir capacidade necessária;
- evidências locais e de CI mostrarem que os benefícios da extração superam custos de rede, consistência e operação.

Toda extração exige ADR substituto com consequências de migração, rollback, segurança, testes, dados e observabilidade. Kubernetes, service mesh ou outra orquestração é uma decisão separada e não é consequência implícita da extração de serviço.
