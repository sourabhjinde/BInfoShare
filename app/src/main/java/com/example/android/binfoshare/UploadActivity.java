package com.example.android.binfoshare;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ezvcard.VCard;

import ezvcard.io.text.VCardReader;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;

public class UploadActivity extends AppCompatActivity {


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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        username = getIntent().getStringExtra("email");
        storageReference= FirebaseStorage.getInstance().getReference();
        imageViewLoad = (ImageView) findViewById(R.id.imageView1);
        LoadImage = (Button)findViewById(R.id.button1);
        createVCard = (Button) findViewById(R.id.button2);
        upload =(Button) findViewById(R.id.upload);
        upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Uri file = Uri.fromFile(new File(storage_path));
                StorageReference riversRef = storageReference.child(username+"/VCard/"+file.getLastPathSegment());
                uploadTask = riversRef.putFile(file);

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
                });

            }
        });

        createVCard.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_INSERT,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);

            }
        });

        LoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

             /*   intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent, IMG_RESULT);*/

                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);

            }
        });
        getPermissionToReadUserContacts();
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {


                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    c.moveToFirst();
                    get(c);
                   /* if (c.moveToFirst()) {


                        String id =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                        String hasPhone =c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                                    null, null);
                            phones.moveToFirst();
                            cNumber = phones.getString(phones.getColumnIndex("data1"));
                            System.out.println("number is:"+cNumber);
                        }
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                    }*/
                }
                break;
        }
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
        FileInputStream fisPhoto = null;
        System.out.println("i m in get cursor");
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));

        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);

        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd;
        AssetFileDescriptor photoFd;
        ContentResolver contentResolver = this.getContentResolver();
        try {
            fd = contentResolver.openAssetFileDescriptor(uri, "r");
            photoFd = contentResolver.openAssetFileDescriptor(displayPhotoUri,"r");
            String mimetype =contentResolver.getType(displayPhotoUri);


            fis = fd.createInputStream();
            fisPhoto = photoFd.createInputStream();

            byte[] buf = new byte[(int) fd.getDeclaredLength()];
            byte[] bufPhoto = new byte[(int) photoFd.getDeclaredLength()];
            fis.read(buf);
            fisPhoto.read(bufPhoto);
            String vcardstring= new String(buf);
            String vfile = "vlatest.vcf";
            VCardReader vCardReader = new VCardReader(vcardstring);
            VCard vCard =vCardReader.readNext();

            vCard.removeProperty(vCard.getPhotos().get(0));
            if(mimetype.contains("jpeg") || mimetype.contains(("jpg")))
            vCard.addProperty(new Photo(bufPhoto,ImageType.JPEG));
            else if(mimetype.contains("png"))
                vCard.addProperty(new Photo(bufPhoto,ImageType.PNG));
            else if(mimetype.contains("gif"))
                vCard.addProperty(new Photo(bufPhoto,ImageType.GIF));


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
                fis.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
  /*  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {

            if (requestCode == IMG_RESULT && resultCode == RESULT_OK
                    && null != data) {


                Uri URI = data.getData();
                String[] FILE = { MediaStore.Images.Media.DATA };


                Cursor cursor = getContentResolver().query(URI,
                        FILE, null, null, null);

                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(FILE[0]);
                ImageDecode = cursor.getString(columnIndex);
                cursor.close();

               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       try{
                       BitmapFactory.Options options = new BitmapFactory.Options();
                       options.inJustDecodeBounds = true;
                       BitmapFactory
                               .decodeFile(ImageDecode, options);

                       options.inSampleSize =

                               calculateInSampleSize(options, imageViewLoad.getWidth(), imageViewLoad.

                                       getHeight());
                       options.inJustDecodeBounds = false;
                       final Bitmap bmp = BitmapFactory.decodeFile(ImageDecode, options);
                           runOnUiThread(new Runnable() //run on ui thread
                           {
                               public void run()
                               {

                                   imageViewLoad.setImageBitmap(bmp);

                               }
                           });

                       File vcfFile = new File(getExternalFilesDir(null), "generated.vcf");

                       VCard vcard = new VCard();
                       vcard.setVersion(VCardVersion.V3_0);

                       vcard.setFormattedName(new

                               FormattedName("Sourabh" + " " + "Jinde"));
                       ByteArrayOutputStream stream = new ByteArrayOutputStream();
                       bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

                       vcard.addPhoto(new

                               Photo(stream.toByteArray(), ImageType.PNG));
                       vcard.write(vcfFile);
                       Intent i = new Intent();
                       i.setAction(android.content.Intent.ACTION_VIEW);
                       i.setDataAndType(Uri.fromFile(vcfFile), "text/x-vcard");

                       startActivity(i);
                   } catch (Exception e) {
                   }
                   }
               }).start();

            }
        } catch (Exception e) {
            Toast.makeText(this, "Please try again", Toast.LENGTH_LONG)
                    .show();
        }

    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }*/
}
