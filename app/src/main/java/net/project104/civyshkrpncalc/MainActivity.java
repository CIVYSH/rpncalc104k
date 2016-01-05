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
import java.math.RoundingMode;
import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

/*
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ViewSwitcher;
import android.util.Log;
import android.view.WindowManager;
import android.view.View.MeasureSpec;
*/

public class MainActivity extends Activity{
	final static double CONSTANT_PI = 3.141592653589793;
	final static double CONSTANT_E = 2.718281828459045;
	final static RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
	final static int DEFAULT_SCALE = 9;

	static class Scroller implements Runnable{
		HorizontalScrollView view;
		Scroller(HorizontalScrollView view){
			this.view = view;
		}
		public void run(){
			view.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
		}
	}

	LinkedList <BigDecimal> mListNumbers;
	List <Integer> mListDigits;
	int mPosDecimal = -1;
	String mAngleMode;
	boolean mUsingViewAnimator;
	int[] mDigitButtonsIds;
	HistorySaver mHistorySaver;

	ViewAnimator mSwitcherFunctions;
	HorizontalScrollView mScrollNumbers, mScrollDigits, mScrollError;
	LinearLayout mLayoutNumbers;
	TextView mTvDigits, mTvError, mTvWritingDigits, mTvAngleMode;
	Menu mOptionsMenu;

	enum Operation{
		ADDITION, MULTIPLICATION, EXPONENTIATION,
		SUBTRACTION, DIVISION, INVERSION, SQUARE, 
		SQUAREROOT, NEGATIVE, LOG10, LOGXY,
		SINE, COSINE, TANGENT, 
		ARCSINE, ARCCOSINE, ARCTANGENT,
		DTOR, RTOD, SUMMATION, MEAN,
		TRIANGLE_SURFACE, HYPOTENUSE_PYTHAGORAS, LEG_PYTHAGORAS, CIRCLE_SURFACE, 
		CONSTANTPI, CONSTANTE
	}
	enum DeleteType {TEXT, SEPARATOR, DIGIT};

