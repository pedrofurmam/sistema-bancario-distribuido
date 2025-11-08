package autenticador;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;


public class Token {
    // Classe interna para armazenar informações do token
    private static class TokenInfo {
        private final String cpf;
        private boolean ativo;

        public TokenInfo(String cpf) {
            this.cpf = cpf;
            this.ativo = true;
        }

        public String getCpf() { return cpf; }
        public boolean isAtivo() { return ativo; }
        public void desativar() { this.ativo = false; }
    }
    // Armazena tokens ativos em memória
    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    /**
     * Gera novo token quando usuário faz login
     * Token permanece válido até logout manual
     */
    public static String gerarToken(String cpf) {
        // Remove tokens antigos do mesmo CPF (apenas um login por CPF)
        invalidarTokensPorCpf(cpf);

        String token = UUID.randomUUID().toString();

        tokens.put(token, new TokenInfo(cpf));

        System.out.println("Token gerado para " + cpf + ": " + token);
        return token;
    }

    /**
     * Verifica se token é válido
     */
    public static String validarToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            TokenInfo tokenInfo = tokens.get(token);
            if (tokenInfo == null) {
                System.out.println("Token não encontrado: " + token);
                return null;
            }

            if (!tokenInfo.isAtivo()) {
                System.out.println("Token inativo: " + token);
                return null;
            }

            // Token válido e ativo
            return tokenInfo.getCpf();

        } catch (Exception e) {
            System.err.println("Erro ao validar token: " + e.getMessage());
            return null;
        }
    }


    /**
     * Verifica se um CPF específico tem um token ativo
     */
    public static boolean cpfTemTokenAtivo(String cpf) {
        return tokens.values().stream()
                .anyMatch(tokenInfo -> cpf.equals(tokenInfo.getCpf()) && tokenInfo.isAtivo());
    }

    /**
     * Remove token específico (logout manual)
     */
    public static boolean invalidarToken(String token) {
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo != null && tokenInfo.isAtivo()) {
            tokenInfo.desativar();
            System.out.println("Token invalidado para CPF: " + tokenInfo.getCpf());
            return true;
        }
        return false;
    }

    /**
     * Remove todos os tokens de um CPF específico
     */
    public static void invalidarTokensPorCpf(String cpf) {
        tokens.values().stream()
                .filter(tokenInfo -> cpf.equals(tokenInfo.getCpf()) && tokenInfo.isAtivo())
                .forEach(tokenInfo -> {
                    tokenInfo.desativar();
                    System.out.println("Token invalidado para CPF: " + cpf);
                });
    }

    /**
     * Retorna número de tokens ativos
     */
    public static int contarTokensAtivos() {
        return (int) tokens.values().stream()
                .filter(TokenInfo::isAtivo)
                .count();
    }


}