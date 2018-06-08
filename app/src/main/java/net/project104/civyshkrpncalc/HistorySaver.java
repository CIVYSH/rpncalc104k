package net.project104.civyshkrpncalc;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

class HistorySaver implements Serializable {
    private transient WeakReference<ActivityMain> activity;
    private LinkedList<Change> changes = new LinkedList<>();
    private int currentChangeIndex;

    HistorySaver(ActivityMain activity) {
        this.activity = new WeakReference<>(activity);
        currentChangeIndex = -1;
    }

    private void insertChange(int position, Change change) {
        if (position >= changes.size()) {
            changes.add(change);
            currentChangeIndex = changes.size() - 1;
        } else {
            changes.set(position, change);
            for (int i = position + 1, size = changes.size(); i < size; i++) {
                changes.removeLast();
            }
            currentChangeIndex = position;
        }
    }

    private Change getPrevious() {
        try {
            return changes.get(currentChangeIndex);
        }catch(IndexOutOfBoundsException e) {
            return null;
        }
    }

    void saveSimpleChange(SimpleChange change) {
        Change previousChange = getPrevious();
        if (previousChange instanceof SimpleChange) {
            SimpleChange mergedChange = change.merge((SimpleChange) previousChange);
            if (mergedChange != null) {
                insertChange(currentChangeIndex, mergedChange);
            } else {
                insertChange(currentChangeIndex + 1, change);
            }
        } else {
            insertChange(currentChangeIndex + 1, change);
        }
    }

    void saveSwapChange(SwapChange change) {
        insertChange(currentChangeIndex + 1, change);
    }

    synchronized void goBack() {
        if (currentChangeIndex >= 0) {
            changes.get(currentChangeIndex).undo();
            --currentChangeIndex;
        }
    }

    synchronized void goForward() {
        if (currentChangeIndex < changes.size() - 1) {
            ++currentChangeIndex;
            changes.get(currentChangeIndex).redo();
        }
    }

    ActivityMain getActivity() {
        return activity.get();
    }

    void setActivity(ActivityMain activity) {
        this.activity = new WeakReference<>(activity);
    }
}
