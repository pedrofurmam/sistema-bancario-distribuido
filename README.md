
# Protocolo de Mensagens - Sistema Bancário Simples

Um projeto para a disciplina de Sistemas Distribuídos que define um protocolo de comunicação baseado em JSON para as operações de um sistema bancário simplificado.

**Criadores:**

* Yan Jardim Leal
* Gabriel Pereira Neves

**Testers**
* Thomas Valeranovicz de Oliveira
* Rafael Adonis Menon

## 1. Pré-requisitos e Instalação

Para a correta manipulação das mensagens JSON, o projeto utiliza a biblioteca  **Jackson** . Recomendamos utilizarem Maven, é necessário adicionar as seguintes dependências ao seu projeto Java:

1.1 Links com as dependências (Caso saiba instalar sozinho)
* **Jackson Databind:**
  * [https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.17.1](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind)
* **Jackson Core:**
  * [https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core/2.17.1](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-core/2.17.1 "null")
* **Jackson Annotations:**
  * [https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations/2.17.1](https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-annotations/2.17.1 "null")
 
1.2 Guia com as dependências (Caso não saiba instalar sozinho)
- Baixar a ferramenta [Eclipse](https://eclipseide.org/)
- Na aba de 'Criar um Projeto' selecione a opção 'Criar um projeto Maven'
- Habilite o botão "Create a simple project" e clique em next
- Preencha todas as informações requiridas (Ou peça para alguma IA caso não se sinta criativo(a))
- Ao criar, você terá um arquivo pom.xml, colar dentro de < project > COLE AQUI < /project >:
  
`<dependencies>`    
`  <dependency>`        
`    <groupId>com.fasterxml.jackson.core</groupId>`        
`    <artifactId>jackson-databind</artifactId>`        
`    <version>2.19.2</version>`    
`  </dependency>`       
`</dependencies>`

- Para instalar as dependencias de fato, você deve clicar com o botão direito em seu projeto->Maven->Update Project, isso instalará as dependências.

1.3 Possíveis Erros
- Caso esteja instalando eclipse pela primeira vez, o erro "Downloading external resources is disabled. [DownloadResourceDisabled]" pode ocorrer, para consertar basta ir em Window->Preferences->Maven->Habilite os botões que dizem "donwload" e "uptade"->Apply

## 2. Como Utilizar o Validador

O repositório fornece uma classe `Validator` para garantir que as mensagens trocadas entre cliente e servidor sigam o protocolo definido.

1. Baixe o repositório do GitHub.
2. Crie uma package chamada "validador".
3. Mova as classes `Validator.java` e `RulesEnum.java` para a pasta recém criada.
4. Antes de enviar uma mensagem (ou após recebê-la), utilize os métodos estáticos da classe `Validator` para verificar sua integridade:

```
// Exemplo de uso
String jsonParaEnviar = "...";

// Valida uma mensagem que será enviada de um cliente para o servidor, a função irá converter a String em JSON e irá verificar.
Validator.validateClient(jsonParaEnviar);

// Valida uma mensagem que será enviada do servidor para um cliente, a função irá converter a String em JSON e irá verificar.
Validator.validateServer(jsonParaEnviar);

```

A função pode lançar uma exceção ou imprimir no console caso a mensagem esteja fora dos padrões definidos.

## 3. Regras de Negócio e Padrões Gerais

### 3.1. Estrutura de Dados

O sistema opera com três entidades principais:

* **Usuário:** Os dados devem ser persistidos e respeitar as propriedades  **ACID** . Operações de CRUD completas são suportadas.
* **Transação:** Os dados devem ser persistidos e respeitar as propriedades  **ACID** . Operações de CR (Criar e Ler) são suportadas.
* **Sessão:** Os dados de sessão (tokens) podem ser mantidos em memória. Operações de CRD (Criar, Ler, Deletar) são suportadas.

### 3.2. Padrão do Campo `operacao`

Toda mensagem trocada deve conter um campo `operacao`, que define a ação a ser executada.

**Valores possíveis para `operacao`:**

* `usuario_login`
* `usuario_logout`
* `usuario_criar`
* `usuario_ler`
* `usuario_atualizar`
* `usuario_deletar`
* `transacao_criar`
* `transacao_ler`

### 3.3. Padrão de Resposta (`status` e `info`)

Toda resposta do servidor deve conter os campos `status` (booleano) e `info` (string) para indicar o resultado da operação.

```
{
  "status": false,
  "info": "Erro ao enviar uma transação, saldo insuficiente."
}

```

### 3.4. Padrão do Token de Autenticação

O token de sessão, gerado no login e utilizado para autenticar operações subsequentes, deve ser sempre tratado como uma  **String** .

## 4. Protocolo da API: Objetos e Mensagens

A seguir, a especificação detalhada para cada operação.

### 4.1. Login de Usuário (`usuario_login`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_login",
  "cpf": "123.456.789-00",
  "senha": "senhaSegura123"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_login",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
  "status": true,
  "info": "Login bem-sucedido."
}

