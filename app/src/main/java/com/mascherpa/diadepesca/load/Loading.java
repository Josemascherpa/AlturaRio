package com.mascherpa.diadepesca.load;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mascherpa.diadepesca.R;
import com.mascherpa.diadepesca.UI.ManagerUILoading;
import com.mascherpa.diadepesca.data.Rio;
import com.mascherpa.diadepesca.databinding.LoadinguiBinding;
import com.mascherpa.diadepesca.network.CheckInternet;
import com.mascherpa.diadepesca.network.DataProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class Loading extends AppCompatActivity {

    private DataProvider recoveryData;

    private LoadinguiBinding binding;

    private ManagerUILoading managerUI;
    CheckInternet checkInternet;


    //Firebase

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private GoogleSignInClient mGoogleSignInClient;

    String email;
    private static final int RC_SIGN_IN = 20;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LoadinguiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //hago ping a google para verificar si tengo internet o no
        checkInternet = new CheckInternet();
        if(checkInternet.isOnline() ){

            database = FirebaseDatabase.getInstance();//guardo instancia a base de datos
            auth = FirebaseAuth.getInstance();//guardo instancia de auth
            managerUI = new ManagerUILoading(binding, this,this);//creo instancia de manager ui

            FirebaseUser currentUser = auth.getCurrentUser();//obtengo el usuario actual
            if (currentUser != null) {
                showLoadingIntent();//Muestro ui cargando
                validateUser(currentUser.getUid(), exist -> {//Valido usuario
                    if (exist) {//Existe usuario
                        recoveryDataRiosAndStartMain();//scrapeo pagina
                    }else{//no existe usuario
                        userNotLoadedInDB();//desconecto usuario, y logueo o registro
                    }
                });
            } else {
                loginOrRegister();//logeo o registro
            }
        }else{
            showMessage("no tienes internet. Por favor reinicia la app.");
        }

    }

    private void showLoadingIntent(){//UI cargando para escenas donde se necesite
        binding.overlayLoading.setVisibility(View.VISIBLE);
        managerUI.closeBottomSheetBehavior();//cierro sheetbehaviour
    }

    private void userNotLoadedInDB(){//desconecto usuario, y logueo o registro
        auth.signOut();
        loginOrRegister();
    }

    private void loginOrRegister(){
        GoogleSignInOptions gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) //configuro inicio de sesion
                .requestIdToken(getString(R.string.client_id))//id token del sv
                .requestEmail().build();//pido el email del usuario
        mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(),gso);//obtengo el cliente de inicio de sesion
        // Set OnClickListener for login button
        binding.signupGmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();//inicio sesion
            }
        });
    }

    private void googleSignIn() {//inicio interfaz de inicio de sesion de google
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent,RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){//Verifico inicio sesion google
            showLoadingIntent();//Muestro ui de carga
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {//Obtengo la cuenta de google desde la task
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());//autentico usuario con firebase

            }catch (ApiException e){//manej excepciones
                int statusCode = e.getStatusCode();
                switch (statusCode) {
                    case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                        showMessage("Sign in cancelled");
                        break;
                    case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                        showMessage("Sign in failed");
                        break;
                    case GoogleSignInStatusCodes.NETWORK_ERROR:
                        showMessage("Network error");
                        break;
                    case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                        showMessage("Invalid account");
                        break;
                    case GoogleSignInStatusCodes.ERROR:
                        showMessage("Error code 10: Configuration issue");
                        break;
                    default:
                        showMessage("Error code: " + statusCode + " - " + e.getMessage());
                        break;
                }
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);//Obtengo credencia
        auth.signInWithCredential(credential)//Autentico
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();//obtengo usuario
                        HashMap<String, Object> map = new HashMap<>();//almaceno datos del usuario
                        map.put("id", user.getUid());
                        map.put("name", user.getDisplayName());
                        map.put("profile", user.getPhotoUrl().toString());
                        // Llamamos a validateUser y le pasamos el userUid y un Listener para manejar el resultado
                        validateUser(user.getUid(), exist -> {
                            if (exist) {
                                // El usuario existe, continuar con el flujo normal
                                recoveryDataRiosAndStartMain();
                            } else {
                                // El usuario no existe, crear una nueva cuenta
                                createNewAccount(map);//creo nueva cuenta pasando el map
                            }
                        });
                    } else {
                        showMessage("error");
                    }
                });
    }


    // Modificamos validateUser para que acepte un Listener para manejar el resultado
    private void validateUser(String firebaseUID, OnUserValidationListener listener) {

        if (database != null) {
            DatabaseReference usersRef = database.getReference("users");//referencia a base de datos usuarios
            Query query = usersRef.orderByChild("id").equalTo(firebaseUID); // mando una query para verificar que exista el id
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean exist = dataSnapshot.exists(); // verifico si el usuario existe y devuelvo true or false
                    listener.onUserValidation(exist); // notifico el resultado al Listener

                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showMessage("Error en la consulta: " + error.getMessage());
                }
            });
        }
    }

    // Interfaz para manejar el resultado de la validación del usuario
    interface OnUserValidationListener {
        void onUserValidation(boolean exist);
    }

    //metodo para crear una nueva cuenta en la base de datos
    private void createNewAccount(HashMap<String, Object> userData) {
        DatabaseReference usersRef = database.getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            usersRef.child(user.getUid()).setValue(userData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            recoveryDataRiosAndStartMain();//obtengo data
                        } else {
                            showMessage("Error al crear la cuenta en la base de datos");
                        }
                    });
        } else {
            showMessage("El usuario no está autenticado");
        }
    }


    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event){//Metodo para cuando se toque fuera del area del bottomsheets se cierre, obteniendo el rect que ocupa la barra
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (managerUI.returnStateBottomSheetsSignUp()== BottomSheetBehavior.STATE_EXPANDED) {
                Rect outRect = new Rect();
                binding.standardBottomSheet.getGlobalVisibleRect(outRect);

                if(!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    managerUI.getBottomSheetsSign().setState(BottomSheetBehavior.STATE_COLLAPSED);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    public void recoveryDataRiosAndStartMain(){
        recoveryData = new DataProvider("https://contenidosweb.prefecturanaval.gob.ar/alturas/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Rio> data = recoveryData.loadDataRio();//Screapeo en otro hilo para no trabar la ui
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {//cargo main activity ya con ls datos guardados y los mando a la main
                        Class MainActivity = com.mascherpa.diadepesca.MainActivity.class;
                        Intent intent = new Intent(Loading.this,MainActivity);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("listaRios",(Serializable)data);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void attachBaseContext(Context newBase) {//ajusto config de densiidad de pantalla y la escala de la fuente, por si el telefono la tiene editada y se vea mal
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


}

