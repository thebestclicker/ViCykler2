package server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.mysql.jdbc.Connection;
import com.sun.org.apache.regexp.internal.REUtil;
import rpc.ApplicationService;
import shared.DTO.*;


import javax.xml.transform.Result;
import java.sql.*;
import java.util.ArrayList;
import java.util.regex.MatchResult;

/**
 * DAO
 */
public class ApplicationServiceImpl extends RemoteServiceServlet implements ApplicationService {

    private final String DATABASE_URL = "jdbc:mysql://localhost:3306/vicykler";
    private final String USERNAME = "dummy";
    private final String PASSWORD = "Meme_1234";
    //Meme_1234
    private Connection connection;

    public ApplicationServiceImpl(){
        try {
             connection = (Connection) DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
            System.out.println("Successful");
        } catch (SQLException err){
            err.printStackTrace();
            //https://stackoverflow.com/questions/2434592/difference-in-system-exit0-system-exit-1-system-exit1-in-java
            System.exit(1);
        }
    }

    @Override
    public Person authorizePerson(String email, String password) throws Exception {
        Person foundPerson = null;

        PreparedStatement findMatch = connection.prepareStatement("SELECT * FROM persons WHERE Email LIKE ? AND Password LIKE ?");
        findMatch.setString(1, email);
        findMatch.setString(2, password);

        try {
            ResultSet resultSet = findMatch.executeQuery();

            if (resultSet.next()){
                if (resultSet.getString("PersonType").equalsIgnoreCase("PARTICIPANT")){
                    Participant foundParticipant = new Participant();
                    foundParticipant.setName(resultSet.getString("PersonName"));
                    foundParticipant.setEmail(resultSet.getString("Email"));
                    foundParticipant.setCyclistType(resultSet.getString("CyclistType"));
                    foundParticipant.setTeamID(resultSet.getInt("TeamID"));
                    foundParticipant.setFirmName(resultSet.getString("FirmName"));

                    foundPerson = foundParticipant;
                } else if (resultSet.getString("PersonType").equalsIgnoreCase("ADMIN")){
                    foundPerson = new Admin();
                }
            }


        } catch (SQLException err){
            err.printStackTrace();
        }
        return foundPerson;
    }

    /***
     *
     * @return The names of every person in the database
     * @throws Exception
     */
    @Override
    public String returnPersons() throws Exception {

        ArrayList<String> personNames = new ArrayList<>();

        PreparedStatement findPerson = connection.prepareStatement("SELECT PersonName FROM persons");
        ResultSet resultSet = findPerson.executeQuery();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        while(resultSet.next()){
            personNames.add(resultSet.getString(1));
        }

        System.out.println(String.join(", ", personNames));
        return String.join(", ", personNames);
    }

    @Override
    public ArrayList<Participant> getAllParticipants() throws Exception {

        System.out.println("Running: getAllPersons()");

        ArrayList<Participant> participants = new ArrayList<>();
        int numberOfColums;
        Participant participant;
        ResultSet resultSet = null;

        try {
            PreparedStatement findPersons = connection.prepareStatement("SELECT * FROM persons INNER JOIN teams ON persons.TeamID = teams.TeamID");
            resultSet = findPersons.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            numberOfColums = metaData.getColumnCount();

            System.out.println("Number of colums: " + numberOfColums);

            while(resultSet.next()){
                if(!resultSet.getString("PersonType").equalsIgnoreCase("ADMIN")){
                    participant = new Participant();

                    participant.setName(resultSet.getString("PersonName").toLowerCase());
                    participant.setEmail(resultSet.getString("Email").toLowerCase());
                    participant.setCyclistType(resultSet.getString("CyclistType").toLowerCase());
                    participant.setPersonType(resultSet.getString("PersonType"));
                    participant.setFirmName(resultSet.getString("FirmName"));
                    participant.setTeamID(resultSet.getInt("TeamID"));
                    participant.setTeamName(resultSet.getString("TeamName"));


                    participants.add(participant);
                }
            }
        } catch (SQLException err){
            err.printStackTrace();
        } finally {
            try {
                resultSet.close();
            } catch (Exception err){
                err.printStackTrace();
            }
        }

        System.out.println(participants.size());
        return participants;
    }

