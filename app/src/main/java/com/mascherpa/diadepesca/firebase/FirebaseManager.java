package com.mascherpa.diadepesca.firebase;

import static android.provider.Settings.System.getString;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mascherpa.diadepesca.R;
import com.mascherpa.diadepesca.load.Loading;

public class FirebaseManager {
    FirebaseDatabase database;
    FirebaseAuth auth;
    FirebaseUser user;
    Context context;
    String client_id;
    public FirebaseManager(Context getContexts, String getClient_id){
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        context = getContexts;
        client_id = getClient_id;
    }
    public void changeNameDatabase(String name){
        DatabaseReference userRef = database.getReference("users").child(auth.getUid()).child("name");
        userRef.setValue(name);
        showMessage("nombre cambiado con exito!");
    }

    public void deleteAccount(String userID) {
        if (user != null) {
            // Intenta eliminar la cuenta
            user.delete()
                    .addOnCompleteListener(taskDelete -> {
                        if (taskDelete.isSuccessful()) {
                            DatabaseReference userRef = database.getReference("users").child(userID);
                            userRef.removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {//la cuenta se elimino de la bd
                                    //redirige a una nueva Activity
                                    Intent intent = new Intent(context, Loading.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    context.startActivity(intent);
                                } else {//la cuenta no se pudo elimino de la bd
                                    showMessage("Error al eliminar el usuario de la base de datos: " + task.getException().getMessage());
                                }
                            });
                            FirebaseAuth.getInstance().signOut();
                            //Limpiola autenticacion de persistencia de datos de auth para que se pueda volver a elegir cuenta
                            ClearCacheGoogle();
                        } else {//error al eliminar la cuenta
                            showMessage("Error al eliminar la cuenta: " + taskDelete.getException().getMessage());
                        }
                    });
        } else {
            showMessage("El usuario no está autenticado");
        }
    }

    public void ClearCacheGoogle(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(client_id)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        mGoogleSignInClient.signOut();
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
