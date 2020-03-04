package br.com.lukeprot.sendimage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Permissions permissions;
    private final int PICK_IMAGE_GALERY_REQUEST = 1;
    private final int PICK_IMAGE_CAMERA_REQUEST = 2;
    private final int INVOQUE_GPS_REQUEST = 613;

    private TextView txtLatitude;
    private TextView txtLongitude;
    private ImageView imgArquivo;
    private Button btnGps;
    private Button btnCoordenadas;
    private Button btnGaleria;
    private Button btnCamera;
    private Button btnEnviar;

    File imgFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissions = new Permissions(this);
        permissions.getPermissions();
        initUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initListeners();
    }

    private void initUi(){
        txtLatitude = (TextView) findViewById(R.id.txt_latitude);
        txtLongitude = (TextView) findViewById(R.id.txt_longitude);
        imgArquivo = (ImageView) findViewById(R.id.img_foto);
        btnGps = (Button) findViewById(R.id.btn_gps);
        btnCoordenadas = (Button) findViewById(R.id.btn_coordenadas);
        btnGaleria = (Button) findViewById(R.id.btn_galeria);
        btnCamera = (Button) findViewById(R.id.btn_camera);
        btnEnviar = (Button) findViewById(R.id.btn_enviar);
    }

    private void initListeners(){
        btnGps.setOnClickListener(this);
        btnCoordenadas.setOnClickListener(this);
        btnGaleria.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
        btnEnviar.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_gps :
                permissions.getPermissionGPS();
            break;
            case R.id.btn_coordenadas :
                obterLocalizacao();
            break;
            case R.id.btn_galeria :
                obterDaGaleria();
            break;
            case R.id.btn_camera :
                obterDaCamera();
            break;
            case R.id.btn_enviar :
                new EnvioTask().execute(obterImagemParaEnvio());
            break;
        }
    }

    private String obterImagemParaEnvio(){
        return "";
    }

    private void obterDaGaleria(){
        startActivityForResult(
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("image/*"), PICK_IMAGE_GALERY_REQUEST
        );
    }

    private void obterDaCamera(){
        String diretorio = Environment.getExternalStorageDirectory() + "/Android/data/br.com.lukeprot.sendimage/cache/";

        File dir = new File(diretorio);
        if (!dir.exists()) {dir.mkdirs();}

        imgFile = new File(
                diretorio,
                "img_" + new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date())
        );

        Uri temp;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            temp = FileProvider.getUriForFile(this, "br.com.lukeprot.sendimage.fileprovider", imgFile);
        }else{
            temp = Uri.fromFile(imgFile);
        }

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(this.getPackageManager()) != null){
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, temp);
            this.startActivityForResult(cameraIntent, PICK_IMAGE_CAMERA_REQUEST);
        }
    }

    private void obterLocalizacao(){

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        String locationProvider = LocationManager.NETWORK_PROVIDER;

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            //Toast.makeText(this,"Habilite o GPS!",Toast.LENGTH_LONG).show();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        getCoordenadas(locationManager.getLastKnownLocation(locationProvider));
    }

    private void getCoordenadas(Location lastKnownLocation){
        if(lastKnownLocation != null) {
            txtLatitude.setText(String.format("Latitude : %s", String.valueOf(lastKnownLocation.getLatitude())));
            txtLongitude.setText(String.format("Longitude : %s", String.valueOf(lastKnownLocation.getLongitude())));
        }else{
            txtLatitude.setText("Não Localizado");
            txtLongitude.setText("Não Localizado");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_GALERY_REQUEST && resultCode == Activity.RESULT_OK) {
            try {
                File tempFile = FileUtil.from(this, data.getData());
                imgArquivo.setImageBitmap(BitmapFactory.decodeFile(tempFile.toString()));
            }catch (IOException e) {
                showMessage("Falha ao obter a imagem! :(");
            }
        }else if(requestCode == PICK_IMAGE_CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            imgArquivo.setImageBitmap(BitmapFactory.decodeFile(imgFile.toString()));
        }else {
            if((requestCode == PICK_IMAGE_GALERY_REQUEST || requestCode == PICK_IMAGE_CAMERA_REQUEST)
                    && resultCode == Activity.RESULT_CANCELED){
                showMessage("Não foi possível obter a imagem! :(");
            }
            if(requestCode == INVOQUE_GPS_REQUEST && resultCode == Activity.RESULT_CANCELED){
                showMessage("Não foi habilitar o GPS! :(");
            }
        }
    }

    private void showMessage(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class EnvioTask extends AsyncTask<String, Integer, String>{

        private String result = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String imageInBase64 = params[0];

            result = "Imagem em base64 pronta para enviar...";

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            showMessage(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
