package net.project104.civyshkrpncalc;

enum CalculatorError {
    NOT_IMPLEMENTED,
    DIV_BY_ZERO, TOO_BIG, NEGATIVE_SQRT,
    TAN_OUT_DOMAIN, ARCSIN_OUT_RANGE, ARCCOS_OUT_RANGE,
    LOG_OUT_RANGE, WRONG_FACTORIAL,
    NEGATIVE_RADIUS, NEGATIVE_BASE_EXPONENTIATION,
    ROOT_INDEX_ZERO, NEGATIVE_RADICAND, LOG_BASE_ONE,
    SIDE_NEGATIVE, NOT_TRIANGLE, COMPLEX_NUMBER,
    TOO_MANY_ARGS, NOT_ENOUGH_ARGS, LAST_NOT_INT,
    ;

    public static int getRStringId(CalculatorError error){
        switch (error){
            case NOT_IMPLEMENTED: return R.string.operationNotImplemented;
            case DIV_BY_ZERO: return R.string.divisionByZero;
            case TOO_BIG: return R.string.numberTooBig;
            case NEGATIVE_SQRT: return R.string.negativeSquareRoot;
            case TAN_OUT_DOMAIN: return R.string.tangentOutOfDomain;
            case ARCSIN_OUT_RANGE: return R.string.arcsineOutOfRange;
            case ARCCOS_OUT_RANGE: return R.string.arccosineOutOfRange;
            case LOG_OUT_RANGE: return R.string.logOutOfRange;
            case WRONG_FACTORIAL: return R.string.wrongFactorial;
            case NEGATIVE_RADIUS: return R.string.negativeRadius;
            case NEGATIVE_BASE_EXPONENTIATION: return R.string.negativeBaseExponentiation;
            case ROOT_INDEX_ZERO: return R.string.rootIndexZero;
            case NEGATIVE_RADICAND: return R.string.negativeRadicand;
            case LOG_BASE_ONE: return R.string.logBaseIsOne;
            case SIDE_NEGATIVE: return R.string.sideCantBeNegative;
            case NOT_TRIANGLE: return R.string.notATriangle;
            case COMPLEX_NUMBER: return R.string.complexNumber;
            case TOO_MANY_ARGS: return R.string.tooManyArguments;
            case NOT_ENOUGH_ARGS: return R.string.notEnoughArguments;
            case LAST_NOT_INT: return R.string.nIsNotInteger;
            default: return R.string.operationNotImplemented;
        }
    }
}