	static class HistorySaver implements Serializable {
		transient WeakReference<MainActivity> activity;
		private List <Step> listSteps;
		int currentStep;
		HistorySaver(MainActivity activity){
			this.activity = new WeakReference<MainActivity>(activity);
			listSteps = new LinkedList <Step>();
			currentStep = 0;
		}
		HistorySaver(MainActivity activity, HistorySaver old){
			this.activity = new WeakReference<MainActivity>(activity);
			currentStep = old.currentStep;
			listSteps = new LinkedList <Step>();
			for(Step oldStep : old.listSteps){

				if(oldStep instanceof StepOperation){
					listSteps.add(new StepOperation(activity, (StepOperation) oldStep));
				}else if(oldStep instanceof StepEnter){
					listSteps.add(new StepEnter(activity, (StepEnter) oldStep));
				}else if(oldStep instanceof StepDigits){
					listSteps.add(new StepDigits(activity, (StepDigits) oldStep));
				}else if(oldStep instanceof StepDelete){
					listSteps.add(new StepDelete(activity, (StepDelete) oldStep));
				}else if(oldStep instanceof StepClear){
					listSteps.add(new StepClear(activity, (StepClear) oldStep));
				}else if(oldStep instanceof StepPop){
					listSteps.add(new StepPop(activity, (StepPop) oldStep));
				}else if(oldStep instanceof StepSwitch){
					listSteps.add(new StepSwitch(activity));
				}
			}
		}
		private void addStep(Step step){
			int size = listSteps.size();
			if(currentStep == size){
				listSteps.add(step);
				++currentStep;
			}else{
				listSteps.set(currentStep, step);
				++currentStep;
				for(int i = currentStep; i < size; ++i){
					listSteps.remove(currentStep);
				}
			}
		}
		private Step getPreviousStep(){
			if(listSteps.isEmpty()){
				return null;
			}else if(currentStep < 1){
				return null;
			}else{
				return listSteps.get(currentStep - 1);
			}
		}
		void saveOperation(int arity, BigDecimal[] operands, String textDigits, BigDecimal result){
			Step step = new StepOperation(activity.get(), arity, operands, textDigits, result);
			addStep(step);
		}
		void saveEnter(List <Integer> savedDigits, int savedPos, String textDigits, BigDecimal number,
				boolean reEnterNumber){
			Step step = new StepEnter(activity.get(), savedDigits, savedPos, textDigits, number, reEnterNumber);
			addStep(step);
		}
		void saveDigits(){
			if(getPreviousStep() instanceof StepDigits){
				// nothing;
			}else{
				Step step = new StepDigits(activity.get());
				addStep(step);
			}
		}
		void saveDelete(String savedText){ 
			Step step = new StepDelete(savedText);
			addStep(step);
		}
		void saveDelete(int digit, boolean decimalDeleted){
			Step step = new StepDelete(activity.get(), digit, decimalDeleted);
			addStep(step);
		}
		void saveClear(List <BigDecimal> savedNumbers, List <Integer> savedDigits, int savedDecimalPos,
				String digitsText, int writingDigits){
			Step step = new StepClear(activity.get(), savedNumbers, savedDigits, savedDecimalPos, digitsText, writingDigits);
			addStep(step);
		}
		void savePop(List <Integer> savedDigits, int savedDecimalPos){
			Step step = new StepPop(activity.get(), savedDigits, savedDecimalPos);
			addStep(step);
		}
		void savePop(BigDecimal number){
			Step step = new StepPop(activity.get(), number);
			addStep(step);
		}
		void saveSwitch(){
			Step step = new StepSwitch(activity.get());
			addStep(step);
		}
		synchronized void goBack(){
			boolean goBackAgain = true;
			while(currentStep > 0 && goBackAgain == true){
				--currentStep;
				goBackAgain = false;
				Step step = listSteps.get(currentStep);
				step.undo();
				// which follows is used to eventually keep undoing deletes
				// do that only if both this and previous steps are:
				// 1. instanceof StepDelete
				// 2. type != text
				if(step instanceof StepDelete){
					if(((StepDelete) step).type != DeleteType.TEXT && currentStep > 0){
						step = listSteps.get(currentStep - 1);
						if(step instanceof StepDelete){
							if(((StepDelete) step).type != DeleteType.TEXT){
								goBackAgain = true;
							}
						}
					}
				}
			}
		}
		synchronized void goForward(){
			if(currentStep < listSteps.size()){
				listSteps.get(currentStep).redo();
				++currentStep;
			}
		}
	}
	interface Step extends Serializable{
		void undo();
		void redo();
	}
	static class StepOperation implements Step {
		transient WeakReference<MainActivity> activity;
		BigDecimal[] operands;
		BigDecimal result;
		int arity;
		String savedText;
		StepOperation(MainActivity activity, int arity, BigDecimal[] operands, String textDigits, BigDecimal result){
			this.activity = new WeakReference<MainActivity>(activity);
			this.arity = arity;
			this.operands = operands;
			this.savedText = textDigits;
			this.result = result;
		}
		StepOperation(MainActivity activity, StepOperation old){
			this.activity = new WeakReference<MainActivity>(activity);
			this.arity = old.arity;
			this.operands = old.operands;
			this.savedText = old.savedText;
			this.result = old.result;
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			act.mListNumbers.removeLast();
			act.removeLastLayoutNumber();
			for(int i = 0; i < arity; ++i){
				act.mListNumbers.add(operands[i]);
				act.showNumber(operands[i]);
			}
			act.mTvDigits.setText(savedText);
			act.showWritingDigits(false);
		}
		@Override
		public void redo(){
			MainActivity act = activity.get();
			for(int i = 0; i < arity; ++i){
				act.mListNumbers.removeLast();
				act.removeLastLayoutNumber();
			}
			act.mListNumbers.add(result);
			act.showNumber(result);
			act.showDigits(result);
			act.showWritingDigits(false);
		}
	}
	static class StepEnter implements Step {
		transient WeakReference<MainActivity> activity;
		List <Integer> savedDigits;
		int savedPos;
		String textDigits;
		BigDecimal number;
		boolean reEnterNumber;
		StepEnter(MainActivity activity, List<Integer> savedDigits, int savedPos, String textDigits, BigDecimal number,
				  boolean reEnterNumber){
			this.activity = new WeakReference<MainActivity>(activity);
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedPos = savedPos;
			this.textDigits = textDigits;
			this.number = number;
			this.reEnterNumber = reEnterNumber;
		}
		StepEnter(MainActivity activity, StepEnter old){
			this(activity, old.savedDigits, old.savedPos, old.textDigits, old.number, old.reEnterNumber);
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			act.mListNumbers.removeLast();
			act.removeLastLayoutNumber();
			act.mListDigits = new ArrayList <Integer>(savedDigits);
			act.mPosDecimal = savedPos;
			act.mTvDigits.setText(textDigits);
			act.showWritingDigits(true);
		}
		@Override
		public void redo() {
			activity.get().touchedEnter(reEnterNumber, false);
		}
	}
	static class StepDigits implements Step {
		transient WeakReference<MainActivity> activity;
		List <Integer> savedDigits;
		int savedPos;
		StepDigits(MainActivity activity){
			this.activity = new WeakReference<MainActivity>(activity);
		}
		StepDigits(MainActivity activity, StepDigits old){
			this(activity);
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			savedDigits = new ArrayList <Integer>(act.mListDigits);
			savedPos = act.mPosDecimal;
			act.mTvDigits.setText("");
			act.showWritingDigits(true);
			act.mListDigits.clear();
			act.mPosDecimal = -1;
		}
		public void redo(){
			MainActivity act = activity.get();
			act.mListDigits = new ArrayList <Integer>(savedDigits);
			act.mPosDecimal = savedPos;
			act.showDigitsWithZeros(act.mListDigits, act.mPosDecimal);
			act.showWritingDigits(true);
		}
	}
	static class StepDelete implements Step {
		transient WeakReference<MainActivity> activity;
		String savedText;
		int digit;
		DeleteType type;
		StepDelete(String savedText){
			this.savedText = savedText;
			type = DeleteType.TEXT;
		}
		StepDelete(MainActivity activity, int d, boolean decimalDeleted){
			this.activity = new WeakReference<MainActivity>(activity);
			if(decimalDeleted){
				type = DeleteType.SEPARATOR;
			}else{
				type = DeleteType.DIGIT;
			}
			this.digit = d;
		}
		StepDelete(MainActivity activity, StepDelete old){
			this(activity, old.digit, old.type==DeleteType.SEPARATOR);
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			switch(type){
			case TEXT:
				act.mTvDigits.setText(savedText);
				act.showWritingDigits(false);
			break;
			case SEPARATOR:
				act.mPosDecimal = digit;
				act.showDigits(act.mListDigits, act.mPosDecimal);
				act.showWritingDigits(true);
			break;
			case DIGIT:
				act.mListDigits.add(Integer.valueOf(digit));
				act.showDigits(act.mListDigits, act.mPosDecimal);
				act.showWritingDigits(true);
			break;
			}
		}
		@Override
		public void redo(){
			activity.get().touchedDelete(false);
		}
	}
	static class StepClear implements Step {
		transient WeakReference<MainActivity> activity;
		LinkedList <BigDecimal> savedNumbers;
		List <Integer> savedDigits;
		int savedDecimalPos;
		String digitsText;
		int writingDigits;
		StepClear(MainActivity activity, List<BigDecimal> savedNumbers, List<Integer> savedDigits, int savedDecimalPos,
				  String digitsText, int writingDigits){
			this.activity = new WeakReference<MainActivity>(activity);
			this.savedNumbers = new LinkedList <BigDecimal>(savedNumbers);
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedDecimalPos = savedDecimalPos;
			this.digitsText = new String(digitsText);
			this.writingDigits = writingDigits;
		}
		StepClear(MainActivity activity, StepClear old){
			this(activity, old.savedNumbers, old.savedDigits, old.savedDecimalPos, old.digitsText, old.writingDigits);
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			act.mListNumbers = new LinkedList <BigDecimal>(savedNumbers);
			act.mListDigits = new ArrayList <Integer>(savedDigits);
			act.mPosDecimal = this.savedDecimalPos;
			for(BigDecimal num : act.mListNumbers){
				act.showNumber(num);
			}
			act.mTvDigits.setText(digitsText);
			act.showWritingDigits(writingDigits == View.VISIBLE);
			act.showOnlyScrollView(act.mScrollDigits);
		}
		@Override
		public void redo(){
			activity.get().touchedClear(false);
		}
	}
	static class StepPop implements Step {
		transient WeakReference<MainActivity> activity;
		boolean atDigits;// where it was popped
		List <Integer> savedDigits;
		int savedDecimalPos;
		BigDecimal number;
		StepPop(MainActivity activity, List<Integer> savedDigits, int savedDecimalPos){
			this.activity = new WeakReference<MainActivity>(activity);
			this.atDigits = true;
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedDecimalPos = savedDecimalPos;
		}
		StepPop(MainActivity activity, BigDecimal number){
			this.activity = new WeakReference<MainActivity>(activity);
			this.atDigits = false;
			this.number = number;
		}
		StepPop(MainActivity activity, StepPop old){
			this.activity = new WeakReference<MainActivity>(activity);
			this.atDigits = old.atDigits;
			this.savedDigits = old.savedDigits;
			this.savedDecimalPos = old.savedDecimalPos;
			this.number = old.number;
		}
		@Override
		public void undo(){
			MainActivity act = activity.get();
			if(atDigits){
				act.mListDigits = new ArrayList <Integer>(savedDigits);
				act.mPosDecimal = savedDecimalPos;
				act.showDigits(act.mListDigits, act.mPosDecimal);
				act.showWritingDigits(true);
			}else{
				act.mListNumbers.add(number);
				act.showNumber(number);
			}
		}
		@Override
		public void redo(){
			activity.get().touchedPopNumber(false);
		}
	}
	static class StepSwitch implements Step {
		transient WeakReference<MainActivity> activity;
		StepSwitch(MainActivity activity){
			this.activity = new WeakReference<MainActivity>(activity);
		}
		@Override
		public void undo(){
			activity.get().touchedSwitch(false);
		}
		@Override
		public void redo(){
			activity.get().touchedSwitch(false);
		}
	}

