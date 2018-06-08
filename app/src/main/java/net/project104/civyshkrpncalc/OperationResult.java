package net.project104.civyshkrpncalc;

import java.math.BigDecimal;
import java.util.ArrayList;

class OperationResult {
    BigDecimal[] operands;
    ArrayList<BigDecimal> results = new ArrayList<>(1);
    String error = null;

    OperationResult(BigDecimal[] operands) {
        this.operands = operands.clone();
    }

    void add(BigDecimal number){
        results.add(number);
    }
}
