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

import java.math.BigDecimal;
import java.math.MathContext;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.terlici.dragndroplist.DragNDropListView;

import static android.view.View.GONE;

public class ActivityMain extends Activity implements ViewTreeObserver.OnScrollChangedListener {
    final static String TAG = ActivityMain.class.getSimpleName();

    final static BigDecimal BIG_PI = new BigDecimal(Math.PI);
    final static BigDecimal BIG_EULER = new BigDecimal(Math.E);
    final static BigDecimal BIG_PHI = new BigDecimal("1.618033988749894");

    final static int ARITY_ALL = -1;// takes all numbers in stack
    final static int ARITY_ZERO_ONE = -2;// takes one number if it's being written by user
    final static int ARITY_N = -3;// takes N+1 numbers, let N be the first number in the stack

    final static String DEGREE_MODE_PARAM = "AngleMode";
    final static String NUMBER_STACK_PARAM = "NumberStack";
    final static String HISTORY_SAVER_PARAM = "HistorySaver";
    final static String SWITCHER_INDEX_PARAM = "SwitcherIndex";
    final static String EDITABLE_NUMBER_PARAM = "EditableNumber";

    static final boolean MERGE_DECIMAL = true;
    static final boolean MERGE_DIGITS = true;
    static final boolean MERGE_MINUS = true;
    static final boolean MERGE_E = true;


    enum UpdateStackFlag {
        KEEP_PREVIOUS, REMOVE_PREVIOUS
    }


    private class ListViewScroller implements Runnable {
        ListView view;

        ListViewScroller(ListView view) {
            this.view = view;
        }

        public void run() {
            view.smoothScrollToPosition(numberStack.size() - 1);
        }
    }

