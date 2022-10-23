package com.example.testlab_18;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SessionActivity extends AppCompatActivity {

    EditText txtChangePass;
    Spinner spnChangeAPI;

    ListView lstctl;
    ArrayList<Session> lst = new ArrayList<>();
    ArrayAdapter<Session> adp;

    Intent i;

    ApiHelper apiHelper;

    String key;

    ArrayAdapter<String> adpAPI;

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        g.graph = new DB(this, "graph.db", null, 1);

        spnChangeAPI = findViewById(R.id.spinnerAPI);
        txtChangePass = findViewById(R.id.editTextPassChng);
        lstctl = findViewById(R.id.listViewSession);
        lstctl.setOnItemClickListener((parent, view, position, id) ->
        {
            String sessionToken = lst.get(position).token;
            apiHelper = new ApiHelper(this);
            if (apiHelper.send(this, "DELETE", "/session/close?token=" + sessionToken) == 200)
            updateList();
            if (sessionToken.equals(key))
            {
                i = new Intent(this, AuthActivity.class);
                finish();
                startActivity(i);
            }
        });
        adp = new ArrayAdapter<Session>(this, android.R.layout.simple_list_item_1, lst);
        lstctl.setAdapter(adp);

        adpAPI = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        adpAPI.add("http://89.108.78.244:5000");
        adpAPI.add("http://nodegraph.spbcoit.ru:5000");
        adpAPI.add("http://127.0.0.1:5000");
        spnChangeAPI.setAdapter(adpAPI);
        spnChangeAPI.setSelection(0);

        i = getIntent();
        key = i.getStringExtra("key");

        updateList();
    }

    public void updateList()
    {
        lst.clear();
        apiHelper = new ApiHelper(this)
        {
            @Override
            public void on_ready(String res) throws Exception
            {
                JSONArray jsonArray = new JSONArray(res);

                for (int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    Integer id = Integer.parseInt(obj.getString("id"));
                    String token = obj.getString("token");
                    //Date dt = new Date(Integer.parseInt(obj.getString("timestamp")));
                    //String abc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dt);
                    String timestamp = obj.getString("timestamp");

                    Session session = new Session(id, token, timestamp);
                    lst.add(session);
                }
                adp.notifyDataSetChanged();
            }
        };
        apiHelper.send(this, "GET", "/session/list?token=" + key);
    }

    public void onDeleteAcc(View v)
    {
        apiHelper = new ApiHelper(this);
        if (apiHelper.send(this, "DELETE", "/account/delete?token=" + key) == 200)
        {
            i = new Intent(this, AuthActivity.class);
            finish();
            startActivity(i);
        }
    }


    public void onReturn(View v)
    {
        i = new Intent(this, GraphActivity.class);
        i.putExtra("key", key);
        finish();
        startActivity(i);
    }

    public void onChangePass(View v)
    {
        String newPass = txtChangePass.getText().toString();
        apiHelper = new ApiHelper(this);
        apiHelper.send(this, "POST", "/account/update?token=" + key + "&secret=" + newPass);
    }

    public void onChangeAPI(View v)
    {
        g.graph.saveEndPoint(adpAPI.getItem(spnChangeAPI.getSelectedItemPosition()));
    }

    public void onCloseCurrentSession(View v)
    {
        apiHelper = new ApiHelper(this);
        if (apiHelper.send(this, "DELETE", "/session/close?token=" + key) == 200)
        {
            i = new Intent(this, AuthActivity.class);
            finish();
            startActivity(i);
        }
    }
}