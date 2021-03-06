package client.appcontroller;

import client.ui.Content;
import client.ui.admin.AdminView;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ListDataProvider;
import client.rpc.ApplicationServiceAsync;
import shared.DTO.Firm;
import shared.DTO.Participant;
import shared.DTO.Team;

import java.util.ArrayList;
import java.util.Comparator;

public class AdminController {

    private Content content;
    private ApplicationServiceAsync rpcService;
    private Participant currentParticipant;
    private Team currentTeam;
    private Firm currentFirm;
    private AdminView adminView;

    public AdminController(Content content, ApplicationServiceAsync rpcService){
        this.content = content;
        this.rpcService = rpcService;

        /**
         * Der bliver valgt at lave admin vievet i constructoren i det at vi gerne vil have at den laver et nyt view hver gang,
         * så clickHandlers ikke bliver tilføjet flere gang hver gang med logger ind
         */
        this.adminView = new AdminView();
        content.getMainDeck().add(adminView);
        content.getMainDeck().showWidget(adminView);

        addClickHandlers();
    }

    /**
     * Her bliver der tilføjet clickHandlers til adminControlleren, som forbinder knapperne i adminView
     * med clickHandler metoderne i adminControlleren.
     */
    private void addClickHandlers(){
        adminView.addClickHandlers(new AdminClickHandler());
        
        adminView.getChangeParticipantView().addClickHandlers(new ChangeParticipantClickHandler());
        adminView.getChangeTeamView().addClickhandlers(new ChangeTeamClickHandler());
        adminView.getChangeFirmView().addClickHandlers(new ChangeFirmClickHandler());

        adminView.getShowTeamsView().getCreateTeamBtn().addClickHandler(new CreateTeamClickHandler());
        adminView.getShowFirmsView().addClickHandler(new CreateFirmClickHandler());

        // Delegate
        adminView.getShowParticipantsView().setDelegate(new ChangeParticipantDelegateHandler());
        adminView.getShowTeamsView().setDelegate(new ChangeTeamDelegateHandler());
        adminView.getShowFirmsView().setDelegate(new ChangeFirmDelegateHandler());
        
        // Tilføjer ChangeHandler
        adminView.getShowTeamsView().getFirmListBox().addChangeHandler(new SearchParticipantsChangeHandler());
        adminView.getChangeParticipantView().getFirmNameList().addChangeHandler(new CreateParticipantsChangeHandler());
    }

