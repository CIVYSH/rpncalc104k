package net.project104.civyshkrpncalc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import static net.project104.civyshkrpncalc.ActivityMain.BIG_EULER;
import static net.project104.civyshkrpncalc.ActivityMain.BIG_PHI;
import static net.project104.civyshkrpncalc.ActivityMain.BIG_PI;
import static net.project104.civyshkrpncalc.CalculatorError.*;

public class Calculator {

    public final static int GOOD_PRECISION = 10;
    public final static RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public final static MathContext DEFAULT_MATH_CONTEXT = new MathContext(GOOD_PRECISION, ROUNDING_MODE);

    private AngleMode angleMode;

    public Calculator(){
    }

    public void setAngleMode(AngleMode mode) {
        angleMode = mode;
    }

    public AngleMode getAngleMode(){
        return angleMode;
    }

    public static MathContext getGoodContext(BigDecimal... operands) {
        int precision = GOOD_PRECISION;
        for (BigDecimal op : operands) {
            precision = Math.max(precision, op.precision());
        }
        return new MathContext(precision, ROUNDING_MODE);
    }

    public static boolean doubleIsInfinite(BigDecimal number) {
        return number.doubleValue() == Double.NEGATIVE_INFINITY ||
                number.doubleValue() == Double.POSITIVE_INFINITY;
    }

    public static BigDecimal getHardcodedCosine(BigDecimal angle, AngleMode angleMode)
            throws IllegalArgumentException
    {
        BigDecimal degrees = angle;
        if (angleMode == AngleMode.RADIAN) {
            degrees = toDegrees(angle);
        }

        BigDecimal remainder = degrees.abs().remainder(new BigDecimal(360), getGoodContext(angle));
        if(remainder.compareTo(new BigDecimal(90)) == 0 || remainder.compareTo(new BigDecimal(270)) == 0){
            return BigDecimal.ZERO;
        }else{
            throw new IllegalArgumentException();
        }
    }

    public static BigDecimal getHardcodedTangent(BigDecimal angle, AngleMode angleMode)
            throws IllegalArgumentException
    {
        BigDecimal degrees = angle;
        if (angleMode == AngleMode.RADIAN) {
            degrees = toDegrees(angle);
        }

        BigDecimal remainder = degrees.abs().remainder(new BigDecimal(360), getGoodContext(angle)).setScale(9, RoundingMode.HALF_UP);
        if(remainder.compareTo(new BigDecimal(90)) == 0 || remainder.compareTo(new BigDecimal(270)) == 0){
            return null;
        }else{
            throw new IllegalArgumentException();
        }
    }

    public static BigInteger factorial(BigInteger operand) {
        if (operand.compareTo(BigInteger.ZERO) < 0) {
            throw new ArithmeticException("Negative factorial");
        } else if (operand.compareTo(BigInteger.ZERO) == 0) {
            return BigInteger.ONE;
        } else {
            BigInteger result = BigInteger.ONE;
            for (BigInteger i = operand; i.compareTo(BigInteger.ONE) > 0; i = i.subtract(BigInteger.ONE)) {
                result = result.multiply(i);
            }
            return result;
        }
    }

    public static BigDecimal toDegrees(BigDecimal radians) {
        return radians.multiply(new BigDecimal("57,2957795130823208768"), getGoodContext(radians));
    }

    public static BigDecimal toRadians(BigDecimal degrees) {
        return degrees.multiply(new BigDecimal("0.01745329251994329577"), getGoodContext(degrees));
    }

    /* 0 */

    public void random(OperationBundle bundle){
        bundle.add(new BigDecimal(Math.random()));
    }

    /* 1 */

