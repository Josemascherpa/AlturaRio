package com.mascherpa.diadepesca;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.firebase.auth.FirebaseAuth;

import com.mascherpa.diadepesca.FavouriteRio.MySharedPreferences;
import com.mascherpa.diadepesca.UI.ManagerUIMain;
import com.mascherpa.diadepesca.data.Rio;

import com.mascherpa.diadepesca.databinding.MainBinding;
import com.mascherpa.diadepesca.firebase.FirebaseManager;
import com.mascherpa.diadepesca.load.Loading;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView altura_tv;
    TextView variacion_tv;
    TextView fecha_tv;
    ImageView flecha_iv;
    LottieAnimationView favButton;
    AutoCompleteTextView buscaRios_ATV;
    TextView nombreRio;
    Integer rioSave = null;
    private XYPlot plot;

    LottieAnimationView compartirAltura;


    //////////////////////////////////

    private List<Rio> _Rios;
    String emailUser;
    private ManagerUIMain managerUI;

    private MainBinding mainActivityBinding;
    private FirebaseManager firebaseManager;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityBinding = MainBinding.inflate(getLayoutInflater());
        setContentView(mainActivityBinding.getRoot());
        firebaseManager = new FirebaseManager(getApplicationContext(),getString(R.string.client_id),mainActivityBinding);

        setButtonDeleteAccount();
        setButtonSignOut();
        ChangeName();
        BarWindowBlack();

        //Revisar de hacerlo en el manager ui y lo de abajo
        InitAndCallsManagerUI();

