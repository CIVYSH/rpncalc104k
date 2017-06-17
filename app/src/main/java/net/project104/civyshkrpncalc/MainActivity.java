/*	RPNCalc104k - Calculator for Android using RPN notation
 * 	Copyright 2014 Yeshe Santos García <civysh@outlook.com>
 *	
 *	This file is part of RPNCalc104k
 *	
 *	RPNCalc104k is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.project104.civyshkrpncalc;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import static android.view.View.FOCUS_RIGHT;
import static android.view.View.GONE;

public class MainActivity extends Activity {
    final static String TAG = MainActivity.class.getSimpleName();

    final static BigDecimal BIG_PI = new BigDecimal(Math.PI);
    final static BigDecimal BIG_EULER = new BigDecimal(Math.E);
    final static BigDecimal BIG_PHI = new BigDecimal("1.618033988749894");
    final static int GOOD_PRECISION = 10;
    final static int ARITY_ALL = -1;// set it always < 0
    final static int ARITY_ZERO_ONE = -2;// and different from other ARITY
    final static RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    final static MathContext DEFAULT_DIVIDE_CONTEXT = new MathContext(GOOD_PRECISION, ROUNDING_MODE);

    final static String NUMBER_STACK_PARAM = "NumberStack";
    final static String DEGREE_MODE_PARAM = "AngleMode";
    final static String HISTORY_SAVER_PARAM = "HistorySaver";
    final static String SWITCHER_INDEX_PARAM = "SwitcherIndex";
    static final String EDITABLE_NUMBER_PARAM = "EditableNumber";

    private class ListViewScroller implements Runnable {
        ListView view;

        ListViewScroller(ListView view) {
            this.view = view;
        }

        public void run() {
            view.smoothScrollToPosition(numberStack.size() - 1);
        }
    }

    static private class HorizontalViewScroller implements Runnable {
        HorizontalScrollView view;

        HorizontalViewScroller(HorizontalScrollView view) {
            this.view = view;
        }

        @Override
        public void run() {
            view.fullScroll(FOCUS_RIGHT);
        }
    }

    private DataSetObserver numberStackObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            layoutNumbers.post(new ListViewScroller(layoutNumbers));
        }
    };

    LinkedList<BigDecimal> numberStack = new LinkedList<>();//TODO think of Deque

    private class ViewedString {
        private static final int NUMBER_BUILDER_INITIAL_CAPACITY = 10;
        private StringBuilder text = new StringBuilder(NUMBER_BUILDER_INITIAL_CAPACITY);

        void updateView() {
            tvEditableNumber.setText(text);
            tvEditableNumber.post(new HorizontalViewScroller(scrollEditableNumber));
            scrollEditableNumber.setVisibility(View.VISIBLE);
            scrollError.setVisibility(GONE);
        }

        void reset() {
            text = new StringBuilder(NUMBER_BUILDER_INITIAL_CAPACITY);
            updateView();
        }

        void append(String more) {
            text.append(more);
            updateView();
        }

        void set(String newText) {
            text = new StringBuilder(newText);
            updateView();
        }

        int length() {
            return text.length();
        }

        int indexOf(String s) {
            return text.indexOf(s);
        }

        int lastIndexOf(String s) {
            return text.lastIndexOf(s);
        }

        void deleteCharAt(int i) {
            text.deleteCharAt(i);
            updateView();
        }

        char charAt(int i) {
            return text.charAt(i);
        }

        void setCharAt(int i, char c) {
            text.setCharAt(i, c);
            updateView();
        }

        void insert(int i, String s) {
            text.insert(i, s);
            updateView();
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }

    ViewedString editableNumber = new ViewedString();
    boolean deleteCharBeforeDecimalSeparator = false;
    boolean deleteCharBeforeScientificNotation = false;


    AngleMode angleMode;
    int[] digitButtonsIds;
    HistorySaver historySaver;

    ListView layoutNumbers;
    HorizontalScrollView scrollEditableNumber, scrollError;
    TextView tvEditableNumber, tvError, tvAngleMode;
    ViewAnimator switcherFunctions;

    NumberStackAdapter numberStackAdapter;

    private class OperationData {
        int arity;
        int btnId;

        OperationData(int arity, int btnId) {
            this.arity = arity;
            this.btnId = btnId;
        }
    }

    private HashMap<Operation, OperationData> operationsData;

    enum UpdateStackFlag {
        KEEP_PREVIOUS, REMOVE_PREVIOUS
    }

    enum Operation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,

        SQUARE,
        SQUAREROOT,
        EXPONENTIATION,
        ROOTYX,
        NEGATIVE,
        INVERSION,

        LOG10,
        LOGYX,
        LOGN,
        EXPONENTIAL,
        EXP10,

        SINE,
        COSINE,
        TANGENT,
        ARCSINE,
        ARCCOSINE,
        ARCTANGENT,
        DEGTORAD,
        RADTODEG,

        SUMMATION,
        MEAN,
        CONSTANTPI,
        CONSTANTEULER,
        CONSTANTPHI,

        CIRCLE_SURFACE,
        TRIANGLE_SURFACE,
        HYPOTENUSE_PYTHAGORAS,
        LEG_PYTHAGORAS
    }

    enum AngleMode {
        DEGREE, RADIAN
    }

    static class Change implements Step {
        Deque<BigDecimal> stackNew = new LinkedList<>();//ArrayDeque from API 9
        Deque<BigDecimal> stackOld = new LinkedList<>();
        int stackNewSize = 0;
        String newText, oldText;
        HistorySaver saver;

        public Change(HistorySaver saver, String oldText) {
            this.oldText = oldText;
            this.saver = saver;
        }

        void addOld(BigDecimal number) {
            stackOld.add(number);
        }

        @Override
        public void undo() {
            MainActivity activity = saver.getActivity();

            BigDecimal[] poppedNew = activity.popNumbers(stackNewSize);
            stackNew.addAll(Arrays.asList(poppedNew));
            activity.addNumbers(stackOld);

            newText = activity.editableNumber.toString();
            activity.editableNumber.set(oldText);
        }

        @Override
        public void redo() {
            MainActivity activity = saver.getActivity();

            activity.popNumbers(stackOld.size());
            activity.addNumbers(stackNew);

            activity.editableNumber.set(newText);
        }
    }

    static class HistorySaver implements Serializable {
        transient WeakReference<MainActivity> activity;
        private List<Step> listSteps = new LinkedList<Step>();
        int currentStep;

        HistorySaver(MainActivity activity) {
            this.activity = new WeakReference<MainActivity>(activity);
            currentStep = 0;
        }

        private void addStep(Step step) {
            int size = listSteps.size();
            if (currentStep == size) {
                listSteps.add(step);
                ++currentStep;
            } else {
                listSteps.set(currentStep, step);
                ++currentStep;
                for (int i = currentStep; i < size; ++i) {
                    listSteps.remove(currentStep);
                }
            }
        }

        private Step getPreviousStep() {
            if (listSteps.isEmpty()) {
                return null;
            } else if (currentStep < 1) {
                return null;
            } else {
                return listSteps.get(currentStep - 1);
            }
        }

        void saveChange(Change change) {
            addStep(change);
        }

        void saveOperation(BigDecimal[] operands, String editable, BigDecimal result) {
            addStep(new StepOperation(this, operands, editable, result));
        }

        void saveEnter(String editable) {
            addStep(new StepEnter(this, editable));
        }

        void saveDigits() {
            if (getPreviousStep() instanceof StepDigits) {
                // nothing;
            } else {
                addStep(new StepDigits(this));
            }
        }

        void saveDelete(String editable) {
            addStep(new StepDelete(this, editable));
        }

        void saveClear(List<BigDecimal> savedStack, String editable) {
            addStep(new StepClear(this, savedStack, editable));
        }

        void savePop(String editable) {
            addStep(new StepPop(this, editable));
        }

        void savePop(BigDecimal number) {
            addStep(new StepPop(this, number));
        }

//        void saveSwitch() {
//            Step step = new StepSwitch(activity.get());
//            addStep(step);
//        }

        synchronized void goBack() {
            boolean goBackAgain = true;
            while (currentStep > 0 && goBackAgain) {
                --currentStep;
                goBackAgain = false;
                Step step = listSteps.get(currentStep);
                step.undo();
                // which follows is used to eventually keep undoing deletes
                // do that only if both this and previous steps are:
                // 1. instanceof StepDelete
                // 2. type != text

                //TODO split Delete into DeleteEditableNumber and DeleteErrorMessage (choose other names if you want). Then, refactor this commented block
//                if (step instanceof StepDelete) {
//                    if (((StepDelete) step).type != DeleteType.TEXT && currentStep > 0) {
//                        step = listSteps.get(currentStep - 1);
//                        if (step instanceof StepDelete) {
//                            if (((StepDelete) step).type != DeleteType.TEXT) {
//                                goBackAgain = true;
//                            }
//                        }
//                    }
//                }
            }
        }

        synchronized void goForward() {
            if (currentStep < listSteps.size()) {
                listSteps.get(currentStep).redo();
                ++currentStep;
            }
        }

        MainActivity getActivity() {
            return activity.get();
        }

        void setActivity(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }
    }

    interface Step extends Serializable {
        void undo();

        void redo();
    }

    static class StepOperation implements Step {
        public transient HistorySaver saver;
        BigDecimal[] operands;
        BigDecimal result;
        String savedEditable;

        StepOperation(HistorySaver saver, BigDecimal[] operands, String editable, BigDecimal result) {
            this.saver = saver;
            this.operands = operands;
            this.savedEditable = editable;
            this.result = result;
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            act.numberStack.pop();
            act.numberStack.addAll(Arrays.asList(operands));
            if (savedEditable != null) {
                act.editableNumber.set(savedEditable);
            }
        }

        @Override
        public void redo() {
            MainActivity act = saver.getActivity();
            for (int i = 0; i < operands.length; i++) {
                BigDecimal operand = operands[i];
                act.numberStack.pop();
            }
            act.numberStack.add(result);
            act.editableNumber.reset();
        }
    }

    static class StepEnter implements Step {
        public transient HistorySaver saver;
        String savedEditable;
        boolean fromEditable;

        StepEnter(HistorySaver saver, String editable) {
            this.saver = saver;
            this.savedEditable = editable;
            this.fromEditable = editable != null;
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            if (fromEditable) {
                act.editableNumber.set(savedEditable);
            } else {
                act.popLastNumber();
            }
        }

        @Override
        public void redo() {
            saver.getActivity().clickedEnter(false);
        }
    }

    static class StepDigits implements Step {
        public transient HistorySaver saver;
        String redoEditableNumber;

        StepDigits(HistorySaver saver) {
            this.saver = saver;
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            if (act.editableNumber.length() == 0) {
                Log.e("StepDigits", "undo: editableNumber is empty, that's weird");
            } else {
                redoEditableNumber = act.editableNumber.toString();
                if (!act.editableNumber.toString().equals("-")) {
                    act.popLastNumber();
                } else {
                    //"-" is not in the stack, don't remove last number form it
                }
                act.editableNumber.reset();
            }
        }

        @Override
        public void redo() {
            MainActivity act = saver.getActivity();
            act.editableNumber.set(redoEditableNumber);
//            UpdateStackFlag removeFlag = act.isEditableNumberInStack() ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
            act.updateNumberStackFromEditable(UpdateStackFlag.KEEP_PREVIOUS);
        }
    }

    static class StepDelete implements Step {
        public transient HistorySaver saver;
        String undoEditable, redoEditable;

        StepDelete(HistorySaver saver, String editable) {
            this.saver = saver;
            this.undoEditable = editable;
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            UpdateStackFlag removeFlag = act.isEditableNumberInStack() ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
            redoEditable = act.editableNumber.toString();
            act.editableNumber.set(undoEditable);
            act.updateNumberStackFromEditable(removeFlag);
        }

        @Override
        public void redo() {
            MainActivity act = saver.getActivity();
            UpdateStackFlag removeFlag = act.isEditableNumberInStack() ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
            act.editableNumber.set(redoEditable);
            act.updateNumberStackFromEditable(removeFlag);
        }
    }

    static class StepClear implements Step {
        transient HistorySaver saver;
        LinkedList<BigDecimal> savedStack;
        String savedEditable;

        StepClear(HistorySaver saver, List<BigDecimal> savedStack, String editable) {
            this.saver = saver;
            this.savedStack = new LinkedList<BigDecimal>(savedStack);
            this.savedEditable = editable;
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            act.clearStack();
            act.addNumbers(savedStack);
            act.editableNumber.set(savedEditable);
        }

        @Override
        public void redo() {
            saver.getActivity().clickedClear(false);
        }
    }

    static class StepPop implements Step {
        transient HistorySaver saver;
        boolean fromEditable;// where it was popped
        String savedNumber;

        StepPop(HistorySaver saver, String editable) {
            this.saver = saver;
            this.fromEditable = true;
            this.savedNumber = editable;
        }

        StepPop(HistorySaver saver, BigDecimal number) {
            this.saver = saver;
            this.fromEditable = false;
            this.savedNumber = number.toString();
        }

        @Override
        public void undo() {
            MainActivity act = saver.getActivity();
            if (fromEditable) {
                act.editableNumber.set(savedNumber);
                act.updateNumberStackFromEditable(UpdateStackFlag.KEEP_PREVIOUS);
            } else {
                act.addNumber(new BigDecimal(savedNumber));
            }
        }

        @Override
        public void redo() {
            saver.getActivity().clickedPopNumber(false);
        }
    }

//    static class StepSwitch implements Step {
//        transient WeakReference<MainActivity> activity;
//
//        StepSwitch(MainActivity activity) {
//            this.activity = new WeakReference<MainActivity>(activity);
//        }
//
//        @Override
//        public void undo() {
//            activity.get().clickedSwitch(false);
//        }
//
//        @Override
//        public void redo() {
//            activity.get().clickedSwitch(false);
//        }
//    }

//    class ViewsWidthCopier implements ViewTreeObserver.OnPreDrawListener {
//        View from, to;
//
//        ViewsWidthCopier(View from, View to) {
//            this.from = from;
//            this.to = to;
//        }
//
//        @Override
//        public boolean onPreDraw() {
//            ViewGroup.LayoutParams params = to.getLayoutParams();
//            params.width = from.getWidth();
//            to.setLayoutParams(params);
//            from.getViewTreeObserver().removeOnPreDrawListener(this);
//            return true;
//        }
//    }

    OnClickListener clickListenerDigitButtons = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            for (int i = 0; i < 10; ++i) {
                if (id == digitButtonsIds[i]) {
                    clickedDigit(i, true);
                    break;
                }
            }
        }
    };

    OnClickListener clickListenerOperations = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Operation operation = getOperationFromBtnId(view.getId());
            if (operation != null) {
                clickedOperation(operation);
            } else {
                Log.d(TAG, "Some button is not lonked to any Operation");
            }
        }
    };

    private Operation getOperationFromBtnId(int id) {
        for (Map.Entry<Operation, OperationData> dataEntry : operationsData.entrySet()) {
            if (dataEntry.getValue().btnId == id) {
                return dataEntry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

//        mUsingViewAnimator = getResources().getBoolean(R.bool.layout_uses_view_animator);
//        if (mUsingViewAnimator) {
//            switcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);
//        } else {
//            switcherFunctions = null;
//        }
        switcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);

        scrollEditableNumber = (HorizontalScrollView) findViewById(R.id.scrollEditableNumber);
        scrollError = (HorizontalScrollView) findViewById(R.id.scrollError);
        layoutNumbers = (ListView) findViewById(R.id.listNumbers);
        tvAngleMode = (TextView) findViewById(R.id.tvAngleMode);
        tvEditableNumber = (TextView) findViewById(R.id.tvEditableNumber);
        tvError = (TextView) findViewById(R.id.tvError);

        tvAngleMode.setText("D\nE\nG");
        tvEditableNumber.setGravity(Gravity.RIGHT | Gravity.CENTER);

        angleMode = AngleMode.DEGREE;
        showAngleMode();

        operationsData = new HashMap<>(Operation.values().length);
        operationsData.put(Operation.ADDITION, new OperationData(2, R.id.butAddition));
        operationsData.put(Operation.SUBTRACTION, new OperationData(2, R.id.butSubtraction));
        operationsData.put(Operation.MULTIPLICATION, new OperationData(2, R.id.butMultiplication));
        operationsData.put(Operation.DIVISION, new OperationData(2, R.id.butDivision));

        operationsData.put(Operation.SQUARE, new OperationData(1, R.id.butSquare));
        operationsData.put(Operation.SQUAREROOT, new OperationData(1, R.id.butSquareRoot));
        operationsData.put(Operation.EXPONENTIATION, new OperationData(2, R.id.butExponentiation));
        operationsData.put(Operation.ROOTYX, new OperationData(2, R.id.butRootYX));
        operationsData.put(Operation.NEGATIVE, new OperationData(1, R.id.butNegative));
        operationsData.put(Operation.INVERSION, new OperationData(1, R.id.butInversion));

        operationsData.put(Operation.LOG10, new OperationData(1, R.id.butLog10));
        operationsData.put(Operation.LOGYX, new OperationData(2, R.id.butLogYX));
        operationsData.put(Operation.LOGN, new OperationData(1, R.id.butLn));
        operationsData.put(Operation.EXPONENTIAL, new OperationData(1, R.id.butExponential));
        operationsData.put(Operation.EXP10, new OperationData(1, R.id.but10x));

        operationsData.put(Operation.SINE, new OperationData(1, R.id.butSine));
        operationsData.put(Operation.COSINE, new OperationData(1, R.id.butCosine));
        operationsData.put(Operation.TANGENT, new OperationData(1, R.id.butTangent));
        operationsData.put(Operation.ARCSINE, new OperationData(1, R.id.butArcSine));
        operationsData.put(Operation.ARCCOSINE, new OperationData(1, R.id.butArcCosine));
        operationsData.put(Operation.ARCTANGENT, new OperationData(1, R.id.butArcTangent));
        operationsData.put(Operation.DEGTORAD, new OperationData(1, R.id.butDegreeToRadian));
        operationsData.put(Operation.RADTODEG, new OperationData(1, R.id.butRadianToDegree));

        operationsData.put(Operation.SUMMATION, new OperationData(ARITY_ALL, R.id.butSummation));
        operationsData.put(Operation.MEAN, new OperationData(ARITY_ALL, R.id.butMean));
        operationsData.put(Operation.CONSTANTPI, new OperationData(ARITY_ZERO_ONE, R.id.butPi));
        operationsData.put(Operation.CONSTANTEULER, new OperationData(ARITY_ZERO_ONE, R.id.butEuler));
        operationsData.put(Operation.CONSTANTPHI, new OperationData(ARITY_ZERO_ONE, R.id.butPhi));

        operationsData.put(Operation.CIRCLE_SURFACE, new OperationData(1, R.id.butCircleSurface));
        operationsData.put(Operation.TRIANGLE_SURFACE, new OperationData(3, R.id.butTriangleSurface));
        operationsData.put(Operation.HYPOTENUSE_PYTHAGORAS, new OperationData(2, R.id.butHypotenusePythagoras));
        operationsData.put(Operation.LEG_PYTHAGORAS, new OperationData(2, R.id.butLegPythagoras));

        if (operationsData.size() != Operation.values().length) {
            throw new RuntimeException("There are operations not implemented");
        }

        for (Operation operation : Operation.values()) {
            findViewById(operationsData.get(operation).btnId).setOnClickListener(clickListenerOperations);
        }

        ((Button) findViewById(R.id.butDecimal)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedDecimal(true);
            }
        });

        ((ImageButton) findViewById(R.id.butUndo)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedUndo();
            }
        });
        ((ImageButton) findViewById(R.id.butRedo)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedRedo();
            }
        });

        ((ImageButton) findViewById(R.id.butPopNumber)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedPopNumber(true);
            }
        });
        ((ImageButton) findViewById(R.id.butClear)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedClear(true);
            }
        });
        ((ImageButton) findViewById(R.id.butDelete)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedDelete(true);
            }
        });
        ((ImageButton) findViewById(R.id.butEnterNumber)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedEnter(true);
            }
        });
        ((ImageButton) findViewById(R.id.scientificNotation)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedScientificNotation(true);
            }
        });
        ((Button) findViewById(R.id.minus)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedMinus(true);
            }
        });
//        ((ImageButton) findViewById(R.id.butSwitch)).setOnClickListener(new OnClickListener() {
//            public void onClick(View view) {
//                clickedSwitch(true);
//            }
//        });
        ((ImageButton) findViewById(R.id.butFunctions)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                //from stackoverflow.com
                MainActivity.this.openOptionsMenu(); // activity's onCreateOptionsMenu gets called
//                mOptionsMenu.performIdentifierAction(R.id.menuApplyFunction, 0);
                //TODO switch viewanimator
            }
        });

        numberStack = new LinkedList<BigDecimal>();
        historySaver = new HistorySaver(this);
//        mListDigits = new ArrayList<Integer>();

        Resources res = getResources();
        String buttonName;
        int buttonId;
        digitButtonsIds = new int[10];
        for (int i = 0; i < 10; i++) {
            buttonName = "but" + Integer.toString(i);
            buttonId = res.getIdentifier(buttonName, "id", getPackageName());
            digitButtonsIds[i] = buttonId;
            ((Button) findViewById(buttonId)).setOnClickListener(clickListenerDigitButtons);
        }

        ((ImageButton) findViewById(R.id.butFunctions)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                switcherFunctions.showNext();
                showAngleMode();
            }
        });

        numberStackAdapter = new NumberStackAdapter(this);
        numberStackAdapter.registerDataSetObserver(numberStackObserver);
        layoutNumbers.setAdapter(numberStackAdapter);

//        if (mUsingViewAnimator) {
//            Button b1 = (Button) findViewById(R.id.but1);
//            Button b2 = (Button) findViewById(R.id.but2);
//            ImageButton bUndo = (ImageButton) findViewById(R.id.butUndo);
//            ImageButton bRedo = (ImageButton) findViewById(R.id.butRedo);
//            ViewsWidthCopier copierUndo = new ViewsWidthCopier(b1, bUndo);
//            ViewsWidthCopier copierRedo = new ViewsWidthCopier(b2, bRedo);
//            b1.getViewTreeObserver().addOnPreDrawListener(copierUndo);
//            b2.getViewTreeObserver().addOnPreDrawListener(copierRedo);
//        }

//		 remove? this hack which always shows overflow menu in actionbar.
//		  Used only on emulator with invisible hardware keys
//		try{
//			ViewConfiguration config = ViewConfiguration.get(this);
//			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
//			if(menuKeyField != null){
//				menuKeyField.setAccessible(true);
//				menuKeyField.setBoolean(config, false);
//			}
//		}catch(Exception e){
//			e.printStackTrace();
//		}

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
//        switch (item.getItemId()) {
//            case R.id.menuUndo:
//                clickedUndo();
//                return true;
//            case R.id.menuRedo:
//                clickedRedo();
//                return true;
//            case R.id.menuClear:
//                clickedClear(true);
//                return true;
//            case R.id.menuSurfaceTriangle:
//                clickedOperation(3, Operation.TRIANGLE_SURFACE);
//                return true;
//            case R.id.menuHypotenusePythagoras:
//                clickedOperation(2, Operation.HYPOTENUSE_PYTHAGORAS);
//                return true;
//            case R.id.menuLegPythagoras:
//                clickedOperation(2, Operation.LEG_PYTHAGORAS);
//                return true;
//            case R.id.menuSurfaceCircle:
//                clickedOperation(1, Operation.CIRCLE_SURFACE);
//                return true;
//            case R.id.menuAngleMode:
//                if (angleMode.equals("deg")) {
//                    angleMode = "rad";
//                    tvAngleMode.setText("R\nA\nD");
//                } else {
//                    angleMode = "deg";
//                    tvAngleMode.setText("D\nE\nG");
//                }
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle saver) {
//        saver.putIntegerArrayList("ListDigits", (ArrayList<Integer>) mListDigits);
//        saver.putInt("PosDecimal", mPosDecimal);
        ArrayList<String> numbers = buildStringArrayListNumbers(numberStack);
        saver.putStringArrayList(NUMBER_STACK_PARAM, numbers);
        saver.putString(EDITABLE_NUMBER_PARAM, editableNumber.toString());
        saver.putBoolean(DEGREE_MODE_PARAM, angleMode == AngleMode.DEGREE);
        saver.putSerializable(HISTORY_SAVER_PARAM, historySaver);
        saver.putInt(SWITCHER_INDEX_PARAM, switcherFunctions.getDisplayedChild());
        super.onSaveInstanceState(saver);
    }

    public void onRestoreInstanceState(Bundle saved) {
        super.onRestoreInstanceState(saved);

        ArrayList<String> stringNumbers = saved.getStringArrayList(NUMBER_STACK_PARAM);
        List<BigDecimal> numbers = new ArrayList<>(stringNumbers.size());
        for (String str : stringNumbers) {
            numbers.add(new BigDecimal(str));
        }
        addNumbers(numbers);
        editableNumber.set(saved.getString(EDITABLE_NUMBER_PARAM));
        angleMode = saved.getBoolean(DEGREE_MODE_PARAM) ? AngleMode.DEGREE : AngleMode.RADIAN;
        showAngleMode();

        historySaver = (HistorySaver) saved.getSerializable(HISTORY_SAVER_PARAM);
        historySaver.setActivity(this);
        int indexFunctions = saved.getInt(SWITCHER_INDEX_PARAM);
        for (int i = 0; i < indexFunctions; ++i) {
            switcherFunctions.showNext();
        }
    }

    private Collection<BigDecimal> clearStack() {
        Collection<BigDecimal> cleared = new ArrayList<>(numberStack);
        numberStack.clear();
        numberStackAdapter.notifyDataSetChanged();
        return cleared;
    }

    private void addNumber(BigDecimal number) {
        numberStack.add(number);
        numberStackAdapter.notifyDataSetChanged();
    }

    private void addNumbers(BigDecimal[] numbers) {
        addNumbers(Arrays.asList(numbers));
    }

    private void addNumbers(Collection<BigDecimal> numbers) {
        numberStack.addAll(numbers);
        numberStackAdapter.notifyDataSetChanged();
    }

    private BigDecimal popLastNumber() {
        try {
            BigDecimal number = numberStack.removeLast();
            numberStackAdapter.notifyDataSetChanged();
            return number;
        }catch(NoSuchElementException e) {
            return null;
        }
    }

    private BigDecimal getLastNumber() {
        try {
            return numberStack.getLast();
        }catch(NoSuchElementException e) {
            return null;
        }
    }

    private BigDecimal[] popNumbers(int amount) {
        BigDecimal[] numbers = new BigDecimal[amount];
        for (int i = amount - 1; i >= 0; i--) {
            numbers[i] = numberStack.removeLast();
        }
        numberStackAdapter.notifyDataSetChanged();
        return numbers;
    }

    /**
     * build a BigDecimal from editableNumber and insert it if possible
     *
     * @param flag indicates whether it must remove the last number in the stack before
     *             inserting the new one
     * @return true if a new number has been built and inserted in the stack
     */
    boolean updateNumberStackFromEditable(UpdateStackFlag flag) {
        if (flag == UpdateStackFlag.REMOVE_PREVIOUS) {
            numberStack.removeLast();
        }

        if (editableNumber.length() > 0) {
            String candidateString = editableNumber.toString();
            while (lastCharIs(candidateString, 'E') ||
                    lastCharIs(candidateString, '.') ||
                    lastCharIs(candidateString, '-')) {
                candidateString = candidateString.substring(0, candidateString.length() - 1);
            }

            if (candidateString.length() > 0) {
                BigDecimal newNumber = new BigDecimal(candidateString);
                numberStack.add(newNumber);
                numberStackAdapter.notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    void clickedUndo() {
        historySaver.goBack();
    }

    void clickedRedo() {
        historySaver.goForward();
    }

    boolean isEditableNumberInStack() {
        return editableNumber.length() > 0 && !editableNumber.toString().equals("-");
        //Any other string (0. 0.2 1E 2.3E) must be in stack, but not "-"
    }

    static private boolean lastCharIs(String s, char c) {
        if (s.length() == 0) {
            return false;
        } else {
            return s.charAt(s.length() - 1) == c;
        }
    }

    void clickedDigit(int digit, boolean save) {
        Change change = new Change(historySaver, editableNumber.toString());

        boolean removePrevious = isEditableNumberInStack();
        editableNumber.append(String.valueOf(digit));
        if (save && removePrevious) {
            change.addOld(getLastNumber());
        }

        boolean addedNumber = updateNumberStackFromEditable(removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS);
        if (save) {
//            historySaver.saveDigits();
            if (addedNumber) {
                change.stackNewSize = 1;
            }
            historySaver.saveChange(change);
        }
    }

    void clickedDecimal(boolean save) {
        Change change = new Change(historySaver, editableNumber.toString());
//        if (save) {
//            historySaver.saveDigits();
//        }

        boolean removePrevious = isEditableNumberInStack();
        UpdateStackFlag flagRemove = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
        boolean updated = true;
        if (editableNumber.length() == 0) {
            editableNumber.append("0.");
            deleteCharBeforeDecimalSeparator = true;
        } else if (editableNumber.toString().equals("-")) {
            editableNumber.append("0.");
            deleteCharBeforeDecimalSeparator = true;
        } else {
            if (editableNumber.indexOf("E") == -1) {
                int decimalPosition = editableNumber.indexOf(".");
                if (decimalPosition == -1) {
                    deleteCharBeforeDecimalSeparator = false;
                } else {
                    editableNumber.deleteCharAt(decimalPosition);
                    deleteCharBeforeDecimalSeparator = false;
                }
                editableNumber.append(".");
            } else {
                updated = false;
                //ignore decimals in exponent
            }
        }

//            if (decimalPosition >= 0) {
//                if (editableNumber.indexOf("E") == -1) {
//                    editableNumber.deleteCharAt(decimalPosition);
//                    editableNumber.append(".");
//                    deleteCharBeforeDecimalSeparator = true;
//                }else{
//                    updated = false;
//                    //ignore decimals in exponent
//                }
//            } else if (editableNumber.indexOf("E") == -1) {
//                editableNumber.append(".");
//                deleteCharBeforeDecimalSeparator = false;
//            } else {
//                updated = false;
//                //ignore decimals in exponent
//            }
//        }


        if (updated) {
            if (save && removePrevious) {
                change.addOld(getLastNumber());
            }
            boolean addedNumber = updateNumberStackFromEditable(flagRemove);
            if (save){
                if(addedNumber){
                    change.stackNewSize = 1;
                }
                historySaver.saveChange(change);
            }
        }
    }

    void clickedScientificNotation(boolean save) {
        if (save) {
            Change change = new Change(historySaver, editableNumber.toString());
            historySaver.saveChange(change);
        }
//        if (save) {
//            historySaver.saveDigits();
//        }

        deleteCharBeforeScientificNotation = true;
        if (editableNumber.length() == 0) {
            //"" -> "1E"
            editableNumber.append("1E");
        } else if (editableNumber.toString().equals("-")) {
            //"-" -> "-1E"
            editableNumber.append("1E");
        } else if (Character.valueOf(editableNumber.charAt(editableNumber.length() - 1)).equals('.')) {
            //"3." -> "3.0E"
            editableNumber.append("0E");
        } else if (editableNumber.indexOf("E") == -1) {
            //"1.23" -> "1.23E"
            editableNumber.append("E");
            deleteCharBeforeScientificNotation = false;
        } else {
            //disallow duplicated E
        }
    }

    void clickedMinus(boolean save) {
        Change change = new Change(historySaver, editableNumber.toString());
//        if (save) {
//            historySaver.saveDigits();
//        }

        boolean removePrevious = isEditableNumberInStack();
        UpdateStackFlag flagRemove = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
        if (editableNumber.length() == 0) {
            //"" --> "-"
            editableNumber.append("-");
        } else if (editableNumber.toString().equals("-")) {
            //"-" --> ""
            editableNumber.deleteCharAt(0);
        } else {
            int scientificPosition = editableNumber.indexOf("E");
            if (scientificPosition >= 0) {
                int minusPosition = editableNumber.lastIndexOf("-");
                if (minusPosition == scientificPosition + 1) {
                    //"1E-5" -> "1E5"
                    editableNumber.deleteCharAt(minusPosition);
                } else {
                    //"1E5" -> //"1E-5"
                    editableNumber.insert(scientificPosition + 1, "-");
                }
            } else {
                int minusPosition = editableNumber.indexOf("-");
                if (minusPosition == 0) {
                    //"-42" --> "42"
                    editableNumber.deleteCharAt(0);
                } else {
                    //"41" --> -41"
                    editableNumber.insert(0, "-");
                }
            }
        }

        boolean addedNumber = updateNumberStackFromEditable(flagRemove);
        if (save) {
            if (addedNumber) {
                change.stackNewSize = 1;
            }
            historySaver.saveChange(change);
        }

    }

    void clickedDelete(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            return;
        }

        if (editableNumber.length() >= 1) {
            Change change = new Change(historySaver, editableNumber.toString());
            boolean removePrevious = isEditableNumberInStack();
            UpdateStackFlag flagRemove = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
//            if (save) {
//                historySaver.saveDelete(editableNumber.toString());
//            }
            char deletingChar = editableNumber.charAt(editableNumber.length() - 1);
            editableNumber.deleteCharAt(editableNumber.length() - 1);
            if (deletingChar == 'E' && deleteCharBeforeScientificNotation
                    || deletingChar == '.' && deleteCharBeforeDecimalSeparator) {
                editableNumber.deleteCharAt(editableNumber.length() - 1);
            }
            boolean addedNumber = updateNumberStackFromEditable(flagRemove);
            if (save) {
                if (addedNumber) {
                    change.stackNewSize = 1;
                }
                historySaver.saveChange(change);
            }
        } else if (editableNumber.length() == 1) {
            clickedPopNumber(save);
        }
    }

    boolean clickedEnter(boolean save) {
        Change change = new Change(historySaver, editableNumber.toString());

        boolean editableEntered = false;
        if (editableNumber.length() == 0) {
            //"" uses stack instead of editableNumber, if possible
            if (numberStack.size() != 0) {
                BigDecimal number = getLastNumber();
//                if (save) {
//                    historySaver.saveEnter(null);
//                }

                addNumber(number);
                if (save) {
                    change.stackNewSize = 1;
                }
            }
        } else if (editableNumber.toString().equals("-")) {
            //noop; disallow "-"
            save = false;
        } else {
            boolean endsInEMinus = false;
            int end = editableNumber.length();
            int start = end - 2;
            try {
                String ending = editableNumber.toString().substring(start, end);
                if (ending.equals("E-")) {
                    endsInEMinus = true;
                }
            }catch(IndexOutOfBoundsException e) {
                //noop
            }

            if (endsInEMinus) {
                //noop; disallow "1.2E-"
                save = false;
            } else {
//                if (save) {
//                    historySaver.saveEnter(editableNumber.toString());
//                }
                //TODO check final . or E or .E. I must fix those to build a working BigDecimal, or just let the last number in stack be the right one
//            addNumber(number);//Because number is already in the stack
//            showNumber(number);
//            showDigits(number);
//            showWritingDigits(false);
                editableNumber.reset();
                editableEntered = true;
            }
        }

        if(save){
            historySaver.saveChange(change);
        }

        return editableEntered;
    }

    void clickedPopNumber(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            return;
        }

        Change change = new Change(historySaver, editableNumber.toString());

        if (editableNumber.length() > 0) {
//            if (save) {
//                historySaver.savePop(editableNumber.toString());
//            }
            boolean removePrevious = !editableNumber.toString().equals("-");
            UpdateStackFlag removeFlag = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
            editableNumber.reset();
            if(save) {
                change.addOld(getLastNumber());
            }
            updateNumberStackFromEditable(removeFlag);
        } else {
            BigDecimal poppedNumber = popLastNumber();
            if(save){
                change.addOld(poppedNumber);
                historySaver.saveChange(change);
            }
//            if (save) {
//                historySaver.savePop(poppedNumber);
//            }
        }
    }

    void clickedClear(boolean save) {
        Change change = new Change(historySaver, editableNumber.toString());
//        if (save) {
//            historySaver.saveClear(numberStack, editableNumber.toString());
//        }
        Collection<BigDecimal> cleared = clearStack();
        if(save){
            for (BigDecimal number : cleared) {
                change.addOld(number);
            }
            historySaver.saveChange(change);
        }
        editableNumber.reset();
    }

    void clickedSwitch(boolean save) {
//        if (numberStack.size() >= 2) {
//            if (save) {
//                historySaver.saveSwitch();
//            }
//            BigDecimal num2 = popLastNumber();
//            BigDecimal num1 = popLastNumber();
//            addNumber(num2);
//            addNumber(num1);
//            showNumber(num2);
//            showNumber(num1);
//            if (mListDigits.size() == 0 && mPosDecimal == -1) {
//                showDigits(num1);
//                showWritingDigits(false);
//            }
//        }
        //TODO unimplemented for now, better wait for full rearrangement of numbers
    }

    void clickedOperation(Operation operation) {
        Change change = new Change(historySaver, editableNumber.toString());

        final int arity = operationsData.get(operation).arity;
        int numberOfArguments;
        boolean editableInStack = isEditableNumberInStack();
        editableNumber.reset();
        if (numberStack.size() < ((arity == ARITY_ALL) ? 1 : arity)) {
            showError(getResources().getString(R.string.notEnoughArguments));
        } else {
            if (arity == ARITY_ALL) {
                numberOfArguments = numberStack.size();
            } else if (arity == ARITY_ZERO_ONE) {
                if (editableInStack) {
                    numberOfArguments = 1;
                }else {
                    numberOfArguments = 0;
                }
            } else {
                numberOfArguments = arity;
            }
            BigDecimal[] operands = popNumbers(numberOfArguments);

            BigDecimal result = BigDecimal.ZERO;//valor inicial no usado pero necesario, TODO creo
            BigDecimal radians;
            String error = null;
            switch (arity) {
                case ARITY_ALL:
                    switch (operation) {
                        case SUMMATION:
                            result = operands[0];
                            for (int i = 1; i < numberOfArguments; ++i) {
                                result = result.add(operands[i]);
                            }
                            break;
                        case MEAN:
                            result = operands[0];
                            int precision = 1;
                            for (int i = 1; i < numberOfArguments; ++i) {
                                precision = Math.max(precision, operands[i].precision());
                                result = result.add(operands[i]);
                            }
                            precision = Math.max(precision, GOOD_PRECISION);
                            MathContext mathContext = new MathContext(precision, ROUNDING_MODE);
                            result = result.divide(new BigDecimal(numberOfArguments),mathContext);
                            break;
                        default:
                            error = getResources().getString(R.string.operationNotImplemented);
                            break;
                    }
                    break;
                case ARITY_ZERO_ONE:
                    result = (operation == Operation.CONSTANTPI ? BIG_PI : (operation == Operation.CONSTANTEULER ? BIG_EULER : BIG_PHI).round(DEFAULT_DIVIDE_CONTEXT));
                    if(operands.length == 1){
                        result = result.multiply(operands[0], getGoodContext(operands[0]));
                    }
                    break;
                case 1:
                    switch (operation) {
                        case INVERSION:
                            if (operands[0].compareTo(BigDecimal.ZERO) == 0) {
                                error = getResources().getString(R.string.divisionByZero);
                            } else {
                                result = BigDecimal.ONE.divide(operands[0], DEFAULT_DIVIDE_CONTEXT);
                            }
                            break;
                        case SQUARE:
                            result = operands[0].pow(2, getGoodContext(operands[0]));
                            break;
                        case NEGATIVE:
                            result = operands[0].negate();
                            break;
                        case SQUAREROOT:
                            if (operands[0].compareTo(BigDecimal.ZERO) < 0) {
                                error = getResources().getString(R.string.negativeSquareRoot);
                            } else {
                                result = new BigDecimal(Math.sqrt(operands[0].doubleValue()), getGoodContext(operands[0]));
                            }
                            break;
                        case SINE:
                            radians = operands[0];
                            if (angleMode == AngleMode.DEGREE) {
                                radians = toRadians(operands[0]);
                            }
                            result = new BigDecimal(Math.sin(radians.doubleValue()), getGoodContext(operands[0]));
                            break;
                        case COSINE:
                            radians = operands[0];
                            if (angleMode == AngleMode.DEGREE) {
                                radians = toRadians(operands[0]);
                            }
                            result = new BigDecimal(Math.cos(radians.doubleValue()), getGoodContext(operands[0]));
                            break;
                        case TANGENT:
                            radians = operands[0];
                            if (angleMode == AngleMode.DEGREE) {
                                radians = toRadians(operands[0]);
                            }
                            result = new BigDecimal(Math.tan(radians.doubleValue()), getGoodContext(operands[0]));
                            break;
                        case ARCSINE:
                            if (operands[0].compareTo(BigDecimal.ONE.negate()) < 0
                                    || operands[0].compareTo(BigDecimal.ONE) > 0) {
                                error = getResources().getString(R.string.arcsineOutOfRange);
                            } else {
                                result = new BigDecimal(Math.asin(operands[0].doubleValue()), getGoodContext(operands[0]));
                                if (angleMode == AngleMode.DEGREE) {
                                    result = toDegrees(result);
                                }
                            }
                            break;
                        case ARCCOSINE:
                            if (operands[0].compareTo(new BigDecimal("-1.0")) < 0
                                    || operands[0].compareTo(new BigDecimal("1.0")) > 0) {
                                error = getResources().getString(R.string.arccosineOutOfRange);
                            } else {
                                result = new BigDecimal(Math.acos(operands[0].doubleValue()), getGoodContext(operands[0]));
                                if (angleMode == AngleMode.DEGREE) {
                                    result = toDegrees(result);
                                }
                            }
                            break;
                        case ARCTANGENT:
                            result = new BigDecimal(Math.atan(operands[0].doubleValue()), getGoodContext(operands[0]));
                            if (angleMode == AngleMode.DEGREE) {
                                result = toDegrees(result);
                            }
                            break;
                        case LOG10:
                            if (operands[0].compareTo(BigDecimal.ZERO) <= 0) {
                                error = getResources().getString(R.string.logOutOfRange);
                            } else {
                                result = new BigDecimal(Math.log10(operands[0].doubleValue()), getGoodContext(operands[0]));
                            }
                            break;
                        case LOGN:
                            if (operands[0].compareTo(BigDecimal.ZERO) <= 0) {
                                error = getResources().getString(R.string.logOutOfRange);
                            }else{
                                result = new BigDecimal(Math.log(operands[0].doubleValue()), getGoodContext(operands[0]));
                            }
                            break;
                        case EXPONENTIAL:
                            result = new BigDecimal(Math.exp(operands[0].doubleValue()), getGoodContext(operands[0]));
                            break;
                        case EXP10:
                            result = new BigDecimal(Math.pow(BigDecimal.TEN.doubleValue(), operands[0].doubleValue()), getGoodContext(operands[0]));
                            break;
                        case DEGTORAD:
                            result = toRadians(operands[0]);
                            break;
                        case RADTODEG:
                            result = toDegrees(operands[0]);
                            break;
                        case CIRCLE_SURFACE:
                            if (operands[0].compareTo(BigDecimal.ZERO) < 0) {
                                error = getResources().getString(R.string.negativeRadius);
                            } else {
                                result = operands[0].pow(2).multiply(BIG_PI, getGoodContext(operands[0]));
                            }
                            break;
                        default:
                            error = getResources().getString(R.string.operationNotImplemented);
                            break;
                    }
                    break;
                case 2:
                    switch (operation) {
                        case ADDITION:
                            result = operands[0].add(operands[1]);
                            break;
                        case MULTIPLICATION:
                            result = operands[0].multiply(operands[1]);
                            break;
                        case EXPONENTIATION:
                            //y^x; x is operands[1]; y is operands[0]
                            if (operands[0].compareTo(BigDecimal.ZERO) > 0) {
                                result = new BigDecimal(Math.pow(operands[0].doubleValue(), operands[1].doubleValue()), getGoodContext(operands));
                            }else{
                                try {
                                    BigInteger exponent = operands[1].toBigIntegerExact();
                                    result = new BigDecimal(Math.pow(operands[0].doubleValue(), exponent.doubleValue()));
                                }catch(ArithmeticException e) {
                                    error = getResources().getString(R.string.negativeBaseExponentiation);
                                }
                            }
                            break;
                        case SUBTRACTION:
                            result = operands[0].subtract(operands[1]);
                            break;
                        case DIVISION:
                            if (operands[1].compareTo(BigDecimal.ZERO) == 0) {
                                error = getResources().getString(R.string.divisionByZero);
                            } else {
                                result = operands[0].divide(operands[1], getGoodContext(operands));
                            }
                            break;
                        case ROOTYX:
                            //x^(1/y); x is operands[1]; y is operands[0]
                            if (operands[1].compareTo(BigDecimal.ZERO) > 0) {
                                result = new BigDecimal(Math.pow(operands[1].doubleValue(), BigDecimal.ONE.divide(operands[0], DEFAULT_DIVIDE_CONTEXT).doubleValue()), getGoodContext(operands));
                            }else{
//                                try {
//                                    BigInteger index = operands[1].toBigIntegerExact();
//                                    result = new BigDecimal(Math.pow(operands[0].doubleValue(), BigDecimal.ONE.divide(operands[1], DEFAULT_DIVIDE_CONTEXT).doubleValue()), getGoodContext(operands));
//                                }catch(ArithmeticException e) {
                                    error = getResources().getString(R.string.negativeRadicand);
//                                }
                            }
                            break;
                        case LOGYX:
                            //log(x) in base y; x is operands[1]; y is operands[0]
                            if (operands[0].compareTo(BigDecimal.ZERO) <= 0
                                    || operands[1].compareTo(new BigDecimal("0.0")) <= 0) {
                                error = getResources().getString(R.string.logOutOfRange);
                            } else {
                                result = new BigDecimal(Math.log(operands[1].doubleValue())).
                                        divide(new BigDecimal(Math.log(operands[0].doubleValue())), getGoodContext(operands));
                            }
                            break;
                        case HYPOTENUSE_PYTHAGORAS:
                            if (operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO) < 0) {
                                error = getResources().getString(R.string.sideCantBeNegative);
                            } else {
                                result = new BigDecimal(
                                        Math.hypot(operands[0].doubleValue(), operands[1].doubleValue()),
                                        getGoodContext(operands));
                            }
                            break;
                        case LEG_PYTHAGORAS:
                            if (operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO) < 0) {
                                error = getResources().getString(R.string.sideCantBeNegative);
                            } else {
                                BigDecimal hyp = operands[0].max(operands[1]);
                                BigDecimal leg = operands[0].min(operands[1]);
                                result = new BigDecimal(
                                        Math.sqrt(hyp.pow(2).subtract(leg.pow(2)).doubleValue()),
                                        getGoodContext(operands));
                            }
                            break;
                        default:
                            error = getResources().getString(R.string.operationNotImplemented);
                            break;
                    }
                    break;
                case 3:
                    switch (operation) {
                        case TRIANGLE_SURFACE:
                            BigDecimal p = operands[0].add(operands[1]).add(operands[2]).divide(new BigDecimal(2), BigDecimal.ROUND_UNNECESSARY);
                            BigDecimal q = p.multiply(p.subtract(operands[0]))
                                    .multiply(p.subtract(operands[1]))
                                    .multiply(p.subtract(operands[2]));
                            if (q.compareTo(BigDecimal.ZERO) < 0) {
                                error = getResources().getString(R.string.notATriangle);
                            } else {
                                result = new BigDecimal(Math.sqrt(q.doubleValue()), getGoodContext(operands));
                            }
                            break;
                        default:
                            error = getResources().getString(R.string.operationNotImplemented);
                            break;
                    }
                    break;
            }

            if (error != null) {
                showError(error);
                addNumbers(operands);
            } else {
//                historySaver.saveOperation(operands, editableNumber.toString(), result);
                for (BigDecimal operand : operands) {
                    change.addOld(operand);
                }
                change.stackNewSize = 1;//TODO change this line if support for functions with multiple results is added
                historySaver.saveChange(change);
                addNumber(result);
            }
        }
    }

    private static BigDecimal toDegrees(BigDecimal radians) {
        return radians.multiply(new BigDecimal(180)).divide(BIG_PI, getGoodContext(radians));
    }

    private static BigDecimal toRadians(BigDecimal degrees) {
        return degrees.multiply(BIG_PI).divide(new BigDecimal("180.000000"), getGoodContext(degrees));
    }

    private static MathContext getGoodContext(BigDecimal number) {
        return new MathContext(Math.max(GOOD_PRECISION, number.precision()), ROUNDING_MODE);
    }

    private static MathContext getGoodContext(BigDecimal[] operands) {
        int precision = GOOD_PRECISION;
        for (int i = 0; i < operands.length; i++) {
            precision = Math.max(precision, operands[i].precision());
        }
        return new MathContext(precision, ROUNDING_MODE);
    }

    private static MathContext getImprovedContext(BigDecimal number) {
        return new MathContext(Math.max(GOOD_PRECISION, number.precision()) + 1, ROUNDING_MODE);
    }

    private MathContext getImprovedContext(BigDecimal[] operands) {
        int precision = GOOD_PRECISION;
        for (int i = 0; i < operands.length; i++) {
            precision = Math.max(precision, operands[i].precision());
        }
        return new MathContext(precision + 1, ROUNDING_MODE);    }

    public static String toString(BigDecimal number) {
        int precision = number.precision();
        if (precision >= GOOD_PRECISION) {
            precision--;
        }
        return number.round(new MathContext(precision, ROUNDING_MODE)).toString();
    }

