package dao;

import database.BancoDados;
import modelo.Usuario;
import java.sql.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // Método 1: Verificar se CPF já existe
    public boolean cpfExiste(String cpf) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE cpf = ?";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);  // Substitui o ? pelo CPF
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;  // Retorna true se COUNT > 0

        } catch (Exception e) {
            System.err.println("Erro ao verificar CPF: " + e.getMessage());
            return false;  // Em caso de erro, assume que não existe
        }
    }

    // Método 2: Salvar usuário no banco
    public boolean salvar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (cpf, nome, senha, saldo) VALUES (?, ?, ?, ?)";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Substitui os ? pelos dados do usuário
            stmt.setString(1, usuario.getCpf());
            stmt.setString(2, usuario.getNome());
            stmt.setString(3, usuario.getSenha());
            stmt.setDouble(4, usuario.getSaldo());

            return stmt.executeUpdate() > 0;  // Retorna true se inseriu pelo menos 1 linha

        } catch (Exception e) {
            System.err.println("Erro ao salvar usuário: " + e.getMessage());
            return false;
        }
    }

    // Método 3: Validar login (CPF + senha)
    public boolean validarLogin(String cpf, String senha) {
        String sql = "SELECT senha FROM usuarios WHERE cpf = ?";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String senhaArmazenada = rs.getString("senha");
                return senhaArmazenada.equals(senha);  // Compara senhas
            }
            return false;  // CPF não encontrado

        } catch (Exception e) {
            System.err.println("Erro ao validar login: " + e.getMessage());
            return false;
        }
    }


    public Usuario buscarPorCpf(String cpf) {
        String sql = "SELECT nome, cpf, senha, saldo FROM usuarios WHERE cpf = ?";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String nome = rs.getString("nome");
                String senha = rs.getString("senha");
                double saldo = rs.getDouble("saldo");

                return new Usuario(nome, cpf, senha, saldo);
            }

            return null; // Usuário não encontrado

        } catch (Exception e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
            return null;
        }
    }

    public boolean atualizar(String cpf, JsonNode dadosAtualizacao) {
        try {
            // 1. Verificar se usuário existe
            Usuario usuarioExistente = buscarPorCpf(cpf);
            if (usuarioExistente == null) {
                return false;
            }

            // 2. Construir query dinamicamente baseada nos campos enviados
            StringBuilder sql = new StringBuilder("UPDATE usuarios SET ");
            List<Object> parametros = new ArrayList<>();

            boolean primeiro = true;

            if (dadosAtualizacao.has("nome")) {
                if (!primeiro) sql.append(", ");
                sql.append("nome = ?");
                parametros.add(dadosAtualizacao.get("nome").asText());
                primeiro = false;
            }

            if (dadosAtualizacao.has("senha")) {
                if (!primeiro) sql.append(", ");
                sql.append("senha = ?");
                parametros.add(dadosAtualizacao.get("senha").asText());
                primeiro = false;
            }

            sql.append(" WHERE cpf = ?");
            parametros.add(cpf);

            // 3. Executar atualização
            try (Connection conn = BancoDados.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                for (int i = 0; i < parametros.size(); i++) {
                    stmt.setObject(i + 1, parametros.get(i));
                }

                return stmt.executeUpdate() > 0;

            } catch (SQLException e) {
                System.err.println("Erro ao atualizar usuário: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            System.err.println("Erro ao processar atualização: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarSaldo(String cpf, double novoSaldo) {
        String sql = "UPDATE usuarios SET saldo = ? WHERE cpf = ?";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, novoSaldo);
            stmt.setString(2, cpf);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("Erro ao atualizar saldo: " + e.getMessage());
            return false;
        }
    }

    public boolean deletar(String cpf){
        String sql = "DELETE FROM usuarios WHERE cpf = ?";
        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            int linhasAfetadas = stmt.executeUpdate();

            return linhasAfetadas > 0; // Retorna true se deletou pelo menos 1 linha

        } catch (Exception e) {
            System.err.println("Erro ao deletar usuário: " + e.getMessage());
            return false;
        }
    }
}