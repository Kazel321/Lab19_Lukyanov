package com.example.testlab_18;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.FormatException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    GraphView gv;
    Graph graph;

    Intent i;

    AlertDialog alertDialogNode;
    AlertDialog alertDialogLink;

    EditText txtTextNode;
    EditText txtXNode;
    EditText txtYNode;

    EditText txtValueLink;

    Button btnSaveNode;
    Button btnSaveLink;

    int selectedNode;
    int APIgraphID;

    View dialogViewLink;

    Float valueLink;

    String key;

    boolean isAPI;

    ApiHelper apiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gv = findViewById(R.id.graphView);
        gv.ctx = this;

        i = getIntent();
        onReceiveActivity(i);

        //dialogNodeProperties
        LayoutInflater dialogLayoutNode = LayoutInflater.from(this);
        View dialogViewNode = dialogLayoutNode.inflate(R.layout.dialog_properties, null);
        alertDialogNode = new AlertDialog.Builder(this).create();
        alertDialogNode.setView(dialogViewNode);
        txtTextNode = dialogViewNode.findViewById(R.id.editTextNode);
        txtXNode = dialogViewNode.findViewById(R.id.editTextXNode);
        txtYNode = dialogViewNode.findViewById(R.id.editTextYNode);
        btnSaveNode = dialogViewNode.findViewById(R.id.btnSaveNode);
        btnSaveNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtXNode.getText().toString().isEmpty() || txtYNode.getText().toString().isEmpty())
                {
                    Toast.makeText(dialogViewNode.getContext(), "X or Y is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                String text = txtTextNode.getText().toString();
                Float x = Float.parseFloat(txtXNode.getText().toString());
                Float y = Float.parseFloat(txtYNode.getText().toString());
                if (x < 0 || y < 0)
                {
                    Toast.makeText(dialogViewNode.getContext(), "X or Y is less than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                gv.edit_selected_node(text, x, y);
                alertDialogNode.cancel();
                gv.invalidate();
            }
        });

        //dialogLinkProperties
        LayoutInflater dialogLayoutLink = LayoutInflater.from(this);
        dialogViewLink = dialogLayoutLink.inflate(R.layout.dialog_link_value, null);
        alertDialogLink = new AlertDialog.Builder(this).create();
        alertDialogLink.setView(dialogViewLink);
        txtValueLink = dialogViewLink.findViewById(R.id.editTextLinkvalue);
        btnSaveLink = dialogViewLink.findViewById(R.id.btnSaveLink);
        btnSaveLink.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onClick(View view) {
                if (txtValueLink.getText().toString().isEmpty())
                {
                    Toast.makeText(dialogViewLink.getContext(), "Value is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                valueLink = Float.parseFloat(txtValueLink.getText().toString());
                if (linkEdit)
                {
                    gv.edit_selected_link(valueLink);
                    linkEdit = false;
                }
                else
                gv.link_selected_nodes(valueLink);
                alertDialogLink.cancel();
                gv.invalidate();
            }
        });
    }

    public void on_add_click(View v)
    {
        if (!isAPI)
        gv.add_node();
        else
        {
            apiHelper = new ApiHelper(this)
            {
                @Override
                public void on_ready(String res) throws Exception
                {
                    JSONObject obj = new JSONObject(res);

                    String id = obj.getString("id");
                    gv.add_node();
                    gv.g.node.get(gv.g.node.size()-1).IDapi = Integer.parseInt(id);
                }
            };
            apiHelper.send(this, "PUT", "/node/create?token=" + key +
                    "&id=" + APIgraphID +
                    "&x=" + 100 +
                    "&y=" + 100 +
                    "&name=" + "");
        }
    }

    public void on_remove_click(View v)
    {
        gv.remove_selected_node();
    }

    public void on_link_add(View v)
    {
        if (gv.selected1 < 0) return;
        if (gv.selected2 < 0) return;
        if (gv.check_link_exist(gv.selected1, gv.selected2))
        {
            alertDialogLink.show();
        }
    }

    public void on_link_remove(View v)
    {
        gv.remove_selected_link();
    }

    public void on_graphs(View v)
    {
        i = new Intent(this, GraphActivity.class);
        Graph graph = gv.g;
        int countNode = 0, countLink = 0;
        for (int j = 0; j < graph.node.size(); j++)
        {
            i.putExtra("graph_node_" + j + "x", graph.node.get(j).x);
            i.putExtra("graph_node_" + j + "y", graph.node.get(j).y);
            i.putExtra("graph_node_" + j + "text", graph.node.get(j).text);
            countNode++;
        }
        for (int j = 0; j < graph.link.size(); j++)
        {
            i.putExtra("graph_link_" + j + "a", graph.link.get(j).a);
            i.putExtra("graph_link_" + j + "b", graph.link.get(j).b);
            i.putExtra("graph_link_" + j + "value", graph.link.get(j).value);
            countLink++;
        }
        i.putExtra("countNode", countNode);
        i.putExtra("countLink", countLink);
        finish();
        startActivity(i);
    }

    public void onReceiveActivity(Intent data)
    {
        gv.selected1 = -1;
        gv.selected2 = -1;
        boolean isAPIReceive = i.getBooleanExtra("isAPI", false);
        if (!isAPIReceive)
        {
            isAPI = false;
            gv.isAPI = false;
            gv.g = new Graph();
            int countNode, countLink;
            int a, b;
            float x, y, value;
            String text;
            countNode = data.getIntExtra("countNode", -1);
            countLink = data.getIntExtra("countLink", -1);
            for (int j = 0; j < countNode; j++)
            {
                x = data.getFloatExtra("graph_node_" + j + "x", -1);
                y = data.getFloatExtra("graph_node_" + j + "y", -1);
                text = data.getStringExtra("graph_node_" + j + "text");
                gv.g.add_node(x, y);
                gv.g.node.get(j).text = text;
            }
            for (int j = 0; j < countLink; j++)
            {
                a = data.getIntExtra("graph_link_" + j + "a", -1);
                b = data.getIntExtra("graph_link_" + j + "b", -1);
                value = data.getFloatExtra("graph_link_" + j + "value", -1);
                gv.g.add_link(a, b, value);
            }
            gv.invalidate();
        }
        else if (isAPIReceive)
        {
            isAPI = true;
            gv.isAPI = true;
            gv.g = new Graph();
            APIgraphID = data.getIntExtra("APIgraphID", -1);
            key = data.getStringExtra("key");
            gv.key = key;
            if (data.getBooleanExtra("isCreate", false)) return;
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
                        Float x = Float.parseFloat(obj.getString("x"));
                        Float y = Float.parseFloat(obj.getString("y"));
                        String name = obj.getString("name");

                        gv.g.add_node(x,y);
                        gv.g.node.get(gv.g.node.size()-1).IDapi = id;
                        gv.g.node.get(gv.g.node.size()-1).text = name;
                    }
                    gv.invalidate();
                }
            };
            apiHelper.send(this, "GET", "/node/list?token=" + key + "&id=" + APIgraphID);
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
                        Integer source = Integer.parseInt(obj.getString("source"));
                        Integer target = Integer.parseInt(obj.getString("target"));
                        Float value = Float.parseFloat(obj.getString("value"));

                        gv.g.add_link(gv.g.findIndexNodeFromAPI(source), gv.g.findIndexNodeFromAPI(target), value);
                        gv.g.link.get(gv.g.link.size()-1).IDapi = id;
                    }
                    gv.invalidate();
                }
            };
            apiHelper.send(this, "GET", "/link/list?token=" + key + "&id=" + APIgraphID);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        gv.selected1 = -1;
        gv.selected2 = -1;
        if (resultCode == 1000)
        {
            isAPI = false;
            gv.g = new Graph();
            int countNode, countLink;
            int a, b;
            float x, y, value;
            String text;
            countNode = data.getIntExtra("countNode", -1);
            countLink = data.getIntExtra("countLink", -1);
            for (int j = 0; j < countNode; j++)
            {
                x = data.getFloatExtra("graph_node_" + j + "x", -1);
                y = data.getFloatExtra("graph_node_" + j + "y", -1);
                text = data.getStringExtra("graph_node_" + j + "text");
                gv.g.add_node(x, y);
                gv.g.node.get(j).text = text;
            }
            for (int j = 0; j < countLink; j++)
            {
                a = data.getIntExtra("graph_link_" + j + "a", -1);
                b = data.getIntExtra("graph_link_" + j + "b", -1);
                value = data.getFloatExtra("graph_link_" + j + "value", -1);
                gv.g.add_link(a, b, value);
            }
            gv.invalidate();
        }
        if (resultCode == 2000)
        {
            isAPI = true;
            gv.g = new Graph();
            APIgraphID = Integer.parseInt(data.getStringExtra("APIgraphID"));
            key = data.getStringExtra("key");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean linkEdit = false;

    public void on_node_prop(View v)
    {
        if (gv.nodeLink) {
            Node n = gv.get_selected_node();
            if (n == null) return;
            txtTextNode.setText(n.text);
            txtXNode.setText("" + n.x);
            txtYNode.setText("" + n.y);
            alertDialogNode.show();
        }
        else
        {
            linkEdit = true;
            Link l = gv.get_selected_link();
            if (l == null) return;
            txtValueLink.setText("" + l.value);
            alertDialogLink.show();
        }
    }

}