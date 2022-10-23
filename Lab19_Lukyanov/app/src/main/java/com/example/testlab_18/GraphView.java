package com.example.testlab_18;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.provider.CalendarContract;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.ArrayAdapter;

import org.json.JSONObject;

import java.security.PublicKey;
import java.util.ArrayList;

public class GraphView extends SurfaceView {

    public Activity ctx;
    public Graph g = new Graph();
    Paint p;

    public int selected1 = -1;
    public int selected2 = -1;
    int lasthit = -1;
    int lastHitNodeAndLink = -1;
    int selectedLink = -1;

    float rad = 100.0f;
    float halfside = 5.0f;

    float last_x;
    float last_y;

    float sizeText, sizeValueLink;

    boolean nodeLink;
    public boolean isAPI;

    ApiHelper apiHelper;
    public String key;

    public void edit_selected_node(String text, float x, float y)
    {
        if (selected1 < 0) return;
        if (!isAPI) {
            g.node.get(selected1).text = text;
            g.node.get(selected1).x = x;
            g.node.get(selected1).y = y;
        }
        else
        {
            apiHelper = new ApiHelper(ctx);
            if (apiHelper.send(ctx, "POST", "/node/update?token=" + key + "&id=" + g.node.get(selected1).IDapi + "&x=" + x + "&y=" + y + "&name=" + text) == 200)
            {
                g.node.get(selected1).text = text;
                g.node.get(selected1).x = x;
                g.node.get(selected1).y = y;
            }
        }
    }

    public Node get_selected_node()
    {
        if (selected1 < 0) return null;
        Node n = g.node.get(selected1);
        return n;
    }

    public Link get_selected_link()
    {
        if (selectedLink < 0) return null;
        Link l = g.link.get(selectedLink);
        return l;
    }

    public void edit_selected_link(float value)
    {
        if (selectedLink < 0) return;
        if (!isAPI)
        g.link.get(selectedLink).value = value;
        else
        {
            apiHelper = new ApiHelper(ctx);
            if (apiHelper.send(ctx, "POST", "/link/update?token=" + key + "&id=" + g.link.get(selectedLink).IDapi + "&value=" + value) == 200)
                g.link.get(selectedLink).value = value;
        }
        selectedLink = -1;
    }

    public void add_node()
    {
        g.add_node(100.0f, 100.0f);
        invalidate();
    }

    public void remove_selected_node()
    {
        if (selected1 < 0) return;
        if (!isAPI) {
            g.remove_node(selected1);
            remove_links_at_node(selected1);
        }
        else
        {
            apiHelper = new ApiHelper(ctx);
            if (apiHelper.send(ctx, "DELETE", "/node/delete?token=" + key + "&id=" + g.node.get(selected1).IDapi) == 200) {
                g.remove_node(selected1);
                remove_links_at_node(selected1);
            }
        }
        selected1 = -1;
        invalidate();
    }

    public void link_selected_nodes(float value)
    {
        if (selected1 < 0) return;
        if (selected2 < 0) return;
        if (check_link_exist(selected1, selected2))
        {
            if (!isAPI) {
                g.add_link(selected1, selected2, value);
                invalidate();
            }
            else
            {
                apiHelper = new ApiHelper(ctx)
                {
                    @Override
                    public void on_ready(String res) throws Exception
                    {
                        JSONObject obj = new JSONObject(res);

                        String id = obj.getString("id");

                        g.add_link(selected1, selected2, value);
                        g.link.get(g.link.size()-1).IDapi = Integer.parseInt(id);
                        invalidate();
                    }
                };
                apiHelper.send(ctx, "PUT", "/link/create?token=" + key +
                        "&source=" + g.node.get(selected1).IDapi +
                        "&target=" + g.node.get(selected2).IDapi +
                        "&value=" + value);
            }
            selectedLink = -1;
        }
    }

    public void remove_selected_link()
    {
        if (selectedLink < 0) return;
        if (!isAPI)
        g.remove_link(selectedLink);
        else
        {
            apiHelper = new ApiHelper(ctx);
            if (apiHelper.send(ctx, "DELETE", "/link/delete?token=" + key + "&id=" + g.link.get(selectedLink).IDapi) == 200)
                g.remove_link(selectedLink);
        }
        selectedLink = -1;
        invalidate();
    }


