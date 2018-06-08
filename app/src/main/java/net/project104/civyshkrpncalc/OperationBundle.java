package net.project104.civyshkrpncalc;

import java.math.BigDecimal;
import java.util.ArrayList;

class OperationBundle {
    BigDecimal[] operands;
    ArrayList<BigDecimal> results = new ArrayList<>(1);
    CalculatorError error = null;

    public OperationBundle(BigDecimal[] operands) {
        this.operands = operands.clone();
    }

    public void add(BigDecimal number){
        results.add(number);
    }
}
