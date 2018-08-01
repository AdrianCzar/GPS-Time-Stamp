package com.example.lenovo.testing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private double CityName = 0.0;
    Intent cameraIntent = null;
    private String mLastUpdateTime;
    private final String TAG = "Oops";

    // location updates interval - 1sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private final int CAPTURE_IMAGE_REQUEST = 1;

    // fastest updates interval - 1 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 3000;

    private static final int REQUEST_CHECK_SETTINGS = 100;


    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private boolean mUpdates;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

        new Timer().schedule(new TimerTask(){
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        sendTakePictureIntent();
                    }
                });
            }
        }, 5000);
        init();

        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

    }

    private void sendTakePictureIntent() {

        cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, 1);
            startLocationButtonClick();
        }
    }

    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                updateLocationUI();
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }

        updateLocationUI();
    }


    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            CityName = mCurrentLocation.getLatitude()+ mCurrentLocation.getLongitude();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

    }

    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "Pending Intent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }

    public void startLocationButtonClick() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                          startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public void stopLocationButtonClick() {
        mRequestingLocationUpdates = false;
        stopLocationUpdates();
    }

    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                });
    }

    private void ImageStore(Intent data) {
        String Stamp1 = "";
        String Stamp2 = null;
        String Stamp3 = null;
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mUpdates = true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            Toast.makeText(getApplicationContext(), "Permission for Storage required in settings", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
        if (mUpdates) {
            MyDBHandler DBHandler = null;
            Time_Stamp TS = null;
            List<Address> addresses;
            String Name = null;
            Bitmap bitmap = null;
            String filename = null;
            try {
                filename = getPictureFile().getName();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Not Working 6",
                        Toast.LENGTH_SHORT).show();
            }
            File sd = Environment.getExternalStorageDirectory();
            File dest = new File(sd, filename);
            Bitmap adder = (Bitmap) data.getExtras().get("data");
            adder = adder.copy(Bitmap.Config.ARGB_8888, true);
            String Date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            try {
                Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                addresses = gcd.getFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation
                        .getLongitude(), 1);
                if (addresses.size() > 0) {
                    Name = addresses.get(0).getAddressLine(0);
                    Stamp2 = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
                    Stamp3 = addresses.get(0).getPostalCode();
                    int t = Stamp2.length() + Stamp3.length() + addresses.get(0).getCountryName().length() + 3;
                    int d = 20;
                    for (int i = 0; i < d; i++) {
                        Stamp1 = Stamp1 + Name.charAt(i);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (Stamp1 != null && Stamp2 != null && Stamp3 != null) {
                try {
                    bitmap = writeTextBitmap(adder, Date, Stamp1, Stamp2, Stamp3);
                    TS = new Time_Stamp(Date, Name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "CityName null",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    FileOutputStream out = new FileOutputStream(dest);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.flush();
                    out.close();
                } else {
                    Toast.makeText(MainActivity.this, "permission problem",
                            Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                DBHandler = new MyDBHandler(this, null);
                Toast.makeText(MainActivity.this,"Database Created",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Database Problem",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                DBHandler.addHandler(TS);
                Toast.makeText(MainActivity.this,"Added to Database",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String a = DBHandler.loadHandler();
                Toast.makeText(MainActivity.this,"Database accessible, can fetch resultset",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

            stopLocationButtonClick();
        }
        else
        {
            Toast.makeText(this, "Storage Permission Problem", Toast.LENGTH_SHORT).show();
        }
    }

    private File getPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "GPSTimeStamp_" + timeStamp;
        File storageDir = Environment.getExternalStorageDirectory();
        return (File.createTempFile(pictureFile, ".jpg", storageDir));

    }

    private Bitmap writeTextBitmap(Bitmap bitmap, String text, String text2, String text3, String text4) {

        Typeface tf = Typeface.create("Bell MT", Typeface.NORMAL);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);

        try
        {
            paint.setTextSize(10);
        }
        catch(Exception e)
        {
            Toast.makeText(MainActivity.this,"Date size incorrect",
                    Toast.LENGTH_SHORT).show();
        }

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        try {

            Canvas canvas = new Canvas(bitmap);

            //If the text is bigger than the canvas , reduce the font size
            if (textRect.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
                paint.setTextSize(convertToPixels(7, this));        //Scaling needs to be used for different dpi's

            //Calculate the positions
            int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

            //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
            int yPos = (int) (((canvas.getHeight() / 2) - 16) - ((paint.descent() + paint.ascent()) / 2));

            canvas.drawText(text, xPos, yPos, paint);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            paint.setTextSize(7);
        }
        catch(Exception e)
        {
            Toast.makeText(MainActivity.this,"Line 1 size incorrect",
                    Toast.LENGTH_SHORT).show();
        }

        Rect textRect2 = new Rect();
        paint.getTextBounds(text2, 0, text2.length(), textRect2);

        try {
            Canvas canvas = new Canvas(bitmap);


            //If the text is bigger than the canvas , reduce the font size
            if (textRect2.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
                paint.setTextSize(convertToPixels(7, this));        //Scaling needs to be used for different dpi's

            //Calculate the positions
            int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

            //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
            int yPos = (int) (((canvas.getHeight() / 2) - 8) - ((paint.descent() + paint.ascent()) / 2));

            canvas.drawText(text2, xPos, yPos, paint);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            paint.setTextSize(7);
        }
        catch(Exception e)
        {
            Toast.makeText(MainActivity.this,"Line 2 size incorrect",
                    Toast.LENGTH_SHORT).show();
        }

        Rect textRect3 = new Rect();
        paint.getTextBounds(text3, 0, text3.length(), textRect3);

        try {
            Canvas canvas = new Canvas(bitmap);


            //If the text is bigger than the canvas , reduce the font size
            if (textRect3.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
                paint.setTextSize(convertToPixels(7, this));        //Scaling needs to be used for different dpi's

            //Calculate the positions
            int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

            //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
            int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

            canvas.drawText(text3, xPos, yPos, paint);
        }
        catch(Exception e)
        {
           e.printStackTrace();
        }
        try
        {
            paint.setTextSize(7);
        }
        catch(Exception e)
        {
            Toast.makeText(MainActivity.this,"Line 3 size incorrect",
                    Toast.LENGTH_SHORT).show();
        }

        Rect textRect4 = new Rect();
        paint.getTextBounds(text4, 0, text4.length(), textRect4);

        try {
            Canvas canvas = new Canvas(bitmap);


            //If the text is bigger than the canvas , reduce the font size
            if (textRect4.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
                paint.setTextSize(convertToPixels(7, this));        //Scaling needs to be used for different dpi's

            //Calculate the positions
            int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

            //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
            int yPos = (int) (((canvas.getHeight() / 2) + 8) - ((paint.descent() + paint.ascent()) / 2));

            canvas.drawText(text4, xPos, yPos, paint);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,false);
        return bitmap;
    }

    public static float convertToPixels(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_REQUEST) {
            if (resultCode != RESULT_CANCELED) {
                if (data != null) {
                    if(mCurrentLocation!=null) {
                        ImageStore(data);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this,"Location Unavailable, try after some time",
                                Toast.LENGTH_SHORT).show();
                    }

                    sendTakePictureIntent();
                }
            }
        }
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
