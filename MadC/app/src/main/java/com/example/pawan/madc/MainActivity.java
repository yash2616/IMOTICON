package com.example.pawan.madc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static android.os.Environment.*;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton doc_ocr,fab, gallery, share;
    Boolean isFabOpen = false;
    Animation fab_open,fab_close,rotate_forward,rotate_backward;

    private static final int pickimage=100;
    private static final int opfil=7171;

    Uri imageUri,opfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doc_ocr = (FloatingActionButton)findViewById(R.id.document_Ocr);
        fab = (FloatingActionButton)findViewById(R.id.fab);
        gallery = (FloatingActionButton)findViewById(R.id.fab1);
        share = (FloatingActionButton)findViewById(R.id.fab2);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_bckward);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFAB();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();

            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                openDocument();
            }
        });


        doc_ocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in =new Intent(MainActivity.this,Document_oc.class);
                startActivity(in);
            }
        });
    }

    private void openDocument() {

    }

    private void openGallery() {
        Intent in = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(in,pickimage);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK && requestCode==pickimage)
        {
            imageUri=data.getData();
            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(imageUri);
                final Bitmap bitmap = BitmapFactory.decodeStream(in);
                Document_oc image = new Document_oc();
                //image.recognizeText(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else if(resultCode==RESULT_OK && requestCode==opfil)
        {
           opfile= Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/" + "IMOTICON"));
        }
    }

    public void animateFAB(){

        if(isFabOpen){

            fab.startAnimation(rotate_backward);
            gallery.startAnimation(fab_close);
            share.startAnimation(fab_close);
            gallery.setClickable(false);
            share.setClickable(false);
            isFabOpen = false;
            Log.d("Tab", "close");

        } else {

            fab.startAnimation(rotate_forward);
            gallery.startAnimation(fab_open);
            share.startAnimation(fab_open);
            gallery.setClickable(true);
            share.setClickable(true);
            isFabOpen = true;
            Log.d("Tab","open");

        }
    }
}


