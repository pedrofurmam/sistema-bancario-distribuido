package servidor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import autenticador.Token;
import dao.UsuarioDAO;
import modelo.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ServidorGUI extends JFrame {
    // UI
    private final JTextField tfIP = new JTextField(18);
    private final JTextField tfPorta = new JTextField("12345", 6);
    private final JButton btnIniciar = new JButton("Iniciar Servidor");
    private final JButton btnParar = new JButton("Parar");
    private final JLabel lbStatus = new JLabel("Parado");

    private final DefaultTableModel clientesModel = new DefaultTableModel(new Object[]{"Nome", "CPF", "Token"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabelaClientes = new JTable(clientesModel);

    private final JTextArea areaLog = new JTextArea(10, 100);

    // Servidor
    private ServerThread serverThread;
    private final List<ClientThread> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new ServidorGUI().setVisible(true);
        });
    }

    public ServidorGUI() {
        super("Servidor - GUI");
        montarUI();
        configurarAcoes();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        tfIP.setText(detectarIPLocal());
        btnParar.setEnabled(false);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.BLACK);
        areaLog.setForeground(new Color(0, 220, 0));
    }

    private void montarUI() {
        JPanel topo = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;
        int y=0;
        gbc.gridx=0; gbc.gridy=y; topo.add(new JLabel("IP:"), gbc);
        gbc.gridx=1; topo.add(tfIP, gbc);
        gbc.gridx=2; topo.add(new JLabel("Porta:"), gbc);
        gbc.gridx=3; topo.add(tfPorta, gbc);
        gbc.gridx=4; topo.add(btnIniciar, gbc);
        gbc.gridx=5; topo.add(btnParar, gbc);
        gbc.gridx=6; topo.add(new JLabel("Status:"), gbc);
        gbc.gridx=7; topo.add(lbStatus, gbc);

        JScrollPane spClientes = new JScrollPane(tabelaClientes);
        spClientes.setBorder(BorderFactory.createTitledBorder("Clientes Conectados"));
        tabelaClientes.setFillsViewportHeight(true);

        JScrollPane spLog = new JScrollPane(areaLog);
        spLog.setBorder(BorderFactory.createTitledBorder("Logs de Comunicação"));
        spLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel root = new JPanel(new BorderLayout());
        root.add(topo, BorderLayout.NORTH);
        root.add(spClientes, BorderLayout.CENTER);
        root.add(spLog, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void configurarAcoes() {
        btnIniciar.addActionListener(e -> iniciarServidor());
        btnParar.addActionListener(e -> pararServidor());
    }

    private void iniciarServidor() {
        if (serverThread != null && serverThread.isAlive()) return;
        int porta;
        try { porta = Integer.parseInt(tfPorta.getText().trim()); }
        catch (NumberFormatException ex) { log("Porta inválida."); return; }

        try {


            serverThread = new ServerThread(porta);
            serverThread.start();
            btnIniciar.setEnabled(false);
            btnParar.setEnabled(true);
            lbStatus.setText("Rodando");
            log("Servidor iniciado na porta " + porta);
        } catch (IOException ex) {
            log("Falha ao iniciar servidor: " + ex.getMessage());
        }
    }

    private void pararServidor() {
        if (serverThread != null) {
            serverThread.shutdown();
            serverThread = null;
        }
        for (ClientThread ct : clientes) ct.shutdown();
        clientes.clear();
        SwingUtilities.invokeLater(() -> {
            // Limpa a tabela
            while (clientesModel.getRowCount() > 0) clientesModel.removeRow(0);
            btnIniciar.setEnabled(true);
            btnParar.setEnabled(false);
            lbStatus.setText("Parado");
        });
        log("Servidor parado.");
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
            areaLog.append("[" + ts + "] " + msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private String detectarIPLocal() {
        try {
            // Tenta obter um IP não-loopback
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // Thread que aceita conexões
    private class ServerThread extends Thread {
        private final ServerSocket serverSocket;
        private volatile boolean rodando = true;

        ServerThread(int porta) throws IOException {
            this.serverSocket = new ServerSocket(porta);
            setName("Servidor-Accept-Thread");
        }

        @Override public void run() {
            log("Aguardando conexões...");
            while (rodando) {
                try {
                    Socket s = serverSocket.accept();
                    ClientThread ct = new ClientThread(s);
                    clientes.add(ct);
                    ct.start();
                    log("Cliente conectado: " + s.getRemoteSocketAddress());
                } catch (SocketException se) {
                    break;
                } catch (IOException e) {
                    if (rodando) log("Erro no accept: " + e.getMessage());
                }
            }
            log("Loop de accept finalizado.");
        }

        void shutdown() {
            rodando = false;
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    // Thread de cliente que processa mensagens
    private class ClientThread extends Thread {
        private final Socket socket;
        private volatile boolean rodando = true;
        private final ObjectMapper mapper = new ObjectMapper();
        private String usuarioCpfAtual; // cpf do usuário logado nesta conexão

        ClientThread(Socket socket) {
            this.socket = socket;
            setName("Cliente-" + socket.getRemoteSocketAddress());
        }

        @Override public void run() {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                ProcessadorMensagens processador = new ProcessadorMensagens(socket);
                String linha;
                while (rodando && (linha = in.readLine()) != null) {
                    try {
                        log("Servidor recebeu de " + socket.getRemoteSocketAddress() + ": " + linha);

                        // Parse da requisição para extrair operacao e dados importantes
                        String operacaoReq = null;
                        String cpfReq = null;
                        String tokenReq = null;
                        try {
                            JsonNode nodeReq = mapper.readTree(linha);
                            if (nodeReq != null && nodeReq.has("operacao")) {
                                operacaoReq = nodeReq.get("operacao").asText();
                                if ("usuario_login".equals(operacaoReq)) {
                                    if (nodeReq.has("cpf")) cpfReq = nodeReq.get("cpf").asText();
                                }
                                if (nodeReq.has("token")) tokenReq = nodeReq.get("token").asText();
                            }
                        } catch (Exception ignore) {}

                        // Para logout/deletar, tentar resolver CPF a partir do token ANTES do processamento (token ainda válido)
                        String cpfDoTokenAntes = null;
                        if (("usuario_logout".equals(operacaoReq) || "usuario_deletar".equals(operacaoReq)) && tokenReq != null) {
                            try { cpfDoTokenAntes = Token.validarToken(tokenReq, socket); } catch (Exception ignore) {}
                        }

                        String resp = processador.processarMensagem(linha);
                        if (resp == null) {
                            log("Mensagem sem operação. Encerrando cliente " + socket.getRemoteSocketAddress());
                            break;
                        }

                        out.println(resp);
                        log("Servidor enviou a " + socket.getRemoteSocketAddress() + ": " + resp);

                        // Tratar efeitos colaterais para a lista de usuários logados
                        try {
                            JsonNode nodeResp = mapper.readTree(resp);
                            String operacao = nodeResp.path("operacao").asText("");
                            boolean status = nodeResp.path("status").asBoolean(false);

                            if (status && "usuario_login".equals(operacao) && cpfReq != null) {
                                String token = nodeResp.path("token").asText("");
                                String nome = obterNomePorCpf(cpfReq);
                                usuarioCpfAtual = cpfReq;
                                upsertUsuarioLogado(cpfReq, nome, token);
                            }

                            if (status && ("usuario_logout".equals(operacao) || "usuario_deletar".equals(operacao))) {
                                String cpfRem = cpfDoTokenAntes;
                                if (cpfRem == null && usuarioCpfAtual != null) cpfRem = usuarioCpfAtual;
                                if (cpfRem != null) {
                                    removerUsuarioLogadoPorCpf(cpfRem);
                                    if (cpfRem.equals(usuarioCpfAtual)) {
                                        usuarioCpfAtual = null;
                                    }
                                }
                            }
                        } catch (Exception ignore) {}

                    } catch (Exception ex) {
                        log("Erro de comunicação com " + socket.getRemoteSocketAddress() + ": " + ex.getMessage());
                        String respostaErro = "{\"operacao\":\"erro\",\"status\":false,\"info\":\"Erro de comunicação\"}";
                        out.println(respostaErro);
                        break;
                    }
                }
            } catch (IOException e) {
                log("IO com cliente " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                // Na desconexão, remover usuário logado desta conexão, se houver
                if (usuarioCpfAtual != null) removerUsuarioLogadoPorCpf(usuarioCpfAtual);
                clientes.remove(this);
                log("Cliente desconectado: " + socket.getRemoteSocketAddress());
            }
        }

        void shutdown() {
            rodando = false;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // Insere/atualiza usuário logado na tabela (garante apenas 1 linha por CPF)
    private void upsertUsuarioLogado(String cpf, String nome, String token) {
        SwingUtilities.invokeLater(() -> {
            // Remove entradas com mesmo CPF
            for (int i = clientesModel.getRowCount() - 1; i >= 0; i--) {
                if (Objects.equals(clientesModel.getValueAt(i, 1), cpf)) {
                    clientesModel.removeRow(i);
                }
            }
            clientesModel.addRow(new Object[]{nome == null ? "" : nome, cpf, token});
        });
    }

    // Remove usuário logado pelo CPF
    private void removerUsuarioLogadoPorCpf(String cpf) {
        SwingUtilities.invokeLater(() -> {
            for (int i = clientesModel.getRowCount() - 1; i >= 0; i--) {
                if (Objects.equals(clientesModel.getValueAt(i, 1), cpf)) {
                    clientesModel.removeRow(i);
                }
            }
        });
    }

    private String obterNomePorCpf(String cpf) {
        try {
            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.buscarPorCpf(cpf);
            return u != null ? u.getNome() : "";
        } catch (Exception e) {
            log("Falha ao buscar nome por CPF: " + e.getMessage());
            return "";
        }
    }
}
