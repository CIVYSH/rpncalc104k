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
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.terlici.dragndroplist.DragNDropListView;

import static android.view.View.FOCUS_RIGHT;
import static android.view.View.GONE;

public class MainActivity extends Activity {
    final static String TAG = MainActivity.class.getSimpleName();

    final static BigDecimal BIG_PI = new BigDecimal(Math.PI);
    final static BigDecimal BIG_EULER = new BigDecimal(Math.E);
    final static BigDecimal BIG_PHI = new BigDecimal("1.618033988749894");

    final static int ARITY_ALL = -1;// takes all numbers in stack
    final static int ARITY_ZERO_ONE = -2;// takes one number if it's being written by user
    final static int ARITY_N = -3;// takes N+1 numbers, let N be the first number in the stack

    final static int GOOD_PRECISION = 10;
    final static RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    final static MathContext DEFAULT_MATH_CONTEXT = new MathContext(GOOD_PRECISION, ROUNDING_MODE);

    final static String NUMBER_STACK_PARAM = "NumberStack";
    final static String DEGREE_MODE_PARAM = "AngleMode";
    final static String HISTORY_SAVER_PARAM = "HistorySaver";
    final static String SWITCHER_INDEX_PARAM = "SwitcherIndex";
    static final String EDITABLE_NUMBER_PARAM = "EditableNumber";

    static final boolean MERGE_DIGITS = true;
    static final boolean MERGE_DECIMAL = true;
    static final boolean MERGE_MINUS = true;
    static final boolean MERGE_E = true;

    private class ListViewScroller implements Runnable {
        ListView view;

        ListViewScroller(ListView view) {
            this.view = view;
        }

        public void run() {
            if (Build.VERSION.SDK_INT >= 8) {
                view.smoothScrollToPosition(numberStack.size() - 1);
            }
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
            layoutNumbersDraggable.post(new ListViewScroller(layoutNumbersDraggable));
        }
    };

    LinkedList<BigDecimal> numberStack = new LinkedList<>();

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
    Integer[] digitButtonsIds;
    HistorySaver historySaver;

    DragNDropListView layoutNumbersDraggable;
    HorizontalScrollView scrollEditableNumber, scrollError;
    TextView tvEditableNumber, tvError, tvAngleMode;
    ViewAnimator switcherFunctions;

    NumberStackDragableAdapter numberStackDragableAdapter;

    private class OperationData {
        int arity;
        Integer[] btnIds;

        OperationData(int arity, Integer... btnIds) {
            this.arity = arity;
            this.btnIds = btnIds;
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
        FACTORIAL,

        SINE,
        COSINE,
        TANGENT,
        ARCSINE,
        ARCCOSINE,
        ARCTANGENT,
        SINE_H,
        COSINE_H,
        TANGENT_H,
        DEGTORAD,
        RADTODEG,

        FLOOR,
        ROUND,
        CEIL,

        SUMMATION,
        SUMMATION_N,
        MEAN,
        MEAN_N,
        CONSTANTPI,
        CONSTANTEULER,
        CONSTANTPHI,
        RANDOM,

        CIRCLE_SURFACE,
        TRIANGLE_SURFACE,
        HYPOTENUSE_PYTHAGORAS,
        LEG_PYTHAGORAS,
        QUARATIC_EQUATION
    }

    enum AngleMode {
        DEGREE, RADIAN
    }

    interface Change{
        void undo();
        void redo();
    }

    static class SimpleChange implements Change{
        Deque<BigDecimal> redoNumbers = new LinkedList<>();//ArrayDeque from API 9
        Deque<BigDecimal> undoNumbers = new LinkedList<>();
        int redoNumbersSize = 0;
        String redoText, undoText;
        HistorySaver saver;
        boolean canMerge;
        String tag = "";

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
            MainActivity activity = saver.getActivity();

            BigDecimal[] poppedNew = activity.popNumbers(redoNumbersSize);
            redoNumbers.clear();
            redoNumbers.addAll(Arrays.asList(poppedNew));
            activity.addNumbers(undoNumbers);

            redoText = activity.editableNumber.toString();
            activity.editableNumber.set(undoText);
        }