    /******************************************************/
    /**
     *Menu Box (north) clickHandler
     * ClickHandler metoden her gør at det er muligt at skifte centerWidgets ud med de forskellige widgets
     * Deltager view - Hold view - Firma view - Logud
     */
    class AdminClickHandler implements ClickHandler{
        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == adminView.getParticipantsBtn()){
                adminView.changeView(adminView.getShowParticipantsView());
                createParticipantsTable();
            } else if (event.getSource() == adminView.getTeamsBtn()){
                adminView.changeView(adminView.getShowTeamsView());
                createTeamsTable();
            } else if (event.getSource() == adminView.getFirmsBtn()){
                adminView.changeView(adminView.getShowFirmsView());
                createFirmsTable();
            } else if (event.getSource() == adminView.getLogoutBtn()){
                content.switchToGuestView();
            }
        }
    }

    /**
     * Knapper under delegate knappen i celltabel under participantView.
     * Den første knap er Submit knap, som opdatere en deltagers oplysninger, hvis de er blevet ændret
     * Den næste knap er Gå tilbage, som bringer centerView tilbage til celltabel over deltagere
     * Den sidste knap er Slet deltager, som sletter deltagere fra databasen
     */
    class ChangeParticipantClickHandler implements ClickHandler{
        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == adminView.getChangeParticipantView().getSubmitBtn()){

                Participant changingParticipant = new Participant();

                changingParticipant.setName(adminView.getChangeParticipantView().getNameField().getText());
                changingParticipant.setEmail(adminView.getChangeParticipantView().getEmailField().getText());
                changingParticipant.setPersonType(adminView.getChangeParticipantView().getPersonTypeList().getSelectedValue());
                changingParticipant.setCyclistType(adminView.getChangeParticipantView().getCyclistTypeList().getSelectedValue());
                changingParticipant.setPassword(adminView.getChangeParticipantView().getPassField().getText());
                changingParticipant.setFirmID(Integer.parseInt(adminView.getChangeParticipantView().getFirmNameList().getSelectedValue()));
                changingParticipant.setTeamID(Integer.parseInt(adminView.getChangeParticipantView().getTeamNameList().getSelectedValue()));

                rpcService.changeParticipantInfo(currentParticipant, changingParticipant, new AsyncCallback<Participant>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert(caught.getMessage());
                    }
                    @Override
                    public void onSuccess(Participant result) {

                        Window.alert(result != null ? "Personen er ændret" : "Error");

                        createParticipantsTable();
                        currentParticipant = result;
                    }
                });
            } else if (event.getSource() == adminView.getChangeParticipantView().getReturnBtn()){
                adminView.changeView(adminView.getShowParticipantsView());
            } else if (event.getSource() == adminView.getChangeParticipantView().getDeleteBtn()) {
                rpcService.deleteParticipant(currentParticipant.getEmail(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(String result) {
                        Window.alert(result);
                        createParticipantsTable();
                        adminView.changeView(adminView.getShowParticipantsView());
                    }
                });
            }
        }
    }

    /**
     *Knapper under delegate knappen i TeamView celltabel
     * Første knap er submit, som sætter holdets navn lig det der står i textfeltet
     * Anden knap er Gå tilbage knap, som skifter centerWidget tilbage til oversigten over hold
     * Sidste knap er Slet hold, som sletter holdet fra listen over hold
     */
    class ChangeTeamClickHandler implements ClickHandler{
        /**
         * Called when a native click event is fired.
         *
         * @param event the {@link ClickEvent} that was fired
         */
        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == adminView.getChangeTeamView().getSubmitBtn()){

                //Laver de ønskede ændringer på et nyt hold som bliver sendt afsted til client.rpc kaldet
                Team changingTeam = new Team();
                changingTeam.setTeamName(adminView.getChangeTeamView().getTeamNameField().getText());
                changingTeam.setTeamID(currentTeam.getTeamID());

                rpcService.changeTeamInfo(currentTeam, changingTeam, new AsyncCallback<Team>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Serverfejl");
                    }

                    @Override
                    public void onSuccess(Team result) {
                        Window.alert("Holdet er blevet ændret");
                        createTeamsTable();
                        currentTeam = result;
                    }
                });
            } else if (event.getSource() == adminView.getChangeTeamView().getReturnBtn()){
                adminView.changeView(adminView.getShowTeamsView());
            } else if (event.getSource() == adminView.getChangeTeamView().getDeleteBtn()){
                rpcService.deleteTeam(currentTeam.getTeamID(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(String result) {
                        Window.alert(result);
                        createTeamsTable();
                        adminView.changeView(adminView.getShowTeamsView());
                    }
                });
            }
        }
    }

    /**
     *Knapper under delegate knappen i FirmView celltabel
     * Første knap er submit, som sætter firmaets navn lig det der står i textfeltet
     * Anden knap er Gå tilbage knap, som skifter centerWidget tilbage til oversigten over firmaer
     * Sidste knap er Slet firma, som sletter firmaet fra listen over firmaer
     */
    class ChangeFirmClickHandler implements ClickHandler{
        /**
         * Called when a native click event is fired.
         *
         * @param event the {@link ClickEvent} that was fired
         */
        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == adminView.getChangeFirmView().getSubmitBtn()){
                Firm changingFirm = new Firm();
                changingFirm.setFirmName(adminView.getChangeFirmView().getFirmNameField().getText());

                rpcService.changeFirmInfo(currentFirm, changingFirm, new AsyncCallback<Firm>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(Firm result) {
                        if (result == null){
                            Window.alert("Navnet eksisterer allerede. Vælg venligst et andet");
                        } else {
                            Window.alert("Firmaet er ændret");
                            currentFirm = result;
                            createFirmsTable();
                        }
                    }
                });
            } else if (event.getSource() == adminView.getChangeFirmView().getReturnBtn()){
                adminView.changeView(adminView.getShowFirmsView());
            } else if (event.getSource() == adminView.getChangeFirmView().getDeleteBtn()){
                rpcService.deleteFirm(currentFirm.getID(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(String result) {
                        Window.alert(result);
                        createFirmsTable();
                        adminView.changeView(adminView.getShowFirmsView());
                    }
                });
            }
        }
    }


    /**
     * Opret firma ClickHandler
     * For at oprette firma, skal navnet ikke være taget eller bestå af rent white-spacing (mellumrum)
     */
    class CreateFirmClickHandler implements ClickHandler{
        /**
         * Called when a native click event is fired.
         *
         * @param event the {@link ClickEvent} that was fired
         */
        @Override
        public void onClick(ClickEvent event) {

            if (adminView.getShowFirmsView().getFirmNameField().getText().replaceAll("\\s", "").length() > 0){
                rpcService.createFirm(adminView.getShowFirmsView().getFirmNameField().getText(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {}

                    @Override
                    public void onSuccess(String result) {
                        adminView.getShowFirmsView().getErrField().setText(result);
                        adminView.getShowFirmsView().getFirmNameField().setText("");
                        adminView.getShowFirmsView().getErrField().setText("");
                        createFirmsTable();
                    }
                });
            }
        }
    }

    /**
     * Opret team ClickHandler
     * For at oprette et hold skal man vælge firma i listbox, derefter holdkaptajn og tilsidst sætte navn
     */
    class CreateTeamClickHandler implements ClickHandler{
        /**
         * Called when a native click event is fired.
         *
         * @param event the {@link ClickEvent} that was fired
         */
        @Override
        public void onClick(ClickEvent event) {

            if (adminView.getShowTeamsView().getTeamNameField().getText().length() > 0
                    && adminView.getShowTeamsView().getParticipantListBox().getItemCount() >= 1){
                rpcService.createTeam(
                        adminView.getShowTeamsView().getTeamNameField().getText(),
                        adminView.getShowTeamsView().getParticipantListBox().getSelectedValue(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(String result) {
                        Window.alert(result);
                        createTeamsTable();
                    }
                });
            }
        }
    }

    /********************************************************/
    //DelegateHandlers

    /**
     * Rediger firma delegateHandler (specifik firma - objekt)
     * Når knappen trykkes, kommer man frem til et nyt view med clickhandler ChangeFirm
     */
    class ChangeFirmDelegateHandler implements ActionCell.Delegate<Firm>{
        /**
         * Perform the desired action on the given object.
         *
         * @param object the object to be acted upon
         */
        @Override
        public void execute(Firm object) {
            currentFirm = object;
            adminView.getChangeFirmView().getFirmIDLabel().setText("Du er i gang med at ændre: " + currentFirm.getFirmName());
            adminView.getChangeFirmView().getFirmNameField().setText(currentFirm.getFirmName());
            adminView.changeView(adminView.getChangeFirmView());
        }
    }

    /**
     * Rediger Deltager delegateHandler (specifik deltager - objekt)
     * Ved denne knap bliver view skiftet til ChangeParticipantView og dens clickhandlere
     */
    class ChangeParticipantDelegateHandler implements ActionCell.Delegate<Participant>{
        @Override
        public void execute(Participant object) {

            currentParticipant = object;
            rpcService.getParticipantPassword(object.getEmail(), new AsyncCallback<String>() {
                @Override
                public void onFailure(Throwable caught) {
                    Window.alert("Error med at ændre person");
                }

                @Override
                public void onSuccess(String result) {
                    adminView.getChangeParticipantView().getIdLabel().setText(
                            "Du er i gang med at ændre: " + currentParticipant.getEmail()
                    );

                    adminView.getChangeParticipantView().getNameField().setText(currentParticipant.getName());
                    adminView.getChangeParticipantView().getEmailField().setText(currentParticipant.getEmail());
                    adminView.getChangeParticipantView().getPersonTypeList().setValue(3, currentParticipant.getPersonType());
                    adminView.getChangeParticipantView().getPersonTypeList().setItemText(3, currentParticipant.getPersonType());
                    adminView.getChangeParticipantView().getPersonTypeList().setSelectedIndex(3);
                    adminView.getChangeParticipantView().getCyclistTypeList().setValue(4,currentParticipant.getCyclistType());
                    adminView.getChangeParticipantView().getCyclistTypeList().setItemText(4,currentParticipant.getCyclistType());
                    adminView.getChangeParticipantView().getCyclistTypeList().setSelectedIndex(4);
                    adminView.getChangeParticipantView().getPassField().setText(result);
                    
                    rpcService.getAllFirmsAndTeamsAndParticipants(new AsyncCallback<ArrayList<Firm>>() {
                        @Override
                        public void onFailure(Throwable caught) {

                        }

                        @Override
                        public void onSuccess(ArrayList<Firm> result) {

                            adminView.getChangeParticipantView().getFirmNameList().clear();

                            for (Firm firm : result){
                                adminView.getChangeParticipantView().getFirmNameList().addItem(firm.getFirmName(), Integer.toString(firm.getID()));
                            }


                            for (int i = 0; i < adminView.getChangeParticipantView().getFirmNameList().getItemCount(); i++){
                                if (adminView.getChangeParticipantView().getFirmNameList().getValue(i)
                                        .equalsIgnoreCase(Integer.toString(currentParticipant.getFirmID()))){
                                    adminView.getChangeParticipantView().getFirmNameList().setSelectedIndex(i);
                                    break;
                                }
                            }
                            refreshTeamsWhenCreatingParticipant();
                        }
                    });
                    adminView.changeView(adminView.getChangeParticipantView());
                }
            });
        }
    }

    /**
     * Rediger Team delegateHandler (specifik hold - objekt)
     * Denne knap gør at man kan ændre i en valgt holds oplysninger i et celltabel
     */
    class ChangeTeamDelegateHandler implements ActionCell.Delegate<Team>{
        @Override
        public void execute(Team object) {
            currentTeam = object;
            adminView.getChangeTeamView().getIdLabel().setText(
                    "Ændrer nu i hold #" + currentTeam.getTeamID() + " " + currentTeam.getTeamName());

            adminView.getChangeTeamView().getTeamNameField().setText(
                    currentTeam.getTeamName()
            );

            adminView.changeView(adminView.getChangeTeamView());
        }
    }

    /*********************************************************/

    /**
     *Denne metode ændre en listbox (ChangeHandler), som gør at når en admin opretter hold, så når han vælger firma
     * vil listboxen over mulige holdkaptajne blive sat efter pågældende valgte firma
     */
    class SearchParticipantsChangeHandler implements ChangeHandler{
        /**
         * Called when a change event is fired.
         *
         * @param event the {@link ChangeEvent} that was fired
         */
        @Override
        public void onChange(ChangeEvent event) {
            addParticipantsToListBox();
        }
    }

    /**
     *ChangeHandler for at opdatere hold i forhold til firma, når man ændre i en deltagers oplysninger
     */
    class CreateParticipantsChangeHandler implements ChangeHandler{
        /**
         * Called when a change event is fired.
         *
         * @param event the {@link ChangeEvent} that was fired
         */
        @Override
        public void onChange(ChangeEvent event) {
            refreshTeamsWhenCreatingParticipant();
        }
    }

    /**
     * Metode der opdatere listbox efter hvilket firma der er valgt i ovenstående listbox
     * Bliver brugt i 2 changeHandlers og derfor en metode for sig selv
     */
    private void refreshTeamsWhenCreatingParticipant(){
        rpcService.getAllTeamsAndTeamNameAndParticipants(new AsyncCallback<ArrayList<Team>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(ArrayList<Team> result) {

                adminView.getChangeParticipantView().getTeamNameList().clear();

                for (Team team : result) {
                    if (team.getFirmID() == Integer.parseInt(adminView.getChangeParticipantView().getFirmNameList().getSelectedValue()))
                        adminView.getChangeParticipantView().getTeamNameList().addItem(team.getTeamName(), Integer.toString(team.getTeamID()));
                }

                for (int i = 0; i < adminView.getChangeParticipantView().getTeamNameList().getItemCount(); i++) {
                    if (adminView.getChangeParticipantView().getTeamNameList().getValue(i)
                            .equalsIgnoreCase(Integer.toString(currentParticipant.getTeamID()))){
                        adminView.getChangeParticipantView().getTeamNameList().setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Opretter listDataProvidereren og laver derefter tabellen
     */
    private void createParticipantsTable(){
        rpcService.getAllParticipantsAndTeamNameAndFirmName(new AsyncCallback<ArrayList<Participant>>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Error med at lave tabellen");
            }

            @Override
            public void onSuccess(ArrayList<Participant> result) {
                ListDataProvider<Participant> participantListDataProvider = new ListDataProvider<>();

                participantListDataProvider.getList().addAll(result);

                for (Participant par : participantListDataProvider.getList()) {
                    par.setPassword("****");
                }

                initParticipantsTable(participantListDataProvider);
            }
        });
    }

    /**
     * Denne metode opretter det view der er når man klikker på hold-menuen
     */
    private void createTeamsTable(){
        rpcService.getAllTeamsAndTeamNameAndParticipants(new AsyncCallback<ArrayList<Team>>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Error med at lave tabellen");
            }

            @Override
            public void onSuccess(ArrayList<Team> result) {
                ListDataProvider<Team> teamListDataProvider = new ListDataProvider<>();
                teamListDataProvider.getList().addAll(result);

                initTeamsTable(teamListDataProvider);
            }
        });

        /**
         * Denne kodeblok gør det muligt at tilføje hold.
         * Dette gør den ved at lave listboxes med firms og deres participants
         */
        rpcService.getAllFirmsAndTeamsAndParticipants(new AsyncCallback<ArrayList<Firm>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(ArrayList<Firm> result) {

                adminView.getShowTeamsView().getFirmListBox().clear();


                for (Firm firm : result) {
                    adminView.getShowTeamsView().getFirmListBox().addItem(firm.getFirmName(), Integer.toString(firm.getID()));
                }

                addParticipantsToListBox();

            }
        });

    }

    /**
     * Denne metode opretter det view der er når man klikker på team-menuen
     */
    private void createFirmsTable(){
        rpcService.getAllFirmsAndTeamsAndParticipants(new AsyncCallback<ArrayList<Firm>>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("CreateFirmsTable() err " + caught.getMessage());
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(ArrayList<Firm> result) {
                ListDataProvider<Firm> firmListDataProvider = new ListDataProvider<>();
                firmListDataProvider.getList().addAll(result);

                initFirmsTable(firmListDataProvider);
            }
        });
    }

    /**
     * Opretter tabellen
     * @param participantListDataProvider dette er en liste over alle deltagerne der skal med i tabel
     */
    private void initParticipantsTable(ListDataProvider<Participant> participantListDataProvider){
        CellTable<Participant> cellTable = adminView.getShowParticipantsView().getCellTable();

        participantListDataProvider.addDataDisplay(cellTable);
        //http://www.gwtproject.org/doc/latest/DevGuideUiCellWidgets.html

        /**
         * Fjerner alle kolonner så den laver dem alle igen
         */
        for (int i = 0; i < cellTable.getColumnCount();){
            cellTable.removeColumn(0);
        }

        /***
         * Koden forneden skal kun køre hvis tabellen ikke allerede er lavet.
         * Dette er for at forhindre, at den tilføjer nye kolonner hver gang admin logger ind
         */
        Column nameCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {
                return object.getName() != null ? object.getName() : "*missing*";
            }
        };

        Column emailCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {
                return object.getEmail() != null ? object.getEmail() : "*missing*";
            }
        };

        Column passCol = new Column<Participant, String>(adminView.getShowParticipantsView().getClickableTextCell()) {
            @Override
            public String getValue(Participant object) {
                return object.getPassword();
            }
        };

        Column personTypeCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {
                return object.getPersonType() != null ? object.getPersonType() : "*missing*";
            }
        };

        Column cyclistTypeCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {
                return object.getCyclistType() != null ? object.getCyclistType() : "*missing*";
            }
        };

        Column firmNameCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {

                return object.getFirmName() != null ? object.getFirmName() : "*missing*";
            }
        };

        Column teamIDCol = new Column<Participant, Number>(new NumberCell()) {
            @Override
            public Integer getValue(Participant object) {
                return object.getTeamID();
            }
        };

        Column teamNameCol = new TextColumn<Participant>() {
            @Override
            public String getValue(Participant object) {
                return object.getTeamName();
            }
        };

        Column changeParticipant = new Column<Participant, Participant>(new ActionCell<>("Rediger", adminView.getShowParticipantsView().getDelegate())) {
            @Override
            public Participant getValue(Participant object) {
                return object;
            }
        };

        cellTable.addColumn(nameCol, "Navn");
        cellTable.addColumn(emailCol, "Email");
        cellTable.addColumn(personTypeCol, "Person-type");
        cellTable.addColumn(cyclistTypeCol, "Cyclist-type");
        cellTable.addColumn(passCol, "Password");
        cellTable.addColumn(firmNameCol, "Firma");
        cellTable.addColumn(teamIDCol, "Hold ID");
        cellTable.addColumn(teamNameCol, "Holdnavn");
        cellTable.addColumn(changeParticipant);

        nameCol.setSortable(true);
        emailCol.setSortable(true);
        personTypeCol.setSortable(true);
        cyclistTypeCol.setSortable(true);
        firmNameCol.setSortable(true);
        teamIDCol.setSortable(true);
        teamNameCol.setSortable(true);

        ColumnSortEvent.ListHandler<Participant> sortHandler = new ColumnSortEvent.ListHandler<Participant>(participantListDataProvider.getList());

        sortHandler.setComparator(nameCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(emailCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getEmail().compareTo(o2.getEmail());
            }
        });
        sortHandler.setComparator(personTypeCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getPersonType().compareTo(o2.getPersonType());
            }
        });
        sortHandler.setComparator(cyclistTypeCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getCyclistType().compareTo(o2.getCyclistType());
            }
        });
        sortHandler.setComparator(firmNameCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getFirmName().compareTo(o2.getFirmName());
            }
        });
        sortHandler.setComparator(teamIDCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return Integer.compare(o1.getTeamID(), o2.getTeamID());
            }
        });
        sortHandler.setComparator(teamNameCol, new Comparator<Participant>() {
            @Override
            public int compare(Participant o1, Participant o2) {
                return o1.getTeamName().compareTo(o2.getTeamName());
            }
        });

        cellTable.addColumnSortHandler(sortHandler);

        /***
         * Denne metode sørger for at fjerne "****" fra password under ShowParticipantsView og sætter de rigtige passwords ind.
         * aka ClickableTextCellHandler
         */
        passCol.setFieldUpdater(new FieldUpdater<Participant, String>() {
            @Override
            public void update(int index, Participant object, String value) {

                rpcService.getParticipantPassword(object.getEmail(), new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(String result) {
                        object.setPassword(result);
                        participantListDataProvider.getList().set(index, object);
                        participantListDataProvider.refresh();
                    }
                });
            }
        });
    }

    /**
     * @param teamListDataProvider dette er listen over alle hold der skal være i hold-menuens tabel
     */
    private void initTeamsTable(ListDataProvider<Team> teamListDataProvider){
        CellTable<Team> cellTable = adminView.getShowTeamsView().getCellTable();
        teamListDataProvider.addDataDisplay(cellTable);

        for (int i = 0; i < cellTable.getColumnCount();){
            cellTable.removeColumn(0);
        }

        Column teamID = new Column<Team, Number>(new NumberCell()) {
            @Override
            public Integer getValue(Team object) {
                return  object.getTeamID();
            }
        };

        Column teamName = new TextColumn<Team>() {
            @Override
            public String getValue(Team object) {
                return object.getTeamName();
            }
        };

        Column firmName = new TextColumn<Team>() {
            @Override
            public String getValue(Team object) {
                return object.getFirmName();
            }
        };
//
        Column numberOfParticipants = new Column<Team, Number>(new NumberCell()) {
            @Override
            public Number getValue(Team object) {
                return object.getParticipants().size();
            }
        };

        Column changeTeam = new Column<Team, Team>(new ActionCell<Team>("Rediger", adminView.getShowTeamsView().getDelegate())) {
            @Override
            public Team getValue(Team object) {
                return object;
            }
        };

        cellTable.addColumn(teamID, "Hold ID");
        cellTable.addColumn(teamName, "Holdnavn");
        cellTable.addColumn(firmName, "Firma");
        cellTable.addColumn(numberOfParticipants, "Antal deltagere");
        cellTable.addColumn(changeTeam);

        teamID.setSortable(true);
        teamName.setSortable(true);
        firmName.setSortable(true);
        numberOfParticipants.setSortable(true);
        changeTeam.setSortable(true);

        ColumnSortEvent.ListHandler<Team> sortHandler = new ColumnSortEvent.ListHandler<Team>(teamListDataProvider.getList());

        sortHandler.setComparator(teamID, new Comparator<Team>() {
            @Override
            public int compare(Team o1, Team o2) {
                return Integer.compare(o1.getTeamID(), o2.getTeamID());
            }
        });

        sortHandler.setComparator(teamName, new Comparator<Team>() {
            @Override
            public int compare(Team o1, Team o2) {
                return o1.getTeamName().compareTo(o2.getTeamName());
            }
        });

        sortHandler.setComparator(firmName, new Comparator<Team>() {
            @Override
            public int compare(Team o1, Team o2) {
                return o1.getFirmName().compareTo(o2.getFirmName());
            }
        });

        sortHandler.setComparator(numberOfParticipants, new Comparator<Team>() {
            @Override
            public int compare(Team o1, Team o2) {
                return Integer.compare(o1.getParticipants().size(), o2.getParticipants().size());
            }
        });

        cellTable.addColumnSortHandler(sortHandler);
    }

    /**
     * Opretter tabllen med firmaer
     * @param firmListDataProvider
     */
    private void initFirmsTable(ListDataProvider<Firm> firmListDataProvider){

        CellTable<Firm> cellTable = adminView.getShowFirmsView().getCellTable();

        firmListDataProvider.addDataDisplay(cellTable);

        for (int i = 0; i < cellTable.getColumnCount();){
            cellTable.removeColumn(0);
        }

        Column firmID = new Column<Firm, Number>(new NumberCell()) {
            @Override
            public Integer getValue(Firm object) {
                return object.getID();
            }
        };

        Column firmName = new TextColumn<Firm>() {
            @Override
            public String getValue(Firm object) {
                return object.getFirmName();
            }
        };

        Column numberOfParticipants = new Column<Firm, Number>(new NumberCell()) {
            @Override
            public Integer getValue(Firm object) {
                return object.getParticipants().size();
            }
        };

        Column numberOfTeams = new Column<Firm, Number>(new NumberCell()) {
            @Override
            public Number getValue(Firm object) {
                return object.getTeams().size();
            }
        };

        Column changeFirm = new Column<Firm, Firm>(new ActionCell<Firm>("Rediger", adminView.getShowFirmsView().getDelegate())) {
            @Override
            public Firm getValue(Firm object) {
                return object;
            }
        };

        cellTable.addColumn(firmID, "Firma ID");
        cellTable.addColumn(firmName, "Firmanavn");
        cellTable.addColumn(numberOfParticipants, "Antal deltagere");
        cellTable.addColumn(numberOfTeams, "Antal hold");
        cellTable.addColumn(changeFirm);

        firmID.setSortable(true);
        firmName.setSortable(true);
        numberOfParticipants.setSortable(true);
        numberOfTeams.setSortable(true);

        ColumnSortEvent.ListHandler<Firm> sortHandler = new ColumnSortEvent.ListHandler<>(firmListDataProvider.getList());

        sortHandler.setComparator(firmID, new Comparator<Firm>() {
            @Override
            public int compare(Firm o1, Firm o2) {
                return Integer.compare(o1.getID(), o2.getID());
            }
        });
        sortHandler.setComparator(firmName, new Comparator<Firm>() {
            @Override
            public int compare(Firm o1, Firm o2) {
                return o1.getFirmName().compareTo(o2.getFirmName());
            }
        });
        sortHandler.setComparator(numberOfParticipants, new Comparator<Firm>() {
            @Override
            public int compare(Firm o1, Firm o2) {
                return Integer.compare(o1.getParticipants().size(), o2.getParticipants().size());
            }
        });
        sortHandler.setComparator(numberOfTeams, new Comparator<Firm>() {
            @Override
            public int compare(Firm o1, Firm o2) {
                return Integer.compare(o1.getTeams().size(), o2.getTeams().size());
            }
        });

        cellTable.addColumnSortHandler(sortHandler);
    }

    /**
     *Denne metode bliver brugt af andre metoder, for at vise hvilke deltager der PARTICIPANTS
     */
    private void addParticipantsToListBox(){

        adminView.getShowTeamsView().getParticipantListBox().clear();

        rpcService.getAllParticipantsInFirmFromFirmID(Integer.parseInt(adminView.getShowTeamsView().getFirmListBox().getSelectedValue()), new AsyncCallback<ArrayList<Participant>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(ArrayList<Participant> result) {
                for (Participant participant : result) {
                    if (participant.getPersonType().equalsIgnoreCase("PARTICIPANT")){
                        adminView.getShowTeamsView().getParticipantListBox().addItem(participant.getEmail());
                    }
                }
            }
        });
    }



}
