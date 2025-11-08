package cliente;

import validator.Validator;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


public class Cliente {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.iniciar();
    }

    public void iniciar() {
        if (conectarComServidor()) {
            InterfaceUsuario interfaceUsuario = new InterfaceUsuario(this);
            interfaceUsuario.iniciarMenu();
            fecharConexao();
        }
    }

    private boolean conectarComServidor() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("IP do servidor: ");
            String serverIP = br.readLine();

            System.out.print("Porta do servidor: ");
            int serverPort = Integer.parseInt(br.readLine());

            socket = new Socket(serverIP, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            // Enviar operação conectar
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "conectar");

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(dados);

            String resposta = enviarMensagem(json);

            if (resposta != null && resposta.contains("\"status\":true")) {
                System.out.println("Servidor conectado com sucesso");
                return true;
            } else {
                System.out.println("Servidor rejeitou conexão");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Erro de conexão: " + e.getMessage());
            return false;
        }
    }

    public String enviarMensagem(String json) {
        try {

            Validator.validateClient(json);

            //Mensagem enviada pelo cliente
            System.out.println("Cliente enviou: " + json);

            out.println(json);
            String resposta = in.readLine();


            if (resposta != null) {
                System.out.println("Cliente recebeu: " + resposta);
                try {
                    Validator.validateServer(resposta);
                } catch (Exception e) {
                    System.out.println("Resposta do servidor não segue o protocolo: " + e.getMessage());

                    // Extrair operação da mensagem original para reportar
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(json);
                        String operacaoEnviada = node.get("operacao").asText();

                        // Reportar erro ao servidor
                        ServicoUsuario servico = new ServicoUsuario(this);
                        servico.reportarErroServidor(operacaoEnviada, "Resposta do servidor não segue o protocolo: " + e.getMessage());

                    } catch (Exception ex) {
                        System.err.println("Erro ao extrair operação para report: " + ex.getMessage());
                    }
                }
            }

            return resposta;

        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            return null;
        }
    }

    private void fecharConexao() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }
}