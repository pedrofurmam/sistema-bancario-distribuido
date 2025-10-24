package cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessadorRespostas {
    private ObjectMapper objectMapper;

    public ProcessadorRespostas() {
        this.objectMapper = new ObjectMapper();
    }

    public void processarResposta(String resposta) {
        if (resposta == null) {
            System.out.println("Servidor não respondeu");
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(resposta);
            boolean sucesso = node.get("status").asBoolean();
            String info = node.get("info").asText();

            if (node.has("token")) {
                String token = node.get("token").asText();
                System.out.println("Token do usuário: " + token);
            }

            if (sucesso) {
                System.out.println("Deu certo!  =)" + info);
            } else {
                System.out.println("Deu errado! =( " + info);
            }
        } catch (Exception e) {
            System.out.println(" Erro ao processar resposta");
        }

    }

    public String extrairToken(String resposta){
        try {
            JsonNode node = objectMapper.readTree(resposta);
            return node.get("token").asText();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean verificaAcao(String resposta){
        try {
            JsonNode node = objectMapper.readTree(resposta);
             return  node.get("status").asBoolean();

        } catch (Exception e) {
            return false;
        }

    }



    public void processarDadosUsuario(String resposta) {
        if (resposta == null) {
            System.out.println("Servidor não respondeu");
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(resposta);
            boolean sucesso = node.get("status").asBoolean();
            String info = node.get("info").asText();

            if (sucesso && node.has("usuario")) {
                JsonNode usuario = node.get("usuario");

                System.out.println("\n" + "=".repeat(40));
                System.out.println("           MEUS DADOS");
                System.out.println("=".repeat(40));
                System.out.println("Nome: " + usuario.get("nome").asText());
                System.out.println("CPF: " + usuario.get("cpf").asText());
                System.out.println("Saldo: R$ " + String.format("%.2f", usuario.get("saldo").asDouble()));
                System.out.println("=".repeat(40));

            } else {
                System.out.println("Erro ao consultar dados: " + info);
            }

        } catch (Exception e) {
            System.out.println("Erro ao processar dados do usuário: " + e.getMessage());
        }
    }

    public void processarTransacoes(String resposta) {
        // Implementar quando a operação transacao_ler for criada
    }
}