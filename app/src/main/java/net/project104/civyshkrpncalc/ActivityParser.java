package net.project104.civyshkrpncalc;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import net.project104.infixparser.Calculator;
import net.project104.infixparser.RawText;

public class ActivityParser extends Activity {

    private ImageButton btDelete, btAccept;
    private TextView tvResult;
    private EditText etInput;
    private Calculator calc;

    private View.OnClickListener deleteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            tvResult.setText("");
            etInput.setText("");
            enableAccept(true);
        }
    };

    public ActivityParser(){
        calc = new Calculator();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parser);

        btDelete = (ImageButton) findViewById(R.id.butDelete);
        btAccept = (ImageButton) findViewById(R.id.butAccept);
        tvResult = (TextView) findViewById(R.id.tvResult);
        etInput = (EditText) findViewById(R.id.etInput);

        btDelete.setOnClickListener(deleteListener);

        btAccept.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String result = tvResult.getText().toString();
                if(result.isEmpty()){
                    setResult(RESULT_CANCELED);
                }else {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("Result", result);
                    setResult(Activity.RESULT_OK, returnIntent);
                }
                finish();
            }
        });

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    tvResult.setText("");
                    enableAccept(true);
                    return;
                }

                RawText raw = new RawText(s.toString(), calc);
                try {
                    String result = raw.getValue().toString();
                    tvResult.setText(result);
                    enableAccept(true);
                }catch(Exception e) {
                    enableAccept(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                ;;
            }
        });


        Bundle extras = getIntent().getExtras();
        if(extras != null){
            etInput.setText(extras.getString("Input"));
        }

    }

    private void enableAccept(boolean enable) {
        btAccept.setEnabled(enable);
        if (Build.VERSION.SDK_INT >= 16) {
            btAccept.setImageAlpha(enable ? 255 : 128);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        etInput.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}
