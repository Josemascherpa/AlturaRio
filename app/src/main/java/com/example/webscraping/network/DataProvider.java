package com.example.webscraping.network;

import android.util.Log;

import com.example.webscraping.data.Rio;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataProvider {
    String url;

    public DataProvider(String url) {
        this.url = url;
    }

    public List<Rio> LoadDataRio() {

        List<Rio> listRios = new ArrayList<Rio>();
        try {
            Document doc = Jsoup.connect("https://contenidosweb.prefecturanaval.gob.ar/alturas/").get();
            Elements filas = doc.select("tbody tr");
            for (Element fila : filas) {
                // Agarro las celdas de las filas
                Elements celdas = fila.select("th,td");
                String puerto = "", rio = "", ultimoRegistro = "", variacion = "", fechaHora = "", estado = "",linkDatesGraphs="";
                for (int i = 0; i < celdas.size(); i++) {
                    String contenido = celdas.get(i).text();
                    //voy corriendo la columna para ver cada una
                    switch (i) {
                        case 0:  // Columna "Puerto"
                            puerto = contenido;
                            break;
                        case 1:  // Columna "Río"
                            rio = contenido;
                            break;
                        case 2:  // Columna "Último registro"
                            ultimoRegistro = contenido;
                            break;
                        case 3:  // Columna "Variación"
                            variacion = contenido;
                            break;
                        case 5:  // Columna "Fecha/Hora"
                            fechaHora = contenido;

                            break;
                        case 6:  // Columna "Estado"
                            estado = contenido;
                            break;
                        case 12:
                            Element enlace = celdas.get(i).selectFirst("a[href]");
                            String href = enlace.attr("href");
                            if (href.endsWith("id=")) {
                                linkDatesGraphs = "empty";
                                Log.i("hola", "Enlace sin ID específico: " + linkDatesGraphs);
                            } else {
                                linkDatesGraphs = href;
                                Log.i("hola", "Enlace con ID específico: " + linkDatesGraphs);
                            }
                        default:
                            break;
                    }
                }
                Rio rioData = new Rio(rio,puerto,ultimoRegistro,variacion,fechaHora,estado,linkDatesGraphs);
                listRios.add(rioData);
            }
            return listRios;
        } catch (IOException e) {
            Log.i("Error", e.getMessage() + " " + "ERRORRR");
            return null;
        }

    }
}