	class ViewsWidthCopier implements ViewTreeObserver.OnPreDrawListener{
		View from, to;
		ViewsWidthCopier(View from, View to){
			this.from = from;
			this.to = to;
		}
		@Override
		public boolean onPreDraw(){
			ViewGroup.LayoutParams params = to.getLayoutParams();
			params.width = from.getWidth();
			to.setLayoutParams(params);
			from.getViewTreeObserver().removeOnPreDrawListener(this);
			return true;
		}
	}

	OnClickListener listenerDigitButtons = new OnClickListener(){
		@Override
		public void onClick(View view){
			int id = view.getId();
			for(int i = 0; i < 10; ++i){
				if(id == mDigitButtonsIds[i]){
					touchedDigit(i, true);
					break;
				}
			}
		}
	};
	OnClickListener listenerOperations = new OnClickListener(){
		@Override
		public void onClick(View view){
			switch(view.getId()){
			case R.id.butAddition:
				touchedOperation(2, Operation.ADDITION);
			break;
			case R.id.butMultiplication:
				touchedOperation(2, Operation.MULTIPLICATION);
			break;
			case R.id.butExponentiation:
				touchedOperation(2, Operation.EXPONENTIATION);
			break;
			case R.id.butSubtraction:
				touchedOperation(2, Operation.SUBTRACTION);
			break;
			case R.id.butDivision:
				touchedOperation(2, Operation.DIVISION);
			break;
			case R.id.butInversion:
				touchedOperation(1, Operation.INVERSION);
			break;
			case R.id.butSquare:
				touchedOperation(1, Operation.SQUARE);
			break;
			case R.id.butSquareRoot:
				touchedOperation(1, Operation.SQUAREROOT);
			break;
			case R.id.butNegative:
				touchedOperation(1, Operation.NEGATIVE);
			break;
			case R.id.butSine:
				touchedOperation(1, Operation.SINE);
			break;
			case R.id.butCosine:
				touchedOperation(1, Operation.COSINE);
			break;
			case R.id.butTangent:
				touchedOperation(1, Operation.TANGENT);
			break;
			case R.id.butArcSine:
				touchedOperation(1, Operation.ARCSINE);
			break;
			case R.id.butArcCosine:
				touchedOperation(1, Operation.ARCCOSINE);
			break;
			case R.id.butArcTangent:
				touchedOperation(1, Operation.ARCTANGENT);
			break;
			case R.id.butDegreeToRadian:
				touchedOperation(1, Operation.DTOR);
			break;
			case R.id.butRadianToDegree:
				touchedOperation(1, Operation.RTOD);
			break;
			case R.id.butLog10:
				touchedOperation(1, Operation.LOG10);
			break;
			case R.id.butLogXY:
				touchedOperation(2, Operation.LOGXY);
			break;
			case R.id.butSummation:
				touchedOperation(-1, Operation.SUMMATION);
			break;
			case R.id.butMean:
				touchedOperation(-1, Operation.MEAN);
			break;
			case R.id.butPI:
				touchedOperation(0, Operation.CONSTANTPI);
			break;
			case R.id.butE:
				touchedOperation(0, Operation.CONSTANTE);
			break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState){ 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		mUsingViewAnimator = getResources().getBoolean(R.bool.layout_uses_view_animator);
		if(mUsingViewAnimator){
			mSwitcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);
		}else{
			mSwitcherFunctions = null;
		} 
		mScrollNumbers = (HorizontalScrollView) findViewById(R.id.scrollNumbers);
		mScrollDigits = (HorizontalScrollView) findViewById(R.id.scrollDigits);
		mScrollError = (HorizontalScrollView) findViewById(R.id.scrollError);
		mLayoutNumbers = (LinearLayout) findViewById(R.id.layoutNumbers);
		mLayoutNumbers.setGravity(Gravity.RIGHT);//android:gravity="right" doesn't work
		mTvAngleMode = (TextView) findViewById(R.id.tvAngleMode);
		mTvAngleMode.setText("D\nE\nG");
		mAngleMode = "deg";
		showAngleMode();
		mTvDigits = (TextView) findViewById(R.id.tvDigits);
		mTvDigits.setGravity(Gravity.RIGHT | Gravity.CENTER);
		mTvError = (TextView) findViewById(R.id.tvError);
		showOnlyScrollView(mScrollDigits);
		mTvWritingDigits = (TextView) findViewById(R.id.writingDigits);

		int buttons[] = {
				R.id.butAddition,		R.id.butMultiplication,	R.id.butExponentiation,
				R.id.butSubtraction,	R.id.butDivision,		R.id.butInversion,
				R.id.butSquare,			R.id.butSquareRoot,		R.id.butNegative,
				R.id.butSine,			R.id.butCosine,			R.id.butTangent,
				R.id.butArcSine,		R.id.butArcCosine,		R.id.butArcTangent,
				R.id.butDegreeToRadian,	R.id.butRadianToDegree,
				R.id.butLog10,			R.id.butLogXY,
				R.id.butPI,				R.id.butE,
				R.id.butSummation,		R.id.butMean
		};

		for(int i=0; i<buttons.length; i++){
			findViewById(buttons[i]).setOnClickListener(listenerOperations);
		}

		((Button) findViewById(R.id.butDecimal)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				touchedDecimal(true);
			}
		});

