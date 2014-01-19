package civyshk.rpncalc104k;
import java.lang.reflect.Field;
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
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import civyshk.rpncalc104k.R;

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
	
	LinkedList <BigDecimal> listNumbers;
	List <Integer> listDigits;
	int posDecimal = -1;
	int posDecimalFromGetDigits;
	RoundingMode defaultRounding = RoundingMode.HALF_EVEN;
	int defaultScale = 9;
	ViewAnimator switcherFunctions;
	HorizontalScrollView scrollViewNumbers, scrollViewDigits, scrollViewError;
	LinearLayout layoutNumbers;
	TextView tvDigits, tvError, tvWritingDigits, tvAngleMode;
	String angleMode;
	boolean layoutUsesViewAnimator;
	enum Operation{
		ADDITION, MULTIPLICATION, EXPONENTIATION,
		SUBTRACTION, DIVISION, INVERSION, SQUARE, 
		SQUAREROOT, NEGATIVE, LOG10, LOGXY,
		SINE, COSINE, TANGENT, 
		ARCSINE, ARCCOSINE, ARCTANGENT,
		DTOR, RTOD, SUMMATION, MEAN,
		TRIANGLE_SURFACE, HYPOTENUSE_PYTHAGORAS, LEG_PYTHAGORAS,
		CONSTANTPI, CONSTANTE
	}
	enum UndoType{
		OPERATION, ENTER, DIGITS, DIGIT, DECIMAL, CLEAR, POP, SWITCH, VOID
	}
	class HistorySaver{
		private List <UndoStep> listSteps;
		int currentStep;
		HistorySaver(){
			listSteps = new LinkedList <UndoStep>();
			currentStep = 0;
		}
		private void addStep(UndoStep step){
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
		private UndoStep getPreviousStep(){
			if(listSteps.isEmpty()){
				return null;
			}else if(currentStep < 1){
				return null;
			}else{
				return listSteps.get(currentStep - 1);
			}
		}
		void saveOperation(int arity, BigDecimal[] operand, String textDigits, BigDecimal result){
			UndoStep step = new UndoOperation(arity, operand, textDigits, result);
			addStep(step);
		}
		void saveEnter(List <Integer> savedDigits, int savedPos, String textDigits, BigDecimal number,
				boolean reEnterNumber){
			UndoStep step = new UndoEnter(savedDigits, savedPos, textDigits, number, reEnterNumber);
			addStep(step);
		}
		void saveDigits(){
			if(getPreviousStep() instanceof UndoDigits){
				// nothing;
			}else{
				UndoStep step = new UndoDigits();
				addStep(step);
			}
		}
		void saveDelete(String savedText){ 
			UndoStep step = new UndoDelete(savedText);
			addStep(step);
		}
		void saveDelete(int d, boolean decimalDeleted){
			UndoStep step = new UndoDelete(d, decimalDeleted);
			addStep(step);
		}
		void saveClear(List <BigDecimal> savedNumbers, List <Integer> savedDigits, int savedDecimalPos,
				String digitsText, int writingDigits){
			UndoStep step = new UndoClear(savedNumbers, savedDigits, savedDecimalPos, digitsText, writingDigits);
			addStep(step);
		}
		void savePop(List <Integer> savedDigits, int savedDecimalPos){
			UndoStep step = new UndoPop(savedDigits, savedDecimalPos);
			addStep(step);
		}
		void savePop(BigDecimal number){
			UndoStep step = new UndoPop(number);
			addStep(step);
		}
		void saveSwitch(){
			UndoStep step = new UndoSwitch();
			addStep(step);
		}
		void goBack(){
			boolean goBackAgain = true;
			UndoStep step;
			while(currentStep > 0 && goBackAgain == true){
				--currentStep;
				step = listSteps.get(currentStep);
				step.undo();
				goBackAgain = false;
				// which follows is used to eventually keep undoing deletes
				// do that only if both this and previous steps are:
				// 1. instanceof UndoDelete
				// 2. type != string
				if(step instanceof UndoDelete){
					if(((UndoDelete) step).type != 0 && currentStep > 0){
						step = listSteps.get(currentStep - 1);
						if(step instanceof UndoDelete){
							if(((UndoDelete) step).type != 0){
								goBackAgain = true;
							}
						}
					}
				}
			}
		}
		void goFordward(){ 
			if(currentStep < listSteps.size()){
				listSteps.get(currentStep).redo();
				++currentStep;
			}
		}
	}
	interface UndoStep{
		void undo();
		void redo();
	}
	class UndoOperation implements UndoStep{
		BigDecimal[] operand;
		BigDecimal result;
		int arity;
		String savedText;
		UndoOperation(int arity, BigDecimal[] operand, String textDigits, BigDecimal result){
			this.arity = arity;
			this.operand = operand;
			this.result = result;
			this.savedText = textDigits;
		}
		@Override
		public void undo(){
			listNumbers.removeLast();
			removeLastNumber();
			for(int i = 0; i < arity; ++i){
				listNumbers.add(operand[i]);
				showNumber(operand[i]);
			}
			tvDigits.setText(savedText);
			setWritingDigits(false);
		}
		@Override
		public void redo(){
			for(int i = 0; i < arity; ++i){
				listNumbers.removeLast();
				removeLastNumber();
			}
			listNumbers.add(result);
			showNumber(result);
			showDigits(result);
			setWritingDigits(false);
		}
	}
	class UndoEnter implements UndoStep{
		List <Integer> savedDigits;
		int savedPos;
		String textDigits;
		BigDecimal number;
		boolean reEnterNumber;
		UndoEnter(List <Integer> savedDigits, int savedPos, String textDigits, BigDecimal number,
				boolean reEnterNumber){
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedPos = savedPos;
			this.textDigits = textDigits;
			this.number = number;
			this.reEnterNumber = reEnterNumber;
		}
		@Override
		public void undo(){
			listNumbers.removeLast();
			removeLastNumber();
			listDigits = new ArrayList <Integer>(savedDigits);
			posDecimal = savedPos;
			tvDigits.setText(textDigits);
			setWritingDigits(true);
		}
		@Override
		public void redo(){
			touchedEnter(reEnterNumber, false);
		}
	}
	class UndoDigits implements UndoStep{
		List <Integer> savedDigits;
		int savedPos;
		UndoDigits(){
		}
		@Override
		public void undo(){
			savedDigits = new ArrayList <Integer>(listDigits);
			savedPos = posDecimal;
			tvDigits.setText("");
			setWritingDigits(true);
			listDigits.clear();
			posDecimal = -1;
		}
		public void redo(){
			listDigits = new ArrayList <Integer>(savedDigits);
			posDecimal = savedPos;
			showDigitsWithZeros(listDigits, posDecimal);
			setWritingDigits(true);
		}
	}
	class UndoDelete implements UndoStep{
		String savedText;
		int d, type;
		UndoDelete(String savedText){
			this.savedText = savedText;
			type = 0;// only text deleted
		}
		UndoDelete(int d, boolean decimalDeleted){
			if(decimalDeleted){
				type = 1;// decimal separator deleted
			}else{
				type = 2;// digit deleted
			}
			this.d = d;
		}
		@Override
		public void undo(){
			switch(type){
			case 0:
				tvDigits.setText(savedText);
				setWritingDigits(false);
			break;
			case 1:
				posDecimal = d;
				showDigits(listDigits, posDecimal);
				setWritingDigits(true);
			break;
			case 2:
				listDigits.add(Integer.valueOf(d));
				showDigits(listDigits, posDecimal);
				setWritingDigits(true);
			break;
			}
		}
		@Override
		public void redo(){
			touchedDelete(false);
		}
	}
	class UndoClear implements UndoStep{
		LinkedList <BigDecimal> savedNumbers;
		List <Integer> savedDigits;
		int savedDecimalPos;
		String digitsText;
		int writingDigits;
		UndoClear(List <BigDecimal> savedNumbers, List <Integer> savedDigits, int savedDecimalPos,
				String digitsText, int writingDigits){
			this.savedNumbers = new LinkedList <BigDecimal>(savedNumbers);
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedDecimalPos = savedDecimalPos;
			this.digitsText = new String(digitsText);
			this.writingDigits = writingDigits;
		}
		@Override
		public void undo(){
			listNumbers = new LinkedList <BigDecimal>(savedNumbers);
			listDigits = new ArrayList <Integer>(savedDigits);
			posDecimal = this.savedDecimalPos;
			for(BigDecimal num : listNumbers){
				showNumber(num);
			}
			tvDigits.setText(digitsText);
			setWritingDigits(writingDigits==View.VISIBLE?true:false);
			showOnly(scrollViewDigits);
		}
		@Override
		public void redo(){
			touchedClear(false);
		}
	}
	class UndoPop implements UndoStep{
		boolean atDigits;// where it was popped
		List <Integer> savedDigits;
		int savedDecimalPos;
		BigDecimal number;
		UndoPop(List <Integer> savedDigits, int savedDecimalPos){
			this.atDigits = true;
			this.savedDigits = new ArrayList <Integer>(savedDigits);
			this.savedDecimalPos = savedDecimalPos;
		}
		UndoPop(BigDecimal number){
			this.atDigits = false;
			this.number = number;
		}
		@Override
		public void undo(){
			if(atDigits){
				listDigits = new ArrayList <Integer>(savedDigits);
				posDecimal = savedDecimalPos;
				showDigits(listDigits, posDecimal);
				setWritingDigits(true);
			}else{
				listNumbers.add(number);
				showNumber(number);
			}
		}
		@Override
		public void redo(){
			touchedPopNumber(false);
		}
	}
	class UndoSwitch implements UndoStep{
		@Override
		public void undo(){
			touchedSwitch(false);
		}
		@Override
		public void redo(){
			touchedSwitch(false);
		}
	}
	HistorySaver historySaver;
	final static double CONSTANT_PI = 3.141592653589793;
	final static double CONSTANT_E = 2.718281828459045;
	
	int[] digitButtonsIds;
	OnClickListener listenerDigitButtons = new OnClickListener(){
		@Override
		public void onClick(View view){
			int id = view.getId();
			for(int i = 0; i < 10; ++i){
				if(id == digitButtonsIds[i]){
					touchedDigit(i, true);
					break;
				}
			}
		}
	};
	OnClickListener listenerOperations = new OnClickListener(){
		@Override
		public void onClick(View view){
			int id = view.getId();
			switch(id){
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
		layoutUsesViewAnimator = getResources().getBoolean(R.bool.layout_uses_view_animator);
		if(layoutUsesViewAnimator){
			switcherFunctions = (ViewAnimator) findViewById(R.id.viewSwitcher);
		}else{
			switcherFunctions = null;
		} 
		scrollViewNumbers = (HorizontalScrollView) findViewById(R.id.scrollNumbers);
		scrollViewDigits = (HorizontalScrollView) findViewById(R.id.scrollDigits);
		scrollViewError = (HorizontalScrollView) findViewById(R.id.scrollError);
		layoutNumbers = (LinearLayout) findViewById(R.id.layoutNumbers);
		layoutNumbers.setGravity(Gravity.RIGHT);
		tvAngleMode = (TextView) findViewById(R.id.tvAngleMode);
		tvAngleMode.setText("D\nE\nG");
		angleMode = "deg";
		showAngleMode();
		tvDigits = (TextView) findViewById(R.id.tvDigits);
		tvDigits.setGravity(Gravity.RIGHT | Gravity.CENTER);
		tvError = (TextView) findViewById(R.id.tvError);
		showOnly(scrollViewDigits);
		tvWritingDigits = (TextView) findViewById(R.id.writingDigits);
		
/*
		View rootView = (View) findViewById(R.id.rootView);
		String tag = (String) rootView.getTag();
		tvDigits.setText(tag);
		toast(tag);
		DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if(metrics.density < 0.8){
        	toast("ldpi");
        }else if(metrics.density < 1.3){
        	toast("mdpi");
        }else if(metrics.density < 1.8){
        	toast("hdpi");
        }else{
        	toast("xhdpi");
        }
*/
		
		((ImageButton) findViewById(R.id.butAddition)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butMultiplication)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butExponentiation)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butSubtraction)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butDivision)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butInversion)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butSquare)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butSquareRoot)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butNegative)).setOnClickListener(listenerOperations);
		
		((ImageButton) findViewById(R.id.butSine)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butCosine)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butTangent)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butArcSine)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butArcCosine)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butArcTangent)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butDegreeToRadian)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butRadianToDegree)).setOnClickListener(listenerOperations);
		
		((ImageButton) findViewById(R.id.butLog10)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butLogXY)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butPI)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butE)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butSummation)).setOnClickListener(listenerOperations);
		((ImageButton) findViewById(R.id.butMean)).setOnClickListener(listenerOperations);
		
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
				optionsMenu.performIdentifierAction(R.id.menuApplyFunction, 0);
			}
		});

		listNumbers = new LinkedList <BigDecimal>();
		listDigits = new ArrayList <Integer>();
		historySaver = new HistorySaver();
		Resources res = getResources();
		String buttonName;
		int buttonId;
		digitButtonsIds = new int[10];
		for(int i = 0; i < 10; i++){
			buttonName = "but" + Integer.toString(i);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			digitButtonsIds[i] = buttonId;
			((Button) findViewById(buttonId)).setOnClickListener(listenerDigitButtons);
		}
		if(layoutUsesViewAnimator){
		for(int i=0; i<2; ++i){
			buttonName  = "butFunctions" + Integer.toString(i) + "to" + Integer.toString(i+1);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			((ImageButton) findViewById(buttonId)).setOnClickListener(
					new OnClickListener(){
						public void onClick(View view){
							switcherFunctions.showNext();
							showAngleMode();
						}
					}
				);
			buttonName = "butFunctions" + Integer.toString(i+1) + "to" + Integer.toString(i);
			buttonId = res.getIdentifier(buttonName, "id", getPackageName());
			((ImageButton) findViewById(buttonId)).setOnClickListener(
					new OnClickListener(){
						public void onClick(View view){
							switcherFunctions.showPrevious();
							showAngleMode();
						}
					}
				);			
		}
		}
		
		//fix width for undo & redo buttons , to match other buttons
		if(layoutUsesViewAnimator){
			Button b1 = (Button) findViewById(R.id.but1);
			Button b2 = (Button) findViewById(R.id.but2);
			ImageButton bUndo =  (ImageButton) findViewById(R.id.butUndo);
			ImageButton bRedo =  (ImageButton) findViewById(R.id.butRedo);
			ViewsWidthCopier copierUndo = new ViewsWidthCopier((View) b1, (View) bUndo);
			ViewsWidthCopier copierRedo = new ViewsWidthCopier((View) b2, (View) bRedo);
			b1.getViewTreeObserver().addOnPreDrawListener((ViewTreeObserver.OnPreDrawListener) copierUndo);
			b2.getViewTreeObserver().addOnPreDrawListener((ViewTreeObserver.OnPreDrawListener) copierRedo);
		}
		//HACER en versiones de android pequeñas, donde no hay temas con action bar
		//mostrar undo y redo.
		
		// HACER remove this hack which always shows overflow menu in actionbar.
		// Used only on emulator with real+invisible hardware keys
		try{
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null){
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	class ViewsWidthCopier implements ViewTreeObserver.OnPreDrawListener{
		int width;
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
			//HACER remove listener
			from.getViewTreeObserver().removeOnPreDrawListener(this);
			return true;
		}		
	}
	Menu optionsMenu;
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
		case R.id.menuAngleMode:
			if(angleMode.equals("deg")){
				angleMode = "rad";
				tvAngleMode.setText("R\nA\nD");
			}else{
				angleMode = "deg";
				tvAngleMode.setText("D\nE\nG");
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	@Override
	public void onSaveInstanceState(Bundle saver){
		saver.putIntegerArrayList("listDigits", (ArrayList<Integer>) listDigits);
		saver.putInt("posDecimal", posDecimal);
		ArrayList <String> numbers = buildStringArrayListNumbers(listNumbers);
		saver.putStringArrayList("listNumbers", numbers);
		saver.putString("textDigits", tvDigits.getText().toString());
		saver.putInt("visibility", getWritingDigits());
		saver.putString("angleMode", angleMode);
		if(layoutUsesViewAnimator){
			saver.putInt("indexFunctionsView", switcherFunctions.getDisplayedChild());
		}
		super.onSaveInstanceState(saver);
	}
	public void onRestoreInstanceState(Bundle saved){
		super.onRestoreInstanceState(saved);
		posDecimal = saved.getInt("posDecimal");
		listDigits = saved.getIntegerArrayList("listDigits");
		tvDigits.setText(saved.getString("textDigits"));
		angleMode = saved.getString("angleMode");
		if(saved.getInt("visibility") == View.VISIBLE){
			setWritingDigits(true);
		}else{
			setWritingDigits(false);			
		}
		ArrayList <String> stringNumbers = saved.getStringArrayList("listNumbers");
		for(String str : stringNumbers){
			BigDecimal num = new BigDecimal(str);
			listNumbers.add(num);
			showNumber(num);
		}
		if(layoutUsesViewAnimator){
			int indexFunctions = saved.getInt("indexFunctionsView");
			for(int i=0; i<indexFunctions; ++i){
				switcherFunctions.showNext();
			}
		}
		showAngleMode();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main_menu, menu);
		optionsMenu = menu; 
		boolean b = super.onCreateOptionsMenu(menu);
//		toast("onCreateOptionsMenu");
		return b;
	}
	ArrayList <String> buildStringArrayListNumbers(List <BigDecimal> numbers){
		ArrayList <String> stringNumbers = new ArrayList <String>();
		for(BigDecimal num : numbers){
			stringNumbers.add(removeZeros(num.toString(), false, "."));
		}
		return stringNumbers;
	}
	void touchedUndo(){
		historySaver.goBack();
	}
	void touchedRedo(){
		historySaver.goFordward();
	}
	void touchedDigit(int d, boolean save){
		if(save){
			historySaver.saveDigits();
		}
		listDigits.add(Integer.valueOf(d));
		showDigitsWithZeros(listDigits, posDecimal);
		setWritingDigits(true);
	}
	void touchedDecimal(boolean save){
		if(save)
			historySaver.saveDigits();
		posDecimal = listDigits.size();
		if(listDigits.isEmpty()){
			tvDigits.setText("0" + getResources().getString(R.string.decimalSeparator));
			setWritingDigits(true);
			showOnly(scrollViewDigits);
		}else{
			showDigitsWithZeros(listDigits, posDecimal);
			setWritingDigits(true);
		}
	}
	void touchedDelete(boolean save){
		if(getShownView() == scrollViewError){// sera false siempre que se llame desde
										// delete.redo
			tvError.setText("");
			tvDigits.setText("");
			setWritingDigits(true);
			showOnly(scrollViewDigits);
			return;
		}
		String str = tvDigits.getText().toString();
		if(listDigits.size() == 0 && posDecimal == -1 && str.equals("") == false){
			if(save){
				historySaver.saveDelete(str);
			}
			tvDigits.setText("");
			setWritingDigits(true);
		}else if(str.length() > 0){
			char lastChar = str.charAt(str.length() - 1);
			int digit = Character.getNumericValue(lastChar);
			if(digit < 0){
				if(save)
					historySaver.saveDelete(posDecimal, true);
				posDecimal = -1;
			}else{
				if(save)
					historySaver.saveDelete(digit, false);
				listDigits.remove(listDigits.size() - 1);
			}
			showDigits(listDigits, posDecimal);
			setWritingDigits(true);
		}
	}
	boolean touchedEnter(boolean reEnterNumber, boolean save){
		boolean enteredDigits = false;
		if(listDigits.size() != 0 || posDecimal != -1){
			enteredDigits = true;
			BigDecimal number = buildNumber(listDigits, posDecimal);
			if(save)
				historySaver.saveEnter(listDigits, posDecimal, tvDigits.getText().toString(), number,
						reEnterNumber);
			listNumbers.add(number);
			showNumber(number);
			showDigits(number);
			setWritingDigits(false);
		}else if(listNumbers.size() != 0 && reEnterNumber == true){
			BigDecimal number = listNumbers.getLast();
			if(save)
				historySaver.saveEnter(listDigits, posDecimal, tvDigits.getText().toString(), number,
						reEnterNumber);
			listNumbers.add(number);
			showNumber(number);
		}
		posDecimal = -1;
		listDigits.clear();
		return enteredDigits;
	}
	void touchedPopNumber(boolean save){
		// if there are digits, just clear digits
		if(listDigits.size() != 0 || posDecimal != -1){
			if(save)
				historySaver.savePop(listDigits, posDecimal);
			touchedEnter(false, false);
			tvDigits.setText("");
			setWritingDigits(true);
			showOnly(scrollViewDigits);
			listNumbers.removeLast();
			removeLastNumber();
		}else if(listNumbers.size() != 0){
			if(save)
				historySaver.savePop(listNumbers.getLast());
			showOnly(scrollViewDigits);
			tvDigits.setText("");
			setWritingDigits(true);
			listNumbers.removeLast();
			removeLastNumber();
		}
	}
	void touchedClear(boolean save){
		if(save)
			historySaver.saveClear(listNumbers, listDigits, posDecimal, tvDigits.getText().toString(), getWritingDigits());
		listNumbers.clear();
		listDigits.clear();
		posDecimal = -1;
		tvDigits.setText("");
		setWritingDigits(true);
		showOnly(scrollViewDigits);
		layoutNumbers.removeViews(0, layoutNumbers.getChildCount());
	}
	void touchedSwitch(boolean save){
		if(listNumbers.size() >= 2){
			if(save)
				historySaver.saveSwitch();
			BigDecimal num2 = listNumbers.removeLast();
			BigDecimal num1 = listNumbers.removeLast();
			removeLastNumber();
			removeLastNumber();
			listNumbers.add(num2);
			listNumbers.add(num1);
			showNumber(num2);
			showNumber(num1);
			if(listDigits.size() == 0 && posDecimal == -1){
				showDigits(num1);
				setWritingDigits(false);
			}
		}
	}
	void touchedOperation(int argc, Operation operation){
		boolean enteredDigits = touchedEnter(false, true);
		int numberOfArguments = argc;
		if(listNumbers.size() < ((argc == -1)?1:argc)){
			showError(getResources().getString(R.string.notEnoughArguments));
		}else{
			if(argc == -1){
				numberOfArguments = listNumbers.size();
			}else if(argc == 0 && enteredDigits == true){
				numberOfArguments = 1;
			}else{
				numberOfArguments = argc;
			}
			BigDecimal[] operand = new BigDecimal[numberOfArguments];
			for(int i = numberOfArguments - 1; i >= 0; --i){
				operand[i] = listNumbers.removeLast();
			}
			BigDecimal result = BigDecimal.ZERO;//valor inicial no usado pero necesario
			String error = "";
			switch(argc){
			case -1:
				switch(operation){
				case SUMMATION:
					result = operand[0];
					for(int i=1; i<numberOfArguments; ++i){
						result = result.add(operand[i]).setScale(defaultScale, defaultRounding);
					}
					result = result.setScale(defaultScale, defaultRounding);
				break;
				case MEAN:
					result = operand[0];
					for(int i=1; i<numberOfArguments; ++i){
						result = result.add(operand[i]).setScale(defaultScale, defaultRounding);
					}
					result = result.divide(new BigDecimal(numberOfArguments), defaultScale, defaultRounding);
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
					result = result.multiply(operand[0]);
				}
				result = result.setScale(defaultScale, defaultRounding);
			break;
			case 1:
				switch(operation){
				case INVERSION:
					if(operand[0].compareTo(BigDecimal.ZERO) == 0){
						error = getResources().getString(R.string.divisionByZero);
					}else{
						result = BigDecimal.ONE.divide(operand[0], defaultScale, defaultRounding);
					}
				break;
				case SQUARE:
					result = operand[0].multiply(operand[0]).setScale(
							Math.min(defaultScale, operand[0].scale() * 2), defaultRounding);
				break;
				case NEGATIVE:
					result = operand[0].negate().setScale(defaultScale, defaultRounding);
				break;
				case SQUAREROOT:
					if(operand[0].compareTo(BigDecimal.ZERO) < 0){
						error = getResources().getString(R.string.negativeSquareRoot);
					}else{
						result = new BigDecimal(Math.sqrt(operand[0].doubleValue())).setScale(defaultScale,
								defaultRounding);
					}
				break;
				case SINE:
					if(angleMode.equals("deg")){
						operand[0] = new BigDecimal(Math.toRadians(operand[0].doubleValue()));
					}
					result = new BigDecimal(Math.sin(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
				break;
				case COSINE:
					if(angleMode.equals("deg")){
						operand[0] = new BigDecimal(Math.toRadians(operand[0].doubleValue()));
					}
					result = new BigDecimal(Math.cos(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
				break;
				case TANGENT:
					if(angleMode.equals("deg")){
						operand[0] = new BigDecimal(Math.toRadians(operand[0].doubleValue()));
					}
					result = new BigDecimal(Math.tan(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
				break;
				case ARCSINE:
					if(operand[0].compareTo(new BigDecimal("-1.0")) < 0
							|| operand[0].compareTo(new BigDecimal("1.0")) > 0){
						error = getResources().getString(R.string.arcsineOutOfRange);
					}else{
						result = new BigDecimal(Math.asin(operand[0].doubleValue())).setScale(defaultScale,
								defaultRounding);
						if(angleMode.equals("deg")){
							result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(defaultScale,
									defaultRounding);;
						}
					}
				break;
				case ARCCOSINE:
					if(operand[0].compareTo(new BigDecimal("-1.0")) < 0
							|| operand[0].compareTo(new BigDecimal("1.0")) > 0){
						error = getResources().getString(R.string.arccosineOutOfRange);
					}else{
						result = new BigDecimal(Math.acos(operand[0].doubleValue())).setScale(defaultScale,
								defaultRounding);
						if(angleMode.equals("deg")){
							result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(defaultScale,
									defaultRounding);;
						}
					}
				break;
				case ARCTANGENT:
					result = new BigDecimal(Math.atan(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
					if(angleMode.equals("deg")){
						result = new BigDecimal(Math.toDegrees(result.doubleValue())).setScale(defaultScale,
								defaultRounding);;
					}
				break;
				case LOG10:
					if(operand[0].compareTo(BigDecimal.ZERO) <= 0){
						error = getResources().getString(R.string.logOutOfRange);
					}else{
						result = new BigDecimal(Math.log(operand[0].doubleValue()) / Math.log(10.0)).setScale(
								defaultScale, defaultRounding);
					}
				break;
				case DTOR:
					result = new BigDecimal(Math.toRadians(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
				break;
				case RTOD:
					result = new BigDecimal(Math.toDegrees(operand[0].doubleValue())).setScale(defaultScale,
							defaultRounding);
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			case 2:
				switch(operation){
				case ADDITION:
					result = operand[0].add(operand[1]).setScale(defaultScale, defaultRounding);
				break;
				case MULTIPLICATION:
					result = operand[0].multiply(operand[1]).setScale(defaultScale, defaultRounding);
				break;
				case EXPONENTIATION:
					result = new BigDecimal(Math.pow(operand[0].doubleValue(), operand[1].doubleValue()))
							.setScale(defaultScale, defaultRounding);
				break;
				case SUBTRACTION:
					result = operand[0].subtract(operand[1]).setScale(defaultScale, defaultRounding);
				break;
				case DIVISION:
					if(operand[1].compareTo(BigDecimal.ZERO) == 0){
						error = getResources().getString(R.string.divisionByZero);
					}else{
						result = operand[0].divide(operand[1], defaultScale, defaultRounding);
					}
				break;
				case LOGXY:
					if(operand[0].compareTo(BigDecimal.ZERO) <= 0
							|| operand[1].compareTo(new BigDecimal("0.0")) <= 0){
						error = getResources().getString(R.string.logOutOfRange);
					}else{
						result = new BigDecimal(Math.log(operand[1].doubleValue())
								/ Math.log(operand[0].doubleValue())).setScale(defaultScale, defaultRounding);
					}
				break;
				case HYPOTENUSE_PYTHAGORAS:
					if(operand[0].compareTo(BigDecimal.ZERO) < 0 || operand[1].compareTo(BigDecimal.ZERO)< 0){
						error = getResources().getString(R.string.sideCantBeNegative);
					}else{
						result = new BigDecimal(
								Math.sqrt(
								operand[0].pow(2).add(operand[1].pow(2)).doubleValue()))
								.setScale(defaultScale,	defaultRounding);
					}
				break;
				case LEG_PYTHAGORAS:
					if(operand[0].compareTo(BigDecimal.ZERO) < 0 || operand[1].compareTo(BigDecimal.ZERO)< 0){
						error = getResources().getString(R.string.sideCantBeNegative);
					}else{
						BigDecimal hyp = operand[0].max(operand[1]);
						BigDecimal leg = operand[0].min(operand[1]);
						result = new BigDecimal(
								Math.sqrt(
								hyp.pow(2).subtract(leg.pow(2)).doubleValue()))
								.setScale(defaultScale,	defaultRounding);
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
					BigDecimal p = operand[0].add(operand[1]).add(operand[2]).divide(new BigDecimal("2.0"));
					BigDecimal q = p.multiply(p.subtract(operand[0])).multiply(p.subtract(operand[1]))
							.multiply(p.subtract(operand[2]));
					if(q.compareTo(BigDecimal.ZERO) < 0){
						error = getResources().getString(R.string.notATriangle);
					}else{
						result = new BigDecimal(Math.sqrt(q.doubleValue())).setScale(defaultScale, defaultRounding);
					}
				break;
				default:
					error = getResources().getString(R.string.operationNotImplemented);
				break;
				}
			break;
			}
			if(error.equals("") == false){
				showError(error);
				for(int i = 0; i < argc; ++i){
					listNumbers.add(operand[i]);
				}
			}else{
				historySaver.saveOperation(numberOfArguments, operand, tvDigits.getText().toString(), result);
				for(int i = 0; i < numberOfArguments; ++i){
					removeLastNumber();
				}
				listNumbers.add(result);
				showNumber(result);
				showDigits(result);
				setWritingDigits(false);
			}
		}
	}
	String removeZeros(String string, boolean keepTrailingZeros, String decimalSeparator){
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
	String buildString(List <Integer> listDigits, int pos, boolean keepTrailingZeros, String decimalSeparator){
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
	BigDecimal buildNumber(List <Integer> listDigits, int pos){
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
		layoutNumbers.addView(tv);
		scrollToRight(scrollViewNumbers);
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
		tvDigits.setText(buildString(digits, decimal, false, getResources().getString(R.string.decimalSeparator)));
		showOnly(scrollViewDigits);
	}
	void showDigitsWithZeros(List <Integer> digits, int decimal){
		tvDigits.setText(buildString(digits, decimal, true, getResources().getString(R.string.decimalSeparator)));
		showOnly(scrollViewDigits);
	}
	void showDigits(BigDecimal number){
		tvDigits.setText(localizeDecimalSeparator(removeZeros(number.toString(), false, ".")));
		showOnly(scrollViewDigits);
	}
	void removeLastNumber(){
		if(layoutNumbers.getChildCount() != 0){
			layoutNumbers.removeViewAt(layoutNumbers.getChildCount() - 1);
		}
	}
	void showError(String str){
		tvError.setText(getResources().getString(R.string.error) + ": " + str);
		showOnly(scrollViewError);
	}
	void showOnly(View view){
		if(view == scrollViewDigits){
			showView(scrollViewError, false);
			scrollToRight(scrollViewDigits);
		}else if(view == scrollViewError){
			showView(scrollViewDigits, false);
		}
		showView(view, true);
	}
	void showView(View view, boolean show){
		if(show){
			view.setVisibility(View.VISIBLE);
		}else{
			view.setVisibility(View.GONE);
		}
	}
	void setWritingDigits(boolean show){
		if(show){
			tvWritingDigits.setVisibility(View.VISIBLE);
		}else{
			tvWritingDigits.setVisibility(View.INVISIBLE);
		}
	}
	int getWritingDigits(){
		return tvWritingDigits.getVisibility();
	}
	View getShownView(){
		if(scrollViewError.getVisibility() == View.VISIBLE){
			return scrollViewError;
		}else if(scrollViewDigits.getVisibility() == View.VISIBLE){
			return scrollViewDigits;
		}else{
			return null;
		}
	}
	void showAngleMode(){
		if(layoutUsesViewAnimator == false){
			tvAngleMode.setVisibility(View.VISIBLE);
		}else if(switcherFunctions.getDisplayedChild() == 1){
			tvAngleMode.setVisibility(View.VISIBLE);
		}else{
			tvAngleMode.setVisibility(View.INVISIBLE);
		}
	}
	class Scroller implements Runnable{
		HorizontalScrollView v;
		Scroller(HorizontalScrollView v){
			this.v = v;
		}
		public void run(){
			v.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
		}
	}
	void scrollToRight(HorizontalScrollView v){
		if(v != null){
			v.post((Runnable) new Scroller(v));
		}
	}
	
	void toast(String s){
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}
}
