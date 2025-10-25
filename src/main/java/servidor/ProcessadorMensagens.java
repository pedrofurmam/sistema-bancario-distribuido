package servidor;

import autenticador.Token;
import database.BancoDados;
import modelo.Usuario;
import dao.UsuarioDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import validator.Validator;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import dao.TransacaoDAO;
import java.sql.Connection;
import java.sql.SQLException;

public class ProcessadorMensagens {
    private ObjectMapper objectMapper;

    public ProcessadorMensagens() {
        this.objectMapper = new ObjectMapper();
        BancoDados.criarTabelas();
    }

    public String processarMensagem(String jsonRecebido) {
        String operacao = "erro";

        try {
            // Parse inicial para extrair operação
            JsonNode node = objectMapper.readTree(jsonRecebido);
            operacao = node.has("operacao") ? node.get("operacao").asText() : "erro";

            // Validar com Validator
            Validator.validateClient(jsonRecebido);

            // Processar operação
            String resposta = processarOperacao(node, operacao);

            // Validar resposta
            Validator.validateServer(resposta);
            return resposta;

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            return criarRespostaErro(operacao,"Erro de sintaxe. A mensagem não é um JSON válido.");
        } catch (IllegalArgumentException e) {
            return criarRespostaErro(operacao,e.getMessage());
        } catch (Exception e) {
            // Para Exception genérica do parseJson e outros erros
            return criarRespostaErro(operacao,e.getMessage());
        }
    }


    private String processarOperacao(JsonNode node, String operacao) {
        switch (operacao) {
            case "usuario_criar":
                return processarCadastroUsuario(node);
            case "usuario_login":
                return processarLogin(node);
            case "usuario_ler":
                return processarLeituraUsuario(node);
            case "usuario_atualizar":
                return processarAtualizarUsuario(node);
            case "usuario_deletar":
                return processarDeletarUsuario(node);
            case "usuario_logout":
                return processarLogout(node);
            case "conectar":
                return processarConexao();
            case "depositar":
                return processarDeposito(node);
            case "transacao_criar":
                return processarTransacao(node);
            default:
                return criarRespostaErro(operacao, "Operação ainda não implementada");
        }
    }

    private String criarRespostaErroPorOperacao(String operacao, String detalhe) {
        switch (operacao) {
            case "usuario_criar":
                return criarRespostaErro("usuario_criar", "Ocorreu um erro ao criar usuário.");
            case "usuario_login":
                return criarRespostaErro("usuario_login", "Ocorreu um erro ao realizar login.");
            case "usuario_logout":
                return criarRespostaErro("usuario_logout", "Ocorreu um erro ao realizar logout.");
            case "usuario_ler":
                return criarRespostaErro("usuario_ler", "Erro ao ler dados do usuário.");
            case "usuario_atualizar":
                return criarRespostaErro("usuario_atualizar", "Erro ao atualizar usuário.");
            case "usuario_deletar":
                return criarRespostaErro("usuario_deletar", "Erro ao deletar usuário.");
            case "depositar":
                return criarRespostaErro("depositar", "Erro ao depositar.");
            case "transacao_criar":
                return criarRespostaErro("transacao_criar", "Erro ao criar transação.");
            default:
                return criarRespostaErro("erro", "Erro interno do servidor");
        }
    }

    // MÉTODOS AUXILIARES PARA PADRONIZAR RESPOSTAS
    private String criarRespostaSucesso(String operacao, String info, Map<String, Object> dadosExtras) {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("operacao", operacao);
        resposta.put("status", true);
        resposta.put("info", info);

        if (dadosExtras != null) {
            resposta.putAll(dadosExtras);
        }

        try {
            return objectMapper.writeValueAsString(resposta);
        } catch (Exception e) {
            return criarRespostaErro(operacao, "Erro interno do servidor");
        }
    }

    private String criarRespostaErro(String operacao, String info) {
        return String.format(
                "{\"operacao\":\"%s\",\"status\":false,\"info\":\"%s\"}",
                operacao, info
        );
    }

    private String processarConexao() {
        try {
            return criarRespostaSucesso("conectar", "Servidor conectado com sucesso", null);
        } catch (Exception e) {
            return criarRespostaErro("conectar", "Erro ao se conectar");
        }
    }

    private String processarCadastroUsuario(JsonNode node) {
        try {
            // Extrair dados do JSON
            String nome = node.get("nome").asText();
            String cpf = node.get("cpf").asText();
            String senha = node.get("senha").asText();

            UsuarioDAO dao = new UsuarioDAO();

            // 1. Verificar se CPF já existe
            if (dao.cpfExiste(cpf)) {
                return criarRespostaErro("usuario_criar", "CPF já cadastrado");
            }

            // 2. Criar e salvar usuário (com saldo inicial = 0.0)
            Usuario usuario = new Usuario(nome, cpf, senha);
            boolean sucesso = dao.salvar(usuario);

            // 3. Retornar resultado
            if (sucesso) {
                return criarRespostaSucesso("usuario_criar", "Usuário criado com sucesso", null);
            } else {
                return criarRespostaErro("usuario_criar", "Ocorreu um erro ao criar usuário");
            }

        } catch (Exception e) {
            return criarRespostaErro("usuario_criar", "Erro ao processar dados do usuário");
        }
    }

