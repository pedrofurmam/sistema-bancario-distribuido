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

        // Redirecionar console para GUI ANTES de criar o serviço
        redirecionarConsoleParaGUI();

        this.servicoUsuario = new ServicoUsuario(cliente);

        initializeComponents();
        setupLayout();
        setupEventListeners();

        setTitle("Sistema Bancário - Interface Gráfica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        conectarAoServidor();
    }

    private void redirecionarConsoleParaGUI() {
        System.setOut(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String linha = buffer.toString();
                    if (!linha.trim().isEmpty()) {
                        adicionarLog(linha);
                    }
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        }));

        System.setErr(new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String linha = buffer.toString();
                    if (!linha.trim().isEmpty()) {
                        adicionarLog("ERRO: " + linha);
                    }
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        }));
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
                new Thread(() -> {
                    String resultado = servicoUsuario.fazerLogin(cpf, senha);
                    SwingUtilities.invokeLater(() -> {
                        if (resultado != null) {
                            token = resultado;
                            atualizarBotoes();
                            cardLayout.show(painelFormulario, "VAZIO");
                            campoCpf.setText("");
                            campoSenha.setText("");
                        }
                    });
                }).start();
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
                new Thread(() -> {
                    servicoUsuario.cadastrarUsuario(nome, cpf, senha);
                    SwingUtilities.invokeLater(() -> {
                        cardLayout.show(painelFormulario, "VAZIO");
                        campoNome.setText("");
                        campoCpf.setText("");
                        campoSenha.setText("");
                    });
                }).start();
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
                    if (valor > 0) {
                        new Thread(() -> {
                            servicoUsuario.depositar(token, valor);
                            SwingUtilities.invokeLater(() -> campoValor.setText(""));
                        }).start();
                    } else {
                        adicionarLog("Valor deve ser positivo!");
                    }
                } catch (NumberFormatException ex) {
                    adicionarLog("Valor inválido!");
                }
            } else {
                adicionarLog("Digite um valor!");
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
                    if (valor > 0) {
                        new Thread(() -> {
                            servicoUsuario.enviarDinheiro(token, cpfDestino, valor);
                            SwingUtilities.invokeLater(() -> {
                                campoCpfDestino.setText("");
                                campoValor.setText("");
                            });
                        }).start();
                    } else {
                        adicionarLog("Valor deve ser positivo!");
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
                new Thread(() -> {
                    servicoUsuario.atualizarDados(token, dados);
                    SwingUtilities.invokeLater(() -> {
                        campoNome.setText("");
                        campoSenha.setText("");
                    });
                }).start();
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
                new Thread(() -> {
                    try {
                        String dataInicialISO = formatarDataParaISO(dataInicial, true);
                        String dataFinalISO = formatarDataParaISO(dataFinal, false);
                        servicoUsuario.verTransacoes(token, dataInicialISO, dataFinalISO);
                    } catch (Exception ex) {
                        adicionarLog("Formato de data inválido! Use dd/MM/yyyy");
                    }
                }).start();
            } else {
                adicionarLog("Por favor, preencha ambas as datas");
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
            JButton btnLogin = new JButton("Fazer Login");
            btnLogin.addActionListener(e -> cardLayout.show(painelFormulario, "LOGIN"));
            painelBotoes.add(btnLogin);

            JButton btnCadastro = new JButton("Cadastrar");
            btnCadastro.addActionListener(e -> cardLayout.show(painelFormulario, "CADASTRO"));
            painelBotoes.add(btnCadastro);

        } else {
            JButton btnDados = new JButton("Meus Dados");
            btnDados.addActionListener(e -> {
                new Thread(() -> servicoUsuario.verMeusDados(token)).start();
            });
            painelBotoes.add(btnDados);

            JButton btnDeposito = new JButton("Depositar");
            btnDeposito.addActionListener(e -> cardLayout.show(painelFormulario, "DEPOSITO"));
            painelBotoes.add(btnDeposito);

            JButton btnTransferencia = new JButton("Transferir");
            btnTransferencia.addActionListener(e -> cardLayout.show(painelFormulario, "TRANSFERENCIA"));
            painelBotoes.add(btnTransferencia);

            JButton btnExtrato = new JButton("Extrato");
            btnExtrato.addActionListener(e -> cardLayout.show(painelFormulario, "EXTRATO"));
            painelBotoes.add(btnExtrato);

            JButton btnAtualizar = new JButton("Atualizar Dados");
            btnAtualizar.addActionListener(e -> cardLayout.show(painelFormulario, "ATUALIZACAO"));
            painelBotoes.add(btnAtualizar);

            JButton btnLogout = new JButton("Logout");
            btnLogout.addActionListener(e -> {
                new Thread(() -> {
                    boolean sucesso = servicoUsuario.fazerLogout(token);
                    if (sucesso) {
                        SwingUtilities.invokeLater(() -> {
                            token = null;
                            atualizarBotoes();
                            cardLayout.show(painelFormulario, "VAZIO");
                        });
                    }
                }).start();
            });
            painelBotoes.add(btnLogout);

            JButton btnDeletar = new JButton("Deletar Conta");
            btnDeletar.addActionListener(e -> {
                int confirmacao = JOptionPane.showConfirmDialog(
                        this,
                        "Tem certeza que deseja deletar sua conta? Esta ação não pode ser desfeita.",
                        "Confirmar Exclusão",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirmacao == JOptionPane.YES_OPTION) {
                    new Thread(() -> {
                        boolean sucesso = servicoUsuario.deletarCadastro(token);
                        if (sucesso) {
                            SwingUtilities.invokeLater(() -> {
                                token = null;
                                atualizarBotoes();
                                cardLayout.show(painelFormulario, "VAZIO");
                            });
                        }
                    }).start();
                }
            });
            painelBotoes.add(btnDeletar);
        }

        painelBotoes.revalidate();
        painelBotoes.repaint();
    }

    private void enviarMensagemManual() {
        String mensagem = campoInput.getText().trim();
        if (!mensagem.isEmpty()) {
            new Thread(() -> {
                String resposta = cliente.enviarMensagem(mensagem);
                adicionarLog("Resposta: " + resposta);
            }).start();
            campoInput.setText("");
        }
    }

    private void conectarAoServidor() {
        SwingUtilities.invokeLater(() -> {
            String ip = JOptionPane.showInputDialog(this, "IP do servidor:", "127.0.0.1");
            if (ip != null && !ip.trim().isEmpty()) {
                String portaStr = JOptionPane.showInputDialog(this, "Porta do servidor:", "12345");
                if (portaStr != null && !portaStr.trim().isEmpty()) {
                    try {
                        int porta = Integer.parseInt(portaStr);
                        new Thread(() -> {
                            boolean conectado = cliente.conectarComServidorGUI(ip, porta);
                            if (conectado) {
                                adicionarLog("Conectado ao servidor com sucesso!");
                            } else {
                                adicionarLog("Falha na conexão com o servidor!");
                            }
                        }).start();
                    } catch (NumberFormatException e) {
                        adicionarLog("Porta inválida!");
                    }
                }
            }
        });
    }

    private void adicionarLog(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            areaLog.append("[" + timestamp + "] " + mensagem + "\n");
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