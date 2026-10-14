# Sua Privacidade

App Android que vigia o microfone e a câmera do celular: avisa quando algum app liga um deles fora da tela, guarda o histórico de usos e de notificações e acompanha de perto os apps que você escolher.

> Versão **1.0.0.0 beta**. Mesmo estilo e base técnica do [Contas em Dia](https://github.com/joaopedroangelico/ContasEmDia) e do [Doses](https://github.com/joaopedroangelico/Doses).

## Funcionalidades

- **Aviso na hora** quando o microfone ou a câmera ligam com a **tela apagada** ou o **celular bloqueado**, com o app que provavelmente foi o responsável.
- **Início**: situação agora ("Microfone em uso agora", "Tudo tranquilo hoje"), placar do dia (microfone, câmera, usos suspeitos, notificações) e os usos suspeitos da semana. Tocar num uso mostra os detalhes, leva às permissões do app ou coloca o app sob supervisão.
- **Bem-estar digital** (na tela inicial): tempo de tela do dia em destaque e comparado com a média da semana, desbloqueios, aberturas de apps e notificações, os últimos 7 dias em colunas (toque num dia para ver os detalhes dele) e os apps mais usados em barras. A tela inicial do celular e a barra do sistema não entram na conta, como no Bem-estar digital do Android.
- **Supervisão**: escolha apps para acompanhar de perto. Cada um ganha uma ficha com:
  - **microfone e câmera**: aviso a **qualquer** uso fora da tela (não só com a tela apagada) e os últimos usos;
  - **internet**: quanto gastou hoje e nos últimos 7 dias, separando o que foi **em segundo plano**;
  - **permissões**: as permissões sensíveis que o app tem e um **aviso quando ele ganha uma nova** (ex.: depois de uma atualização);
  - **tempo de uso** e quantas vezes foi aberto, hoje e na semana;
  - **notificações** enviadas, com atalho para elas no histórico.
- **Histórico** em gavetas por dia, com filtro por app:
  - **Microfone e câmera**: horário, duração, app provável e a situação (com o app aberto, fora da tela, tela apagada, em ligação, barrado pelo Android).
  - **Notificações**: app, horário, título e texto de cada notificação recebida (toque para ver o texto inteiro). Apps marcados como **texto oculto** ficam só com o app e o horário.
- **Auditoria**: todos os apps com permissão de microfone e/ou câmera, com o último uso registrado. Um toque abre a tela de permissões do app no Android, para trocar para "Perguntar sempre" ou "Não permitir".
- **Ajustes**: tema, prazo do histórico (7, 30 ou 90 dias; padrão 30), apps com texto oculto, permissões, armazenamento e "apagar todo o histórico".
- **Temas**: Sistema, Lavanda, Menta, Pêssego, Oceano (padrão) e Rosé, no modo claro/escuro do celular.

**Privacidade:** os dados ficam só no celular. O app não tem permissão de internet, fica fora do backup na nuvem e nunca liga o microfone nem a câmera. Veja a [Política de Privacidade](PRIVACIDADE.md).

## O que o app não faz

- **Não bloqueia outros apps.** O Android não dá a nenhum app comum o poder de tirar o microfone ou a câmera de outro app. Quem bloqueia é você, nas permissões de cada app, e o Sua Privacidade leva direto até lá.
- **Não sabe com certeza qual app usou o sensor.** O Android avisa que o microfone/câmera ligou, mas esconde o nome do app. O nome mostrado é um **palpite**:
  1. o app aberto na tela, se ele tem a permissão (provável);
  2. senão, um app rodando em segundo plano com notificação fixa e a permissão: provável se for o único, possível se houver mais de um;
  3. senão, "origem desconhecida". Costuma ser o próprio sistema, como o "Ok Google" ou o ditado do teclado.

Desde o Android 9, apps em segundo plano sem serviço em primeiro plano recebem **silêncio** do microfone. O app registra isso como "Barrado pelo Android".

## Como funciona

A vigia mora no **leitor de notificações** (`VigiaService`). Enquanto o acesso estiver concedido, o Android mantém esse serviço ligado e o religa depois de reiniciar, sem precisar de uma notificação fixa do app. Dentro dele, a `Vigia`:

- escuta `AudioManager.AudioRecordingCallback` (microfone) e `CameraManager.AvailabilityCallback` (câmera, ignorando a lanterna);
- a cada uso, lê se a tela está ligada ou bloqueada, se há ligação em andamento, qual app está na tela (`UsageStatsManager`) e quais apps têm serviço em primeiro plano, e monta o palpite (`palpitar` em `data/Palpite.kt`);
- guarda cada notificação recebida, ignorando as fixas (música, download) e reenvios sem mudança;
- a cada 15 minutos, e quando um app é instalado ou atualizado, compara as permissões dos supervisionados e apaga o que passou do prazo do histórico.

Os avisos do Android viram eventos numa fila tratada um por vez, para um "desligou" nunca passar na frente do "ligou".

## Primeira configuração

1. **Acesso a notificações** (essencial). No Android 13 ou superior, apps instalados por APK precisam antes de *Informações do app → ⋮ → Permitir configurações restritas*; o app explica e leva até lá.
2. **Acesso ao uso**, para o palpite do app, tempo de uso e internet.
3. **Notificações**, para os avisos.
4. **Sem restrição de bateria**, para celulares que encerram apps em segundo plano.

## Tecnologias

Kotlin 2.1 · Jetpack Compose (Material 3) · Room · DataStore · NotificationListenerService · UsageStatsManager · NetworkStatsManager. Android 8.0 (API 26) ou superior.

## Estrutura

```
app/src/main/java/com/suaprivacidade/app/
├── data/      usos, notificações, supervisionados (Room), preferências, apps instalados,
│              estatísticas de uso e internet, palpite e formatação
├── vigia/     leitor de notificações, vigia do microfone/câmera, avisos e acessos do sistema
└── ui/        telas (Início, Supervisão, Histórico, Auditoria, ajustes), ViewModel e temas
```

## Compilar

Pré-requisitos: JDK 17+ e Android SDK (caminho em `local.properties`).

```bash
./gradlew testDebugUnitTest   # testes do palpite e da formatação
./gradlew assembleRelease     # APK otimizado em app/build/outputs/apk/release/
```

A versão release usa a chave descrita em `keystore.properties` (fora do git). Sem ela, o release sai sem assinatura; crie uma chave própria para o Sua Privacidade e **guarde-a com cuidado**: sem ela não é possível publicar atualizações que instalem por cima.