    @Override
    public boolean createParticipant(String email, String name, String cyclistType, String password) throws Exception {
        ArrayList<String> emailList = new ArrayList<>();
        ResultSet emailsResultSet = null;

        try {

            PreparedStatement emailsPreparedStatement = connection.prepareStatement("SELECT Email FROM persons WHERE Email LIKE ?");
            emailsPreparedStatement.setString(1, Character.toString(email.charAt(0)) + "%");
            emailsResultSet = emailsPreparedStatement.executeQuery();

            while(emailsResultSet.next()){
                emailList.add(emailsResultSet.getString("Email"));
            }

            for (String emailName: emailList) {
                if (emailName.equalsIgnoreCase(email)){
                    return false;
                }
            }

            PreparedStatement createParticipant = connection.prepareStatement("INSERT INTO persons(Email, PersonName, CyclistType, Password, PersonType) VALUES (?,?,?,?, 'PARTICIPANT')");
            createParticipant.setString(1, email);
            createParticipant.setString(2, name);
            createParticipant.setString(3, cyclistType);
            createParticipant.setString(4, password);

            createParticipant.executeUpdate();

        } catch (SQLException err){
            err.printStackTrace();
        } finally {
            try {
                emailsResultSet.close();
            } catch (Exception err){
                err.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean createTeam(String name, Participant teamCaptain) throws Exception {

        try {
            PreparedStatement createTeam = connection.prepareStatement("INSERT INTO teams(TeamName) VALUES (?)");
            PreparedStatement findParticipant = connection.prepareStatement("UPDATE persons SET PersonType = 'TEAMCAPTAIN' WHERE Email LIKE ?");


            createTeam.setString(1, name);
            findParticipant.setString(1, teamCaptain.getEmail());

            createTeam.executeUpdate();
            findParticipant.executeUpdate();

            return true;

        } catch (SQLException err){
            err.printStackTrace();
        }

        return false;
    }

    @Override
    public String getParticipantName(String email) throws Exception {

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT PersonName FROM persons WHERE Email LIKE ?");
        preparedStatement.setString(1, email);
        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();

        return resultSet.getString("PersonName");
    }

    @Override
    public String getParticipantCyclistType(String email) throws Exception {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT CyclistType FROM persons WHERE Email LIKE ?");
        preparedStatement.setString(1, email);
        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();

        return resultSet.getString("CyclistType");
    }

    @Override
    public String getParticipantFirmName(String email) throws Exception {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT FirmName FROM persons WHERE Email LIKE ?");
        preparedStatement.setString(1, email);
        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();

        return resultSet.getString("FirmName");
    }

    @Override
    public String getParticipantTeamName(String email) throws Exception {

        PreparedStatement getTeamID = connection.prepareStatement("SELECT TeamID FROM persons WHERE Email = ?");
        getTeamID.setString(1, email);
        ResultSet getTeamIDResult = getTeamID.executeQuery();
        getTeamIDResult.next();
        int teamID = getTeamIDResult.getInt("TeamID");


        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT teams.TeamName, persons.PersonName FROM teams INNER JOIN persons ON teams.TeamID WHERE teams.TeamID = ? AND persons.Email = ?");
        preparedStatement.setInt(1, teamID);
        preparedStatement.setString(2, email);
        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();

        System.out.println("TeamID: " + teamID);
        return resultSet.getString("TeamName");
    }

    @Override
    public String getParticipantPassword(String email) throws Exception {
        PreparedStatement getPassword = connection.prepareStatement("SELECT Password FROM persons WHERE Email = ?");
        getPassword.setString(1, email);
        ResultSet resultSet = getPassword.executeQuery();
        resultSet.next();
        return resultSet.getString("Password");
    }

    @Override
    public boolean changeParticipantInfo(Participant currentParticipant, Participant changingParticipant) throws Exception {

        try {
            PreparedStatement getTeamID = connection.prepareStatement("SELECT TeamID FROM teams WHERE teams.TeamName LIKE ?");
            getTeamID.setString(1, changingParticipant.getTeamName());
            ResultSet getTeamIDRes = getTeamID.executeQuery();
            getTeamIDRes.next();
            int teamID = getTeamIDRes.getInt("TeamID");


            PreparedStatement updateParticipant = connection.prepareStatement(
                    "UPDATE persons SET PersonName = ?, Email = ?,  " +
                            "Password = ?, PersonType = ?, CyclistType = ?, FirmName = ?, TeamID = ? WHERE Email = ?");

            updateParticipant.setString(1,changingParticipant.getName());
            updateParticipant.setString(2,changingParticipant.getEmail());
            updateParticipant.setString(3,changingParticipant.getPassword());
            updateParticipant.setString(4,changingParticipant.getPersonType());
            updateParticipant.setString(5,changingParticipant.getCyclistType());
            updateParticipant.setString(6,changingParticipant.getFirmName());
            updateParticipant.setInt(7,teamID);
            updateParticipant.setString(8, currentParticipant.getEmail());

            updateParticipant.executeUpdate();
            return true;
        } catch (SQLException err){
            err.printStackTrace();
            return false;
        }
    }

    @Override
    public String getGuestStatisticView() throws Exception {

        PreparedStatement statisticForGuest = connection.prepareStatement("SELECT persons.PersonName, persons.FirmName, teams.TeamName " +
                "FROM persons INNER JOIN teams ON teams.TeamID = persons.TeamID");
        return null;
    }

    @Override
    public ArrayList<Team> getAllTeams() throws Exception {
        ArrayList<Team> teams = new ArrayList<>();


        try{
            PreparedStatement getTeams = connection.prepareStatement("SELECT * FROM teams");
            ResultSet getTeamsRes = getTeams.executeQuery();

            while (getTeamsRes.next()){
                Team tempTeam = new Team();
                tempTeam.setTeamID(getTeamsRes.getInt("TeamID"));
                tempTeam.setTeamName(getTeamsRes.getString("TeamName"));
                tempTeam.setFirmName(getTeamsRes.getString("FirmName"));

                teams.add(tempTeam);
            }

            for (Team team: teams) {
                PreparedStatement getParticipants = connection.prepareStatement("SELECT persons.email FROM persons INNER JOIN teams ON persons.TeamID = teams.TeamID WHERE teams.TeamID = ?");
                getParticipants.setInt(1, team.getTeamID());

                ResultSet participantsRes = getParticipants.executeQuery();

                while(participantsRes.next()){
                    team.getParticipants().add(participantsRes.getString("Email"));
                }
            }

            return teams;
        } catch (Exception err){
            err.printStackTrace();
        }


         return teams;
    }

    @Override
    public ArrayList<Firm> getAllFirms() throws Exception {
        return null;
    }

    @Override
    public boolean createFirm(String name) {
        return false;
    }

    @Override
    public boolean changeTeamInfo(Team currentTeam, Team changingTeam) throws Exception {
        try {
            PreparedStatement changeTeam = connection.prepareStatement("UPDATE teams SET TeamName = ? WHERE TeamID = ?");
            changeTeam.setString(1, changingTeam.getTeamName());
            changeTeam.setInt(2, currentTeam.getTeamID());

            changeTeam.executeUpdate();

            return true;

        } catch (SQLException err){
            err.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean changeFirmInfo(Firm currentFirm, Firm changingFirm) throws Exception {
        return false;
    }
}