package benwang93.com.accelog;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class SaveCSVActivity extends AppCompatActivity {

    private static final String FILENAME = "AcceLog_out.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_csv);

        // Edit text for filename
        final EditText ET_filename = (EditText) findViewById(R.id.SaveCSVActivity_EditText_filename);
        ET_filename.setText(FILENAME);

        // TextView for errors
        final TextView TV_error = (TextView) findViewById(R.id.SaveCSVActivity_TextView_Error);

        // Save button
        findViewById(R.id.SaveCSVActivity_Button_Save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // File
                File file;

                // Open file
                try {
                    file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS), ET_filename.getText().toString());
                    getApplicationContext().getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS).mkdirs();
//                    file.mkdirs();
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "File open failed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save data
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(getDataToWrite());
                    out.flush();
                    out.close();
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "File save failed.", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(getApplicationContext(), e.getStackTrace().toString(), Toast.LENGTH_SHORT).show();
                    TV_error.setText(e.getMessage());
                    return;
                }

                String successfulSaveMessage = "Save successful to " + file.getAbsolutePath();
                TV_error.setText(successfulSaveMessage);
                Toast.makeText(getApplicationContext(), successfulSaveMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Close button
        findViewById(R.id.SaveCSVActivity_Button_Close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private byte[] getDataToWrite(){
        // Parse data and write
        return getIntent().getByteArrayExtra(MainActivity.DATA_SAVE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save_csv, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
