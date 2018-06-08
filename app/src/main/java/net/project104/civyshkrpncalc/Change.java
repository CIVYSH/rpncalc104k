package net.project104.civyshkrpncalc;

import java.io.Serializable;

interface Change extends Serializable {
    void undo();
    void redo();
}
