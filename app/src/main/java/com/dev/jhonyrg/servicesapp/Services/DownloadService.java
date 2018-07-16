package com.dev.jhonyrg.servicesapp.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadService extends IntentService {

    public static final String ACTION_DOWNLOAD = "com.dev.jhonyrg.servicesapp.Services.action.DOWNLOAD";
    public static final String ACTION_CONNECTED_DOWNLOAD = "com.dev.jhonyrg.servicesapp.Services.action.CONNECTED_DOWNLOAD";
    public static final String ACTION_START_DOWNLOAD = "com.dev.jhonyrg.servicesapp.Services.action.START_DOWNLOAD";
    public static final String ACTION_END_DOWNLOAD = "com.dev.jhonyrg.servicesapp.Services.action.END_DOWNLOAD";
    public static final String ACTION_ERROR_DOWNLOAD = "com.dev.jhonyrg.servicesapp.Services.action.ERROR_DOWNLOAD";

    private static final String EXTRA_URL = "com.dev.jhonyrg.servicesapp.Services.extra.URL";
    private static final String TAG = "DownloadService";
    InputStream inputFile;
    OutputStream outputFile;
    HttpURLConnection connection;
    String path;
    URL url;
    String nameFile;
    int count;

    public DownloadService() {
        super("DownloadService");
    }

    /**
     * Inicializa este servicio con los parametros necesarios(en este caso solo la Url) para ello se
     * crea un intent que contiene los extras según los parámetros, ha dicho intent se le especifíca
     * una acción
     */
    public static void startActionDownload(Context context, String url) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_URL, url);
        context.startService(intent);
    }

    /**
     * Este metodo es invocado cuando se inicia el servicio(startService())
     * Es aqui donde toma importancia la acción especificada al momento de inicializar el servicio,
     * pues lo primero que se debe hacer dentro de este metodo es obtener dicha accion del intent,
     * para que luego en las validaciones se haga lo que tenga que hacerse con cada acción(en caso
     * que existan mas de una, no es este el caso)
     */

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                final String url = intent.getStringExtra(EXTRA_URL);
                handleActionDownload(url);
            }
        }
    }

    /**
     * Este metodo se encarga de procesar la descarga (en este caso). Este no es un metodo que tenga
     * que ver con el service, sino es un metodo "hecho para no saturar el onHandleIntent()",
     * la linea 67 de esta clase puede ser sustituido por el código contenido en este metodo.
     */
    private void handleActionDownload(String url) {
        try {
            //connect to URL
            this.url = new URL(url);
            this.connection = (HttpURLConnection) this.url.openConnection();
            this.connection.setRequestProperty("Accept-Encoding", "identity");
            this.connection.connect();

            //if not HTTP 200 OK response, then launch exception
            if (this.connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server returned HTTP " + this.connection.getResponseCode()
                        + " " + this.connection.getResponseMessage());
            }

            this.sendMessageConnected();

            //get file name and extension
            this.nameFile = this.url.getFile();
            this.nameFile = nameFile.substring(this.nameFile.lastIndexOf("/")+1, this.nameFile.length());

            //prepare path to save file
            this.path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .getAbsolutePath()+ "/"+nameFile;

            //Change file name if exist xD
            this.count = 0;
            while(new File(this.path).exists()){
                this.path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .getAbsolutePath()+ "/"+ count + nameFile;
                count ++;
            }

            //prepare output file
            outputFile = new FileOutputStream(this.path);

            //create buffer and counter download
            byte data[] = new byte[4096];
            long total = 0;

            //download file
            this.inputFile = new BufferedInputStream(this.connection.getInputStream());
            this.sendMessageStart();

            while ((count = inputFile.read(data)) != -1) {
                total += count;
                outputFile.write(data, 0, count);
            }
        }
        catch (IOException e){
            Log.e(TAG, e.getMessage());
            this.sendMessageError(e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            this.sendMessageError(e.getMessage());
        } finally {
            try {
                //close input  and output stream file
                if (this.outputFile != null)
                    this.outputFile.close();

                if (inputFile != null)
                    inputFile.close();

                //close connection
                if (this.connection != null)
                    this.connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                this.sendMessageError(e.getMessage());
            }
        }

        this.sendMessageEnd();
    }

    /**
     * Envía un intent ligado a la acción que esta realizando el servicio (procesar descarga en
     * este caso | ACTION_DOWNLOAD). Dicho intent debe contener entre sus extras una acción que "identifique"
     * mensaje, y claro, el contenido o información que se desea
     * enviar al usuario.
     * En este caso el intent se crea con la misma acción (ACTION_DOWNLOAD)
     * tambien un "identificador" del mensaje como ACTION_CONNECTED_DOWNLOAD
     * y el contenido del mensaje que quiero mostrar al usuario es "Connected to server"
     * para que éste pueda darse cuenta en que momento se realiza la conexión
     */
    private void sendMessageConnected() {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(ACTION_DOWNLOAD);

        // You can also include some extra data.
        intent.putExtra("action", ACTION_CONNECTED_DOWNLOAD);
        intent.putExtra("message", "Connected to server");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Este metodo envia un mensaje cuando la descarga ha iniciado.
     * Esta "ligado" a la misma acción(ACTION_DOWNLOAD) del servicio pero
     * con un "identificador de mensaje" diferente (ACTION_START_DOWNLOAD)
     */
    private void sendMessageStart() {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(ACTION_DOWNLOAD);

        // You can also include some extra data.
        intent.putExtra("action", ACTION_START_DOWNLOAD);
        intent.putExtra("message", "Download started");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Este metodo envia un mensaje cuando la descarga ha termiando.
     * Esta "ligado" a la misma acción(ACTION_DOWNLOAD) del servicio pero
     * con un "identificador de mensaje" diferente (ACTION_END_DOWNLOAD)
     */
    private void sendMessageEnd() {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(ACTION_DOWNLOAD);

        // You can also include some extra data.
        intent.putExtra("action", ACTION_END_DOWNLOAD);
        intent.putExtra("message", "File saved " +this.path);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Este metodo envia un mensaje cuando ha ocurrido algun error, recibe el parametro message
     * para ser enviado al usuario (este es generico, pensado para pruebas de la app xD).
     * Esta "ligado" a la misma acción(ACTION_DOWNLOAD) del servicio pero
     * con un "identificador de mensaje" diferente (ACTION_ERROR_DOWNLOAD)
     */
    private void sendMessageError(String message) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(ACTION_DOWNLOAD);

        // You can also include some extra data.
        intent.putExtra("action", ACTION_ERROR_DOWNLOAD);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
