package client.appcontroller;

import client.ui.Content;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import client.rpc.ApplicationServiceAsync;
import shared.DTO.*;
import java.util.ArrayList;

//Defualt konstruktør
public class GuestController {

    private Content content;
    private ListDataProvider<Participant> participantListDataProvider;
    private ApplicationServiceAsync rpcService;

    public GuestController(Content content, ApplicationServiceAsync rpcService){
        this.content = content;
        this.participantListDataProvider = new ListDataProvider<>();
        this.rpcService = rpcService;
        //Tilføjer clickhandlers til forskellige elementer på siden
        addClickHandlers();
        createSignUp();
        createStatistic();
    }

    /**
     * Her bliver der tilføjet clickHandlers til GuestControlleren, som forbinder knapperne i GuestView
     * med clickHandler metoderne i GuestControlleren.
     */
    private void addClickHandlers(){
        content.getGuestView().addClickHandlers(new GuestClickHandlers());
        content.getGuestView().getSignUpView().addClickHandlers(new CreateParticipantClickHandler());
        content.getGuestView().getLoginView().addClickHandler(new LoginClickHandler());
    }

    /**
     * En clickhandler klasse som indeholder en onclick metode.
     */
    class GuestClickHandlers implements ClickHandler{

        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == content.getGuestView().getLogindBtn()){
                content.getGuestView().changeView(0);
            } else if (event.getSource() == content.getGuestView().getTilmeldingBtn()){
                content.getGuestView().changeView(1);
            } else if (event.getSource() == content.getGuestView().getLogoImg()){
                content.getGuestView().changeView(content.getGuestView().getStartView());
            } else if (event.getSource() == content.getGuestView().getStatistiskBtn()){
                content.getGuestView().changeView(content.getGuestView().getGuestStatisticView());
                //Dette kald skal være her for at tabellen viser sine data.
                participantListDataProvider.refresh();
            }
        }
    }

    /**
     * Login ClickHandler, som når den bliver klikket tjekker om bruger og kode stemmer over ens
     */
    class LoginClickHandler implements ClickHandler{

        @Override
        public void onClick(ClickEvent event) {
            String username = content.getGuestView().getLoginView().getUsernameTB().getText();
            String password = content.getGuestView().getLoginView().getPasswordTB().getText();

            rpcService.authorizePerson(username, password, new AsyncCallback<Person>() {
                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Server fejl");
                }

                @Override
                public void onSuccess(Person result) {
                    if (result == null){
                        content.getGuestView().getLoginView().getErrMessageLabel().setText("Email og password matcher ikke");
                    }
                    if (result instanceof Participant){
                        new ParticipantController(content, (Participant) result, rpcService);
                    }

                    if (result instanceof Admin){
                        new AdminController(content, rpcService);
                    }
                }
            });
        }
    }

    /**
     * ClickHandler for at oprette en bruger
     * Tjekke om alle kriterier for at oprette en bruger er godkendt hvor den så enten opretter bruger
     * eller udmelder hvad der skal gøres om
     */
    class CreateParticipantClickHandler implements ClickHandler{
        private String name;
        private String email;
        private String cyclistType;
        private String firmName;
        private String password;
        private String passwordCheck;
        private ArrayList<String> errMessage;

        @Override
        public void onClick(ClickEvent event) {
            this.email = content.getGuestView().getSignUpView().getEmailField().getText();
            this.name = content.getGuestView().getSignUpView().getNameField().getText();
            this.cyclistType = content.getGuestView().getSignUpView().getCyclistTypeList().getSelectedValue();
            this.firmName = content.getGuestView().getSignUpView().getFirmList().getSelectedValue();
            this.password = content.getGuestView().getSignUpView().getPasswordField().getText();
            this.passwordCheck = content.getGuestView().getSignUpView().getPasswordCheckField().getText();
            this.errMessage = new ArrayList<>();

            /***
             *  Her benytter jeg kun et enkelt &.
             *  Dette heder bitwise operatoren og evaluere begge sider uden at stoppe dens kondition fra at "short circuiting"
             *  Dette betyder at den kører alle metoderne og ikke kun de første
             */
            Boolean isCorrectSignUp = validateName() & validateEmail() & validatePassword();
            content.getGuestView().getSignUpView().getErrorMessageLabel().setHTML ( "<p>" + String.join("<br>", errMessage) + "</p>");

            if (isCorrectSignUp){

                Participant newParticipant = new Participant();
                newParticipant.setEmail(email);
                newParticipant.setName(name);
                newParticipant.setCyclistType(cyclistType);
                newParticipant.setPassword(password);
                newParticipant.setFirmName(firmName);

                rpcService.createParticipant(newParticipant, new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        System.out.println(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(String result) {

                        Window.alert(result);
                    }
                });
            }
        }

        private boolean validateEmail(){
            int atPosition, dotPosition;

            atPosition = email.indexOf("@");
            dotPosition = email.lastIndexOf(".");

            //Tjekker om det er en valid email-adresse før den efterspørger databasen.
            if (atPosition > 0 &&
                    dotPosition > atPosition &&
                    dotPosition < (email.length()-2) &&
                    email.lastIndexOf("@") == email.indexOf("@") &&
                    !email.contains(" ")) {
                return true;
            }
            errMessage.add("Din Email er ikke valid. Tjek for eventuelle tastefejl ;)");
            return false;
        }

        private boolean validateName() {
            RegExp regExp = RegExp.compile("^[a-zA-Z ]+$");
            MatchResult matchResult = regExp.exec(name);
            Boolean valid = matchResult != null;
            if(!valid)
                errMessage.add("Dit navn må ikke indeholde specialtegn eller tal i vores program ;)");
            return valid;
        }

        private boolean validatePassword(){

            boolean error = false;

            Boolean hasSmallLetters = RegExp.compile("[a-z]").exec(password) != null;
            Boolean hasCapitalLetters = RegExp.compile("[A-X]").exec(password) != null;
            Boolean hasSpaces = RegExp.compile("[ ]").exec(password) != null;
            Boolean hasNumbers = RegExp.compile("[0-9]").exec(password) != null;
            Boolean hasSpecialSymbols = RegExp.compile("[^a-zA-Z0-9 ]+").exec(password) != null;

            if (!password.equals(passwordCheck)){
                errMessage.add("Kodeordene matcher ikke hinanden ;)");
                error = true;
            }

            if (!hasSmallLetters || !hasCapitalLetters){
                errMessage.add("Dit kodeord skal både indeholde store og små bogstaver ;)");
                error = true;
            }

            if (hasSpaces){
                errMessage.add("Der må ikke forekomme mellemrum i dit kodeord ;)");
                error = true;
            }

            if (!hasNumbers){
                errMessage.add("Dit kodeord skal have numre ;)");
                error = true;
            }

            if (!hasSpecialSymbols){
                errMessage.add("Dit kodekord skal have et specialsymbol ;)");
                error = true;
            }

            return !error;
        }
    }

    /**
     * Metode der hente oprettede firmaer, som en ny bruger kan vælge at tilslutte sig
     */

    private void createSignUp(){
        rpcService.getAllFirmsAndTeamsAndParticipants(new AsyncCallback<ArrayList<Firm>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(ArrayList<Firm> result) {
                for (Firm firm :result) {
                    content.getGuestView().getSignUpView().getFirmList().addItem(firm.getFirmName());
                }
            }
        });
    }

    /**
     * Metode der indlæser statistikker over firmaer og hold, som en gæst kan se, når der bliver
     * klikket på knappen Information
     */

    private void createStatistic(){
        rpcService.getAllFirmsAndTeamsAndParticipants(new AsyncCallback<ArrayList<Firm>>() {
            @Override
            public void onFailure(Throwable caught) {Window.alert(caught.getMessage());}

            @Override
            public void onSuccess(ArrayList<Firm> firms) {
                rpcService.getAllTeamsAndTeamNameAndParticipants(new AsyncCallback<ArrayList<Team>>() {
                    @Override
                    public void onFailure(Throwable caught) {Window.alert(caught.getMessage());}

                    @Override
                    public void onSuccess(ArrayList<Team> teams) {
                        rpcService.getAllParticipantsAndTeamNameAndFirmName(new AsyncCallback<ArrayList<Participant>>() {
                            @Override
                            public void onFailure(Throwable caught) {Window.alert(caught.getMessage());}

                            @Override
                            public void onSuccess(ArrayList<Participant> participants) {

                                /**
                                 * Denne metode sørger for at alt fra panelet bliver slettet, så den ikke bygger videre på noget der allerede er der
                                 */
                                content.getGuestView().getGuestStatisticView().getStatisticPanel().clear();

                                content.getGuestView().getGuestStatisticView().getStatisticPanel().add(
                                        new Label("Der er i alt " + firms.size() + " firmaer tilmeldt.")
                                );

                                content.getGuestView().getGuestStatisticView().getStatisticPanel().add(
                                        new Label("Der er i alt " + teams.size() + " hold tilmeldt.")
                                );

                                content.getGuestView().getGuestStatisticView().getStatisticPanel().add(
                                        new Label("Der er i alt " + participants.size() + " deltagere tilmeldt.")
                                );


                                for (Firm firm :firms) {
                                    VerticalPanel tempVerticalPanel = new VerticalPanel();
                                    tempVerticalPanel.addStyleName("fakefakeBtn margintop marginbot");

                                    content.getGuestView().getGuestStatisticView().getStatisticPanel().add(
                                            tempVerticalPanel
                                    );

                                    tempVerticalPanel.add(new Label(firm.getFirmName()));

                                    int numberOfTeamsInFirm = 0;
                                    for (Team team : teams) {
                                        if (team.getFirmID() == firm.getID()){
                                            numberOfTeamsInFirm++;
                                        }
                                    }

                                    int numberOfParticipantsInFirm = 0;
                                    for (Participant participant: participants){
                                        if (participant.getFirmID() == firm.getID()){
                                            numberOfParticipantsInFirm++;
                                        }
                                    }

                                    double pctOfTeams = (double) numberOfTeamsInFirm / (double) teams.size() * (double) 100;
                                    double pctOfParticipants = (double) numberOfParticipantsInFirm / (double) participants.size() * (double) 100;

                                    /**
                                     * Formaterer double så den kun har 2 digits
                                     */
                                    String formattedPctOfTeams = NumberFormat.getFormat("00.00").format(pctOfTeams);
                                    String formattedPctOfParticipants = NumberFormat.getFormat("00.00").format(pctOfParticipants);

                                    tempVerticalPanel.add(new Label("Antal hold i firmaet: " + numberOfTeamsInFirm));
                                    tempVerticalPanel.add(new Label("Antallet af hold udgør: " + formattedPctOfTeams + "%" ));
                                    tempVerticalPanel.add(new Label("Antal deltagere i firmaet: " + numberOfParticipantsInFirm));
                                    tempVerticalPanel.add(new Label("Antallet af deltagere udgør: " + formattedPctOfParticipants + "%"));
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
