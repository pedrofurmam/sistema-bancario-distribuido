package cliente;


import cliente.ProcessadorRespostas;
import cliente.ServicoUsuario;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class InterfaceUsuario {
    private Scanner scanner;
    private ServicoUsuario servicoUsuario;
    private String token = null;

    public InterfaceUsuario(Cliente cliente) {
        this.scanner = new Scanner(System.in);
        this.servicoUsuario = new ServicoUsuario(cliente);
    }

    public void iniciarMenu() {
        while (true) {
            mostrarMenu();
            int opcao = lerOpcao();

            if (!executarOpcao(opcao)) {
                break;
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("\n" + "=".repeat(35));
        System.out.println("       SISTEMA BANCÁRIO");
        System.out.println("=".repeat(35));

        if (token == null) {
            System.out.println("1. Cadastrar usuário");
            System.out.println("2. Fazer login");
            System.out.println("0. Sair");
        } else {
            System.out.println("Bem-vindo ");
            System.out.println("3. Ver meus dados");
            System.out.println("4. Atualizar dados");
            System.out.println("5. Depositar");
            System.out.println("6. Fazer logout");
            System.out.println("7. Deletar cadastro");
            System.out.println("0. Sair");
        }

        System.out.println("=".repeat(35));
    }

    private boolean executarOpcao(int opcao) {
        // Se não está logado, só permite opções 1, 2 e 0
        if (token == null) {
            switch (opcao) {
                case 1:
                    cadastrarUsuario();
                    return true;
                case 2:
                    fazerLogin();
                    return true;
                case 0:
                    System.out.println("Encerrando sistema...");
                    return false;
                default:
                    if (opcao >= 3 && opcao <= 6) {
                        System.out.println("Você precisa estar logado!");
                    } else {
                        System.out.println("Opção inválida!");
                    }
                    return true;
            }
        }

        // Se está logado, só permite opções 3, 4, 5, 6 e 0
        switch (opcao) {
            case 3:
                verMeusDados();
                return true;
            case 4:
                atualizarDados();
                return true;
            case 5:
                fazerDeposito();
                return true;
            case 6:
                fazerLogout();
                return true;
            case 7:
                deletarCadastro();
                return true;
            case 0:
                System.out.println("Encerrando sistema...");
                return false;
            default:
                if (opcao == 1 || opcao == 2) {
                    System.out.println("Você já está logado!");
                } else {
                    System.out.println("Opção inválida!");
                }
                return true;
        }
    }

    private void cadastrarUsuario() {
        System.out.println("\n=== CADASTRO DE USUÁRIO ===");

        System.out.print("Nome (mínimo 6 caracteres): ");
        String nome = scanner.nextLine().trim();

        System.out.print("CPF (000.000.000-00): ");
        String cpf = scanner.nextLine().trim();

        System.out.print("Senha (mínimo 6 caracteres): ");
        String senha = scanner.nextLine().trim();

        servicoUsuario.cadastrarUsuario(nome, cpf, senha);
    }

    private void fazerLogin() {
        System.out.println("\n=== LOGIN ===");

        System.out.print("CPF: ");
        String cpf = scanner.nextLine().trim();

        System.out.print("Senha: ");
        String senha = scanner.nextLine().trim();

        token = servicoUsuario.fazerLogin(cpf, senha);
    }

    private void atualizarDados() {
        System.out.println("\n=== ATUALIZAR DADOS ===");

        Scanner scanner = new Scanner(System.in);
        Map<String, String> dadosAtualizacao = new HashMap<>();

        System.out.print("Novo nome (deixe em branco para não alterar): ");
        String nome = scanner.nextLine().trim();
        if (!nome.isEmpty()) {
            if (nome.length() >= 6 && nome.length() <= 120) {
                dadosAtualizacao.put("nome", nome);
            } else {
                System.out.println("Nome deve ter entre 6 e 120 caracteres");
                return;
            }
        }

        System.out.print("Nova senha (deixe em branco para não alterar): ");
        String senha = scanner.nextLine().trim();
        if (!senha.isEmpty()) {
            if (senha.length() >= 6 && senha.length() <= 120) {
                dadosAtualizacao.put("senha", senha);
            } else {
                System.out.println("Senha deve ter entre 6 e 120 caracteres");
                return;
            }
        }

        if (dadosAtualizacao.isEmpty()) {
            System.out.println("Nenhuma alteração foi solicitada");
            return;
        }

        servicoUsuario.atualizarDados(token, dadosAtualizacao);
    }

    private void verMeusDados() {
        System.out.println("\n=== MEUS DADOS ===");
        servicoUsuario.verMeusDados(token);
    }

    private void fazerLogout() {
        boolean sucesso = servicoUsuario.fazerLogout(token);
        if (sucesso) {
            token = null;
        } else {
            System.out.println("Falha ao fazer logout. Você permanece logado.");
        }
    }

    private void fazerDeposito() {
        System.out.println("\n=== DEPÓSITO ===");

        System.out.print("Valor a depositar: R$ ");
        String valorInput = scanner.nextLine().trim();

        try {
            double valor = Double.parseDouble(valorInput);

            if (valor <= 0) {
                System.out.println("Valor deve ser positivo!");
                return;
            }

            servicoUsuario.depositar(token, valor);

        } catch (NumberFormatException e) {
            System.out.println("Valor inválido! Digite apenas números.");
        }
    }

    private void deletarCadastro(){
        boolean sucesso = servicoUsuario.deletarCadastro(token);
        if (sucesso) {
            token = null;
        } else {
            System.out.println("Falha ao deletar usuario. Você permanece logado.");
        }
    }

    private int lerOpcao() {
        System.out.print("Escolha uma opção: ");
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}