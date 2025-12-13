package cliente;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
//final
public class ClienteGUI extends JFrame {
    // Serviços e estado de sessão
    private final Cliente cliente;
    private final ServicoUsuario servicoUsuario;
    private String token;
    private UsuarioInfo usuarioAtual;

    // Contêiner e navegação
    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    // Telas
    private ConexaoPanel conexaoPanel;
    private AutenticacaoPanel authPanel;
    private PrincipalPanel principalPanel;
    private ExtratoPanel extratoPanel;

    // Logs compartilhados
    private final JTextArea areaLog = new JTextArea(8, 80);

    // Utilidades
    private final ObjectMapper mapper = new ObjectMapper() {{
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }};
    public static void main(String[] args) {
        // Modo headless: java -Dcliente.headless=true ...
        boolean headless = Boolean.getBoolean("cliente.headless");
        if (headless) {
            Cliente cliente = new Cliente();
            if (cliente.conectarComServidorGUI("127.0.0.1", 12345)) {
                InterfaceUsuario cli = new InterfaceUsuario(cliente);
                cli.iniciarMenu();
            } else {
                System.out.println("Falha ao conectar no modo headless.");
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new ClienteGUI().setVisible(true);
        });
    }

    public ClienteGUI() {
        super("Sistema Bancário - Cliente GUI");
        this.cliente = new Cliente();
        this.servicoUsuario = new ServicoUsuario(cliente);
        //this.mapper = new ObjectMapper();
        // Configurar para ignorar propriedades extras
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        configurarLogs();
        montarUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 700);
        setLocationRelativeTo(null);
    }

    // Redireciona System.out/err para a área de logs
    private void configurarLogs() {
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.BLACK);
        areaLog.setForeground(Color.GREEN);

        // OutputStream que acumula bytes e decodifica como UTF-8 ao encontrar \n
        class LogOutputStream extends OutputStream {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            private final String prefix;
            LogOutputStream(String prefix) { this.prefix = prefix == null ? "" : prefix; }
            private void flushLine() {
                if (buffer.size() == 0) return;
                String s = new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                buffer.reset();
                if (!s.trim().isEmpty()) appendLog(prefix.isEmpty() ? s : (prefix + s));
            }
            @Override public synchronized void write(int b) throws IOException {
                if (b == '\n') flushLine(); else buffer.write(b);
            }
            @Override public synchronized void write(byte[] b, int off, int len) throws IOException {
                for (int i = off; i < off + len; i++) write(b[i]);
            }
            @Override public synchronized void flush() throws IOException { flushLine(); }
            @Override public synchronized void close() throws IOException { flushLine(); }
        }

        try {
            PrintStream outPs = new PrintStream(new LogOutputStream(""), true, "UTF-8");
            PrintStream errPs = new PrintStream(new LogOutputStream("ERRO: "), true, "UTF-8");
            System.setOut(outPs);
            System.setErr(errPs);
        } catch (Exception ignore) {
            // Fallback simples caso a plataforma não suporte encoding explícito
            System.setOut(new PrintStream(new LogOutputStream("")));
            System.setErr(new PrintStream(new LogOutputStream("ERRO: ")));
        }
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String ts = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            areaLog.append("[" + ts + "] " + msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private void montarUI() {
        conexaoPanel = new ConexaoPanel();
        authPanel = new AutenticacaoPanel();
        principalPanel = new PrincipalPanel();
        extratoPanel = new ExtratoPanel();

        container.add(conexaoPanel, "conexao");
        container.add(authPanel, "auth");
        container.add(principalPanel, "principal");
        container.add(extratoPanel, "extrato");

        JScrollPane logScroll = new JScrollPane(areaLog);
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs de Comunicação"));

        JPanel root = new JPanel(new BorderLayout());
        root.add(container, BorderLayout.CENTER);
        root.add(logScroll, BorderLayout.SOUTH);
        setContentPane(root);

        cards.show(container, "conexao");
    }

    private void mostrarTela(String nome) { cards.show(container, nome); }

    // Conversão de datas dd/MM/yyyy -> ISO com Z
    private String formatarDataParaISO(String dataInput, boolean inicioDoDia) {
        DateTimeFormatter fmtIn = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate d = LocalDate.parse(dataInput, fmtIn);
        LocalDateTime ldt = inicioDoDia ? d.atStartOfDay() : d.atTime(23,59,59);
        return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
    }

    // Busca dados do usuário (JSON) direto do servidor para preencher a UI
    private UsuarioInfo carregarUsuarioAtual() {
        if (token == null) return null;
        try {
            Map<String, String> dados = new HashMap<>();
            dados.put("operacao", "usuario_ler");
            dados.put("token", token);
            String json = mapper.writeValueAsString(dados);
            String resp = cliente.enviarMensagem(json);
            if (resp == null) return null;
            JsonNode root = mapper.readTree(resp);
            if (!root.path("status").asBoolean()) return null;
            JsonNode u = root.path("usuario");
            UsuarioInfo info = new UsuarioInfo();
            info.nome = u.path("nome").asText("");
            info.cpf = u.path("cpf").asText("");
            info.saldo = u.path("saldo").asDouble(0.0);
            info.token = token;
            return info;
        } catch (Exception e) {
            appendLog("Falha ao carregar usuário: " + e.getMessage());
            return null;
        }
    }

    // ===== Telas =====
    // 1) Conexão ao servidor
    private class ConexaoPanel extends JPanel {
        private final JTextField ipField = new JTextField("127.0.0.1", 12);
        private final JTextField portaField = new JTextField("12345", 6);
        private final JButton btnConectar = new JButton("Conectar");

        ConexaoPanel() {
            super(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;
            gbc.gridx = 0; gbc.gridy = y; add(new JLabel("IP do Servidor:"), gbc);
            gbc.gridx = 1; add(ipField, gbc); y++;
            gbc.gridx = 0; gbc.gridy = y; add(new JLabel("Porta:"), gbc);
            gbc.gridx = 1; add(portaField, gbc); y++;
            gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; add(btnConectar, gbc);

            btnConectar.addActionListener(e -> conectar());
            KeyAdapter enter = new KeyAdapter() { @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode()==KeyEvent.VK_ENTER) conectar(); } };
            ipField.addKeyListener(enter); portaField.addKeyListener(enter);
        }

        private void conectar() {
            String ip = ipField.getText().trim();
            String portaStr = portaField.getText().trim();
            if (ip.isEmpty() || portaStr.isEmpty()) { appendLog("Informe IP e Porta."); return; }
            try {
                int porta = Integer.parseInt(portaStr);
                btnConectar.setEnabled(false);
                new Thread(() -> {
                    boolean ok = cliente.conectarComServidorGUI(ip, porta);
                    SwingUtilities.invokeLater(() -> {
                        btnConectar.setEnabled(true);
                        if (ok) { appendLog("Conectado ao servidor."); mostrarTela("auth"); }
                        else { appendLog("Falha na conexão."); JOptionPane.showMessageDialog(this, "Falha na conexão.", "Erro", JOptionPane.ERROR_MESSAGE); }
                    });
                }).start();
            } catch (NumberFormatException ex) {
                appendLog("Porta inválida.");
            }
        }
    }

    // 2) Autenticação (Login/Cadastro)
    private class AutenticacaoPanel extends JPanel {
        private final JRadioButton rbLogin = new JRadioButton("Login", true);
        private final JRadioButton rbCadastro = new JRadioButton("Cadastrar");
        private final JTextField cpfField = new JTextField(14);
        private final JTextField nomeField = new JTextField(18);
        private final JPasswordField senhaField = new JPasswordField(14);
        private final JButton btnEnviar = new JButton("Enviar");

        AutenticacaoPanel() {
            super(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            ButtonGroup bg = new ButtonGroup(); bg.add(rbLogin); bg.add(rbCadastro);
            JPanel modo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            modo.add(new JLabel("Modo:")); modo.add(rbLogin); modo.add(rbCadastro);

            int y = 0;
            gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2; add(modo, gbc); y++;
            gbc.gridwidth=1;
            gbc.gridx=0; gbc.gridy=y; add(new JLabel("CPF:"), gbc);
            gbc.gridx=1; add(cpfField, gbc); y++;
            gbc.gridx=0; gbc.gridy=y; add(new JLabel("Nome (cadastro):"), gbc);
            gbc.gridx=1; add(nomeField, gbc); y++;
            gbc.gridx=0; gbc.gridy=y; add(new JLabel("Senha:"), gbc);
            gbc.gridx=1; add(senhaField, gbc); y++;
            gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER; add(btnEnviar, gbc);

            nomeField.setEnabled(false);
            rbCadastro.addActionListener(e -> nomeField.setEnabled(true));
            rbLogin.addActionListener(e -> nomeField.setEnabled(false));

            btnEnviar.addActionListener(e -> enviar());
        }

        private void enviar() {
            String cpf = cpfField.getText().trim();
            String nome = nomeField.getText().trim();
            String senha = new String(senhaField.getPassword()).trim();
            if (cpf.isEmpty() || senha.isEmpty()) {
                appendLog("Preencha CPF e Senha.");
                return;
            }

            btnEnviar.setEnabled(false);
            new Thread(() -> {
                try {
                    if (rbCadastro.isSelected()) {
                        boolean ok = servicoUsuario.cadastrarUsuario(nome, cpf, senha);
                        SwingUtilities.invokeLater(() -> {
                            appendLog(ok ? "Cadastro realizado." : "Falha no cadastro.");
                            if (ok) {
                                JOptionPane.showMessageDialog(this, "Cadastro OK. Faça login.", "Info", JOptionPane.INFORMATION_MESSAGE);
                            }
                        });
                    } else {
                        String tk = servicoUsuario.fazerLogin(cpf, senha);
                        if (tk != null) {
                            token = tk;
                            appendLog("Login OK. Token atribuído.");
                            UsuarioInfo info = carregarUsuarioAtual();
                            usuarioAtual = info;
                            SwingUtilities.invokeLater(() -> {
                                principalPanel.preencherUsuario(info);
                                mostrarTela("principal");
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                appendLog("Falha no login.");
                            });
                        }
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        appendLog("Erro durante operação: " + e.getMessage());
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        btnEnviar.setEnabled(true);
                        // Limpar campos de senha por segurança
                        senhaField.setText("");
                    });
                }
            }).start();
        }
    }

    // 3) Tela Principal
    private class PrincipalPanel extends JPanel {
        private final JLabel lbNome = new JLabel("-");
        private final JLabel lbCpf = new JLabel("-");
        private final JLabel lbToken = new JLabel("-");
        private final JLabel lbSaldo = new JLabel("R$ 0,00");

        private final JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        private final JPanel painelDetalhes = new JPanel(new CardLayout());

        private final JPanel pnlAtualizar = new JPanel(new GridBagLayout());
        private final JTextField nomeNovo = new JTextField(18);
        private final JPasswordField senhaNova = new JPasswordField(14);
        private final JButton btnConfAtualizar = new JButton("Confirmar Atualização");

        private final JPanel pnlDeposito = new JPanel(new FlowLayout(FlowLayout.LEFT));
        private final JTextField valorDeposito = new JTextField(10);
        private final JButton btnConfDeposito = new JButton("Confirmar Depósito");

        private final JPanel pnlTransfer = new JPanel(new GridBagLayout());
        private final JTextField valorTransfer = new JTextField(10);
        private final JTextField cpfDestino = new JTextField(14);
        private final JButton btnConfTransfer = new JButton("Confirmar Transferência");

        PrincipalPanel() {
            super(new BorderLayout());

            JPanel header = new JPanel(new GridLayout(2,2,10,6));
            header.setBorder(BorderFactory.createTitledBorder("Dados do Usuário"));
            header.add(new JLabel("Nome:")); header.add(lbNome);
            header.add(new JLabel("CPF:")); header.add(lbCpf);
            JPanel header2 = new JPanel(new GridLayout(2,2,10,6));
            header2.add(new JLabel("Token:")); header2.add(lbToken);
            header2.add(new JLabel("Saldo:")); header2.add(lbSaldo);
            JPanel topo = new JPanel(new GridLayout(1,2,10,6));
            topo.add(header); topo.add(header2);
            add(topo, BorderLayout.NORTH);

            JButton btnAtualizar = new JButton("Atualizar Usuário");
            JButton btnExtrato = new JButton("Extrato");
            JButton btnDepositar = new JButton("Depositar");
            JButton btnTransferir = new JButton("Transferir");
            JButton btnLogout = new JButton("Logout");
            JButton btnDeletar = new JButton("Deletar Usuário");
            JButton btnAtualizarDados = new JButton("Atualizar Saldo");
            painelAcoes.add(btnAtualizar); painelAcoes.add(btnExtrato); painelAcoes.add(btnDepositar);
            painelAcoes.add(btnTransferir);painelAcoes.add(btnAtualizarDados); painelAcoes.add(btnLogout); painelAcoes.add(btnDeletar);
            add(painelAcoes, BorderLayout.CENTER);

            montarPaineisDetalhes();
            add(painelDetalhes, BorderLayout.SOUTH);

            btnAtualizar.addActionListener(e -> mostrarDetalhe("atualizar"));
            btnDepositar.addActionListener(e -> mostrarDetalhe("depositar"));
            btnTransferir.addActionListener(e -> mostrarDetalhe("transferir"));
            btnExtrato.addActionListener(e -> mostrarTela("extrato"));
            btnLogout.addActionListener(e -> doLogout());
            btnDeletar.addActionListener(e -> doDeletar());

            btnConfAtualizar.addActionListener(e -> doAtualizar());
            btnConfDeposito.addActionListener(e -> doDepositar());
            btnConfTransfer.addActionListener(e -> doTransferir());

            btnAtualizarDados.addActionListener(e -> doAtualizarDados());
        }

        private void montarPaineisDetalhes() {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5); gbc.anchor = GridBagConstraints.WEST;
            int y=0; gbc.gridx=0; gbc.gridy=y; pnlAtualizar.add(new JLabel("Novo Nome:"), gbc);
            gbc.gridx=1; pnlAtualizar.add(nomeNovo, gbc); y++;
            gbc.gridx=0; gbc.gridy=y; pnlAtualizar.add(new JLabel("Nova Senha:"), gbc);
            gbc.gridx=1; pnlAtualizar.add(senhaNova, gbc); y++;
            gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER; pnlAtualizar.add(btnConfAtualizar, gbc);

            pnlDeposito.add(new JLabel("Valor:")); pnlDeposito.add(valorDeposito); pnlDeposito.add(btnConfDeposito);

            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.insets = new Insets(5,5,5,5); gbc2.anchor = GridBagConstraints.WEST;
            int y2=0; gbc2.gridx=0; gbc2.gridy=y2; pnlTransfer.add(new JLabel("Valor:"), gbc2);
            gbc2.gridx=1; pnlTransfer.add(valorTransfer, gbc2); y2++;
            gbc2.gridx=0; gbc2.gridy=y2; pnlTransfer.add(new JLabel("Destinatário (CPF):"), gbc2);
            gbc2.gridx=1; pnlTransfer.add(cpfDestino, gbc2); y2++;
            gbc2.gridx=0; gbc2.gridy=y2; gbc2.gridwidth=2; gbc2.anchor=GridBagConstraints.CENTER; pnlTransfer.add(btnConfTransfer, gbc2);

            painelDetalhes.setBorder(BorderFactory.createTitledBorder("Ações"));
            painelDetalhes.add(new JPanel(), "vazio");
            painelDetalhes.add(pnlAtualizar, "atualizar");
            painelDetalhes.add(pnlDeposito, "depositar");
            painelDetalhes.add(pnlTransfer, "transferir");
            mostrarDetalhe("vazio");
        }

        private void mostrarDetalhe(String nome) { ((CardLayout) painelDetalhes.getLayout()).show(painelDetalhes, nome); }

        void preencherUsuario(UsuarioInfo info) {
            if (info == null) return;
            lbNome.setText(info.nome);
            lbCpf.setText(info.cpf);
            lbToken.setText(info.token);
            lbSaldo.setText(String.format("R$ %.2f", info.saldo));
            mostrarDetalhe("vazio");
        }
        private void doAtualizarDados() {
            new Thread(() -> {
                appendLog("Atualizando dados do usuário...");
                UsuarioInfo info = carregarUsuarioAtual();
                usuarioAtual = info; // Atualiza o estado do usuário na classe principal
                SwingUtilities.invokeLater(() -> {
                    preencherUsuario(info);
                    appendLog("Dados atualizados com sucesso.");
                });
            }).start();
        }

        private void doAtualizar() {
            String n = nomeNovo.getText().trim();
            String s = new String(senhaNova.getPassword()).trim();
            if (n.isEmpty() && s.isEmpty()) { appendLog("Nenhuma alteração."); return; }
            Map<String,String> dados = new HashMap<>();
            if (!n.isEmpty()) dados.put("nome", n);
            if (!s.isEmpty()) dados.put("senha", s);
            new Thread(() -> {
                servicoUsuario.atualizarDados(token, dados);
                UsuarioInfo info = carregarUsuarioAtual(); usuarioAtual = info;
                SwingUtilities.invokeLater(() -> preencherUsuario(info));
            }).start();
        }

        private void doDepositar() {
            String v = valorDeposito.getText().trim();
            try {
                double val = Double.parseDouble(v);
                if (val <= 0) { appendLog("Valor deve ser positivo."); return; }
                new Thread(() -> {
                    boolean ok = servicoUsuario.depositar(token, val);
                    appendLog(ok ? "Depósito OK." : "Depósito falhou.");
                    UsuarioInfo info = carregarUsuarioAtual(); usuarioAtual = info;
                    SwingUtilities.invokeLater(() -> preencherUsuario(info));
                }).start();
            } catch (NumberFormatException ex) { appendLog("Valor inválido."); }
        }

        private void doTransferir() {
            String v = valorTransfer.getText().trim();
            String cpf = cpfDestino.getText().trim();
            try {
                double val = Double.parseDouble(v);
                if (val <= 0 || cpf.isEmpty()) { appendLog("Informe valor e CPF destino."); return; }
                new Thread(() -> {
                    boolean ok = servicoUsuario.enviarDinheiro(token, cpf, val);
                    appendLog(ok ? "Transferência OK." : "Transferência falhou.");
                    UsuarioInfo info = carregarUsuarioAtual(); usuarioAtual = info;
                    SwingUtilities.invokeLater(() -> preencherUsuario(info));
                }).start();
            } catch (NumberFormatException ex) { appendLog("Valor inválido."); }
        }

        private void doLogout() {
            new Thread(() -> {
                boolean ok = servicoUsuario.fazerLogout(token);
                if (ok) { token = null; usuarioAtual = null; }
                SwingUtilities.invokeLater(() -> { appendLog(ok ? "Logout OK." : "Logout falhou."); mostrarTela("auth"); });
            }).start();
        }

        private void doDeletar() {
            int c = JOptionPane.showConfirmDialog(this, "Tem certeza que deseja deletar o usuário?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            new Thread(() -> {
                boolean ok = servicoUsuario.deletarCadastro(token);
                if (ok) { token = null; usuarioAtual = null; }
                SwingUtilities.invokeLater(() -> { appendLog(ok ? "Usuário deletado." : "Falha ao deletar."); mostrarTela("auth"); });
            }).start();
        }
    }

    // 4) Extrato de transações
    private class ExtratoPanel extends JPanel {
        private final JTextField dtIni = new JTextField(10);
        private final JTextField dtFim = new JTextField(10);
        private final JButton btnBuscar = new JButton("Buscar");
        private final JButton btnVoltar = new JButton("Voltar");
        private final JTable tabela = new JTable();

        ExtratoPanel() {
            super(new BorderLayout());

            JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            topo.setBorder(BorderFactory.createTitledBorder("Período"));
            topo.add(new JLabel("De (dd/MM/yyyy):")); topo.add(dtIni);
            topo.add(new JLabel("Até (dd/MM/yyyy):")); topo.add(dtFim);
            topo.add(btnBuscar); topo.add(btnVoltar);
            add(topo, BorderLayout.NORTH);

            JScrollPane sp = new JScrollPane(tabela);
            tabela.setFillsViewportHeight(true);
            add(sp, BorderLayout.CENTER);

            LocalDate hoje = LocalDate.now();
            dtFim.setText(hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dtIni.setText(hoje.minusDays(7).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            btnBuscar.addActionListener(e -> buscar());
            btnVoltar.addActionListener(e -> mostrarTela("principal"));
        }

        private void buscar() {
            if (token == null) { appendLog("Faça login para ver extrato."); return; }
            String ini = dtIni.getText().trim();
            String fim = dtFim.getText().trim();
            try {
                String dataInicial = formatarDataParaISO(ini, true);
                String dataFinal = formatarDataParaISO(fim, false);
                new Thread(() -> {
                    DefaultTableModel model = carregarTransacoes(dataInicial, dataFinal);
                    SwingUtilities.invokeLater(() -> tabela.setModel(model));
                }).start();
            } catch (Exception ex) {
                appendLog("Datas inválidas. Use dd/MM/yyyy.");
            }
        }

        private DefaultTableModel carregarTransacoes(String dataInicial, String dataFinal) {
            String[] cols = {"Data", "Tipo", "Valor", "Usuário"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
            try {
                Map<String,String> dados = new HashMap<>();
                dados.put("operacao", "transacao_ler");
                dados.put("token", token);
                dados.put("data_inicial", dataInicial);
                dados.put("data_final", dataFinal);
                String json = mapper.writeValueAsString(dados);
                String resp = cliente.enviarMensagem(json);
                if (resp == null) return model;
                JsonNode root = mapper.readTree(resp);
                if (!root.path("status").asBoolean()) return model;
                JsonNode arr = root.path("transacoes");
                String meuCpf = usuarioAtual != null ? usuarioAtual.cpf : "";
                for (JsonNode t : arr) {
                    String data = t.path("criado_em").asText("");
                    double valor = t.path("valor_enviado").asDouble(0.0);
                    JsonNode enviador = t.path("usuario_enviador");
                    JsonNode recebedor = t.path("usuario_recebedor");
                    String cpfEnv = enviador.path("cpf").asText("");
                    String cpfRec = recebedor.path("cpf").asText("");
                    boolean souEnv = cpfEnv.equals(meuCpf);
                    String tipo = souEnv ? "Enviado" : "Recebido";
                    String contraparte = souEnv ? recebedor.path("nome").asText("") + " ("+cpfRec+")" : enviador.path("nome").asText("") + " ("+cpfEnv+")";
                    model.addRow(new Object[]{data, tipo, String.format("R$ %.2f", valor), contraparte});
                }
            } catch (Exception e) {
                appendLog("Falha ao carregar transações: " + e.getMessage());
            }
            return model;
        }
    }

    // DTO simples
    private static class UsuarioInfo { String nome; String cpf; String token; double saldo; }
}

