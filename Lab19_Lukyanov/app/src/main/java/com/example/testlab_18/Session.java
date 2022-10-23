package com.example.testlab_18;

import java.sql.Timestamp;

public class Session
{
    public int id;
    public String token;
    public String timestamp;

    public Session(int id, String token, String timestamp)
    {
        this.id = id;
        this.token = token;
        this.timestamp = timestamp;
    }

    public String toString()
    {
        return id + "\t|\t" + token;
                //+ "\t|\t" + timestamp;
    }
}
