package servidor;

import validator.Validator;
import database.BancoDados;
import java.net.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import validator.Validator;
import java.util.HashMap;
//final
public class Servidor extends Thread {

    protected Socket clientSocket;

    public static void main(String[] args) throws IOException {

        // Inicializar banco de dados
        BancoDados.criarTabelas();
        ServerSocket serverSocket = null;

        System.out.println("Qual porta o servidor deve usar? ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int porta = Integer.parseInt(br.readLine());

        System.out.println("Servidor carregado na porta " + porta);
        System.out.println("Aguardando conexao....\n ");

        try {
            serverSocket = new ServerSocket(porta);  // instancia o socket do servidor na porta especificada
            System.out.println("Criado Socket de Conexao.\n");
            try {
                while (true) {
                    new Servidor(serverSocket.accept());
                    System.out.println("Accept ativado. Esperando por uma conexao...\n");
                }
            } catch (IOException e) {
                System.err.println("Accept falhou!");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Nao foi possivel ouvir a porta " + porta);
            System.exit(1);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Nao foi possivel fechar a porta " + porta);
                System.exit(1);
            }
        }
    }

    // Constructor
    private Servidor(Socket clientSoc) {
        clientSocket = clientSoc;
        start();
    }

    /**
     * Override java.language.Thread
     */
    @Override
    public void run() {
        System.out.println("Nova thread de comunicacao iniciada.\n");

        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            ProcessadorMensagens processador = new ProcessadorMensagens(clientSocket);
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                try {

                    System.out.println("Servidor recebeu: " + inputLine);
                    // Validar mensagem do cliente
                    //Validator.validateClient(inputLine);
                    String resposta = processador.processarMensagem(inputLine);

                    // Se resposta for null, encerrar conexão
                    if (resposta == null) {
                        System.out.println("Cliente enviou mensagem sem operacao. Encerrando conexão.");
                        break;
                    }



                    System.out.println("Servidor enviou: " + resposta);
                    out.println(resposta);

                } catch (Exception e) {
                    // Apenas erros críticos de comunicação
                    System.err.println("Erro de comunicação: " + e.getMessage());
                    String respostaErro = "{\"operacao\":\"erro\",\"status\":false,\"info\":\"Erro de comunicação\"}";
                    out.println(respostaErro);
                    break; // Encerra conexão
                }

            }

        } catch (IOException e) {
            System.err.println("Erro de comunicação: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }
}
