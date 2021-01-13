package com.jsd.customimagemodellabeling;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    ImageView imgSelect;
    Button btnDetect;
    TextView txt, txt_tip;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgSelect = findViewById(R.id.img);
        btnDetect = findViewById(R.id.btn_detect);
        txt = findViewById(R.id.txt);
        txt_tip = findViewById(R.id.txt_tip);
        pd = new ProgressDialog(this);

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i, "select an image.."), 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && data != null) {
            txt_tip.setVisibility(View.GONE);
            imgSelect.setImageURI(data.getData());

            FirebaseVisionImage image;
            try {
                image = FirebaseVisionImage.fromFilePath(getApplicationContext(), data.getData());

                FirebaseAutoMLLocalModel localModel = new FirebaseAutoMLLocalModel.Builder()
                        .setAssetFilePath("manifest.json")
                        .build();

                final FirebaseVisionImageLabeler labeler;
                try {
                    FirebaseVisionOnDeviceAutoMLImageLabelerOptions options =
                            new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel)
                                    // Evaluate your model in the Firebase console
                                    .setConfidenceThreshold(0.6f)
                                    // to determine an appropriate value.
                                    .build();
                    labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);

                    labeler.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                    // Task completed successfully
                                    // ...
                                    if (labels.size() == 0) {
                                        txt.setText("No results.");
                                    } else {
                                        for (FirebaseVisionImageLabel label : labels) {
                                            String text = label.getText();
                                            float confidence = label.getConfidence();

                                            txt.setText("Name: " + text + "\n" + "Confidence: " + (confidence * 100) + "%");
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    Log.d("Fail", "onFailure: No Detected..");
                                }
                            });
                } catch (FirebaseMLException e) {
                    // ...
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}