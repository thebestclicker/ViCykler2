package shared.DTO;//Alexander Van Le && Oliver Lange

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Kilde: Y. Daniel Liang (2015), s. 431
 */
public class Participant extends Person implements IsSerializable {
    //Variabler
    private String cyclistType;
    private String firmName;
    private String teamID;


    //Default constructor
    public Participant(){
        super("null", "null", "null");
        cyclistType = "null";
        firmName = "null";
        teamID = "null";
    }

    //Getter
    public String getCyclistType() {
        return cyclistType;
    }

    //Setter
    public void setCyclistType(String cyclistType) {
        this.cyclistType = cyclistType;
    }

    public void setFirmName(String firmName) {
        this.firmName = firmName;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    public String getFirmName() {
        return firmName;
    }

    public String getTeamID() {
        return teamID;
    }
}