```

### 4.2. Logout de Usuário (`usuario_logout`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_logout",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_logout",
  "status": true,
  "info": "Logout realizado com sucesso."
}

```

### 4.3. Criação de Usuário (`usuario_criar`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_criar",
  "nome": "Gabriel Pereira Neves",
  "cpf": "123.456.789-00",
  "senha": "senhaSegura123"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_criar",
  "status": true,
  "info": "Usuário criado com sucesso."
}

```

### 4.4. Leitura de Dados do Usuário (`usuario_ler`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_ler",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_ler",
  "status": true,
  "info": "Dados do usuário recuperados com sucesso.",
  "usuario": {
    "cpf": "123.456.789-00",
    "saldo": 5430.21,
    "nome": "Gabriel Pereira Neves"
  }
}

```

### 4.5. Atualização de Dados do Usuário (`usuario_atualizar`)

*Nota: Apenas os campos a serem alterados devem ser enviados. A omissão de um campo significa que ele não deve ser modificado.*

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_atualizar",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
  "usuario": {
    "nome": "Gabriel P. Neves",
    "senha": "novaSenhaSuperSegura456"
  }
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_atualizar",
  "status": true,
  "info": "Usuário atualizado com sucesso."
}

```

### 4.6. Deleção de Usuário (`usuario_deletar`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "usuario_deletar",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "usuario_deletar",
  "status": true,
  "info": "Usuário deletado com sucesso."
}

```

### 4.7. Criação de Transação (`transacao_criar`)

#### Envio (Cliente → Servidor)

```
{
  "operacao": "transacao_criar",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
  "valor": 150.75,
  "cpf": "098.765.432-11"
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "transacao_criar",
  "status": true,
  "info": "Transação realizada com sucesso."
}

```

### 4.8. Leitura de Transações (`transacao_ler`)

*Nota: Esta operação utiliza **paginação** para lidar com grandes volumes de dados.*

#### Envio (Cliente → Servidor)

```
{
  "operacao": "transacao_ler",
  "token": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
  "pagina": 1,
  "limite": 20
}

```

#### Recebimento (Servidor → Cliente)

```
{
  "operacao": "transacao_ler",
  "status": true,
  "info": "Transações recuperadas com sucesso.",
  "transacoes": [
    {
      "id": 101,
      "valor_enviado": 150.75,
      "usuario_enviador": {
        "nome": "Gabriel Pereira Neves",
        "cpf": "123.456.789-00"
      },
      "usuario_recebedor": {
        "nome": "Yan Jardim Leal",
        "cpf": "098.765.432-11"
      }
    }
  ]
}

```

## 5. Tipagem de Dados

| **Campo(s)**           | **Tipo de Dado** | **Descrição**                                                                                                                                                                                   |
| ---------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `id`,`pagina`,`limite` | **`int`**      | Valores numéricos inteiros.                                                                                                                                                                            |
| `valor_enviado`,`saldo`  | **`double`**   | Valores numéricos de ponto flutuante.*Nota: Para este projeto,`double`é aceitável. Em sistemas de produção, o ideal seria usar `long`para representar centavos e evitar erros de precisão.* |
| `cpf`  | **`String: 000.000.000-00`**   | O Validador apenas valida se o CPF está na formatação, não se é válido. |
| `nome`,`senha`  | **`String: Min 6 e Max 120 caracteres`**   | O Validador apenas valida o tamanho, ele desconsidera espaços no começo. |
| Todos os outros campos       | **`String: Min 3 e Max 200`**   | Valores de texto.                                                                                                                                                                                       |
