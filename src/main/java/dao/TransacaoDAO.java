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

            String timestamp = Instant.now().atZone(java.time.ZoneOffset.UTC)
                    .format(ISO_FORMATTER);

            stmt.setDouble(1, valor);
            stmt.setString(2, cpfEnviador);
            stmt.setString(3, cpfRecebedor);
            stmt.setString(4, timestamp);
            stmt.setString(5, timestamp);

            boolean sucesso = stmt.executeUpdate() > 0;

            if (sucesso) {
                String tipo = cpfEnviador.equals(cpfRecebedor) ? "DEPÓSITO" : "TRANSFERÊNCIA";
                System.out.printf(" %s salva: %.2f de %s para %s em %s%n",
                        tipo, valor, cpfEnviador, cpfRecebedor, timestamp);
            } else {
                System.out.println(" Falha ao salvar transação");
            }

            return sucesso;

        } catch (Exception e) {
            System.err.println(" Erro ao criar transação: " + e.getMessage());
            e.printStackTrace();
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
    AND datetime(t.criado_em) >= datetime(?) AND datetime(t.criado_em) <= datetime(?)
    ORDER BY t.criado_em DESC
    """;

        List<Map<String, Object>> transacoes = new ArrayList<>();

        try (Connection conn = BancoDados.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            stmt.setString(2, cpf);

            // CORREÇÃO: Usar função datetime() do SQLite para comparação correta
            String dataInicialFormatada = dataInicial.replace("Z", "").replace("T", " ");
            String dataFinalFormatada = dataFinal.replace("Z", "").replace("T", " ");

            stmt.setString(3, dataInicialFormatada);
            stmt.setString(4, dataFinalFormatada);

            System.out.printf(" Buscando transações para CPF: %s%n", cpf);
            System.out.printf(" Período: %s até %s%n", dataInicialFormatada, dataFinalFormatada);

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
                transacao.put("criado_em", formatarDataSeNecessario(rs.getString("criado_em")));
                transacao.put("atualizado_em", formatarDataSeNecessario(rs.getString("atualizado_em")));

                transacoes.add(transacao);

                String tipo = rs.getString("cpf_enviador").equals(rs.getString("cpf_recebedor")) ? "DEPÓSITO" : "TRANSFERÊNCIA";
                System.out.printf(" %s encontrado: ID=%d, Valor=%.2f, Data=%s%n",
                        tipo, rs.getInt("id"), rs.getDouble("valor_enviado"), rs.getString("criado_em"));
            }

            System.out.printf(" Total de transações encontradas: %d%n", transacoes.size());
            return transacoes;

        } catch (Exception e) {
            System.err.println(" Erro ao buscar transações: " + e.getMessage());
            e.printStackTrace();
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