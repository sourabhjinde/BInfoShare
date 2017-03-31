package com.example.android.binfoshare;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.example.android.binfoshare.barcode.BarcodeCaptureActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.encoder.BarcodeMatrix;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Observer;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;

public class MainActivity extends AppCompatActivity implements  ConnectivityReceiver.ConnectivityReceiverListener{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 2;

    ImageView imageView;
    String EditTextValue ;
    public final static int QRcodeWidth = 500 ;
    Bitmap bitmap ;
    private FirebaseAuth auth;

    ImageView imageViewLoad;
    Button LoadImage;
    Button createVCard;
    Button upload;
    StorageReference storageReference;
    static final int PICK_CONTACT=1;
    private static final int WRITE_DATA_REQUEST =2;
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST = 1;
    String storage_path;
    UploadTask uploadTask;
    String username;
    ProgressBar progressBar;
    ProgressDialog uploadProgress;
    RelativeLayout relativeLayout;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(myToolbar);

        auth= FirebaseAuth.getInstance();
        FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
        auth.addAuthStateListener(authListener);

        Button scanBarcodeButton = (Button) findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });

        username = getIntent().getStringExtra("email");

        EditTextValue = "gs://binfoshare.appspot.com/" + username;
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        relativeLayout = (RelativeLayout) findViewById(R.id.loadingPanel) ;
        relativeLayout.getLayoutParams().height = QRcodeWidth;
        imageView = (ImageView)findViewById(R.id.imageView);
        if(savedInstanceState == null) {
            String image =PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("IMAGE", "defaultStringIfNothingFound");

            if(image!=null && !image.equals("defaultStringIfNothingFound"))
            {
                bitmap = decodeToBase64(image);
                imageView.post(new Runnable() {
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        progressBar.setVisibility(View.GONE);
                    }

                });
            }
            else {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            bitmap = TextToImageEncode(EditTextValue);

                            imageView.post(new Runnable() {
                                public void run() {
                                    imageView.setImageBitmap(bitmap);
                                    progressBar.setVisibility(View.GONE);
                                }

                            });

                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("IMAGE", encodeTobase64(bitmap)).commit();
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            }

        }else{
            bitmap = savedInstanceState.getParcelable("QRCODE");
            imageView.setImageBitmap(bitmap);
        }



        storageReference= FirebaseStorage.getInstance().getReference();
        imageViewLoad = (ImageView) findViewById(R.id.imageView1);
        uploadProgress = new ProgressDialog(MainActivity.this);
        uploadProgress.setCancelable(false);
        uploadProgress.setMessage("Uploading your vcard");
        uploadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //uploadProgress.setIndeterminate(true);

        getPermissionToReadUserContacts();

    }

    public String encodeTobase64(Bitmap image) {
        Bitmap immage = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

        Log.d("Image Log:", imageEncoded);
        return imageEncoded;
    }

    public static Bitmap decodeToBase64(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    @Override
    protected void onResume() {
        super.onResume();

        MyApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if(!isConnected)
            showSnack("Check internet Connection!!");
    }

    private boolean checkConnection() {
        return ConnectivityReceiver.isConnected();
    }

    // Showing the status in Snackbar
    private void showSnack(String message) {
        int color = Color.RED;

        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(color);
        snackbar.show();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putParcelable("QRCODE", bitmap);
        super.onSaveInstanceState(outState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void getPermissionToReadUserContacts() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED ) {

            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},
                    READ_CONTACTS_PERMISSIONS_REQUEST);
        }
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, WRITE_DATA_REQUEST);
        }
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_DATA_REQUEST);
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_CONTACTS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read Contacts permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Read Contacts permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void get(Cursor cursor)
    {
        //cursor.moveToFirst();
        FileInputStream fis=null;
        InputStream fisPhoto = null;
        System.out.println("i m in get cursor");
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri;

       // displayPhotoUri=Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);

        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd;
        AssetFileDescriptor photoFd;
        ContentResolver contentResolver = this.getContentResolver();
        try {
            fd = contentResolver.openAssetFileDescriptor(uri, "r");
         //   photoFd = contentResolver.openAssetFileDescriptor(displayPhotoUri,"r");
         //   String mimetype =contentResolver.getType(displayPhotoUri);




            fis = fd.createInputStream();
      //      fisPhoto = photoFd.createInputStream();
            fisPhoto = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver,contactUri,true);

            byte[] buf = new byte[(int) fd.getDeclaredLength()];
        //    byte[] bufPhoto = new byte[(int) photoFd.getDeclaredLength()];

            fis.read(buf);
            String vcardstring= new String(buf);
            String vfile = "vlatest.vcf";
            VCardReader vCardReader = new VCardReader(vcardstring);
            VCard vCard =vCardReader.readNext();
            List<Photo> photos = vCard.getPhotos();
            if(photos !=null && !photos.isEmpty())
            vCard.removeProperty(vCard.getPhotos().get(0));

            if(fisPhoto!=null) {
                String mimetype =getType(fisPhoto);
                byte[] bufPhoto = new byte[(int) fisPhoto.available()];
                fisPhoto.read(bufPhoto);
             if(mimetype.contains("jpg") || mimetype.contains("jpeg"))
                vCard.addProperty(new Photo(bufPhoto, ImageType.JPEG));

            else if (mimetype.contains("png"))
                    vCard.addProperty(new Photo(bufPhoto, ImageType.PNG));
                else if (mimetype.contains("gif"))
                    vCard.addProperty(new Photo(bufPhoto, ImageType.GIF));
            }


            storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + vfile; // vfile is the variable of type string and hold value like vcf file name whatever name u give to this ariable it will save with that name.
            String filelocation=storage_path;
            System.out.println("this is file location "+Environment.getExternalStorageDirectory().toString()+File.separator+vfile);
            FileOutputStream mFileOutputStream = new FileOutputStream(storage_path, false);
            vCard.write(mFileOutputStream);

        } catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        finally
        {
            try {
                if(fis!=null)
                fis.close();
                if(fisPhoto!=null)
                    fisPhoto.close();

            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public String getType(InputStream inputStream){
        BitmapFactory.Options opt = new BitmapFactory.Options();
    /* The doc says that if inJustDecodeBounds set to true, the decoder
     * will return null (no bitmap), but the out... fields will still be
     * set, allowing the caller to query the bitmap without having to
     * allocate the memory for its pixels. */
        opt.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(inputStream, null, opt);

        return opt.outMimeType;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId())
        {
            case R.id.action_sign_out:
                // Single menu item is selected do something
                // Ex: launching new activity/screen or show alert message
                auth.signOut();

// this listener will be called when there is change in firebase user session

                return true;

            case R.id.upload :

                intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);

                return true;

            case R.id.create_new_vcard :
                intent = new Intent(Intent.ACTION_INSERT,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);

                return true;



            default:
                return super.onOptionsItemSelected(item);
        }
    }

    Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.DATA_MATRIX.QR_CODE,
                    QRcodeWidth, QRcodeWidth, null
            );
        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.colorPrimaryDark):getResources().getColor(R.color.colorWhite);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_CONTACT)
        {
            if (resultCode == Activity.RESULT_OK) {


                Uri contactData = data.getData();
                Cursor c = getContentResolver().query(contactData, null, null, null, null);
                c.moveToFirst();
                get(c);

               //Upload
                if(checkConnection()) {
                    Uri file = Uri.fromFile(new File(storage_path));
                    StorageReference riversRef = storageReference.child(username + "/VCard/" + file.getLastPathSegment());
                    uploadTask = riversRef.putFile(file);

                    uploadProgress.setProgress(0);
                    uploadProgress.show();


// Register observers to listen for when the download is done or if it fails
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        @SuppressWarnings("VisibleForTests")
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                            Long progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            uploadProgress.setProgress(progress.intValue());

                        }
                    }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            uploadProgress.dismiss();
                            Snackbar snackbar = Snackbar
                                    .make(MainActivity.this.getWindow().getDecorView().getRootView(), "Upload completed successfully", Snackbar.LENGTH_LONG);

                            snackbar.show();
                        }
                    });
                }else{
                    showSnack("Check Internet Connection !!");
                }
            }

        }
        else if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                 //   mResultTextView.setText(barcode.displayValue);
                    Intent intentExtras = new Intent(MainActivity.this,InfoActivity.class);
                    intentExtras.putExtra("BARCODE_DATA",barcode.displayValue);
                    this.startActivity(intentExtras);
                }// else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, "error");
        } else super.onActivityResult(requestCode, resultCode, data);
    }


}