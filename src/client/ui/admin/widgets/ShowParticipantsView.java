package client.ui.admin.widgets;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import shared.DTO.Participant;

public class ShowParticipantsView extends Composite {
    interface ShowParticipantsViewUiBinder extends UiBinder<HTMLPanel, ShowParticipantsView> {
    }

    private static ShowParticipantsViewUiBinder ourUiBinder = GWT.create(ShowParticipantsViewUiBinder.class);

    private ClickableTextCell clickableTextCell;

    private ActionCell.Delegate<Participant> delegate;

    @UiField
    CellTable<Participant> cellTable;

    public ShowParticipantsView() {
        initWidget(ourUiBinder.createAndBindUi(this));
        clickableTextCell = new ClickableTextCell();
        cellTable.setVisibleRange(0,1000);
    }


    /**
     * Jeg har valgt at flytte denne metode til AdminController klassen da der er for meget logik til at det giver mening at have den her i dette view.
     * Det er bedre at samle al logikken i controllerne.
     */

    public ClickableTextCell getClickableTextCell() {
        return clickableTextCell;
    }


    public CellTable<Participant> getCellTable() {
        return cellTable;
    }

    public void setDelegate(ActionCell.Delegate<Participant> delegate) {
        this.delegate = delegate;
    }

    public ActionCell.Delegate<Participant> getDelegate() {
        return delegate;
    }
}