    private String processarLogin(JsonNode node) {
        try {
            String cpf = node.get("cpf").asText();
            String senha = node.get("senha").asText();

            UsuarioDAO dao = new UsuarioDAO();

            // 1. Validar credenciais no banco
            if (dao.validarLogin(cpf, senha)) {
                // 2. Gerar token para usuário válido
                String token = Token.gerarToken(cpf);

                // 3. Criar dados extras com token
                Map<String, Object> extras = new HashMap<>();
                extras.put("token", token);

                return criarRespostaSucesso("usuario_login", "Login realizado com sucesso", extras);
            }

            return criarRespostaErro("usuario_login", "Login inválido");

        } catch (Exception e) {
            return criarRespostaErro("usuario_login", "Erro ao processar login");
        }
    }

    private String processarLogout(JsonNode node) {
        try {
            String token = node.get("token").asText();

            // 1. Validar se token é válido
            String cpf = Token.validarToken(token);
            if (cpf == null) {
                return criarRespostaErro("usuario_logout", "Token inválido ou expirado");
            }

            // 2. Invalidar o token
            Token.invalidarToken(token);

            return criarRespostaSucesso("usuario_logout", "Logout realizado com sucesso", null);

        } catch (Exception e) {
            return criarRespostaErro("usuario_logout", "Erro ao processar logout");
        }
    }

    private String processarLeituraUsuario(JsonNode node) {
        try {
            String token = node.get("token").asText();

            // 1. Validar se token é válido
            String cpf = Token.validarToken(token);
            if (cpf == null) {
                return criarRespostaErro("usuario_ler", "Token inválido ou expirado");
            }

            // 2. Buscar dados do usuário no banco
            UsuarioDAO dao = new UsuarioDAO();
            Usuario usuario = dao.buscarPorCpf(cpf);

            if (usuario == null) {
                return criarRespostaErro("usuario_ler", "Usuário não encontrado");
            }

            // 3. extrair dados para enviar json
            Map<String, Object> dadosUsuario = new HashMap<>();
            dadosUsuario.put("nome", usuario.getNome());
            dadosUsuario.put("cpf", usuario.getCpf());
            dadosUsuario.put("saldo", usuario.getSaldo());

            Map<String, Object> extras = new HashMap<>();
            extras.put("usuario", dadosUsuario);

            return criarRespostaSucesso("usuario_ler", "Dados do usuário recuperados com sucesso", extras);

        } catch (Exception e) {
            return criarRespostaErro("usuario_ler", "Erro ao buscar dados do usuário");
        }
    }

    private String processarAtualizarUsuario(JsonNode node) {
        try {
            String token = node.get("token").asText();

            // 1. Validar se token é válido
            String cpf = Token.validarToken(token);
            if (cpf == null) {
                return criarRespostaErro("usuario_atualizar", "Token inválido ou expirado");
            }

            // 2. Extrair dados do usuário a serem atualizados
            JsonNode usuarioNode = node.get("usuario");
            if (usuarioNode == null || !usuarioNode.isObject()) {
                return criarRespostaErro("usuario_atualizar", "Dados do usuário não fornecidos");
            }

            // 3. Validar se pelo menos um campo foi enviado
            if (!usuarioNode.has("nome") && !usuarioNode.has("senha")) {
                return criarRespostaErro("usuario_atualizar", "Pelo menos um campo deve ser fornecido para atualização");
            }

            // 4. Atualizar no banco de dados
            UsuarioDAO dao = new UsuarioDAO();
            boolean sucesso = dao.atualizar(cpf, usuarioNode);

            if (sucesso) {
                return criarRespostaSucesso("usuario_atualizar", "Usuário atualizado com sucesso", null);
            } else {
                return criarRespostaErro("usuario_atualizar", "Erro ao atualizar usuário");
            }

        } catch (Exception e) {
            return criarRespostaErro("usuario_atualizar", "Erro ao processar dados do usuário");
        }
    }

    private String processarDeposito(JsonNode node) {
        try {
            String token = node.get("token").asText();

            // 1. Validar se token é válido
            String cpf = Token.validarToken(token);
            if (cpf == null) {
                return criarRespostaErro("depositar", "Token inválido ou expirado");
            }

            // 2. Extrair valor do depósito
            if (!node.has("valor_enviado")) {
                return criarRespostaErro("depositar", "Campo valor_enviado é obrigatório");
            }

            double valorDeposito = node.get("valor_enviado").asDouble();

            // 3. Validar se valor é válido (positivo)
            if (valorDeposito <= 0) {
                return criarRespostaErro("depositar", "Valor do depósito deve ser positivo");
            }

            if (!temMaximoDuasCasasDecimais(valorDeposito)) {
                return criarRespostaErro("depositar", "Valor deve ter no máximo 2 casas decimais");
            }

            // 4. Buscar saldo atual do usuário
            UsuarioDAO dao = new UsuarioDAO();
            Usuario usuario = dao.buscarPorCpf(cpf);

            if (usuario == null) {
                return criarRespostaErro("depositar", "Usuário não encontrado");
            }

            // 5. Calcular novo saldo
            double novoSaldo = usuario.getSaldo() + valorDeposito;

            // 6. Atualizar saldo no banco
            boolean sucesso = dao.atualizarSaldo(cpf, novoSaldo);

            if (sucesso) {
                return criarRespostaSucesso("depositar", "Deposito realizado com sucesso", null);
            } else {
                return criarRespostaErro("depositar", "Erro ao depositar");
            }

        } catch (Exception e) {
            return criarRespostaErro("depositar", "Erro ao processar depósito");
        }
    }

