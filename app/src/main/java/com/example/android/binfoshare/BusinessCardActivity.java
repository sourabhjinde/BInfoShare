package com.example.android.binfoshare;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;


import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Photo;
import ezvcard.property.Telephone;

import static com.example.android.binfoshare.BusinessCardActivity.progress_bar_type;

public class BusinessCardActivity extends AppCompatActivity {
    // button to show progress dialog
    Button btnShowProgress;

    // Progress Dialog
    private ProgressDialog pDialog;

    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    private ImageView my_image;
    private TextView name;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    // File url to download
    private static String file_url ="https://firebasestorage.googleapis.com/v0/b/binfoshare.appspot.com/o/Birla%2Fvcard.vcf?alt=media&token=cdecd861-dbea-44a7-9f30-7465605bd13a";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_card);

        verifyStoragePermissions(this);

        Intent myIntent =getIntent();

        if(myIntent !=null)
        {
           file_url= myIntent.getStringExtra("URL");
            Log.d("url",file_url);
        }
        //file_url ="https://static.pexels.com/photos/24353/pexels-photo.jpg";
        // show progress bar button


        my_image = (ImageView) findViewById(R.id.my_image);
        name = (TextView) findViewById(R.id.name);
      //  new DownloadFileFromURL().execute(file_url);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(file_url).child("VCard").child("vlatest.vcf");

      //  StorageReference storageRef = storage.getReferenceFromUrl(file_url).child("vcard.vcf");
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(true);
        pDialog.setMessage("File downloading ...");
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setProgress(0);
        pDialog.setMax(100);
        pDialog.show();
        // Image view to show image after downloading
        try {
          //  final File localFile = File.createTempFile("vcard", "vcf");
           final File localFile = new File(Environment.getExternalStorageDirectory().toString() + "/vcard.vcf");
            storageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                    pDialog.dismiss();

                    try {
                        List<VCard> vcards = Ezvcard.parse(localFile).all();
                        for (VCard vcard : vcards) {
                            //  System.out.println("Name: " + vcard.getFormattedName().getValue());
                            name.setText(vcard.getFormattedName().getValue());
                            for (Photo photo : vcard.getPhotos()){
                                byte data[] = photo.getData();
                                if(data !=null) {
                                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    my_image.setImageBitmap(bmp);
                                }
                                //save byte array to file
                            }

                            System.out.println("Telephone numbers:");
                    for (Telephone tel : vcard.getTelephoneNumbers()) {
                        name.append("\n"+tel.getTypes() + ": " + tel.getText());
                    }
                         for(Email email:   vcard.getEmails()){
                             name.append("\n"+email.getValue());

                         }
                            for(Address address :vcard.getAddresses()){
                                name.append("\n"+address.getExtendedAddressFull());
                            }
                        }
                    }catch (IOException e)
                    {
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            });
        } catch (Exception e ) {}
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream("/sdcard/downloadedfile.vcf");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(""+(int)((total*100)/lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ",e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type);

            // Displaying downloaded image into image view
            // Reading image path from sdcard
          //  String vCardPath = Environment.getExternalStorageDirectory().toString() + "/vcard.vcf";
            File file = new File(Environment.getExternalStorageDirectory().toString() + "/downloadedfile.vcf");

            try {
                List<VCard> vcards = Ezvcard.parse(file).all();
                for (VCard vcard : vcards) {
                  //  System.out.println("Name: " + vcard.getFormattedName().getValue());
                    name.setText(vcard.getFormattedName().getValue());
                    for (Photo photo : vcard.getPhotos()){
                        byte data[] = photo.getData();
                        if(data !=null) {
                            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            my_image.setImageBitmap(bmp);
                        }
                        //save byte array to file
                    }
                  /*  System.out.println("Telephone numbers:");
                    for (Telephone tel : vcard.getTelephoneNumbers()) {
                        System.out.println(tel.getTypes() + ": " + tel.getText());
                    }*/
                }
            }catch (IOException e)
            {
            }
        }

    }
}


