package com.project.tp2_web_outalebomar.jsf;

import com.project.tp2_web_outalebomar.llm.LlmClient;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation qui dure pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable (le backing bean peut être mis en mémoire secondaire).
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;


    private boolean roleSystemeChangeable = true;

    private List<SelectItem> listeRolesSysteme;


    private String question;

    private String reponse;

    private StringBuilder conversation = new StringBuilder();


    @Inject
    private FacesContext facesContext;


    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    /**
     * setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question.
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question au serveur.
     * En attendant de l'envoyer à un LLM, le serveur fait un traitement quelconque, juste pour tester :
     * Le traitement consiste à copier la question en minuscules et à l'entourer avec "||". Le rôle système
     * est ajouté au début de la première réponse.
     *
     * @return null pour rester sur la même page.
     */
    @Inject
    private LlmClient llmClient;  // À ajouter en haut de la classe Bb

    public String envoyer() {
        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Si c'est la première question, définir le rôle système
        if (this.conversation.isEmpty()) {
            llmClient.setSystemRole(roleSysteme);
            // Invalide le bouton pour changer le rôle système
            this.roleSystemeChangeable = false;
        }

        // Appel au LLM
        try {
            this.reponse = llmClient.envoyerQuestion(question);
        } catch (Exception e) {
            this.reponse = "Erreur lors de la communication avec le LLM : " + e.getMessage();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur LLM", e.getMessage());
            facesContext.addMessage(null, message);
        }

        // Afficher la conversation
        afficherConversation();
        return null;
    }


    /**
     * Pour un nouveau chat.
     * Termine la portée view en retournant "index" (la page index.xhtml sera affichée après le traitement
     * effectué pour construire la réponse) et pas null. null aurait indiqué de rester dans la même page (index.xhtml)
     * sans changer de vue.
     * Le fait de changer de vue va faire supprimer l'instance en cours du backing bean par CDI et donc on reprend
     * tout comme au début puisqu'une nouvelle instance du backing va être utilisée par la page index.xhtml.
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis
            this.listeRolesSysteme = new ArrayList<>();
            // Vous pouvez évidemment écrire ces rôles dans la langue que vous voulez.
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));
        }

        return this.listeRolesSysteme;
    }

}
