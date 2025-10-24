package autenticador;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class Token {
    // Armazena tokens ativos em memória
    private static final Map<String, String> tokens = new ConcurrentHashMap<>();

    /**
     * Gera novo token quando usuário faz login
     * Token permanece válido até logout manual
     */
    public static String gerarToken(String cpf) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, cpf);  // token → cpf do usuário

        System.out.println("Token gerado para " + cpf + ": " + token);
        return token;
    }

    /**
     * Verifica se token é válido
     */
    public static String validarToken(String token) {
        try {
            return tokens.get(token); // Retorna o CPF ou null se token não existir
        } catch (Exception e) {
            System.err.println("Erro ao validar token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Remove token (logout manual)
     */
    public static void invalidarToken(String token) {
        tokens.remove(token);
    }


}