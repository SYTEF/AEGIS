# Glossário do AEGIS

Este glossário define termos usados entre documentos. O armazenamento físico e os formatos da API podem evoluir, mas as implementações devem preservar essas distinções.

## Release

Um escopo lógico e versionado de entrega, como v1.0.0. Agrupa um ou mais candidatos e as decisões históricas tomadas sobre eles. A versão da release não é suficiente para identificar os bits executáveis.

## Candidato

Uma tentativa de avaliação de uma release, vinculada a exatamente uma identidade imutável de build, ambiente-alvo e corte de evidência. Vários candidatos podem existir para a mesma release; substituir um nunca sobrescreve suas evidências nem seu histórico de decisões.

## Build

Uma identidade imutável de saída derivada de um commit específico do código-fonte e das entradas resolvidas do build. Pode identificar um ou mais artefatos. Uma versão de exibição sem identidade de commit/artefato não oferece proveniência de evidência adequada.

## Execução

Uma invocação atribuível de uma suíte de testes, Caso de Teste, varredura de segurança, cenário de performance, experimento de resiliência ou outra fonte de evidência aprovada. Registra fonte efetiva, build/candidato, ambiente, tempos, versão da ferramenta/schema e histórico de tentativas.

## Evidência

Um resultado atribuível ou referência imutável que sustenta uma conclusão de engenharia/release, como resultado de teste, relatório, captura de tela, trace, trecho de log, achado de segurança ou medição de performance. A Evidência inclui metadados de proveniência, atualização, sensibilidade e retenção; uma captura de tela ou payload não confiável isoladamente não é prova.

## Gate

Uma condição de política determinística e versionada, avaliada contra um snapshot de evidências do candidato. Seu resultado é PASS, FAIL, INSUFFICIENT_EVIDENCE, NOT_APPLICABLE ou ERROR. Gates críticos não podem ser sobrepostos por pontuação numérica.

## Risco

Uma classificação versionada da incerteza e do impacto potencial do candidato usando resultados de gates, evidências e, quando aplicável, Pontuação de Qualidade. Os níveis iniciais são LOW, MEDIUM, HIGH, CRITICAL e UNKNOWN. Risco não equivale à taxa de aprovação.

## Decisão

Um registro imutável de responsabilidade humana que aprova, bloqueia ou — somente quando a política permitir — aprova com exceção explícita e limitada no tempo um candidato/build exato. Uma recomendação de máquina é uma entrada para a decisão, nunca a própria decisão.

## Termos relacionados

- **Pontuação de Qualidade (Quality Score):** futura entrada derivada e explicável da política; NOT_APPLICABLE antes da maturidade do Motor de Qualidade.
- **Recomendação:** futura saída do Motor (APPROVE, REVIEW ou BLOCK) sob política versionada; nunca faz deploy nem cancela bloqueio crítico.
- **ID de correlação:** identificador diagnóstico limitado de um fluxo lógico; não é autenticação, idempotência nem prova de correção.
- **ID de trace:** identificador de um trace causal de telemetria; uma correlação pode mapear vários traces após retries ou replays.
