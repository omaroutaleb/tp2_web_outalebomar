package com.project.tp2_web_outalebomar.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.Dependent;
import java.io.Serializable;

/**
 * Gère l'interface avec l'API de Gemini via LangChain4j.
 * Son rôle est essentiellement de lancer une requête à chaque nouvelle
 * question qu'on veut envoyer à l'API.
 *
 * De portée Dependent pour réinitialiser la conversation à chaque fois que
 * l'instance qui l'utilise est renouvelée.
 */
@Dependent
public class LlmClient implements Serializable {

    // Rôle système choisi par l'utilisateur
    private String systemRole;

    // Assistant IA créé par LangChain4j (proxy de l'interface Assistant)
    private Assistant assistant;

    // Mémoire pour garder l'historique de la conversation
    private ChatMemory chatMemory;

    /**
     * Constructeur
     * 1. Récupère la clé API
     * 2. Crée le ChatModel (instance représentant le LLM)
     * 3. Crée l'assistant avec la mémoire
     */
    public LlmClient() {
        // 1. Récupération de la clé secrète depuis variable d'environnement
        String apiKey = System.getenv("GEMINI_API_KEY");

        // TEMPORAIRE - Décommenter pour tester avec clé hardcodée
        // apiKey = "AIzaSy...VOTRE_CLE_ICI";

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY n'est pas définie ! " +
                            "Définissez-la comme variable d'environnement."
            );
        }

        // 2. Création du modèle de chat (ChatModel = représentation du LLM)
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .build();

        // 3. Création de l'assistant avec mémoire (max 10 messages comme demandé)
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * MÉTHODE 1 REQUISE PAR LE TP2
     * Setter pour le rôle système.
     * Vide la mémoire et ajoute le nouveau rôle système comme SystemMessage.
     *
     * @param systemRole le rôle choisi par l'utilisateur depuis la liste déroulante
     */
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;

        // Vider la mémoire car le contexte change complètement
        this.chatMemory.clear();

        // Ajouter le rôle système dans la mémoire comme SystemMessage
        if (systemRole != null && !systemRole.isBlank()) {
            this.chatMemory.add(SystemMessage.from(systemRole));
        }
    }

    /**
     * MÉTHODE 2 REQUISE PAR LE TP2
     * Envoie une question au LLM et retourne la réponse.
     * Utilise l'instance de l'interface Assistant (implémentée par LangChain4j).
     *
     * @param question la question de l'utilisateur
     * @return la réponse du LLM
     */
    public String envoyerQuestion(String question) {
        return assistant.chat(question);
    }
}
