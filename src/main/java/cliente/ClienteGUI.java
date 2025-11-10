package cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClienteGUI extends JFrame {
    private Cliente cliente;
    private ServicoUsuario servicoUsuario;
    private JTextArea areaLog;
    private JTextField campoInput;
    private JButton botaoEnviar;
    private JPanel painelPrincipal;
    private JPanel painelBotoes;
    private String token = null;

    // Componentes para formulários
    private JPanel painelFormulario;
    private CardLayout cardLayout;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ClienteGUI().setVisible(true);
        });
    }

    public ClienteGUI() {
        this.cliente = new Cliente();
        this.servicoUsuario = new ServicoUsuario(cliente);

        initializeComponents();
        setupLayout();
        setupEventListeners();

        setTitle("Sistema Bancário - Interface Gráfica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Conectar automaticamente ao servidor
        conectarAoServidor();
    }

    private void initializeComponents() {
        areaLog = new JTextArea(15, 50);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.BLACK);
        areaLog.setForeground(Color.GREEN);

        campoInput = new JTextField(40);
        botaoEnviar = new JButton("Enviar");

        painelPrincipal = new JPanel(new BorderLayout());
        painelBotoes = new JPanel(new GridLayout(0, 2, 5, 5));

        cardLayout = new CardLayout();
        painelFormulario = new JPanel(cardLayout);

        criarFormularios();
        atualizarBotoes();
    }

    private void criarFormularios() {
        // Formulário de Login
        JPanel formLogin = criarFormularioLogin();
        painelFormulario.add(formLogin, "LOGIN");

        // Formulário de Cadastro
        JPanel formCadastro = criarFormularioCadastro();
        painelFormulario.add(formCadastro, "CADASTRO");

        // Formulário de Depósito
        JPanel formDeposito = criarFormularioDeposito();
        painelFormulario.add(formDeposito, "DEPOSITO");

        // Formulário de Transferência
        JPanel formTransferencia = criarFormularioTransferencia();
        painelFormulario.add(formTransferencia, "TRANSFERENCIA");

        // Formulário de Atualização
        JPanel formAtualizacao = criarFormularioAtualizacao();
        painelFormulario.add(formAtualizacao, "ATUALIZACAO");

        // Formulário de Extrato
        JPanel formExtrato = criarFormularioExtrato();
        painelFormulario.add(formExtrato, "EXTRATO");

        // Panel vazio
        JPanel painelVazio = new JPanel();
        painelVazio.add(new JLabel("Selecione uma operação"));
        painelFormulario.add(painelVazio, "VAZIO");

        cardLayout.show(painelFormulario, "VAZIO");
    }

    private JPanel criarFormularioLogin() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoCpf = new JTextField(15);
        JPasswordField campoSenha = new JPasswordField(15);
        JButton botaoLogin = new JButton("Fazer Login");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("CPF:"), gbc);
        gbc.gridx = 1;
        painel.add(campoCpf, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        painel.add(campoSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        painel.add(botaoLogin, gbc);

        botaoLogin.addActionListener(e -> {
            String cpf = campoCpf.getText().trim();
            String senha = new String(campoSenha.getPassword()).trim();

            if (!cpf.isEmpty() && !senha.isEmpty()) {
                token = servicoUsuario.fazerLogin(cpf, senha);
                if (token != null) {
                    adicionarLog("Login realizado com sucesso!");
                    atualizarBotoes();
                    cardLayout.show(painelFormulario, "VAZIO");
                    campoCpf.setText("");
                    campoSenha.setText("");
                } else {
                    adicionarLog("Falha no login!");
                }
            } else {
                adicionarLog("Preencha todos os campos!");
            }
        });

        return painel;
    }

    private JPanel criarFormularioCadastro() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoNome = new JTextField(20);
        JTextField campoCpf = new JTextField(15);
        JPasswordField campoSenha = new JPasswordField(15);
        JButton botaoCadastrar = new JButton("Cadastrar");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        painel.add(campoNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("CPF:"), gbc);
        gbc.gridx = 1;
        painel.add(campoCpf, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        painel.add(campoSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        painel.add(botaoCadastrar, gbc);

        botaoCadastrar.addActionListener(e -> {
            String nome = campoNome.getText().trim();
            String cpf = campoCpf.getText().trim();
            String senha = new String(campoSenha.getPassword()).trim();

            if (!nome.isEmpty() && !cpf.isEmpty() && !senha.isEmpty()) {
                boolean sucesso = servicoUsuario.cadastrarUsuario(nome, cpf, senha);
                if (sucesso) {
                    adicionarLog("Cadastro realizado com sucesso!");
                    cardLayout.show(painelFormulario, "VAZIO");
                    campoNome.setText("");
                    campoCpf.setText("");
                    campoSenha.setText("");
                } else {
                    adicionarLog("Falha no cadastro!");
                }
            } else {
                adicionarLog("Preencha todos os campos!");
            }
        });

        return painel;
    }

    private JPanel criarFormularioDeposito() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoValor = new JTextField(15);
        JButton botaoDepositar = new JButton("Depositar");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("Valor (R$):"), gbc);
        gbc.gridx = 1;
        painel.add(campoValor, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        painel.add(botaoDepositar, gbc);

        botaoDepositar.addActionListener(e -> {
            String valorStr = campoValor.getText().trim();

            if (!valorStr.isEmpty()) {
                try {
                    double valor = Double.parseDouble(valorStr);
                    boolean sucesso = servicoUsuario.depositar(token, valor);
                    if (sucesso) {
                        adicionarLog("Depósito realizado com sucesso!");
                        cardLayout.show(painelFormulario, "VAZIO");
                        campoValor.setText("");
                    } else {
                        adicionarLog("Falha no depósito!");
                    }
                } catch (NumberFormatException ex) {
                    adicionarLog("Valor inválido!");
                }
            } else {
                adicionarLog("Informe o valor!");
            }
        });

        return painel;
    }

    private JPanel criarFormularioTransferencia() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoCpfDestino = new JTextField(15);
        JTextField campoValor = new JTextField(15);
        JButton botaoTransferir = new JButton("Transferir");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("CPF Destino:"), gbc);
        gbc.gridx = 1;
        painel.add(campoCpfDestino, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("Valor (R$):"), gbc);
        gbc.gridx = 1;
        painel.add(campoValor, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        painel.add(botaoTransferir, gbc);

        botaoTransferir.addActionListener(e -> {
            String cpfDestino = campoCpfDestino.getText().trim();
            String valorStr = campoValor.getText().trim();

            if (!cpfDestino.isEmpty() && !valorStr.isEmpty()) {
                try {
                    double valor = Double.parseDouble(valorStr);
                    boolean sucesso = servicoUsuario.enviarDinheiro(token, cpfDestino, valor);
                    if (sucesso) {
                        adicionarLog("Transferência realizada com sucesso!");
                        cardLayout.show(painelFormulario, "VAZIO");
                        campoCpfDestino.setText("");
                        campoValor.setText("");
                    } else {
                        adicionarLog("Falha na transferência!");
                    }
                } catch (NumberFormatException ex) {
                    adicionarLog("Valor inválido!");
                }
            } else {
                adicionarLog("Preencha todos os campos!");
            }
        });

        return painel;
    }

    private JPanel criarFormularioAtualizacao() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoNome = new JTextField(20);
        JPasswordField campoSenha = new JPasswordField(15);
        JButton botaoAtualizar = new JButton("Atualizar");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("Novo Nome:"), gbc);
        gbc.gridx = 1;
        painel.add(campoNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("Nova Senha:"), gbc);
        gbc.gridx = 1;
        painel.add(campoSenha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        painel.add(botaoAtualizar, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        painel.add(new JLabel("(Deixe em branco para não alterar)"), gbc);

        botaoAtualizar.addActionListener(e -> {
            String nome = campoNome.getText().trim();
            String senha = new String(campoSenha.getPassword()).trim();

            Map<String, String> dados = new HashMap<>();
            if (!nome.isEmpty()) dados.put("nome", nome);
            if (!senha.isEmpty()) dados.put("senha", senha);

            if (!dados.isEmpty()) {
                servicoUsuario.atualizarDados(token, dados);
                adicionarLog("Dados atualizados!");
                cardLayout.show(painelFormulario, "VAZIO");
                campoNome.setText("");
                campoSenha.setText("");
            } else {
                adicionarLog("Nenhuma alteração solicitada!");
            }
        });

        return painel;
    }

    private JPanel criarFormularioExtrato() {
        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField campoDataInicial = new JTextField(10);
        JTextField campoDataFinal = new JTextField(10);
        JButton botaoConsultar = new JButton("Consultar Extrato");

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("Data Inicial (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1;
        painel.add(campoDataInicial, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(new JLabel("Data Final (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1;
        painel.add(campoDataFinal, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        painel.add(botaoConsultar, gbc);

        botaoConsultar.addActionListener(e -> {
            String dataInicial = campoDataInicial.getText().trim();
            String dataFinal = campoDataFinal.getText().trim();

            if (!dataInicial.isEmpty() && !dataFinal.isEmpty()) {
                try {
                    String dataInicialISO = formatarDataParaISO(dataInicial, true);
                    String dataFinalISO = formatarDataParaISO(dataFinal, false);

                    servicoUsuario.verTransacoes(token, dataInicialISO, dataFinalISO);
                    adicionarLog("Consultando extrato...");
                    cardLayout.show(painelFormulario, "VAZIO");
                    campoDataInicial.setText("");
                    campoDataFinal.setText("");

                } catch (Exception ex) {
                    adicionarLog("Formato de data inválido! Use dd/MM/yyyy");
                }
            } else {
                adicionarLog("Preencha as datas!");
            }
        });

        return painel;
    }

    private void setupLayout() {
        JScrollPane scrollPane = new JScrollPane(areaLog);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel painelInput = new JPanel(new BorderLayout());
        painelInput.add(campoInput, BorderLayout.CENTER);
        painelInput.add(botaoEnviar, BorderLayout.EAST);

        JPanel painelSuperior = new JPanel(new BorderLayout());
        painelSuperior.add(scrollPane, BorderLayout.CENTER);
        painelSuperior.add(painelInput, BorderLayout.SOUTH);

        JPanel painelInferior = new JPanel(new BorderLayout());
        painelInferior.add(painelBotoes, BorderLayout.NORTH);
        painelInferior.add(painelFormulario, BorderLayout.CENTER);

        painelPrincipal.add(painelSuperior, BorderLayout.CENTER);
        painelPrincipal.add(painelInferior, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void setupEventListeners() {
        botaoEnviar.addActionListener(e -> enviarMensagemManual());

        campoInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enviarMensagemManual();
                }
            }
        });
    }

    private void atualizarBotoes() {
        painelBotoes.removeAll();

        if (token == null) {
            // Usuário não logado
            JButton btnLogin = new JButton("Fazer Login");
            btnLogin.addActionListener(e -> cardLayout.show(painelFormulario, "LOGIN"));

            JButton btnCadastro = new JButton("Cadastrar");
            btnCadastro.addActionListener(e -> cardLayout.show(painelFormulario, "CADASTRO"));

            painelBotoes.add(btnLogin);
            painelBotoes.add(btnCadastro);

        } else {
            // Usuário logado
            JButton btnDados = new JButton("Meus Dados");
            btnDados.addActionListener(e -> {
                servicoUsuario.verMeusDados(token);
                adicionarLog("Consultando dados...");
            });

            JButton btnAtualizar = new JButton("Atualizar Dados");
            btnAtualizar.addActionListener(e -> cardLayout.show(painelFormulario, "ATUALIZACAO"));

            JButton btnDeposito = new JButton("Depositar");
            btnDeposito.addActionListener(e -> cardLayout.show(painelFormulario, "DEPOSITO"));

            JButton btnTransferencia = new JButton("Transferir");
            btnTransferencia.addActionListener(e -> cardLayout.show(painelFormulario, "TRANSFERENCIA"));

            JButton btnExtrato = new JButton("Extrato");
            btnExtrato.addActionListener(e -> cardLayout.show(painelFormulario, "EXTRATO"));

            JButton btnLogout = new JButton("Logout");
            btnLogout.addActionListener(e -> {
                boolean sucesso = servicoUsuario.fazerLogout(token);
                if (sucesso) {
                    token = null;
                    atualizarBotoes();
                    cardLayout.show(painelFormulario, "VAZIO");
                    adicionarLog("Logout realizado com sucesso!");
                } else {
                    adicionarLog("Falha no logout!");
                }
            });

            JButton btnDeletar = new JButton("Deletar Conta");
            btnDeletar.addActionListener(e -> {
                int confirmacao = JOptionPane.showConfirmDialog(
                        this,
                        "Tem certeza que deseja deletar sua conta?\nEsta ação não pode ser desfeita!",
                        "Confirmar Exclusão",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirmacao == JOptionPane.YES_OPTION) {
                    boolean sucesso = servicoUsuario.deletarCadastro(token);
                    if (sucesso) {
                        token = null;
                        atualizarBotoes();
                        cardLayout.show(painelFormulario, "VAZIO");
                        adicionarLog("Conta deletada com sucesso!");
                    } else {
                        adicionarLog("Falha ao deletar conta!");
                    }
                }
            });

            painelBotoes.add(btnDados);
            painelBotoes.add(btnAtualizar);
            painelBotoes.add(btnDeposito);
            painelBotoes.add(btnTransferencia);
            painelBotoes.add(btnExtrato);
            painelBotoes.add(btnLogout);
            painelBotoes.add(btnDeletar);
        }

        painelBotoes.revalidate();
        painelBotoes.repaint();
    }

    private void enviarMensagemManual() {
        String mensagem = campoInput.getText().trim();
        if (!mensagem.isEmpty()) {
            try {
                String resposta = cliente.enviarMensagem(mensagem);
                adicionarLog("Enviado: " + mensagem);
                if (resposta != null) {
                    adicionarLog("Resposta: " + resposta);
                }
                campoInput.setText("");
            } catch (Exception e) {
                adicionarLog("Erro ao enviar: " + e.getMessage());
            }
        }
    }

    private void conectarAoServidor() {
        SwingUtilities.invokeLater(() -> {
            String serverIP = JOptionPane.showInputDialog(this, "IP do servidor:", "localhost");
            String portStr = JOptionPane.showInputDialog(this, "Porta do servidor:", "12345");

            if (serverIP != null && portStr != null) {
                try {
                    int port = Integer.parseInt(portStr);

                    // Simular entrada do usuário para o método conectarComServidor
                    System.setProperty("server.ip", serverIP);
                    System.setProperty("server.port", portStr);

                    // Executar em thread separada para não bloquear a GUI
                    new Thread(() -> {
                        if (cliente.conectarComServidorGUI(serverIP, port)) {
                            SwingUtilities.invokeLater(() -> {
                                adicionarLog("Conectado ao servidor " + serverIP + ":" + port);
                                setTitle("Sistema Bancário - Conectado a " + serverIP + ":" + port);
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                adicionarLog("Falha na conexão com o servidor!");
                                JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor!", "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();

                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Porta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void adicionarLog(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append(mensagem + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private String formatarDataParaISO(String dataInput, boolean inicioDodia) {
        try {
            java.time.format.DateTimeFormatter formatoEntrada = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.time.LocalDate data = java.time.LocalDate.parse(dataInput, formatoEntrada);

            java.time.LocalDateTime dataHora;
            if (inicioDodia) {
                dataHora = data.atStartOfDay();
            } else {
                dataHora = data.atTime(23, 59, 59);
            }

            return dataHora.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";

        } catch (Exception e) {
            throw new IllegalArgumentException("Data inválida: " + dataInput);
        }
    }
}