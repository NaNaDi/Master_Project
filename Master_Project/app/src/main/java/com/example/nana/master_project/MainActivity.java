package com.example.nana.master_project;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import static android.R.id.message;
import static java.text.DateFormat.getDateInstance;

public class MainActivity extends AppCompatActivity {

    //https://inducesmile.com/android/android-camera2-api-example-tutorial/

    private static final String TAG = "MainActivity";
    private Button takePictureButton;
    private TextureView textureView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected  CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private AlertDialog mAlertBuilder;

    private AdvertiseData advData1;
    private AdvertiseData advData2;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseSettings advSettings;
    private AdvertiseCallback advCallback;

    private MessageDigest md;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                takePicture();
            }
        });
    }

    public boolean isBluetoothEnabled() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            //Toast.makeText(this, "Please turn Bluetooth on or wait for updating your coordinates!!", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Please turn Bluetooth on or wait for updating your coordinates!!");
            return false;
        } else {
            if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
                //Toast.makeText(this, "Multiple advertisement is not supported", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Multiple advertisement is not supported");
                return false;
            } else return true;
        }
    }

    public void setupAdvertising(byte[] hash){

        if (isBluetoothEnabled()) {
            BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
            advSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .setTimeout(5000)
                    .build();

            int timestamp = (int) (System.currentTimeMillis() / 1000);
            //String deviceID = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);

            advData1 = new AdvertiseData.Builder().setIncludeDeviceName( true).setIncludeTxPowerLevel(false).addManufacturerData(0x4343,hash).build();

            advCallback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    //Toast.makeText(getApplicationContext(), "Your message was sent successfully", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Your message was sent successfully");
                    super.onStartSuccess(settingsInEffect);
                }

                @Override
                public void onStartFailure(int errorCode) {
                    Log.i(TAG, "Advertising onStartFailure: " + errorCode);
                    //Toast.makeText(getApplicationContext(), "onStartFailure " + errorCode, Toast.LENGTH_SHORT).show();
                    super.onStartFailure(errorCode);
                }
            };

            advertiser.startAdvertising(advSettings, advData1,advCallback);
        }
    }

    public void startBTLE() {
        //((Switch)findViewById(R.id.toggleButton)).setChecked(true);
        if (advertiser!= null) advertiser.startAdvertising(advSettings, advData1, advCallback);
        //btleScanner.startScan(btleScanCallback);
        //btleScanner.startScan( btleFilter, btleSettings, new myScanCallback() );
        Log.d(TAG, "BTLE advertising started");
    }

    public void stopBTLE() {
        //((Switch)findViewById(R.id.toggleButton)).setChecked(false);
        if (advertiser != null) advertiser.stopAdvertising(advCallback);
        Log.d(TAG, "BTLE advertising stopped");
        //if (btleScanner != null) btleScanner.stopScan(btleScanCallback);
        //Log.d("BT", "scanning stopped");
    }

    public byte[] createHash(byte[] bytes){
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        return md.digest(bytes);



    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        final File file = new File(Environment.getExternalStorageDirectory()+"/app-image" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + ".jpg");
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //file = new File(Environment.getExternalStorageDirectory()+"/app-image" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + ".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;

                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }

                    //save also to twitter
                    if (LoginActivity.isActive(MainActivity.this)) {

                        mAlertBuilder = new AlertDialog.Builder(MainActivity.this).create();
                        mAlertBuilder.setCancelable(false);
                        mAlertBuilder.setTitle(R.string.please_wait_title);
                        View view = getLayoutInflater().inflate(R.layout.view_loading, null);
                        ((TextView) view.findViewById(R.id.messageTextViewFromLoading)).setText(getString(R.string.posting_image_message));
                        mAlertBuilder.setView(view);
                        mAlertBuilder.show();

                        setupAdvertising(createHash(bytes));
                        Log.d(TAG, "image hash: " + Arrays.toString(createHash(bytes)));
                        startBTLE();

                        uploadToTwitter(file, "uploaded via Fake News Authentication App", new TwitterCallback() {

                            @Override
                            public void onFinsihed(Boolean response) {
                                mAlertBuilder.dismiss();
                                Log.d(TAG, "----------------response----------------" + response);
                                Toast.makeText(MainActivity.this, getString(R.string.image_posted_on_twitter), Toast.LENGTH_SHORT).show();
                                stopBTLE();
                            }
                        });
                    } else{
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Saved: " + file.getName());
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (LoginActivity.isActive(this)) {

            mAlertBuilder = new AlertDialog.Builder(this).create();
            mAlertBuilder.setCancelable(false);
            mAlertBuilder.setTitle(R.string.please_wait_title);
            View view = getLayoutInflater().inflate(R.layout.view_loading, null);
            ((TextView) view.findViewById(R.id.messageTextViewFromLoading)).setText(getString(R.string.posting_image_message));
            mAlertBuilder.setView(view);
            mAlertBuilder.show();

            uploadToTwitter(file, "uploaded via Fake News Authentication App", new TwitterCallback() {

                @Override
                public void onFinsihed(Boolean response) {
                    mAlertBuilder.dismiss();
                    Log.d(TAG, "----------------response----------------" + response);
                    Toast.makeText(MainActivity.this, getString(R.string.image_posted_on_twitter), Toast.LENGTH_SHORT).show();
                }
            });
        } else{
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void uploadToTwitter(final File file, final String message, final TwitterCallback postResponse){

        if(!LoginActivity.isActive(this)){
            Log.d(TAG, "LoginActivity not Active");
            postResponse.onFinsihed(false);
            return;
        }

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        configurationBuilder.setDebugEnabled(true);

        configurationBuilder.setOAuthConsumerKey(this.getResources().getString(R.string.twitter_consumer_key));

        configurationBuilder.setOAuthConsumerSecret(this.getResources().getString(R.string.twitter_consumer_secret));

        configurationBuilder.setOAuthAccessToken(LoginActivity.getAccessToken((this)));

        configurationBuilder.setOAuthAccessTokenSecret(LoginActivity.getAccessTokenSecret(this));

        Configuration configuration = configurationBuilder.build();

        final Twitter twitter = new TwitterFactory(configuration).getInstance();

        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                Log.d(TAG, file.exists() + "");
                try {
                    if (file.exists()) {
                        StatusUpdate status = new StatusUpdate(message);
                        status.setMedia(file);
                        twitter.updateStatus(status);
                    }else{
                        Log.d(TAG, "----- Invalid File ----------");
                        success = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "success = false");
                    success = false;
                }



                final boolean finalSuccess = success;

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        postResponse.onFinsihed(finalSuccess);
                    }
                });

            }
        }).start();
    }

    public static abstract class TwitterCallback{
        public abstract void onFinsihed(Boolean success);
    }
}