    public GraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        p = new Paint();
        p.setAntiAlias(true);
        setWillNotDraw(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                int i = get_node_at_xy(x, y);
                lasthit = i;
                lastHitNodeAndLink = lasthit;
                if (lastHitNodeAndLink < 0)
                {
                    nodeLink = false;
                    lastHitNodeAndLink = get_link_at_xy(x,y);
                }
                else nodeLink = true;
                if (i < 0)
                {
                    selected1 = -1;
                    selected2 = -1;
                }
                else
                {
                    if (selected1 >= 0) selected2 = i;
                    else selected1 = i;
                }
                selectedLink = get_link_at_xy(x, y);
                /*
                selected = -1;
                for (int i = g.node.size() - 1; i >= 0 ; i--)
                {
                    Node n = g.node.get(i);
                    float dx = x - n.x;
                    float dy = y - n.y;
                    if (dx*dx+dy*dy <= rad*rad)
                    {
                        selected = i;
                        break;
                    }
                }
                */
                last_x = x;
                last_y = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                break;

            case MotionEvent.ACTION_MOVE:
            {
                if (lasthit >= 0 && nodeLink)
                {
                    Node n = g.node.get(lasthit);
                    if (!isAPI) {
                        n.x += x - last_x;
                        n.y += y - last_y;
                    }
                    else
                    {
                        float newx = n.x + x - last_x;
                        float newy = n.y + y - last_y;
                        apiHelper = new ApiHelper(ctx);
                        if (apiHelper.send(ctx, "POST", "/node/update?token=" + key + "&id=" + g.node.get(lasthit).IDapi + "&x=" + newx + "&y=" + newy + "&name=" + n.text) == 200)
                        {
                            n.x += x - last_x;
                            n.y += y - last_y;
                        }
                    }
                    invalidate();
                }
                last_x = x;
                last_y = y;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public float calculateAngle(float x0, float y0, float x1, float y1)
    {
        float angle = (float)Math.toDegrees(Math.atan2(x1 - x0, y1 - y0));

        angle = angle + (float)Math.ceil(-angle / 360) * 360; //Keep angle between 0 and 360

        return angle;
    }

    public class Rectangle
    {
        public int linkID;
        public float x0;
        public float x1;
        public float y0;
        public float y1;

        public Rectangle(int linkID, float x0, float y0, float x1, float y1)
        {
            this.linkID = linkID;
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
        }
    }
    Rectangle[] linkRectangles;
    ArrayList<Integer> skip = new ArrayList<Integer>();

    public boolean isLinkSkip(int id)
    {
        for (int i = 0; i < skip.size(); i++)
        {
            if (id == skip.get(i)) return false;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(255, 255, 255));
        //int skip = -1;
        boolean twoLines = false;
        skip.clear();
        linkRectangles = new Rectangle[g.link.size()];
        for (int i = 0; i < g.link.size(); i++)
        {

            Link l = g.link.get(i);
            Node na = g.node.get(l.a);
            Node nb = g.node.get(l.b);
            p.setColor(Color.argb(127, 0, 0, 0));
            canvas.drawLine(na.x, na.y, nb.x, nb.y, p);

            //arrows
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(0,0,0));
            float angle = calculateAngle(na.x, na.y, nb.x, nb.y);
            angle = 180 - angle;
            Path arrow_path = new Path();

            Matrix arrow_matrix = new Matrix();

            arrow_matrix.postRotate(angle, nb.x, nb.y);

            arrow_path.moveTo(nb.x, nb.y);
            arrow_path.lineTo(nb.x - 5, nb.y + 10);
            arrow_path.moveTo(nb.x, nb.y);
            arrow_path.lineTo(nb.x + 5, nb.y + 10);
            arrow_path.lineTo(nb.x - (5), nb.y + 10);
            arrow_path.transform(arrow_matrix);

            canvas.drawPath(arrow_path, p);
            if (isLinkSkip(i))
            {
                float bx = (na.x + nb.x) * 0.5f;
                float by = (na.y + nb.y) * 0.5f;
                float x0 = bx - halfside;
                float y0 = by - halfside;
                float x1 = bx + halfside;
                float y1 = by + halfside;

                //value and twolines
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.rgb(0, 0, 0));
                sizeValueLink = String.valueOf(l.value).length() * 2.9f;
                twoLines = false;
                for (int j = i+1; j < g.link.size(); j++) {
                    if (g.link.get(i).a == g.link.get(j).b && g.link.get(i).b == g.link.get(j).a) {
                        sizeValueLink = String.valueOf(g.link.get(i).value).length() * 2.9f;
                        canvas.drawText("" + g.link.get(i).value, x0 + halfside - sizeValueLink, y0 + halfside * 2 + 15, p);
                        linkRectangles[i] = new Rectangle(i, x0 - sizeValueLink, y0 + 10, x1 + sizeValueLink + 3, y1 + 20);
                        canvas.drawRect(x0 - sizeValueLink, y0 + 10, x1 + sizeValueLink + 3, y1 + 20, p);
                        sizeValueLink = String.valueOf(g.link.get(j).value).length() * 2.9f;
                        canvas.drawText("" + g.link.get(j).value, x0 + halfside - sizeValueLink, y0 + halfside * 2 - 15, p);
                        linkRectangles[j] = new Rectangle(j, x0 - sizeValueLink, y0 - 20, x1 + sizeValueLink + 3, y1 - 10);
                        canvas.drawRect(x0 - sizeValueLink, y0 - 20, x1 + sizeValueLink + 3, y1 - 10, p);
                        twoLines = true;
                        //skip = j;
                        skip.add(j);
                    }
                }
                if (!twoLines) {
                    canvas.drawText("" + l.value, x0 + halfside - sizeValueLink, y0 + halfside * 2, p);
                    linkRectangles[i] = new Rectangle(i, x0 - sizeValueLink, y0 - 5, x1 + sizeValueLink + 3, y1 + 5);
                    canvas.drawRect(x0 - sizeValueLink, y0-5, x1 + sizeValueLink+3, y1+5, p);
                }
            }
        }
        for (int i = 0; i < g.node.size(); i++)
        {
            Node n = g.node.get(i);

            p.setStyle(Paint.Style.FILL);

            if (i == selected1) p.setColor(Color.argb(50, 127, 0, 255));
            else if (i == selected2 ) p.setColor(Color.argb(50, 255, 0, 50));
            else p.setColor(Color.argb(50, 0, 127, 255));

            canvas.drawCircle(n.x, n.y, rad, p);

            p.setStyle(Paint.Style.STROKE);

            if (i == selected1) p.setColor(Color.rgb(127, 0, 255));
            else if (i == selected2) p.setColor(Color.rgb(255, 0, 50));
            else p.setColor(Color.rgb(0,127,255));

            canvas.drawCircle(n.x, n.y, rad, p);

            if (n.text != null && n.text != "" && !n.text.isEmpty())
            {
                p.setColor(Color.rgb(0,0,0));
                sizeText = n.text.length()*2.9f;
                canvas.drawText(n.text, n.x-sizeText, n.y+rad+20, p);
            }
        }
        //super.onDraw(canvas);
    }

