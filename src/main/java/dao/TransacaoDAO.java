package dao;

import database.BancoDados;
import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TransacaoDAO {

    // Formatter para garantir formato ISO 8601 sem frações de segundo
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public boolean criarTransacao(String cpfEnviador, String cpfRecebedor, double valor) {
        String sql = "INSERT INTO transacoes (valor_enviado, cpf_enviador, cpf_recebedor, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Formato correto ISO 8601 UTC sem frações de segundo
            String timestamp = Instant.now().atZone(java.time.ZoneOffset.UTC)
                    .format(ISO_FORMATTER);

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

    public List<Map<String, Object>> buscarTransacoesPorPeriodo(String cpf, String dataInicial, String dataFinal) {
        String sql = """
        SELECT t.id, t.valor_enviado, t.cpf_enviador, t.cpf_recebedor, 
               t.criado_em, t.atualizado_em,
               u1.nome as nome_enviador, u2.nome as nome_recebedor
        FROM transacoes t
        INNER JOIN usuarios u1 ON t.cpf_enviador = u1.cpf
        INNER JOIN usuarios u2 ON t.cpf_recebedor = u2.cpf
        WHERE (t.cpf_enviador = ? OR t.cpf_recebedor = ?)
        AND t.criado_em >= ? AND t.criado_em <= ?
        ORDER BY t.criado_em DESC
        """;

        List<Map<String, Object>> transacoes = new ArrayList<>();

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            stmt.setString(2, cpf);
            stmt.setString(3, dataInicial);
            stmt.setString(4, dataFinal);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> transacao = new HashMap<>();
                transacao.put("id", rs.getInt("id"));
                transacao.put("valor_enviado", rs.getDouble("valor_enviado"));

                Map<String, String> enviador = new HashMap<>();
                enviador.put("nome", rs.getString("nome_enviador"));
                enviador.put("cpf", rs.getString("cpf_enviador"));

                Map<String, String> recebedor = new HashMap<>();
                recebedor.put("nome", rs.getString("nome_recebedor"));
                recebedor.put("cpf", rs.getString("cpf_recebedor"));

                transacao.put("usuario_enviador", enviador);
                transacao.put("usuario_recebedor", recebedor);

                // Garantir que as datas estejam no formato correto
                String criadoEm = rs.getString("criado_em");
                String atualizadoEm = rs.getString("atualizado_em");

                // Se as datas já estão no formato correto, usar direto
                // Caso contrário, você pode precisar fazer parse e reformat
                transacao.put("criado_em", formatarDataSeNecessario(criadoEm));
                transacao.put("atualizado_em", formatarDataSeNecessario(atualizadoEm));

                transacoes.add(transacao);
            }

            return transacoes;

        } catch (Exception e) {
            System.err.println("Erro ao buscar transações: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String formatarDataSeNecessario(String data) {
        try {
            // Se a data já está no formato correto, retorna como está
            if (data.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
                return data;
            }

            // Se tem frações de segundo, remove elas
            if (data.contains(".")) {
                int dotIndex = data.indexOf(".");
                int zIndex = data.indexOf("Z");
                if (zIndex > dotIndex) {
                    return data.substring(0, dotIndex) + "Z";
                }
            }

            return data;
        } catch (Exception e) {
            System.err.println("Erro ao formatar data: " + e.getMessage());
            return data;
        }
    }
}