# Musicin 🎵

Player de música para Android, no estilo do Lark Player: toca as músicas que já estão guardadas no seu telefone e deixa você criar playlists.

## O que o app faz

- Lê todas as músicas do telefone (ignora toques e áudios muito curtos)
- Busca por música, artista ou álbum
- Tocar tudo / modo aleatório
- Mini player na parte de baixo + tela de player completa com capa do álbum, barra de progresso, anterior/próxima, aleatório e repetir (desligado, todas, uma)
- Continua tocando com a tela desligada ou com o app fechado, com notificação de controle e botões na tela de bloqueio e nos fones
- Playlists: criar, renomear, excluir, adicionar/remover músicas e tocar
- Menu ⋮ em cada música: tocar a seguir, adicionar à fila, adicionar à playlist
- Tema verde (claro e escuro, segue o do telefone)
- Ícone arredondado

## Como baixar e instalar

1. Abra a aba **Releases** deste repositório no GitHub (ou a aba **Actions** → último build → **Artifacts**).
2. Baixe o arquivo **Musicin.apk**.
3. Abra o arquivo no telefone e toque em **Instalar** (se pedir, permita instalar apps de fontes desconhecidas para o navegador ou gerenciador de arquivos que você usou).
4. Ao abrir o Musicin pela primeira vez, toque em **Permitir acesso** para ele ler suas músicas e mostrar a notificação do player.

## Como usar

- **Músicas**: toque numa música para tocar a lista toda a partir dela. Use ⋮ para mais opções.
- **Playlists**: toque em **Nova playlist**, dê um nome e depois adicione músicas pelo ⋮ de cada música (opção "Adicionar à playlist").
- Toque no mini player (barra de baixo) para abrir o player completo; a setinha ⌄ ou o botão voltar do telefone fecham.

## Como gerar o APK (pelo Termux)

Na pasta do projeto:

```bash
bash enviar.sh
```

Isso cria o repositório no GitHub e envia tudo. O GitHub Actions compila sozinho (leva uns 5 a 10 minutos) e publica o `Musicin.apk` na aba **Releases**.

Se o `enviar.sh` não funcionar, crie um repositório vazio chamado `Musicin` no site do GitHub e rode:

```bash
git init -b main
git add -A
git commit -m "Musicin"
git remote add origin https://github.com/SEU_USUARIO/Musicin.git
git push -u origin main
```

## Tecnologias

Kotlin, Jetpack Compose (Material 3), Media3 (ExoPlayer + MediaSession), build com Gradle 8.9 / AGP 8.5.2.
