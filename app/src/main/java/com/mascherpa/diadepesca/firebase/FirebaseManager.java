package com.mascherpa.diadepesca.firebase;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mascherpa.diadepesca.R;
import com.mascherpa.diadepesca.databinding.MainBinding;
import com.mascherpa.diadepesca.load.Loading;

public class FirebaseManager {
    FirebaseDatabase database;
    FirebaseAuth auth;
    FirebaseUser user;
    Context context;
    String client_id;

    public String nameUser;
    MainBinding managerUI;
    public FirebaseManager(Context getContexts, String getClient_id, MainBinding binding){
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        context = getContexts;
        client_id = getClient_id;
        managerUI = binding;
        getNameUser();
        getImageUser();

    }

    private void getImageUser() {//Obtengo referencia a base de datos, para obtener el link de la imagen
        DatabaseReference imageRef = database.getReference("users").child(auth.getUid()).child("profile");
        imageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Obtener el valor de la clave "name"
                String linkimage = dataSnapshot.getValue(String.class);
                Glide.with(context)//usando glide, seteo la imagen de usuario del gmail
                        .load(linkimage)
                        .placeholder(R.drawable.refreshwidget)
                        .error(R.drawable.app_widget_background)
                        .transform(new CircleCrop())
                        .into(managerUI.imageUser);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar errores de lectura de datos, si es necesario
                Log.e("Error", "Error al leer el valor de la clave 'name': " + databaseError.getMessage());
            }
        });

    }

    public void getNameUser(){//Obtengo ref a la base de datos, para obtener el nombre de usuario y setearlo al inicio
        DatabaseReference userRef = database.getReference("users").child(auth.getUid()).child("name");
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Obtener el valor de la clave "name"
                nameUser = dataSnapshot.getValue(String.class);
                managerUI.tvNombreUser.setText("Hola "+nameUser+"!!");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Error", "Error al leer el valor de la clave 'name': " + databaseError.getMessage());
            }
        });
    }
    public void deleteAccount(Context context) { //eliminar cuenta, obtengo refencia al usuario, lo borro de la base de datos y luego la auth, y vuelvo a la activity primera
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.delete()
                                    .addOnCompleteListener(taskDelete -> {
                                        if (taskDelete.isSuccessful()) {
                                            // El usuario se eliminó correctamente
                                            FirebaseAuth.getInstance().signOut();
                                            clearCacheGoogle();
                                            Intent intent = new Intent(context, Loading.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//cierro tareas
                                            context.startActivity(intent);
                                        } else {
                                            // Error al eliminar el usuario
                                            showMessage("Error al eliminar el usuario: " + taskDelete.getException().getMessage());
                                        }
                                    });
                        } else {
                            // Error al eliminar los datos del usuario de la base de datos
                            showMessage("Error al eliminar el usuario de la base de datos: " + task.getException().getMessage());
                        }
                    });
        } else {
            showMessage("El usuario no está autenticado");
        }
    }


    public void clearCacheGoogle(){ //creo un cliente sign in para limpiar el cacehe y estado de sesion
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(client_id)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        mGoogleSignInClient.signOut();
    }

    public void signOut(Context context) {//cierro sesion, limpio tareas y vuelvo a la activity de inicio
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth != null) {
            auth.signOut();
            clearCacheGoogle();
            Intent intent = new Intent(context, Loading.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//clear tasks
            context.startActivity(intent);
        } else {
            showMessage("Error: FirebaseAuth instance is null");
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }




}
