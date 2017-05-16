package com.kelly.wutfish;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.textservice.TextServicesManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int RESULT_CAMERA_CAPTURE = 0;
    static final int RESULT_LOAD_IMG = 1;
    static final int REQUEST_READ_STORAGE = 0;
    static final int REQUEST_WRITE_STORAGE = 1;
    static final int REQUEST_CAMERA = 2;

    private ImageView mImageView;
    private Button mCameraButton;
    private Button mStartButton;
    private Activity mActivity;
    private Bitmap mImage;
    private TextView mResultLabel;
    private final String TAG = "WutFish.MainActivity";

    private Classifier classifier;
    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/retrained_graph_optimized.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/retrained_labels.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setOnClickListener(mImageViewListener);

        mCameraButton = (Button) findViewById(R.id.cameraButton);
        mCameraButton.setOnClickListener(mCameraButtonListener);

        mStartButton = (Button) findViewById(R.id.startButton);
        mStartButton.setOnClickListener(mStartButtonListener);

        mResultLabel = (TextView) findViewById(R.id.resultLabel);

        mActivity = this;
        initTensorflowProcess();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        dispatchOpenGalleryIntent();
                    }
                } else {
                    Toast.makeText(this.getBaseContext(), "Could not retrieve photo from phone. Permission must be granted for this feature.", Toast.LENGTH_SHORT);
                }
                return;
            }
            case REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        dispatchOpenGalleryIntent();
                    }
                } else {
                    Toast.makeText(this.getBaseContext(), "Could not retrieve photo from phone. Permission must be granted for this feature.", Toast.LENGTH_SHORT);
                }
                return;
            }
            case REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this.getBaseContext(), "Could not open camera. Permission must be granted for this feature.", Toast.LENGTH_SHORT);
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case RESULT_CAMERA_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    mImage = (Bitmap) extras.get("data");
                    mImageView.setImageBitmap(mImage);
                }
                else {
                    Log.d(TAG, "Error capturing picture from camera.");
                }
                break;
            case RESULT_LOAD_IMG:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imgDecodableString = cursor.getString(columnIndex);
                    cursor.close();
                    mImage = BitmapFactory.decodeFile(imgDecodableString);
                    mImageView.setImageBitmap(mImage);
                }
                else {
                    Log.d(TAG, "Error loading image from photo gallery.");
                }
                break;
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            }
        }
    }

    private void requestStoragePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, RESULT_CAMERA_CAPTURE);
        }
    }

    private void dispatchOpenGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    private void initTensorflowProcess() {
        try {
            classifier =
                    TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
        }
        catch(Exception ex) {
            Toast.makeText(this.getBaseContext(), "Could not create classification instance.", Toast.LENGTH_SHORT);
        }
    }

    private void processPicture() {
        try {
            Bitmap bitmap = Bitmap.createScaledBitmap(mImage, INPUT_SIZE, INPUT_SIZE, false);
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
            mResultLabel.setText(results.toString());
        }
        catch(Exception ex) {
            Toast.makeText(this.getBaseContext(), "An error occurred while classifying the image.", Toast.LENGTH_SHORT);
        }
    }

    private View.OnClickListener mImageViewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (ContextCompat.checkSelfPermission(mActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(mActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            } else {
                dispatchOpenGalleryIntent();
            }
        }
    };

    private View.OnClickListener mCameraButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (ContextCompat.checkSelfPermission(mActivity,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
            } else {
                dispatchTakePictureIntent();
            }
        }
    };

    private View.OnClickListener mStartButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mImage != null)
                processPicture();
            else
                Toast.makeText(mActivity.getBaseContext(), "No image found!", Toast.LENGTH_SHORT);
        }
    };
}
