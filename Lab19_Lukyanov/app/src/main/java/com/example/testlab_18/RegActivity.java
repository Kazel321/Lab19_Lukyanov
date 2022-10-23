package com.example.testlab_18;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class RegActivity extends AppCompatActivity {

    EditText login, passwd, rePasswd;
    String log, pass, session;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        login = findViewById(R.id.editTextRegLogin);
        passwd = findViewById(R.id.editTextRegPass);
    }

    public void on_sign_up_reg(View v)
    {
        if (login.getText().toString().isEmpty() || passwd.getText().toString().isEmpty())
        {
            Toast.makeText(this, "Text field(s) is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        log = login.getText().toString();
        pass = passwd.getText().toString();
        ApiHelper apiHelper = new ApiHelper(this)
        {
            @Override
            public void on_fail()
            {
                //Toast.makeText(this.ctx, "This login is exist", Toast.LENGTH_SHORT).show();
                //super.on_fail();
            }
        };
        int succ = apiHelper.send(this, "PUT", "/account/create?name=" + log + "&secret=" + pass);
        if (succ != 500)
        {
            ApiHelper signInApiHelper = new ApiHelper(this)
            {
                @Override
                public void on_ready(String res) throws Exception
                {
                    JSONObject obj = new JSONObject(res);

                    String token = obj.getString("token");
                    g.graph.saveToken(token);

                    i = new Intent(this.ctx, GraphActivity.class);
                    i.putExtra("key", token);
                    finish();
                    startActivity(i);
                }
            };
            signInApiHelper.send(this, "PUT", "/session/open?name=" + log + "&secret=" + pass);
        }
        else Toast.makeText(RegActivity.this, "This login is exist", Toast.LENGTH_SHORT).show();
    }

    public void on_sign_in_reg(View v)
    {
        i = new Intent(this, AuthActivity.class);
        finish();
        startActivity(i);
    }
}