    private DataSetObserver numberStackObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            layoutNumbersDraggable.post(new ListViewScroller(layoutNumbersDraggable));
        }
    };

    private class ViewedString {
        private static final int NUMBER_BUILDER_INITIAL_CAPACITY = 10;
        private StringBuilder text = new StringBuilder(NUMBER_BUILDER_INITIAL_CAPACITY);

        void updateView() {
            char separator = getString(R.string.decimalSeparator).charAt(0);
            tvEditableNumber.setText(text.toString().replace('.', separator));
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

        void pop() {
            text.deleteCharAt(text.length() - 1);
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

    private class StackAnimator implements ViewTreeObserver.OnGlobalLayoutListener {
        Set<Integer> toAnimateViews = new HashSet<>();
        ValueAnimator itemBackgroundAnimator;

        StackAnimator(){
            if (Build.VERSION.SDK_INT >= 11) {
                toAnimateViews = new HashSet<>(0);
                itemBackgroundAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                        getResources().getColor(R.color.dropped_background_color),
                        getResources().getColor(R.color.stack_number_background));
                itemBackgroundAnimator.setDuration(getResources().getInteger(R.integer.dropped_background_duration));
            }
        }

        void animate(int position){
            if (Build.VERSION.SDK_INT >= 11) {
                toAnimateViews.add(position);
                itemBackgroundAnimator.end();
                itemBackgroundAnimator.removeAllUpdateListeners();
            }
        }

        @Override
        public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < 11) {
                return;
            }
            for(int i = 0, size = layoutNumbersDraggable.getChildCount(); i < size; i++){
                View view = layoutNumbersDraggable.getChildAt(i);
                int position = ((NumberStackDraggableAdapter.ViewHolder) view.getTag()).position;
                if (toAnimateViews.contains(position)) {
                    itemBackgroundAnimator.addUpdateListener(new BackgroundColorSetter(view));
                    toAnimateViews.remove(position);
                    if (toAnimateViews.isEmpty()) {
                        itemBackgroundAnimator.start();
                    }
                }
            }
        }
    }

    private class OperationData {
        int arity;
        Integer[] btnIds;

        OperationData(int arity, Integer... btnIds) {
            this.arity = arity;
            this.btnIds = btnIds;
        }
    }

    private class ThreadedOperation implements Callable<OperationBundle>{
        private final OperationBundle bundle;
        private final Operation operation;
        private final int arity;
        private final int numberOfArguments;

        ThreadedOperation(OperationBundle results, Operation operation, int arity, int numberOfArguments){
            this.bundle = new OperationBundle(results.operands);
            this.operation = operation;
            this.arity = arity;
            this.numberOfArguments = numberOfArguments;
        }

        Future<OperationBundle> start(){
            return threadPool.submit(this);
        }

        @Override
        public OperationBundle call() {
            try {
                if (arity == ARITY_ALL) {
                    switch (operation) {
                        case SUMMATION:         calc.summation(bundle);break;
                        case MEAN:              calc.mean(bundle);break;
                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED;break;
                    }
                } else if (arity == ARITY_ZERO_ONE) {
                    switch (operation){
                        case CONSTANTPI:        calc.pi(bundle); break;
                        case CONSTANTPHI:       calc.phi(bundle); break;
                        case CONSTANTEULER:     calc.euler(bundle); break;
                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED; break;
                    }
                } else if (arity == ARITY_N) {
                    try {
                        long nLong = bundle.operands[0].longValueExact();
                        if (nLong <= Integer.MAX_VALUE) {
                            int userNumberOperands = (int) nLong;
                            if (numberStack.size() < userNumberOperands) {
                                bundle.error = CalculatorError.NOT_ENOUGH_ARGS;
                            } else {
                                //pop N more results.operands
                                ArrayList<BigDecimal> fullOperands = new ArrayList<>(userNumberOperands + 1);
                                fullOperands.add(bundle.operands[0]);
                                if(!Thread.currentThread().isInterrupted()){
                                    //TODO this doesn't really protects from a weird race condition where the task can
                                    //be cancelled in this right moment (after if()) and it'll still pop
                                    //numbers from the stack. The main thread is assuming that task.cancel()
                                    //prevents the stack to be modified here, but here it's doing exactly that.
                                    //
                                    //The race condition can only happen if the code before this line is so slow
                                    //that it can't complete before the task times out.
                                    // That means one whole second when I wrote this comment.
                                    fullOperands.addAll(Arrays.asList(popNumbers(userNumberOperands)));
                                    bundle.operands = fullOperands.toArray(new BigDecimal[userNumberOperands + 1]);
                                    switch (operation) {
                                        case SUMMATION_N:   calc.summationN(bundle);break;
                                        case MEAN_N:        calc.meanN(bundle);break;
                                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED;break;
                                    }
                                }else{
                                    //noop; The result won't be used, there's no need to set the error
                                }
                            }
                        } else {
                            bundle.error = CalculatorError.TOO_MANY_ARGS;
                        }
                    }catch(ArithmeticException e) {
                        bundle.error = CalculatorError.LAST_NOT_INT;
                    }
                } else if (arity == 0) {
                    switch (operation) {
                        case RANDOM: calc.random(bundle);break;
                        default: bundle.error = CalculatorError.NOT_IMPLEMENTED; break;
                    }
                } else if (arity == 1) {
                    switch (operation) {
                        case INVERSION:             calc.inversion(bundle); break;
                        case SQUARE:                calc.square(bundle); break;
                        case NEGATIVE:              calc.negative(bundle); break;
                        case SQUAREROOT:            calc.squareRoot(bundle); break;
                        case SINE:                  calc.sine(bundle); break;
                        case COSINE:                calc.cosine(bundle); break;
                        case TANGENT:               calc.tangent(bundle); break;
                        case ARCSINE:               calc.arcsine(bundle); break;
                        case ARCCOSINE:             calc.arccosine(bundle); break;
                        case ARCTANGENT:            calc.arctangent(bundle); break;
                        case SINE_H:                calc.sineH(bundle); break;
                        case COSINE_H:              calc.cosineH(bundle); break;
                        case TANGENT_H:             calc.tangentH(bundle); break;
                        case LOG10:                 calc.log10(bundle); break;
                        case LOGN:                  calc.logN(bundle); break;
                        case EXPONENTIAL:           calc.exponential(bundle); break;
                        case FACTORIAL:             calc.factorial(bundle); break;
                        case DEGTORAD:              calc.deg2rad(bundle); break;
                        case RADTODEG:              calc.rad2deg(bundle); break;
                        case FLOOR:                 calc.floor(bundle); break;
                        case ROUND:                 calc.round(bundle); break;
                        case CEIL:                  calc.ceil(bundle); break;
                        case CIRCLE_SURFACE:        calc.circleSurface(bundle); break;
                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED; break;
                    }
                } else if (arity == 2) {
                    switch (operation) {
                        case ADDITION:              calc.addition(bundle);break;
                        case MULTIPLICATION:        calc.multiplication(bundle);break;
                        case EXPONENTIATION:        calc.exponentiation(bundle);break;
                        case SUBTRACTION:           calc.subtraction(bundle);break;
                        case DIVISION:              calc.joyDivision(bundle);break;
                        case ROOTYX:                calc.rootYX(bundle);break;
                        case LOGYX:                 calc.logYX(bundle);break;
                        case HYPOTENUSE_PYTHAGORAS: calc.hypotenusePythagoras(bundle);break;
                        case LEG_PYTHAGORAS:        calc.legPythagoras(bundle);break;
                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED;break;
                    }
                } else if (arity == 3) {
                    switch (operation) {
                        case TRIANGLE_SURFACE:      calc.triangleSurface(bundle);break;
                        case QUARATIC_EQUATION:     calc.quadraticEquation(bundle);break;
                        default:    bundle.error = CalculatorError.NOT_IMPLEMENTED;break;
                    }
                } else {
                    bundle.error = CalculatorError.NOT_IMPLEMENTED;
                }
            }catch(ArithmeticException e) {
                bundle.error = CalculatorError.NOT_IMPLEMENTED;
            }

            return bundle;
        }
    }

    private abstract class MyOnClickListener implements OnClickListener {
        @Override
        final public void onClick(View v) {
            if (!ActivityMain.this.layoutNumbersDraggable.isDragging()) {
                myOnClick(v);
            }
        }

        abstract void myOnClick(View v);
    }

    MyOnClickListener clickListenerDigitButtons = new MyOnClickListener() {
        @Override
        public void myOnClick(View view) {
            int id = view.getId();
            for (int i = 0; i < 10; ++i) {
                if (id == digitButtonsIds[i]) {
                    clickedDigit(i, true);
                    break;
                }
            }
        }
    };

    MyOnClickListener clickListenerOperations = new MyOnClickListener() {
        @Override
        public void myOnClick(View view) {
            Operation operation = getOperationFromBtnId(view.getId());
            if (operation != null) {
                clickedOperation(operation);
            } else {
                Log.d(TAG, "Some button is not linked to any Operation");
            }
        }
    };


    private ValueAnimator errorBackgroundAnimator;

    private LinkedList<BigDecimal> numberStack = new LinkedList<>();
    private Calculator calc = new Calculator();

    private ViewedString editableNumber = new ViewedString();
    private boolean deleteCharBeforeDecimalSeparator = false;
    private boolean deleteCharBeforeScientificNotation = false;


    private Integer[] digitButtonsIds;
    private HistorySaver historySaver;

    private DragNDropListView layoutNumbersDraggable;
    private ImageView arrowUp, arrowDown;
    private HorizontalScrollView scrollEditableNumber, scrollError;
    private TextView tvEditableNumber, tvError, tvAngleMode;
    private ViewAnimator switcherFunctions;

    private NumberStackDraggableAdapter numberStackDraggableAdapter;

    private StackAnimator stackAnimator;

    private HashMap<Operation, OperationData> operationsData;

    MyThreadPoolExecutor threadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        switcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);

        scrollEditableNumber = (HorizontalScrollView) findViewById(R.id.scrollEditableNumber);
        scrollError = (HorizontalScrollView) findViewById(R.id.scrollError);
        scrollError.setVisibility(GONE);
        layoutNumbersDraggable = (DragNDropListView) findViewById(R.id.listNumbersDraggable);
        tvAngleMode = (TextView) findViewById(R.id.tvAngleMode);
        tvEditableNumber = (TextView) findViewById(R.id.tvEditableNumber);
        tvError = (TextView) findViewById(R.id.tvError);

        tvEditableNumber.setGravity(Gravity.END | Gravity.CENTER);
        tvEditableNumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentParse = new Intent(ActivityMain.this, ActivityParser.class);
                intentParse.putExtra("Input", editableNumber.toString());
                startActivityForResult(intentParse, 1);
            }
        });

        calc.setAngleMode(AngleMode.DEGREE);
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


        ((ImageButton) findViewById(R.id.butUndo)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedUndo();
            }
        });
        ((ImageButton) findViewById(R.id.butRedo)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedRedo();
            }
        });

        ((Button) findViewById(R.id.butDecimal)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedDecimal(true);
            }
        });
        ((ImageButton) findViewById(R.id.butPopNumber)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedPopNumber(true);
            }
        });
        ((ImageButton) findViewById(R.id.butClear)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedClear(true);
            }
        });
        ((ImageButton) findViewById(R.id.butDelete)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedDelete(true);
            }
        });
        ((ImageButton) findViewById(R.id.butEnterNumber)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedEnter(true);
            }
        });
        ((ImageButton) findViewById(R.id.scientificNotation)).setOnClickListener(new MyOnClickListener() {
            @Override
            public void myOnClick(View view) {
                clickedScientificNotation(true);
            }
        });
        ((Button) findViewById(R.id.minus)).setOnClickListener(new MyOnClickListener() {
            @Override
            public void myOnClick(View view) {
                clickedMinus(true);
            }
        });
        ((ImageButton) findViewById(R.id.butDegRad)).setOnClickListener(new MyOnClickListener() {
            @Override
            public void myOnClick(View view) {
                switchAngleMode();
            }
        });


        ((ImageButton) findViewById(R.id.butFunctionsMore)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                switcherFunctions.showNext();
            }
        });
        ((ImageButton) findViewById(R.id.butFunctionsLess)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                switcherFunctions.showNext();
            }
        });
        ((ImageButton) findViewById(R.id.butSwitch)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
                clickedSwap(true, -1, -1);
            }
        });
        ((ImageButton) findViewById(R.id.butSwitch2)).setOnClickListener(new MyOnClickListener() {
            public void myOnClick(View view) {
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

        numberStackDraggableAdapter = new NumberStackDraggableAdapter(this);
        numberStackDraggableAdapter.registerDataSetObserver(numberStackObserver);
        layoutNumbersDraggable.setDragNDropAdapter(numberStackDraggableAdapter);

        layoutNumbersDraggable.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long itemId) {
                BigDecimal number = (BigDecimal) adapter.getItemAtPosition(position);
                ActivityMain.this.clickedStackNumber(true, number);
                stackAnimator.animate(position);
                stackAnimator.animate(numberStack.size() - 1);
            }
        });

        arrowUp = (ImageView) findViewById(R.id.arrow_up);
        arrowDown = (ImageView) findViewById(R.id.arrow_down);
        layoutNumbersDraggable.getViewTreeObserver().addOnScrollChangedListener(this);
        stackAnimator = new StackAnimator();
        layoutNumbersDraggable.getViewTreeObserver().addOnGlobalLayoutListener(stackAnimator);
        layoutNumbersDraggable.post(new Runnable() {
            @Override
            public void run() {
                updateArrowVisibility();
            }
        });

        if (Build.VERSION.SDK_INT >= 11) {
            errorBackgroundAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    getResources().getColor(R.color.new_error_background_color),
                    getResources().getColor(R.color.editable_background_color));
            errorBackgroundAnimator.setDuration(getResources().getInteger(R.integer.dropped_background_duration));
            errorBackgroundAnimator.addUpdateListener(new BackgroundColorSetter(findViewById(R.id.parentDigits)));
        }

        final int corePoolSize = Integer.MAX_VALUE;
        final int maximumPoolSize = Integer.MAX_VALUE;
        final long keepAliveTimeS = 60L;
        final TimeUnit keepAliveUnit = TimeUnit.SECONDS;
        final int maxPoolQueueSize = 10;
        threadPool = new MyThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveTimeS, keepAliveUnit,
                new LinkedBlockingQueue<Runnable>(maxPoolQueueSize));

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                enteredParsedNumber(data.getStringExtra("Result"), true);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "Activity parse was canceled");
            }
        }
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
        saver.putBoolean(DEGREE_MODE_PARAM, calc.getAngleMode() == AngleMode.DEGREE);
        saver.putSerializable(HISTORY_SAVER_PARAM, historySaver);
        saver.putInt(SWITCHER_INDEX_PARAM, switcherFunctions.getDisplayedChild());
        super.onSaveInstanceState(saver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disableKiller();
    }

    @Override
    protected void onStop() {
        if(threadPool.getActiveCount() > 0){
            enableKiller();
        }
        super.onStop();
    }

    @Override
    public void onScrollChanged() {
        layoutNumbersDraggable.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateArrowVisibility();
            }
        }, 280);
    }


    private void enableKiller(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(alarmManager != null) {
            Intent intent = new Intent(this, CleanerService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1000 * 10, pendingIntent);
            Log.d(TAG, "Schedule an alarm to kill app & zombie threads in 10s");
        }
    }

    private void disableKiller(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, CleanerService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Disabled any scheduled cleaner/killer");
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
        calc.setAngleMode(saved.getBoolean(DEGREE_MODE_PARAM) ? AngleMode.DEGREE : AngleMode.RADIAN);
        showAngleMode();

        historySaver = (HistorySaver) saved.getSerializable(HISTORY_SAVER_PARAM);
        historySaver.setActivity(this);
        int indexFunctions = saved.getInt(SWITCHER_INDEX_PARAM);
        for (int i = 0; i < indexFunctions; ++i) {
            switcherFunctions.showNext();
        }
    }

    private Collection<BigDecimal> clearStack() {
        Collection<BigDecimal> cleared = numberStack;
        numberStack = new LinkedList<>();
        numberStackDraggableAdapter.notifyDataSetChanged();
        return cleared;
    }

    private void addNumber(BigDecimal number) {
        numberStack.add(number);
        numberStackDraggableAdapter.notifyDataSetChanged();
    }

    private void addNumbers(BigDecimal[] numbers) {
        addNumbers(Arrays.asList(numbers));
    }

    public void addNumbers(Collection<BigDecimal> numbers) {
        numberStack.addAll(numbers);
        numberStackDraggableAdapter.notifyDataSetChanged();
    }

    private BigDecimal popLastNumber() {
        try {
            BigDecimal number = numberStack.removeLast();
            numberStackDraggableAdapter.notifyDataSetChanged();
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

    public BigDecimal[] popNumbers(int amount) {
        BigDecimal[] numbers = new BigDecimal[amount];
        for (int i = amount - 1; i >= 0; i--) {
            numbers[i] = numberStack.removeLast();
        }
        numberStackDraggableAdapter.notifyDataSetChanged();
        return numbers;
    }

    public int getNumberStackSize(){
        return numberStack.size();
    }

    public BigDecimal getNumberStackAt(int pos){
        return numberStack.get(pos);
    }

    /**
     * build a BigDecimal from editableNumber and insert it if possible
     *
     * @param flag indicates whether it must remove the last number in the stack before
     *             inserting the new one
     * @return true if a new number has been built and inserted in the stack
     */
    private boolean addEditableToStack(UpdateStackFlag flag) throws NumberFormatException {
        BigDecimal removed = null;
        if (flag == UpdateStackFlag.REMOVE_PREVIOUS) {
            removed = numberStack.removeLast();
        }

        if (editableNumber.length() > 0) {
            String candidateString = editableNumber.toString();
            while (lastCharIs(candidateString, 'E') ||
                    lastCharIs(candidateString, '.') ||
                    lastCharIs(candidateString, '-')) {
                candidateString = candidateString.substring(0, candidateString.length() - 1);
            }

            if (candidateString.length() > 0) {
                try {
                    BigDecimal newNumber = new BigDecimal(candidateString);
                    numberStack.add(newNumber);
                    numberStackDraggableAdapter.notifyDataSetChanged();
                    return true;
                }catch(NumberFormatException e) {
                    if (removed != null) {
                        numberStack.add(removed);
                        numberStackDraggableAdapter.notifyDataSetChanged();
                    }
                    throw e;
                }
            }
        }
        numberStackDraggableAdapter.notifyDataSetChanged();
        return false;
    }

    private void clickedUndo() {
        historySaver.goBack();
    }

    private void clickedRedo() {
        historySaver.goForward();
    }

    private boolean isEditableNumberInStack() {
        return editableNumber.length() > 0 && !editableNumber.toString().equals("-");
        //Any other string (0. 0.2 1E 2.3E) must be in stack, but not "-"
    }

    private void enteredParsedNumber(String text, boolean save){
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        simpleChange.setTag("Parsed: " + text);

        boolean removePrevious = isEditableNumberInStack();
        if (save && removePrevious) {
            simpleChange.addOld(getLastNumber());
        }
        editableNumber.set(text);

        try {
            boolean addedNumber = addEditableToStack(removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS);
            if (save) {
                if (addedNumber) {
                    simpleChange.setRedoNumbersSize(1);
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }catch(NumberFormatException e) {
            editableNumber.pop();
        }
    }

    private void clickedDigit(int digit, boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString(), MERGE_DIGITS);
        simpleChange.setTag("Digit: " + digit);

        boolean removePrevious = isEditableNumberInStack();
        if (save && removePrevious) {
            simpleChange.addOld(getLastNumber());
        }
        editableNumber.append(String.valueOf(digit));

        try {
            boolean addedNumber = addEditableToStack(removePrevious ? UpdateStackFlag.REMOVE_PREVIOUS : UpdateStackFlag.KEEP_PREVIOUS);
            if (save) {
                if (addedNumber) {
                    simpleChange.setRedoNumbersSize(1);
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }catch(NumberFormatException e) {
            editableNumber.pop();
        }
    }

    private void clickedDecimal(boolean save) {
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
                    simpleChange.setRedoNumbersSize(1);
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    private void clickedScientificNotation(boolean save) {
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
                    simpleChange.setRedoNumbersSize(1);
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    private void clickedMinus(boolean save) {
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
                int plusPosition = editableNumber.lastIndexOf("+");
                if (minusPosition == scientificPosition + 1) {
                    //"1E-5" -> "1E5"
                    editableNumber.deleteCharAt(minusPosition);
                } else if (plusPosition == scientificPosition + 1) {
                    //"1E+5" -> "1E-5"
                    editableNumber.deleteCharAt(scientificPosition + 1);
                    editableNumber.insert(scientificPosition + 1, "-");

                }else{
                    //"1E5" -> "1E-5"
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
                simpleChange.setRedoNumbersSize(1);
            }
            historySaver.saveSimpleChange(simpleChange);
        }

    }

    private void clickedDelete(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            scrollEditableNumber.setVisibility(View.VISIBLE);
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
            editableNumber.pop();
            if (deletingChar == 'E' && deleteCharBeforeScientificNotation
                    || deletingChar == '.' && deleteCharBeforeDecimalSeparator) {
                editableNumber.pop();
            }

            if (lastCharIs(editableNumber.toString(), '+')) {
                editableNumber.pop();
            }

            boolean addedNumber = addEditableToStack(flagRemove);
            if (save) {
                if (addedNumber) {
                    simpleChange.setRedoNumbersSize(1);
                }
                historySaver.saveSimpleChange(simpleChange);
            }
        }
    }

    private void clickedEnter(boolean save) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        simpleChange.setTag("Enter: " + editableNumber.toString());

        if (editableNumber.length() == 0) {
            // "" --> use stack instead of editableNumber, if possible
            if (numberStack.size() != 0) {
                BigDecimal number = getLastNumber();
                addNumber(number);
                stackAnimator.animate(numberStack.size() - 1);
                stackAnimator.animate(numberStack.size() - 2);
                if (save) {
                    simpleChange.setRedoNumbersSize(1);
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

    private void clickedPopNumber(boolean save) {
        if (scrollError.getVisibility() == View.VISIBLE) {
            scrollError.setVisibility(GONE);
            scrollEditableNumber.setVisibility(View.VISIBLE);
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

    private void clickedClear(boolean save) {
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

    private void clickedStackNumber(boolean save, BigDecimal number) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());
        simpleChange.setTag("StackNumber: " + number.toString());

        editableNumber.set(number.toString());
        addEditableToStack(UpdateStackFlag.KEEP_PREVIOUS);
        if (save) {
            simpleChange.setRedoNumbersSize(1);
            historySaver.saveSimpleChange(simpleChange);
        }
    }

    public void clickedSwap(boolean save, int startPosition, int endPosition) {
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
        stackAnimator.animate(startPosition);
        stackAnimator.animate(endPosition);

        numberStackDraggableAdapter.notifyDataSetChanged();
    }

    private void clickedOperation(Operation operation) {
        SimpleChange simpleChange = new SimpleChange(historySaver, editableNumber.toString());

        final int arity = operationsData.get(operation).arity,
                  availableArguments = numberStack.size();
        int numberOfArguments;
        boolean enoughArguments,
                editableInStack = isEditableNumberInStack();

        editableNumber.reset();

        if (arity == ARITY_ALL) {
            numberOfArguments = availableArguments;
            enoughArguments = availableArguments != 0;
        } else if (arity == ARITY_ZERO_ONE) {
            if (editableInStack) {
                numberOfArguments = 1;
            } else {
                numberOfArguments = 0;
            }
            enoughArguments = availableArguments >= numberOfArguments;
        } else if (arity == ARITY_N) {
            numberOfArguments = 1;
            enoughArguments = availableArguments >= numberOfArguments;
        } else {
            numberOfArguments = arity;
            enoughArguments = availableArguments >= numberOfArguments;
        }

        if (!enoughArguments) {
            showError(getResources().getString(R.string.notEnoughArguments));
        } else {
            BigDecimal[] operands = popNumbers(numberOfArguments);

            //The operation could be slow with big numbers, so I'm running it in a thread
            // 1. The timeout is hardcoded
            // 2. the variables in & out are all mixed
            // 3. the code is pretty much unclear
            // 4. a rare race condition is theorically detected and unfixed
            // 5. Too long operations are abandoned in a zombie thread, wasting CPU & battery
            // 5.a Well, when the app goes to background, an alarm kills the app to free resources
            // TODO: Fix that race condition

            OperationBundle operationBundle = new OperationBundle(operands);
            ThreadedOperation operationTask = new ThreadedOperation(operationBundle, operation, arity, numberOfArguments);
            Future<OperationBundle> futureResult = operationTask.start();

            String error = null;
            try {
                operationBundle = futureResult.get(1, TimeUnit.SECONDS);
                if(operationBundle.error != null) {
                    error = getString(CalculatorError.getRStringId(operationBundle.error));
                }
            }catch(InterruptedException | ExecutionException | TimeoutException e) {
                futureResult.cancel(true);
                error = getString(R.string.longOperation);
                Log.d(TAG, String.format("Operation too long. %d zombie threads will be killed when app goes to background", threadPool.getActiveCount()));
            }

            operands = operationBundle.operands;
            List<BigDecimal> results = operationBundle.results;

            if (error != null) {
                showError(error);
                addNumbers(operands);
            } else {
                for (BigDecimal operand : operands) {
                    simpleChange.addOld(operand);
                }
                simpleChange.setRedoNumbersSize(results.size());
                historySaver.saveSimpleChange(simpleChange);
                addNumbers(results);

                for(int i = 0; i < results.size(); i++) {
                    stackAnimator.animate(numberStack.size() - 1 - i);
                }
            }
        }
    }

    /**
     * This is used to show 4.499999 as 4.500000, while internally it's still stored as 4.499999
     *
     * @param number
     * @return
     */
    public String asString(BigDecimal number) {
        int precision = number.precision();
        if (precision >= Calculator.GOOD_PRECISION) {
            precision--;
        }
        return localizeDecimalSeparator(number.round(new MathContext(precision, Calculator.ROUNDING_MODE)).toString());
    }

    String localizeDecimalSeparator(String str) {
        char decimalSeparator = getResources().getString(R.string.decimalSeparator).charAt(0);
        return str.replace('.', decimalSeparator);
    }

    public void resetEditableNumber(){
        editableNumber.reset();
    }

    public void setEditableNumber(String number){
        editableNumber.set(number);
    }

    public String getEditableNumberStr(){
        return editableNumber.toString();
    }

    public BigDecimal numberStackRemove(int position) {
        return numberStack.remove(position);
    }

    public void numberStackAdd(int position, BigDecimal number) {
        numberStack.add(position, number);
    }

    public void notifyStackChanged(){
        numberStackDraggableAdapter.notifyDataSetChanged();
    }

    private Operation getOperationFromBtnId(int id) {
        for (Map.Entry<Operation, OperationData> dataEntry : operationsData.entrySet()) {
            if (Arrays.asList(dataEntry.getValue().btnIds).contains(id)) {
                return dataEntry.getKey();
            }
        }
        return null;
    }

    private void switchAngleMode() {
        if (calc.getAngleMode() == AngleMode.DEGREE) {
            calc.setAngleMode(AngleMode.RADIAN);
        } else {
            calc.setAngleMode(AngleMode.DEGREE);
        }
        showAngleMode();
    }

    private void showAngleMode() {
        tvAngleMode.setText(calc.getAngleMode() == AngleMode.DEGREE ? "D\nE\nG" : "R\nA\nD");
    }

    private void showError(String str) {
        tvError.setText(getResources().getString(R.string.error) + ": " + str);
        scrollError.setVisibility(View.VISIBLE);
        scrollEditableNumber.setVisibility(GONE);

        if (Build.VERSION.SDK_INT >= 11) {
            errorBackgroundAnimator.start();
        }
    }

    private void updateArrowVisibility() {
        // code taken from my fork of Anuto TD

        final int numberViews = layoutNumbersDraggable.getChildCount();
        if (numberViews <= 0) {
            arrowUp.setVisibility(View.INVISIBLE);
            arrowDown.setVisibility(View.INVISIBLE);
            return;
        }

        final int firstVisibleNumber = layoutNumbersDraggable.getFirstVisiblePosition();
        if (firstVisibleNumber == 0) {
            arrowUp.setVisibility(layoutNumbersDraggable.getChildAt(0).getTop() < -20 ? View.VISIBLE : View.INVISIBLE);
        } else {
            arrowUp.setVisibility(firstVisibleNumber > 0 ? View.VISIBLE : View.INVISIBLE);
        }

        final int stackSize = numberStack.size();
        final int lastVisibleNumber = layoutNumbersDraggable.getLastVisiblePosition();
        if (lastVisibleNumber == stackSize - 1) {
            int marginFix = (int) getResources().getDimension(R.dimen.arrowMarginFix);
            arrowDown.setVisibility(layoutNumbersDraggable.getChildAt(numberViews - 1).getBottom() > layoutNumbersDraggable.getHeight() + marginFix ? View.VISIBLE : View.INVISIBLE);
        } else {
            arrowDown.setVisibility(lastVisibleNumber < stackSize - 1 ? View.VISIBLE : View.INVISIBLE);
        }
    }


    private static ArrayList<String> buildStringArrayListNumbers(List<BigDecimal> numbers) {
        ArrayList<String> stringNumbers = new ArrayList<String>();
        for (BigDecimal num : numbers) {
            stringNumbers.add(removeZeros(num.toString(), false, "."));
        }
        return stringNumbers;
    }

    private static String removeZeros(String string, boolean keepTrailingZeros, String decimalSeparator) {
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

    private static boolean lastCharIs(String s, char c) {
        return s.length() != 0 && s.charAt(s.length() - 1) == c;
    }



}
//TODO catch NumberFormatException in every addEditableToStack call
//TODO 1000! & drag <- Fix the hack in DragNDropListView
//TODO add little help (dragNdrop, click on number, &c). Where?
//TODO long press in operation -> help
//TODO long press on error or editable - > copy
//TODO explore new layouts with editable above everything else. Stack listview would need to be reversed and scrolled to top
//TODO make standard deviation with mean or something. Test estadísticos, quizás Q de dixon, etc.
//TODO replace all sqrt with custom BigSqrt
//TODO tvAngleMode must leave. Make two DEG<->RAD drawables and switch between them. Or maybe it's just the tv for editable which is too small
//TODO custom button panel layout