//        ButtonSharedFriends();
//        AutocompleteFilling();
//        EditUIWithAutocomplete();
//        FavRio();
    }
    private void ChangeName(){
        mainActivityBinding.changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseManager.changeNameDatabase(mainActivityBinding.editTextName.getText().toString());
            }
        });
    }
    private void setButtonSignOut(){
        mainActivityBinding.signuout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                firebaseManager.ClearCacheGoogle();
                finish();
                Intent intent = new Intent(getApplicationContext(), Loading.class);
                startActivity(intent);
            }
        });
    }

    private void setButtonDeleteAccount(){
        mainActivityBinding.deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseManager.deleteAccount(FirebaseAuth.getInstance().getUid());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Intent intent = new Intent(this, Loading.class);
        startActivity(intent);
    }

    //ZOOM FIXED
    @Override
    protected void attachBaseContext(Context newBase) {
        final Configuration configuration = newBase.getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final DisplayMetrics displayMetrics = newBase.getResources().getDisplayMetrics();
            if (displayMetrics.densityDpi != DisplayMetrics.DENSITY_DEVICE_STABLE) {
                // Current density is different from Default Density. Override it
                configuration.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
            }
        }
        configuration.fontScale = 1.0f;
        Context newContext = newBase.createConfigurationContext(configuration);
        super.attachBaseContext(newContext);
    }



    private void IdsTextViewsRio() {
//        plot = (XYPlot) findViewById(R.id.plot);
//        flecha_iv = (ImageView) findViewById(R.id.f_variacion);
//        altura_tv = (TextView) findViewById(R.id.tv_altura);
//        variacion_tv = (TextView) findViewById(R.id.tv_variacion);
//        fecha_tv = (TextView) findViewById(R.id.tv_fechaUltimoRegistro);
//        compartirAltura = (LottieAnimationView) findViewById(R.id.compartir_altura);
//        buscaRios_ATV = (AutoCompleteTextView) findViewById(R.id.ac_tv);

//        favButton = (LottieAnimationView) findViewById(R.id.id_fav);
        favButton.setVisibility(View.GONE);
    }
    private void CreateGraphs(Float[] flpoints, String[] fechaBottom) {
        plot.clear();
        //puntos en el grafico
        XYSeries s1 = new SimpleXYSeries(SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "", flpoints);

        //tamaño etiqueta en los puntos
        PointLabelFormatter plf = new PointLabelFormatter();
        plf.getTextPaint().setTextSize(PixelUtils.spToPix(12));

        //Seteo de lineas colores y puntos e colores
        LineAndPointFormatter format = new LineAndPointFormatter(Color.rgb(16, 157, 249), Color.rgb(255, 255, 255), null, null);
        format.setPointLabelFormatter(plf);
        format.getLinePaint().setStrokeWidth(3);

        //los agrego al grafico
        plot.addSeries(s1, format);

        //seteo de rangos de etiquetas
        plot.setRangeBoundaries(0, BoundaryMode.FIXED, getLargestNumber(flpoints), BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 9.5, BoundaryMode.FIXED);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setRotation(10);
        plot.getGraph().getLineLabelInsets().setBottom(PixelUtils.dpToPix(4));

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
                int index = ((Number) object).intValue();
                return buffer.append(fechaBottom[index]);
            }

            @Override
            public Object parseObject(String string, ParsePosition position) {
                return null;
            }

        });

        //modificaciones titulo
        plot.setTitle("Historial Alturas");
        plot.getTitle().setMarginTop(55f);
        plot.getTitle().getLabelPaint().setTextSize(PixelUtils.spToPix(13));
        plot.getTitle().getLabelPaint().setTypeface(Typeface.DEFAULT_BOLD);

        //lineas del grafico y transparencia en grilla
        plot.getGraph().getDomainOriginLinePaint().setColor(Color.rgb(255, 255, 255));
        plot.getGraph().getDomainOriginLinePaint().setStrokeWidth(1.5f);
        plot.getGraph().getRangeOriginLinePaint().setColor(Color.rgb(255, 255, 255));
        plot.getGraph().getRangeOriginLinePaint().setStrokeWidth(1.5f);
        plot.getGraph().setRangeGridLinePaint(null);
        plot.getGraph().setDomainGridLinePaint(null);

        plot.redraw();


    }


    private void ButtonSharedFriends() {
        compartirAltura.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String infoEnviar = nombreRio.getText()+  "\nAltura Actual: " + altura_tv.getText() + "Mts" + " \nVariacion: " + variacion_tv.getText() + " \nUltima Actualizacion: " + fecha_tv.getText();
                sendIntent.putExtra(Intent.EXTRA_TEXT, infoEnviar);
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });

    }

    private void BarWindowBlack() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(MainActivity.this.getResources().getColor(R.color.black));
    }

    private void InitAndCallsManagerUI(){
        managerUI = new ManagerUIMain(mainActivityBinding,getApplicationContext());
        RecoveryIntentLoading();
        managerUI.AutocompleteFilling();


    }
    private void RecoveryIntentLoading() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            managerUI._Rios = (List<Rio>) bundle.getSerializable("listaRios");
            Log.i("hola", String.valueOf(managerUI._Rios.size()));
        }
    }





    private void DirectionAndColorArrow(){
        if(!variacion_tv.getText().toString().equals("- Mts") && Float.parseFloat(variacion_tv.getText().toString().substring(0,5))<0){
            variacion_tv.setTextColor(Color.parseColor("#D24545"));
            flecha_iv.setImageResource(R.drawable.fbajando);
            flecha_iv.setImageAlpha(255);
        }else if(!variacion_tv.getText().toString().equals("- Mts") && Float.parseFloat(variacion_tv.getText().toString().substring(0,5))>0){//
            variacion_tv.setTextColor(Color.parseColor("#557C55"));
            flecha_iv.setImageResource(R.drawable.fsubiendo);
            flecha_iv.setImageAlpha(255);
        }else if(variacion_tv.getText().toString().equals("- Mts")){
            variacion_tv.setTextColor(Color.parseColor("#DAD6D6"));
            flecha_iv.setImageAlpha(0);
        }
    }



    private float getLargestNumber(Float[] arrayfloat){
        Float largestNumber=10f;
        for(int i=0;i<arrayfloat.length;i++){
            if(arrayfloat[i]>largestNumber){
                largestNumber=arrayfloat[i]+5f;
            }
        }
        return largestNumber;
    }

    private void FavRio(){
        favButton.setOnClickListener(v -> {
            favButton.playAnimation();
            MySharedPreferences.saveInteger(this,rioSave);
            Toast.makeText(this,"Agregado a favoritos!!",Toast.LENGTH_LONG).show();
//            favButton.setVisibility(View.GONE);

        });
        favButton.addAnimatorListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(@NonNull Animator animation) {


            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
//                favButton.pauseAnimation();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
    }

    private void EmptyDates(){
        altura_tv.setVisibility(View.GONE);
        plot.setVisibility(View.GONE);
        compartirAltura.setVisibility(View.GONE);
        Toast.makeText(this,"Datos no registrados",Toast.LENGTH_LONG).show();
    }
    private void VisibilityDates(){
        altura_tv.setVisibility(View.VISIBLE);
        plot.setVisibility(View.VISIBLE);
        compartirAltura.setVisibility(View.VISIBLE);
    }



}