    public int get_node_at_xy(float x, float y)
    {
        for (int i = g.node.size() - 1; i >= 0; i--)
        {
            Node n = g.node.get(i);
            float dx = x - n.x;
            float dy = y - n.y;
            if (dx * dx + dy * dy <= rad * rad) return i;
        }
        return -1;
    }

    public int get_link_at_xy(float x, float y)
    {
        for (int i = 0; i < g.link.size(); i++)
        {
            /*
            Link l = g.link.get(i);
            Node na = g.node.get(l.a);
            Node nb = g.node.get(l.b);
            float bx = (na.x + nb.x) * 0.5f;
            float by = (na.y + nb.y) * 0.5f;
            float x0 = bx - halfside;
            float y0 = by - halfside;
            float x1 = bx + halfside;
            float y1 = by + halfside;
            if (x >= x0 && x <= x1 && y >= y0 && y <= y1) return i;

             */
            float x0 = linkRectangles[i].x0;
            float y0 = linkRectangles[i].y0;
            float x1 = linkRectangles[i].x1;
            float y1 = linkRectangles[i].y1;
            if (x >= x0 && x <= x1 && y >= y0 && y <= y1) return i;
        }
        return -1;
    }

    public void remove_links_at_node(int node)
    {
        for (int i = g.link.size()-1; i >= 0; i--)
        {
            if (g.link.get(i).a == node || g.link.get(i).b == node)
            {
                g.remove_link(i);
            }
        }
        for (int i = 0; i < g.link.size(); i++)
        {
            if (g.link.get(i).a > node) g.link.get(i).a--;
            if (g.link.get(i).b > node) g.link.get(i).b--;
        }
    }

    public boolean check_link_exist(int a, int b)
    {
        for (int i = 0; i < g.link.size(); i++)
        {
            if (g.link.get(i).a == a && g.link.get(i).b == b)
                return false;
        }
        return true;
    }



    public int findIndexLinkFromAPI(int IDapi)
    {
        for (int i = 0; i < g.link.size(); i++)
        {
            if (g.link.get(i).IDapi == IDapi) return i;
        }
        return -1;
    }
}
