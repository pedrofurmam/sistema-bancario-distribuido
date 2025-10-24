package database;

import java.sql.*;
import java.util.Scanner;

public class GerenciadorBanco {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== GERENCIADOR DE BANCO DE DADOS ===");

        while (true) {
            mostrarMenu();
            int opcao = lerOpcao();

            switch (opcao) {
                case 1:
                    listarUsuarios();
                    break;
                case 2:
                    deletarUsuarioPorCpf();
                    break;
                case 3:
                    deletarTodosUsuarios();
                    break;
                case 0:
                    System.out.println("Encerrando...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }

            System.out.println("\nPressione ENTER para continuar...");
            scanner.nextLine();
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("           MENU PRINCIPAL");
        System.out.println("=".repeat(40));
        System.out.println("1. Listar todos os usuários");
        System.out.println("2. Deletar usuário por CPF");
        System.out.println("3. Deletar todos os usuários");
        System.out.println("0. Sair");
        System.out.println("=".repeat(40));
        System.out.print("Escolha uma opção: ");
    }

    private static int lerOpcao() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void listarUsuarios() {
        String sql = "SELECT cpf, nome, saldo FROM usuarios ORDER BY nome";

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n" + "=".repeat(60));
            System.out.println("                    USUÁRIOS CADASTRADOS");
            System.out.println("=".repeat(60));

            boolean temUsuarios = false;
            while (rs.next()) {
                temUsuarios = true;
                String cpf = rs.getString("cpf");
                String nome = rs.getString("nome");
                double saldo = rs.getDouble("saldo");

                System.out.printf("CPF: %s | Nome: %s | Saldo: R$ %.2f%n", cpf, nome, saldo);
            }

            if (!temUsuarios) {
                System.out.println("Nenhum usuário encontrado.");
            }

            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("Erro ao listar usuários: " + e.getMessage());
        }
    }

    private static void deletarUsuarioPorCpf() {
        System.out.print("Digite o CPF do usuário a ser deletado (000.000.000-00): ");
        String cpf = scanner.nextLine().trim();

        if (cpf.isEmpty()) {
            System.out.println("CPF não pode estar vazio!");
            return;
        }

        String sql = "DELETE FROM usuarios WHERE cpf = ?";

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 0) {
                System.out.println("Usuário deletado com sucesso!");
            } else {
                System.out.println("Usuário com CPF " + cpf + " não encontrado.");
            }

        } catch (Exception e) {
            System.err.println("Erro ao deletar usuário: " + e.getMessage());
        }
    }

    private static void deletarTodosUsuarios() {
        System.out.print("Tem certeza que deseja deletar TODOS os usuários? (s/N): ");
        String confirmacao = scanner.nextLine().trim().toLowerCase();

        if (!confirmacao.equals("s") && !confirmacao.equals("sim")) {
            System.out.println("Operação cancelada.");
            return;
        }

        String sql = "DELETE FROM usuarios";

        try (Connection conn = BancoDados.getConnection();
             Statement stmt = conn.createStatement()) {

            int usuariosDeletados = stmt.executeUpdate(sql);
            System.out.println("Todos os usuários foram deletados!");
            System.out.println("Total: " + usuariosDeletados + " usuário(s) removido(s)");

        } catch (Exception e) {
            System.err.println("Erro ao deletar todos os usuários: " + e.getMessage());
        }
    }
}