# ADR-010 — Arquitetura do frontend do AEGIS Commerce

- Status: Accepted
- Data: 2026-08-16
- Responsáveis pela decisão: NEXUS / responsável pelo repositório
- Requisitos relacionados: REQ-CAT-001 a REQ-CAT-009, NFR-ACC-001, NFR-SEC-001, NFR-TEST-001, NFR-PORT-001
- Substitui: nenhum
- Substituído por: nenhum

## Contexto

A Fase 02 foi ampliada deliberadamente para entregar a primeira experiência utilizável do AEGIS no navegador. Ela deve demonstrar um produto SaaS de engenharia com hierarquia visual, responsividade, acessibilidade e estados completos, sem antecipar Auth/RBAC, um design system amplo ou infraestrutura de frontend sem uso atual.

O frontend consumirá uma API Spring Boot separada e não possui requisito de renderização no servidor, SEO público ou backend-for-frontend. Sua arquitetura precisa manter o limite Catalog reconhecível, reduzir divergência de contrato e permitir testes de comportamento sem concentrar tudo em diretórios globais genéricos.

## Direcionadores da decisão

- Experiência Product profissional e acessível.
- Estrutura por feature/domínio proporcional ao escopo.
- Integração real com API e estados assíncronos completos.
- Execução local simples e segura em loopback.
- Baixo custo de dependências e abstrações.
- Testes rápidos de unidade, componente, integração e acessibilidade.

## Opções consideradas

### Framework React com SSR/full stack

Rejeitado nesta fase. SSR, server actions e deploy próprio não resolvem um requisito atual e duplicariam responsabilidades do backend Java.

### SPA React com build customizado

Rejeitada porque configuração manual de bundler e dev server adicionaria manutenção sem vantagem para o produto.

### SPA React/TypeScript com Vite e organização por feature

Aceita. Resolve build e desenvolvimento local com uma arquitetura pequena, explícita e compatível com a API separada.

### Biblioteca visual completa

Bootstrap, Material UI e templates genéricos foram rejeitados para a Fase 02. Eles acelerariam scaffolding, mas reduziriam a identidade AEGIS e introduziriam uma superfície ampla antes de haver componentes suficientes para justificá-la.

## Decisão

O frontend será uma SPA React + TypeScript construída com Vite.

### Limites e estado

- React Router controla rotas e deep links.
- TanStack Query controla server state, cache, invalidação e mutations.
- Filtros, paginação e ordenação compartilháveis permanecem na URL.
- Estado de formulário é local e usa React Hook Form com schemas Zod.
- Não será introduzida biblioteca de estado global na Fase 02.
- O client HTTP será pequeno, explícito e tipado pelo contrato definido no [ADR-011](ADR-011-openapi-contract.md).

### Organização

~~~text
frontend/src
├── app
│   ├── layout
│   ├── providers
│   ├── router
│   └── styles
├── features
│   └── catalog
│       └── products
│           ├── api
│           ├── components
│           ├── model
│           ├── routes
│           └── schemas
└── shared
    ├── api
    ├── lib
    └── ui
~~~

Diretórios compartilhados recebem somente itens comprovadamente transversais. Regras de Product não migram para `shared`, e não serão criados depósitos globais gigantes de componentes, hooks ou utils.

### UI e identidade

- Styling usa CSS Modules e CSS custom properties para tokens semânticos globais.
- A experiência é light-first; tokens devem permitir um tema futuro, mas dark mode completo permanece fora da Fase 02.
- HTML semântico é preferido a uma biblioteca ampla de primitives.
- Lucide React fornece apenas os ícones efetivamente usados.
- Não haverá Bootstrap, Material UI, design system completo ou Storybook.
- A identidade visual deve parecer uma ferramenta SaaS/engineering moderna, não um e-commerce tradicional ou template administrativo genérico.
- Movimento é discreto e respeita `prefers-reduced-motion`.

### Rotas e telas da Fase 02

- Application Shell com Commerce → Products e aviso persistente de preview local.
- Products List.
- Create Product.
- Product Details.
- Edit Product.
- Confirmação de desativação.

Gestão visual completa de Category permanece fora. O formulário de Product usa `GET /api/v1/categories?status=ACTIVE` para obter somente as Categories reais elegíveis oferecidas pelo backend.

### Local Development Preview

- Dev server e backend ficam ligados a loopback.
- O dev server usa proxy local para `/api`; CORS permissivo não será usado para simplificar desenvolvimento.
- A UI não simula login, token, role ou autorização.
- O aviso de que Auth/RBAC não existem permanece visível.
- Qualquer tentativa de executar a prévia mutável com exposição pública deve falhar com segurança.

### Estratégia de teste

- Vitest para o runner.
- React Testing Library, user-event e jest-dom para comportamento de componentes.
- MSW no boundary HTTP para fluxos e erros.
- Verificações automatizadas e revisão manual de acessibilidade.
- Smoke full-stack manual com frontend, backend e PostgreSQL reais.
- Playwright permanece adiado e exigirá a decisão do ADR-005 antes da primeira suíte E2E.

## Design Quality Gate

O frontend só atende à Fase 02 quando comprovar, por critérios objetivos:

- fluxo completo de listagem, criação, detalhe, edição e desativação;
- loading, empty, no-results, erro, validação, conflito e sucesso;
- navegação por teclado e focus visível;
- contraste e semântica compatíveis com WCAG 2.2 AA nos fluxos críticos;
- reflow responsivo sem quebra ou overflow horizontal nos viewports aprovados;
- submit duplicado impedido e conflito concorrente sem perda silenciosa da entrada;
- nenhuma informação somente por cor;
- nenhum erro de console não justificado;
- uso consistente de tokens e componentes existentes;
- ausência de dados fictícios usados apenas como decoração.

“Estar bonito” não é critério suficiente nem verificável.

## Consequências

### Positivas

- O limite Catalog permanece visível também no frontend.
- A stack cobre necessidades atuais sem framework full stack ou estado global.
- A UI pode evoluir com componentes pequenos e testáveis.
- Acessibilidade, responsividade e estados passam a fazer parte da Definition of Done.

### Negativas

- A identidade visual exige trabalho próprio de CSS e revisão, em vez de montagem por template.
- O frontend adiciona uma segunda toolchain e jobs de CI.
- Smoke full-stack manual não oferece a mesma repetibilidade de uma suíte E2E.

### Neutras

- Dark mode, Storybook e uma biblioteca de primitives podem ser revisitados quando houver uso comprovado.
- A Fase 03 adicionará Auth/RBAC e jornadas Playwright sem precisar substituir a arquitetura de Product.

## Validação

1. Lint, typecheck, testes e build frontend reproduzíveis.
2. Testes de componente/integração para todos os estados críticos.
3. Revisão de acessibilidade automatizada e manual.
4. Design Quality Gate documentado e aprovado.
5. Smoke full-stack manual usando API e PostgreSQL reais.
6. Verificação de que o frontend não inicia publicamente por configuração padrão.
7. Revisão de dependências que confirme ausência de abstrações não usadas.
