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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

public class SaveCSVActivity extends AppCompatActivity {

    private static final String FILENAME_BASE = "AcceLog/AcceLog_out_";
    private static final String FILENAME_EXTENSION = ".csv";
    private static final String CSV_HEADER = "Timestamp,accel_X (G),accel_Y (G),accel_Z (G)\n";

    static SimpleDateFormat sdf_filename = new SimpleDateFormat("yyyyMMd-HHmmss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_csv);

        // Edit text for filename
        final EditText ET_filename = (EditText) findViewById(R.id.SaveCSVActivity_EditText_filename);
        ET_filename.setText(FILENAME_BASE + sdf_filename.format(MainActivity.startTime) + FILENAME_EXTENSION);

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
                    // Use Documents directory
//                    file = new File(Environment.getExternalStoragePublicDirectory(
//                            Environment.DIRECTORY_DOCUMENTS), ET_filename.getText().toString());

                    // Use AcceLog folder
                    StringTokenizer filenameTokens = new StringTokenizer(ET_filename.getText().toString(), "/");
//                    File dir = new File(Environment.getExternalStorageDirectory(), ET_dirname.getText().toString());

                    // Assemble directory name
                    String directories = "";
                    while (filenameTokens.countTokens() > 1) {
                        directories = directories.concat("/" + filenameTokens.nextToken());
                    }
                    File dir = new File(Environment.getExternalStorageDirectory(), directories);
                    TV_error.append("Directory: " + dir.getAbsolutePath() + "\n");
                    dir.mkdirs();
//                    file = new File(dir, ET_filename.getText().toString());

                    // Create file
                    file = new File(dir, filenameTokens.nextToken());
                    TV_error.append("Filename: " + file.getName() + "\n");

                    getApplicationContext().getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS).mkdirs();

                    // Don't continue if file exists
                    if (file.exists()){
                        Toast.makeText(getApplicationContext(), "Error saving: File exists.", Toast.LENGTH_SHORT).show();
                        TV_error.append("Error saving: File exists.");
                        return;
                    }
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "File open failed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save data
                try {
                    FileOutputStream out = new FileOutputStream(file);

                    // Write column headers
                    out.write(CSV_HEADER.getBytes());

                    // Write array out to .csv file
                    for (AccelSample sample : MainActivity.accelSamples){
                        try {
                            String data = MainActivity.sdf_graph.format(new Date(sample.time)) + "," + sample.aX + "," + sample.aY +
                                    "," + sample.aZ + "\n";
                            out.write(data.getBytes());
                        } catch (Exception e){
                            TV_error.append(e.getMessage()+"\n");
                        }
                    }

                    // Complete file write
//                    out.write(getDataToWrite());
                    out.flush();
                    out.close();
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "File save failed.", Toast.LENGTH_SHORT).show();
//                    Toast.makeText(getApplicationContext(), e.getStackTrace().toString(), Toast.LENGTH_SHORT).show();
                    TV_error.append(e.getMessage() + "\n");
                    return;
                }

                String successfulSaveMessage = "Save successful to " + file.getAbsolutePath() + "\n";
                TV_error.append(successfulSaveMessage);
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
