package client.appcontroller;

import client.ui.Content;
import client.ui.admin.AdminView;
import client.ui.admin.widgets.ShowFirmsView;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ListDataProvider;
import rpc.ApplicationServiceAsync;
import shared.DTO.Firm;
import shared.DTO.Participant;
import shared.DTO.Team;

import java.util.ArrayList;
import java.util.List;
//import server.withoutDB.Data;

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
         * så cli
         */
        this.adminView = new AdminView();
        content.getMainDeck().add(adminView);
        content.getMainDeck().showWidget(adminView);

        addClickhandlers();
    }

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

    class ChangeTeamClickHandler implements ClickHandler{
        /**
         * Called when a native click event is fired.
         *
         * @param event the {@link ClickEvent} that was fired
         */
        @Override
        public void onClick(ClickEvent event) {
            if (event.getSource() == adminView.getChangeTeamView().getSubmitBtn()){

                //Laver et det ønskede ændringer på et nyt hold som bliver sendt afsted
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
                        createTeamsTable();
                        adminView.changeView(adminView.getShowTeamsView());
                    }
                });
            }
        }
    }

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
                    adminView.getChangeParticipantView().getFirmNameField().setText(currentParticipant.getFirmName());
                    adminView.getChangeParticipantView().getTeamNameField().setText(currentParticipant.getTeamName());

                    adminView.changeView(adminView.getChangeParticipantView());
                }
            });
        }
    }

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
                changingParticipant.setFirmName(adminView.getChangeParticipantView().getFirmNameField().getText());
                changingParticipant.setTeamName(adminView.getChangeParticipantView().getTeamNameField().getText());

                rpcService.changeParticipantInfo(currentParticipant, changingParticipant, new AsyncCallback<Participant>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert(caught.getMessage());
                    }
                    @Override
                    public void onSuccess(Participant result) {
                        Window.alert("Personen er ændret");
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
                        createParticipantsTable();
                        adminView.changeView(adminView.getShowParticipantsView());
                    }
                });
            }
        }
    }

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
                        createFirmsTable();
                        adminView.changeView(adminView.getShowFirmsView());
                    }
                });
            }
        }
    }

    class ShowFirmsViewClickHandler implements ClickHandler{
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

    private void addClickhandlers(){
        adminView.addClickHandlers(new AdminClickHandler());
        adminView.getShowParticipantsView().setDelegate(new ChangeParticipantDelegateHandler());
        adminView.getChangeParticipantView().addClickHandlers(new ChangeParticipantClickHandler());
        adminView.getShowTeamsView().setDelegate(new ChangeTeamDelegateHandler());
        adminView.getChangeTeamView().addClickhandlers(new ChangeTeamClickHandler());
        adminView.getShowFirmsView().setDelegate(new ChangeFirmDelegateHandler());
        adminView.getChangeFirmView().addClickHandlers(new ChangeFirmClickHandler());
        adminView.getShowFirmsView().addClickHandler(new ShowFirmsViewClickHandler());
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

//                adminView.getShowParticipantsView().initTable(participantListDataProvider);
                initParticipantsTable(participantListDataProvider);
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

    private void createTeamsTable(){
        rpcService.getAllTeams(new AsyncCallback<ArrayList<Team>>() {
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
    }

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
    }

    private void createFirmsTable(){
        rpcService.getAllFirms(new AsyncCallback<ArrayList<Firm>>() {
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
     * Opretter tabllen
     * @param firmListDataProvider
     */
    private void initFirmsTable(ListDataProvider<Firm> firmListDataProvider){

        CellTable<Firm> cellTable = adminView.getShowFirmsView().getCellTable();

        firmListDataProvider.addDataDisplay(cellTable);

//        Window.alert("Navn " + firmListDataProvider.getList().get(0).getParticipants().get(0));
//        Window.alert(Integer.toString((int)firmListDataProvider.getList().get(0).getParticipants().size()));

        for (int i = 0; i < cellTable.getColumnCount();){
            cellTable.removeColumn(0);
        }


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

        cellTable.addColumn(firmName, "Firmanavn");
        cellTable.addColumn(numberOfParticipants, "Antal deltagere");
        cellTable.addColumn(numberOfTeams, "Antal hold");
        cellTable.addColumn(changeFirm);
    }


}