    public void inversion(OperationBundle bundle){
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) == 0) {
            bundle.error = DIV_BY_ZERO;
        } else {
            bundle.add(BigDecimal.ONE.divide(bundle.operands[0], DEFAULT_MATH_CONTEXT));
        }
    }

    public void square(OperationBundle bundle){
        try {
            bundle.add(bundle.operands[0].pow(2, getGoodContext(bundle.operands[0])));
        }catch(ArithmeticException e) {
            bundle.error = TOO_BIG;
        }
    }

    public void negative(OperationBundle bundle) {
        bundle.add(bundle.operands[0].negate());
    }

    public void squareRoot(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = NEGATIVE_SQRT;
        } else if (doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            bundle.add(new BigDecimal(Math.sqrt(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
        }
    }

    public void sine(OperationBundle bundle) {
        BigDecimal radians = bundle.operands[0];
        if (angleMode == AngleMode.DEGREE) {
            radians = toRadians(bundle.operands[0]);
        }

        if (Calculator.doubleIsInfinite(radians)) {
            bundle.error = TOO_BIG;
        } else {
            bundle.add(new BigDecimal(Math.sin(radians.doubleValue()),
                    Calculator.getGoodContext(bundle.operands[0])));
        }
    }

    public void cosine(OperationBundle bundle) {
        try {
            bundle.add(Calculator.getHardcodedCosine(bundle.operands[0], angleMode));
        }catch(IllegalArgumentException e) {
            BigDecimal radians = bundle.operands[0];
            if (angleMode == AngleMode.DEGREE) {
                radians = Calculator.toRadians(bundle.operands[0]);
            }

            if (Calculator.doubleIsInfinite(radians)) {
                bundle.error = TOO_BIG;
            } else {
                bundle.add(new BigDecimal(Math.cos(radians.doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
            }
        }
    }

    public void tangent(OperationBundle bundle) {
        try {
            BigDecimal result = Calculator.getHardcodedTangent(bundle.operands[0], angleMode);
            if (result != null) {
                bundle.add(result);
            } else {
                bundle.error = TAN_OUT_DOMAIN;
            }
        }catch(IllegalArgumentException e) {
            BigDecimal radians = bundle.operands[0];
            if (angleMode == AngleMode.DEGREE) {
                radians = Calculator.toRadians(bundle.operands[0]);
            }

            if (Calculator.doubleIsInfinite(radians)) {
                bundle.error = TOO_BIG;
            } else {
                bundle.add(new BigDecimal(Math.tan(radians.doubleValue()), Calculator.getGoodContext(bundle.operands[0])).setScale(9, RoundingMode.HALF_UP));
            }
        }
    }

    public void arcsine(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ONE.negate()) < 0
                || bundle.operands[0].compareTo(BigDecimal.ONE) > 0) {
            bundle.error = ARCSIN_OUT_RANGE;
        } else if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            BigDecimal result = new BigDecimal(Math.asin(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0]));
            if (angleMode == AngleMode.DEGREE) {
                result = Calculator.toDegrees(result);
            }
            bundle.add(result);
        }
    }

    public void arccosine(OperationBundle bundle){
        if (bundle.operands[0].compareTo(new BigDecimal("-1.0")) < 0
                || bundle.operands[0].compareTo(new BigDecimal("1.0")) > 0) {
            bundle.error = ARCCOS_OUT_RANGE;
        } else if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            BigDecimal result = new BigDecimal(Math.acos(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0]));
            if (angleMode == AngleMode.DEGREE) {
                result = Calculator.toDegrees(result);
            }
            bundle.add(result);
        }
    }

    public void arctangent(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            BigDecimal result = new BigDecimal(Math.atan(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0]));
            if (angleMode == AngleMode.DEGREE) {
                result = Calculator.toDegrees(result);
            }
            bundle.add(result);
        }
    }

    public void sineH(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            try {
                bundle.add(new BigDecimal(Math.sinh(bundle.operands[0].doubleValue()),
                        Calculator.getGoodContext(bundle.operands[0])));
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    public void cosineH(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            try {
                bundle.add(new BigDecimal(Math.cosh(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    public void tangentH(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            try {
                bundle.add(new BigDecimal(Math.tanh(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    public void log10(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) <= 0) {
            bundle.error = LOG_OUT_RANGE;
        } else if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            bundle.add(new BigDecimal(Math.log10(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
        }
    }

    public void logN(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) <= 0) {
            bundle.error = LOG_OUT_RANGE;
        } else if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            bundle.add(new BigDecimal(Math.log(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
        }
    }

    public void exponential(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0])) {
            bundle.error = TOO_BIG;
        } else {
            try {
                bundle.add(new BigDecimal(Math.exp(bundle.operands[0].doubleValue()), Calculator.getGoodContext(bundle.operands[0])));
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    public void factorial(OperationBundle bundle) {
        try {
            if (bundle.operands[0].compareTo(new BigDecimal(1000)) > 0) {
                bundle.error = TOO_BIG;
            } else {
                BigInteger operand = bundle.operands[0].toBigIntegerExact();
                bundle.add(new BigDecimal(Calculator.factorial(operand)));
            }
        }catch(ArithmeticException e) {
            bundle.error = WRONG_FACTORIAL;
        }
    }

    public void deg2rad(OperationBundle bundle) {
        bundle.add(Calculator.toRadians(bundle.operands[0]));
    }

    public void rad2deg(OperationBundle bundle) {
        bundle.add(Calculator.toDegrees(bundle.operands[0]));
    }

    public void floor(OperationBundle bundle) {
        bundle.add(bundle.operands[0].setScale(0, RoundingMode.FLOOR));
    }

    public void round(OperationBundle bundle) {
        bundle.add(bundle.operands[0].setScale(0, RoundingMode.HALF_UP));
    }

    public void ceil(OperationBundle bundle) {
        bundle.add(bundle.operands[0].setScale(0, RoundingMode.CEILING));
    }

    public void circleSurface(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = NEGATIVE_RADIUS;
        } else {
            try {
                bundle.add(bundle.operands[0].pow(2).multiply(BIG_PI, Calculator.getGoodContext(bundle.operands[0])));
            }catch(ArithmeticException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    /* 2 */

    public void addition(OperationBundle bundle) {
        bundle.add(bundle.operands[0].add(bundle.operands[1]));
    }

    public void multiplication(OperationBundle bundle) {
        bundle.add(bundle.operands[0].multiply(bundle.operands[1]));
    }

    public void exponentiation(OperationBundle bundle) {
        //y^x; x is results.operands[1]; y is results.operands[0]
        if (Calculator.doubleIsInfinite(bundle.operands[0]) ||
                Calculator.doubleIsInfinite(bundle.operands[1]))
        {
            bundle.error = TOO_BIG;
        } else if (bundle.operands[0].compareTo(BigDecimal.ZERO) > 0) {
            try {
                bundle.add(new BigDecimal(Math.pow(bundle.operands[0].doubleValue(), bundle.operands[1].doubleValue()), Calculator.getGoodContext(bundle.operands)));
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        } else {
            try {
                BigInteger exponent = bundle.operands[1].toBigIntegerExact();
                bundle.add(new BigDecimal(Math.pow(bundle.operands[0].doubleValue(), exponent.doubleValue())));
            }catch(ArithmeticException e) {
                bundle.error = NEGATIVE_BASE_EXPONENTIATION;
            }catch(NumberFormatException e) {
                bundle.error = TOO_BIG;
            }
        }
    }

    public void subtraction(OperationBundle bundle) {
        bundle.add(bundle.operands[0].subtract(bundle.operands[1]));
    }

    public void joyDivision(OperationBundle bundle) {
        if (bundle.operands[1].compareTo(BigDecimal.ZERO) == 0) {
            bundle.error = DIV_BY_ZERO;
        } else {
            bundle.add(bundle.operands[0].divide(bundle.operands[1], Calculator.getGoodContext(bundle.operands)));
        }
    }

    public void rootYX(OperationBundle bundle) {
        //x^(1/y); x is results.operands[1]; y is results.operands[0]
        if (Calculator.doubleIsInfinite(bundle.operands[1])) {
            bundle.error = TOO_BIG;
        } else if (bundle.operands[0].compareTo(BigDecimal.ZERO) == 0) {
            bundle.error = ROOT_INDEX_ZERO;
        } else if (bundle.operands[1].compareTo(BigDecimal.ZERO) > 0) {
            bundle.add(new BigDecimal(
                    Math.pow(bundle.operands[1].doubleValue(), BigDecimal.ONE.divide(bundle.operands[0], Calculator.DEFAULT_MATH_CONTEXT).doubleValue()),
                    Calculator.getGoodContext(bundle.operands)));
        } else {
            bundle.error = NEGATIVE_RADICAND;
        }
    }

    public void logYX(OperationBundle bundle) {
        //log(x) in base y; x is results.operands[1]; y is results.operands[0]
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) <= 0
                || bundle.operands[1].compareTo(new BigDecimal("0.0")) <= 0)
        {
            bundle.error = LOG_OUT_RANGE;
        } else if (Calculator.doubleIsInfinite(bundle.operands[0]) || Calculator.doubleIsInfinite(bundle.operands[1])) {
            bundle.error = TOO_BIG;
        } else {
            BigDecimal divisor = new BigDecimal(Math.log(bundle.operands[0].doubleValue()));
            if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                bundle.error = LOG_BASE_ONE;
            } else {
                bundle.add(new BigDecimal(Math.log(bundle.operands[1].doubleValue()))
                            .divide(divisor, Calculator.getGoodContext(bundle.operands)));
            }
        }
    }

    public void hypotenusePythagoras(OperationBundle bundle) {
        if (Calculator.doubleIsInfinite(bundle.operands[0]) || Calculator.doubleIsInfinite(bundle.operands[1])) {
            bundle.error = TOO_BIG;
        } else if (bundle.operands[0].compareTo(BigDecimal.ZERO) < 0 || bundle.operands[1].compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = SIDE_NEGATIVE;
        } else {
            bundle.add(bundle.operands[0]);
            bundle.add(bundle.operands[1]);
            bundle.add(new BigDecimal(
                    Math.hypot(bundle.operands[0].doubleValue(), bundle.operands[1].doubleValue()),
                    Calculator.getGoodContext(bundle.operands)));
            //TODO don't use Math.hypot and avoid .doubleValue()
        }
    }

    public void legPythagoras(OperationBundle bundle) {
        if (bundle.operands[0].compareTo(BigDecimal.ZERO) < 0 || bundle.operands[1].compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = SIDE_NEGATIVE;
        } else {
            BigDecimal hyp = bundle.operands[0].max(bundle.operands[1]);
            BigDecimal leg = bundle.operands[0].min(bundle.operands[1]);
            BigDecimal subtract = hyp.pow(2).subtract(leg.pow(2));
            if (Calculator.doubleIsInfinite(subtract)) {
                bundle.error = TOO_BIG;
            } else {
                bundle.add(bundle.operands[0]);
                bundle.add(bundle.operands[1]);
                bundle.add(new BigDecimal(Math.sqrt(subtract.doubleValue()), Calculator.getGoodContext(bundle.operands)));
            }
        }
    }

    public void triangleSurface(OperationBundle bundle) {
        BigDecimal p = bundle.operands[0]
                        .add(bundle.operands[1])
                        .add(bundle.operands[2])
                        .divide(new BigDecimal(2), Calculator.DEFAULT_MATH_CONTEXT);
        BigDecimal q = p.multiply(p.subtract(bundle.operands[0]))
                        .multiply(p.subtract(bundle.operands[1]))
                        .multiply(p.subtract(bundle.operands[2]));
        if (q.compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = NOT_TRIANGLE;
        } else if (Calculator.doubleIsInfinite(q)) {
            bundle.error = TOO_BIG;
        } else {
            bundle.add(new BigDecimal(Math.sqrt(q.doubleValue()), Calculator.getGoodContext(bundle.operands)));
        }
    }

    public void quadraticEquation(OperationBundle bundle) {
        //0=ax2+bx+c, a==z==op[0]; b==y==op[1]; c==x==op[2]
        BigDecimal a = bundle.operands[0];
        BigDecimal b = bundle.operands[1];
        BigDecimal c = bundle.operands[2];
        BigDecimal radicand = b.pow(2).subtract(a.multiply(c).multiply(new BigDecimal(4)));
        if (Calculator.doubleIsInfinite(radicand)) {
            bundle.error = TOO_BIG;
        } else if (radicand.compareTo(BigDecimal.ZERO) < 0) {
            bundle.error = COMPLEX_NUMBER;
        } else {
            BigDecimal root = new BigDecimal(Math.sqrt(radicand.doubleValue()));
            bundle.add(root.subtract(b).divide(a.multiply(new BigDecimal(2)), Calculator.DEFAULT_MATH_CONTEXT));
            bundle.add(root.negate().subtract(b).divide(a.multiply(new BigDecimal(2)), Calculator.DEFAULT_MATH_CONTEXT));
        }
    }

    /* ALL */

    public void summation(OperationBundle bundle) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal op : bundle.operands) {
            result = result.add(op);
        }
        bundle.add(result);
    }

    public void mean(OperationBundle bundle) {
        BigDecimal result = BigDecimal.ZERO;
        int precision = 1;
        for (BigDecimal op  :bundle.operands) {
            precision = Math.max(precision, op.precision());
            result = result.add(op);
        }
        precision = Math.max(precision, Calculator.GOOD_PRECISION);
        MathContext mathContext = new MathContext(precision, Calculator.ROUNDING_MODE);
        bundle.add(result.divide(new BigDecimal(bundle.operands.length), mathContext));
    }

    public void pi(OperationBundle bundle) {
        BigDecimal result = BIG_PI.round(Calculator.DEFAULT_MATH_CONTEXT);
        if (bundle.operands.length == 1) {
            result = result.multiply(bundle.operands[0], Calculator.getGoodContext(bundle.operands[0]));
        }
        bundle.add(result);
    }

    public void phi(OperationBundle bundle) {
        BigDecimal result = BIG_PHI.round(Calculator.DEFAULT_MATH_CONTEXT);
        if (bundle.operands.length == 1) {
            result = result.multiply(bundle.operands[0], Calculator.getGoodContext(bundle.operands[0]));
        }
        bundle.add(result);
    }

    public void euler(OperationBundle bundle) {
        BigDecimal result = BIG_EULER.round(Calculator.DEFAULT_MATH_CONTEXT);
        if (bundle.operands.length == 1) {
            result = result.multiply(bundle.operands[0], Calculator.getGoodContext(bundle.operands[0]));
        }
        bundle.add(result);
    }

    /* N */

    public void summationN(OperationBundle bundle) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 1; i < bundle.operands.length; i++) {
            result = result.add(bundle.operands[i]);
        }
        bundle.add(result);
    }

    public void meanN(OperationBundle bundle) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 1; i < bundle.operands.length; i++) {
            result = result.add(bundle.operands[i]);
        }
        result = result.divide(bundle.operands[0], Calculator.getGoodContext(bundle.operands));
        bundle.add(result);
    }
}
