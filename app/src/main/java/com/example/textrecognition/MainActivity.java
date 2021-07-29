package com.example.textrecognition;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    DatabaseHelper mDatabaseHelper;


    final Context context = this;
    EditText mResultEt;
    ImageView mPreviewIv;
    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1002;

    String cameraPermission[];
    String storagePermission[];
    Uri image_uri;
    TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultEt=findViewById(R.id.resultEt);
        mPreviewIv=findViewById(R.id.imageIv);

        //cam permission
        cameraPermission=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mDatabaseHelper=new DatabaseHelper(getBaseContext(),mDatabaseHelper.DATABASE_NAME,null,1);
    }
   //action bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    //handle actionbar item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.addImage){ //to choose an image
            showImageImportDialog();
        }
        if(id==R.id.speech){  //text to speech conversion

            textToSpeech = new TextToSpeech(getBaseContext(),
                    new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if(status!=TextToSpeech.ERROR) {
                                textToSpeech.setLanguage(Locale.UK);
                                if(!mResultEt.getText().toString().isEmpty()){
                                    Toast.makeText(getApplicationContext(),"Reading aloud", Toast.LENGTH_LONG).show();
                                    String text = mResultEt.getText().toString();
                                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                                }
                                else{
                                    Toast.makeText(getApplicationContext(),"Scan image to read aloud", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
        }
        if(id==R.id.save){
            LayoutInflater li = LayoutInflater.from(context);
            View promptsView = li.inflate(R.layout.prompts, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setView(promptsView);

            final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            (dialog, id1) -> {
                                if (!mResultEt.getText().toString().isEmpty()) {
                                    try {
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 23);
                                        File folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                                        if(!userInput.getText().toString().isEmpty()) {
                                            File myFile = new File(folder, userInput.getText().toString() + ".txt");
                                            FileOutputStream fstream = new FileOutputStream(myFile);
                                            fstream.write(mResultEt.getText().toString().getBytes());
                                            fstream.close();
                                            Toast.makeText(getApplicationContext(), "Details Saved in " + myFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            Toast.makeText(getApplicationContext(), "File name is empty, please enter a valid file name", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (FileNotFoundException e) {
                                        Toast.makeText(getApplicationContext(), "Could not save file", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        Toast.makeText(getApplicationContext(), "Could not save file", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                                else{
                                    Toast.makeText(getApplicationContext(), "Cannot save an empty file", Toast.LENGTH_SHORT).show();
                                }
                            })
                    .setNegativeButton("Cancel",
                            (dialog, id12) -> dialog.cancel());

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

        }
        if(id==R.id.share){
            if (!mResultEt.getText().toString().isEmpty()) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                String toSend = mResultEt.getText().toString();
                i.putExtra(android.content.Intent.EXTRA_TEXT, toSend);
                startActivity(Intent.createChooser(i, "Share via"));
            }
            else{
                Toast.makeText(getApplicationContext(), "Scan or enter text to share", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        String[] items={"Camera","Gallery"};
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {   //camera
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickCamera();
                    }
                }
                if (which == 1) {   //gallery
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");  //returns data of type image
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NewPic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image to text");
        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result=ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result= ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) { //that is if the dialog for permissions appeared and user either allowed or denied permissions
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) { //cropping gallery pic
                CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) { //cropping camera pic
                CropImage.activity(image_uri).setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
        }
        //to get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mPreviewIv.setImageURI(resultUri);
                mResultEt.setText("");   //new
                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();
                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if(recognizer.isOperational())
                {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    if(items.size()==0)
                        Toast.makeText(this, "Cant recognize text", Toast.LENGTH_SHORT).show();
                    else {
                        //get text from sb
                        for (int i = 0; i < items.size(); i++) {
                            TextBlock myItem = items.valueAt(i);
                            sb.append(myItem.getValue());
                            sb.append(" ");
                        }
                        //set text to editable
                        mResultEt.setText(sb.toString());

                        byte[] data1 = getBitmapAsByteArray(bitmap);
                        boolean insertData=mDatabaseHelper.addData(mResultEt.getText().toString(),data1);
                        /*if(insertData){
                            Toast.makeText(getBaseContext(),"Data inserted successfully",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getBaseContext(),"Could not insert data",Toast.LENGTH_SHORT).show();
                        }*/
                    }
                }
                else{
                    Toast.makeText(this, "Dependencies not yet available", Toast.LENGTH_SHORT).show();
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

