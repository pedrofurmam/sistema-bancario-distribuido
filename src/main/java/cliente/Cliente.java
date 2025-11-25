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
            System.out.println("Cliente enviou: " + json);
            out.println(json);
            String resposta = in.readLine();

            if (resposta != null) {
                System.out.println("Cliente recebeu: " + resposta);
                try {
                    Validator.validateServer(resposta);
                    return resposta;
                } catch (Exception e) {
                    System.out.println("Resposta do servidor não segue o protocolo: " + e.getMessage());

                    // Reporta o erro ao servidor
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode noOriginal = mapper.readTree(json);
                        String operacaoOriginal = noOriginal.get("operacao").asText();
                        reportarErroServidor(operacaoOriginal, e.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Falha ao reportar erro do servidor: " + ex.getMessage());
                    }

                    // Retorna null para que a camada de serviço trate o erro e o menu seja reexibido.
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            return null;
        }
    }
    private void reportarErroServidor(String operacaoEnviada, String motivoErro) {
        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "erro_servidor");
            dados.put("operacao_enviada", operacaoEnviada);
            dados.put("info", motivoErro);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(dados);

            enviarMensagemSemResposta(json);

        } catch (Exception e) {
            System.err.println("Erro ao construir o relatório de erro do servidor: " + e.getMessage());
        }
    }
    public void enviarMensagemSemResposta(String json) {
        try {
            // Apenas envia a mensagem para o servidor.
            out.println(json);
            System.out.println("Cliente (report) enviou: " + json);
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem sem resposta: " + e.getMessage());
        }
    }

    public boolean conectarComServidorGUI(String serverIP, int serverPort) {
        try {
            socket = new Socket(serverIP, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Enviar operação conectar
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "conectar");

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(dados);

            String resposta = enviarMensagem(json);

            return resposta != null && resposta.contains("\"status\":true");

        } catch (Exception e) {
            System.err.println("Erro de conexão: " + e.getMessage());
            return false;
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