        @Override
        public void redo() {
            MainActivity activity = saver.getActivity();

            activity.popNumbers(undoNumbers.size());
            activity.addNumbers(redoNumbers);

            activity.editableNumber.set(redoText);
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
            }else{
                return null;
            }
        }
    }

    static class SwapChange implements Change{
        int startPosition, endPosition;
        HistorySaver saver;
        String undoText;

        public SwapChange(HistorySaver saver, String undoText, int startPosition, int endPosition) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.saver = saver;
            this.undoText = undoText;
        }


        @Override
        public void undo() {
            MainActivity activity = saver.getActivity();

            BigDecimal draggingNumber = activity.numberStack.remove(endPosition);
            activity.numberStack.add(startPosition, draggingNumber);
            activity.editableNumber.set(undoText);

            activity.numberStackDragableAdapter.notifyDataSetChanged();
        }

        @Override
        public void redo() {
            MainActivity activity = saver.getActivity();

//            activity.clickedSwap(false, startPosition, endPosition);
            BigDecimal draggingNumber = activity.numberStack.remove(startPosition);
            activity.numberStack.add(endPosition, draggingNumber);
            activity.editableNumber.reset();

            activity.numberStackDragableAdapter.notifyDataSetChanged();
        }
    }

    static class HistorySaver implements Serializable {
        transient WeakReference<MainActivity> activity;
        private LinkedList<Change> changes = new LinkedList<Change>();
        int currentChangeIndex;

        HistorySaver(MainActivity activity) {
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
            if(previousChange instanceof SimpleChange){
                SimpleChange mergedChange = change.merge((SimpleChange) previousChange);
                if (mergedChange != null) {
                    insertChange(currentChangeIndex, mergedChange);
                }else {
                    insertChange(currentChangeIndex + 1, change);
                }
            } else {
                insertChange(currentChangeIndex + 1, change);
            }
        }

        void saveSwapChange(SwapChange change){
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

        MainActivity getActivity() {
            return activity.get();
        }

        void setActivity(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }
    }

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
                Log.d(TAG, "Some button is not linked to any Operation");
            }
        }
    };

    private Operation getOperationFromBtnId(int id) {
        for (Map.Entry<Operation, OperationData> dataEntry : operationsData.entrySet()) {
            if (Arrays.asList(dataEntry.getValue().btnIds).contains(id)) {
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

        switcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);

        scrollEditableNumber = (HorizontalScrollView) findViewById(R.id.scrollEditableNumber);
        scrollError = (HorizontalScrollView) findViewById(R.id.scrollError);
        layoutNumbersDraggable = (DragNDropListView) findViewById(R.id.listNumbersDraggable);
        tvAngleMode = (TextView) findViewById(R.id.tvAngleMode);
        tvEditableNumber = (TextView) findViewById(R.id.tvEditableNumber);
        tvError = (TextView) findViewById(R.id.tvError);

        tvEditableNumber.setGravity(Gravity.END | Gravity.CENTER);

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
        operationsData.put(Operation.FACTORIAL, new OperationData(1, R.id.butFactorial));

        operationsData.put(Operation.SINE, new OperationData(1, R.id.butSine));
        operationsData.put(Operation.COSINE, new OperationData(1, R.id.butCosine));
        operationsData.put(Operation.TANGENT, new OperationData(1, R.id.butTangent));
        operationsData.put(Operation.ARCSINE, new OperationData(1, R.id.butArcSine));
        operationsData.put(Operation.ARCCOSINE, new OperationData(1, R.id.butArcCosine));
        operationsData.put(Operation.ARCTANGENT, new OperationData(1, R.id.butArcTangent));
        operationsData.put(Operation.SINE_H, new OperationData(1, R.id.butSineH));
        operationsData.put(Operation.COSINE_H, new OperationData(1, R.id.butCosineH));
        operationsData.put(Operation.TANGENT_H, new OperationData(1, R.id.butTangentH));

        operationsData.put(Operation.FLOOR, new OperationData(1, R.id.butFloor));
        operationsData.put(Operation.ROUND, new OperationData(1, R.id.butRound));
        operationsData.put(Operation.CEIL, new OperationData(1, R.id.butCeil));

        operationsData.put(Operation.DEGTORAD, new OperationData(1, R.id.butDegreeToRadian));
        operationsData.put(Operation.RADTODEG, new OperationData(1, R.id.butRadianToDegree));
        operationsData.put(Operation.RANDOM, new OperationData(0, R.id.butRandom));

        operationsData.put(Operation.CONSTANTPI, new OperationData(ARITY_ZERO_ONE, R.id.butPi));
        operationsData.put(Operation.CONSTANTEULER, new OperationData(ARITY_ZERO_ONE, R.id.butEuler));
        operationsData.put(Operation.CONSTANTPHI, new OperationData(ARITY_ZERO_ONE, R.id.butPhi));

        operationsData.put(Operation.SUMMATION, new OperationData(ARITY_ALL, R.id.butSummation));
        operationsData.put(Operation.SUMMATION_N, new OperationData(ARITY_N, R.id.butSummationRange));
        operationsData.put(Operation.MEAN, new OperationData(ARITY_ALL, R.id.butMean));
        operationsData.put(Operation.MEAN_N, new OperationData(ARITY_N, R.id.butMeanRange));

        operationsData.put(Operation.CIRCLE_SURFACE, new OperationData(1, R.id.butCircleSurface));
        operationsData.put(Operation.TRIANGLE_SURFACE, new OperationData(3, R.id.butTriangleSurface));
        operationsData.put(Operation.HYPOTENUSE_PYTHAGORAS, new OperationData(2, R.id.butHypotenusePythagoras));
        operationsData.put(Operation.LEG_PYTHAGORAS, new OperationData(2, R.id.butLegPythagoras));
        operationsData.put(Operation.QUARATIC_EQUATION, new OperationData(3, R.id.butQuadraticEquation));

        if (operationsData.size() != Operation.values().length) {
            throw new RuntimeException("There are operations not implemented");
        }

        for (Operation operation : Operation.values()) {
            for (int i = 0; i < operationsData.get(operation).btnIds.length; i++) {
                int btnId = operationsData.get(operation).btnIds[i];
                findViewById(btnId).setOnClickListener(clickListenerOperations);
            }
        }


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

        ((Button) findViewById(R.id.butDecimal)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedDecimal(true);
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
        ((ImageButton) findViewById(R.id.butDegRad)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchAngleMode();
            }
        });


        ((ImageButton) findViewById(R.id.butFunctionsMore)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                switcherFunctions.showNext();
            }
        });
        ((ImageButton) findViewById(R.id.butFunctionsLess)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                switcherFunctions.showNext();
            }
        });
        ((ImageButton) findViewById(R.id.butSwitch)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedSwap(true, -1, -1);
            }
        });
        ((ImageButton) findViewById(R.id.butSwitch2)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                clickedSwap(true, -1, -1);
            }
        });

        numberStack = new LinkedList<BigDecimal>();
        historySaver = new HistorySaver(this);

        Resources res = getResources();
        String buttonName;
        digitButtonsIds = new Integer[10];
        for (int i = 0; i < 10; i++) {
            buttonName = "but" + Integer.toString(i);
            digitButtonsIds[i] = res.getIdentifier(buttonName, "id", getPackageName());
            ((Button) findViewById(digitButtonsIds[i])).setOnClickListener(clickListenerDigitButtons);
        }

        numberStackDragableAdapter = new NumberStackDragableAdapter(this);
        numberStackDragableAdapter.registerDataSetObserver(numberStackObserver);
        layoutNumbersDraggable.setDragNDropAdapter(numberStackDragableAdapter);

        layoutNumbersDraggable.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long itemId) {
                BigDecimal number = (BigDecimal) adapter.getItemAtPosition(position);
                MainActivity.this.clickedStackNumber(true, number);
            }
        });

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