//    void showNumber(BigDecimal number) {
//        TextView tv = new TextView(this);
//        // HACER poner atributos al tv
//        // float margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//        // 14, getResources().getDisplayMetrics());
//        float margin = getResources().getDimension(R.dimen.number_view_horiz_margin);
////		float textSize = getResources().getDimension(R.dimen.list_numbers_text_size);
//        int textSize = getResources().getDimensionPixelSize(R.dimen.list_numbers_text_size);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT);
//        params.setMargins((int) margin, 0, (int) margin, 0);
//        tv.setLayoutParams(params);
//        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
//        tv.setGravity(Gravity.CENTER_VERTICAL);
//        tv.setText(localizeDecimalSeparator(removeZeros(number.toString(), false, ".")));
//
//        scrollNumbersToBottom();
//        return;
//    }

    String localizeDecimalSeparator(String str) {
        StringBuilder localized = new StringBuilder(str);
        char decimalSeparator = getResources().getString(R.string.decimalSeparator).charAt(0);
        //TODO man, wtf, use indexOf()
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                localized.setCharAt(i, decimalSeparator);
                break;
            }
        }
        return localized.toString();
    }

//    void showDigits(List<Integer> digits, int decimal) {
//        tvEditableNumber.setText(buildString(digits, decimal, false, getResources().getString(R.string.decimalSeparator)));
//        showOnlyScrollView(scrollEditableNumber);
//    }
//
//    void showDigitsWithZeros(List<Integer> digits, int decimal) {
//        tvEditableNumber.setText(buildString(digits, decimal, true, getResources().getString(R.string.decimalSeparator)));
//        showOnlyScrollView(scrollEditableNumber);
//    }
//
//    void showDigits(BigDecimal number) {
//        tvEditableNumber.setText(localizeDecimalSeparator(removeZeros(number.toString(), false, ".")));
//        showOnlyScrollView(scrollEditableNumber);
//    }
//
//    void removeLastLayoutNumber() {
//        //TODO check calls
////        if (layoutNumbers.getChildCount() != 0) {
////            layoutNumbers.removeViewAt(layoutNumbers.getChildCount() - 1);
////        }
//    }

    void showError(String str) {
        tvError.setText(getResources().getString(R.string.error) + ": " + str);
        scrollError.setVisibility(View.VISIBLE);
//        tvError.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                tvError.setVisibility(View.VISIBLE);
//            }
//        }, 1000);
        scrollEditableNumber.setVisibility(GONE);
//        showOnlyScrollView(scrollError);
    }

