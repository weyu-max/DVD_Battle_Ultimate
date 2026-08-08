# Plano de Implementação - DVD Battle Ultimate

Criação de um menu estilo "Brawl Stars" e mecânica de jogo de batalha inspirada no protetor de tela do DVD.

## Mudanças Propostas

### UI do Menu Principal

#### [MODIFY] [activity_main.xml](file:///C:/Users/wesle/AndroidStudioProjects/DVDBattleUltimate/app/src/main/res/layout/activity_main.xml)
- Implementar layout com 3 botões principais: **Modos**, **Personagem**, e **Config**.
- Estilização colorida inspirada em jogos mobile de batalha.

#### [MODIFY] [MainActivity.java](file:///C:/Users/wesle/AndroidStudioProjects/DVDBattleUltimate/app/src/main/java/com/darkonly/dvdbattleultimate/MainActivity.java)
- Adicionar lógica para abrir as novas telas (ou placeholders por enquanto).
- Configurar o botão "Jogar" (dentro de Modos ou direto no menu) para iniciar o jogo.

### Sistema de Jogo

#### [NEW] [GameActivity.java](file:///C:/Users/wesle/AndroidStudioProjects/DVDBattleUltimate/app/src/main/java/com/darkonly/dvdbattleultimate/GameActivity.java)
- Activity que hospedará a visão do jogo.

#### [NEW] [GameView.java](file:///C:/Users/wesle/AndroidStudioProjects/DVDBattleUltimate/app/src/main/java/com/darkonly/dvdbattleultimate/GameView.java)
- Custom View para renderizar os personagens (quadrados).
- Lógica de movimento "DVD" (quicando nas bordas).
- Detecção de colisão e sistema de vida (3 corações).
- Mecânica de **Dash**:
    - Toque na tela para ativar.
    - 3s de cooldown.
    - 10x velocidade por 0.2s.
    - Efeito de translucidez e intangibilidade.
- Barras de vida verdes abaixo dos personagens.

### Recursos

#### [MODIFY] [colors.xml](file:///C:/Users/wesle/AndroidStudioProjects/DVDBattleUltimate/app/src/main/res/values/colors.xml)
- Adicionar cores para o menu e personagens.

## Plano de Verificação

### Testes Manuais
- Abrir o app e verificar se o menu aparece corretamente.
- Iniciar o jogo e testar o movimento dos quadrados.
- Testar o Dash com toque na tela: verificar velocidade, translucidez e cooldown.
- Verificar se a colisão normal tira vida e se o Dash permite atravessar inimigos causando dano.
- Validar se as barras de vida aparecem embaixo dos personagens.
