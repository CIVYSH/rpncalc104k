package net.project104.civyshkrpncalc;

import java.math.BigDecimal;

class SwapChange implements Change {
    private int startPosition, endPosition;
    private HistorySaver saver;
    private String undoText;

    public SwapChange(HistorySaver saver, String undoText, int startPosition, int endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.saver = saver;
        this.undoText = undoText;
    }


    @Override
    public void undo() {
        ActivityMain activity = saver.getActivity();

        BigDecimal draggingNumber = activity.numberStackRemove(endPosition);
        activity.numberStackAdd(startPosition, draggingNumber);
        activity.setEditableNumber(undoText);

        activity.notifyStackChanged();
    }

    @Override
    public void redo() {
        ActivityMain activity = saver.getActivity();

//            activity.clickedSwap(false, startPosition, endPosition);
        BigDecimal draggingNumber = activity.numberStackRemove(startPosition);
        activity.numberStackAdd(endPosition, draggingNumber);
        activity.resetEditableNumber();

        activity.notifyStackChanged();
    }
}