//    void showOnlyScrollView(HorizontalScrollView view) {
//        if (view == scrollEditableNumber) {
//            showScrollView(scrollError, false);
//            scrollNumbersToBottom();
//        } else if (view == scrollError) {
//            showScrollView(scrollEditableNumber, false);
//        }
//        showScrollView(view, true);
//    }
//
//    void showScrollView(HorizontalScrollView view, boolean show) {
//        if (show) {
//            view.setVisibility(View.VISIBLE);
//        } else {
//            view.setVisibility(View.GONE);
//        }
//    }
//
//    void showWritingDigits(boolean show) {
//        if (show) {
//            tvWritingDigits.setVisibility(View.VISIBLE);
//        } else {
//            tvWritingDigits.setVisibility(View.INVISIBLE);
//        }
//    }
//
//    int getWritingDigitsVisibility() {
//        return tvWritingDigits.getVisibility();
//    }
//
//    View getShownView() {
//        if (scrollError.getVisibility() == View.VISIBLE) {
//            return scrollError;
//        } else if (scrollEditableNumber.getVisibility() == View.VISIBLE) {
//            return scrollEditableNumber;
//        } else {
//            return null;
//        }
//    }

    void showAngleMode() {
//        if (usingViewAnimator == false) {
//            tvAngleMode.setVisibility(View.VISIBLE);
//        } else if (switcherFunctions.getDisplayedChild() == 1) {
//            tvAngleMode.setVisibility(View.VISIBLE);
//        } else {
//            tvAngleMode.setVisibility(View.INVISIBLE);
//        }
        tvAngleMode.setVisibility(View.VISIBLE);
    }

    static String buildString(List<Integer> listDigits, int pos, boolean keepTrailingZeros, String decimalSeparator) {
        int numberOfDigits = listDigits.size();
        StringBuilder numberString = new StringBuilder(numberOfDigits + 2);
        for (int i = 0; i < numberOfDigits; ++i) {
            if (pos == i) {
                if (pos == 0) {
                    numberString.append(0);
                }
                numberString.append(decimalSeparator);
            }
            numberString.append(listDigits.get(i));
        }
        if (pos == numberOfDigits) {
            numberString.append(decimalSeparator);
        }
        return removeZeros(numberString.toString(), keepTrailingZeros, decimalSeparator);
    }

    static BigDecimal buildNumber(List<Integer> listDigits, int pos) {
        int numberOfDigits = listDigits.size();
        BigDecimal number;
        if (numberOfDigits == 0) {
            number = new BigDecimal("0.0");
        } else {
            StringBuilder numberString = new StringBuilder(numberOfDigits + 2);
            for (int i = 0; i < numberOfDigits; ++i) {
                if (pos == i) {
                    if (pos == 0) {
                        numberString.append(0);
                    }
                    numberString.append(".");
                }
                numberString.append(listDigits.get(i));
            }
            number = new BigDecimal(numberString.toString());
        }
        return number;
    }

    static String removeZeros(String string, boolean keepTrailingZeros, String decimalSeparator) {
        char decimalSeparatorChar = decimalSeparator.charAt(0);
        boolean keepRemoving = true;
        boolean scientific = false;
        StringBuilder str = new StringBuilder(string);
        while (keepRemoving && str.length() >= 2) {// delete left zeros
            if (str.charAt(0) == '0'
                    && str.charAt(1) != decimalSeparatorChar
                    && str.charAt(1) != 'E') {
                str.deleteCharAt(0);
            } else {
                if (str.charAt(1) == 'E') {
                    scientific = true;
                }
                keepRemoving = false;
            }
        }
        // keepRemoving = !keepTrailingZeros && !scientific;
        keepRemoving = !(keepTrailingZeros || scientific);
        if (!keepRemoving) {
            if (scientific) {
                return "0";
            } else {
                return str.toString();
            }
        }
        int pos = 0;
        int posDec = str.length();
        while (pos < str.length()) {
            if (str.charAt(pos) == decimalSeparatorChar) {
                posDec = pos;
            }
            pos++;
        }
        pos = str.length() - 1;
        while (keepRemoving && posDec < pos) {
            if (str.charAt(pos) == '0') {
                str.deleteCharAt(pos);
                if (str.charAt(pos - 1) == decimalSeparatorChar) {
                    str.deleteCharAt(pos - 1);
                    keepRemoving = false;
                }
                pos--;
            } else {
                keepRemoving = false;
            }
        }
        return str.toString();
    }

    static ArrayList<String> buildStringArrayListNumbers(List<BigDecimal> numbers) {
        ArrayList<String> stringNumbers = new ArrayList<String>();
        for (BigDecimal num : numbers) {
            stringNumbers.add(removeZeros(num.toString(), false, "."));
        }
        return stringNumbers;
    }

}
//TODO semitransparent fn+ icon, and move that + symbol