		((ImageButton) findViewById(R.id.butUndo)).setOnClickListener(new OnClickListener() {
			public void onClick(View view){
				 touchedUndo();
			}
		});
		((ImageButton) findViewById(R.id.butRedo)).setOnClickListener(new OnClickListener() {
			public void onClick(View view){
				touchedRedo();
			}
		});
		
		((ImageButton) findViewById(R.id.butPopNumber)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				touchedPopNumber(true);
			}
		});
		((ImageButton) findViewById(R.id.butDelete)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				touchedDelete(true);
			}
		});
		((ImageButton) findViewById(R.id.butEnterNumber)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				touchedEnter(true, true);
			}
		});
		((ImageButton) findViewById(R.id.butSwitch)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				touchedSwitch(true);
			}
		});
		((ImageButton) findViewById(R.id.butFunctions)).setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				//from stackoverflow.com
				MainActivity.this.openOptionsMenu(); // activity's onCreateOptionsMenu gets called
				mOptionsMenu.performIdentifierAction(R.id.menuApplyFunction, 0);
			}
		});

		mListNumbers = new LinkedList <BigDecimal>();
		mListDigits = new ArrayList <Integer>();
		mHistorySaver = new HistorySaver(this);
		Resources res = getResources();
		String buttonName;
		int buttonId;
		mDigitButtonsIds = new int[10];
		for(int i = 0; i < 10; i++){
			buttonName = "but" + Integer.toString(i);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			mDigitButtonsIds[i] = buttonId;
			((Button) findViewById(buttonId)).setOnClickListener(listenerDigitButtons);
		}
		if(mUsingViewAnimator){
		for(int i=0; i<2; ++i){
			buttonName  = "butFunctions" + Integer.toString(i) + "to" + Integer.toString(i+1);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			((ImageButton) findViewById(buttonId)).setOnClickListener(
					new OnClickListener(){
						public void onClick(View view){
							mSwitcherFunctions.showNext();
							showAngleMode();
						}
					}
				);
			buttonName = "butFunctions" + Integer.toString(i+1) + "to" + Integer.toString(i);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			((ImageButton) findViewById(buttonId)).setOnClickListener(
					new OnClickListener(){
						public void onClick(View view){
							mSwitcherFunctions.showPrevious();
							showAngleMode();
						}
					}
				);			
		}
		}
		
		if(mUsingViewAnimator){
			Button b1 = (Button) findViewById(R.id.but1);
			Button b2 = (Button) findViewById(R.id.but2);
			ImageButton bUndo =  (ImageButton) findViewById(R.id.butUndo);
			ImageButton bRedo =  (ImageButton) findViewById(R.id.butRedo);
			ViewsWidthCopier copierUndo = new ViewsWidthCopier( b1, bUndo);
			ViewsWidthCopier copierRedo = new ViewsWidthCopier( b2, bRedo);
			b1.getViewTreeObserver().addOnPreDrawListener(copierUndo);
			b2.getViewTreeObserver().addOnPreDrawListener(copierRedo);
		}
		//Toast.makeText(this, "CREATE", Toast.LENGTH_LONG).show();
		
		/* remove? this hack which always shows overflow menu in actionbar.
		  Used only on emulator with invisible hardware keys
		try{
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null){
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}catch(Exception e){
			e.printStackTrace();
		}*/
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		// Handle item selection
		switch(item.getItemId()){
		case R.id.menuUndo:
			touchedUndo();
			return true;
		case R.id.menuRedo:
			touchedRedo();
			return true;
		case R.id.menuClear:
			touchedClear(true);
			return true;
		case R.id.menuSurfaceTriangle:
			touchedOperation(3, Operation.TRIANGLE_SURFACE);
			return true;
		case R.id.menuHypotenusePythagoras:
			touchedOperation(2, Operation.HYPOTENUSE_PYTHAGORAS);
			return true;
		case R.id.menuLegPythagoras:
			touchedOperation(2, Operation.LEG_PYTHAGORAS);
			return true;
		case R.id.menuSurfaceCircle:
			touchedOperation(1, Operation.CIRCLE_SURFACE);
			return true;
		case R.id.menuAngleMode:
			if(mAngleMode.equals("deg")){
				mAngleMode = "rad";
				mTvAngleMode.setText("R\nA\nD");
			}else{
				mAngleMode = "deg";
				mTvAngleMode.setText("D\nE\nG");
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void onSaveInstanceState(Bundle saver){
		saver.putIntegerArrayList("ListDigits", (ArrayList<Integer>) mListDigits);
		saver.putInt("PosDecimal", mPosDecimal);
		ArrayList <String> numbers = buildStringArrayListNumbers(mListNumbers);
		saver.putStringArrayList("ListNumbers", numbers);
		saver.putString("textDigits", mTvDigits.getText().toString());
		saver.putInt("visibility", getWritingDigitsVisibility());
		saver.putString("AngleMode", mAngleMode);
		saver.putSerializable("HistorySaver", mHistorySaver);
		if(mUsingViewAnimator){
			saver.putInt("indexFunctionsView", mSwitcherFunctions.getDisplayedChild());
		}
		super.onSaveInstanceState(saver);
	}
	public void onRestoreInstanceState(Bundle saved){
		super.onRestoreInstanceState(saved);
		mPosDecimal = saved.getInt("PosDecimal");
		mListDigits = saved.getIntegerArrayList("ListDigits");
		mTvDigits.setText(saved.getString("textDigits"));
		mAngleMode = saved.getString("AngleMode");
		if(saved.getInt("visibility") == View.VISIBLE){
			showWritingDigits(true);
		}else{
			showWritingDigits(false);
		}
		ArrayList <String> stringNumbers = saved.getStringArrayList("ListNumbers");
		for(String str : stringNumbers){
			BigDecimal num = new BigDecimal(str);
			mListNumbers.add(num);
			showNumber(num);
		}
		HistorySaver oldHistorySaver = (HistorySaver) saved.getSerializable("HistorySaver");
		mHistorySaver = new HistorySaver(this, oldHistorySaver);
		if(mUsingViewAnimator){
			int indexFunctions = saved.getInt("indexFunctionsView");
			for(int i=0; i<indexFunctions; ++i){
				mSwitcherFunctions.showNext();
			}
		}
		showAngleMode();
		//Toast.makeText(this, "RESTORE", Toast.LENGTH_LONG).show();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main_menu, menu);
		mOptionsMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	void touchedUndo(){
		mHistorySaver.goBack();
	}
	void touchedRedo(){
		mHistorySaver.goForward();
	}
	void touchedDigit(int digit, boolean save){
		if(save){
			mHistorySaver.saveDigits();
		}
		mListDigits.add(Integer.valueOf(digit));
		showDigitsWithZeros(mListDigits, mPosDecimal);
		showWritingDigits(true);
	}
	void touchedDecimal(boolean save){
		if(save)
			mHistorySaver.saveDigits();
		mPosDecimal = mListDigits.size();
		if(mListDigits.isEmpty()){
			mTvDigits.setText("0" + getResources().getString(R.string.decimalSeparator));
			showWritingDigits(true);
			showOnlyScrollView(mScrollDigits);
		}else{
			showDigitsWithZeros(mListDigits, mPosDecimal);
			showWritingDigits(true);
		}
	}
	void touchedDelete(boolean save){
		if(getShownView() == mScrollError){// false when called from delete.redo()
			mTvError.setText("");
			mTvDigits.setText("");
			showWritingDigits(true);
			showOnlyScrollView(mScrollDigits);
			return;
		}
		String str = mTvDigits.getText().toString();
		if(mListDigits.size() == 0 && mPosDecimal == -1 && str.equals("") == false){
			if(save){
				mHistorySaver.saveDelete(str);
			}
			mTvDigits.setText("");
			showWritingDigits(true);
		}else if(str.length() > 0){
			char lastChar = str.charAt(str.length() - 1);
			int digit = Character.getNumericValue(lastChar);
			if(digit < 0){
				if(save)
					mHistorySaver.saveDelete(mPosDecimal, true);
				mPosDecimal = -1;
			}else{
				if(save)
					mHistorySaver.saveDelete(digit, false);
				mListDigits.remove(mListDigits.size() - 1);
			}
			showDigits(mListDigits, mPosDecimal);
			showWritingDigits(true);
		}
	}
	boolean touchedEnter(boolean reEnterNumber, boolean save){
		boolean enteredDigits = false;
		if(mListDigits.size() != 0 || mPosDecimal != -1){
			enteredDigits = true;
			BigDecimal number = buildNumber(mListDigits, mPosDecimal);
			if(save)
				mHistorySaver.saveEnter(mListDigits, mPosDecimal, mTvDigits.getText().toString(), number,
						reEnterNumber);
			mListNumbers.add(number);
			showNumber(number);
			showDigits(number);
			showWritingDigits(false);
		}else if(mListNumbers.size() != 0 && reEnterNumber == true){
			BigDecimal number = mListNumbers.getLast();
			if(save)
				mHistorySaver.saveEnter(mListDigits, mPosDecimal, mTvDigits.getText().toString(), number,
						reEnterNumber);
			mListNumbers.add(number);
			showNumber(number);
		}
		mPosDecimal = -1;
		mListDigits.clear();
		return enteredDigits;
	}
	void touchedPopNumber(boolean save){
		// if there are digits, just clear digits
		if(mListDigits.size() != 0 || mPosDecimal != -1){
			if(save)
				mHistorySaver.savePop(mListDigits, mPosDecimal);
			touchedEnter(false, false);
			mTvDigits.setText("");
			showWritingDigits(true);
			showOnlyScrollView(mScrollDigits);
			mListNumbers.removeLast();
			removeLastLayoutNumber();
		//if there are not digits, delete last number
		}else if(mListNumbers.size() != 0){
			if(save)
				mHistorySaver.savePop(mListNumbers.getLast());
			showOnlyScrollView(mScrollDigits);
			mTvDigits.setText("");
			showWritingDigits(true);
			mListNumbers.removeLast();
			removeLastLayoutNumber();
		}
	}
	void touchedClear(boolean save){
		if(save)
			mHistorySaver.saveClear(mListNumbers, mListDigits, mPosDecimal, mTvDigits.getText().toString(), getWritingDigitsVisibility());
		mListNumbers.clear();
		mListDigits.clear();
		mPosDecimal = -1;
		mTvDigits.setText("");
		showWritingDigits(true);
		showOnlyScrollView(mScrollDigits);
		mLayoutNumbers.removeViews(0, mLayoutNumbers.getChildCount());
	}
	void touchedSwitch(boolean save){
		if(mListNumbers.size() >= 2){
			if(save) {
				mHistorySaver.saveSwitch();
			}
			BigDecimal num2 = mListNumbers.removeLast();
			BigDecimal num1 = mListNumbers.removeLast();
			removeLastLayoutNumber();
			removeLastLayoutNumber();
			mListNumbers.add(num2);
			mListNumbers.add(num1);
			showNumber(num2);
			showNumber(num1);
			if(mListDigits.size() == 0 && mPosDecimal == -1){
				showDigits(num1);
				showWritingDigits(false);
			}
		}
	}
	void touchedOperation(int argc, Operation operation){
		boolean enteredDigits = touchedEnter(false, true);
		int numberOfArguments;
		if(mListNumbers.size() < ((argc == -1)?1:argc)){
			setScrollViewError(getResources().getString(R.string.notEnoughArguments));
		}else{
			if(argc == -1){
				numberOfArguments = mListNumbers.size();
			}else if(argc == 0 && enteredDigits == true){
				numberOfArguments = 1;
			}else{
				numberOfArguments = argc;
			}
			BigDecimal[] operands = new BigDecimal[numberOfArguments];
			for(int i = numberOfArguments - 1; i >= 0; --i){
				operands[i] = mListNumbers.removeLast();
			}
			BigDecimal result = BigDecimal.ZERO;//valor inicial no usado pero necesario
			BigDecimal radians;
			String error = "";
			switch(argc){
			case -1:
				switch(operation){
				case SUMMATION:
					result = operands[0];
					for(int i=1; i<numberOfArguments; ++i){
						result = result.add(operands[i]).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
					result = result.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case MEAN:
					result = operands[0];
					for(int i=1; i<numberOfArguments; ++i){
						result = result.add(operands[i]).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
					result = result.divide(new BigDecimal(numberOfArguments), DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			case 0:
				switch(operation){
				case CONSTANTPI:
					result = new BigDecimal(CONSTANT_PI);
				break;
				case CONSTANTE:
					result = new BigDecimal(CONSTANT_E);
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
				if(enteredDigits){
					result = result.multiply(operands[0]);
				}
				result = result.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
			break;
			case 1:
				switch(operation){
				case INVERSION:
					if(operands[0].compareTo(BigDecimal.ZERO) == 0){
						error = getResources().getString(R.string.divisionByZero);
					}else{
						result = BigDecimal.ONE.divide(operands[0], DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				case SQUARE:
					result = operands[0].multiply(operands[0]).setScale(
							Math.min(DEFAULT_SCALE, operands[0].scale() * 2), DEFAULT_ROUNDING);
				break;
				case NEGATIVE:
					result = operands[0].negate().setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case SQUAREROOT:
					if(operands[0].compareTo(BigDecimal.ZERO) < 0){
						error = getResources().getString(R.string.negativeSquareRoot);
					}else{
						result = new BigDecimal(Math.sqrt(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
								DEFAULT_ROUNDING);
					}
				break;
				case SINE:
					radians = operands[0];
					if(mAngleMode.equals("deg")){
						radians = new BigDecimal(Math.toRadians(operands[0].doubleValue()));
					}
					result = new BigDecimal(Math.sin(radians.doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
				break;
				case COSINE:
					radians = operands[0];
					if(mAngleMode.equals("deg")){
						radians = new BigDecimal(Math.toRadians(operands[0].doubleValue()));
					}
					result = new BigDecimal(Math.cos(radians.doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
				break;
				case TANGENT:
					radians = operands[0];
					if(mAngleMode.equals("deg")){
						radians = new BigDecimal(Math.toRadians(operands[0].doubleValue()));
					}
					result = new BigDecimal(Math.tan(radians.doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
				break;
				case ARCSINE:
					if(operands[0].compareTo(new BigDecimal("-1.0")) < 0
							|| operands[0].compareTo(new BigDecimal("1.0")) > 0){
						error = getResources().getString(R.string.arcsineOutOfRange);
					}else{
						result = new BigDecimal(Math.asin(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
								DEFAULT_ROUNDING);
						if(mAngleMode.equals("deg")){
							result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(DEFAULT_SCALE,
									DEFAULT_ROUNDING);;
						}
					}
				break;
				case ARCCOSINE:
					if(operands[0].compareTo(new BigDecimal("-1.0")) < 0
							|| operands[0].compareTo(new BigDecimal("1.0")) > 0){
						error = getResources().getString(R.string.arccosineOutOfRange);
					}else{
						result = new BigDecimal(Math.acos(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
								DEFAULT_ROUNDING);
						if(mAngleMode.equals("deg")){
							result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(DEFAULT_SCALE,
									DEFAULT_ROUNDING);;
						}
					}
				break;
				case ARCTANGENT:
					result = new BigDecimal(Math.atan(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
					if(mAngleMode.equals("deg")){
						result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(DEFAULT_SCALE,
								DEFAULT_ROUNDING);;
					}
				break;
				case LOG10:
					if(operands[0].compareTo(BigDecimal.ZERO) <= 0){
						error = getResources().getString(R.string.logOutOfRange);
					}else{
						result = new BigDecimal(Math.log(operands[0].doubleValue()) / Math.log(10.0)).setScale(
								DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				case DTOR:
					result = new BigDecimal(Math.toRadians(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
				break;
				case RTOD:
					result = new BigDecimal(Math.toDegrees(operands[0].doubleValue())).setScale(DEFAULT_SCALE,
							DEFAULT_ROUNDING);
				break;
				case CIRCLE_SURFACE:
					if(operands[0].compareTo(BigDecimal.ZERO) < 0){
						error = getResources().getString(R.string.negativeRadius);
					}else{
						result = operands[0].pow(2).multiply(new BigDecimal(CONSTANT_PI)).setScale(DEFAULT_SCALE,
								DEFAULT_ROUNDING);
					}
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			case 2:
				switch(operation){
				case ADDITION:
					result = operands[0].add(operands[1]).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case MULTIPLICATION:
					result = operands[0].multiply(operands[1]).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case EXPONENTIATION:
					result = new BigDecimal(Math.pow(operands[0].doubleValue(), operands[1].doubleValue()))
							.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case SUBTRACTION:
					result = operands[0].subtract(operands[1]).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				break;
				case DIVISION:
					if(operands[1].compareTo(BigDecimal.ZERO) == 0){
						error = getResources().getString(R.string.divisionByZero);
					}else{
						result = operands[0].divide(operands[1], DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				case LOGXY:
					if(operands[0].compareTo(BigDecimal.ZERO) <= 0
							|| operands[1].compareTo(new BigDecimal("0.0")) <= 0){
						error = getResources().getString(R.string.logOutOfRange);
					}else{
						result = new BigDecimal(Math.log(operands[1].doubleValue())
								/ Math.log(operands[0].doubleValue())).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				case HYPOTENUSE_PYTHAGORAS:
					if(operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO)< 0){
						error = getResources().getString(R.string.sideCantBeNegative);
					}else{
						result = new BigDecimal(
								Math.sqrt(
								operands[0].pow(2).add(operands[1].pow(2)).doubleValue()))
								.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				case LEG_PYTHAGORAS:
					if(operands[0].compareTo(BigDecimal.ZERO) < 0 || operands[1].compareTo(BigDecimal.ZERO)< 0){
						error = getResources().getString(R.string.sideCantBeNegative);
					}else{
						BigDecimal hyp = operands[0].max(operands[1]);
						BigDecimal leg = operands[0].min(operands[1]);
						result = new BigDecimal(
								Math.sqrt(
								hyp.pow(2).subtract(leg.pow(2)).doubleValue()))
								.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			case 3:
				switch(operation){
				case TRIANGLE_SURFACE:
					BigDecimal p = operands[0].add(operands[1]).add(operands[2]).divide(new BigDecimal("2.0"));
					BigDecimal q = p.multiply(p.subtract(operands[0])).multiply(p.subtract(operands[1]))
							.multiply(p.subtract(operands[2]));
					if(q.compareTo(BigDecimal.ZERO) < 0){
						error = getResources().getString(R.string.notATriangle);
					}else{
						result = new BigDecimal(Math.sqrt(q.doubleValue())).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
					}
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			}
			if(error.equals("") == false){
				setScrollViewError(error);
				for(int i = 0; i < argc; ++i){
					mListNumbers.add(operands[i]);
				}
			}else{
				mHistorySaver.saveOperation(numberOfArguments, operands, mTvDigits.getText().toString(), result);
				for(int i = 0; i < numberOfArguments; ++i){
					removeLastLayoutNumber();
				}
				mListNumbers.add(result);
				showNumber(result);
				showDigits(result);
				showWritingDigits(false);
			}
		}
	}

	void showNumber(BigDecimal number){
		TextView tv = new TextView(this);
		// HACER poner atributos al tv
		// float margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
		// 14, getResources().getDisplayMetrics());
		float margin = getResources().getDimension(R.dimen.number_view_horiz_margin);
//		float textSize = getResources().getDimension(R.dimen.list_numbers_text_size);
		int textSize = getResources().getDimensionPixelSize(R.dimen.list_numbers_text_size);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT);
		params.setMargins((int) margin, 0, (int) margin, 0);
		tv.setLayoutParams(params);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		tv.setGravity(Gravity.CENTER_VERTICAL);
		tv.setText(localizeDecimalSeparator(removeZeros(number.toString(), false, ".")));
		mLayoutNumbers.addView(tv);
		scrollToRight(mScrollNumbers);
		return;
	}
	String localizeDecimalSeparator(String str){
		StringBuilder localized = new StringBuilder(str);
		char decimalSeparator = getResources().getString(R.string.decimalSeparator).charAt(0);
		for(int i=0; i<str.length(); i++){
			if(str.charAt(i) == '.'){
				localized.setCharAt(i, decimalSeparator);
				break;
			}
		}
		return localized.toString();
	}
	void showDigits(List <Integer> digits, int decimal){
		mTvDigits.setText(buildString(digits, decimal, false, getResources().getString(R.string.decimalSeparator)));
		showOnlyScrollView(mScrollDigits);
	}
	void showDigitsWithZeros(List <Integer> digits, int decimal){
		mTvDigits.setText(buildString(digits, decimal, true, getResources().getString(R.string.decimalSeparator)));
		showOnlyScrollView(mScrollDigits);
	}
	void showDigits(BigDecimal number){
		mTvDigits.setText(localizeDecimalSeparator(removeZeros(number.toString(), false, ".")));
		showOnlyScrollView(mScrollDigits);
	}
	void removeLastLayoutNumber(){
		if(mLayoutNumbers.getChildCount() != 0){
			mLayoutNumbers.removeViewAt(mLayoutNumbers.getChildCount() - 1);
		}
	}
	void setScrollViewError(String str){
		mTvError.setText(getResources().getString(R.string.error) + ": " + str);
		showOnlyScrollView(mScrollError);
	}
	void showOnlyScrollView(HorizontalScrollView view){
		if(view == mScrollDigits){
			showScrollView(mScrollError, false);
			scrollToRight(mScrollDigits);
		}else if(view == mScrollError){
			showScrollView(mScrollDigits, false);
		}
		showScrollView(view, true);
	}
	void showScrollView(HorizontalScrollView view, boolean show){
		if(show){
			view.setVisibility(View.VISIBLE);
		}else{
			view.setVisibility(View.GONE);
		}
	}
	void showWritingDigits(boolean show){
		if(show){
			mTvWritingDigits.setVisibility(View.VISIBLE);
		}else{
			mTvWritingDigits.setVisibility(View.INVISIBLE);
		}
	}
	int getWritingDigitsVisibility(){
		return mTvWritingDigits.getVisibility();
	}
	View getShownView(){
		if(mScrollError.getVisibility() == View.VISIBLE){
			return mScrollError;
		}else if(mScrollDigits.getVisibility() == View.VISIBLE){
			return mScrollDigits;
		}else{
			return null;
		}
	}
	void showAngleMode(){
		if(mUsingViewAnimator == false){
			mTvAngleMode.setVisibility(View.VISIBLE);
		}else if(mSwitcherFunctions.getDisplayedChild() == 1){
			mTvAngleMode.setVisibility(View.VISIBLE);
		}else{
			mTvAngleMode.setVisibility(View.INVISIBLE);
		}
	}

	static String buildString(List <Integer> listDigits, int pos, boolean keepTrailingZeros, String decimalSeparator){
		int numberOfDigits = listDigits.size();
		StringBuilder numberString = new StringBuilder(numberOfDigits + 2);
		for(int i = 0; i < numberOfDigits; ++i){
			if(pos == i){
				if(pos == 0){
					numberString.append(0);
				}
				numberString.append(decimalSeparator);
			}
			numberString.append(listDigits.get(i));
		}
		if(pos == numberOfDigits){
			numberString.append(decimalSeparator);
		}
		return removeZeros(numberString.toString(), keepTrailingZeros, decimalSeparator);
	}

	static BigDecimal buildNumber(List <Integer> listDigits, int pos){
		int numberOfDigits = listDigits.size();
		BigDecimal number;
		if(numberOfDigits == 0){
			number = new BigDecimal("0.0");
		}else{
			StringBuilder numberString = new StringBuilder(numberOfDigits + 2);
			for(int i = 0; i < numberOfDigits; ++i){
				if(pos == i){
					if(pos == 0){
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

	static void scrollToRight(HorizontalScrollView view){
		if(view != null){
			view.post(new Scroller(view));
		}
	}

	static String removeZeros(String string, boolean keepTrailingZeros, String decimalSeparator){
		char decimalSeparatorChar = decimalSeparator.charAt(0);
		boolean keepRemoving = true;
		boolean scientific = false;
		StringBuilder str = new StringBuilder(string);
		while(keepRemoving && str.length() >= 2){// delete left zeros
			if(str.charAt(0) == '0'
					&& str.charAt(1) != decimalSeparatorChar
					&& str.charAt(1) != 'E'){
				str.deleteCharAt(0);
			}else{
				if(str.charAt(1) == 'E'){
					scientific = true;
				}
				keepRemoving = false;
			}
		}
		// keepRemoving = !keepTrailingZeros && !scientific;
		keepRemoving = !(keepTrailingZeros || scientific);
		if( !keepRemoving){
			if(scientific){
				return "0";
			}else{
				return str.toString();
			}
		}
		int pos = 0;
		int posDec = str.length();
		while(pos < str.length()){
			if(str.charAt(pos) == decimalSeparatorChar){
				posDec = pos;
			}
			pos++;
		}
		pos = str.length() - 1;
		while(keepRemoving && posDec < pos){
			if(str.charAt(pos) == '0'){
				str.deleteCharAt(pos);
				if(str.charAt(pos - 1) == decimalSeparatorChar){
					str.deleteCharAt(pos - 1);
					keepRemoving = false;
				}
				pos--;
			}else{
				keepRemoving = false;
			}
		}
		return str.toString();
	}

	static ArrayList <String> buildStringArrayListNumbers(List <BigDecimal> numbers){
		ArrayList <String> stringNumbers = new ArrayList <String>();
		for(BigDecimal num : numbers){
			stringNumbers.add(removeZeros(num.toString(), false, "."));
		}
		return stringNumbers;
	}

}