    private String processarDeletarUsuario(JsonNode node) {
        try {
            String token = node.get("token").asText();


            // 1. Validar se token é válido
            String cpf = Token.validarToken(token);
            if (cpf == null) {
                return criarRespostaErro("usuario_deletar", "Token inválido ou expirado");
            }

            // 2. solicitar exclusao ao banco
            UsuarioDAO dao = new UsuarioDAO();
            boolean sucesso = dao.deletar(cpf);


            // 3. Retornar resultado
            if (sucesso) {
                return criarRespostaSucesso("usuario_deletar", "Usuário deletado com sucesso", null);
            } else {
                return criarRespostaErro("usuario_deletar", "Erro ao deletar usuário");
            }

        } catch (Exception e) {
            return criarRespostaErro("usuario_deletar", "Erro ao processar dados do usuário");
        }
    }

    private String processarTransacao(JsonNode node) {
        try {
            String token = node.get("token").asText();

            // 1. Validar token
            String cpfEnviador = Token.validarToken(token);
            if (cpfEnviador == null) {
                return criarRespostaErro("transacao_criar", "Token inválido ou expirado");
            }

            // 2. Extrair dados da transação
            if (!node.has("valor") || !node.has("cpf_destino")) {
                return criarRespostaErro("transacao_criar", "Campos valor e cpf_destino são obrigatórios");
            }

            double valor = node.get("valor").asDouble();
            String cpfRecebedor = node.get("cpf_destino").asText();

            // 3. Validações de negócio
            if (valor <= 0) {
                return criarRespostaErro("transacao_criar", "Valor da transação deve ser positivo");
            }

            if (!temMaximoDuasCasasDecimais(valor)) {
                return criarRespostaErro("transacao_criar", "Valor deve ter no máximo 2 casas decimais");
            }

            if (cpfEnviador.equals(cpfRecebedor)) {
                return criarRespostaErro("transacao_criar", "Não é possível enviar dinheiro para si mesmo");
            }

            UsuarioDAO usuarioDAO = new UsuarioDAO();

            // 4. Verificar se remetente existe e tem saldo suficiente
            Usuario enviador = usuarioDAO.buscarPorCpf(cpfEnviador);
            if (enviador == null) {
                return criarRespostaErro("transacao_criar", "Usuário enviador não encontrado");
            }

            if (enviador.getSaldo() < valor) {
                return criarRespostaErro("transacao_criar", "Saldo insuficiente");
            }

            // 5. Verificar se destinatário existe
            Usuario recebedor = usuarioDAO.buscarPorCpf(cpfRecebedor);
            if (recebedor == null) {
                return criarRespostaErro("transacao_criar", "CPF de destino não encontrado");
            }

            // 6. Executar transação (atomicidade)
            try (Connection conn = BancoDados.getConnection()) {
                conn.setAutoCommit(false); // Iniciar transação

                // 6.1. Debitar do enviador
                double novoSaldoEnviador = enviador.getSaldo() - valor;
                if (!usuarioDAO.atualizarSaldo(cpfEnviador, novoSaldoEnviador)) {
                    conn.rollback();
                    return criarRespostaErro("transacao_criar", "Erro ao debitar valor do enviador");
                }

                // 6.2. Creditar ao recebedor
                double novoSaldoRecebedor = recebedor.getSaldo() + valor;
                if (!usuarioDAO.atualizarSaldo(cpfRecebedor, novoSaldoRecebedor)) {
                    conn.rollback();
                    return criarRespostaErro("transacao_criar", "Erro ao creditar valor ao recebedor");
                }

                // 6.3. Registrar transação
                TransacaoDAO transacaoDAO = new TransacaoDAO();
                if (!transacaoDAO.criarTransacao(cpfEnviador, cpfRecebedor, valor)) {
                    conn.rollback();
                    return criarRespostaErro("transacao_criar", "Erro ao registrar transação");
                }

                conn.commit(); // Confirmar transação
                return criarRespostaSucesso("transacao_criar", "Transação realizada com sucesso", null);

            } catch (Exception e) {
                return criarRespostaErro("transacao_criar", "Erro ao processar transação");
            }

        } catch (Exception e) {
            return criarRespostaErro("transacao_criar", "Erro ao processar dados da transação");
        }
    }

    private boolean temMaximoDuasCasasDecimais(double valor) {
        try {
            BigDecimal bd = BigDecimal.valueOf(valor);
            return bd.scale() <= 2;
        } catch (Exception e) {
            return false;
        }
    }
}