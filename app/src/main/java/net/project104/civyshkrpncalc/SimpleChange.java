package net.project104.civyshkrpncalc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

class SimpleChange implements Change {
    private Deque<BigDecimal> redoNumbers = new LinkedList<>();//ArrayDeque from API 9
    private Deque<BigDecimal> undoNumbers = new LinkedList<>();
    private int redoNumbersSize = 0;
    private String redoText, undoText;
    private HistorySaver saver;
    private boolean canMerge;
    private String tag = "";

    SimpleChange(HistorySaver saver, String oldText) {
        this(saver, oldText, false);
    }

    SimpleChange(HistorySaver saver, String oldText, boolean canMerge) {
        this.undoText = oldText;
        this.saver = saver;
        this.canMerge = canMerge;
    }

    void addOld(BigDecimal number) {
        undoNumbers.add(number);
    }

    @Override
    public void undo() {
        ActivityMain activity = saver.getActivity();

        BigDecimal[] poppedNew = activity.popNumbers(redoNumbersSize);
        redoNumbers.clear();
        redoNumbers.addAll(Arrays.asList(poppedNew));
        activity.addNumbers(undoNumbers);

        redoText = activity.getEditableNumberStr();
        activity.setEditableNumber(undoText);
    }

    @Override
    public void redo() {
        ActivityMain activity = saver.getActivity();

        activity.popNumbers(undoNumbers.size());
        activity.addNumbers(redoNumbers);

        activity.setEditableNumber(redoText);
    }

    @Override
    public String toString() {
        return String.format("Change %s", tag);
    }

    public SimpleChange merge(SimpleChange other) {
        if (!canMerge) {
            return null;
        } else if (other != null && other.canMerge) {
            //This merge assumes that both Changes represent Digit, Decimal or Scientific changes
            //Delete and other changes need different code for merge to work
            SimpleChange newChange = new SimpleChange(saver, other.undoText, true);
            newChange.redoNumbersSize = 1;
            newChange.undoNumbers = other.undoNumbers;
            return newChange;
        } else {
            return null;
        }
    }

    public void setTag(String t){
        tag = t;
    }

    public void setRedoNumbersSize(int redoNumbersSize) {
        this.redoNumbersSize = redoNumbersSize;
    }
}
