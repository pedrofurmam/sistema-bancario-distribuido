package dao;

import database.BancoDados;
import java.sql.*;
import java.time.Instant;

public class TransacaoDAO {

    public boolean criarTransacao(String cpfEnviador, String cpfRecebedor, double valor) {
        String sql = "INSERT INTO transacoes (valor_enviado, cpf_enviador, cpf_recebedor, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String timestamp = Instant.now().toString(); // Formato ISO 8601 UTC

            stmt.setDouble(1, valor);
            stmt.setString(2, cpfEnviador);
            stmt.setString(3, cpfRecebedor);
            stmt.setString(4, timestamp);
            stmt.setString(5, timestamp);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("Erro ao criar transação: " + e.getMessage());
            return false;
        }
    }
}