//		 this hack always shows overflow menu in actionbar.
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
        numberStackDragableAdapter.notifyDataSetChanged();
        return cleared;
    }

    private void addNumber(BigDecimal number) {
        numberStack.add(number);
        numberStackDragableAdapter.notifyDataSetChanged();
    }

    private void addNumbers(BigDecimal[] numbers) {
        addNumbers(Arrays.asList(numbers));
    }

    private void addNumbers(Collection<BigDecimal> numbers) {
        numberStack.addAll(numbers);
        numberStackDragableAdapter.notifyDataSetChanged();
    }

    private BigDecimal popLastNumber() {
        try {
            BigDecimal number = numberStack.removeLast();
            numberStackDragableAdapter.notifyDataSetChanged();
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
        numberStackDragableAdapter.notifyDataSetChanged();
        return numbers;
    }

    /**
     * build a BigDecimal from editableNumber and insert it if possible
     *
     * @param flag indicates whether it must remove the last number in the stack before
     *             inserting the new one
     * @return true if a new number has been built and inserted in the stack
     */
    boolean addEditableToStack(UpdateStackFlag flag) {
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
                numberStackDragableAdapter.notifyDataSetChanged();
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

    void clickedDigit(int digit, boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString(), MERGE_DIGITS);
        simpleChange.tag = "Digit: " + digit;

        boolean removePrevious = isEditableNumberInStack();
        if (save && removePrevious) {
            simpleChange.addOld(getLastNumber());
        }
        editableNumber.append(String.valueOf(digit));

        boolean addedNumber = addEditableToStack(removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS);
        if (save) {
            if (addedNumber) {
                simpleChange.redoNumbersSize = 1;
            }
            historySaver.saveSimpleChange(simpleChange);
        }
    }

    void clickedDecimal(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString(), MERGE_DECIMAL);

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

        if (updated) {
            if (save && removePrevious) {
                simpleChange.addOld(getLastNumber());
            }
            boolean addedNumber = addEditableToStack(flagRemove);
            if (save) {
                if (addedNumber) {
                    simpleChange.redoNumbersSize = 1;
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    void clickedScientificNotation(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString(), MERGE_E);

        boolean removePrevious = isEditableNumberInStack();
        UpdateStackFlag flagRemove = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
        boolean updated = true;
        if (editableNumber.length() == 0) {
            //"" -> "1E"
            editableNumber.append("1E");
            deleteCharBeforeScientificNotation = true;
        } else if (editableNumber.toString().equals("-")) {
            //"-" -> "-1E"
            editableNumber.append("1E");
            deleteCharBeforeScientificNotation = true;
        } else if (Character.valueOf(editableNumber.charAt(editableNumber.length() - 1)).equals('.')) {
            //"3." -> "3.0E"
            editableNumber.append("0E");
            deleteCharBeforeScientificNotation = true;
        } else if (editableNumber.indexOf("E") == -1) {
            //"1.23" -> "1.23E"
            editableNumber.append("E");
            deleteCharBeforeScientificNotation = false;
        } else {
            //disallow duplicated E
            updated = false;
            //don't change deleteCharBeforeScientificNotation
        }

        if (updated) {
            if (save && removePrevious) {
                simpleChange.addOld(getLastNumber());
            }
            boolean addedNumber = addEditableToStack(flagRemove);
            if (save) {
                if (addedNumber) {
                    simpleChange.redoNumbersSize = 1;
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    void clickedMinus(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString(), MERGE_MINUS);

        boolean removePrevious = isEditableNumberInStack();
        if (save && removePrevious) {
            simpleChange.addOld(getLastNumber());
        }
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

        boolean addedNumber = addEditableToStack(flagRemove);
        if (save) {
            if (addedNumber) {
                simpleChange.redoNumbersSize = 1;
            }
            historySaver.saveSimpleChange(simpleChange);
        }

    }

    void clickedDelete(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            return;
        }

        if (editableNumber.length() >= 1) {
            SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
            boolean removePrevious = isEditableNumberInStack();
            if (save && removePrevious) {
                simpleChange.addOld(getLastNumber());
            }
            UpdateStackFlag flagRemove = removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS;
            char deletingChar = editableNumber.charAt(editableNumber.length() - 1);
            editableNumber.deleteCharAt(editableNumber.length() - 1);
            if (deletingChar == 'E' && deleteCharBeforeScientificNotation
                    || deletingChar == '.' && deleteCharBeforeDecimalSeparator) {
                editableNumber.deleteCharAt(editableNumber.length() - 1);
            }
            boolean addedNumber = addEditableToStack(flagRemove);
            if (save) {
                if (addedNumber) {
                    simpleChange.redoNumbersSize = 1;
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    void clickedEnter(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        simpleChange.tag = "Enter: " + editableNumber.toString();

        if (editableNumber.length() == 0) {
            // "" --> use stack instead of editableNumber, if possible
            if (numberStack.size() != 0) {
                BigDecimal number = getLastNumber();
                addNumber(number);
                if (save) {
                    simpleChange.redoNumbersSize = 1;
                }
            }
        } else if (editableNumber.toString().equals("-")) {
            save = false;
        } else {
            editableNumber.reset();
        }

        if (save) {
            historySaver.saveSimpleChange(simpleChange);
        }
    }

    void clickedPopNumber(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            return;
        }

        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());

        boolean popLast = !editableNumber.toString().equals("-");
        BigDecimal poppedNumber = null;
        if (popLast) {
            poppedNumber = popLastNumber();
        }

        if (save) {
            if (poppedNumber != null) {
                simpleChange.addOld(poppedNumber);
                if (editableNumber.length() != 0) {
                    historySaver.saveSimpleChange(simpleChange);
                }
            }
        }

        editableNumber.reset();
    }

    void clickedClear(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        Collection<BigDecimal> cleared = clearStack();
        if (save) {
            for (BigDecimal number : cleared) {
                simpleChange.addOld(number);
            }
            historySaver.saveSimpleChange(simpleChange);
        }
        editableNumber.reset();
    }

    void clickedStackNumber(boolean save, BigDecimal number) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        simpleChange.tag = "StackNumber: " + number.toString();

        editableNumber.set(number.toString());
        addEditableToStack(UpdateStackFlag.KEEP_PREVIOUS);
        if (save) {
            simpleChange.redoNumbersSize = 1;
            historySaver.saveSimpleChange(simpleChange);
        }
    }

    void clickedSwap(boolean save, int startPosition, int endPosition) {
        if (startPosition == -1 || endPosition == -1) {
            endPosition = numberStack.size() - 1;
            startPosition = endPosition - 1;
            if (startPosition < 0 || endPosition <= 0) {
                return;
            }
        }

        SwapChange change = new SwapChange(historySaver, editableNumber.toString(), startPosition, endPosition);
        BigDecimal draggingNumber = numberStack.remove(startPosition);
        numberStack.add(endPosition, draggingNumber);
        editableNumber.reset();
        if (save) {
            historySaver.saveSwapChange(change);
        }
        numberStackDragableAdapter.notifyDataSetChanged();
    }

    void clickedOperation(Operation operation) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());

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
                } else {
                    numberOfArguments = 0;
                }
            } else if (arity == ARITY_N) {
                numberOfArguments = 1;
            } else {
                numberOfArguments = arity;
            }
            BigDecimal[] operands = popNumbers(numberOfArguments);

            List<BigDecimal> results = new ArrayList<>(1);
            String error = null;
            if (arity == ARITY_ALL) {
                BigDecimal result;
                switch (operation) {
                    case SUMMATION:
                        result = operands[0];
                        for (int i = 1; i < numberOfArguments; ++i) {
                            result = result.add(operands[i]);
                        }
                        results.add(result);
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
                        result = result.divide(new BigDecimal(numberOfArguments), mathContext);
                        results.add(result);
                        break;
                    default:
                        error = getResources().getString(R.string.operationNotImplemented);
                        break;
                }
            } else if (arity == ARITY_ZERO_ONE) {
                BigDecimal result = (operation == Operation.CONSTANTPI ? BIG_PI : (operation == Operation.CONSTANTEULER ? BIG_EULER : BIG_PHI).round(DEFAULT_MATH_CONTEXT));
                if (operands.length == 1) {
                    result = result.multiply(operands[0], getGoodContext(operands[0]));
                }
                results.add(result);
            } else if (arity == ARITY_N) {
                int userNumberOperands;
                try {
                    long nLong = operands[0].longValueExact();
                    if (nLong <= Integer.MAX_VALUE) {
                        userNumberOperands = (int) nLong;

                        if (numberStack.size() < userNumberOperands) {
                            error = getResources().getString(R.string.notEnoughArguments);
                        } else {
                            //pop N more operands
                            Collection<BigDecimal> fullOperands = new ArrayList<>(userNumberOperands + 1);
                            fullOperands.add(operands[0]);
                            fullOperands.addAll(Arrays.asList(popNumbers(userNumberOperands)));
                            operands = fullOperands.toArray(new BigDecimal[userNumberOperands + 1]);
                            BigDecimal result = BigDecimal.ZERO;
                            switch (operation) {
                                case SUMMATION_N:
                                    for (int i = 1; i <= userNumberOperands; i++) {
                                        result = result.add(operands[i]);
                                    }
                                    break;
                                case MEAN_N:
                                    for (int i = 1; i <= userNumberOperands; i++) {
                                        result = result.add(operands[i]);
                                    }
                                    result = result.divide(new BigDecimal(userNumberOperands), getGoodContext(operands));
                                    break;
                                default:
                                    error = getResources().getString(R.string.operationNotImplemented);
                                    break;
                            }
                            results.add(result);
                        }
                    } else {
                        error = getResources().getString(R.string.tooManyArguments);
                    }
                }catch(ArithmeticException e) {
                    error = getResources().getString(R.string.nIsNotInteger);
                }
            } else if (arity == 0) {
                BigDecimal result;
                switch (operation) {
                    case RANDOM:
                        result = new BigDecimal(Math.random());
                        results.add(result);
                        break;
                    default:
                        error = getResources().getString(R.string.operationNotImplemented);
                        break;
                }
            } else if (arity == 1) {
                BigDecimal result;
                BigDecimal radians;
                switch (operation) {
                    case INVERSION:
                        if (operands[0].compareTo(BigDecimal.ZERO) == 0) {
                            error = getResources().getString(R.string.divisionByZero);
                        } else {
                            result = BigDecimal.ONE.divide(operands[0], DEFAULT_MATH_CONTEXT);
                            results.add(result);
                        }
                        break;
                    case SQUARE:
                        result = operands[0].pow(2, getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case NEGATIVE:
                        result = operands[0].negate();
                        results.add(result);
                        break;
                    case SQUAREROOT:
                        if (operands[0].compareTo(BigDecimal.ZERO) < 0) {
                            error = getResources().getString(R.string.negativeSquareRoot);
                        } else {
                            result = new BigDecimal(Math.sqrt(operands[0].doubleValue()), getGoodContext(operands[0]));
                            results.add(result);
                        }
                        break;
                    case SINE:
                        radians = operands[0];
                        if (angleMode == AngleMode.DEGREE) {
                            radians = toRadians(operands[0]);
                        }
                        result = new BigDecimal(Math.sin(radians.doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case COSINE:
                        radians = operands[0];
                        if (angleMode == AngleMode.DEGREE) {
                            radians = toRadians(operands[0]);
                        }
                        result = new BigDecimal(Math.cos(radians.doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case TANGENT:
                        radians = operands[0];
                        if (angleMode == AngleMode.DEGREE) {
                            radians = toRadians(operands[0]);
                        }
                        result = new BigDecimal(Math.tan(radians.doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
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
                            results.add(result);
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
                            results.add(result);
                        }
                        break;
                    case ARCTANGENT:
                        result = new BigDecimal(Math.atan(operands[0].doubleValue()), getGoodContext(operands[0]));
                        if (angleMode == AngleMode.DEGREE) {
                            result = toDegrees(result);
                            results.add(result);
                        }
                        break;
                    case SINE_H:
                        result = new BigDecimal(Math.sinh(operands[0].doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case COSINE_H:
                        result = new BigDecimal(Math.cosh(operands[0].doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case TANGENT_H:
                        result = new BigDecimal(Math.tanh(operands[0].doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case LOG10:
                        if (operands[0].compareTo(BigDecimal.ZERO) <= 0) {
                            error = getResources().getString(R.string.logOutOfRange);
                        } else {
                            result = new BigDecimal(Math.log10(operands[0].doubleValue()), getGoodContext(operands[0]));
                            results.add(result);
                        }
                        break;
                    case LOGN:
                        if (operands[0].compareTo(BigDecimal.ZERO) <= 0) {
                            error = getResources().getString(R.string.logOutOfRange);
                        } else {
                            result = new BigDecimal(Math.log(operands[0].doubleValue()), getGoodContext(operands[0]));
                            results.add(result);
                        }
                        break;
                    case EXPONENTIAL:
                        result = new BigDecimal(Math.exp(operands[0].doubleValue()), getGoodContext(operands[0]));
                        results.add(result);
                        break;
                    case FACTORIAL:
                        try {
                            if (operands[0].compareTo(new BigDecimal(1000)) > 0) {
                                error = getResources().getString(R.string.numberTooBig);
                            } else {
                                BigInteger operand = operands[0].toBigIntegerExact();
                                result = new BigDecimal(factorial(operand));
                                results.add(result);
                            }
                        }catch(ArithmeticException e) {
                            error = getResources().getString(R.string.wrongFactorial);
                        }
                        break;
                    case DEGTORAD:
                        result = toRadians(operands[0]);
                        results.add(result);
                        break;
                    case RADTODEG:
                        result = toDegrees(operands[0]);
                        results.add(result);
                        break;
                    case FLOOR:
                        results.add(operands[0].setScale(0, RoundingMode.FLOOR));
                        break;
                    case ROUND:
                        results.add(operands[0].setScale(0, RoundingMode.HALF_UP));
                        break;
                    case CEIL:
                        results.add(operands[0].setScale(0, RoundingMode.CEILING));
                        break;
                    case CIRCLE_SURFACE:
                        if (operands[0].compareTo(BigDecimal.ZERO) < 0) {
                            error = getResources().getString(R.string.negativeRadius);
                        } else {
                            result = operands[0].pow(2).multiply(BIG_PI, getGoodContext(operands[0]));
                            results.add(result);
                        }
                        break;
                    default:
                        error = getResources().getString(R.string.operationNotImplemented);
                        break;
                }
            } else if (arity == 2) {
                switch (operation) {
                    case ADDITION:
                        results.add(operands[0].add(operands[1]));
                        break;
                    case MULTIPLICATION:
                        results.add(operands[0].multiply(operands[1]));
                        break;
                    case EXPONENTIATION:
                        //y^x; x is operands[1]; y is operands[0]
                        if (operands[0].compareTo(BigDecimal.ZERO) > 0) {
                            results.add(new BigDecimal(Math.pow(operands[0].doubleValue(), operands[1].doubleValue()), getGoodContext(operands)));
                        } else {
                            try {
                                BigInteger exponent = operands[1].toBigIntegerExact();
                                BigDecimal result = new BigDecimal(Math.pow(operands[0].doubleValue(), exponent.doubleValue()));
                                results.add(result);
                            }catch(ArithmeticException e) {
                                error = getResources().getString(R.string.negativeBaseExponentiation);
                            }
                        }
                        break;
                    case SUBTRACTION:
                        results.add(operands[0].subtract(operands[1]));
                        break;
                    case DIVISION:
                        if (operands[1].compareTo(BigDecimal.ZERO) == 0) {
                            error = getResources().getString(R.string.divisionByZero);
                        } else {
                            results.add(operands[0].divide(operands[1], getGoodContext(operands)));
                        }
                        break;
                    case ROOTYX:
                        //x^(1/y); x is operands[1]; y is operands[0]
                        if (operands[1].compareTo(BigDecimal.ZERO) > 0) {
                            results.add(new BigDecimal(
                                    Math.pow(operands[1].doubleValue(), BigDecimal.ONE.divide(operands[0], DEFAULT_MATH_CONTEXT).doubleValue()),
                                    getGoodContext(operands)));
                        } else {
                            error = getResources().getString(R.string.negativeRadicand);
                        }
                        break;
                    case LOGYX:
                        //log(x) in base y; x is operands[1]; y is operands[0]
                        if (operands[0].compareTo(BigDecimal.ZERO) <= 0
                                || operands[1].compareTo(new BigDecimal("0.0")) <= 0) {
                            error = getResources().getString(R.string.logOutOfRange);
                        } else {
                            results.add(new BigDecimal(Math.log(operands[1].doubleValue())).
                                    divide(new BigDecimal(Math.log(operands[0].doubleValue())), getGoodContext(operands)));
                        }
                        break;
                    case HYPOTENUSE_PYTHAGORAS:
                        if (operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO) < 0) {
                            error = getResources().getString(R.string.sideCantBeNegative);
                        } else {
                            results.add(operands[0]);
                            results.add(operands[1]);
                            results.add(new BigDecimal(
                                    Math.hypot(operands[0].doubleValue(), operands[1].doubleValue()),
                                    getGoodContext(operands)));
                        }
                        break;
                    case LEG_PYTHAGORAS:
                        if (operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO) < 0) {
                            error = getResources().getString(R.string.sideCantBeNegative);
                        } else {
                            BigDecimal hyp = operands[0].max(operands[1]);
                            BigDecimal leg = operands[0].min(operands[1]);
                            results.add(operands[0]);
                            results.add(operands[1]);
                            results.add(new BigDecimal(
                                    Math.sqrt(hyp.pow(2).subtract(leg.pow(2)).doubleValue()),
                                    getGoodContext(operands)));
                        }
                        break;
                    default:
                        error = getResources().getString(R.string.operationNotImplemented);
                        break;
                }
            } else if (arity == 3) {
                switch (operation) {
                    case TRIANGLE_SURFACE:
                        BigDecimal p = operands[0].add(operands[1]).add(operands[2]).divide(new BigDecimal(2), DEFAULT_MATH_CONTEXT);
                        BigDecimal q = p.multiply(p.subtract(operands[0]))
                                .multiply(p.subtract(operands[1]))
                                .multiply(p.subtract(operands[2]));
                        if (q.compareTo(BigDecimal.ZERO) < 0) {
                            error = getResources().getString(R.string.notATriangle);
                        } else {
                            results.add(new BigDecimal(Math.sqrt(q.doubleValue()), getGoodContext(operands)));
                        }
                        break;
                    case QUARATIC_EQUATION:
                        //0=ax2+bx+c, a==z==op[0]; b==y==op[1]; c==x==op[2]
                        BigDecimal a = operands[0];
                        BigDecimal b = operands[1];
                        BigDecimal c = operands[2];
                        BigDecimal root = new BigDecimal(Math.sqrt(b.pow(2).subtract(a.multiply(c).multiply(new BigDecimal(4))).doubleValue()));
                        results.add(root.subtract(b).divide(a.multiply(new BigDecimal(2)), DEFAULT_MATH_CONTEXT));
                        results.add(root.negate().subtract(b).divide(a.multiply(new BigDecimal(2)), DEFAULT_MATH_CONTEXT));
                        break;
                    default:
                        error = getResources().getString(R.string.operationNotImplemented);
                        break;
                }
            } else {
                error = getResources().getString(R.string.operationNotImplemented);
            }

            if (error != null) {
                showError(error);
                addNumbers(operands);
            } else {
                for (BigDecimal operand : operands) {
                    simpleChange.addOld(operand);
                }
                simpleChange.redoNumbersSize = results.size();
                historySaver.saveSimpleChange(simpleChange);
                addNumbers(results);
            }
        }
    }

    private static BigInteger factorial(BigInteger operand) {
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

    /** This is used to show 4.499999 as 4.500000, while internally it's still stored as 4.499999
     * @param number
     * @return
     */
    public static String toString(BigDecimal number) {
        int precision = number.precision();
        if (precision >= GOOD_PRECISION) {
            precision--;
        }
        return String.format(number.round(new MathContext(precision, ROUNDING_MODE)).toString());
    }

    String localizeDecimalSeparator(String str) {
        StringBuilder localized = new StringBuilder(str);
        char decimalSeparator = getResources().getString(R.string.decimalSeparator).charAt(0);
        int position = str.indexOf('.');
        localized.setCharAt(position, decimalSeparator);
        return localized.toString();
    }

    private void switchAngleMode() {
        if (angleMode == AngleMode.DEGREE) {
            angleMode = AngleMode.RADIAN;
        }else{
            angleMode = AngleMode.DEGREE;
        }
        showAngleMode();
    }

    void showAngleMode() {
        tvAngleMode.setText(angleMode == AngleMode.DEGREE ? "D\nE\nG" : "R\nA\nD");
    }

    void showError(String str) {
        tvError.setText(getResources().getString(R.string.error) + ": " + str);
        scrollError.setVisibility(View.VISIBLE);
        scrollEditableNumber.setVisibility(GONE);
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

    static boolean lastCharIs(String s, char c) {
        if (s.length() == 0) {
            return false;
        } else {
            return s.charAt(s.length() - 1) == c;
        }
    }

    static ArrayList<String> buildStringArrayListNumbers(List<BigDecimal> numbers) {
        ArrayList<String> stringNumbers = new ArrayList<String>();
        for (BigDecimal num : numbers) {
            stringNumbers.add(removeZeros(num.toString(), false, "."));
        }
        return stringNumbers;
    }

}
//From most important to least:

//TODO release at this point. Solve above TODOs, think of below ones

//TODO localize decimal separator: localize editableNumber & stack numbers
//TODO highlight reordered numbers for a little while
//TODO add some background to editableNumber
//TODO fix icon fn- to exactly match fn+
//TODO tvError gone and visible makes DEG move up and down, slightly
//TODO add litle help (dragNdrop, click on number, &c)
//TODO make standard deviation with mean or something. Test estadísticos, quizás Q de dixon, etc.
//TODO replace all sqrt with custom BigSqrt
