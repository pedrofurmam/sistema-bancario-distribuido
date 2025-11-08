package cliente;

import cliente.Cliente;
import cliente.ProcessadorRespostas;
import com.fasterxml.jackson.databind.ObjectMapper;
import validator.Validator;

import java.util.HashMap;
import java.util.Map;

public class ServicoUsuario {
    private Cliente cliente;
    private ProcessadorRespostas processador;
    private ObjectMapper mapper;
    private String token;

    public ServicoUsuario(Cliente cliente) {
        this.cliente = cliente;
        this.processador = new ProcessadorRespostas();
        this.mapper = new ObjectMapper();
    }

    public boolean cadastrarUsuario(String nome, String cpf, String senha) {
        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_criar");
            dados.put("nome", nome);
            dados.put("cpf", cpf);
            dados.put("senha", senha);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);
            Validator.validateServer(resposta);


            return processador.verificaAcao(resposta);

        } catch (IllegalArgumentException e) {
            // Tratar erros de validação de forma elegante
            System.out.println("Erro de validação: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao processar cadastro: " + e.getMessage());
            return false;
        }
    }

    public String fazerLogin(String cpf, String senha) {
        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_login");
            dados.put("cpf", cpf);
            dados.put("senha", senha);

            String json = mapper.writeValueAsString(dados);
            Validator.validateClient(json);

            String resposta = cliente.enviarMensagem(json);

            Validator.validateServer(resposta);

            processador.processarResposta(resposta);

            if (processador.verificaAcao(resposta)) {
                return processador.extrairToken(resposta);
            }
            return null;

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao processar login: " + e.getMessage());
            return null;
        }
    }

    public boolean fazerLogout(String token) {
        if (token == null) {
            System.out.println("Você não está logado!");
            return false;
        }

        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_logout");
            dados.put("token", token);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);

            Validator.validateServer(resposta);

            processador.processarResposta(resposta);

            return processador.verificaAcao(resposta);

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao processar logout: " + e.getMessage());
            return false;
        }
    }

    public void verMeusDados(String token) {
        if (token == null) {
            System.out.println("Você precisa estar logado!");
            return;
        }

        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_ler");
            dados.put("token", token);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);


            if (resposta != null) {
                Validator.validateServer(resposta);


                processador.processarDadosUsuario(resposta);

                processador.verificaAcao(resposta);
            }

            return;

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Erro ao consultar dados: " + e.getMessage());
            return;
        }
    }

    public void atualizarDados(String token, Map<String, String> dadosAtualizacao) {
        if (token == null) {
            System.out.println("Token inválido");
            return;
        }

        if (dadosAtualizacao == null || dadosAtualizacao.isEmpty()) {
            System.out.println("Nenhum dado fornecido para atualização");
            return;
        }

        try {
            Map<String, Object> dados = new HashMap<>();
            dados.put("operacao", "usuario_atualizar");
            dados.put("token", token);
            dados.put("usuario", dadosAtualizacao);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);

            ProcessadorRespostas processador = new ProcessadorRespostas();
            processador.processarResposta(resposta);

        } catch (Exception e) {
            System.out.println("Erro ao atualizar dados: " + e.getMessage());
        }
    }

    public boolean depositar(String token, double valor) {
        if (token == null) {
            System.out.println("Você precisa estar logado!");
            return false;
        }

        try {
            Map<String, Object> dados = new HashMap<>();
            dados.put("operacao", "depositar");
            dados.put("token", token);
            dados.put("valor_enviado", valor);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);

            if (resposta != null) {
                Validator.validateServer(resposta);
                processador.processarResposta(resposta);
                return processador.verificaAcao(resposta);
            }

            return false;

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao processar depósito: " + e.getMessage());
            return false;
        }
    }

    public boolean enviarDinheiro(String token, String cpfDestino, double valor) {
        try {
            Map<String, Object> dados = new HashMap<>();
            dados.put("operacao", "transacao_criar");
            dados.put("token", token);
            dados.put("cpf_destino", cpfDestino);
            dados.put("valor", valor);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);

            ProcessadorRespostas processador = new ProcessadorRespostas();
            boolean sucesso = processador.verificaAcao(resposta);





            if (sucesso) {
                System.out.println("Transferência realizada com sucesso!");
            } else {
                processador.processarResposta(resposta);
            }

            return sucesso;

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação na tranferência: " + e.getMessage());
            return false;
        } catch (Exception e) {
          System.err.println("Erro ao realizar tranferência: " + e.getMessage());
          return false;
        }
    }

    public void verTransacoes(String token, String dataInicial, String dataFinal) {
        if (token == null) {
            System.out.println("Você precisa estar logado!");
            return;
        }

        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "transacao_ler");
            dados.put("token", token);
            dados.put("data_inicial", dataInicial);
            dados.put("data_final", dataFinal);

            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);

            if (resposta != null) {
                Validator.validateServer(resposta);
                processador.processarTransacoes(resposta);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao consultar transações: " + e.getMessage());
        }
    }

    public void reportarErroServidor(String operacaoEnviada, String motivoErro) {
        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "erro_servidor");
            dados.put("operacao_enviada", operacaoEnviada);
            dados.put("info", motivoErro);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(dados);

            System.out.println("Reportando erro do servidor: " + motivoErro);
            cliente.enviarMensagem(json);

        } catch (Exception e) {
            System.err.println("Erro ao reportar erro do servidor: " + e.getMessage());
        }
    }

    public boolean deletarCadastro(String token){

        if (token == null) {
            System.out.println("Você precisa estar logado!");
            return false;
        }

        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_deletar");
            dados.put("token", token);
            String json = mapper.writeValueAsString(dados);

            Validator.validateClient(json);
            String resposta = cliente.enviarMensagem(json);



            ProcessadorRespostas processador = new ProcessadorRespostas();
            boolean sucesso = processador.verificaAcao(resposta);

            if (sucesso) {
                System.out.println("Conta deletada com sucesso!");
            } else {
                processador.processarResposta(resposta);
            }

            return sucesso;

        } catch (IllegalArgumentException e) {
            System.out.println("Erro de validação: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao deletar cadastro: " + e.getMessage());
            return false;
        }
    }
}