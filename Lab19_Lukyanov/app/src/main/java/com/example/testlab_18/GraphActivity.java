package com.example.testlab_18;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class GraphActivity extends AppCompatActivity {

    ListView lstctl;
    ArrayList<Graph> lst = new ArrayList<>();
    ArrayAdapter<Graph> adp;

    TextView tvSelectedGraph;
    EditText txtName;

    GraphView gv;
    Graph graph;

    Intent i;

    int selectedGraph;

    String key;

    ApiHelper apiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        g.graph = new DB(this, "graph.db", null, 1);

        tvSelectedGraph = findViewById(R.id.tvSelGraph);
        txtName = findViewById(R.id.editTextRename);
        lstctl = findViewById(R.id.listView);
        lstctl.setOnItemClickListener((parent, view, position, id) ->
        {
            selectedGraph = (int) id + 1;
            tvSelectedGraph.setText("Selected graph: " + selectedGraph);
        });
        adp = new ArrayAdapter<Graph>(this, android.R.layout.simple_list_item_1, lst);
        lstctl.setAdapter(adp);
        boolean isTokenActive = chkTokenActive(g.graph.getToken());

        i = getIntent();

        key = i.getStringExtra("key");
        if (key == null && !isTokenActive)
        {
            i = new Intent(this, AuthActivity.class);
            startActivity(i);
        }
        else
        {
            if (key == null)
            key = g.graph.getToken();
        }

        updateList();

        graph = new Graph();
        int countNode, countLink;
        String text;
        int a, b;
        float x, y, value;
        countNode = i.getIntExtra("countNode", -1);
        countLink = i.getIntExtra("countLink", -1);
        for (int j = 0; j < countNode; j++)
        {
            x = i.getFloatExtra("graph_node_" + j + "x", -1);
            y = i.getFloatExtra("graph_node_" + j + "y", -1);
            text = i.getStringExtra("graph_node_" + j + "text");
            graph.add_node(x, y);
            graph.node.get(j).text = text;
        }
        for (int j = 0; j < countLink; j++)
        {
            a = i.getIntExtra("graph_link_" + j + "a", -1);
            b = i.getIntExtra("graph_link_" + j + "b", -1);
            value = i.getFloatExtra("graph_link_" + j + "value", -1);
            graph.add_link(a, b, value);
        }
    }

    public boolean chkTokenActive(String token)
    {
        ApiHelper apiHelper = new ApiHelper(this);
        if (apiHelper.send(this, "GET", "/session/list?token=" + token) == 200) return true;
        return false;
    }

    public void updateList()
    {
        lst.clear();
        g.graph.getAllGraphs(lst);
        getApiGraphs(lst);
        while (!apiHelper.isEnd) ;
        adp.notifyDataSetChanged();
    }

    public void onSave(View v)
    {
        graph.name = txtName.getText().toString();
        g.graph.onSaveGraph(graph);
        updateList();
    }

    public void onLoad(View v)
    {
        if (selectedGraph < 0) return;
        if (lst.get(selectedGraph-1).apiNumber < 0) {
            i = new Intent(this, MainActivity.class);
            i.putExtra("isAPI", false);
            graph = g.graph.onLoadGraph(selectedGraph);
            int countNode = 0, countLink = 0;
            for (int j = 0; j < graph.node.size(); j++) {
                i.putExtra("graph_node_" + j + "x", graph.node.get(j).x);
                i.putExtra("graph_node_" + j + "y", graph.node.get(j).y);
                i.putExtra("graph_node_" + j + "text", graph.node.get(j).text);
                countNode++;
            }
            for (int j = 0; j < graph.link.size(); j++) {
                i.putExtra("graph_link_" + j + "a", graph.link.get(j).a);
                i.putExtra("graph_link_" + j + "b", graph.link.get(j).b);
                i.putExtra("graph_link_" + j + "value", graph.link.get(j).value);
                countLink++;
            }
            i.putExtra("countNode", countNode);
            i.putExtra("countLink", countLink);
            setResult(1000, i);
            finish();
            startActivity(i);
        }
        else
        {
            i = new Intent(this, MainActivity.class);
            i.putExtra("isAPI", true);
            i.putExtra("key", key);
            i.putExtra("APIgraphID", lst.get(selectedGraph-1).apiNumber);
            setResult(2000, i);
            finish();
            startActivity(i);
        }
    }

    public void onRename(View v)
    {
        if (lst.get(selectedGraph-1).apiNumber < 0)
        g.graph.onRenameGraph(txtName.getText().toString(), selectedGraph);
        else if (lst.get(selectedGraph-1).apiNumber > 0)
        {
            apiHelper = new ApiHelper(this);
            apiHelper.send(this, "POST", "/graph/update?token=" + key + "&id=" + lst.get(selectedGraph-1).apiNumber + "&name=" + txtName.getText().toString());
        }
        updateList();
    }

    public void onDelete(View v)
    {
        if (lst.get(selectedGraph-1).apiNumber < 0)
        g.graph.onDeleteGraph(selectedGraph);
        else if (lst.get(selectedGraph-1).apiNumber > 0)
        {
            apiHelper = new ApiHelper(this);
            apiHelper.send(this, "DELETE", "/graph/delete?token=" + key + "&id=" + lst.get(selectedGraph-1).apiNumber);
        }
        updateList();
    }

    public void onCopy(View v)
    {
        if (lst.get(selectedGraph-1).apiNumber < 0)
        g.graph.onSaveGraph(g.graph.onLoadGraph(selectedGraph));
        else if (lst.get(selectedGraph-1).apiNumber > 0)
        {
            //Graph graph1 = onReceiveGraph(lst.get(selectedGraph-1).apiNumber, this, key);
            //graph1.name = lst.get(selectedGraph-1).name;
            //onSaveGraphAPI(graph1, this, key);
        }
        updateList();
    }

    public static Graph onReceiveGraph(int APIgraphID, Activity ctx, String key)
    {
        Graph graphApi = new Graph();
        final boolean[] importNode = {false};
        final boolean[] importLink = {false};
        ApiHelper apiHelper = new ApiHelper(ctx)
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

                    graphApi.add_node(x,y);
                    graphApi.node.get(graphApi.node.size()-1).IDapi = id;
                    graphApi.node.get(graphApi.node.size()-1).text = name;
                }
                importNode[0] = true;
            }
        };
        apiHelper.send(ctx, "GET", "/node/list?token=" + key + "&id=" + APIgraphID);
        if (importNode[0]) {
            apiHelper = new ApiHelper(ctx) {
                @Override
                public void on_ready(String res) throws Exception {
                    JSONArray jsonArray = new JSONArray(res);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        Integer id = Integer.parseInt(obj.getString("id"));
                        Integer source = Integer.parseInt(obj.getString("x"));
                        Integer target = Integer.parseInt(obj.getString("y"));
                        Float value = Float.parseFloat(obj.getString("name"));

                        graphApi.add_link(graphApi.findIndexNodeFromAPI(source), graphApi.findIndexNodeFromAPI(target), value);
                        graphApi.link.get(graphApi.link.size() - 1).IDapi = id;
                    }
                    importLink[0] = true;
                }
            };
            apiHelper.send(ctx, "GET", "/link/list?token=" + key + "&id=" + APIgraphID);
            if (importLink[0])
            return graphApi;
        }
        while (true);
    }

    public static void onSaveGraphAPI(Graph graphApi, Activity ctx, String key)
    {
        ApiHelper apiHelper;
        final int[] graphID = new int[1];
        apiHelper = new ApiHelper(ctx)
        {
            @Override
            public void on_ready(String res) throws Exception
            {
                JSONObject obj = new JSONObject(res);

                graphID[0] = Integer.parseInt(obj.getString("id"));
            }
        };
        apiHelper.send(ctx, "PUT", "/graph/create?token=" + key + "&name=" + graphApi.name);
        for (int j = 0; j < graphApi.node.size(); j++)
        {
            graphApi.node.get(j).IDapi = graphID[0];
            int finalJ = j;
            apiHelper = new ApiHelper(ctx)
            {
                @Override
                public void on_ready(String res) throws Exception
                {
                    JSONObject obj = new JSONObject(res);

                    String id = obj.getString("id");
                    graphApi.node.get(finalJ).IDapi = Integer.parseInt(id);
                }
            };
            apiHelper.send(ctx, "PUT", "/node/create?token=" + key +
                    "&id=" + graphApi.node.get(j).IDapi +
                    "&x=" + graphApi.node.get(j).x +
                    "&y=" + graphApi.node.get(j).y +
                    "&name=" + graphApi.node.get(j).text);
        }
        for (int j = 0; j < graphApi.link.size(); j++)
        {
            apiHelper = new ApiHelper(ctx);
            int IDapiA = graphApi.link.get(j).a;
            int IDapiB = graphApi.link.get(j).b;
            apiHelper.send(ctx, "PUT", "/link/create?token=" + key +
                    "&source=" + graphApi.node.get(IDapiA).IDapi +
                    "&target=" + graphApi.node.get(IDapiB).IDapi +
                    "&value=" + graphApi.link.get(j).value);
        }
    }

    public void onReset(View v)
    {
        g.graph.onClearGraphs();
        updateList();
    }

    public void onCreateGraph(View v)
    {
        String graphName = txtName.getText().toString();
        ApiHelper apiHelper = new ApiHelper(this)
        {
            @Override
            public void on_ready(String res) throws Exception
            {
                JSONObject obj = new JSONObject(res);

                String graphID = obj.getString("id");

                i = new Intent(this.ctx, MainActivity.class);
                i.putExtra("isAPI", true);
                i.putExtra("isCreate", true);
                i.putExtra("APIgraphID", graphID);
                i.putExtra("key", key);
                finish();
                startActivity(i);
            }
        };
        apiHelper.send(this, "PUT", "/graph/create?token=" + key + "&name=" + graphName);
    }

    public void onSessionSettings(View v)
    {
        i = new Intent(this, SessionActivity.class);
        i.putExtra("key", key);
        finish();
        startActivity(i);
    }


    public void getApiGraphs(ArrayList<Graph> lst)
    {
        apiHelper = new ApiHelper(this)
        {
            @Override
            public void on_ready(String res) throws Exception
            {
                JSONArray jsonArray = new JSONArray(res);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id = jsonObject.getString("id");
                    String name = jsonObject.getString("name");

                    Graph graph2 = new Graph();
                    graph2.apiNumber = Integer.parseInt(id);
                    graph2.name = name;
                    lst.add(graph2);
                }
                adp.notifyDataSetChanged();
            }
        };
        apiHelper.send(this, "GET", "/graph/list?token=" + key);
    }


}