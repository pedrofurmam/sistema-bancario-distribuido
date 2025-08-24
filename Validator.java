package validador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Validator {

    private Validator() {}

    // ObjectMapper é a classe principal do Jackson para converter JSON.
    // É uma boa prática reutilizar a mesma instância.
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Valida uma mensagem JSON enviada do Cliente para o Servidor.
     *
     * @param jsonString A mensagem JSON como uma String.
     * @throws Exception se o JSON for inválido ou não seguir o protocolo.
     */
    public static void validateClient(String jsonString) throws Exception {
        JsonNode rootNode = parseJson(jsonString);

        // Valida a presença e o tipo do campo 'operacao'
        JsonNode operacaoNode = getRequiredField(rootNode, "operacao");
        if (!operacaoNode.isTextual()) {
            throw new IllegalArgumentException("O campo 'operacao' deve ser uma String.");
        }

        // Converte a string da operação para o nosso Enum
        RulesEnum operacao = RulesEnum.getEnum(operacaoNode.asText());

        // Chama o método de validação específico para a operação
        switch (operacao) {
            case USUARIO_LOGIN:
                validateUsuarioLoginClient(rootNode);
                break;
            case USUARIO_LOGOUT:
                validateUsuarioLogoutClient(rootNode);
                break;
            case USUARIO_CRIAR:
                validateUsuarioCriarClient(rootNode);
                break;
            case USUARIO_LER:
                validateUsuarioLerClient(rootNode);
                break;
            case USUARIO_ATUALIZAR:
                validateUsuarioAtualizarClient(rootNode);
                break;
            case USUARIO_DELETAR:
                validateUsuarioDeletarClient(rootNode);
                break;
            case TRANSACAO_CRIAR:
                validateTransacaoCriarClient(rootNode);
                break;
            case TRANSACAO_LER:
                validateTransacaoLerClient(rootNode);
                break;
            default:
                throw new IllegalArgumentException("Operação do cliente desconhecida ou não suportada: " + operacao);
        }
    }

    /**
     * Valida uma mensagem JSON enviada do Servidor para o Cliente.
     *
     * @param jsonString A mensagem JSON como uma String.
     * @throws Exception se o JSON for inválido ou não seguir o protocolo.
     */
    public static void validateServer(String jsonString) throws Exception {
        JsonNode rootNode = parseJson(jsonString);

        // Toda resposta do servidor deve ter 'operacao', 'status' e 'info'
        JsonNode operacaoNode = getRequiredField(rootNode, "operacao");
        if (!operacaoNode.isTextual()) {
            throw new IllegalArgumentException("O campo 'operacao' na resposta do servidor deve ser uma String.");
        }
        
        JsonNode statusNode = getRequiredField(rootNode, "status");
        if (!statusNode.isBoolean()) {
            throw new IllegalArgumentException("O campo 'status' na resposta do servidor deve ser um booleano (true/false).");
        }

        getRequiredField(rootNode, "info"); // Apenas verifica a presença

        RulesEnum operacao = RulesEnum.getEnum(operacaoNode.asText());

        // Chama a validação específica apenas se o status for true (sucesso)
        if (statusNode.asBoolean()) {
            switch (operacao) {
                case USUARIO_LOGIN:
                    validateUsuarioLoginServer(rootNode);
                    break;
                case USUARIO_LER:
                    validateUsuarioLerServer(rootNode);
                    break;
                case TRANSACAO_LER:
                    validateTransacaoLerServer(rootNode);
                    break;
                // Outras operações de sucesso não retornam dados adicionais, então não precisam de validação extra.
                default:
                    break; 
            }
        }
    }

    // ===================================================================================
    // MÉTODOS DE VALIDAÇÃO PRIVADOS (CLIENTE -> SERVIDOR)
    // ===================================================================================

    private static void validateUsuarioLoginClient(JsonNode node) {
        validateCpfFormat(node, "cpf");
        validateStringMinLength(node, "senha", 6);
    }

    private static void validateUsuarioLogoutClient(JsonNode node) {
        checkRequiredString(node, "token");
    }

    private static void validateUsuarioCriarClient(JsonNode node) {
        validateStringMinLength(node, "nome", 6);
        validateCpfFormat(node, "cpf");
        validateStringMinLength(node, "senha", 6);
    }

    private static void validateUsuarioLerClient(JsonNode node) {
        checkRequiredString(node, "token");
    }

    private static void validateUsuarioAtualizarClient(JsonNode node) {
        checkRequiredString(node, "token");
        JsonNode usuarioNode = getRequiredObject(node, "usuario");
        
        if (!usuarioNode.has("nome") && !usuarioNode.has("senha")) {
            throw new IllegalArgumentException("O objeto 'usuario' para atualização deve conter pelo menos o campo 'nome' ou 'senha'.");
        }
        // Valida os campos apenas se eles existirem na requisição
        if (usuarioNode.has("nome")){
            validateStringMinLength(usuarioNode, "nome", 6);
        }
        if (usuarioNode.has("senha")){
            validateStringMinLength(usuarioNode, "senha", 6);
        }
    }

    private static void validateUsuarioDeletarClient(JsonNode node) {
        checkRequiredString(node, "token");
    }

    private static void validateTransacaoCriarClient(JsonNode node) {
        checkRequiredString(node, "token");
        validateCpfFormat(node, "cpf");
        getRequiredNumber(node, "valor");
    }

    private static void validateTransacaoLerClient(JsonNode node) {
        checkRequiredString(node, "token");
        getRequiredInt(node, "pagina");
        getRequiredInt(node, "limite");
    }

    // ===================================================================================
    // MÉTODOS DE VALIDAÇÃO PRIVADOS (SERVIDOR -> CLIENTE)
    // ===================================================================================

    private static void validateUsuarioLoginServer(JsonNode node) {
        checkRequiredString(node, "token");
    }

    private static void validateUsuarioLerServer(JsonNode node) {
        JsonNode usuarioNode = getRequiredObject(node, "usuario");
        validateCpfFormat(usuarioNode, "cpf");
        validateStringMinLength(usuarioNode, "nome", 6);
        getRequiredNumber(usuarioNode, "saldo");
        if (usuarioNode.has("senha")) {
            throw new IllegalArgumentException("A resposta do servidor para 'usuario_ler' não deve conter o campo 'senha'.");
        }
    }
    
    private static void validateTransacaoLerServer(JsonNode node) {
        JsonNode transacoesNode = getRequiredArray(node, "transacoes");
        for (JsonNode transacao : transacoesNode) {
            getRequiredInt(transacao, "id");
            getRequiredNumber(transacao, "valor_enviado");
            JsonNode enviadorNode = getRequiredObject(transacao, "usuario_enviador");
            validateStringMinLength(enviadorNode, "nome", 6);
            validateCpfFormat(enviadorNode, "cpf");
            JsonNode recebedorNode = getRequiredObject(transacao, "usuario_recebedor");
            validateStringMinLength(recebedorNode, "nome", 6);
            validateCpfFormat(recebedorNode, "cpf");
        }
    }

    // ===================================================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // ===================================================================================

    private static JsonNode parseJson(String jsonString) throws Exception {
        try {
            return mapper.readTree(jsonString);
        } catch (Exception e) {
            throw new Exception("Erro de sintaxe. A mensagem não é um JSON válido.", e);
        }
    }

    private static JsonNode getRequiredField(JsonNode parentNode, String fieldName) {
        if (parentNode.has(fieldName) && !parentNode.get(fieldName).isNull()) {
            return parentNode.get(fieldName);
        }
        throw new IllegalArgumentException("O campo obrigatório '" + fieldName + "' não foi encontrado ou é nulo.");
    }

    private static void checkRequiredString(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isTextual()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser do tipo String.");
        }
    }

    private static void validateStringMinLength(JsonNode parentNode, String fieldName, int minLength) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isTextual()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser do tipo String.");
        }
        // .trim() remove espaços em branco no começo e no fim da String
        if (field.asText().trim().length() < minLength) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ter no mínimo " + minLength + " caracteres (desconsiderando espaços no início).");
        }
    }

    private static void validateCpfFormat(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isTextual()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser do tipo String.");
        }
        String cpf = field.asText();
        // Expressão Regular que valida o formato 000.000.000-00
        String cpfRegex = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}";
        if (!cpf.matches(cpfRegex)) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve estar no formato '000.000.000-00'.");
        }
    }
    
    private static void getRequiredNumber(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isNumber()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser do tipo numérico (int, double, etc).");
        }
    }

    private static void getRequiredInt(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isInt()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser do tipo int.");
        }
    }

    private static JsonNode getRequiredObject(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isObject()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser um objeto JSON (ex: { ... }).");
        }
        return field;
    }
    
    private static JsonNode getRequiredArray(JsonNode parentNode, String fieldName) {
        JsonNode field = getRequiredField(parentNode, fieldName);
        if (!field.isArray()) {
            throw new IllegalArgumentException("O campo '" + fieldName + "' deve ser um array JSON (ex: [ ... ]).");
        }
        return field;
    }
}