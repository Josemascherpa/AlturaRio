package com.mascherpa.diadepesca.UI;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mascherpa.diadepesca.CustomAutocompleteEditText.CustomAutoCompleteAdapter;
import com.mascherpa.diadepesca.FavouriteRio.MySharedPreferences;
import com.mascherpa.diadepesca.R;
import com.mascherpa.diadepesca.data.Rio;
import com.mascherpa.diadepesca.databinding.MainBinding;
import com.mascherpa.diadepesca.firebase.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class ManagerUIMain {
    Integer rioSave = null;
    private MainBinding binding;
    public List<Rio> _Rios = new ArrayList<>();
    private Context contextMain;

    public ManagerUIMain(MainBinding bindingMain, Context context, FirebaseManager firebaseManager){
        this.binding = bindingMain;
        contextMain=context;
        buttonSharedFriends();
        clickImageUser(firebaseManager);
        setInformationRio("Información Rio");
        clickedNameRio();

    }
    public void autocompleteFilling() {//Relleno del buscador con lo scrapeado anteriormente
        List<String> arrayNombreRios = new ArrayList<>();
        for (int i = 0; i < _Rios.size(); i++) {
            String nombreAutocompletado = _Rios.get(i).getNombre() + " " + _Rios.get(i).getPuerto();
            arrayNombreRios.add(nombreAutocompletado);
        }//Guardo en el array todos los rios
        CustomAutoCompleteAdapter adapter = new CustomAutoCompleteAdapter(contextMain, arrayNombreRios);
        binding.buscaRio.setAdapter(adapter);
        binding.buscaRio.setThreshold(1); // Configura el número mínimo de caracteres antes de que se muestren sugerencias
        editUIWithAutocomplete();
        favRio();
    }

    private void editUIWithAutocomplete() {
        //If I haven't saved anything yet, the integer won't exist, which means GetRioFavs() will return 0, but since a river exists with 0, I set it to return 9999.
        if(MySharedPreferences.getRioFavs(contextMain)!=9999){//Si tengo un rio en favorito, lo muestro para que aparezca primero

            binding.favAnimation.setVisibility(View.INVISIBLE);
            for(int i=0;i<_Rios.size();i++){
                if(i==MySharedPreferences.getRioFavs(contextMain)){
                    Rio rio = _Rios.get(i);
                    binding.alturaRio.setText(rio.getAltura());
                    binding.variacionRio.setText(rio.getVariacion() + " Mts");
                    binding.fechaTv.setText(sortedDateTV(rio.getFecha()));
                    binding.nombreRio.setText(rio.getNombre() + "(" + rio.getPuerto() + ")");
                    rioSave = i;
                }
            }
            directionAndColorArrow();//cambio color y flecha dependiendo la altura
            linearInformationVisible();//Seteo para ver si tiene que mostrarse o no
            setInformationRio("Información Rio");
        }else{
            linearInformationOcult();//Oculto linear informacion para que se elija algun rio
            setInformationRio("Selecciona un Rio");
        }

        //Set the UI for when a river is selected from the Spinner.
        binding.buscaRio.setOnItemClickListener((parent, view, position, id) -> {//Cuando selecciono algun rio
            String rioSeleccionado = (String) parent.getItemAtPosition(position);
//            favButton.setVisibility(View.VISIBLE);
            for (int i = 0; i < _Rios.size(); i++) {
                if (((_Rios.get(i).getNombre()+ _Rios.get(i).getPuerto()).replace(" ", "")).equals((rioSeleccionado.replace(" ", "")))) {
                    if(i!=MySharedPreferences.getRioFavs(contextMain)){
                        binding.favAnimation.setVisibility(View.VISIBLE);
                        binding.favAnimation.setProgress(0);

                    }else{
                        binding.favAnimation.setVisibility(View.INVISIBLE);
                    }

                    rioSave = i;//Guardo en una variable para cuando presione favorito
                    Rio rio = _Rios.get(i);

                    binding.alturaRio.setText(rio.getAltura());
                    binding.variacionRio.setText(rio.getVariacion() + " Mts");
                    binding.fechaTv.setText(sortedDateTV(rio.getFecha()));
                    binding.nombreRio.setText(rio.getNombre() + "(" + rio.getPuerto() + ")");

                }
            }
            directionAndColorArrow();
            //Clear focus and clear keyboard
            binding.buscaRio.clearFocus();//Saco el focus en el buscador
            InputMethodManager imm = (InputMethodManager) contextMain.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.buscaRio.getWindowToken(), 0);
            linearInformationVisible();//muestro info
            setInformationRio("Información Rio");
        });
    }

    private String sortedDateTV(String fecha){//Configuracion de como debe mostrarse la fecha
        String fechaReturn = fecha;
        String hora = fechaReturn.substring(fechaReturn.length()-4);
        hora = hora.substring(0,2)+":"+hora.substring(2);
        fechaReturn = fechaReturn.substring(0,fechaReturn.length()-4)+hora;
        fechaReturn = fechaReturn.replaceAll("/24-", " ");
        fechaReturn = fechaReturn.replace("/","-");
        if(fechaReturn.contains("12:00")){
            fechaReturn=fechaReturn+" pm";
        }else{
            fechaReturn=fechaReturn+" am";
        }
        return fechaReturn;
    }

    private void directionAndColorArrow(){//Color texto y que flecha mostrar en base a la altura
        if(!binding.variacionRio.getText().toString().equals("- Mts") && Float.parseFloat(binding.variacionRio.getText().toString().substring(0,5))<0){
            binding.variacionRio.setTextColor(Color.parseColor("#D24545"));
            binding.flechaIv.setImageResource(R.drawable.fbajando);
            binding.flechaIv.setImageAlpha(255);
        }else if(!binding.variacionRio.getText().toString().equals("- Mts") && Float.parseFloat(binding.variacionRio.getText().toString().substring(0,5))>0){//
            binding.variacionRio.setTextColor(Color.parseColor("#28B725"));
            binding.flechaIv.setImageResource(R.drawable.fsubiendo);
            binding.flechaIv.setImageAlpha(255);
        }else if(binding.variacionRio.getText().toString().equals("- Mts")){
            binding.variacionRio.setTextColor(Color.parseColor("#DAD6D6"));
            binding.flechaIv.setImageAlpha(0);
        }
    }

    public void buttonSharedFriends() {//Boton compartir
        binding.compartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                if(rioSave!=null){//Tengo seleccionado un rio
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    String infoEnviar = binding.nombreRio.getText() + "\nAltura Actual: " + binding.alturaRio.getText() + "Mts" + " \nVariacion: " + binding.variacionRio.getText() + " \nUltima Actualizacion: " + binding.fechaTv.getText();
                    sendIntent.putExtra(Intent.EXTRA_TEXT, infoEnviar);
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    if (context != null) {
                        context.startActivity(shareIntent);
                    }
                }else{
                    showMessage("Por favor, selecciona un rio",context);
                }

            }
        });

    }
    private void showMessage(String message,Context context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void favRio(){//Manejo del boton favorito
        binding.favAnimation.setOnClickListener(v -> {
            if(rioSave!=null){
                binding.favAnimation.playAnimation();
                MySharedPreferences.saveInteger(contextMain,rioSave);//guardo el numero de rio en el shared preferences
                showMessage("Agregado a favoritos!!",contextMain);
            }else{
                showMessage("Por favor, selecciona un rio",contextMain);
            }

        });
        binding.favAnimation.addAnimatorListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(@NonNull Animator animation) {


            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                binding.favAnimation.setVisibility(View.INVISIBLE);//Termina la animacion y oculto el boton
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
    }

    private void clickImageUser(FirebaseManager firebaseManager){
        binding.imageUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(v.getContext(), v);//Creo popup para el desconectar o eliminar cuenta
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());//hago el inflate
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.logout_user) {//Si presiono logout cierro sesion
                            firebaseManager.signOut(v.getContext());
                        } else if (itemId == R.id.delete_user) {//Si presiono delete borro cuenta
                            firebaseManager.deleteAccount(v.getContext());
                        }
                        return true;
                    }
                });
                popup.show();//muestro popup
            };
        });
    }

    private void linearInformationVisible(){
        binding.linearInformacionRio.setVisibility(View.VISIBLE);
    }
    private void linearInformationOcult(){
        binding.linearInformacionRio.setVisibility(View.GONE);
    }
    private void setInformationRio(String string){
        binding.informacionRio.setText(string);
    }
    private void clickedNameRio(){//Por ahi el rio no se ve entero, entonces si clicleas el nombre, muestra el nombre completo
        binding.nombreRio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessage(binding.nombreRio.getText().toString(),contextMain);
            }
        });
    }



}
