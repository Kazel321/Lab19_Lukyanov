package com.example.testlab_18;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {

    //String api_endpoint = "http://89.108.78.244:5000";
    //http://89.108.78.244:5000/
    //http://nodegraph.spbcoit.ru:5000/
    EditText login, passwd;
    CheckBox chkSave;
    String log, pass, session = "null";
    String[] logPass;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        login = findViewById(R.id.editTextAuthLogin);
        passwd = findViewById(R.id.editTextAuthPass);
        chkSave = findViewById(R.id.chkSave);

        g.graph = new DB(this, "graph.db", null, 1);
        logPass = g.graph.getSettings();
        if (logPass != null)
        {
            chkSave.setChecked(true);
            login.setText(logPass[0]);
            passwd.setText(logPass[1]);
        }
        else
        {
            chkSave.setChecked(false);
            login.setText("");
            passwd.setText("");
        }
    }

    public void on_sign_in_auth(View v)
    {
        if (login.getText().toString().isEmpty() || passwd.getText().toString().isEmpty())
        {
            Toast.makeText(this, "Text field(s) is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        log = login.getText().toString();
        pass = passwd.getText().toString();
        if (chkSave.isChecked()) //If save needed
        {
            g.graph = new DB(this, "graph.db", null, 1);
            g.graph.saveSettings(log, pass);
        }
        else g.graph.onDeleteAll(); //Else delete save
        ApiHelper apiHelper = new ApiHelper(this)
        {
            @Override
            public void on_ready(String res) throws Exception {

                JSONObject obj = new JSONObject(res);

                String token = obj.getString("token");
                g.graph.saveToken(token);

                i = new Intent(ctx, GraphActivity.class);
                i.putExtra("key", token);
                finish();
                startActivity(i);

                /*
                if (res.equals("null")) //Not open session
                {
                    Toast.makeText(this.ctx, "Incorrect login or password", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    session = res.replace("\"", "");
                    i = new Intent(this.ctx, MainActivity.class);
                    i.putExtra("key", session);
                    finish();
                    startActivity(i); //Start list activity
                }
                */
            }
        };
        apiHelper.send(this, "PUT", "/session/open?name=" + log + "&secret=" + pass);
        //apiHelper.send(this, "PUT", "/session/open?name=lukyanov&secret=123");
    }

    public void on_sign_up_auth(View v)
    {
        i = new Intent(this, RegActivity.class);
        finish();
        startActivity(i);
    }
}