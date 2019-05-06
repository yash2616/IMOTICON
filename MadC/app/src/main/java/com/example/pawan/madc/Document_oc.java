package com.example.pawan.madc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pawan.madc.helper.GraphicOverlay;
import com.example.pawan.madc.helper.TextGraphic;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dmax.dialog.SpotsDialog;

public class Document_oc extends AppCompatActivity implements TextToSpeech.OnInitListener, ExampleDialog.ExampleDialogListener
 {

    CameraView cameraView;

    android.app.AlertDialog waitingDialog;

    GraphicOverlay graphicOverlay;

    Button btnCapture,btnsave,btnspeak;

    EditText editText;

    TextToSpeech tts;

    String recognized="";

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        waitingDialog.dismiss();
        graphicOverlay.clear();
        recognized="";
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        waitingDialog.dismiss();
        graphicOverlay.clear();
        recognized="";
        tts.stop();
    }

    @Override
    protected void onDestroy() {
        if(tts!=null)
        {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_oc);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        }

        waitingDialog = new SpotsDialog.Builder()
                .setCancelable(false)
                .setMessage("Recognizing...")
                .setContext(this)
                .build();

        cameraView=(CameraView)findViewById(R.id.camera_view);
        graphicOverlay=(GraphicOverlay)findViewById(R.id.graphic_overlay);
        btnCapture=(Button)findViewById(R.id.btn_capture);
        btnsave=(Button)findViewById(R.id.save_button);
        btnspeak=(Button)findViewById(R.id.voice);

        tts = new TextToSpeech(this,this);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.start();
                cameraView.captureImage();
                graphicOverlay.clear();
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {
                Log.e("Camera Kit Error",cameraKitError.getMessage());
                Toast.makeText(Document_oc.this,"Camera Kit Error "+cameraKitError.getMessage(),Toast.LENGTH_SHORT).show(); //error
            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                waitingDialog.show();

                //Image Processing
                Bitmap bitmap= cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);

                cameraView.stop();

                recognizeText(bitmap);


            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        btnspeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceinput();
            }
        });

    }

    private void voiceinput() {
        CharSequence text = recognized;
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"id1");
    }

     @Override
     public void onInit(int status) {
         if(status==TextToSpeech.SUCCESS)
         {
             int result = tts.setLanguage(Locale.US);
             float i = 50;

             if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
             {
                 Toast.makeText(Document_oc.this,"Language Not Supported",Toast.LENGTH_SHORT).show();
             }
             else {
                 btnspeak.setEnabled(true);
                 voiceinput();
             }
         }
         else {
             Toast.makeText(Document_oc.this,"Initialization Problem",Toast.LENGTH_SHORT).show();
         }
     }

     private void openDialog() {
        ExampleDialog exampleDialog = new ExampleDialog();
        exampleDialog.show(getSupportFragmentManager(),"File Name");
    }

    public void recognizeText(Bitmap bitmap) {
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder()
                        .setLanguageHints(Arrays.asList("en"))
                        .build();


        //Error

        FirebaseVisionTextRecognizer textRecognizer =FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);


        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        Toast.makeText(Document_oc.this,"Cloud Text Method Working",Toast.LENGTH_SHORT).show();
                        drawTextResult(firebaseVisionText);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Document_oc.this,"Cloud Method Error",Toast.LENGTH_SHORT).show();
                        Log.d("IMOTICON ERROR",e.getMessage());

                        FirebaseVisionTextRecognizer textRecognizer0 = FirebaseVision.getInstance()
                                .getOnDeviceTextRecognizer();

                            textRecognizer0.processImage(image)
                                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                    @Override
                                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                        Toast.makeText(Document_oc.this,"On Device Working",Toast.LENGTH_SHORT).show();
                                        drawTextResult(firebaseVisionText);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(Document_oc.this,"Device Method Error",Toast.LENGTH_SHORT).show();
                                        Log.e("Imoticon Error",e.getMessage());
                                        waitingDialog.dismiss();
                                    }
                                });

                        waitingDialog.dismiss();
                    }
                });
    }

    private void drawTextResult(FirebaseVisionText firebaseVisionText) {

        //Get Text Blocks
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();


        if(blocks.size()==0)
        {
            Toast.makeText(this,"No Text Found",Toast.LENGTH_SHORT).show();
            waitingDialog.dismiss();
            return;
        }

        graphicOverlay.clear();
        for(int i=0;i<blocks.size();i++)
        {
            //Get Lines
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            //Get Elements
            for(int j=0;j<lines.size();j++)
            {
                //getting it
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

                for(int k=0;k<elements.size();k++)
                {
                    //DrawElement
                    TextGraphic textGraphic = new TextGraphic(graphicOverlay,elements.get(k));
                    recognized=recognized+" ";
                    recognized=recognized.concat(elements.get(k).getText()+" ");
                    graphicOverlay.add(textGraphic);
                }
                recognized=recognized+" ";
            }
            recognized=recognized+"\n";
        }


        //Dismiss Dialog
        waitingDialog.dismiss();

    }

    @Override
    public void applyText(String Filename) {

        //create folder
        File Folder= new File(Environment.getExternalStorageDirectory()+"/"+"IMOTICON");

        if(!Folder.exists())
        {
            Folder.mkdir();
        }

        File file = new File(Folder,Filename+".txt");

        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            openDialog();
            Toast.makeText(this,"File Already Present Please Rename",Toast.LENGTH_SHORT).show();
            finish();
        }

        //write to file
        try{
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(recognized.getBytes());
            fileOutputStream.close();
            Toast.makeText(this, "Saved Succesfully", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this,"File Not Found",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error Saving", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case 1000:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}
