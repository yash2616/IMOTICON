package com.example.pawan.madc;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.pawan.madc.helper.GraphicOverlay;
import com.google.cloud.vision.v1p2beta1.ImageSource;
import com.google.cloud.vision.v1p3beta1.AnnotateImageRequest;
import com.google.cloud.vision.v1p3beta1.AnnotateImageResponse;
import com.google.cloud.vision.v1p3beta1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1p3beta1.Block;
import com.google.cloud.vision.v1p3beta1.Feature;
import com.google.cloud.vision.v1p3beta1.Feature.Type;
import com.google.cloud.vision.v1p3beta1.Image;
import com.google.cloud.vision.v1p3beta1.ImageAnnotatorClient;
import com.google.cloud.vision.v1p3beta1.ImageContext;
import com.google.cloud.vision.v1p3beta1.Page;
import com.google.cloud.vision.v1p3beta1.Paragraph;
import com.google.cloud.vision.v1p3beta1.Symbol;
import com.google.cloud.vision.v1p3beta1.TextAnnotation;
import com.google.cloud.vision.v1p3beta1.Word;
import com.google.protobuf.ByteString;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class Handwriting_ocr extends AppCompatActivity {

    CameraView cameraView;
    android.app.AlertDialog waitingDialog;
    GraphicOverlay graphicOverlay;
    Button btnCapture,btnsave;


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handwritting_ocr);

        waitingDialog = new SpotsDialog.Builder()
                .setCancelable(false)
                .setMessage("Recognizing...")
                .setContext(this)
                .build();

        cameraView=(CameraView)findViewById(R.id.camera_view);
        graphicOverlay=(GraphicOverlay)findViewById(R.id.graphic_overlay);
        btnCapture=(Button)findViewById(R.id.btn_capture);
        btnsave=(Button)findViewById(R.id.save_button);

        try{btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.start();
                cameraView.captureImage();
                graphicOverlay.clear();
            }
        });}
        catch (Exception e)
        {
            Log.e("Button error",e.getMessage());
            Log.e("Cause Of Error", String.valueOf(e.getCause()));
            finish();
        }

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {
                Log.e("Camera Kit Error",cameraKitError.getMessage());
                Toast.makeText(Handwriting_ocr.this,"Camera Kit Error "+cameraKitError.getMessage(),Toast.LENGTH_SHORT).show(); //error
            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                waitingDialog.show();

                //Image Processing
                Bitmap bitmap= cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);

                cameraView.stop();

                recognizeText(bitmap,"Hand");

                waitingDialog.dismiss();

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }



    private void recognizeText(Bitmap bitmap,String name) {



        File filesDir = getApplicationContext().getFilesDir();
        File imageFile = new File(filesDir, name + ".jpg");

        OutputStream os;
        try {
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error bitmap", Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
        }

        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = null;
        try {
            imgBytes = ByteString.readFrom(new FileInputStream(imageFile));

            Toast.makeText(Handwriting_ocr.this,"We Got The imgbytes",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.d("Image Bytes Captured",e.getMessage());
            Toast.makeText(this,"Error in getting image bytes",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        Image img = Image.newBuilder().setContent(imgBytes).build();


        Feature feat = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        // Set the Language Hint codes for handwritten OCR
        ImageContext imageContext =
                ImageContext.newBuilder().addLanguageHints("en-t-i0-handwrit").build();


        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .setImageContext(imageContext)
                        .build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            client.close();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page : annotation.getPagesList())
                {
                    String pageText = "";
                    for (Block block : page.getBlocksList())
                    {
                        String blockText = "";
                        for (Paragraph para : block.getParagraphsList())
                        {
                            String paraText = "";
                            for (Word word : para.getWordsList())
                            {
                                String wordText = "";
                                for (Symbol symbol : word.getSymbolsList())
                                {
                                    wordText = wordText + symbol.getText();
                                    System.out.format(
                                            "Symbol text: %s (confidence: %f)\n",
                                            symbol.getText(), symbol.getConfidence());
                                }
                                System.out.format("Word text: %s (confidence: %f)\n\n", wordText, word.getConfidence());
                                paraText = String.format("%s %s", paraText, wordText);
                            }
                            // Output Example using Paragraph:
                            System.out.println("\nParagraph: \n" + paraText);
                            System.out.format("Paragraph Confidence: %f\n", para.getConfidence());
                            blockText = blockText + paraText;
                        }
                        pageText = pageText + blockText;
                    }
                }
                System.out.println("\nComplete annotation:");
                System.out.println(annotation.getText());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
